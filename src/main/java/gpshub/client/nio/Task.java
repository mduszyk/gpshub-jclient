package gpshub.client.nio;

public abstract class Task { 
	
	/**
	 * weather this task should be rescheduled
	 */
	protected boolean persistent = false;
	
	public boolean isPersistent() {
		return persistent;
	}

	public abstract void execute();

}
