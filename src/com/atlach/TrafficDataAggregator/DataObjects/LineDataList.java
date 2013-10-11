package com.atlach.TrafficDataAggregator.DataObjects;

import java.util.ArrayList;

/**
 * <b>LineDataList Object</b> </br>Object used to represent Traffic Data for a
 * set of Lines (i.e. segment of road) using a Timestamp value and a list of
 * LineData objects
 * 
 * @author francis
 * 
 */
public class LineDataList {
	/* Fields */
	public ArrayList<LineData> data;
	public String timestamp;

	/* Constructor */
	public LineDataList() {
		data = new ArrayList<LineData>();
	}
}
