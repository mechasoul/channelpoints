package my.cute.channelpoints.obs.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import my.cute.channelpoints.obs.ObsEventListener;

public class EventHandler {

	/*
	 * this map relates event classes to all registered listeners that handle events
	 * of that given class. ie, for some type T extends EventBase, 
	 * this.listeners.get(Class<T>) will return a List<ObsEventListener<?>>
	 * where each element in the list is actually of type ObsEventListener<T>.
	 * afaik there's no way to enforce this at compile time (like a way to relate
	 * the classes to the listeners), so we use generics and are very careful about
	 * how we use the structure
	 * 
	 * this is a slight pain but as far as i can tell the alternative is to create 
	 * separate event listener data structures for each new event type, or to create
	 * specific event listener classes for different event types, or something else
	 * along those lines. this approach means we don't have to add anything new to
	 * this part of the program when creating a new event and still lets us make 
	 * new listeners fairly easily with a lambda and whatever (see ObsEventListener)
	 */
	private final ConcurrentMap<Class<? extends EventBase>, List<ObsEventListener<? extends EventBase>>> listeners;
	
	public EventHandler() {
		this.listeners = new ConcurrentHashMap<>();
	}
	
	/**
	 * registers a new event listener. after calling this method, any further events
	 * of the listener's parameterized type will trigger a call to the given listener
	 * @param listener the new event listener to add
	 */
	public void registerEventListener(ObsEventListener<?> listener) {
		/*
		 * eventClass is of the same type that listener is parameterized with.
		 * this is the only method in which elements are added to the lists in
		 * this.listeners, and any given listener is always added only to the
		 * list obtained via this.listeners.get(listener.getEventClass()).
		 * consequently, we can be sure that when we do
		 * this.listeners.get(Class<T extends EventBase>), all the listeners in
		 * the returned list are actually of type ObsEventListener<T>
		 */
		Class<? extends EventBase> eventClass = listener.getEventClass();
		List<ObsEventListener<?>> list = this.listeners.get(eventClass);
		if(list == null) {
			list = Collections.synchronizedList(new ArrayList<>(3));
			this.listeners.putIfAbsent(eventClass, list);
			//get(EventType) guaranteed to not be null
			list = this.listeners.get(eventClass);
		} 
		list.add(listener);
	}
	
	/**
	 * calls all registered listeners for the given event type. if any listener
	 * returns true, it's removed and will no longer be called when any further
	 * events of this type are fired
	 * @param <T> the type of event being handled
	 * @param event the event to be passed to all existing listeners of its type
	 */
	public <T extends EventBase> void handleEvent(T event) {
		List<ObsEventListener<? extends EventBase>> eventListeners = this.listeners.get(event.getClass());
		if(eventListeners == null) {
			System.out.println("null eventlisteners");
			return;
		}
		synchronized(eventListeners) {
			Iterator<ObsEventListener<? extends EventBase>> iterator = eventListeners.iterator();
			while(iterator.hasNext()) {
				/*
				 * as noted in registerEventListener(ObsEventListener<?>), for any given
				 * Class<T extends EventBase>, the list returned by 
				 * this.listeners.get(Class<T>) is guaranteed to only contain elements
				 * of type ObsEventListener<T>, so this cast is safe and the warning can
				 * be ignored
				 */
				@SuppressWarnings("unchecked")
				ObsEventListener<T> castedListener = (ObsEventListener<T>)iterator.next();
				if(castedListener.accept(event))
					iterator.remove();
			}
		}
	}
}
