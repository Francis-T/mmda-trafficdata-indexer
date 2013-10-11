package com.atlach.TrafficDataIndexer;

/**
 * Interface for handling Traffic Data Indexer timed events
 * 
 * @author francis
 * 
 */
public interface TrafficDataIndexerEvent {

	/**
	 * Handler for the timer expiration event. Note that the timer being
	 * referred to here is the one used to control when the traffic update tasks
	 * are run.
	 */
	public void onTimerExpire();

	/**
	 * Handler for the timer interruption event. Note that the timer being
	 * referred to here is the one used to control when the traffic update tasks
	 * are run.
	 */
	public void onTimerInterrupt();

	/**
	 * Handler for the traffic update conclusion event
	 */
	public void onUpdateDone();
}
