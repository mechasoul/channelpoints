package my.cute.channelpoints.obs.events;

import java.util.Arrays;
import java.util.Optional;

public enum EventType {

	//scenes
	SwitchScenes,
	
	//media
	MediaStarted,
	
	//scene items
	SceneItemVisibilityChanged,
	
	//studio mode
	StudioModeSwitched;
	
	public static Class<? extends EventBase> toEventClass(EventType type) {
		switch(type) {
			case MediaStarted:
				return MediaStartedEvent.class;
			case StudioModeSwitched:
				return StudioModeSwitchedEvent.class;
			case SwitchScenes:
				return SwitchScenesEvent.class;
			case SceneItemVisibilityChanged:
				return SceneItemVisibilityChangedEvent.class;
			default:
				throw new IllegalArgumentException("unknown event type: " + type);
		}
	}
	
	/**
	 * like valueOf, but returns an Optional&lt;EventType&gt; containing the corresponding
	 * EventType instead of the EventType itself. if no EventType exists with the 
	 * provided name, the returned Optional will be empty, unlike the original valueOf
	 * method, which throws an exception
	 * @param value the name of the EventType value to be returned
	 * @return an Optional either containing the EventType with the same name as the 
	 * provided String if such an EventType exists, otherwise an empty Optional
	 */
	public static Optional<EventType> tryValueOf(String value) {
		return Arrays.stream(EventType.values()).filter(eventType -> eventType.name().equals(value)).findFirst();
	}
}
