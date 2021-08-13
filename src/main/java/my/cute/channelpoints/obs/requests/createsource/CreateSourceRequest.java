package my.cute.channelpoints.obs.requests.createsource;

import java.util.Map;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

/*
 * TODO write down appropriate sourceSettings for different types of sources somewhere
 * in this class in a javadoc or something
 */
/**
 * convenience compound request provided by obs-websocket. creates a source, creates
 * a scene item, and adds the source to that scene item
 */
public class CreateSourceRequest extends RequestBase {

	private final String sourceName;
	private final String sourceKind;
	private final String sceneName;
	private final Map<String, Object> sourceSettings;
	private final boolean setVisible;
	
	/**
	 * creates a new CreateSourceRequest with the given parameters (all required). note 
	 * this request creates a source, creates a scene item, and adds the source to that
	 * scene item
	 * @param sourceName the name of the new source
	 * @param sourceKind the type of the new source, eg "image_source" or "vlc_source"
	 * @param sceneName the name of the scene to add the new source to
	 * @param sourceSettings a map of settings for the new source. note this varies
	 * greatly depending on sourceKind
	 */
	public CreateSourceRequest(String sourceName, String sourceKind, String sceneName, Map<String, Object> sourceSettings) {
		super(RequestType.CreateSource);
		this.sourceName = sourceName;
		this.sourceKind = sourceKind;
		this.sceneName = sceneName;
		this.sourceSettings = sourceSettings;
		this.setVisible = true;
	}
	
	public CreateSourceRequest(String sourceName, String sourceKind, String sceneName, Map<String, Object> sourceSettings,
			boolean setVisible) {
		super(RequestType.CreateSource);
		this.sourceName = sourceName;
		this.sourceKind = sourceKind;
		this.sceneName = sceneName;
		this.sourceSettings = sourceSettings;
		this.setVisible = setVisible;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getSourceKind() {
		return this.sourceKind;
	}

	public String getSceneName() {
		return this.sceneName;
	}

	public Map<String, Object> getSourceSettings() {
		return this.sourceSettings;
	}

	public boolean isSetVisible() {
		return this.setVisible;
	}
}
