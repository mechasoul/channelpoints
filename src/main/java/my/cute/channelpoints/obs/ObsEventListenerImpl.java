package my.cute.channelpoints.obs;

import java.util.function.Function;

import my.cute.channelpoints.obs.events.EventBase;

class ObsEventListenerImpl<T extends EventBase> implements ObsEventListener<T> {

	private final Class<T> eventClass;
	private final Function<T, Boolean> function;
	
	ObsEventListenerImpl(Class<T> eventClass, Function<T, Boolean> function) {
		this.eventClass = eventClass;
		this.function = function;
	}

	@Override
	public boolean accept(T event) {
		return this.function.apply(event);
	}

	public Class<T> getEventClass() {
		return this.eventClass;
	}
}
