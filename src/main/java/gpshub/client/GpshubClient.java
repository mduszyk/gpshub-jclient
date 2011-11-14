package gpshub.client;

public interface GpshubClient {
	
	void addCmdHandler(CmdPkgHandler handler);
	
	void addGpsHandler(GpsPkgHandler handler);
	
	CmdChannel getCmdChannel();
	
	GpsChannel getGpsChannel();
	
	void start() throws ChannelException;
	
}
