package gpshub.client;

public class GpshubClientException extends Exception {

	private static final long serialVersionUID = -6854518968054743689L;

	public GpshubClientException(String msg) {
		super(msg);
	}
	
	public GpshubClientException(String msg, Throwable th) {
		super(msg, th);
	}

}
