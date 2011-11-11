package gpshub.client;

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
	private Object data;
	
	public void CommanPackage() {
		
	}
	
	public void CommanPackage(byte code, short lenght, Object data) {
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

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
}
