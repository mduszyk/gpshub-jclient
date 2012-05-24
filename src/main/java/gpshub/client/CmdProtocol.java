package gpshub.client;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class CmdProtocol {

	public int getRegisterNickAck(CmdPkg pkg) {
		ByteBuffer bbuf = ByteBuffer.wrap(pkg.getData());

		byte status = bbuf.get();
		if(status == 1) {
			int myId = bbuf.getInt();
			return myId;
		}
		return 0;
	}

	public int getInitializeUdpToken(CmdPkg pkg) {
		ByteBuffer bbuf = ByteBuffer.wrap(pkg.getData());

		int token = bbuf.getInt(); 
		return token;
	}

	public byte getUdpAck(CmdPkg pkg) {
		return pkg.getData()[0];
	}

	public Map<Integer, String> getBuddiesIds(CmdPkg pkg) {
		Map<Integer, String> buddiesIds = new HashMap<Integer, String>();
		ByteBuffer bbuf = ByteBuffer.wrap(pkg.getData());
		int dataLength = pkg.getLength() - 3;
		
		byte[] buf = new byte[dataLength];

		int n = 0;
		while (n < dataLength) {
			int userid = bbuf.getInt();
			n += 4;
			int i = 0;
			byte b;
			while ((b = bbuf.get()) != 0)
				buf[i++] = b;
			String nick = new String(buf, 0, i);
			buddiesIds.put(userid, nick);
			i++; // skip '\0'
			n += i;
		}

		return buddiesIds;
	}
	
	private CmdPkg buildPkg(byte code, String data) {
		CmdPkg pkg = new CmdPkg();
		pkg.setCode(code);
		byte[] dataBytes;
		try {
			dataBytes = data.getBytes("latin1");
		} catch (UnsupportedEncodingException e) {
			dataBytes = data.getBytes();
		}
		int packageLength = dataBytes.length + 3;
		pkg.setLength((short) packageLength);
		pkg.setData(dataBytes);
		
		return pkg;
	}
	
	public CmdPkg buildRegisterNickPkg(String nick) {
		return buildPkg(CmdPkg.REGISTER_NICK, nick);
	}
	
	public CmdPkg buildAddBuddiesPkg(String csv) {
		return buildPkg(CmdPkg.ADD_BUDDIES, csv);
	}
	
	public CmdPkg buildRemoveBuddiesPkg(String csv) {
		return buildPkg(CmdPkg.REMOVE_BUDDIES, csv);
	}

}
