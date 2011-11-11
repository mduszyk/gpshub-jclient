package gpshub.client.io;

import gpshub.client.GpsPkg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GpsChannel {
	public static final int MAX_PKG_LEN = 512;
	
	private DatagramSocket socket;

	public GpsChannel(InetAddress host, int port) throws Exception{
		
		try {
			socket = new DatagramSocket();
			socket.connect(host, port);
		} catch (IOException e) {
			throw new Exception("Unable to connect to a server");
		}
	}
	
	private void send(byte[] data){
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
		try {
			socket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void initializeGPS(int userId, int UDPToken){
		ByteBuffer bBuffer = ByteBuffer.allocate(4*2);
		bBuffer.order(ByteOrder.BIG_ENDIAN);
		bBuffer.putInt(userId);
		bBuffer.putInt(UDPToken);
		send(bBuffer.array());
	}
	
	public void sendPosition(int userId, int longitude, int latitude, int altitude){
		ByteBuffer bBuffer = ByteBuffer.allocate(4*4);
		bBuffer.order(ByteOrder.BIG_ENDIAN);
		bBuffer.putInt(userId);
		bBuffer.putInt(longitude);
		bBuffer.putInt(latitude);
		bBuffer.putInt(altitude);
		send(bBuffer.array());
	}
	
	public void sendPosition(int userId, int longitude, int latitude){
		sendPosition(userId, longitude, latitude, 0);
	}
	
	public GpsPkg recv(){
		try{
			byte[] dataBuffer = new byte[16];
			DatagramPacket readPacket = new DatagramPacket(dataBuffer, 16); 
			socket.receive(readPacket);
			return getUserPosition(readPacket);
		} catch (IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	private GpsPkg getUserPosition(DatagramPacket dp){
		ByteBuffer bBuffer = ByteBuffer.wrap(dp.getData());
		GpsPkg gpsPackage = new GpsPkg();
		gpsPackage.setUserId(bBuffer.getInt(0));
		gpsPackage.setLongitude(bBuffer.getInt(4));
		gpsPackage.setLatitude(bBuffer.getInt(8));
		if (dp.getLength() == 16)
			gpsPackage.setAltitude(bBuffer.getInt(12));
		return gpsPackage;
	}

}