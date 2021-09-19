package my.cute.channelpoints.obs.events;

import java.util.Arrays;
import java.util.Optional;

public enum EventType {
	/*
	 * implementation notes
	 * implementing a new event type (events listed in 
	 * https://github.com/Palakis/obs-websocket/blob/4.x-current/docs/generated/protocol.md)
	 * 1. add the name of the event here (preferably in the originally written order) 
	 * 		name must be typed exactly
	 * 2. create the event class. add its response items listed in the protocol. use the
	 * 		gson SerializedName annotation if the original response item name doesn't match
	 * 		java naming convention. use private, not final. use super constructor with the
	 * 		matching EventType you just added. create getters. (see existing event classes
	 * 		for examples)
	 * 3. add the enum type and event class to the toEventClass method below
	 */

	//scenes
	SwitchScenes,
	
	//media
	MediaStarted,
	
	//scene items
	SceneItemVisibilityChanged,
	SceneItemTransformChanged,
	
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
			case SceneItemTransformChanged:
				return SceneItemTransformChangedEvent.class;
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
