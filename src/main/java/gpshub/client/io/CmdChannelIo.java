package gpshub.client.io;

import gpshub.client.ChannelException;
import gpshub.client.CmdChannel;
import gpshub.client.CmdPkg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class CmdChannelIo implements CmdChannel {
	
	public static final int MAX_PKG_LEN = 512;
	
	private InetAddress host;
	private int port;
	
	private Socket socket;
	private DataInputStream dataStreamIn;
	private DataOutputStream dataStreamOut;
	
	public CmdChannelIo(InetAddress host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void connect() throws ChannelException {
		InputStream streamIn;
		OutputStream streamOut;
		try {
			socket = new Socket(host, port);
			streamOut = socket.getOutputStream();
			streamIn = socket.getInputStream();
		} catch (IOException e) {
			throw new ChannelException("Couldn't connect cmd channel " + 
					host + ":" + port, e);
		}
		dataStreamIn = new DataInputStream(streamIn);
		dataStreamOut = new DataOutputStream(streamOut);
	}
	
	private void send(int packageType, String data) throws IOException {
		byte[] dataBytes = data.getBytes("latin1");
		int packageLength = dataBytes.length + 3;
		dataStreamOut.writeByte(packageType);
		dataStreamOut.writeShort(packageLength);
		dataStreamOut.writeBytes(data);
	}
	
	public void registerNick(String nick) throws ChannelException {
		try {
			send(CmdPkg.REGISTER_NICK, nick);
		} catch (IOException e) {
			throw new ChannelException("Error sending data: register nick", e);
		}
	}
	
	public void addBuddies(String csv) throws ChannelException {
		try {
			send(CmdPkg.ADD_BUDDIES, csv);
		} catch (IOException e) {
			throw new ChannelException("Error sending data: add buddies", e);
		}
	}
	
	public void removeBuddies(String csv) throws ChannelException {
		try {
			send(CmdPkg.REMOVE_BUDDIES, csv);
		} catch (IOException e) {
			throw new ChannelException("Error sending data: add buddies", e);
		}
	}
	
	public CmdPkg recv() throws ChannelException {
		try {
			byte code = dataStreamIn.readByte();					
			short totalLength = dataStreamIn.readShort();
			
			CmdPkg cmd = new CmdPkg();
			cmd.setCode(code);
			cmd.setLength(totalLength);
			cmd.setData(readData(totalLength - 3));
				
			return cmd;
			
		} catch (IOException e) {
			throw new ChannelException("Erorr in cmd channel's recv", e);
		}
	}
	
	private byte[] readData(int len) throws IOException {
		byte[] buf = new byte[len];
		dataStreamIn.read(buf, 0, len);
		
		return buf;
	}
	
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// hard to close, eh ?
		}
	}

}