package my.cute.channelpoints.obs.requests.playpausemedia;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class PlayPauseMediaRequest extends RequestBase {

	private final String sourceName;
	private boolean playPause;
	
	public PlayPauseMediaRequest(String sourceName, boolean play) {
		super(RequestType.PlayPauseMedia);
		this.sourceName = sourceName;
		this.playPause = play;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public boolean isPlayPause() {
		return this.playPause;
	}

}
