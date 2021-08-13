package my.cute.channelpoints.obs.requests.transitiontoprogram;

import com.google.gson.annotations.SerializedName;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class TransitionToProgramRequest extends RequestBase {

	public static class Transition {
		private String name;
		private int duration;
		
		public Transition(String name, int duration) {
			this.name = name;
			this.duration = duration;
		}
		
		public String getName() {
			return this.name;
		}
		public int getDuration() {
			return this.duration;
		}
	}
	
	@SerializedName("with-transition")
	private final Transition withTransition;
	
	public TransitionToProgramRequest(String name, int duration) {
		super(RequestType.TransitionToProgram);
		this.withTransition = new Transition(name, duration);
	}
	
	public TransitionToProgramRequest() {
		super(RequestType.TransitionToProgram);
		this.withTransition = null;
	}

	public Transition getWithTransition() {
		return this.withTransition;
	}

}
