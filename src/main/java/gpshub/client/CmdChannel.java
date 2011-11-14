package gpshub.client;

public interface CmdChannel {
	
	void registerNick(String nick) throws ChannelException;
	
	void addBuddies(String csv) throws ChannelException;
	
	void removeBuddies(String csv) throws ChannelException;
	
	void close();

}
