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
import java.util.HashMap;
import java.util.Map;

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
			
			CmdPkg commandPackage = new CmdPkg();
			commandPackage.setCode(code);
			commandPackage.setLength(totalLength);
			
			switch(code) {
				case CmdPkg.REGISTER_NICK_ACK:
					commandPackage.setData(getRegisterNickAck());
					break;
				case CmdPkg.INITIALIZE_UDP:
					commandPackage.setData(getInitializeUdpToken());
					break;
				case CmdPkg.BUDDIES_IDS:
					commandPackage.setData(getBuddiesIds((totalLength - 3)));
					break;
				case CmdPkg.INITIALIZE_UDP_ACK:
					commandPackage.setData(getUdpAck(totalLength - 3));
					break;
			}
			
			return commandPackage;
			
		} catch (IOException e) {
			throw new ChannelException("Erorr receiving cmd pkg", e);
		}
	}
	
	private int getRegisterNickAck() throws ChannelException {
		byte status;
		try {
			status = dataStreamIn.readByte();
			if(status == 1){
				int myId = dataStreamIn.readInt();
				return myId;
			}
			return 0;
		} catch (IOException e) {
			throw new ChannelException("Erorr processing pkg: " +
					"register nick ack", e);
		}
	}
	
	private int getInitializeUdpToken() throws ChannelException {
		try {
			int token = dataStreamIn.readInt(); 
			return token;
		} catch (IOException e) {
			throw new ChannelException("Erorr processing pkg: " +
					"initialize UDP", e);
		}
	}
	
	private byte getUdpAck(int dataLength) throws ChannelException {
		try {
			return dataStreamIn.readByte();
		} catch (IOException e) {
			throw new ChannelException("Erorr processing pkg:  " +
					"initialize UPD ACK", e);
		}
		
	}
	
	private Map<Integer, String> getBuddiesIds(int dataLength) 
	throws IOException {
		Map<Integer, String> buddiesIds = new HashMap<Integer, String>();
		
		byte[] buf = new byte[dataLength];
		
		int n = 0;
		while (n < dataLength) {
			int userid = dataStreamIn.readInt();
			n += 4;
			int i = 0;
			byte b;
			while ((b = dataStreamIn.readByte()) != 0)
				buf[i++] = b;
			String nick = new String(buf, 0, i);
			buddiesIds.put(userid, nick);
			i++; // skip '\0'
			n += i;
		}
		
		return buddiesIds;
	}
	
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// hard to close, eh ?
		}
	}

}