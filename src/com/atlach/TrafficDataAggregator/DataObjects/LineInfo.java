package com.atlach.TrafficDataAggregator.DataObjects;

/**
 * <b>LineInfo Object</b>
 * </br>Object used to represent Traffic Data for a set of Lines (i.e. segment of road)
 * using a timestamp value and a Line Data string. This is essentially a condensed version
 * of the LineDataList which does not allow for compression of Line Data (since it uses a
 * hard List)
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
			System.out.println("Warning: Blank line data string! (" + this.timestamp + ")");
	}
}
