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

import my.cute.channelpoints.misc.ConditionalExecutorService;
import my.cute.channelpoints.obs.events.EventBase;
import my.cute.channelpoints.obs.events.MediaStartedEvent;
import my.cute.channelpoints.obs.events.SceneItemTransformChangedEvent;
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
import my.cute.channelpoints.obs.requests.getmediastate.GetMediaStateRequest;
import my.cute.channelpoints.obs.requests.getmediastate.GetMediaStateResponse;
import my.cute.channelpoints.obs.requests.getsceneitemlist.GetSceneItemListRequest;
import my.cute.channelpoints.obs.requests.getsceneitemlist.GetSceneItemListResponse;
import my.cute.channelpoints.obs.requests.getsourcesettings.GetSourceSettingsRequest;
import my.cute.channelpoints.obs.requests.getsourcesettings.GetSourceSettingsResponse;
import my.cute.channelpoints.obs.requests.getstudiomodestatus.GetStudioModeStatusRequest;
import my.cute.channelpoints.obs.requests.getstudiomodestatus.GetStudioModeStatusResponse;
import my.cute.channelpoints.obs.requests.getvideoinfo.GetVideoInfoRequest;
import my.cute.channelpoints.obs.requests.getvideoinfo.GetVideoInfoResponse;
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
	
	/*
	 * TODO
	 * queue for adding video sources? or should that be managed externally?
	 */
	
	private static final transient Logger log = LoggerFactory.getLogger(OBSWebSocketClient.class);
	private static final double DEFAULT_VIDEO_VOLUME = -3.6;

	private final WebSocket socket;
	private final Gson gson;
	private final MessageReceiver receiver;
	private final String password;
	private boolean isConnected = false;
	private final Queue<RequestContainer<? extends ResponseBase>> queuedRequests;
	//TODO remove?
	private final ConditionalExecutorService executor;
	//time before a new video source is made visible, in milliseconds. intended to combat buffering by some amount
	private final int videoBufferingDelay;
	
	private final AtomicReference<String> currentSceneName = new AtomicReference<>();
	private final AtomicBoolean studioMode = new AtomicBoolean();
	private final AtomicInteger canvasHeight = new AtomicInteger();
	private final AtomicInteger canvasWidth = new AtomicInteger();

	public OBSWebSocketClient(ScheduledExecutorService executor, String password, int videoBufferingDelay) 
			throws InterruptedException, ExecutionException {
		this.receiver = new MessageReceiver(this);
		this.socket = HttpClient.newHttpClient()
				.newWebSocketBuilder()
				.buildAsync(URI.create("ws://localhost:4444"), new MyListener(this.receiver, executor))
				.get();
		this.gson = new Gson();
		this.password = password;
		//TODO wrap this in a synchronized thing?
		this.queuedRequests = new ArrayDeque<>(4);
		this.executor = new ConditionalExecutorService(executor);
		this.videoBufferingDelay = videoBufferingDelay;
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
	
	/**
	 * registers a new event listener. for any type T extends EventBase, whenever an event of type T occurs,
	 * all event listeners registered to that class will be executed. the provided function will define what
	 * action should be taken when a corresponding event occurs - ie, the provided function will be used as {@link my.cute.channelpoints.obs.ObsEventListener#accept(EventBase)}
	 * @param <T> the type of event that the new event listener should execute on
	 * @param eventClass the class corresponding to the type T
	 * @param action the action to be taken when an event of the given type occurs. if the function returns 
	 * true, the event listener will no longer be fired on any other events of this type and will be 
	 * effectively deleted, and if the function returns false, the event listener will continue to fire on 
	 * events of this type
	 */
	public <T extends EventBase> void registerEventListener(Class<T> eventClass, Function<T, Boolean> action) {
		this.receiver.registerEventListener(ObsEventListener.createEventListener(eventClass, action));
	}
	
	/**
	 * see {@link #registerEventListener(Class, Function)}. the action defined by the provided listener will
	 * be taken whenever an event of the corresponding type is fired, until its action returns true
	 * @param listener the listener to be newly registered. will continue to fire on events of its type (as
	 * specified by <code>listener.getEventClass()</code>) until its defined action returns true
	 */
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
	
	public void retrieveMediaState(String sourceName, Consumer<GetMediaStateResponse> callback) {
		this.sendMessage(new GetMediaStateRequest(sourceName), GetMediaStateResponse.class, callback);
	}
	
	public CompletableFuture<String> retrieveMediaState(String sourceName) {
		CompletableFuture<String> future = new CompletableFuture<>();
		this.sendMessageWithFuture(new GetMediaStateRequest(sourceName), GetMediaStateResponse.class, future);
		return future;
	}
	
	public void retrieveVideoInfo(Consumer<GetVideoInfoResponse> callback) {
		this.sendMessage(new GetVideoInfoRequest(), GetVideoInfoResponse.class, callback);
	}
	
	public CompletableFuture<GetVideoInfoResponse> retrieveVideoInfo() {
		CompletableFuture<GetVideoInfoResponse> future = new CompletableFuture<>();
		this.sendMessageWithFuture(new GetVideoInfoRequest(), GetVideoInfoResponse.class, future);
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
		this.createSourceNetworkVideo(sourceName, location, this.getCurrentSceneName(), timestamp);
	}
	
	public void createSourceNetworkVideo(String sourceName, String location, String sceneName, int timestamp) {
		this.createSourceNetworkVideo(sourceName, location, sceneName, timestamp, 0f, 0f);
	}
	
	public void createSourceNetworkVideo(String sourceName, String location, String sceneName, int timestamp,
			double width, double height) {
		this.createSourceNetworkVideo(sourceName, location, sceneName, timestamp, width, height, null);
	}
	/*
	 * TODO doc this + future version
	 * visibility timing basically still good - buffering problem stil lexists, bt w/e
	 * maybe consider implementing a playtime function? if we can schedule the delete 
	 * along with the visibility timing or something it might be better
	 * actually that really shouldnt matter since the future is completed at visible time
	 * same thing?
	 */
	/**
	 * creates a new video source using the provided direct network video, and adds it to the specified
	 * scene as a sceneitem with the same name. the new video sceneitem will play starting from the 
	 * provided timestamp and will be scaled according to the provided height and width. the provided
	 * callback will be triggered on sceneitem creation. the new video's volume will be set according 
	 * to {@link #DEFAULT_VIDEO_VOLUME}
	 * <p>
	 * implementation-wise, this method will register some event listeners as in 
	 * {@link #createSourceNetworkVideoSettingsAndListeners(String, String, String, int, double, double, Consumer)}
	 * <p>
	 * visibility timing: the newly created source will be created visible (a new vlc source won't begin
	 * playing until it's visible, even if set to always play regardless of visibility), then will be
	 * hidden as soon as it fires a MediaStarted event, so that some of the buffering/resizing won't be 
	 * visible in obs. after the video is scaled, a specified delay will pass (as determined by 
	 * {@link #getVideoBufferingDelay()}), and then the video will be made visible
	 * @param sourceName the name to be used for the new source and sceneitem
	 * @param location a direct link to a network video to be used for the new vlc source
	 * @param sceneName the name of the scene to add the new sceneitem to
	 * @param timestamp the time to start video playback from, in seconds (0 will play from start)
	 * @param width a number in [0.0, 1.0] representing the maximum portion of the output canvas the 
	 * newly created video source will use (eg, 0.5 to use up to half the canvas). the new source will 
	 * be scaled according to either width or height, whichever is smaller (ie, the new source will fall
	 * within the rectangle defined by width = (width) * this.getOutputWidth() and height = (height) *
	 * this.getOutputHeight() with its top left corner coinciding with the top left corner of the output
	 * canvas). if 0.0 is used, no scaling will occur (video's actual size will be used)
	 * @param height see width, except for height
	 * @param callbackOnCreation a consumer that will be triggered upon creation of the new sceneitem. 
	 * the video will not yet be playing when this is triggered
	 * @throws NullPointerException if sourceName, location, or sceneName are null
	 * @throws IllegalArgumentException if width or height are < 0.0 or > 1.0
	 */
	public void createSourceNetworkVideo(String sourceName, String location, String sceneName,
			int timestamp, double width, double height, Consumer<CreateSourceResponse> callbackOnCreation) {
		//note the use of scheduling setVisible according to the set video buffering delay
		Map<String, Object> sourceSettings = this.createSourceNetworkVideoSettingsAndListeners(sourceName, location,
				sceneName, timestamp, width, height, source -> 
				this.schedule(() -> this.setVisible(source, true), this.getVideoBufferingDelay(), TimeUnit.MILLISECONDS));
		
		this.sendMessage(new CreateSourceRequest(sourceName, "vlc_source", sceneName, sourceSettings, true), 
				CreateSourceResponse.class, response -> {
					this.setVolume(sourceName, DEFAULT_VIDEO_VOLUME);
					if(callbackOnCreation != null) callbackOnCreation.accept(response);
				});
	}
	
	/**
	 * see {@link #createSourceNetworkVideo(String, String, String, int, double, double, Consumer)},
	 * except returning a CompletableFuture that will complete with the new sceneitem's item id when
	 * the video is scaled (this should be about the same time that the new media source enters the "playing"
	 * media state, although it will probably still be buffering when the future is completed). this allows
	 * chaining of arbitrary code at a point when the new media source is basically ready, visible, and 
	 * playing (eg, could be used to schedule a delete on the new source after some time)
	 * @param sourceName
	 * @param location
	 * @param sceneName
	 * @param timestamp
	 * @param width
	 * @param height
	 * @param callbackOnCreation
	 * @return a CompletableFuture that will complete with the new sceneitem's item id after it's scaled. 
	 * strictly speaking, upon completion of the future, the media is guaranteed to have fired a MediaStarted 
	 * event, been scaled according to the provided parameters, and been made visible
	 */
	public CompletableFuture<Integer> createSourceNetworkVideoAsFuture(String sourceName, String location, String sceneName,
			int timestamp, double width, double height, Consumer<CreateSourceResponse> callbackOnCreation) {
		AtomicInteger createdSourceItemId = new AtomicInteger(-1);
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		Map<String, Object> sourceSettings = this.createSourceNetworkVideoSettingsAndListeners(sourceName, location, 
				sceneName, timestamp, width, height, source -> { 
					this.schedule(() -> {
						this.setVisible(source, true, visibleResponse -> {
							int itemId = createdSourceItemId.get();
							if(itemId != -1) {
								future.complete(itemId);
							} else {
								future.completeExceptionally(new 
										IllegalArgumentException("no item id generated for the created source '"
												+ source + "'"));
							}
						});
					}, this.getVideoBufferingDelay(), TimeUnit.MILLISECONDS);
				});
		
		//video won't start playing until it's visible
		this.sendMessage(new CreateSourceRequest(sourceName, "vlc_source", sceneName, sourceSettings, true), 
				CreateSourceResponse.class, response -> {
					this.setVolume(sourceName, DEFAULT_VIDEO_VOLUME);
					createdSourceItemId.set(response.getItemId());
					if(callbackOnCreation != null) callbackOnCreation.accept(response);
				});
		return future;
	}

	/**
	 * helper method to perform necessary checks and prepare for the creation of a new vlc video source
	 * <p>
	 * two event listeners will be registered as a result of calling this method: the first will trigger
	 * on MediaStartedEvent, and if the event corresponds to the source name used in this method call,
	 * that source will have its timestamp set if one was provided, and will also be hidden (source is 
	 * hidden during resizing/some of buffering to smooth viewing experience. the provided callback should
	 * be used to make it visible again)<br>
	 * the second will cause the source to be scaled according to the provided width and height as soon
	 * as scaling is possible, and then to trigger the provided callback
	 * @param sourceName the name of the new source/scene item
	 * @param location direct link to the network video for the new vlc source
	 * @param sceneName the name of the scene to add the new source to as a sceneitem
	 * @param timestamp the time to start playing the video from, in seconds (0 will play from the start)
	 * @param width scaling factor for the new source's size, representing the portion of the output canvas
	 * that the new source can occupy (eg 0.5 represents half of the output canvas's width). measured from
	 * the left side of the canvas. must be a number in (0.0, 1.0). if 0.0 is used, no scaling will occur
	 * @param height scaling factor for the new source's size, representing the portion of the output canvas
	 * that the new source can occupy (eg 0.5 represents half of the output canvas's height). measured from
	 * the top of the canvas. must be a number in (0.0, 1.0). if 0.0 is used, no scaling will occur
	 * @param callbackOnScale a consumer that will be triggered with the name of the new scene item
	 * when it's resized. at this point the source will be hidden and will probably be buffering; this
	 * callback should probably be used to at least make the source visible, in addition to anything else
	 * @return a map suitable for use as the sourceSettings parameter in creation of a new vlc video source,
	 * as from {@link #createDefaultSourceNetworkVideoSettings(String)}
	 * @throws NullPointerException if sourceName, location, or sceneName are null
	 * @throws IllegalArgumentException if width or height are < 0.0 or > 1.0
	 */
	private Map<String, Object> createSourceNetworkVideoSettingsAndListeners(String sourceName, String location,
			String sceneName, int timestamp, double width, double height, Consumer<String> callbackOnScale) {
		Objects.requireNonNull(sourceName, "source name may not be null");
		Objects.requireNonNull(location, "location may not be null");
		Objects.requireNonNull(sceneName, "scene name may not be null");
		
		if(width < 0.0 || width > 1.0) throw new IllegalArgumentException("width must be between 0 (inclusive) "
				+ "and 1 (inclusive)");
		if(height < 0.0 || height > 1.0) throw new IllegalArgumentException("height must be between 0 (inclusive) "
				+ "and 1 (inclusive)");
		
		Map<String, Object> sourceSettings = this.createDefaultSourceNetworkVideoSettings(location);
		
		this.registerEventListener(MediaStartedEvent.class, event -> {
			if(event.getSourceName().equals(sourceName)) {
				if(timestamp > 0) {
					this.setMediaTime(sourceName, timestamp, mediaTimeResponse -> {
						this.setVisible(sourceName, false);
					});
				} else {
					this.setVisible(sourceName, false);
				}
				return true;
			} else {
				return false;
			}
		});
		
		this.registerListenerToScaleVideoSource(sourceName, width, height, callbackOnScale);
		
		return sourceSettings;
	}
	
	/**
	 * method to scale a given video source according to some given percentage of the
	 * output canvas
	 * it takes some time (due to buffering, probably) for the new source's transform
	 * to be automatically updated to the original video's resolution, so we don't 
	 * change the transform until that happens (we don't know the actual resolution 
	 * until that happens anyway but even if we did eg by youtube-dl i think any changes
	 * we make would be overwritten once the video buffers). thus we use event listener
	 * <p>
	 * video will be scaled according to its original aspect ratio so that its width
	 * will be equal to (width) * this.getOutputWidth() or its height will be equal
	 * to (height) * this.getOutputHeight(), whichever is smaller (ie, the video will
	 * fall in the rectangle defined by width = (width) * this.getOutputWidth() and
	 * height = (height) * this.getOutputHeight() and its upper left corner in the 
	 * upper left corner of the output canvas
	 * <p>
	 * note that if 0.0 is used for either parameter, no scaling will occur (this 
	 * method will effectively do nothing)
	 * @param sourceName
	 * @param width a number between 0 (inclusive) and 1 (inclusive) representing
	 * the percentage of the output canvas's width that can be used for the video 
	 * (eg 0.5 for the video to be limited to at most half of the canvas's width).
	 * if 0.0 is used, no resizing will occur
	 * @param height a number between 0 (inclusive) and 1 (inclusive) representing
	 * the percentage of the output canvas's height that can be used for the video 
	 * (eg 0.5 for the video to be limited to at most half of the canvas's height).
	 * if 0.0 is used, no resizing will occur
	 * @param callbackOnScale a consumer that uses the given scene item name. can
	 * take any action, and will be called after scaling the source occurs
	 */
	private void registerListenerToScaleVideoSource(String sourceName, double width, double height, 
			Consumer<String> callbackOnScale) {
		this.registerEventListener(SceneItemTransformChangedEvent.class, event -> {
			if(event.getItemName().equals(sourceName) && event.getTransform().getSourceHeight() > 0
					&& event.getTransform().getSourceWidth() > 0) {
				if((width == 0.0 || height == 0.0) && callbackOnScale != null) {
					callbackOnScale.accept(sourceName);
					return true;
				}
				
				double scalingFactor = width * (double)this.getCanvasWidth() / (double)event.getTransform().getSourceWidth();
				if(scalingFactor * (double)event.getTransform().getSourceHeight() > height * (double)this.getCanvasHeight()) {
					scalingFactor = height * (double)this.getCanvasHeight() / (double)event.getTransform().getSourceHeight();
				}
				
				this.setSceneItemScale(sourceName, scalingFactor, scalingFactor, callbackOnScale);
				return true;
			} else {
				return false;
			}
		});
	}
	
	/**
	 * returns a map suitable for use as sourceSettings in the creation of a new vlc video source.
	 * notable settings are the provided video location, no looping, video always plays regardless
	 * of visibility
	 * @param location direct link to the new network video
	 * @return a map suitable for use as the sourceSettings parameter in the creation of a new vlc video source
	 */
	private Map<String, Object> createDefaultSourceNetworkVideoSettings(String location) {
		Map<String, Object> sourceSettings = new HashMap<>(5);
		List<Map<String, Object>> playlist = new ArrayList<>(3);
		Map<String, Object> videoSettings = new HashMap<>(5);
		videoSettings.put("hidden", false);
		videoSettings.put("selected", false);
		videoSettings.put("value", location);
		playlist.add(videoSettings);
		sourceSettings.put("loop", false);
		sourceSettings.put("network_caching", 4000.0);
		sourceSettings.put("playback_behavior", "always_play");
		sourceSettings.put("playlist", playlist);
		sourceSettings.put("shuffle", false);
		sourceSettings.put("track", 1.0);
		return sourceSettings;
	}
	
	//old
//	private void setMediaVisibleOnPlaying(String sourceName) {
//		this.executor.scheduleWithFixedDelayAndConditionAsync(() -> {
//			return this.getMediaState(sourceName).thenApplyAsync(mediaState -> {
//				if(mediaState.equals("playing")) {
//					this.setVisible(sourceName, true);
//					return false;
//				} else {
//					return true;
//				}
//			}, this.executor);
//		}, 0, 100, TimeUnit.MILLISECONDS);
//	}
	
	//old
//	private void setMediaVisibleOnPlaying(String sourceName, AtomicInteger sourceItemId, 
//			CompletableFuture<Integer> futureItemIdTask) {
//		this.setVisible(sourceName, false);
//		this.executor.scheduleWithFixedDelayAndConditionAsync(() -> {
//			return this.getMediaState(sourceName).thenApplyAsync(mediaState -> {
//				if(mediaState.equals("playing")) {
//					this.setVisible(sourceName, true, visibilityResponse -> {
//						int itemId = sourceItemId.get();
//						if(itemId != -1) 
//							futureItemIdTask.complete(itemId);
//						else 
//							futureItemIdTask.completeExceptionally(new IllegalArgumentException("no item "
//									+ "id generated for the created source '" + sourceName + "'"));
//					});
//					return false;
//				} else {
//					return true;
//				}
//			}, this.executor);
//		}, 0, 100, TimeUnit.MILLISECONDS);
//	}
	
	
	/*
	 * TODO
	 * might need to use a builder for this method after all
	 */
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, double, double, Consumer)},
	 * with default parameters timestamp 0s, current scene, no callback, no video resizing
	 * @param sourceName
	 * @param location
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location) {
		this.createAndPlayYoutubeVideo(sourceName, location, null);
	}
	
//	public CreateAndPlayYoutubeVideoHolder createAndPlayYoutubeVideo(String sourceName, String location) {
//		return new CreateAndPlayYoutubeVideoHolder(sourceName, location);
//	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, double, double, Consumer)},
	 * with default parameters current scene, no callback, no video resizing
	 * @param sourceName
	 * @param location
	 * @param timestamp
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, int timestamp) {
		this.createAndPlayYoutubeVideo(sourceName, location, timestamp, null);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, double, double, Consumer)},
	 * with default parameters timestamp 0s, current scene, no video resizing
	 * @param sourceName
	 * @param location
	 * @param callback
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, 
			Consumer<CreateSourceResponse> callback) {
		this.createAndPlayYoutubeVideo(sourceName, location, 0, callback);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, double, double, Consumer)},
	 * with default parameters current scene, no video resizing
	 * @param sourceName
	 * @param location
	 * @param timestamp
	 * @param callback
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, int timestamp,
			Consumer<CreateSourceResponse> callback) {
		this.createAndPlayYoutubeVideo(sourceName, location, this.getCurrentSceneName(), timestamp, callback);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, double, double, Consumer)},
	 * with default parameter no video resizing
	 * @param sourceName
	 * @param location
	 * @param sceneName
	 * @param timestamp
	 * @param callback
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, String sceneName, int timestamp, 
			Consumer<CreateSourceResponse> callback) {
		this.createAndPlayYoutubeVideo(sourceName, location, sceneName, timestamp, 0.0, 0.0, callback);
	}
	
	/**
	 * convenience method to create a new source using the given youtube direct video url and
	 * add it to the given scene (as in {@link #createSourceNetworkVideo(String, String, String, int, Consumer)})
	 * and then also immediately begin playback of the new video source. the created video source
	 * will be scaled so its width is equal to (width) * this.getOutputWidth() or height is equal
	 * to (height) * this.getOutputHeight(), whichever is smaller (ie, the source will fall within
	 * the rectangle defined by width = (width) * this.getOutputWidth() and height = (height) *
	 * this.getOutputHeight(), with top left corner coinciding with the top left corner of the
	 * output canvas). if 0.0 is used for width or height, no scaling of the new video will occur<p>
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
	 * @param width a scaling factor in [0.0, 1.0] that represents the portion of the output 
	 * canvas that can be used for the new video source, starting from the left side of the 
	 * screen (ie the video's size will be a max of width * this.getOutputWidth()). if 0.0 
	 * is used, no resizing will occur
	 * @param height see width, except for height instead of width. measured from the top of
	 * the screen. if 0.0 is used, no resizing will occur
	 * @param callback action to be taken once the new source has been created (playback will
	 * almost certainly not have started yet)
	 */
	public void createAndPlayYoutubeVideo(String sourceName, String location, String sceneName,
			int timestamp, double width, double height, Consumer<CreateSourceResponse> callback) {

		Consumer<CreateSourceResponse> modifiedCallback = response -> {
			this.nextMedia(sourceName, nextMediaResponse -> {
				callback.accept(response);
			});
		};
		this.createSourceNetworkVideo(sourceName, location, sceneName, timestamp, width, height, modifiedCallback);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideoAsFuture(String, String, String, int, double, double)},
	 * except defaulting to no video resizing
	 * @param sourceName
	 * @param location
	 * @param sceneName
	 * @param timestamp
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location, String sceneName,
			int timestamp) {
		return this.createAndPlayYoutubeVideoAsFuture(sourceName, location, sceneName, timestamp, 0.0, 0.0);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideoAsFuture(String, String, String, int, double, double)},
	 * except defaulting to current scene, no video resizing
	 * @param sourceName
	 * @param location
	 * @param timestamp
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location, int timestamp) {
		return this.createAndPlayYoutubeVideoAsFuture(sourceName, location, this.getCurrentSceneName(), timestamp);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideoAsFuture(String, String, String, int, double, double)},
	 * except defaulting to no timestamp (ie, play from start of video), no video resizing
	 * @param sourceName
	 * @param location
	 * @param sceneName
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location, String sceneName) {
		return this.createAndPlayYoutubeVideoAsFuture(sourceName, location, sceneName, 0);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideoAsFuture(String, String, String, int, double, double)},
	 * except defaulting to the current scene, timestamp of 0 (ie, play from start of video), no video resizing
	 * @param sourceName
	 * @param location
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location) {
		return this.createAndPlayYoutubeVideoAsFuture(sourceName, location, 0);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideoAsFuture(String, String, String, int, double, double)},
	 * except defaulting to current scene, timestamp of 0 (ie, play from start of video)
	 * @param sourceName
	 * @param location
	 * @param width
	 * @param height
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location, double width,
			double height) {
		return this.createAndPlayYoutubeVideoAsFuture(sourceName, location, 0, width, height);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideoAsFuture(String, String, String, int, double, double)},
	 * except defaulting to current scene
	 * @param sourceName
	 * @param location
	 * @param timestamp
	 * @param width
	 * @param height
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location, int timestamp,
			double width, double height) {
		return this.createAndPlayYoutubeVideoAsFuture(sourceName, location, this.getCurrentSceneName(), timestamp, width, height);
	}
	
	/**
	 * see {@link #createAndPlayYoutubeVideo(String, String, String, int, double, double, Consumer)}, except rather
	 * than using a Consumer callback for tasks to execute once the video is playing, a 
	 * CompletableFuture is returned instead. this future will be completed once the newly created
	 * scene item is scaled (probably about the same time that it enters the "playing" mediastate)
	 * and its completion value will be the item id for that newly created vlc video source. see 
	 * {@link #createSourceNetworkVideoAsFuture(String, String, String, int, double, double, Consumer)}
	 * for exact details
	 * @param sourceName
	 * @param location
	 * @param sceneName
	 * @param timestamp
	 * @param width
	 * @param height
	 * @return
	 */
	public CompletableFuture<Integer> createAndPlayYoutubeVideoAsFuture(String sourceName, String location, String sceneName,
			int timestamp, double width, double height) {
		return this.createSourceNetworkVideoAsFuture(sourceName, location, sceneName, timestamp, width, height, response -> {
			this.nextMedia(sourceName, null);
		});
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
	
	public void setSceneItemScale(String sourceName, double scaleX, double scaleY) {
		this.setSceneItemScale(sourceName, scaleX, scaleY, null);
	}
	
	/**
	 * scales the given scene item according to the given scale factors. after scaling occurs, the
	 * provided callback will be triggered with the scene item's name
	 * @param sourceName the name of the scene item to scale
	 * @param scaleX the new x scale factor
	 * @param scaleY the new y scale factor
	 * @param callback a consumer that accepts the name of the resized scene item. can take any action,
	 * and will be triggered after the scaling is complete
	 */
	public void setSceneItemScale(String sourceName, double scaleX, double scaleY, Consumer<String> callback) {
		this.sendMessage(new SetSceneItemPropertiesRequest.Builder().sceneItemName(sourceName)
				.scaleX(scaleX)
				.scaleY(scaleY)
				.build(),
				SetSceneItemPropertiesResponse.class, response -> {
					if(callback != null) callback.accept(sourceName);
				});
	}
	
	public void setSceneItemBounds(String sourceName, double boundsX, double boundsY) {
		this.setSceneItemBounds(sourceName, boundsX, boundsY, null);
	}
	
	public void setSceneItemBounds(String sourceName, double boundsX, double boundsY, Consumer<SetSceneItemPropertiesResponse> callback) {
		System.out.println("set bounds: " + boundsX + ", " + boundsY);
		this.sendMessage(new SetSceneItemPropertiesRequest.Builder().sceneItemName(sourceName)
				.boundsType("OBS_BOUNDS_STRETCH")
				.boundsX(boundsX)
				.boundsY(boundsY)
				.build(),
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
			if(responseType.equals(SetSceneItemPropertiesResponse.class)) {
				System.out.println(this.gson.toJson(request));
			}
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
	
	public int getCanvasHeight() {
		return this.canvasHeight.get();
	}
	
	void initCanvasHeight(int height) {
		this.canvasHeight.compareAndSet(0, height);
	}
	
	public int getCanvasWidth() {
		return this.canvasWidth.get();
	}
	
	void initCanvasWidth(int width) {
		this.canvasWidth.compareAndSet(0,  width);
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
	
	public int getVideoBufferingDelay() {
		return this.videoBufferingDelay;
	}
	
	/**
	 * initializes commonly used values so a request doesn't need to be made every time they're
	 * required
	 */
	void initialize() {
		this.retrieveCurrentScene(sceneResponse -> this.initCurrentSceneName(sceneResponse.getName()));
		this.retrieveStudioModeStatus().thenAccept(status -> this.initStudioMode(status));
		this.retrieveVideoInfo().thenAccept(response -> {
			this.initCanvasHeight(response.getBaseHeight());
			this.initCanvasWidth(response.getBaseWidth());
		});
		
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
}
