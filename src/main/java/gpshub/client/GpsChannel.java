package gpshub.client;

public interface GpsChannel {

	void sendPosition(int longitude, int latitude);

	void sendPosition(int longitude, int latitude, int altitude);
	
}
