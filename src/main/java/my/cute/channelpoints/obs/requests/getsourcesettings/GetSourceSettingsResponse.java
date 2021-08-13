package my.cute.channelpoints.obs.requests.getsourcesettings;

import java.util.Map;

import my.cute.channelpoints.obs.requests.ResponseBase;

public class GetSourceSettingsResponse extends ResponseBase {

	private String sourceName;
	private String sourceType;
	private Map<String, Object> sourceSettings;
	
	public String getSourceName() {
		return this.sourceName;
	}
	public String getSourceType() {
		return this.sourceType;
	}
	public Map<String, Object> getSourceSettings() {
		return this.sourceSettings;
	}
	
}
