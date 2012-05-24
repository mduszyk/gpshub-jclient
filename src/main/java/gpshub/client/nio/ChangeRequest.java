package gpshub.client.nio;
import java.nio.channels.SelectableChannel;

public class ChangeRequest {
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	
	public SelectableChannel channel;
	public int type;
	public int ops;
	
	public ChangeRequest(SelectableChannel channel, int type, int ops) {
		this.channel = channel;
		this.type = type;
		this.ops = ops;
	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + type;
		hash = 31 * hash + ops;
		hash = 31 * hash + (channel == null ? 0 : channel.hashCode());
		
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		// object must be ChangeRequest at this point
		ChangeRequest cr = (ChangeRequest) obj;
		
		return type == cr.type && ops == cr.ops &&
				(channel == cr.channel 
					|| (channel != null && channel.equals(cr.channel)));
	}
	
	
}
