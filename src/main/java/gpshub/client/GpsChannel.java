package gpshub.client;

public interface GpsChannel {

	void sendPosition(int longitude, int latitude) throws ChannelException;

	void sendPosition(int longitude, int latitude, int altitude)
	throws ChannelException;
	
	void close();
	
}
