package gpshub.client;

public interface GpshubClient {
	
	void addCmdHandler(CmdPkgHandler handler);
	
	void addGpsHandler(GpsPkgHandler handler);
	
	void start();

}
