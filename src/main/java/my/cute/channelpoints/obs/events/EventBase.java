package my.cute.channelpoints.obs.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.requests.RequestBase;

public abstract class EventBase {

	protected static final transient Logger log = LoggerFactory.getLogger(RequestBase.class);
	
	@SerializedName("update-type")
	private EventType updateType;
	@SerializedName("stream-timecode")
	private String streamTimecode;
	@SerializedName("rec-timecode")
	private String recordingTimecode;
	
	public EventBase(EventType type) {
		this.updateType = type;
	}
	
	public EventType getUpdateType() {
		return this.updateType;
	}
	public String getStreamTimecode() {
		return this.streamTimecode;
	}
	public String getRecordingTimecode() {
		return this.recordingTimecode;
	}
	
}
