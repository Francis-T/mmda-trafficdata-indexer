package com.atlach.TrafficDataIndexer;

/**
 * Interface for allowing TrafficDataIndexer events to be received and handled
 * by the implementing class
 * 
 * @author francis
 * 
 */
public interface TrafficDataIndexerNotifier {
	/**
	 * Handler for the conclusion of a Traffic Data update session
	 * 
	 * @param message
	 *            - a message string
	 */
	public void onUpdateDone(String message);

	/**
	 * Handler for the status messages received from the Indexer. Only used with
	 * the UI.
	 * 
	 * @param s
	 *            - a status string
	 */
	public void onStatusUpdate(String s);

	/**
	 * Handler for the event where the traffic data file is finally saved to
	 * disk
	 * 
	 * @param filename
	 *            - the name of the saved traffic data file
	 * @param timestamp
	 *            - the timestamp to associate with the saved traffic data file
	 */
	public void onTrafficDataFileSaved(String filename, String timestamp);
}
