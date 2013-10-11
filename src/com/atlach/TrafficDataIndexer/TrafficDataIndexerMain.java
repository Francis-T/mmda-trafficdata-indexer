package com.atlach.TrafficDataIndexer;

import java.util.ArrayList;

import com.atlach.TrafficDataIndexer.TrafficDataManager.MonitoredLocation;

/**
 * <b>TrafficDataIndexerMain Class</b> </br>Handles the running of Get Traffic
 * Data tasks using an instance of the TrafficDataManager class
 * 
 * @author francis
 * 
 */
public class TrafficDataIndexerMain implements TrafficDataIndexerEvent {
	private static final long TIMER_DURATION = 1000;
	private static final long TIMER_COUNTDOWN = 900000 / TIMER_DURATION;
	private static final long COUNT_DOWN_TO_MINS = TIMER_COUNTDOWN / 15;
	private static final long SECONDS_MAGNIFIER = TIMER_DURATION / 1000;
	private boolean shouldUseWaitThreads = false;
	public boolean isRunning = true;

	public GetTrafficDataTask getTrafficDataTask = null;
	public IndexerTimerTask indexerTimerTask = null;
	public Thread indexerTimerThread = null;
	public Thread getTrafficDataThread = null;

	private TrafficDataIndexerNotifier notifyEvent = null;

	public TrafficDataIndexerMain(TrafficDataIndexerNotifier ev) {
		notifyEvent = ev;
	}

	public TrafficDataIndexerMain(TrafficDataIndexerNotifier ev,
			boolean useWaitThreads) {
		notifyEvent = ev;
		shouldUseWaitThreads = useWaitThreads;
	}

	/*****************************************************************************************/
	/** PUBLIC METHODS **/
	/*****************************************************************************************/
	/**
	 * Starts the TrafficDataIndexer
	 */
	public void start() {
		this.runIndexerTimerTask();
	}

	/**
	 * Stops the TrafficDataIndexer
	 */
	public void stop() {
		this.isRunning = false;

		if (indexerTimerThread != null) {
			if (indexerTimerThread.isAlive()) {
				indexerTimerThread.interrupt();
			}
		}

		if (getTrafficDataTask != null) {
			getTrafficDataTask.tdm.stopReadOperations();
		}

		if (getTrafficDataThread != null) {
			if (getTrafficDataThread.isAlive()) {
				getTrafficDataThread.interrupt();
			}
		}
	}

	@Override
	public void onTimerExpire() {
		System.out.println("[onTimerExpire] Callback called.");
		System.out.println("==================================");

		indexerTimerThread = null;
		indexerTimerTask = null;

		if (shouldUseWaitThreads) {
			if (!isRunning) {
				this.runIndexerTimerTask();
			}
		}

	}

	@Override
	public void onTimerInterrupt() {
		System.out.println("[onTimerInterrupt] Callback called.");
		System.out.println("==================================");

		indexerTimerThread = null;
		indexerTimerTask = null;

		if (isRunning) {
			isRunning = false;
		}

	}

	@Override
	public void onUpdateDone() {
		System.out.println("[onUpdateDone] Callback called.");

		getTrafficDataTask = null;
		getTrafficDataThread = null;

		isRunning = false;
	}

	/*****************************************************************************************/
	/** PRIVATE METHODS **/
	/*****************************************************************************************/
	/**
	 * Runs the Get Traffic Data Task. This one starts the thread which calls
	 * for update of the Traffic Data using the TrafficDataManager
	 */
	private void runGetTrafficDataTask() {
		if (getTrafficDataTask == null) {
			getTrafficDataTask = new GetTrafficDataTask(this);
			if (getTrafficDataThread == null) {
				getTrafficDataThread = new Thread(getTrafficDataTask);
			}
			getTrafficDataThread.start();
			isRunning = true;
		}
	}

	/**
	 * Runs the Get Traffic Data Task
	 */
	private void runIndexerTimerTask() {
		System.out.println("[runIndexerTimerTask] Started");
		if (indexerTimerTask == null) {
			indexerTimerTask = new IndexerTimerTask(this);
			if (indexerTimerThread == null) {
				indexerTimerThread = new Thread(indexerTimerTask);
			}
			indexerTimerThread.start();
		}
		System.out.println("[runIndexerTimerTask] Finished");
	}

	/*****************************************************************************************/
	/** INTERNAL CLASSES **/
	/*****************************************************************************************/
	/**
	 * Runnable class for Get Traffic Data Tasks </br>This task performs the
	 * actual update of the traffic data using an instance of the
	 * TrafficDataManager class
	 * 
	 * @author francis
	 * 
	 */
	class GetTrafficDataTask implements Runnable {
		private TrafficDataIndexerEvent indexerEvent;
		public TrafficDataManager tdm = null;

		public GetTrafficDataTask(TrafficDataIndexerEvent e) {
			indexerEvent = e;
		}

		@Override
		public void run() {
			System.out.println("[GetTrafficDataTask] Thread Started");
			tdm = new TrafficDataManager(indexerEvent, notifyEvent);

			try {
				@SuppressWarnings("unused")
				ArrayList<MonitoredLocation> monitoredLocList = tdm
						.getAllLineTrafficData();
			} catch (InterruptedException e1) {
				System.out.println("[GetTrafficDataTask] Thread Interrupted.");
				tdm.stopReadOperations();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			indexerEvent.onUpdateDone();
			notifyEvent.onUpdateDone("");
		}
	}

	/**
	 * Runnable class for Indexer Timer Tasks </br><b>Purpose:</b> </br>The
	 * Indexer Timer task is continuously run whenever the Indexer is started.
	 * Depending on the value of the shouldUseWaitThreads boolean flag, it will
	 * either run once (if false) or it will run continuously (true). The
	 * purpose of this was to allow the module to be used either via a crontab'd
	 * command line operation (i.e. the system handles the waiting) or via
	 * Graphical-User Interface (i.e. the program handles its own waiting)
	 * 
	 * @author francis
	 * 
	 */
	class IndexerTimerTask implements Runnable {
		private TrafficDataIndexerEvent indexerEvent;

		public IndexerTimerTask(TrafficDataIndexerEvent e) {
			indexerEvent = e;
		}

		@Override
		public void run() {

			if (shouldUseWaitThreads) {
				try {
					System.out.println("[IndexerTimerTask] Thread Started");
					runGetTrafficDataTask();

					long minutesSec = 0;
					long secondsSec = 0;

					/*
					 * Basically, we wait for fifteen full minutes before we
					 * move on to the Get Traffic Data Update. Every second, we
					 * update the GUI status using the NotifyEvent here
					 */
					for (int i = 0; i < TIMER_COUNTDOWN; i++) {
						minutesSec = (TIMER_COUNTDOWN - i) / COUNT_DOWN_TO_MINS;
						secondsSec = ((TIMER_COUNTDOWN - i) % COUNT_DOWN_TO_MINS)
								* SECONDS_MAGNIFIER;

						if (!isRunning) {
							notifyEvent.onStatusUpdate("Next update due in: "
									+ minutesSec + " mins and " + secondsSec
									+ " secs. ");
						}

						Thread.sleep(TIMER_DURATION);
					}
				} catch (InterruptedException e) {
					System.out
							.println("[IndexerTimerTask] Thread Interrupted.");
					indexerEvent.onTimerInterrupt();
					return;
				}
			} else {
				System.out.println("[IndexerTimerTask] Thread Started");
				runGetTrafficDataTask();
			}
			System.out.println("[IndexerTimerTask] Thread Finished");

			/* Call on TimerExpire */
			indexerEvent.onTimerExpire();
		}

	}
}
