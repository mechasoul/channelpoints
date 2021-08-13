package my.cute.channelpoints.obs.events;

import com.google.gson.annotations.SerializedName;

public class StudioModeSwitchedEvent extends EventBase {

	@SerializedName("new-state")
	private boolean newState;
	
	public StudioModeSwitchedEvent() {
		super(EventType.StudioModeSwitched);
	}

	public boolean getNewState() {
		return this.newState;
	}
}
