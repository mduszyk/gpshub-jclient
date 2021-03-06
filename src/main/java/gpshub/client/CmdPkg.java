package gpshub.client;

import java.nio.ByteBuffer;

public class CmdPkg {
	
	public static final byte REGISTER_NICK = 0x1;
	public static final byte ADD_BUDDIES = 0x2;
	public static final byte REMOVE_BUDDIES = 0x3;
	public static final byte REGISTER_NICK_ACK = 0x65;
	public static final byte INITIALIZE_UDP = 0x69;
	public static final byte INITIALIZE_UDP_ACK = 0x6A;
	public static final byte BUDDIES_IDS = (byte) 0x96;
	
	private byte code;
	private short length;
	private byte[] data;
	
	public void CommanPackage() {
		
	}
	
	public void CommanPackage(byte code, short lenght, byte[] data) {
		this.code = code;
		this.length = lenght;
		this.data = data;
	}

	public byte getCode() {
		return code;
	}

	public void setCode(byte code) {
		this.code = code;
	}

	public short getLength() {
		return length;
	}

	public void setLength(short length) {
		this.length = length;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public ByteBuffer toByteBuffer() {
		ByteBuffer bbuf = ByteBuffer.allocate(length);
		bbuf.put(code);
		bbuf.putShort(length);
		bbuf.put(data);
		
		bbuf.flip();
		
		return bbuf;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CMDPKG ");
		sb.append(code);
		sb.append(" ");
		sb.append(length);
		sb.append(" ");
		sb.append(data.toString());
		
		return sb.toString();
	}
}
