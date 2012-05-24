package gpshub.client.nio;

import gpshub.client.ChannelException;
import gpshub.client.CmdChannel;
import gpshub.client.CmdPkg;
import gpshub.client.CmdPkgHandler;
import gpshub.client.GpsChannel;
import gpshub.client.GpsPkgHandler;
import gpshub.client.GpshubClient;
import gpshub.client.GpshubErrorHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GpshubClientNio extends Thread implements GpshubClient, CmdPkgHandler {

	private InetAddress host;
	private int portCmd;
	private int portGps;

	private CmdChannelNio cmdChannel;
	private GpsChannelNio gpsChannel;

	private Selector selector;

	private Set<ChangeRequest> pendingChanges = new HashSet<ChangeRequest>();
	private List<Task> pendingTasks = new LinkedList<Task>();

	private Integer userid;
	private Integer udptoken;

	private volatile boolean udpInitialized = false;


	public GpshubClientNio(String host, int portCmd, int portGps, 
			GpshubErrorHandler errh) throws IOException {
		this.host = InetAddress.getByName(host);
		this.portCmd = portCmd;
		this.portGps = portGps;
		selector = initSelector();

		cmdChannel = new CmdChannelNio(this, this.host, portCmd);
		cmdChannel.addPackageHandler(this);
		cmdChannel.initiateConnection();

		gpsChannel = new GpsChannelNio(this, this.host, portGps);
		gpsChannel.initiateConnection();

	}

	protected void queueChangeRequest(ChangeRequest changeRequest) {
		synchronized(pendingChanges) {
			pendingChanges.add(changeRequest);
		}
	}

	@Override
	public void run() {
		// main selection loop
		while (true) {
			try {
				processPendingChanges();
				processPendingTasks();
				selector.select(500);
				processSelectedKeys();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void processPendingChanges() throws ClosedChannelException {
		// Process any pending changes
		synchronized (pendingChanges) {
			if (!pendingChanges.isEmpty()) {
				Iterator changes = pendingChanges.iterator();
				while (changes.hasNext()) {
					ChangeRequest change = (ChangeRequest) changes.next();
					switch (change.type) {
					case ChangeRequest.CHANGEOPS:
						SelectionKey key = change.channel.keyFor(selector);
						key.interestOps(change.ops);
						break;
					case ChangeRequest.REGISTER:
						change.channel.register(selector, change.ops);
						break;
					}
				}
				pendingChanges.clear();
			}
		}
	}

	private void processPendingTasks() {
		synchronized(pendingTasks) {
			if (!pendingTasks.isEmpty()) {
				Iterator<Task> tasks = pendingTasks.iterator();
				List<Task> perisitent = new LinkedList<Task>();
				while (tasks.hasNext()) {
					Task t = tasks.next();
					t.execute();
					if (t.isPersistent()) {
						perisitent.add(t);
					}
				}
				pendingTasks.clear();
				pendingTasks.addAll(perisitent);
			}
		}
	}

	private void processSelectedKeys() throws IOException {
		Iterator selectedKeys = selector.selectedKeys().iterator();
		while (selectedKeys.hasNext()) {
			SelectionKey key = (SelectionKey) selectedKeys.next();
			selectedKeys.remove();

			if (!key.isValid()) {
				continue;
			}

			if (key.channel() == cmdChannel.getSocketChannel()) {
				cmdChannel.processSelectedKey(key);
			} else {
				gpsChannel.processSelectedKey(key);
			}
		}
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		return SelectorProvider.provider().openSelector();
	}


	@Override
	public void addCmdHandler(CmdPkgHandler handler) {
		cmdChannel.addPackageHandler(handler);
	}

	@Override
	public void addGpsHandler(GpsPkgHandler handler) {
		gpsChannel.addPackageHandler(handler);
	}

	@Override
	public CmdChannel getCmdChannel() {
		return cmdChannel;
	}

	@Override
	public GpsChannel getGpsChannel() {
		return gpsChannel;
	}

	@Override
	public GpsChannel getInitializedGpsChannel() {
		// TODO Auto-generated method stub
		return null;
	}

	private void scheduleInitializeUdp() {
		Task initializeUdpTask = new Task() {
			@Override
			public void execute() {
				if (!udpInitialized) {
					try {
						gpsChannel.initializeGPS(userid, udptoken);
					} catch (ChannelException e) {
						// TODO nio channel never throws this exception
					}
					persistent = true;
				} else {
					persistent = false;
				}
			}
		};
		synchronized (pendingTasks) {
			pendingTasks.add(initializeUdpTask);
		}
	}

	@Override
	public void handle(CmdPkg cmd) {
		switch(cmd.getCode()) {

		case CmdPkg.REGISTER_NICK_ACK:
			Integer id = cmdChannel.getProtocol().getRegisterNickAck(cmd);
			if (id != 0) {
				userid = id;
				if (udptoken != null) {
					udpInitialized = false;
					scheduleInitializeUdp();
				}
			}
			break;

		case CmdPkg.INITIALIZE_UDP:
			udptoken = cmdChannel.getProtocol().getInitializeUdpToken(cmd);
			if (userid != null) {
				udpInitialized = false;
				scheduleInitializeUdp();
			}
			break;

		case CmdPkg.INITIALIZE_UDP_ACK:
			Byte status = cmdChannel.getProtocol().getUdpAck(cmd);
			if (status == 1) {
				udpInitialized = true;
			}
			break;

		}
	}

}
