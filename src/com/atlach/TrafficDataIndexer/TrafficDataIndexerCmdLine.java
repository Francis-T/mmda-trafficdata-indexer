package com.atlach.TrafficDataIndexer;

import com.atlach.TrafficDataAggregator.TrafficDataAggregatorMain;

/**
 * <b>TrafficDataIndexerCmdLine Class</b>
 * </br>Provides a Command Line Interface for the TrafficDataIndexer and TrafficDataAggregator.
 * The old name was retained for historical purposes.
 * @author francis
 *
 */
public class TrafficDataIndexerCmdLine implements TrafficDataIndexerNotifier {
	private TrafficDataIndexerMain trafficIndexer = null;
	private TrafficDataAggregatorMain trafficAggregator = null;
	private ShutdownHookTask shutdownTask = null;
	private String filename;
	private String timestamp;
	
	public TrafficDataIndexerCmdLine() {
		shutdownTask = new ShutdownHookTask();
	}

	/*****************************************************************************************/
	/** PUBLIC METHODS																		**/
	/*****************************************************************************************/
	@Override
	public void onStatusUpdate(String s) {
		// TODO Auto-generated method stub
		return;
	}

	@Override
	public void onTrafficDataFileSaved(String filename, String timestamp) {
		this.filename = new String(filename);
		this.timestamp = new String(timestamp);
		System.out.println("[onTrafficDataFileSaved] Filename: " + this.filename + ", Timestamp: " + this.timestamp);
	}

	@Override
	public void onUpdateDone(String message) {
		if (trafficAggregator == null) {
			trafficAggregator = new TrafficDataAggregatorMain();
		}
		
		trafficAggregator.runPushDataTask(filename, timestamp);
	}

	/*****************************************************************************************/
	/** PRIVATE METHODS																		**/
	/*****************************************************************************************/
	/**
	 * Starts the Traffic Data Indexer
	 */
	private void startIndexer() {
		if (trafficIndexer == null) {
			trafficIndexer = new TrafficDataIndexerMain(this, false);
		}
		trafficIndexer.start();
	}

	/*****************************************************************************************/
	/** INTERNAL CLASSES																	**/
	/*****************************************************************************************/
	/**
	 * Shutdown Hook Task for a graceful exit even if we use CTRL+C on the program
	 * @author francis
	 *
	 */
	class ShutdownHookTask extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			System.out.println("Shutdown hook ran!");
			
			if (trafficIndexer != null) {
				trafficIndexer.stop();
			}
			
			if (trafficAggregator != null) {
				trafficAggregator.stopOperation();
			}
		}
		
	}

	/**
	 * Runs the Traffic Data Indexer CLI
	 * @param args
	 */
	public static void main(String[] args) {		
		TrafficDataIndexerCmdLine tdcm = new TrafficDataIndexerCmdLine();
		
		/* Add interceptor shutdown hook */
		Runtime.getRuntime().addShutdownHook(tdcm.shutdownTask);
		
		tdcm.startIndexer();
	}	
}
