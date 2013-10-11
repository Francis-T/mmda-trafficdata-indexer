package com.atlach.TrafficDataAggregator.DataObjects;

import java.util.List;

/**
 * <b>HistDataFileInfo Object</b>
 * </br>Object used to represent Historical Data File header information
 * @author francis
 *
 */
public class HistDataFileInfo {
	public String datesCovered = "";
	public List<String> tagList = null;
	
	public HistDataFileInfo (String datesCovered, List<String> tagList) {
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
