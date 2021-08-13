package my.cute.channelpoints.obs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import my.cute.channelpoints.obs.events.EventBase;
import my.cute.channelpoints.obs.events.MediaStartedEvent;
import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;
import my.cute.channelpoints.obs.requests.ResponseBase;
import my.cute.channelpoints.obs.requests.authenticate.AuthenticateRequest;
import my.cute.channelpoints.obs.requests.authenticate.AuthenticateResponse;
import my.cute.channelpoints.obs.requests.createsource.CreateSourceRequest;
import my.cute.channelpoints.obs.requests.createsource.CreateSourceResponse;
import my.cute.channelpoints.obs.requests.deletesceneitem.DeleteSceneItemRequest;
import my.cute.channelpoints.obs.requests.deletesceneitem.DeleteSceneItemResponse;
import my.cute.channelpoints.obs.requests.getauthrequired.GetAuthRequiredRequest;
import my.cute.channelpoints.obs.requests.getauthrequired.GetAuthRequiredResponse;
import my.cute.channelpoints.obs.requests.getcurrentscene.GetCurrentSceneRequest;
import my.cute.channelpoints.obs.requests.getcurrentscene.GetCurrentSceneResponse;
import my.cute.channelpoints.obs.requests.getmediasourceslist.GetMediaSourcesListRequest;
import my.cute.channelpoints.obs.requests.getmediasourceslist.GetMediaSourcesListResponse;
import my.cute.channelpoints.obs.requests.getsceneitemlist.GetSceneItemListRequest;
import my.cute.channelpoints.obs.requests.getsceneitemlist.GetSceneItemListResponse;
import my.cute.channelpoints.obs.requests.getsourcesettings.GetSourceSettingsRequest;
import my.cute.channelpoints.obs.requests.getsourcesettings.GetSourceSettingsResponse;
import my.cute.channelpoints.obs.requests.getstudiomodestatus.GetStudioModeStatusRequest;
import my.cute.channelpoints.obs.requests.getstudiomodestatus.GetStudioModeStatusResponse;
import my.cute.channelpoints.obs.requests.nextmedia.NextMediaRequest;
import my.cute.channelpoints.obs.requests.nextmedia.NextMediaResponse;
import my.cute.channelpoints.obs.requests.playpausemedia.PlayPauseMediaRequest;
import my.cute.channelpoints.obs.requests.playpausemedia.PlayPauseMediaResponse;
import my.cute.channelpoints.obs.requests.restartmedia.RestartMediaRequest;
import my.cute.channelpoints.obs.requests.restartmedia.RestartMediaResponse;
import my.cute.channelpoints.obs.requests.setmediatime.SetMediaTimeRequest;
import my.cute.channelpoints.obs.requests.setmediatime.SetMediaTimeResponse;
import my.cute.channelpoints.obs.requests.setsceneitemproperties.SetSceneItemPropertiesRequest;
import my.cute.channelpoints.obs.requests.setsceneitemproperties.SetSceneItemPropertiesResponse;
import my.cute.channelpoints.obs.requests.setvolume.SetVolumeRequest;
import my.cute.channelpoints.obs.requests.setvolume.SetVolumeResponse;
import my.cute.channelpoints.obs.requests.transitiontoprogram.TransitionToProgramRequest;
import my.cute.channelpoints.obs.requests.transitiontoprogram.TransitionToProgramResponse;

public class OBSWebSocketClient {
	
	private static final transient Logger log = LoggerFactory.getLogger(OBSWebSocketClient.class);
	private static final double DEFAULT_VIDEO_VOLUME = -3.6;

	private final WebSocket socket;
	private final Gson gson;
	private final MessageReceiver receiver;
	private final String password;
	private boolean isConnected = false;
	private final Queue<RequestContainer<? extends ResponseBase>> queuedRequests;
	private final ScheduledExecutorService executor;
	
	private final AtomicReference<String> currentSceneName = new AtomicReference<>();
	private final AtomicBoolean studioMode = new AtomicBoolean();

	public OBSWebSocketClient(ScheduledExecutorService executor, String password) throws InterruptedException, ExecutionException {
		this.receiver = new MessageReceiver(this);
		this.socket = HttpClient.newHttpClient()
				.newWebSocketBuilder()
				.buildAsync(URI.create("ws://localhost:4444"), new MyListener(this.receiver, executor))
				.get();
		this.gson = new Gson();
		this.password = password;
		//TODO wrap this in a synchronized thing?
		this.queuedRequests = new ArrayDeque<>(4);
		this.executor = executor;
	}


	public void connect() {
		System.out.println("start connect");
		this.sendMessage(new GetAuthRequiredRequest(), GetAuthRequiredResponse.class);
	}
	
	public void authenticate(String challenge, String salt) {
		System.out.println("start authenticate");
		try {
			this.sendMessage(new AuthenticateRequest(this.generateAuthResponse(challenge, salt)), AuthenticateResponse.class, (response) -> {
				if(response.getStatus().equals("ok")) {
					this.setConnectionStatus(true);
					this.sendQueuedRequests();
				} else {
					log.error("authentication failed!");
				}
			});
		} catch (NoSuchAlgorithmException e) {
			//will never happen unless SHA-256 is changed to not be valid algorithm for MessageDigest.getInstance(String)
			throw new AssertionError(e);
		}	
	}
	
	public <T extends EventBase> void registerEventListener(Class<T> eventClass, Function<T, Boolean> action) {
		this.receiver.registerEventListener(ObsEventListener.createEventListener(eventClass, action));
	}
	
	public void registerEventListener(ObsEventListener<? extends EventBase> listener) {
		this.receiver.registerEventListener(listener);
	}
	
	public void retrieveSceneItemList(Consumer<GetSceneItemListResponse> callback) {
		this.sendMessage(new GetSceneItemListRequest(), GetSceneItemListResponse.class, callback);
	}
	
	public void retrieveSourceSettings(String sourceName, Consumer<GetSourceSettingsResponse> callback) {
		this.sendMessage(new GetSourceSettingsRequest(sourceName), GetSourceSettingsResponse.class, callback);
	}
	
	public void retrieveCurrentScene(Consumer<GetCurrentSceneResponse> callback) {
		this.sendMessage(new GetCurrentSceneRequest(), GetCurrentSceneResponse.class, callback);
	}
	
	public void retrieveMediaSourceList(Consumer<GetMediaSourcesListResponse> callback) {
		this.sendMessage(new GetMediaSourcesListRequest(), GetMediaSourcesListResponse.class, callback);
	}
	
	public void retrieveStudioModeStatus(Consumer<GetStudioModeStatusResponse> callback) {
		this.sendMessage(new GetStudioModeStatusRequest(), GetStudioModeStatusResponse.class, callback);
	}
	
	public CompletableFuture<Boolean> retrieveStudioModeStatus() {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		this.sendMessageWithFuture(new GetStudioModeStatusRequest(), GetStudioModeStatusResponse.class, future);
		return future;
	}
	
	/**
	 * create a new source from an image and adds it to the given scene as a visible scene item
	 * @param sourceName the name to give to the new source
	 * @param imagePath the path to the image to use
	 * @param sceneName the name of the scene to add the source to
	 * @param callback any action to take with the item id for the newly created scene item for 
	 * the new source
	 */
	public void createSourceImage(String sourceName, Path imagePath, String sceneName, Consumer<CreateSourceResponse> callback) {
		Map<String, Object> sourceSettings = new HashMap<>(3);
		sourceSettings.put("file", imagePath.toAbsolutePath().toString());
		this.sendMessage(new CreateSourceRequest(sourceName, "image_source", sceneName, sourceSettings), 
				CreateSourceResponse.class, callback);
	}
	
	/**
	 * see {@link #createSourceImage(String, Path, String, Consumer)}, except with no callback action
	 * @param sourceName
	 * @param imagePath
	 * @param sceneName
	 */
	public void createSourceImage(String sourceName, Path imagePath, String sceneName) {
		this.createSourceImage(sourceName, imagePath, sceneName, null);
	}
	
	/**
	 * see {@link #createSourceImage(String, Path, String, Consumer)}, except using the current scene 
	 * instead of specifying a scene to use
	 * @param sourceName
	 * @param imagePath
	 * @param callback
	 */
	public void createSourceImage(String sourceName, Path imagePath, Consumer<CreateSourceResponse> callback) {
		String sceneName = this.getCurrentSceneName();
		if(sceneName == null) {
			/*
			 * request is made to set current scene name once authentication finishes, but it's possible for
			 * this method to be called before that request resolves, in which case scene name is null. we
			 * just make another request for scene name, and use that (1 extra request is irrelevant. also
			 * we don't need to set scene name as part of this response bc like i said a request is already
			 * being made that will set it)
			 */
			this.retrieveCurrentScene(response -> {
				this.createSourceImage(sourceName, imagePath, response.getName(), callback);
			});
		} else {
			this.createSourceImage(sourceName, imagePath, sceneName, callback);
		}
	}
	
	/**
	 * see {@link #createSourceImage(String, Path, String, Consumer)}, except with no callback action,
	 * and using the current scene
	 * @param sourceName
	 * @param imagePath
	 */
	public void createSourceImage(String sourceName, Path imagePath) {
		this.createSourceImage(sourceName, imagePath, (Consumer<CreateSourceResponse>)null);
	}
	
	public void createSourceNetworkVideo(String sourceName, String location) {
		this.createSourceNetworkVideo(sourceName, location, 0);
	}
	
	public void createSourceNetworkVideo(String sourceName, String location, int timestamp) {
		this.createSourceNetworkVideo(sourceName, location, this.getCurrentSceneName(), timestamp, null);
	}
	
	public void createSourceNetworkVideo(String sourceName, String location, String sceneName,
			int timestamp, Consumer<CreateSourceResponse> callback) {
		Objects.requireNonNull(sourceName, "source name may not be null");
		Objects.requireNonNull(location, "location may not be null");
		Objects.requireNonNull(sceneName, "scene name may not be null");
		
		Map<String, Object> sourceSettings = new HashMap<>(5);
		List<Map<String, Object>> playlist = new ArrayList<>(3);
		Map<String, Object> videoSettings = new HashMap<>(5);
		videoSettings.put("hidden", false);
		videoSettings.put("selected", false);
		videoSettings.put("value", location);
		playlist.add(videoSettings);
		sourceSettings.put("loop", false);
		sourceSettings.put("network_caching", 1000.0);
		sourceSettings.put("playback_behavior", "always_play");
		sourceSettings.put("playlist", playlist);
		sourceSettings.put("shuffle", false);
		sourceSettings.put("track", 1.0);
		boolean startVisible = true;
		
		/*
		 * TODO
		 * want to try a small delay after mediastarted to see if that fixes black screen on
		 * video load (eg if its due to buffering). so when playing video, need to
		 * 
		 * 0. create source, set it to always play, set visibility off
		 * 1. change volume
		 * 2. if its youtube video, make nextmediarequest to trigger mediastarted
		 * 3. on mediastarted,  
		 * 		if timestamp exists, setmediatime
		 * 		after setmediatime or if timestamp didnt exist, delay for some time
		 * 		after delay, setvisible
		 * 		after visibility change, commit and do original callback on source creation? (or do this earlier?)
		 */
		
		//new TODO
		this.registerEventListener(MediaStartedEvent.class, event -> {
			if(event.getSourceName().equals(sourceName)) {
				if(timestamp > 0) {
					this.setMediaTime(sourceName, timestamp, mediaTimeResponse -> {
						this.schedule(() -> {
							this.setVisible(sourceName, true, null);
						}, 1500, TimeUnit.MILLISECONDS);
					});
				}
				return true;
			} else {
				return false;
			}
		});
		
		//old
		if(timestamp > 0) {
			startVisible = false;
			this.registerEventListener(MediaStartedEvent.class, event -> {
				if(event.getSourceName().equals(sourceName)) {
					this.setMediaTime(sourceName, timestamp, response -> {
						this.setVisible(sourceName, true, visibilityResponse -> {
							this.commit();
						});
					});
					return true;
				} else {
					return false;
				}
			});
		}
		this.sendMessage(new CreateSourceRequest(sourceName, "vlc_source", sceneName, sourceSettings, startVisible), 
				CreateSourceResponse.class, response -> {
					this.setVolume(sourceName, DEFAULT_VIDEO_VOLUME);
					if(callback != null) callback.accept(response);
				});
	}
	
	/*
	 * TODO test
	 * modified version of above to use future instead of chained callbacks + logic to add
	 * delay.
	 * returns future with created source item id. future should complete once video is buffered
	 * and begins playing
	 */
	public CompletableFuture<Integer> createSourceNetworkVideoAsFuture(String sourceName, String location, String sceneName,
			int timestamp, Consumer<CreateSourceResponse> callbackOnCreation) {
		Objects.requireNonNull(sourceName, "source name may not be null");
		Objects.requireNonNull(location, "location may not be null");
		Objects.requireNonNull(sceneName, "scene name may not be null");
		
		Map<String, Object> sourceSettings = new HashMap<>(5);
		List<Map<String, Object>> playlist = new ArrayList<>(3);
		Map<String, Object> videoSettings = new HashMap<>(5);
		videoSettings.put("hidden", false);
		videoSettings.put("selected", false);
		videoSettings.put("value", location);
		playlist.add(videoSettings);
		sourceSettings.put("loop", false);
		sourceSettings.put("network_caching", 4000.0);
		sourceSettings.put("playback_behavior", "pause_unpause");
		sourceSettings.put("playlist", playlist);
		sourceSettings.put("shuffle", false);
		sourceSettings.put("track", 1.0);
		
		/*
		 * TODO
		 * want to try a small delay after mediastarted to see if that fixes black screen on
		 * video load (eg if its due to buffering). so when playing video, need to
		 * 
		 * 0. create source, set it to always play, set visibility off
		 * 1. change volume
		 * 2. if its youtube video, make nextmediarequest to trigger mediastarted
		 * 3. on mediastarted,  
		 * 		if timestamp exists, setmediatime
		 * 		after setmediatime or if timestamp didnt exist, pause
		 * 		delay for some time
		 * 		after delay, setvisible
		 * 		after visibility change, play
		 * 		after play, commit and do original callback on source creation? (or do this earlier?)
		 */
		
		AtomicInteger createdSourceItemId = new AtomicInteger(-1);
		CompletableFuture<Integer> future = new CompletableFuture<>();
		this.registerEventListener(MediaStartedEvent.class, event -> {
			if(event.getSourceName().equals(sourceName)) {
				if(timestamp > 0) {
					this.setMediaTime(sourceName, timestamp, mediaTimeResponse -> {
						this.bufferThenPlayVideoSource(sourceName, createdSourceItemId, future);
					});
				} else {
					this.bufferThenPlayVideoSource(sourceName, createdSourceItemId, future);
				}
				return true;
			} else {
				return false;
			}
		});
		this.sendMessage(new CreateSourceRequest(sourceName, "vlc_source", sceneName, sourceSettings, true), 
				CreateSourceResponse.class, response -> {
					this.setVolume(sourceName, DEFAULT_VIDEO_VOLUME);
					createdSourceItemId.set(response.getItemId());
					if(callbackOnCreation != null) callbackOnCreation.accept(response);
					this.commit();
				});
		return future;
	}
	
	private void bufferThenPlayVideoSource(String sourceName, AtomicInteger sourceItemId, 
			CompletableFuture<Integer> futureItemIdTask) {
		this.setVisible(sourceName, false, pauseResponse -> {
			this.commit();
			this.schedule(() -> {
				this.setVisible(sourceName, true, visibilityResponse -> {
					this.commit();
					int itemId = sourceItemId.get();
					if(itemId != -1) {
						futureItemIdTask.complete(itemId);
					} else {
						futureItemIdTask.completeExceptionally(new 
								IllegalArgumentException("no item id generated for the created source '"
										+ sourceName + "'"));
					}
				});
			}, 6000, TimeUnit.MILLISECONDS);
		});
	}
	
//	/**
//	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, Consumer)},
//	 * with default parameters timestamp 0s, current scene, no callback 
//	 * @param sourceName
//	 * @param location
//	 */
//	public void createAndPlayYoutubeVideo(String sourceName, String location) {
//		this.createAndPlayYoutubeVideo(sourceName, location, null);
//	}
	
	public CreateAndPlayYoutubeVideoHolder createAndPlayYoutubeVideo(String sourceName, String location) {
		return new CreateAndPlayYoutubeVideoHolder(sourceName, location);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, Consumer)},
	 * with default parameters current scene, no callback
	 * @param sourceName
	 * @param location
	 * @param timestamp
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, int timestamp) {
		this.createAndPlayYoutubeVideo(sourceName, location, timestamp, null);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, Consumer)},
	 * with default parameters timestamp 0s, current scene
	 * @param sourceName
	 * @param location
	 * @param callback
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, 
			Consumer<CreateSourceResponse> callback) {
		this.createAndPlayYoutubeVideo(sourceName, location, 0, callback);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, Consumer)},
	 * with default parameter current scene
	 * @param sourceName
	 * @param location
	 * @param timestamp
	 * @param callback
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, int timestamp,
			Consumer<CreateSourceResponse> callback) {
		this.createAndPlayYoutubeVideo(sourceName, location, this.getCurrentSceneName(), timestamp, callback);
	}
	
	/*
	 * TODO transformation
	 */
	/**
	 * convenience method to create a new source using the given youtube direct video url and
	 * add it to the given scene (as in {@link #createSourceNetworkVideo(String, String, String, int, Consumer)})
	 * and then also immediately begin playback of the new video source. if obs is currently in
	 * studio mode (as determined by {@link #isStudioMode()}), the preview scene will 
	 * automatically be transitioned to program, so that playing the video will immediately
	 * be reflected on the stream<p>
	 * note: specifically, this creates a new vlc video source from the given location, adds
	 * it to the given scene as a sceneitem, advances the vlc playlist one item, then begins
	 * playing that item. this might work in general for all (or some) network videos, but
	 * the playlist process seems kind of specific and is done with the sole intent of 
	 * properly playing youtube videos, so this method is named as such. technically speaking,
	 * the given video url doesn't need to be from youtube, but if it isn't, this method may
	 * or may not work
	 * @param sourceName the name of the new video source to create
	 * @param location the url of the network video to add as a source
	 * @param sceneName the name of the scene to add the new source to
	 * @param timestamp the timestamp of the video to start playback from, in seconds
	 * @param callback action to be taken once the new source has been created and playback
	 * has started
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, String sceneName,
			int timestamp, Consumer<CreateSourceResponse> callback) {

		/*
		 * TODO seeing if i need playPauseMedia request or if itll play by itself
		 */
//		Consumer<CreateSourceResponse> modifiedCallback = response -> {
//			this.nextMedia(sourceName, nextMediaResponse -> {
//				this.playPauseMedia(sourceName, true, playResponse -> {
//					callback.accept(response);
//				});
//			});
//		};
		Consumer<CreateSourceResponse> modifiedCallback = response -> {
			this.nextMedia(sourceName, nextMediaResponse -> {
				callback.accept(response);
			});
		};
		this.createSourceNetworkVideo(sourceName, location, sceneName, timestamp, modifiedCallback);
	}
	
	public CompletableFuture<Integer> createAndPlayYoutubeVideo(String sourceName, String location, String sceneName,
			int timestamp) {
		return this.createSourceNetworkVideoAsFuture(sourceName, location, sceneName, timestamp, response -> {
			this.nextMedia(sourceName, null);
		});
	}
	
	CompletableFuture<Integer> createAndPlayYoutubeVideo(CreateAndPlayYoutubeVideoHolder holder) {
		return this.createAndPlayYoutubeVideo(holder.getSourceName(), holder.getLocation(), holder.getSceneName(),
				holder.getTimestamp());
	}
	
	/**
	 * play or pause the given media source in the current scene
	 * @param sourceName the name of the media source to play or pause
	 * @param play true to play the media, false to pause the media
	 * @param callback
	 */
	public void playPauseMedia(String sourceName, boolean play, Consumer<PlayPauseMediaResponse> callback) {
		/*
		 * note the obs websocket api uses false to play media and true to pause media. i've chosen
		 * to use the opposite because it seems more intuitive to me, so we flip the provided 
		 * parameter when creating the request
		 */
		this.sendMessage(new PlayPauseMediaRequest(sourceName, !play), PlayPauseMediaResponse.class, callback);
	}
	
	/**
	 * skips to the next media item in the playlist
	 * @param sourceName the name of a vlc source with a playlist
	 * @param callback
	 */
	public void nextMedia(String sourceName, Consumer<NextMediaResponse> callback) {
		this.sendMessage(new NextMediaRequest(sourceName), NextMediaResponse.class, callback);
	}
	
	public void restartMedia(String sourceName, Consumer<RestartMediaResponse> callback) {
		this.sendMessage(new RestartMediaRequest(sourceName), RestartMediaResponse.class, callback);
	}
	
	public void setMediaTime(String sourceName, int timestamp) {
		this.setMediaTime(sourceName, timestamp, null);
	}
	
	/**
	 * sets the current time of the given media source to the given timestamp
	 * @param sourceName the name of the media source to adjust time for
	 * @param timestamp the time to set the media source to, in seconds
	 */
	public void setMediaTime(String sourceName, int timestamp, Consumer<SetMediaTimeResponse> callback) {
		this.sendMessage(new SetMediaTimeRequest(sourceName, timestamp * 1000), SetMediaTimeResponse.class, callback);
		System.out.println("sent setmediatimerequest: " + sourceName + ", " + timestamp);
	}
	
	public void setVolume(String sourceName, double volume) {
		this.setVolume(sourceName, volume, null);
	}
	public void setVolume(String sourceName, double volume, Consumer<SetVolumeResponse> callback) {
		this.sendMessage(new SetVolumeRequest(sourceName, volume), SetVolumeResponse.class, callback);
	}
	
	public void setVisible(String sourceName, boolean visible) {
		this.setVisible(sourceName, visible, null);
	}
	
	public void setVisible(String sourceName, boolean visible, Consumer<SetSceneItemPropertiesResponse> callback) {
		this.sendMessage(new SetSceneItemPropertiesRequest.Builder().sceneItemName(sourceName).visible(visible).build(), 
				SetSceneItemPropertiesResponse.class, callback);
	}
	
	public void deleteSceneItem(int itemId) {
		this.deleteSceneItem(null, itemId);
	}
	
	public void deleteSceneItem(String sceneName, int itemId) {
		this.deleteSceneItem(sceneName, null, itemId);
	}
	
	public void deleteSceneItem(String sceneName, String itemName, int itemId) {
		this.deleteSceneItem(sceneName, itemName, itemId, null);
	}
	
	public void deleteSceneItem(String sceneName, String itemName, int itemId, Consumer<DeleteSceneItemResponse> callback) {
		this.sendMessage(new DeleteSceneItemRequest(sceneName, itemName, itemId), DeleteSceneItemResponse.class, 
				response -> {
					this.commit();
					if(callback != null) callback.accept(response);
				});
	}
	
	public void transitionToProgram() {
		this.transitionToProgram(null);
	}
	
	/**
	 * transitions between preview and program views in obs, assuming studio mode is enabled.
	 * i think an error response happens if studio mode is not enabled?
	 * @param callback
	 */
	public void transitionToProgram(Consumer<TransitionToProgramResponse> callback) {
		this.sendMessage(new TransitionToProgramRequest(), TransitionToProgramResponse.class, callback);
	}
	
	public void transitionToProgram(String transition, int duration) {
		this.transitionToProgram(transition, duration, null);
	}
	
	public void transitionToProgram(String transition, int duration, Consumer<TransitionToProgramResponse> callback) {
		this.sendMessage(new TransitionToProgramRequest(transition, duration), TransitionToProgramResponse.class, callback);
	}
	
	public void commit() {
		if(this.isStudioMode()) {
			this.transitionToProgram();
		}
	}

	private void sendMessage(RequestBase request, Class<? extends ResponseBase> responseType) {
		this.sendMessage(request, responseType, null);
	}
	
	private synchronized <T extends ResponseBase> void sendMessage(RequestBase request, Class<T> responseType, Consumer<T> callback) {
		if(this.isConnected || request.getRequestType() == RequestType.GetAuthRequired || request.getRequestType() == RequestType.Authenticate) {
			this.receiver.prepareForMessage(request.getMessageId(), responseType, callback);
			this.socket.sendText(this.gson.toJson(request), true)
				.whenComplete((socket, throwable) -> {
					if(throwable != null) {
						this.receiver.stopPreparingForMessage(request.getMessageId());
						throwable.printStackTrace();
					}
				});
		} else {
			this.queuedRequests.add(new RequestContainer<T>(request, responseType, callback));
		}
	}
	
	private synchronized <T extends ResponseBase> void sendMessageWithFuture(RequestBase request, Class<T> responseType, 
			CompletableFuture<?> future) {
		//note getauthrequired, authenticate requests aren't done with futures currently, so only check if connected
		if(this.isConnected) {
			this.receiver.prepareForMessageAsFuture(request.getMessageId(), responseType, future);
			this.socket.sendText(this.gson.toJson(request), true)
				.whenComplete((socket, throwable) -> {
					if(throwable != null)
						this.receiver.stopPreparingForMessage(request.getMessageId());
				});
		} else {
			this.queuedRequests.add(new RequestContainer<T>(request, responseType, future));
		}
	}
	
	void setConnectionStatus(boolean status) {
		this.isConnected = status;
	}
	
	/*
	 * TODO consider doing...something about the possibility of this being null
	 * if it's called too quickly
	 */
	/**
	 * returns the name of the current scene. this value is automatically set by a
	 * call to {@link #initialize()} when the connection to obs is established, and
	 * is updated by an event listener whenever the current scene is switched. note
	 * that if a call to this method is made before the initial request is completed,
	 * null will be returned
	 * @return the name of the currently active scene. may be null if this method is
	 * called immediately, before the initial obs connection completes
	 */
	public String getCurrentSceneName() {
		return this.currentSceneName.get();
	}
	
	void setCurrentSceneName(String name) {
		this.currentSceneName.set(name);
	}
	
	void initCurrentSceneName(String name) {
		this.currentSceneName.compareAndSet(null, name);
	}
	
	/*
	 * what does this return if i call it too quickly? false?
	 */
	public boolean isStudioMode() {
		return this.studioMode.get();
	}
	
	public void schedule(Runnable action, long delay, TimeUnit unit) {
		this.executor.schedule(action, delay, unit);
	}
	
	public void shutdown() {
		this.executor.shutdown();
	}
	
	void setStudioMode(boolean studioMode) {
		this.studioMode.set(studioMode);
	}
	
	void initStudioMode(boolean studioMode) {
		this.studioMode.compareAndSet(false, studioMode);
	}
	
	/**
	 * initializes commonly used values so a request doesn't need to be made every time they're
	 * required
	 */
	void initialize() {
		this.retrieveCurrentScene(sceneResponse -> this.initCurrentSceneName(sceneResponse.getName()));
		this.retrieveStudioModeStatus().thenAccept(status -> this.initStudioMode(status));
	}
	
	public void printStoredCallbacks() {
		this.receiver.printStoredCallbacks();
	}
	
	private void sendQueuedRequests() {
		int queueSize = this.queuedRequests.size();
		for(int i=0; i < queueSize; i++) {
			this.sendMessage(this.queuedRequests.remove());
		}
	}
	
	private <T extends ResponseBase> void sendMessage(RequestContainer<T> requestContainer) {
		if(requestContainer.getCallback() != null) 
			this.sendMessage(requestContainer.getRequest(), requestContainer.getResponseType(), requestContainer.getCallback());
		else
			this.sendMessageWithFuture(requestContainer.getRequest(), requestContainer.getResponseType(), requestContainer.getFuture());
	}
	
	private String generateAuthResponse(String challenge, String salt) throws NoSuchAlgorithmException {
		MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
		sha256Digest.update((this.password + salt).getBytes(StandardCharsets.UTF_8));
		String secret = Base64.getEncoder().encodeToString(sha256Digest.digest());
		sha256Digest.update((secret + challenge).getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(sha256Digest.digest());
	}
	
	private static final class RequestContainer<T extends ResponseBase> {
		private final RequestBase request;
		private final Class<T> responseType;
		private final Consumer<T> callback;
		private final CompletableFuture<?> future;
		
		private RequestContainer(RequestBase request, Class<T> responseType, Consumer<T> callback) {
			this.request = request;
			this.responseType = responseType;
			this.callback = callback;
			this.future = null;
		}
		
		private RequestContainer(RequestBase request, Class<T> responseType, CompletableFuture<?> future) {
			this.request = request;
			this.responseType = responseType;
			this.callback = null;
			this.future = future;
		}
		
		public RequestBase getRequest() {
			return this.request;
		}

		public Class<T> getResponseType() {
			return this.responseType;
		}

		public Consumer<T> getCallback() {
			return this.callback;
		}
		
		public CompletableFuture<?> getFuture() {
			return this.future;
		}
	}
	
	public final class CreateAndPlayYoutubeVideoHolder {
		private final String sourceName;
		private final String location;
		private String sceneName;
		private int timestamp;
		private Consumer<CreateSourceResponse> callback;
		
		private CreateAndPlayYoutubeVideoHolder(String sourceName, String location) {
			this.sourceName = sourceName;
			this.location = location;
			this.sceneName = getCurrentSceneName();
		}
		
		public CreateAndPlayYoutubeVideoHolder sceneName(String sceneName) {
			this.sceneName = sceneName;
			return this;
		}
		
		public CreateAndPlayYoutubeVideoHolder timestamp(int timestamp) {
			this.timestamp = timestamp;
			return this;
		}
		
		public CreateAndPlayYoutubeVideoHolder callback(Consumer<CreateSourceResponse> callback) {
			this.callback = callback;
			return this;
		}
		
		public CompletableFuture<Integer> send() {
			return createAndPlayYoutubeVideo(this);
		}

		String getSourceName() {
			return this.sourceName;
		}

		String getLocation() {
			return this.location;
		}

		String getSceneName() {
			return this.sceneName;
		}

		int getTimestamp() {
			return this.timestamp;
		}

		Consumer<CreateSourceResponse> getCallback() {
			return this.callback;
		}
	}
}
