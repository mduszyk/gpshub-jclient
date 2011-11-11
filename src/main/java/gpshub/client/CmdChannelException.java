package gpshub.client;

public class CmdChannelException extends GpshubClientException {

	private static final long serialVersionUID = -7411662979354096942L;

	public CmdChannelException(String msg) {
		super(msg);
	}
	
	public CmdChannelException(String msg, Throwable th) {
		super(msg, th);
	}

}
