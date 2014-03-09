package org.uninottstt.android.util;

public class MonitoredThread {

	private int interruptTimeout = 10000;

	private boolean allowRerun = false;

	private Thread thread = null;

	public MonitoredThread(final Runnable r) {
		thread = new Thread(r);
		thread.setName("MonitoredThread " + System.currentTimeMillis());
	}

	public boolean startWithMonitorBlock() {

		long start;
		boolean interrupted;

		do {
			thread.start();
			interrupted = false;
			start = System.currentTimeMillis();
			while (thread.isAlive()) {
				try {
					if (thread.isInterrupted()) {
						// wait for death
					} else if ((System.currentTimeMillis() - start) > interruptTimeout) {
						// start = System.currentTimeMillis();
						thread.interrupt();
						System.out.println(thread.getName()
								+ " has been interrupted after timing out");
						interrupted = true;
					}
					Thread.sleep(10);
				} catch (Exception e) {
					continue;
				}
			}
		} while (allowRerun && interrupted);

		return true;
	}

	public int getInterruptTimeout() {
		return interruptTimeout;
	}

	public void setInterruptTimeout(int interruptTimeout) {
		this.interruptTimeout = interruptTimeout;
	}

	public boolean isAllowRerun() {
		return allowRerun;
	}

	public void setAllowRerun(boolean allowRerun) {
		this.allowRerun = allowRerun;
	}

}
