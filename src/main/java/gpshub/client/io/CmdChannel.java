package gpshub.client.io;

import gpshub.client.CmdPkg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class CmdChannel {
	
	public static final int MAX_PKG_LEN = 512;
	
	private Socket socket;
	private DataInputStream dataStreamIn;
	private DataOutputStream dataStreamOut;

	public CmdChannel(InetAddress host, int port) throws Exception{
		
		try {
			socket = new Socket(host, port);
			OutputStream streamOut = socket.getOutputStream();
			InputStream streamIn = socket.getInputStream();
			dataStreamIn = new DataInputStream(streamIn);
			dataStreamOut = new DataOutputStream(streamOut);
		} catch (IOException e) {
			throw new Exception("Unable to connect to a server");
		}
	}
	
	private void send(int packageType, String data){
		try {
			byte[] dataBytes = data.getBytes("UTF-8");
			int packageLength = dataBytes.length + 3;
			try {
				dataStreamOut.writeByte(packageType);
				dataStreamOut.writeShort(packageLength);
				dataStreamOut.writeBytes(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void registerNick(String nick){
		send(CmdPkg.REGISTER_NICK, nick);
	}
	
	public void addBuddies(String csv){
		send(CmdPkg.ADD_BUDDIES, csv);
	}
	
	public void removeBuddies(String csv){
		send(CmdPkg.REMOVE_BUDDIES, csv);
	}
	
	public CmdPkg recv(){
		try{
			byte code = dataStreamIn.readByte();					
			short totalLength = dataStreamIn.readShort();
			
			CmdPkg commandPackage = new CmdPkg();
			commandPackage.setCode(code);
			commandPackage.setLength(totalLength);
			switch((int)code){
				case CmdPkg.REGISTER_NICK_ACK: commandPackage.setData(getRegisterNickAck()); break;
				case CmdPkg.INITIALIZE_UDP: commandPackage.setData(getInitializeUdpToken()); break;
				case CmdPkg.BUDDIES_IDS: commandPackage.setData(getBuddiesIds((totalLength - 3))); break;
				case CmdPkg.INITIALIZE_UDP_ACK: commandPackage.setData(getUdpAck(totalLength - 3)); break;
			}
			return commandPackage;
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int getRegisterNickAck() throws Exception {
		byte status;
		try {
			status = dataStreamIn.readByte();
			if(status == 1){
				int myId = dataStreamIn.readInt();
				return myId;
			}
			return 0;
		} catch (IOException e) {
			throw new Exception("Erorr while registering nick");
		}
	}
	
	private int getInitializeUdpToken() throws Exception {
		try {
			int token = dataStreamIn.readInt(); 
			return token;
		} catch (IOException e) {
			throw new Exception("Erorr while registering nick");
		}
	}
	
	private byte getUdpAck(int dataLength) throws Exception {
		try {
			return dataStreamIn.readByte();
		} catch (IOException e) {
			throw new Exception("Erorr while reading UPD ACK");
		}
		
	}
	
	private Map<Integer, String> getBuddiesIds(int dataLength) throws IOException {
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

}