package my.cute.channelpoints.obs.requests.restartmedia;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class RestartMediaRequest extends RequestBase {

	private final String sourceName;
	
	public RestartMediaRequest(String sourceName) {
		super(RequestType.RestartMedia);
		this.sourceName = sourceName;
	}

	public String getSourceName() {
		return this.sourceName;
	}

}
