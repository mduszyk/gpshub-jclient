package gpshub.client.io;

import gpshub.client.CmdPkg;
import gpshub.client.CmdPkgHandler;

import java.util.ArrayList;
import java.util.List;

public class CmdChannelListener extends Thread {
	
	private CmdChannel commandChannel;
	private Boolean read;

	private List<CmdPkgHandler> observers;
	
	public CmdChannelListener(CmdChannel cmdChannel) {
		this.commandChannel = cmdChannel;
		read = true;
		observers = new ArrayList<CmdPkgHandler>();
	}
	
	@Override
	public void run() {
		while(read) {
			CmdPkg commandPackage = commandChannel.recv();
			if(commandPackage != null){
				notifyObeservers(commandPackage);
			}		
		}
	}
	
	public void addObserver(CmdPkgHandler packageHandler) {
		observers.add(packageHandler);
	}
	
	public void stopListening(){
		read = false;
	}
	
	private void notifyObeservers(CmdPkg commandPackage) {
		for(CmdPkgHandler observer : observers){
			observer.handle(commandPackage);
		}
	}
}