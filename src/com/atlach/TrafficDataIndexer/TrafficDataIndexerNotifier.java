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
