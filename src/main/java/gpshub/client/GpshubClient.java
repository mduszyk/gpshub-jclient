package gpshub.client;

public interface GpshubClient {
	
	void addCmdHandler(CmdPkgHandler handler);
	
	void addGpsHandler(GpsPkgHandler handler);
	
	CmdChannel getCmdChannel();
	
	GpsChannel getGpsChannel();
	
	GpsChannel getInitializedGpsChannel();

	void start() throws ChannelException;
	
}
