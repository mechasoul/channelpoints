package my.cute.channelpoints.obs;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import my.cute.channelpoints.obs.events.EventBase;
import my.cute.channelpoints.obs.events.EventHandler;
import my.cute.channelpoints.obs.events.EventType;
import my.cute.channelpoints.obs.events.StudioModeSwitchedEvent;
import my.cute.channelpoints.obs.events.SwitchScenesEvent;
import my.cute.channelpoints.obs.requests.ResponseBase;
import my.cute.channelpoints.obs.requests.authenticate.AuthenticateResponse;
import my.cute.channelpoints.obs.requests.getauthrequired.GetAuthRequiredResponse;
import my.cute.channelpoints.obs.requests.getmediastate.GetMediaStateResponse;
import my.cute.channelpoints.obs.requests.getsceneitemlist.GetSceneItemListResponse;
import my.cute.channelpoints.obs.requests.getstudiomodestatus.GetStudioModeStatusResponse;
import my.cute.channelpoints.obs.requests.getvideoinfo.GetVideoInfoResponse;
import my.cute.channelpoints.obs.types.SimpleSceneItem;

public class MessageReceiver {
	
	private class CallbackWrapper<T extends ResponseBase> {
		private final Class<T> responseType;
		private final Consumer<T> callback;
		
		private CallbackWrapper(Class<T> type, Consumer<T> callback) {
			this.responseType = type;
			this.callback = callback;
		}

		public Class<T> getResponseType() {
			return this.responseType;
		}

		public Consumer<T> getCallback() {
			return this.callback;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CallbackWrapper [responseType=");
			builder.append(this.responseType);
			builder.append(", callback=");
			builder.append(this.callback);
			builder.append("]");
			return builder.toString();
		}
	}
	
	private class FutureWrapper<T extends ResponseBase> {
		private final Class<T> responseType;
		private final CompletableFuture<?> future;
		
		private FutureWrapper(Class<T> type, CompletableFuture<?> future) {
			this.responseType = type;
			this.future = future;
		}

		public Class<T> getResponseType() {
			return this.responseType;
		}

		public CompletableFuture<?> getFuture() {
			return this.future;
		}
	}
	
	/*
	 * ok i have some problems with generics and wildcards and stuff in this class
	 * and it's in working condition now but i still feel uncomfortable with how i've chosen to implement
	 * a lot of it
	 * 
	 * initially, i had two maps, one mapping messageId -> response type, and one mapping messageId ->
	 * consumers (callbacks). at the time a request is made, a callback is optionally provided by user,
	 * telling what to do with the response data when it arrives. in order to execute this callback later,
	 * the callback is stored. also, the callback must use the actual subtype of ResponseBase, so that it
	 * can access all the data contained in that specific response type 
	 * 
	 * (NOTE i could instead have the
	 * user-provided consumers take the relevant data exclusive to the response type eg the callback for
	 * GetSceneItemListRequest could take a List<SimpleSceneItem> rather than a GetSceneItemListResponse that 
	 * the user uses to access the item list via get method. this is what i was doing originally, and the
	 * request-creating methods in OBSWebSocketClient were wrapping the user-provided callbacks in a 
	 * consumer that took a general ResponseBase, checked and casted it to the relevant response type, and
	 * then provided its data to the user-provided callback. this was functional, but i had to duplicate
	 * this code in every OBSWebSocketClient request method, since the exact parameters provided to the 
	 * user-given callback were different for each request type. i'm not sure how i could avoid duplicating
	 * this code without using reflection to pass the relevant response get methods to something, which led
	 * me to look at alternate ways of doing it so i ended up on this)
	 * 
	 * so in order to store these consumers, either i do a Map<String, Consumer> and have unparameterized 
	 * type scariness, or i do Map<String, Consumer<? extends ResponseBase>>. this has to be ? and not an
	 * actual type, since all the consumers could take different response types. it also can't just be
	 * Consumer<ResponseBase> since, again, the consumers need to use the actual response type in order to
	 * access type-specific information. i also store the Class of the expected response type, since that's
	 * needed to construct the response object via gson.fromJson(String, Class), so we also have 
	 * Map<String, Class<? extends ResponseBase>>
	 * 
	 * so when message is received, it's only a json string, so we check it for messageId. using messageId 
	 * we retrieve the class of the response type to build and we build it via gson. so now we have a response
	 * object that's type capture#1-of ? extends ResponseBase, and when we retrieve the callback, it takes
	 * capture#2-of ? extends ResponseBase, so the response can't be passed to the callback! 
	 * 
	 * so i figure i need some way to associate the two wildcard classes. i can't provide a generic type 
	 * parameter to the method since i only have a string (and i have no way to show the two wildcards are
	 * actually the same type anyway). i thought a simple wrapper class would be a good idea - it can hold 
	 * the class required to construct the response object as well as the consumer that uses it (i think
	 * technically i could omit the class and get it from the consumer via reflection but i really want to
	 * avoid using reflection if possible. using class objects already makes me a bit uncomfortable), and 
	 * since a single wrapper object would be parameterized with T extends ResponseBase, both the class and
	 * the consumer should have the same type (wrapper.getClass() gives T, wrapper.getCallback() gives 
	 * Consumer<T>). but when i put this into practice, doing
	 * 
	 * CallbackWrapper<? extends ResponseBase> wrapper = (...) //get wrapper
	 * this.processResponse(this.gson.fromJson(data, wrapper.getResponseType()), wrapper.getCallback());
	 * 
	 * gives compile error in line 2, same thing! (wrapper.getResponseType() is capture#1-of ?, so response
	 * object is capture#1-of ?, and wrapper.getCallback() is Consumer<capture#2-of ?>, even though they
	 * come from the same wrapper object and consequently must share parameter types...
	 * 
	 * i could parameterize receiveMessage with T and cast (CallbackWrapper<T>) wrapper, but this gives
	 * unchecked cast warning with no real way to check it, at which point i might as well just use raw
	 * consumers or whatever. this is the solution i ended up on (independently, although i was surprised to
	 * find after that it's the recommended solution as said in
	 * https://docs.oracle.com/javase/tutorial/java/generics/capture.html ) - using a helper method to
	 * capture the wildcard type as a single type parameter. it works, there's no warnings, but it feels 
	 * really incorrect and i think there must be a more elegant way to do this without resorting to raw 
	 * types or reflection or whatever. ie, some way this is simply solvable with generics and wildcards
	 * by themselves. maybe something to come back to when i'm more experienced
	 */
	private final ConcurrentMap<String, CallbackWrapper<? extends ResponseBase>> responseCallbacks;
	/*
	 * re: the above, when it comes to the future model, i frankly don't see any way it'd be possible
	 * to avoid some kind of raw type/unchecked cast situation. since futures take some arbitrary
	 * data from the result, they could be parameterized with any type, so i can't think of how i'd
	 * be able to eventually do like future.complete(booleanValue) for a future that corresponds to
	 * GetStudioModeStatusRequest - at the time i complete the future i'd need to know it's 
	 * CompletableFuture<Boolean>, so i'd need to have a data structure that holds specifically
	 * CompletableFuture<Boolean>, but also when i originally pass the future in to this class to
	 * hold until the response arrives, i'd need to, again, pass it in as CompletableFuture<Boolean>
	 * and not as CompletableFuture<?>, otherwise i can't add it to my CompletableFuture<Boolean>-
	 * taking structure. so then i'm required to have like, separate prepareForSomeTypeResponse() 
	 * methods for each request that can use futures, so they can take the appropriately parameterized
	 * future and place it into the corresponding structure, and at that point i'd frankly rather just
	 * accept having an unchecked cast warning
	 */
	private final ConcurrentMap<String, FutureWrapper<? extends ResponseBase>> responseFutures;
	private final EventHandler eventHandler;
	private final Gson gson;
	private final OBSWebSocketClient client;
	
	MessageReceiver(OBSWebSocketClient client) {
		this.responseCallbacks = new ConcurrentHashMap<>();
		this.responseFutures = new ConcurrentHashMap<>();
		this.gson = new GsonBuilder()
				.create();
		this.client = client;
		
		this.eventHandler = new EventHandler();
		this.registerEventListener(ObsEventListener.createEventListener(SwitchScenesEvent.class, event -> {
			this.client.setCurrentSceneName(event.getSceneName());
			return false;
		}));
		this.registerEventListener(ObsEventListener.createEventListener(StudioModeSwitchedEvent.class, event -> {
			this.client.setStudioMode(event.getNewState());
			return false;
		}));
	}

	/*
	 * see enormous comment above this.responseCallbacks declaration for discussion on this method
	 * and its helper method and why im doing it this way
	 */
	/**
	 * takes a json-formatted obs-websocket response message, processes it into an actual response
	 * object, and then does stuff with it. at minimum, this will look for a user-provided 
	 * callback provided with this response's corresponding request, and execute it if it exists
	 * @param data the json String received from the websocket listener
	 */
	public void receiveMessage(String data) {
		JsonElement jsonElement = JsonParser.parseString(data);
		if(jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			if(jsonObject.has("message-id")) {
				String messageId = jsonObject.get("message-id").getAsString();
				CallbackWrapper<? extends ResponseBase> wrapper = this.responseCallbacks.remove(messageId);
				System.out.println("received message id: " + messageId + ", wrapper: " + (wrapper == null ? "null" : wrapper.toString()));
				System.out.println("msg: " + data);
				if(wrapper != null) {
					this.receiveMessageHelper(data, wrapper);
				} else {
					FutureWrapper<? extends ResponseBase> futureWrapper = this.responseFutures.remove(messageId);
					if(futureWrapper != null) {
						this.processResponse(this.gson.fromJson(data, futureWrapper.getResponseType()), futureWrapper.getFuture());
					}
				}
				
			} else if (jsonObject.has("update-type")) {
				System.out.println("event: " + data);
				this.processEvent(data, jsonObject.get("update-type").getAsString());
			}
		}
	}
	
	/**
	 * helper method for {@link #receiveMessage(String)}, capturing the wildcard so we can use it
	 * normally. big mental dump about why i did it this way in comment higher up in class
	 * @param <T> the actual type of the given response
	 * @param data the json string to be built into the response object
	 * @param wrapper the container for the response type and callback
	 */
	private <T extends ResponseBase> void receiveMessageHelper(String data, CallbackWrapper<T> wrapper) {
		T response = this.gson.fromJson(data, wrapper.getResponseType());
		this.processResponse(response, wrapper.getCallback());
	}
	
	<T extends ResponseBase> void prepareForMessage(String messageId, Class<T> responseType) {
		this.prepareForMessage(messageId, responseType, null);
	}
	
	<T extends ResponseBase> void prepareForMessage(String messageId, Class<T> responseType, Consumer<T> callback) {
		this.responseCallbacks.put(messageId, new CallbackWrapper<>(responseType, callback));
	}
	
	<T extends ResponseBase> void prepareForMessageAsFuture(String messageId, Class<T> responseType, CompletableFuture<?> future) {
		this.responseFutures.put(messageId, new FutureWrapper<T>(responseType, future));
	}
	
	void stopPreparingForMessage(String messageId) {
		if(this.responseCallbacks.remove(messageId) == null) {
			this.responseFutures.remove(messageId);
		}
	}
	
	<T extends EventBase> void registerEventListener(ObsEventListener<T> listener) {
		this.eventHandler.registerEventListener(listener);
	}
	
	private <T extends ResponseBase> void processResponse(T response, Consumer<T> callback) {
		switch(response.getClass().getSimpleName()) {
			case "GetAuthRequiredResponse":
				GetAuthRequiredResponse authRequiredResponse = (GetAuthRequiredResponse) response;
				if(authRequiredResponse.isAuthRequired())
					this.client.authenticate(authRequiredResponse.getChallenge(), authRequiredResponse.getSalt());
				else 
					this.client.initialize();
				break;
			case "AuthenticateResponse":
				AuthenticateResponse authResponse = (AuthenticateResponse) response;
				if(authResponse.getStatus().equals("ok")) 
					this.client.initialize();
				else
					throw new RuntimeException("authentication failed! unable to connect");
				//intentionally fall through in case of successful authenticate callback
			default:
				if(callback != null) callback.accept(response);
				break;
		}
	}
	
	/*
	 * TODO doc the suppressed warning
	 */
	@SuppressWarnings("unchecked")
	private <T extends ResponseBase> void processResponse(T response, CompletableFuture<?> future) {
		switch(response.getClass().getSimpleName()) {
			case "GetStudioModeStatusResponse":
				GetStudioModeStatusResponse statusResponse = (GetStudioModeStatusResponse) response;
				CompletableFuture<Boolean> statusFuture = (CompletableFuture<Boolean>)future;
				statusFuture.complete(statusResponse.isStudioMode());
				break;
			case "GetSceneItemListResponse":
				GetSceneItemListResponse itemListResponse = (GetSceneItemListResponse) response;
				CompletableFuture<List<SimpleSceneItem>> itemListFuture = (CompletableFuture<List<SimpleSceneItem>>) future;
				itemListFuture.complete(itemListResponse.getSceneItems());
				break;
			case "GetMediaStateResponse":
				GetMediaStateResponse mediaStateResponse = (GetMediaStateResponse) response;
				CompletableFuture<String> mediaStateFuture = (CompletableFuture<String>) future;
				mediaStateFuture.complete(mediaStateResponse.getMediaState());
				break;
			case "GetVideoInfoResponse":
				GetVideoInfoResponse videoInfoResponse = (GetVideoInfoResponse) response;
				CompletableFuture<GetVideoInfoResponse> videoInfoFuture = (CompletableFuture<GetVideoInfoResponse>) future;
				videoInfoFuture.complete(videoInfoResponse);
				break;
			default:
				if(future != null) {
					future.completeExceptionally(new IllegalArgumentException("can't use futures with response type '"
							+ response.getClass().getSimpleName() + "'"));
				}
		}
	}
	
	private void processEvent(String data, String eventType) {
		Optional<EventType> possibleType = EventType.tryValueOf(eventType);
		possibleType.ifPresent(type -> {
			Class<? extends EventBase> eventClass = EventType.toEventClass(type);
			this.eventHandler.handleEvent(this.gson.fromJson(data, eventClass));
		});
	}
	
	void printStoredCallbacks() {
		this.responseCallbacks.forEach((id, wrapper) -> {
			System.out.println("message id " + id + ": " + wrapper);
		});
	}
}
