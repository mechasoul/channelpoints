package my.cute.channelpoints.obs.requests.getmediastate;

import my.cute.channelpoints.obs.requests.ResponseBase;

public class GetMediaStateResponse extends ResponseBase {

	private String mediaState;

	/**
	 * gets the media state returned by this GetMediaStateResponse
	 * @return the media state. one of ["none", "playing", "opening", "buffering", "paused", "stopped",
	 * "ended", "error", "unknown"]
	 */
	public String getMediaState() {
		return this.mediaState;
	}
}
