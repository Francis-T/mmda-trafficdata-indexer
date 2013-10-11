package com.atlach.TrafficDataAggregator.DataObjects;

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
 * <b>LineInfo Object</b> </br>Object used to represent Traffic Data for a set
 * of Lines (i.e. segment of road) using a timestamp value and a Line Data
 * string. This is essentially a condensed version of the LineDataList which
 * does not allow for compression of Line Data (since it uses a hard List)
 * 
 * @author francis
 * 
 */
public class LineInfo {
	public String timestamp;
	public String lineDataStr;

	/* Constructor */
	public LineInfo(String dateTime, String lineData) {
		timestamp = dateTime;
		lineDataStr = lineData;

		if (lineDataStr.equals(""))
			System.out.println("Warning: Blank line data string! ("
					+ this.timestamp + ")");
	}
}
