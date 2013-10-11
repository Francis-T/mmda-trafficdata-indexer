package com.atlach.TrafficDataAggregator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.atlach.TrafficDataAggregator.DataObjects.*;

/**
 * <b>DataFileManager Class</b>
 * </br>
 * Used for handling all interactions with Data Files. It mostly serves to simplify things from the 
 * viewpoint of whichever object is using this class.
 * </br>
 * </br>
 * Also contains various static utility functions for 
 * conveniently manipulating compressed/encoded Line Data 
 * strings.
 * @author francis
 *
 */
public class DataFileManager {
	public static final int STATUS_OK = 0;
	public static final int STATUS_FAILED = -1;

	private List<String> datesCoveredList = null;
	private boolean hasTriggeredDebugFlag = false;
	private String tagsetFileName = "tags.txt";
	private FileSystemInterface fsi;
	
	public DataFileManager() {
		fsi = new FileSystemInterface();
		datesCoveredList = new ArrayList<String>();
	}

	/*****************************************************************************************/
	/** PUBLIC METHODS																		**/
	/*****************************************************************************************/
	/**
	 * Parses all Traffic Data Files in a given folder and creates a list of HistData objects. 
	 * 
	 * TODO: This large method should be broken up into individual parts in the future
	 * 
	 * @param directoryStr	- Pathname of the target directory
	 * @param histDataList	- HistData list to be filled
	 * @param datesFilter	- Optional filter for file name dates to exclude from parsing
	 * @return an integer indicating the exit status for this method
	 */
	public int parseTrafficDataFiles(String directoryStr, 
									 List<HistData> histDataList, 
									 String datesFilter) {
		/* Navigate to the directory */
		File directory = new File(directoryStr);
		if ((directory.exists() == false) || (directory.isDirectory() == false)) {
			System.out.println("[ERROR] Invalid directory specified: "
					+ directoryStr);
			return STATUS_FAILED;
		}
		
		if (histDataList == null) {
			System.out.println("[ERROR] HistDataList not initialized!");
			return STATUS_FAILED;
		}

		File[] fList = directory.listFiles(new FileDateFilter(datesFilter));
		ArrayList<File> fileNames = new ArrayList<File>();
		/*
		 * Create a list of filenames ending with "TrafficRec.txt" References:
		 * http://java.dzone.com/articles/java-example-list-all-files OR
		 * http://docs.oracle.com/javase/tutorial/essential/io/dirs.html#listdir
		 */
		for (int i = 0; i < fList.length; i++) {
			if (fList[i].getName().contains("TrafficRec.txt")) {
				
				if (fileNames.size() == 0) {
					fileNames.add(fList[i]);
					continue;
				}
				
				String fileNameSplit[] = fList[i].getName().split("_");
				int insIdx = 0;
				int dateVal = Integer.parseInt(fileNameSplit[0]);
				
				String compFileSplit[] = fileNames.get(0).getName().split("_");
				
				int compVal = Integer.parseInt(compFileSplit[0]);
				
				int j = 0;
				
				while (dateVal > compVal) {
					insIdx = j;
					
					if (j >= fileNames.size()) {
						insIdx = -1;
						break;
					}
					
					compFileSplit = fileNames.get(j++).getName().split("_");
					compVal = Integer.parseInt(compFileSplit[0]);
				}
				
				if (insIdx >= 0) {
					fileNames.add(insIdx, fList[i]);
				} else {
					fileNames.add(fList[i]);
				}
			} else {
				System.out.println("NOT added to file list: " + fList[i].getName());
			}
		}
		
		/* Load the tagset file to be used */
		List<TagInfo> tagInfoList = null;
		try {
			tagInfoList = fsi.loadTagsetFile(tagsetFileName);
		} catch (IOException e) {
			e.printStackTrace();
			return STATUS_FAILED;
		}

		HistData hd = null;
		LineDataList tempDataList = null;

		/* Run parseFileContents(...) for each listed filename */
		for (int i = 0; i < fileNames.size(); i++) {
			/* Initialize our holder variables */
			tempDataList = null;
			hd = null;
			
			/* Initialize the flag for indicating whether we are using a new HistData
			 * object (which should be added to the masterList at the end of the loop)
			 * or reusing an old one (no more need to add it again to the masterList).
			 * 
			 * HISTORY: Without this, the original files being created were insanely
			 * 			large (on the order of ~1.6MB/660KB) in comparison to when we
			 * 			have this boolean flag (~54.9KB/18.1KB). 
			 * 
			 * 			Basically, there were lots of duplicate data.
			 */
			boolean usesNewHistDataObject = false;
			
			/*
			 * Extract date associated with this file name. 
			 * e.g. "20130901_0000_TrafficRec.txt" ---> "20130901"
			 */
			String deconStr[] = fileNames.get(i).getName().split("_");
			String dateStr = deconStr[0];
			String timeStr = deconStr[1];
			if (timeStr.length() < 4) {
				timeStr += "0";
			}

			/* Check if we already have a HistData object that can hold this new data */
			for (int k = 0; k < histDataList.size(); k++) {
				if (histDataList.get(k).date.equals(dateStr)) {
					hd = histDataList.get(k);
					break;
				}
			}
			
			/* If we don't have a HistData object yet, just create a new one */
			if (hd == null) {
				hd = new HistData(dateStr);
				usesNewHistDataObject = true;
			}

			/* Load traffic data file information into a temporary list */
			tempDataList = fsi.loadTrafficDataFile(fileNames.get(i));
			
			/* Catch the case where the tempDataList is empty */
			if (tempDataList == null) {
				/* Skip this file then */
				continue;
			}

			/* Create the Line Data String we are going to store in the HistData object */
			/* What we're doing here basically is taking the traffic values stored in
			 * each LineData object stored in tempDataList and mapping them to their
			 * appropriate ASCII64/Base64 values.
			 * 
			 * NOTE: Since we're using each traffic data value byte to store the last four
			 * 		traffic conditions associated with each line, there doesn't seem to
			 * 		be any significant memory/space savings as a result. That said, we
			 * 		might be better off storing this as a full binary file instead of
			 * 		converting it to text first.
			 * 
			 * NOTE: The ASCII64/Base64 encoding system used here is currently incorrect
			 */
			String lineDataStr = createLineDataString(tempDataList);

			/* Check if we already have LineInfo entry in the HistData object which has
			 * the same hour as the data we are going to be parsing in */
			/* Derive the hour and minute substrings */
			int hour = Integer.parseInt(timeStr.substring(0, 2));
			int mins = Integer.parseInt(timeStr.substring(2, 4));
			String augTimeStr = "";
			String hourStr = "";
			LineInfo lineInfo = null;
			int k = -1;
			
			/* Setup the final time string depending on the hour and minute values we currently have */
			if (mins >= 30) {
				hourStr = ((hour+1 < 10) ? "0" : "") + Integer.toString(hour+1);
			} else {
				hourStr = ((hour < 10) ? "0" : "") + Integer.toString(hour);
			}
			augTimeStr = hourStr + "00";
			
			for (k = 0; k < hd.dataList.size(); k++) {
				int compHour = Integer.parseInt(hd.dataList.get(k).timestamp.substring(0, 2));
				/* In case of the latter half of an hour, this will be factored in to
				 * the next hour instead of this one (unless we are at hour 23) */
				if ( (hour < 23) &&
					 (mins >= 30) ) {
					if ((hour+1) == compHour) {
						lineInfo = hd.dataList.get(k);
						
						augTimeStr = (((compHour < 10) ? "0" : "") + 
										Integer.toString(compHour) + "00");
						break;
					}
				} else {
					if (hour == compHour) {
						lineInfo = hd.dataList.get(k);
						break;
					}
				}
			}
			
			String finalStr = null;
			if (lineInfo != null) {
				/* If we already have a particular LineInfo object to write to,
				 * (possibly due to the previous step) then attempt to merge the
				 * line data strings */
				String oldStr = decompressString(lineInfo.lineDataStr);
				String mergedStr = mergeLineDataStrings(oldStr, lineDataStr);
				
				if (mergedStr.equals("") == false) {
					finalStr = compressString(mergedStr);
				} else {
					/* Fall back to the original plan if merging fails */
					System.out.println("Warning: Failed to merge to an existing line data string!");
					if (!hasTriggeredDebugFlag) {
						System.out.println("   NEW: " + lineDataStr);
						System.out.println("   OLD: " + oldStr);
						System.out.println("MERGED: " + mergedStr);
					}
					finalStr = compressString(oldStr);
				}

				lineInfo.lineDataStr = finalStr;
				continue;
			} else {
				/* Compress the string and create a new LineInfo object for it */
				finalStr = compressString(lineDataStr);
				
				lineInfo = new LineInfo(augTimeStr, finalStr);
			}

			/* Lookup tag information from the tagInfoList */
			/* Skip the tag addition step if we dont have valid tag information */
			if (tagInfoList != null) {
				for (int j = 0; j < tagInfoList.size(); j++) {
					String compDate = tagInfoList.get(j).date;
					
					if (hd.date.equals(compDate)) {
						hd.tagset = tagInfoList.get(j).tagset;
						break;
					}
				}
			}
			
			/* Attempt to add this date to the running datesCoveredList if it isnt there yet */
			boolean existsInDateList = false;
			for (int j = 0; j < datesCoveredList.size(); j++) {
				if (dateStr.equals(datesCoveredList.get(j)) == true){
					existsInDateList = true;
					break;
				}
			}
			if (existsInDateList == false) {
				addToDatesCoveredList(dateStr);
			}

			/* Some kind of mechanism to insert this properly to the data list */
			insertToDataList(lineInfo, hd.dataList);
			
			/* Finally, add the HistData object to the masterList */
			if (usesNewHistDataObject) {
				histDataList.add(hd);
			}
			
			if (hasTriggeredDebugFlag)	// DEBUG
				break;	// DEBUG
		}
		
		return STATUS_OK;
	}
	
	/**
	 * Normalizes the line data strings for all elements in the given HistData 
	 * list.
	 * @param hdList - the list of HistData objects to be normalized
	 * @return an integer indicating the exit status for this method
	 */
	public int normalizeHistDataList(List<HistData> hdList) {
		for (int i = 0; i < hdList.size(); i++) {
			HistData hd = hdList.get(i);
			for (int j = 0; j < hd.dataList.size(); j++) {
				LineInfo li = hd.dataList.get(j);
				String tmpStr = decompressString(li.lineDataStr);
				li.lineDataStr = compressString(normalizeLineDataString(tmpStr));
			}
		}
		return STATUS_OK;
	}
	
	/**
	 * Saves the day's partial traffic data to a Traffic Data Part File 
	 * @param partFileName - Name of the Traffic Data Part file to save to 
	 * @param dataList - An Object containing a list of LineData objects
	 * @return an integer indicating the exit status for this method
	 */
	public int savePartialData(String partFileName, LineDataList dataList) {
		int result = STATUS_OK;
		/* Create the line data string from the dataList */
		String initLineDataStr = createLineDataString(dataList);
		
		/** [DEBUG] 2013-10-06 **/
		if (initLineDataStr.length() != 284) {
			System.out.println("[savePartialData] Weird line data string length prior to " +
											   "compression! (" + initLineDataStr.length() + ")");
			System.out.println("[savePartialData] Printing out dataList contents (" + 
											   				   dataList.data.size() + ")...");
			
			for (int i = 0; i < dataList.data.size(); i++) {
				try {
					System.out.println("[" + i + "] " + dataList.data.get(i).toString());
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
		/** [DEBUG] 2013-10-06 **/
		
		String lineDataStr = compressString(initLineDataStr);
		String timestamp = dataList.timestamp;
		
		if (decompressString(lineDataStr).length() != 284) {
			System.out.println("[savePartialData] Weird line data string length caught on " + 
												 "decompress! (" + initLineDataStr.length() + ")");
		}
		
		try {
			result = fsi.saveDataToPartFile(partFileName, timestamp, lineDataStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * Merges together two HistData objects, updating old line data and adding in 
	 * new ones wherever appropriate.
	 * @param newHistData - the newer HistData object to be merged
	 * @param oldHistData - the older HistData object to be merged
	 * @return an integer indicating the exit status for this method
	 */
	public int mergeHistData(HistData newHistData, HistData oldHistData) {
		if ((newHistData == null) || (oldHistData == null)) {
			return STATUS_FAILED;
		}
		
		for (int i = 0; i < oldHistData.dataList.size(); i++) {
			String oldTimeStr = oldHistData.dataList.get(i).timestamp;
			String oldLineDataStr = decompressString(oldHistData.dataList.get(i).lineDataStr);
			
			for (int j = 0; j < newHistData.dataList.size(); j++) {
				String newTimeStr = newHistData.dataList.get(j).timestamp;
				if (oldTimeStr.equals(newTimeStr)) {
					String newLineDataStr = 
								  decompressString(newHistData.dataList.get(j).lineDataStr);
					
					String mergedStr = mergeLineDataStrings(oldLineDataStr, newLineDataStr);
					
					if (mergedStr.equals("")) {
						System.out.println("[mergeHistData] Warning: Returned merged string is " + 
																						"empty!");
						mergedStr = oldLineDataStr;
					}
					oldHistData.dataList.get(i).lineDataStr = compressString(mergedStr);
				}
			}
		}
		
		/* Cycle through the list again and merge together whichever entries are mergeable */
		for (int i = 0; i < newHistData.dataList.size(); i++) {
			boolean hasNoMergeables = true;
			String newTimeStr = newHistData.dataList.get(i).timestamp;
			for (int j = 0; j < oldHistData.dataList.size(); j++) {
				String oldTimeStr = oldHistData.dataList.get(j).timestamp;
				if (newTimeStr.equals(oldTimeStr)) {
					hasNoMergeables = false;
				}
			}
			
			if (hasNoMergeables) {
				insertToDataList(newHistData.dataList.get(i), oldHistData.dataList);
			}
		}
		
		return STATUS_OK;
	}
	

	/**
	 * Gets the "dates covered" string for the last parseTrafficDataFiles() 
	 * operation. 
	 * 
	 * TODO: Consider replacing this with a method that just draws all
	 * 		 dates from a HistData list instead.
	 * @return the dates covered string
	 */
	public String getDatesCoveredString() {
		String datesStr = "";
		
		if (datesCoveredList.size() == 0) {
			return datesStr;
		}

		int prevMonth = Integer.parseInt(datesCoveredList.get(0).substring(4, 6));
		int prevDay = Integer.parseInt(datesCoveredList.get(0).substring(6, 8));
		
		datesStr += datesCoveredList.get(0);
		
		for (int i = 1; i < datesCoveredList.size(); i++) {
			int month = Integer.parseInt(datesCoveredList.get(i).substring(4, 6));
			int day = Integer.parseInt(datesCoveredList.get(i).substring(6, 8));
			
			/* If value of this day does not immediately follow that of the day
			 * before, and we're still in the same month, then we must have skipped
			 * some days.
			 */
			if ((day != prevDay+1) && (month == prevMonth)) {
				datesStr += "-";
				datesStr += datesCoveredList.get(i-1);
				datesStr += ",";
				
				datesStr += datesCoveredList.get(i);
			}
			
			/* If were at the tail end of the list, do this */
			if (i+1 >= datesCoveredList.size()) {
				if ((day == prevDay+1) && (month == prevMonth)) {
					datesStr += "-";
					datesStr += datesCoveredList.get(i);
				} else {
					datesStr += ", ";
					datesStr += datesCoveredList.get(i);
				}
			}

			prevDay = day;
			prevMonth = month;
		}
		
		return datesStr.trim();
	}
	
	/**
	 * Reduces the amount of data obtained from the Traffic Data Part File
	 * by merging together LineData entries for the same hour.
	 * @param histData - HistData object containing the list of LineData
	 * 					 obtained from the Traffic Data Part File
	 * @return the reduced HistData object
	 */
	public HistData reducePartHistData(HistData histData) {
		HistData redHistData = new HistData(histData.date);
		
		for (int i = 0; i < histData.dataList.size(); i++) {
			LineInfo outerLineInfo = histData.dataList.get(i);
			String outerTimeStr = outerLineInfo.timestamp;
			
			boolean wasMerged = false;
			for (int j = 0; j < redHistData.dataList.size(); j++) {
				LineInfo innerLineInfo = redHistData.dataList.get(j);
				String innerTimeStr = innerLineInfo.timestamp;
				
				/* Means that we have encountered this LineInfo object before. 
				 * Therefore, merge. */
				if (innerTimeStr.equals(outerTimeStr)) {
					String oldStr = decompressString(innerLineInfo.lineDataStr);
					String newStr = decompressString(outerLineInfo.lineDataStr);
					String mergeStr = mergeLineDataStrings(oldStr, newStr);
					
					if (mergeStr.equals("") == false) {
						innerLineInfo.lineDataStr = compressString(mergeStr);
					} else {
						innerLineInfo.lineDataStr = compressString(oldStr);
					}
					wasMerged = true;
					break;
				}
			}
			
			/* If this was not merged, then add it to the reduced data list.*/
			if (!wasMerged) {
				insertToDataList(outerLineInfo, redHistData.dataList);
			}
		}
		
		return redHistData;
	}

	/*****************************************************************************************/
	/** PUBLIC STATIC METHODS																**/
	/*****************************************************************************************/
	/**
	 * Inserts a Line Info object into the running Line Info list. This method
	 * basically ensures that the Line Info list elements are in chronological
	 * order based on their LineInfo.timestamp values
	 * @param lineInfo - LineInfo object to be added to the dataList
	 * @param dataList - list which should contain the LineInfo object
	 * @return an integer indicating the exit status for this method
	 */
	public static int insertToDataList(LineInfo lineInfo, List<LineInfo> dataList) {
		int insertId = getDataListInsertionIndex(lineInfo.timestamp, dataList);
		if (insertId != -1) {
			dataList.add(insertId, lineInfo);
		} else {
			dataList.add(lineInfo);
		}
		return STATUS_OK;
	}

	
	/**
	 * Compresses a given string by removing adjacent repeating characters. 
	 * The resulting string may later be fed into the decompressString()
	 * utility to restore the original string.
	 * @param str - the original, uncompressed String
	 * @return the compressed String
	 */
	public static String compressString(String str) {
		String newStr = "";
		char next = 0;
		boolean isCompressing = false;
		boolean shouldSupressCompression = false;
		byte sameCounter = 1;
		
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			
			if (i+1 < str.length()) {
				next = str.charAt(i+1);
				
				/* Determine if compressing this is worth it */
				if ((i+2 < str.length()) && (!isCompressing)) {
					/* If this repeating character is only two
					 * characters long, don't bother trying to
					 * compress it since we're effectively
					 * adding 50% more to its uncompressed
					 * size instead. */
					if (c != str.charAt(i+2)) {
						shouldSupressCompression = true;
					} else {
						shouldSupressCompression = false;
					}
				}
			} else {
				next = 0;
			}
			
			if ((c == next) && (!shouldSupressCompression)) {
				if (!isCompressing) {
					sameCounter++;
					isCompressing = true;
					newStr += ".";
				} else {
					if (sameCounter < Constants.base64chars.length() - 1) {
						sameCounter++;
					} else {
						/* Have to prematurely break the compression */
						isCompressing = false;
						newStr += Constants.base64chars.charAt(sameCounter);
						sameCounter = 1;
						newStr += c;
					}
				}
			} else {
				if (isCompressing) {
					isCompressing = false;
					newStr += Constants.base64chars.charAt(sameCounter);
					sameCounter = 1;
				}
				newStr += c;
			}
			
			/* Reset suppress compression value if it has been set */
			if (shouldSupressCompression) {
				shouldSupressCompression = false;
			}
		}
		return newStr;
	}

	/**
	 * Decompresses a string which has previously been compressed using the 
	 * compressString() utility. All omitted repeating characters are restored.
	 * @param str - the compressed String
	 * @return the decompressed String
	 */
	public static String decompressString(String str) {
		String newStr = "";

		boolean shouldDecompress = false;
		byte decompVal = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '.') {
				shouldDecompress = true;
				continue;
			}
			if (shouldDecompress) {
				if (decompVal == 0) {
					decompVal = (byte)(Constants.base64chars.indexOf(c));
				} else {
					for (int j = 0; j < decompVal; j++) {
						newStr += c;
					}
					shouldDecompress = false;
					decompVal = 0;
				}
			} else {
				newStr += c;
			}
		}
		
		if (newStr.length() != 284) {
			System.out.println("[decompressString] Warning: Invalid decompressed string " +
															"length: " + newStr.length());
		}
		return newStr;
	}
	

	/**
	 * Creates a Line Data string given a list of Line Data objects
	 * @param dataList - LineDataList containing line date to be used in creating the line
	 * 						data string.
	 * @return a String containing Line Data information
	 */
	public static String createLineDataString(LineDataList dataList) {
		String lineDataStr = "";
		for (int j = 0; j < dataList.data.size(); j++) {
			byte b[] = new byte[2];
			
			b[0] = (byte) ((dataList.data.get(j).trafficSB) % 7);
			b[1] = (byte) ((dataList.data.get(j).trafficNB) % 7);

			lineDataStr += Constants.base64chars.charAt(b[0]);
			lineDataStr += Constants.base64chars.charAt(b[1]);

			// System.out.println("Generated String: " + lineDataStr );
		}
		
		if (lineDataStr.length() != 284) {
			System.out.println("[createLineDataString] Warning: Possibly invalid line "   + 
															  	"data string w/ length: " + 
																     lineDataStr.length() + " !");
			System.out.println("[createLineDataString] 			> Data List Elements: " + 
																     dataList.data.size());
		}
		return lineDataStr;
	}

	/*****************************************************************************************/
	/** PRIVATE METHODS																		**/
	/*****************************************************************************************/
	/**
	 * Merges two Line Data strings together. This method is used for updating
	 * the encoded traffic data in the older string. 
	 * @param oldStr - the old line data string
	 * @param newStr - the new line data string
	 * @return the merged line data string or a blank string if the merge could
	 * 			not be performed
	 */
	private String mergeLineDataStrings(String oldStr, String newStr) {
		String mergedStr = "";
		byte oldByte = 0;
		byte newByte = 0;
		int i = 0;
		
		try {
			/* Check if the length of the new line data string is sane.
			 * Since there are usually 142 distinct lines, the length
			 * of this string should approximately be 284 characters since
			 * we are counting differently for both northbound and
			 * southbound traffic */
			if (newStr.length() != 284) {
				System.out.println("[mergeLineDataStrings] Warning: Weird line data string length: " + newStr.length());
				return mergedStr;
			}
	
			/* Check that both strings have the same length */
			if (oldStr.length() != newStr.length()) {
				return mergedStr;
			}
			
			for (i = 0; i < oldStr.length(); i++) {
				/* Get the old byte from the old string and shift it 
				 * to the left by two places. */
				oldByte = (byte) ((Constants.base64chars.indexOf(oldStr.charAt(i)) << 2) & 63);
				/* Get the new byte from the new string */
				newByte = (byte) (Constants.base64chars.indexOf(newStr.charAt(i)) & 3);
				
				/* Merge together the old and new bytes to generate 
				 * a new character which will be pushed into the
				 * merged string. */
				mergedStr += Constants.base64chars.charAt( ((oldByte + newByte) & 63) );
			}
		} catch (StringIndexOutOfBoundsException e) {
			if (!hasTriggeredDebugFlag) {
				System.out.println("StringIndexOutOfBoundsException occurred!");
				System.out.println("OldByte: " + oldByte + ", NewByte: " + newByte);
				System.out.println("Shifted: " + 
									(Constants.base64chars.indexOf(oldStr.charAt(i)) << 2));
				System.out.println("UnShifted: " + 
									(Constants.base64chars.indexOf(oldStr.charAt(i))));
				System.out.println("Original: " + oldStr.charAt(i));
				System.out.println("  OLD:" + oldStr);
				System.out.println("  NEW:" + newStr);
				System.out.println("MERGE:" + mergedStr);
				hasTriggeredDebugFlag = true;
				System.out.println("=========================================");
			}
			e.printStackTrace();
		}
		return mergedStr;
	}
	
	/**
	 * Adds a date string to the list of covered dates for the current
	 * parseTrafficDataFiles() operation.
	 * 
	 * TODO: Consider replacing this with a method that just draws all
	 * 		 dates from a HistData list instead.
	 * @param dateStr - the date string to be added to the list
	 */
	private void addToDatesCoveredList(String dateStr) {
		if (datesCoveredList.size() == 0) {
			System.out.println("Added to date list: " + dateStr);
			datesCoveredList.add(dateStr);
			return;
		}
		
		int insIdx = 0;
		int dateVal = Integer.parseInt(dateStr);
		int compVal = Integer.parseInt(datesCoveredList.get(0));
		
		int j = 1;
		
		while (dateVal > compVal) {
			insIdx = j;
			
			if (j >= datesCoveredList.size()) {
				insIdx = -1;
				break;
			}
			
			compVal = Integer.parseInt(datesCoveredList.get(j++));
		}
		
		if (insIdx >= 0) {
			datesCoveredList.add(insIdx, dateStr);
			System.out.println("Added to date list: " + dateStr);
		} else {
			datesCoveredList.add(dateStr);
			System.out.println("Added to date list: " + dateStr);
		}
	}

	/**
	 * Gets the proper data list insertion index for a new element with
	 * a particular timestamp string. This method is part of the mechanism
	 * which basically ensures that 
	 * @param timeStr
	 * @param dataList
	 * @return
	 */
	private static int getDataListInsertionIndex(String timeStr, List<LineInfo> dataList) {
		int timeVal = Integer.parseInt(timeStr);
		int guessVal = 0;
		
		int floorId = 0;
		int ceilId = dataList.size()-1;
		int guessId = ceilId;
		int insId = 0;
		
		if (dataList.size() > 0) {
			guessVal = Integer.parseInt(dataList.get(guessId).timestamp);
		} else {
			return -1;
		}
		
		if (timeVal > guessVal) {
			return -1;
		}
		
		while (dataList.size() > 0) {
			if (timeVal < guessVal) {
				if ((guessId-floorId) == 1) {
					if (Integer.parseInt(dataList.get(floorId).timestamp) > timeVal) {
						insId = floorId;
					} else {
						insId = guessId;
					}
					break; 
				}
				
				if (ceilId == floorId) {
					break;
				}
				
				ceilId = guessId;
				guessId = ((ceilId - floorId) / 2) + floorId;
				guessVal = Integer.parseInt(dataList.get(guessId).timestamp);
				
			} else { /* timeVal > guessVal */
				if ((ceilId-guessId) == 1) {
					if (Integer.parseInt(dataList.get(ceilId).timestamp) < timeVal) {
						insId = guessId;
					} else {
						insId = ceilId;
					}
					insId = ceilId;
					break; 
				}
				
				if (ceilId == floorId) {
					break;
				}
				
				floorId = guessId;
				guessId = (((ceilId - floorId) / 2) + floorId);
				guessVal = Integer.parseInt(dataList.get(guessId).timestamp);
				
			}
		}
		return insId;
	}

	/**
	 * Normalizes the traffic condition values inside a Line Data string.
	 * This method basically gets the average of all traffic condition
	 * values previously 'merged' into the line data string. This is
	 * usually done after parsing in multiple traffic data files and we
	 * want to obtain the average traffic condition to be used as reference
	 * for future merge-ins.
	 * @param lineDataStr - the line data string to be normalized
	 * @return the normalized line data string
	 */
	private String normalizeLineDataString(String lineDataStr) {
		String normalizedStr = "";
		int i = 0;
		
		/* Check if the length of the new line data string is sane.
		 * Since there are usually 142 distinct lines, the length
		 * of this string should approximately be 284 characters since
		 * we are counting differently for both northbound and
		 * southbound traffic */
		if (lineDataStr.length() != 284) {
			System.out.println("[normalizeLineDataString] Warning: Weird line data " + 
																   "string length: " + 
																   lineDataStr.length());
			return normalizedStr;
		}
		
		for (i = 0; i < lineDataStr.length(); i++) {
			byte b[] = new byte[4];
			
			int charIdx = Constants.base64chars.indexOf(lineDataStr.charAt(i));
			b[0] = (byte) ((charIdx >> 6) & 3);
			b[1] = (byte) ((charIdx >> 4) & 3);
			b[2] = (byte) ((charIdx >> 2) & 3);
			b[3] = (byte) (charIdx & 3);
			
			int weighedSum = 0;
			float finDiv = 0.0f;
			for (int j = 0; j < b.length; j++) {
				weighedSum += b[j] * (j+1);
				if (b[j] != 0) {
					finDiv += (float) (j+1);
				}
			}
			
			int finInt = Math.round(weighedSum / finDiv);
			
			normalizedStr += Constants.base64chars.charAt( finInt == 0 ? 1 : finInt );
		}
		return normalizedStr;
	}
	/*****************************************************************************************/
	/** UNUSED/RESERVED METHODS																**/
	/*****************************************************************************************/
	/**
	 * Unused utility function for extracting line data from a line data string
	 * @param lineDataStr - line data string from which line data will be extracted
	 * @return a list of LineData objects
	 */
	public List<LineData> extractLineData(String lineDataStr) {
		List<LineData> ld = new ArrayList<LineData>();

		short locCount = 0;

		int startIdx = 0;
		int endIdx = 2;
		
		String extStr = decompressString(lineDataStr);

		while (startIdx < extStr.length()) {
			/* Get two characters */
			String sub = extStr.substring(startIdx, endIdx);
			

			/* Convert them back to bytes */
			char c[] = new char[sub.length()];
			for (int i = 0; i < c.length; i++) {
				c[i] = sub.charAt(i);
			}
			
			byte sb = (byte)(Constants.base64chars.indexOf(c[0]));
			byte nb = (byte)(Constants.base64chars.indexOf(c[1]));
			
			ld.add(new LineData(locCount++, sb, nb));

			/* Increment the indices */
			startIdx += 2;
			endIdx += 2;

			if (startIdx >= extStr.length()) {
				break;
			}

			if (endIdx > extStr.length()) {
				endIdx = extStr.length();
			}

		}

		for (int i = 0; i < ld.size(); i++) {
			System.out.println(ld.get(i).toString());
		}

		return ld;
	}
}
