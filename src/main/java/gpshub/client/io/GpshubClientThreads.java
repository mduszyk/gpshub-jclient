package gpshub.client.io;

import gpshub.client.CmdChannel;
import gpshub.client.CmdChannelException;
import gpshub.client.CmdPkg;
import gpshub.client.CmdPkgHandler;
import gpshub.client.GpsChannel;
import gpshub.client.GpsChannelException;
import gpshub.client.GpsPkgHandler;
import gpshub.client.GpshubClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GpshubClientThreads implements GpshubClient {
	
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

	
	public GpshubClientThreads(String host, int portCmd, int portGps) throws UnknownHostException {
		this.host = InetAddress.getByName(host);
		this.portCmd = portCmd;
		this.portGps = portGps;

		cmdChannel = new CmdChannelIo(this.host, portCmd);
		gpsChannel = new GpsChannelIo(this.host, portGps);
		
		cmdListener = new CmdChannelListener(cmdChannel);
		cmdListener.addObserver(new CmdPkgHandler() {
			@Override
			public void handle(CmdPkg cmd) {
				handleCmdPkg(cmd);
			}
		});
		gpsListener = new GpsChannelListener(gpsChannel);
	}
	
	private void handleCmdPkg(CmdPkg cmd) {
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
					gpsChannel.initializeGPS(userid, udptoken);
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
	public void start() throws CmdChannelException, GpsChannelException {
		try {
			cmdChannel.connect();
		} catch (IOException e) {
			throw new CmdChannelException("Couldn't connect cmd channel " + host + ":" + portCmd, e);
		}
		try {
			gpsChannel.connect();
		} catch (SocketException e) {
			throw new GpsChannelException("Couldn't connect gps channel " + host + ":" + portGps, e);
		}
		
		cmdListener.setDaemon(true);
		gpsListener.setDaemon(true);
		
		cmdListener.start();
		gpsListener.start();
		
		started = true;
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

	/**
	 * Return gps channel when it is ready, method waits on condition variable
	 * until gps channel is initialized. This method can't be invoked before start().
	 */
	@Override
	public GpsChannel getGpsChannel() {
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
		return gpsChannel;
	}

}
