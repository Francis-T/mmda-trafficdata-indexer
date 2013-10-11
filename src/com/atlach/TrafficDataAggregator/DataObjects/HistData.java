package com.atlach.TrafficDataAggregator.DataObjects;

import java.util.ArrayList;
import java.util.List;

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
