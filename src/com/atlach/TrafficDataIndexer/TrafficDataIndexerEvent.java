package com.atlach.TrafficDataIndexer;

/* 	Copyright (C) 2013	Francis T., Zara P.
 * 
 * 	This file is a part of the MMDA Traffic Data Indexer and Aggregator Program
 * 
 * 	This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
