package gpshub.client.nio;

import gpshub.client.ChannelException;
import gpshub.client.GpsChannel;
import gpshub.client.GpsPkg;
import gpshub.client.GpsPkgHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class GpsChannelNio implements GpsChannel {

	private GpshubClientNio selectingThread;
	private InetAddress host;
	private int port;
	
	private Integer userid;

	private DatagramChannel datagramChannel;
	
	private List<ByteBuffer> pendingPackages = new LinkedList<ByteBuffer>();
	private List<GpsPkgHandler> handlers = new LinkedList<GpsPkgHandler>();
	
	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

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

	private void read(SelectionKey key) throws IOException {
		DatagramChannel datagramChannel = (DatagramChannel) key.channel();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = datagramChannel.read(readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			datagramChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}
		
		if (readBuffer.position() >= 12) {
			readBuffer.flip();
			
			GpsPkg gps = new GpsPkg();
			gps.setUserId(readBuffer.getInt(0));
			gps.setLongitude(readBuffer.getInt(4));
			gps.setLatitude(readBuffer.getInt(8));
			if (readBuffer.position() >= 16)
				gps.setAltitude(readBuffer.getInt(12));
			
			handlePkg(gps);
			
			readBuffer.compact();
		}
	}
	
	private void handlePkg(GpsPkg gps) {
		for (GpsPkgHandler handler : handlers) {
			handler.handle(gps);
		}
	}

	private void write(SelectionKey key) throws IOException {
		DatagramChannel datagramChannel = (DatagramChannel) key.channel();
		
		synchronized (pendingPackages) {
			while (!pendingPackages.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) pendingPackages.get(0);
				datagramChannel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				pendingPackages.remove(0);
			}

			if (pendingPackages.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for data.
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}
	
	private void queueSend(ByteBuffer pkg) {
		synchronized (pendingPackages) {
			pendingPackages.add(pkg);
		}
		selectingThread.queueChangeRequest(new ChangeRequest(datagramChannel, 
				ChangeRequest.CHANGEOPS,
				SelectionKey.OP_READ | SelectionKey.OP_WRITE));
	}

	public DatagramChannel getSocketChannel() {
		return datagramChannel;
	}

	public void initializeGPS(int userid, int udptoken) 
			throws ChannelException {
		this.userid = userid;
		
		ByteBuffer bBuffer = ByteBuffer.allocate(8);
		bBuffer.order(ByteOrder.BIG_ENDIAN);
		bBuffer.putInt(userid);
		bBuffer.putInt(udptoken);
		
		queueSend(bBuffer);
	}

	@Override
	public void sendPosition(int longitude, int latitude)
			throws ChannelException {
		sendPosition(longitude, latitude, 0);
	}

	@Override
	public void sendPosition(int longitude, int latitude, int altitude)
			throws ChannelException {
		if (userid == null) {
			throw new IllegalStateException("GpsChannel is not initialized, " +
					"invoke initializeGPS before sending any data");
		}
		ByteBuffer bBuffer = ByteBuffer.allocate(16);
		bBuffer.order(ByteOrder.BIG_ENDIAN);
		bBuffer.putInt(userid);
		bBuffer.putInt(longitude);
		bBuffer.putInt(latitude);
		bBuffer.putInt(altitude);
		
		queueSend(bBuffer);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

}
