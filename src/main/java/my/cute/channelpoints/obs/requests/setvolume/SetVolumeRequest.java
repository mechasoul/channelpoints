package my.cute.channelpoints.obs.requests.setvolume;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class SetVolumeRequest extends RequestBase {

	private final String source;
	private final double volume;
	private final boolean useDecibel = true;
	
	/**
	 * create a new request to set volume of a given source
	 * @param sourceName the name of the source to adjust volume for
	 * @param volume the volume, in decibels, to set the volume to
	 */
	public SetVolumeRequest(String sourceName, double volume) {
		super(RequestType.SetVolume);
		this.source = sourceName;
		this.volume = volume;
	}

	public String getSource() {
		return this.source;
	}

	public double getVolume() {
		return this.volume;
	}

	public boolean isUseDecibel() {
		return useDecibel;
	}
}
