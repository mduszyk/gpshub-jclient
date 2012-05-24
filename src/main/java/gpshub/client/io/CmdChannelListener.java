package gpshub.client.io;

import gpshub.client.ChannelException;
import gpshub.client.CmdPkg;
import gpshub.client.CmdPkgHandler;
import gpshub.client.GpshubErrorHandler;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class CmdChannelListener extends Thread {
	
	private CmdChannelIo commandChannel;
	private Boolean read;

	private List<CmdPkgHandler> observers;
	private GpshubErrorHandler errh;
	
	public CmdChannelListener(CmdChannelIo cmdChannel, 
			GpshubErrorHandler errh) {
		this.commandChannel = cmdChannel;
		this.errh = errh;
		read = true;
		observers = new ArrayList<CmdPkgHandler>();
	}
	
	@Override
	public void run() {
		while(read) {
			try {
				CmdPkg cmd = commandChannel.recv();
				if(cmd != null)
					notifyObeservers(cmd);
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
	
	public void addPackageHandler(CmdPkgHandler packageHandler) {
		observers.add(packageHandler);
	}
	
	public void stopListening(){
		read = false;
	}
	
	private void notifyObeservers(CmdPkg commandPackage) {
		for(CmdPkgHandler observer : observers) {
			observer.handle(commandPackage);
		}
	}

}