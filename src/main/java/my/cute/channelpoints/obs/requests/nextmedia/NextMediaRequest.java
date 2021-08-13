package my.cute.channelpoints.obs.requests.nextmedia;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class NextMediaRequest extends RequestBase {

	private final String sourceName;
	
	public NextMediaRequest(String sourceName) {
		super(RequestType.NextMedia);
		this.sourceName = sourceName;
	}

	public String getSourceName() {
		return this.sourceName;
	}
	
}
