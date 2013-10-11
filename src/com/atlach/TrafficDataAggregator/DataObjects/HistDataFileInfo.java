package com.atlach.TrafficDataAggregator.DataObjects;

import java.util.List;

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
 * <b>HistDataFileInfo Object</b> </br>Object used to represent Historical Data
 * File header information
 * 
 * @author francis
 * 
 */
public class HistDataFileInfo {
	public String datesCovered = "";
	public List<String> tagList = null;

	public HistDataFileInfo(String datesCovered, List<String> tagList) {
		this.datesCovered = datesCovered;
		this.tagList = tagList;
	}

	public void printInfo() {
		System.out.println("Dates Covered: " + this.datesCovered);
		System.out.println("Tags:");
		for (int i = 0; i < tagList.size(); i++) {
			System.out.println("-" + tagList.get(i));
		}
	}
}
