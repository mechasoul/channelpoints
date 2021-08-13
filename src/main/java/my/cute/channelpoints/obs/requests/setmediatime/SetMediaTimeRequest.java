package my.cute.channelpoints.obs.requests.setmediatime;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class SetMediaTimeRequest extends RequestBase {
	
	private final String sourceName;
	private final int timestamp;

	/**
	 * create a new request
	 * @param sourceName the name of the media source to set time for
	 * @param timestamp the time to set the media source to, in milliseconds
	 */
	public SetMediaTimeRequest(String sourceName, int timestamp) {
		super(RequestType.SetMediaTime);
		this.sourceName = sourceName;
		this.timestamp = timestamp;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public int getTimestamp() {
		return this.timestamp;
	}
}
