package gpshub.client.io;

import gpshub.client.ChannelException;
import gpshub.client.GpsChannel;
import gpshub.client.GpsPkg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GpsChannelIo implements GpsChannel {
	public static final int MAX_PKG_LEN = 512;
	
	private InetAddress host;
	private int port;
	
	private DatagramSocket socket;
	
	private Integer userid;

	public GpsChannelIo(InetAddress host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void connect() throws ChannelException {
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			throw new ChannelException("Couldn't connect gps channel " +
					host + ":" + port, e);
		}
		socket.connect(host, port);
	}
	
	private void send(byte[] data) throws IOException {
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
		socket.send(datagramPacket);
	}
	
	public void initializeGPS(int userid, int UDPToken) 
	throws ChannelException {
		this.userid = userid;
		ByteBuffer bBuffer = ByteBuffer.allocate(4*2);
		bBuffer.order(ByteOrder.BIG_ENDIAN);
		bBuffer.putInt(userid);
		bBuffer.putInt(UDPToken);
		try {
			send(bBuffer.array());
		} catch (IOException e) {
			throw new ChannelException("Error initializing gps channel", e);
		}
	}
	
	public void sendPosition(int longitude, int latitude, int altitude) 
	throws ChannelException {
		if (userid == null) {
			throw new IllegalStateException("GpsChannel is not initialized, " +
					"invoke initializeGPS before sending any data");
		}
		ByteBuffer bBuffer = ByteBuffer.allocate(4*4);
		bBuffer.order(ByteOrder.BIG_ENDIAN);
		bBuffer.putInt(userid);
		bBuffer.putInt(longitude);
		bBuffer.putInt(latitude);
		bBuffer.putInt(altitude);
		try {
			send(bBuffer.array());
		} catch (IOException e) {
			throw new ChannelException("Error sending position", e);
		}
	}
	
	public void sendPosition(int longitude, int latitude) 
	throws ChannelException {
		sendPosition(longitude, latitude, 0);
	}
	
	public GpsPkg recv() throws ChannelException{
		try{
			byte[] dataBuffer = new byte[16];
			DatagramPacket readPacket = new DatagramPacket(dataBuffer, 16); 
			socket.receive(readPacket);
			return getUserPosition(readPacket);
		} catch (IOException e) {
			throw new ChannelException("Erorr in gps channel's recv", e);
		}
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
	
	public void close() {
		socket.close();
	}

}