package gpshub.client.io;

import gpshub.client.ChannelException;
import gpshub.client.GpsPkg;
import gpshub.client.GpsPkgHandler;
import gpshub.client.GpshubErrorHandler;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class GpsChannelListener extends Thread {
	
	private GpsChannelIo gpsChannel;
	private Boolean read;

	private List<GpsPkgHandler> observers;
	private GpshubErrorHandler errh;
	
	public GpsChannelListener(GpsChannelIo myGpsChannel, 
			GpshubErrorHandler errh) {
		this.gpsChannel = myGpsChannel;
		this.errh = errh;
		read = true;
		observers = new ArrayList<GpsPkgHandler>();
	}
	
	@Override
	public void run() {
		while(read){
			GpsPkg gpsPackage;
			try {
				gpsPackage = gpsChannel.recv();
				if(gpsPackage != null) { 
					notifyObeservers(gpsPackage);
				}
			} catch (ChannelException e) {
				if (e.getCause() instanceof SocketException) {
					break;
				}
				errh.handleError(e);
				break;
			} catch (Exception e) {
				errh.handleError(e);
				break;
			}
		}
	}
	
	public void addPackageHandler(GpsPkgHandler packageHandler) {
		observers.add(packageHandler);
	}
	
	public void stopListening(){
		read = false;
	}
	
	private void notifyObeservers(GpsPkg gpsPackage) {
		for(GpsPkgHandler observer : observers){
			observer.handle(gpsPackage);
		}
	}
}
