package my.cute.channelpoints.obs.requests.getsourcesettings;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class GetSourceSettingsRequest extends RequestBase {

	private final String sourceName;
	private final String sourceType;
	
	public GetSourceSettingsRequest(String name) {
		super(RequestType.GetSourceSettings);
		this.sourceName = name;
		this.sourceType = null;
	}

	public GetSourceSettingsRequest(String name, String type) {
		super(RequestType.GetSourceSettings);
		this.sourceName = name;
		this.sourceType = type;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getSourceType() {
		return this.sourceType;
	}
}
