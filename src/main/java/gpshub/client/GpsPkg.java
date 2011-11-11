package gpshub.client;

public class GpsPkg {
	
	private int userId;
	private int longitude;
	private int latitude;
	private int altitude;
	
	public GpsPkg() {
		
	}

	public GpsPkg(int userId, int longitude, int latitude, int altitude) {
		this.userId = userId;
		this.longitude = longitude;
		this.latitude = latitude;
		this.altitude = altitude;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getLongitude() {
		return longitude;
	}

	public void setLongitude(int longitude) {
		this.longitude = longitude;
	}

	public int getLatitude() {
		return latitude;
	}

	public void setLatitude(int latitude) {
		this.latitude = latitude;
	}

	public int getAltitude() {
		return altitude;
	}

	public void setAltitude(int altitude) {
		this.altitude = altitude;
	}

}
