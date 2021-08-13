package my.cute.channelpoints.obs.requests.authenticate;

import my.cute.channelpoints.obs.requests.RequestBase;
import my.cute.channelpoints.obs.requests.RequestType;

public class AuthenticateRequest extends RequestBase {
	
	private final String auth;

	public AuthenticateRequest(String auth) {
		super(RequestType.Authenticate);
		this.auth = auth;
	}

	public String getAuth() {
		return this.auth;
	}

}
