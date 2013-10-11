package com.atlach.TrafficDataAggregator.DataObjects;

import java.util.ArrayList;
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
 * <b>HistData Object</b> </br>Object used to represent Historical Traffic Data
 * for a set of Lines
 * 
 * @author francis
 * 
 */
public class HistData {
	/* Fields */
	public List<LineInfo> dataList;
	public String date;
	public String tagset;
	public boolean hasChanged = false;

	/* Constructor */
	public HistData(String d) {
		dataList = new ArrayList<LineInfo>();
		date = d;
	}

	public HistData(String d, String t) {
		dataList = new ArrayList<LineInfo>();
		date = d;
		tagset = t;
	}
}
