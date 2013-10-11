package com.atlach.TrafficDataAggregator;

import java.io.File;
import java.io.FileFilter;

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
 * <b>FileDateFilter Class</b> </br> Used to filter out Raw Traffic Data Files
 * which should no longer be included when we are (re)generating a Traffic
 * Historical Data File from scratch. Basically, all files falling within the
 * "DatesCovered" tag of the HistData file are avoided.
 * 
 * @author francis
 * 
 */
public class FileDateFilter implements FileFilter {

	private String filterStr[];

	public FileDateFilter(String f) {
		filterStr = f.split(",");
	}

	@Override
	public boolean accept(File file) {
		String fnStr = file.getName();

		if (fnStr.contains("_TrafficRec.txt") == false) {
			return false;
		}

		String fnSplit[] = fnStr.split("_");

		if (fnSplit.length != 3) {
			return false;
		}

		for (int i = 0; i < filterStr.length; i++) {
			if (filterStr[i].equals("")) {
				continue;
			}

			if (filterStr[i].contains("-")) {
				String rangeStr[] = filterStr[i].split("-");

				/* Perform range test */
				int highRange = Integer.parseInt(rangeStr[1].trim());
				int lowRange = Integer.parseInt(rangeStr[0].trim());
				int compVal = Integer.parseInt(fnSplit[0].trim());

				if ((compVal >= lowRange) && (compVal <= highRange)) {
					return false;
				}
			} else {
				if (fnSplit[0].contains(filterStr[i])) {
					return false;
				}
			}
		}

		return true;
	}
}
