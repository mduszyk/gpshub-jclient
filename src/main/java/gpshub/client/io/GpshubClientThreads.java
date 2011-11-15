package gpshub.client.io;

import gpshub.client.ChannelException;
import gpshub.client.CmdChannel;
import gpshub.client.CmdPkg;
import gpshub.client.CmdPkgHandler;
import gpshub.client.GpsChannel;
import gpshub.client.GpsPkgHandler;
import gpshub.client.GpshubClient;
import gpshub.client.GpshubErrorHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GpshubClientThreads implements GpshubClient, CmdPkgHandler {
	
	private CmdChannelIo cmdChannel;
	private GpsChannelIo gpsChannel;
	
	private CmdChannelListener cmdListener;
	private GpsChannelListener gpsListener;
	
	private InetAddress host;
	private int portCmd;
	private int portGps;
	
	private Integer userid;
	private Integer udptoken;
	
	private volatile boolean udpInitialized = false;
	
	private Lock condLock = new ReentrantLock();
	private Condition gpsCond  = condLock.newCondition();
	
	private volatile boolean started = false;

	
	public GpshubClientThreads(String host, int portCmd, int portGps, 
			GpshubErrorHandler errh) throws UnknownHostException {
		this.host = InetAddress.getByName(host);
		this.portCmd = portCmd;
		this.portGps = portGps;

		cmdChannel = new CmdChannelIo(this.host, portCmd);
		gpsChannel = new GpsChannelIo(this.host, portGps);
		
		cmdListener = new CmdChannelListener(cmdChannel, errh);
		cmdListener.addObserver(this);
		gpsListener = new GpsChannelListener(gpsChannel, errh);
	}
	
	@Override
	public void handle(CmdPkg cmd) {
		switch(cmd.getCode()) {
		
		case CmdPkg.REGISTER_NICK_ACK:
			Integer id = (Integer) cmd.getData();
			if (id != 0) {
				userid = id;
				if (udptoken != null) {
					runUdpInitializer();
				}
			}
			break;
			
		case CmdPkg.INITIALIZE_UDP:
			udptoken = (Integer) cmd.getData();
			if (userid != null) {
				udpInitialized = false;
				runUdpInitializer();
			}
			break;

		case CmdPkg.INITIALIZE_UDP_ACK:
			Byte status = (Byte) cmd.getData();
			if (status == 1) {
				udpInitialized = true;
				// signal condition variable
				condLock.lock();
				try {
					gpsCond.signal();
				} finally {
					condLock.unlock();
				}
			}
			break;
		
		}
	}
	
	private void runUdpInitializer() {
		new Thread() {
			public void run() {
				while (!udpInitialized) {
					try {
						gpsChannel.initializeGPS(userid, udptoken);
					} catch (ChannelException e1) {
						// try to wait and do it again
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// sleeping interrupted, who cares
					}
				}
			}
		}.start();
	}

	@Override
	public void start() throws ChannelException {
		cmdChannel.connect();
		gpsChannel.connect();
		
		cmdListener.setDaemon(true);
		gpsListener.setDaemon(true);
		
		cmdListener.start();
		gpsListener.start();
		
		started = true;
	}
	
	public void stop() {
		
	}
	
	@Override
	public void addCmdHandler(CmdPkgHandler handler) {
		cmdListener.addObserver(handler);
	}

	@Override
	public void addGpsHandler(GpsPkgHandler handler) {
		gpsListener.addObserver(handler);
	}

	@Override
	public CmdChannel getCmdChannel() {
		return cmdChannel;
	}

	@Override
	public GpsChannel getGpsChannel() {
		return gpsChannel;
	}
	
	public GpsChannel getInitializedGpsChannel() {
		if (!udpInitialized) {
			return null;
		}
		
		return gpsChannel;
	}
	
	/**
	 * Method waits on condition variable until gps channel is initialized.
	 * This method can't be invoked before start().
	 */
	public void waitForGpsChannel() {
		if (!started) {
			throw new IllegalStateException("GpshubClient is not started!");
		}
		// wait for gps channel
		condLock.lock();
		try {
			while (!udpInitialized) {
				try {
					gpsCond.await();
				} catch (InterruptedException e) {
					// check condition and wait again if necessary
				}
			}
		} finally {
			condLock.unlock();
		}
	}

}
