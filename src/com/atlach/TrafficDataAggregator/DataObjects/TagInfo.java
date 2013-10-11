package com.atlach.TrafficDataAggregator.DataObjects;

/**
 * <b>TagInfo Object</b>
 * </br>Object used to represent tag information
 * @author francis
 *
 */
public class TagInfo {
	public String tagset = "";
	public String date = "";
	
	public TagInfo(String d, String t) {
		date = d;
		tagset = t;
	}
}
