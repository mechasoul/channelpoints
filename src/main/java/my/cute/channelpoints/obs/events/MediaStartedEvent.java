package my.cute.channelpoints.obs.events;

public class MediaStartedEvent extends EventBase {

	private String sourceName;
	private String sourceKind;
	
	public MediaStartedEvent() {
		super(EventType.MediaStarted);
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getSourceKind() {
		return this.sourceKind;
	}
}
