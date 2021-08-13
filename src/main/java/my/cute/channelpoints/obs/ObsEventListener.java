package my.cute.channelpoints.obs;

import java.util.function.Function;

import my.cute.channelpoints.obs.events.EventBase;

public interface ObsEventListener<T extends EventBase> {
	/*
	 * TODO figure out proper class visibility / package structure for event classes
	 */
	/**
	 * defines the action an event listener should take when an event of the corresponding type 
	 * is fired. in general, can take any action. event listeners can be configured to be 
	 * deleted after some condition is satisfied via this method's return value; when this 
	 * event listener is triggered, if this method returns true, the listener will be deleted
	 * and will no longer trigger on any further events of this type. if this method returns 
	 * false, it will continue to be triggered upon further events of this type
	 * @param event the event that triggered the listener
	 * @return true if this event listener should no longer be fired on events of this type,
	 * and false if it should continue to be fired on events of this type
	 */
	public boolean accept(T event);
	
	public Class<T> getEventClass();
	
	public static <T extends EventBase> ObsEventListener<T> createEventListener(Class<T> eventClass, Function<T, Boolean> action) {
		return new ObsEventListenerImpl<>(eventClass, action);
	}
	
//	public default Class<T> getEventClass() {
//		try {
//			TypeToken<T> token = new TypeToken<T>() {};
//			System.out.println(token.getRawType());
//			Type type = this.getClass().getDeclaredMethod("accept", EventBase.class).getGenericParameterTypes()[0];
//			return (Class<T>)type;
//		} catch (NoSuchMethodException | SecurityException e) {
//			throw new AssertionError(e);
//		}
//	}
}
