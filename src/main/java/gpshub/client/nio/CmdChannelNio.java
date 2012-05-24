package gpshub.client.nio;

import gpshub.client.ChannelException;
import gpshub.client.CmdChannel;
import gpshub.client.CmdPkg;
import gpshub.client.CmdPkgHandler;
import gpshub.client.CmdProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class CmdChannelNio implements CmdChannel {
	
	private GpshubClientNio selectingThread;
	private InetAddress host;
	private int port;
	
	private SocketChannel socketChannel;
	
	private List<ByteBuffer> pendingPackages = new LinkedList<ByteBuffer>();
	private List<CmdPkgHandler> handlers = new LinkedList<CmdPkgHandler>();
	
	private CmdProtocol protocol = new CmdProtocol();
	
	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
	
	public CmdChannelNio(GpshubClientNio selectingThread, InetAddress host, int port) {
		this.selectingThread = selectingThread;
		this.host = host;
		this.port = port;
	}
	
	
	protected void initiateConnection() throws IOException {
		// Create a non-blocking socket channel
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
	
		// Kick off connection establishment
		boolean isConnected = socketChannel.connect(
				new InetSocketAddress(host, port));
		
		if (isConnected) {
			selectingThread.queueChangeRequest(new ChangeRequest(socketChannel, 
					ChangeRequest.REGISTER, SelectionKey.OP_READ));
		} else {
			// Queue a channel registration since the caller is not the 
			// selecting thread. As part of the registration we'll register
			// an interest in connection events. These are raised when a channel
			// is ready to complete connection establishment.
			selectingThread.queueChangeRequest(new ChangeRequest(socketChannel, 
						ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		}
	}
	
	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
	
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			System.out.println(e);
			key.cancel();
			return;
		}
	
		// Register an interest in reading on this channel
		key.interestOps(SelectionKey.OP_READ);
	}

	protected void processSelectedKey(SelectionKey key) throws IOException {
		// Check what event is available and deal with it
		if (key.isConnectable()) {
			finishConnection(key);
		} else if (key.isReadable()) {
			read(key);
		} else if (key.isWritable()) {
			write(key);
		}
	}
	
	protected void addPackageHandler(CmdPkgHandler handler) {
		handlers.add(handler);
	}
	
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}
		
		if (readBuffer.position() > 3) {
			readBuffer.flip();
			byte code = readBuffer.get();					
			short totalLength = readBuffer.getShort();
			
			if (readBuffer.position() >= totalLength) {
				// we have complete package

				byte[] buf = new byte[totalLength - 3];
				readBuffer.get(buf, 0, totalLength - 3);

				CmdPkg cmd = new CmdPkg();
				cmd.setCode(code);
				cmd.setLength(totalLength);
				cmd.setData(buf);
				
				handlePkg(cmd);
				
			} else {
				readBuffer.rewind();
			}
			
			readBuffer.compact();
		}

	}
	
	private void handlePkg(CmdPkg cmd) {
		for (CmdPkgHandler handler : handlers) {
			handler.handle(cmd);
		}
	}
	
	private void write(SelectionKey key) throws IOException {
		synchronized (pendingPackages) {
			while (!pendingPackages.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) pendingPackages.get(0);
				socketChannel.write(buf);
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
	
	public SocketChannel getSocketChannel() {
		return socketChannel;
	}
	
	private void queueSend(ByteBuffer pkg) {
		synchronized (pendingPackages) {
			pendingPackages.add(pkg);
		}
		selectingThread.queueChangeRequest(new ChangeRequest(socketChannel, 
				ChangeRequest.CHANGEOPS,
				SelectionKey.OP_READ | SelectionKey.OP_WRITE));
	}
	
	@Override
	public void registerNick(String nick) throws ChannelException {
		CmdPkg pkg = protocol.buildRegisterNickPkg(nick);
		queueSend(pkg.toByteBuffer());
	}

	@Override
	public void addBuddies(String csv) throws ChannelException {
		CmdPkg pkg = protocol.buildAddBuddiesPkg(csv);
		queueSend(pkg.toByteBuffer());
	}

	@Override
	public void removeBuddies(String csv) throws ChannelException {
		CmdPkg pkg = protocol.buildRemoveBuddiesPkg(csv);
		queueSend(pkg.toByteBuffer());
	}

	public CmdProtocol getProtocol() {
		return protocol;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

}
