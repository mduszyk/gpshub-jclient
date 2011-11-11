package gpshub.client;

public class GpsChannelException extends GpshubClientException {
	
	private static final long serialVersionUID = 3840143983433795154L;

	public GpsChannelException(String msg) {
		super(msg);
	}

	public GpsChannelException(String msg, Throwable th) {
		super(msg, th);
	}

}
