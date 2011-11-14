package gpshub.client;

public class ChannelException extends Exception {
	
	private static final long serialVersionUID = 3840143983433795154L;

	public ChannelException(String msg) {
		super(msg);
	}

	public ChannelException(String msg, Throwable th) {
		super(msg, th);
	}

}
