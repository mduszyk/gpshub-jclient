package gpshub.client.io;

import gpshub.client.GpsPkg;
import gpshub.client.GpsPkgHandler;

import java.util.ArrayList;
import java.util.List;

public class GpsChannelListener extends Thread {
	
	private GpsChannelIo gpsChannel;
	private Boolean read;

	private List<GpsPkgHandler> observers;
	
	public GpsChannelListener(GpsChannelIo myGpsChannel){
		this.gpsChannel = myGpsChannel;
		read = true;
		observers = new ArrayList<GpsPkgHandler>();
	}
	
	@Override
	public void run() {
		while(read){
			GpsPkg gpsPackage = gpsChannel.recv();
			if(gpsPackage != null){
				notifyObeservers(gpsPackage);
			}		
		}
	}
	
	public void addObserver(GpsPkgHandler packageHandler) {
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
