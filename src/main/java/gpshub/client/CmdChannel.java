package gpshub.client;

public interface CmdChannel {
	
	void registerNick(String nick);
	
	void addBuddies(String csv);
	
	void removeBuddies(String csv);

}
