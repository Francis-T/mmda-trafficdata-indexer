package com.atlach.TrafficDataAggregator;

import com.atlach.TrafficDataIndexer.TrafficDataIndexerNotifier;
/**
 * <b>TrafficDataAggregatorMain Class</b>
 * </br>Handles the running of Push Traffic Data and Generate Historical Data tasks using an
 * instance of the TrafficDataAggregator class 
 * @author francis
 *
 */
public class TrafficDataAggregatorMain implements TrafficDataIndexerNotifier {
	private boolean isRunning = false;
	public PushTrafficDataTask aggregatorPushDataTask = null;
	public GenHistDataTask aggregatorGenHistDataTask = null;
	public Thread aggregatorPushDataThread = null;
	public Thread aggregatorGenHistDataThread = null;

	/*****************************************************************************************/
	/** PUBLIC METHODS																		**/
	/*****************************************************************************************/
	/**
	 * Runs Push Traffic Data using the TrafficDataAggregator through a thread 
	 * @param filename - the name of the Raw Traffic Data File to push
	 * @param timestamp - the timestamp to associate with the data
	 */
	public void runPushDataTask(String filename, String timestamp) {
		if (aggregatorPushDataTask == null) {
			aggregatorPushDataTask = new PushTrafficDataTask(filename, timestamp, this);
			if (aggregatorPushDataThread == null) {
				aggregatorPushDataThread = new Thread(aggregatorPushDataTask);
			}
			aggregatorPushDataThread.start();
		}
	}

	/**
	 * Runs (Re)generate Historical Data using the TrafficDataAggregator through a thread 
	 * @param directory - the directory containing Raw Traffic Data Files to be used to 
	 * 					  generate the Hist Data File
	 */
	public void runGenerateHistDataTask(String directory) {
		if (aggregatorGenHistDataTask == null) {
			aggregatorGenHistDataTask = new GenHistDataTask(directory, this);
			if (aggregatorGenHistDataThread == null) {
				aggregatorGenHistDataThread = new Thread(aggregatorGenHistDataTask);
			}
			aggregatorGenHistDataThread.start();
		}
	}
	
	/*****************************************************************************************/
	/** INTERNAL CLASSES																	**/
	/*****************************************************************************************/
	/**
	 * Runnable class for Re(generate) Historical Data tasks
	 * @author francis
	 *
	 */
	class GenHistDataTask implements Runnable {
		private String directory = null;
		public TrafficDataAggregator tda = null;
		private TrafficDataIndexerNotifier notifEvent = null;
		
		public GenHistDataTask (String d, TrafficDataIndexerNotifier e) {
			tda = new TrafficDataAggregator();
			
			directory = d;
			notifEvent = e;
		}
		@Override
		public void run() {
			if (directory != null) {
				int result = tda.generateHistDataFile(directory);
				if (result != TrafficDataAggregator.STATUS_OK) {
					System.out.println("[GenHistDataTask] Generate Hist Data File Failed!");
				}
			} else {
				System.out.println("[GenHistDataTask] Directory is null!");
			}
			notifEvent.onUpdateDone("[GenHistDataTask]");
		}
	}
	
	/**
	 * Runnable class for Push Traffic Data tasks
	 * @author francis
	 *
	 */
	class PushTrafficDataTask implements Runnable {
		private String filename = null;
		private String timestamp = null;
		public TrafficDataAggregator tda = null;
		private TrafficDataIndexerNotifier notifEvent = null;
		
		public PushTrafficDataTask(String filename, String timestamp, TrafficDataIndexerNotifier e) {
			tda = new TrafficDataAggregator();
			
			this.filename = filename;
			this.timestamp = timestamp;
			this.notifEvent = e;
		}

		@Override
		public void run() {
			isRunning = true;
			
			int result = tda.pushTrafficData(filename, timestamp, "");
//			int result = tda.offloadCollectedData("TrafficData.part", "Weekend|Saturday, Weather|Rain|Moderate", new FileSystemInterface(), true);
			if (result != TrafficDataAggregator.STATUS_OK) {
				System.out.println("[onUpdateDone] Push Traffic Data Failed!");
			}
			notifEvent.onUpdateDone("[PushTrafficDataTask]");
		}
		
	}
	
	/**
	 * Waits for the operation of the Push Traffic Data and Generate Hist Data tasks to stop.
	 * </br>NOTE: It may cause lots of problems if we stop this prematurely, especially when
	 * 			  we're dealing with HistData files which we can't readily recreate.
	 */
	public void stopOperation() {
		if (isRunning) {
			try {
				System.out.println("Waiting for push traffic data task to finish...");
				aggregatorPushDataThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onUpdateDone(String message) {
		if (message.contains("PushTrafficDataTask")) {
			this.aggregatorPushDataTask = null;
			this.aggregatorPushDataThread = null;
			isRunning = false;
		} else if (message.contains("GenHistDataTask")) {
			this.aggregatorGenHistDataTask = null;
			this.aggregatorGenHistDataThread = null;
		}
	}

	@Override
	public void onStatusUpdate(String s) {
		/* Not Used */
	}

	@Override
	public void onTrafficDataFileSaved(String filename, String timestamp) {
		/* Not Used */
	}
}
