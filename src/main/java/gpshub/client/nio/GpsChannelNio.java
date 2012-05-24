package gpshub.client.nio;

import gpshub.client.ChannelException;
import gpshub.client.CmdPkgHandler;
import gpshub.client.GpsChannel;
import gpshub.client.GpsPkgHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;

public class GpsChannelNio implements GpsChannel {

	private GpshubClientNio selectingThread;
	private InetAddress host;
	private int port;
	
	private Integer userid;

	private DatagramChannel datagramChannel;
	
	private List<GpsPkgHandler> handlers;

	public GpsChannelNio(GpshubClientNio selectingThread, InetAddress host, int port) {
		this.selectingThread = selectingThread;
		this.host = host;
		this.port = port;
	}

	protected void initiateConnection() throws IOException {
		// Create a non-blocking socket channel
		datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);

		// Kick off connection establishment
		datagramChannel.connect(new InetSocketAddress(host, port));

		// Queue a channel registration since the caller is not the 
		// selecting thread. As part of the registration we'll register
		// an interest in connection events. These are raised when a channel
		// is ready to complete connection establishment.
		selectingThread.queueChangeRequest(new ChangeRequest(datagramChannel, 
				ChangeRequest.REGISTER, SelectionKey.OP_READ | SelectionKey.OP_WRITE));
	}

	protected void processSelectedKey(SelectionKey key) throws IOException {

		// Check what event is available and deal with it
		if (key.isReadable()) {
			read(key);
		} else if (key.isWritable()) {
			write(key);
		}

	}
	
	protected void addPackageHandler(GpsPkgHandler handler) {
		handlers.add(handler);
	}

	private void read(SelectionKey key) {

	}

	private void write(SelectionKey key) {

	}

	public DatagramChannel getSocketChannel() {
		return datagramChannel;
	}





	public void initializeGPS(int userid, int UDPToken) 
			throws ChannelException {
//		this.userid = userid;
//		ByteBuffer bBuffer = ByteBuffer.allocate(4*2);
//		bBuffer.order(ByteOrder.BIG_ENDIAN);
//		bBuffer.putInt(userid);
//		bBuffer.putInt(UDPToken);
//		try {
//			send(bBuffer.array());
//		} catch (IOException e) {
//			throw new ChannelException("Error initializing gps channel", e);
//		}
	}

	@Override
	public void sendPosition(int longitude, int latitude)
			throws ChannelException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendPosition(int longitude, int latitude, int altitude)
			throws ChannelException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
