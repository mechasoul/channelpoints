package my.cute.channelpoints.obs.requests.getmediastate;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class GetMediaStateRequest extends RequestBase {

	private final String sourceName;
	
	public GetMediaStateRequest(String sourceName) {
		super(RequestType.GetMediaState);
		this.sourceName = sourceName;
	}

	public String getSourceName() {
		return this.sourceName;
	}
}
