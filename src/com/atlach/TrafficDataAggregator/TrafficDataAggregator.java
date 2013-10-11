package com.atlach.TrafficDataAggregator;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.atlach.TrafficDataAggregator.DataObjects.*;

/**
 * <b>TrafficDataAggregator Class</b> </br>Handles the aggregation of Traffic
 * Data from generated Raw Traffic Data Files. Functionality is provided for the
 * following cases: </br>- (Re)generating Historical Data from a directory of
 * Raw Traffic Data Files </br>- Updating Historical Data from a directory of
 * Raw Traffic Data Files </br>- Push Traffic Data from a Raw Traffic Data File
 * into a Traffic Data Part File. </br>- Offload the data in a Part File into a
 * Historical Data File
 * 
 * @author francis
 * 
 */
public class TrafficDataAggregator {
	public static final int STATUS_OK = 0;
	public static final int STATUS_FAILED = -1;

	private DataFileManager dfm = null;
	private FileSystemInterface fsi = null;
	private String defaultHistDataDir = "Traffic_Records";
	private String histDataFileName = "TrafficData.hist";
	private String defaultPartFileName = "TrafficData.part";
	private String tagFileName = "tags.txt";
	private boolean useCompression = false;

	public TrafficDataAggregator() {
		dfm = new DataFileManager();
		fsi = new FileSystemInterface();
	}

	public TrafficDataAggregator(String histDataFile, String tagFile,
			boolean shouldCompress) {
		histDataFileName = histDataFile;
		tagFileName = tagFile;
		useCompression = shouldCompress;

		dfm = new DataFileManager();
		fsi = new FileSystemInterface();
	}

	/*****************************************************************************************/
	/** PUBLIC METHODS **/
	/*****************************************************************************************/
	/**
	 * Generates a new HistData file given the Traffic Data files stored in the
	 * directory specified by targetDir
	 * 
	 * @param targetDir
	 *            - the name of the directory where the Traffic Data files are
	 *            stored
	 * @return an integer indicating the exit status for this method
	 */
	public int generateHistDataFile(String targetDir) {
		System.out.println("[generateHistDataFile] Started (for Dir)");
		int result = STATUS_OK;
		List<HistData> tagCentricList = null;
		List<HistData> masterList = null;
		HistDataFileInfo hdInfo = null;

		/* Create the masterList */
		masterList = new ArrayList<HistData>();

		/*
		 * Attempt to obtain information about the pre-existing HistData file if
		 * it exists
		 */
		String datesCovered = "";
		hdInfo = fsi.getHistDataFileInfo(histDataFileName, useCompression);
		if (hdInfo != null) {
			datesCovered = hdInfo.datesCovered;
			hdInfo.printInfo();
		}

		/*
		 * Invoke the parsing of all non-aggregated traffic data files in the
		 * target dir
		 */
		result = dfm.parseTrafficDataFiles(targetDir, masterList, datesCovered);
		if (result != STATUS_OK) {
			System.out
					.println("[generateHistDataFile] Failed to parse traffic data files!");
			return STATUS_FAILED;
		}

		/*
		 * Normalize the line data strings for all HistData objects in the
		 * masterList
		 */
		/*
		 * NOTE: This is done since we want to condense the traffic data bytes
		 * into their rough average since we can store up to 4 instances of
		 * traffic info for each particular "line"/"location" per byte.
		 */
		result = dfm.normalizeHistDataList(masterList);
		if (result != STATUS_OK) {
			System.out
					.println("[generateHistDataFile] Failed to normalize HistData list!");
			return STATUS_FAILED;
		}

		/*
		 * Since our initial list is essentially date-based, we will have to do
		 * a second sorting in order to create the Tag-based list. In this step,
		 * we will merge together the information within HistData objects which
		 * have the same set of tags. This simply means that multiple dates may
		 * be merged in a single tagset.
		 */
		/* Create Tag Based List */
		tagCentricList = new ArrayList<HistData>();
		this.createTagCentricList(tagCentricList, masterList);

		/* Generate a list of tags based on our tagCentricList */
		List<String> parsedTagList = generateTagList(tagCentricList);

		/* Update the datesCovered value */
		datesCovered = updateDateRangeString(datesCovered,
				dfm.getDatesCoveredString());

		/* Reuse the HistDataFileInfo object from before */
		hdInfo = new HistDataFileInfo(datesCovered, parsedTagList);
		/*
		 * TODO: fsi.getDatesCoveredString() may be too reliant on
		 * parseTrafficDataFiles() being called before it...
		 */

		try {
			/* Prepare the HistData file we will be writing on */
			fsi.prepareHistDataFile(histDataFileName, hdInfo, useCompression);

			/*
			 * Cycle through each HistData object in the tagCentricList and save
			 * the information to the specified HistData file
			 */
			int i = 0;
			for (i = 0; i < tagCentricList.size(); i++) {
				HistData hd = tagCentricList.get(i);
				fsi.saveDataToFile(histDataFileName, hd.dataList, hd.tagset,
						true, useCompression);
			}
			System.out.println("INFO: A total of " + i
					+ " elements were changed.");
		} catch (IOException e) {
			e.printStackTrace();
			return STATUS_FAILED;
		}

		System.out
				.println("[generateHistDataFile] Finished Successfully (for Dir).");
		return STATUS_OK;
	}

	/**
	 * Updates an existing HistData file with new Traffic Data from the
	 * specified directory.
	 * 
	 * @param refList
	 *            - a reference HistData object list containing existing
	 *            Historical Data
	 * @param targetDir
	 *            - the name of the directory where the Traffic Data files are
	 *            stored
	 * @return an integer indicating the exit status for this method
	 */
	public int updateHistDataFile(List<HistData> refList, String targetDir) {
		System.out.println("[updateHistDataFile] Started (for Dir)");
		int result = STATUS_OK;
		FileSystemInterface fsi = new FileSystemInterface();
		List<HistData> masterList = null;
		HistDataFileInfo hdInfo = null;

		/* If the reference list is empty, return an error */
		if (refList == null) {
			/*
			 * TODO We might try incorporating reference list generation here as
			 * well
			 */
			System.out
					.println("ERROR: Cannot perform updates on an empty reference list!");
			return STATUS_FAILED;
		}

		/* Create the masterList */
		masterList = new ArrayList<HistData>();

		/*
		 * Attempt to obtain information about the pre-existing HistData file if
		 * it exists
		 */
		String datesCovered = "";
		hdInfo = fsi.getHistDataFileInfo(histDataFileName, useCompression);
		if (hdInfo != null) {
			datesCovered = hdInfo.datesCovered;
			hdInfo.printInfo();
		}

		/*
		 * Invoke the parsing of all non-aggregated traffic data files in the
		 * target dir
		 */
		result = dfm.parseTrafficDataFiles(targetDir, masterList, datesCovered);
		if (result != STATUS_OK) {
			System.out
					.println("[generateHistDataFile] Failed to parse traffic data files!");
			return STATUS_FAILED;
		}

		/*
		 * Normalize the line data strings for all HistData objects in the
		 * masterList
		 */
		/*
		 * NOTE: This is done since we want to condense the traffic data bytes
		 * into their rough average since we can store up to 4 instances of
		 * traffic info for each particular "line"/"location" per byte.
		 */
		result = dfm.normalizeHistDataList(masterList);
		if (result != STATUS_OK) {
			System.out
					.println("[generateHistDataFile] Failed to normalize HistData list!");
			return STATUS_FAILED;
		}

		/*
		 * Since our initial list is essentially date-based, we will have to do
		 * a second sorting in order to create the Tag-based list. In this step,
		 * we will merge together the information within HistData objects which
		 * have the same set of tags. This simply means that multiple dates may
		 * be merged in a single tagset.
		 */
		/* Create Tag Based List */
		createTagCentricList(refList, masterList);

		/* Generate the tag list */
		List<String> parsedTagList = generateTagList(refList);

		/* Update the datesCovered value */
		datesCovered = updateDateRangeString(datesCovered,
				dfm.getDatesCoveredString());

		/* Reuse the HistDataFileInfo object from before */
		hdInfo = new HistDataFileInfo(datesCovered, parsedTagList);
		/*
		 * TODO: fsi.getDatesCoveredString() may be too reliant on
		 * parseHistDataFiles() being called before it...
		 */

		try {
			/* Prepare the HistData file we will be writing on */
			fsi.prepareHistDataFile(histDataFileName, hdInfo, useCompression);

			/*
			 * Cycle through each HistData object in the tagCentricList and save
			 * the information to the specified HistData file
			 */
			int c = 0;
			for (int i = 0; i < refList.size(); i++) {
				if (refList.get(i).hasChanged == false) {
					continue;
				} else {
					System.out.println("> Incorporating changes for: "
							+ refList.get(i).tagset);
				}
				HistData hd = refList.get(i);
				fsi.saveDataToFile(histDataFileName, hd.dataList, hd.tagset,
						true, useCompression);
				c++;
			}

			System.out.println("INFO: A total of " + c
					+ " elements were changed.");
		} catch (IOException e) {
			e.printStackTrace();
			return STATUS_FAILED;
		}

		System.out
				.println("[updateHistDataFile] Finished Successfully (for Dir).");
		return STATUS_OK;
	}

	/**
	 * Updates a HistData file with new information drawn from a Part File.
	 * </br> <i>(This is mostly used for continuously updating the running Part
	 * File with intermediate Traffic Data as the day progresses)</i>
	 * 
	 * @param partFileName
	 *            - the name of the Part File
	 * @param partFileTag
	 *            - the tag to use for this Part File
	 * @param partFileDate
	 *            - the date associated with this Part File
	 * @return an integer indicating the exit status for this method
	 */
	public int updateHistDataFile(String partFileName, String partFileTag,
			String partFileDate) {
		System.out.println("[updateHistDataFile] Started (for Part File)");
		FileSystemInterface fsi = new FileSystemInterface();
		HistDataFileInfo hdInfo = null;

		/*
		 * Attempt to obtain information about the pre-existing HistData file if
		 * it exists NOTE: It SHOULD exist if we're going to update it with a
		 * _part_
		 */
		String datesCovered = "";
		hdInfo = fsi.getHistDataFileInfo(histDataFileName, useCompression);
		if (hdInfo != null) {
			datesCovered = hdInfo.datesCovered;
		} else {
			System.out.println("WARNING: HistData file does not yet exist!");
			System.out.println("INFO: Attempting to generate HistData file...");
			/* Attempt to generate the hist file */
			if (generateHistDataFile(defaultHistDataDir) != STATUS_OK) {
				System.out.println("ERROR: Failed to generate HistData file!");
				return STATUS_FAILED;
			}

			/* Attempt to obtain the HistData file info again */
			hdInfo = fsi.getHistDataFileInfo(histDataFileName, useCompression);
			if (hdInfo == null) {
				System.out.println("ERROR: HistData file!");
				return STATUS_FAILED;
			}
			datesCovered = hdInfo.datesCovered;
		}

		/* Create the hdTagList */
		/*
		 * If the tag for the part file, does not yet exist, then add it to the
		 * hdTagList. Otherwise, just pass the same list on.
		 */
		List<String> hdTagList = hdInfo.tagList;
		boolean isDistinctTag = true;
		for (int i = 0; i < hdTagList.size(); i++) {
			if (hdTagList.get(i).equals(partFileTag)) {
				isDistinctTag = false;
				break;
			}
		}
		if (isDistinctTag) {
			hdTagList.add(partFileTag);
		}

		/* Update the datesCovered value */
		datesCovered = updateDateRangeString(datesCovered, partFileDate);

		/* Reuse the HistDataFileInfo object from before */
		hdInfo = new HistDataFileInfo(datesCovered, hdTagList);
		/*
		 * TODO: fsi.getDatesCoveredString() may be too reliant on
		 * parseHistDataFiles() being called before it...
		 */

		try {
			HistData oldHistData = null;
			HistData newHistData = new HistData("UNKNOWN", partFileTag);
			/* Now, load the part file */
			newHistData.dataList = fsi.loadPartDataFile(partFileName);
			if (newHistData.dataList == null) {
				System.out
						.println("[updateHistDataFile] ERROR: Failed to load specified"
								+ " Part File!");
				return STATUS_FAILED;
			}

			/* Attempt to reduce the data loaded from the part file */
			newHistData = dfm.reducePartHistData(newHistData);

			/*
			 * If this tag is not distinct, then we must merge it with old
			 * historical data
			 */
			if (!isDistinctTag) {
				/* Load old historical data associated with this tag from a file */
				oldHistData = fsi.loadHistDataTagFromFile(histDataFileName,
						partFileTag, useCompression);

				if (oldHistData == null) {
					System.out
							.println("[updateHistDataFile] ERROR: Failed to load Tag from "
									+ "Hist Data File!");
					return STATUS_FAILED;
				}
				/* Merge the old historical data with the new one */
				dfm.mergeHistData(oldHistData, newHistData);
			}

			/* Set the tagset for the newHistData object */
			newHistData.tagset = partFileTag;

			/* Prepare the HistData file we will be writing on */
			fsi.prepareHistDataFile(histDataFileName, hdInfo, useCompression);

			/* Save the data to the HistData file */
			fsi.saveDataToFile(histDataFileName, newHistData.dataList,
					newHistData.tagset, true, useCompression);
		} catch (IOException e) {
			e.printStackTrace();
			return STATUS_FAILED;
		}

		System.out
				.println("[updateHistDataFile] Finished Successfully (for Part File).");
		return STATUS_OK;
	}

	/**
	 * Pushes Traffic Data from the regular-format .csv file produced by
	 * TrafficDataIndexer to a Part File maintained by the
	 * TrafficDataAggregator. This is the compact version of this method,
	 * filling in the name of the Part File with the default Part File name.
	 * </br> <i>(This is mostly used for continuously updating the running Part
	 * File with intermediate Traffic Data as the day progresses.)</i>
	 * 
	 * @param trafficDataFileName
	 *            - the name of the target Traffic Data file produced by
	 *            TrafficDataIndexer
	 * @param timestamp
	 *            - the time to associate with this data part
	 * @param dataTag
	 *            - the tag to associate with this data part
	 * @return an integer indicating the exit status for this method
	 */
	public int pushTrafficData(String trafficDataFileName, String timestamp,
			String dataTag) {
		return pushTrafficData(trafficDataFileName, timestamp, dataTag,
				defaultPartFileName);
	}

	/**
	 * Pushes Traffic Data from the regular-format .csv file produced by
	 * TrafficDataIndexer to a Part File maintained by the
	 * TrafficDataAggregator. This is the full version of this method, having
	 * the Part File name as an additional argument </br> <i>(This is mostly
	 * used for continuously updating the running Part File with intermediate
	 * Traffic Data as the day progresses.)</i>
	 * 
	 * @param trafficDataFileName
	 *            - the name of the target Traffic Data file produced by
	 *            TrafficDataIndexer
	 * @param timestamp
	 *            - the time to associate with this data part
	 * @param dataTag
	 *            - the tag to associate with this data part
	 * @param partFileName
	 *            - the name of the target the Part File
	 * @return an integer indicating the exit status for this method
	 */
	public int pushTrafficData(String trafficDataFileName, String timestamp,
			String dataTag, String partFileName) {
		System.out.println("[pushTrafficData] Started");
		int result = STATUS_FAILED;
		FileSystemInterface fsi = new FileSystemInterface();

		/* Load line data from the specified traffic data file */
		LineDataList dataList = fsi.loadTrafficDataFile(trafficDataFileName);

		/* Associate the line data with a specific time value */
		dataList.timestamp = timestamp;

		/* Check if we should attempt to update the tags file */
		/* Updates at: 2AM, 8AM, 2PM, 8PM */
		int hour = Integer.parseInt(dataList.timestamp.substring(0, 2));

		System.out
				.println("[pushTrafficData] Checking if tag file should be updated "
						+ "(timestamp = " + timestamp + ")...");
		if (((hour % 6) - 2) == 0) {
			System.out
					.println("[pushTrafficData] Conditions are right. Updating tag file...");
			result = pushTagFileUpdate();
			if (result != STATUS_OK) {
				System.out
						.println("[pushTrafficData] Error: Failed to update the tag file!");
			}
			System.out.println("[pushTrafficData] Tag file update successful.");
		}

		/* Offload previous day's data */
		if (hour == 0) {
			System.out
					.println("[pushTrafficData] Attempting to offload previous day's data...");
			result = offloadCollectedData(partFileName, "", fsi);
			if (result != STATUS_OK) {
				System.out
						.println("[pushTrafficData] Error: Failed to offload data!");
				return STATUS_FAILED;
			}
			System.out.println("[pushTrafficData] Data offload successful.");
		}

		System.out.println("[pushTrafficData] Saving Part Data to File...");
		/* Save obtained data to the part file */
		result = dfm.savePartialData(partFileName, dataList);

		System.out
				.println("[pushTrafficData] Finished. Exit Status: " + result);
		return result;
	}

	/**
	 * Offloads data collected in the Part File into the HistData file. This is
	 * the compact version of this method, disallowing the option to force
	 * offloading of partial data.
	 * 
	 * @param partFileName
	 *            - the name of the Part File to offload into the HistData file
	 * @param dataTag
	 *            - the tag to associate with this Part File's data
	 * @param fsi
	 *            - the FileSystemInterface object to use for the offload
	 *            transaction TODO: This may no longer be necessary since we
	 *            already instantiated a FileSystemInterface object when the
	 *            TrafficDataAggregator was instantiated.
	 * @return an integer indicating the exit status for this method
	 */
	public int offloadCollectedData(String partFileName, String dataTag,
			FileSystemInterface fsi) {
		return offloadCollectedData(partFileName, dataTag, fsi, false);
	}

	/**
	 * Offloads data collected in the Part File into the HistData file. This is
	 * the full version of this method, allowing the option to force offloading
	 * of partial data.
	 * 
	 * @param partFileName
	 *            - the name of the Part File to offload into the HistData file
	 * @param dataTag
	 *            - the tag to associate with this Part File's data
	 * @param fsi
	 *            - the FileSystemInterface object to use for the offload
	 *            transaction TODO: This may no longer be necessary since we
	 *            already instantiated a FileSystemInterface object when the
	 *            TrafficDataAggregator was instantiated.
	 * @param forceOffload
	 *            - a boolean indicating whether data offload should be forced
	 * @return an integer indicating the exit status for this method
	 */
	public int offloadCollectedData(String partFileName, String dataTag,
			FileSystemInterface fsi, boolean forceOffload) {
		System.out.println("[offloadCollectedData] Started");
		int result = STATUS_FAILED;
		int checkResult = 0;

		if (!forceOffload) {
			/*
			 * Check whether it is sane for us to offload data from the part
			 * file
			 */
			try {
				checkResult = fsi.checkPartFileTimestamps(partFileName);
			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 * i.e. If we have less than 16 (2/3rds of the day) complete
			 * records, then we might not have sufficient collected data to
			 * offload
			 */
			if (checkResult < 16) {
				System.out
						.println("[offloadCollectedData] Error: Not enough data to "
								+ "offload (" + checkResult + ")!");
				return STATUS_FAILED;
			}
		}

		/* Prepare the necessary part file information */
		String partFileDate = getPreviousDayDate();
		String partFileTag = dataTag;
		if (dataTag.equals("")) {
			try {
				partFileTag = fsi.getTagForDate(tagFileName, partFileDate);

				if ((partFileTag == null) || (partFileTag.equals(""))) {
					System.out
							.println("[offloadCollectedData] Warning: No associated tags could"
									+ " be found!");

					/* Use the default tagging scheme instead: */
					/* i.e. <Day of Week Type>|<Day of Week Name> */
					partFileTag = getDefaultFileTag();
					System.out
							.println("[offloadCollectedData] Using default tag: "
									+ partFileTag);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out
				.println("[offloadCollectedData] Updating HistData file with previous day's"
						+ " Part File data...");
		/* Update the Historical Data file with the Part File information */
		result = updateHistDataFile(partFileName, partFileTag, partFileDate);

		System.out.println("[offloadCollectedData] Deleting old part file...");
		/* Delete the part file upon offload */
		File partFile = new File(partFileName);
		if (partFile.exists()) {
			partFile.delete();
		}

		System.out.println("[offloadCollectedData] Finished. Exit Status: "
				+ result);
		return result;
	}

	/**
	 * Updates the tag file used by the Aggregator with the current day's tag
	 * inferred from the current date and the current weather conditions.
	 * 
	 * @return an integer indicating the exit status for this method
	 */
	public int pushTagFileUpdate() {
		System.out.println("[pushTagFileUpdate] Started");
		int result = STATUS_OK;

		List<TagInfo> tagInfoList = null;
		try {
			tagInfoList = fsi.loadTagsetFile(tagFileName);
		} catch (IOException e) {
			e.printStackTrace();
			return STATUS_FAILED;
		}

		/* Get the date string for today */
		String todayStr = getCurrentDateString();

		/* Check if there is already an entry for this in the loaded tags */
		int i = 0;
		for (i = 0; i < tagInfoList.size(); i++) {
			if (tagInfoList.get(i).date.equals(todayStr)) {
				/* save the current index */
				break;
			}
		}

		if (i == tagInfoList.size()) {
			/* This means that there wasn't a tag entry for this date before */
			/* Get the tag string for today */
			String defTag = getDefaultFileTag(todayStr);
			String weatherTag = getWeatherTag();

			tagInfoList
					.add(new TagInfo(todayStr, (defTag + ", " + weatherTag)));
		} else {
			/*
			 * Otherwise, extract the old weather tag and see if it should be
			 * "upgraded" with the current weather information
			 */
			String defTag = getDefaultFileTag(todayStr);
			String weatherTag = getWeatherTag();
			String currentTag = tagInfoList.get(i).tagset;
			String newWeatherTag = "retain";

			if (weatherTag.contains("Rain")) {
				if (currentTag.contains("Rain")) {
					if (currentTag.contains("Moderate")
							&& (weatherTag.contains("Heavy") || weatherTag
									.contains("Storm"))) {
						newWeatherTag = weatherTag;
					} else if (currentTag.contains("Heavy")
							&& weatherTag.contains("Storm")) {
						newWeatherTag = weatherTag;
					}
				} else if (currentTag.contains("Snow")) {
					newWeatherTag = "retain";
				} else {
					newWeatherTag = weatherTag;
				}
			} else if (weatherTag.contains("Overcast")) {
				if (currentTag.contains("Rain") || currentTag.contains("Snow")) {
					newWeatherTag = "retain";
				} else {
					newWeatherTag = weatherTag;
				}
			}

			if (newWeatherTag.contains("retain") == false) {
				tagInfoList.get(i).tagset = (defTag + ", " + newWeatherTag);
			}
		}

		/* Save new tag file */
		try {
			result = fsi.saveTagFile(tagFileName, tagInfoList);
			if (result != STATUS_OK) {
				System.out
						.println("[pushTagFileUpdate] Failed to save tag file!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("[pushTagFileUpdate] Finished. Exit Status: "
				+ result);
		return result;
	}

	/*****************************************************************************************/
	/** PRIVATE FUNCTIONS **/
	/*****************************************************************************************/
	private static final String dayOfWeekStr[] = { "Unknown-day", "Sunday",
			"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

	/**
	 * Gets the current date string, specific to GMT+8
	 * 
	 * @return the current date string
	 */
	private String getCurrentDateString() {
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		Date date = Calendar.getInstance(tz).getTime();

		return new SimpleDateFormat("yyyyMMdd").format(date);
	}

	/**
	 * Obtains a tag describing the current weather conditions according to data
	 * from WorldWeatherOnline.com.
	 * 
	 * @return the weather condition tag string
	 */
	private String getWeatherTag() {
		WeatherDataManager wdm = new WeatherDataManager();

		String weatherStr = wdm.sendWeatherDataRequest("Manila", 1);

		return ("Weather|" + wdm.extractCurrentWeatherCondition(weatherStr));
	}

	/**
	 * Attempts to get the default tag to be used with the current Part Data
	 * file to be offloaded.
	 * 
	 * @return a tag string
	 */
	private String getDefaultFileTag() {
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		int day = Calendar.getInstance(tz).get(Calendar.DAY_OF_WEEK);

		if (day <= 1) {
			day = 7;
		} else {
			day--;
		}

		String dayType = "Weekend";

		if ((day > 1) && (day < 7)) {
			dayType = "Weekday";
		}

		return (dayType + "|" + dayOfWeekStr[day]);
	}

	/**
	 * Attempts to get the default tag to be used for a specific date
	 * 
	 * @return a tag string
	 */
	private String getDefaultFileTag(String dateStr) {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		Date date = null;
		try {
			date = format.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		cal.setTime(date);

		int day = cal.get(Calendar.DAY_OF_WEEK);

		String dayType = "Weekend";

		if ((day > 1) && (day < 7)) {
			dayType = "Weekday";
		}

		return (dayType + "|" + dayOfWeekStr[day]);
	}

	/**
	 * Gets the date of the previous day. This is mostly used during Part Data
	 * File offload.
	 * 
	 * @return the previous day's date String
	 */
	private String getPreviousDayDate() {
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		Date date = Calendar.getInstance(tz).getTime();

		String initDateStr = new SimpleDateFormat("yyyy/MM/dd").format(date);
		String initDateSplit[] = initDateStr.split("/");

		int year = Integer.parseInt(initDateSplit[0]);
		int month = Integer.parseInt(initDateSplit[1]);
		int day = Integer.parseInt(initDateSplit[2]);

		if (day == 1) {
			if (month == 1) {
				month = 12;
				year--;
			} else {
				month--;
			}
			day = 31;
		} else {
			day--;
		}

		String dayStr = ((day > 9) ? (Integer.toString(day)) : ("0" + day));
		String monthStr = ((month > 9) ? (Integer.toString(month))
				: ("0" + month));
		String yearStr = (Integer.toString(year));

		return (yearStr + "" + monthStr + "" + dayStr);
	}

	/**
	 * Updates the given date range string with a new date range. This is mostly
	 * used to update the "DatesCovered" tag used in the header of Historical
	 * Data Files.
	 * 
	 * @param oldDateRange
	 * @param newDateRange
	 * @return
	 */
	private String updateDateRangeString(String oldDateRange,
			String newDateRange) {
		String dateRangeStr = "";

		/*
		 * Combine the oldDateRange and newDateRange strings, and then
		 * deconstruct the result into a string array
		 */
		String dateRangeArr[] = (oldDateRange + "," + newDateRange).split(",");

		/* */
		for (int i = 0; i < dateRangeArr.length; i++) {
			if (dateRangeArr[i].equals("")) {
				continue;
			}

			/* Case for when this is a range string */
			if (dateRangeArr[i].contains("-")) {
				String rangeStr[] = dateRangeArr[i].split("-");
				int highRange = Integer.parseInt(rangeStr[1].trim());
				int lowRange = Integer.parseInt(rangeStr[0].trim());

				for (int j = 0; j < dateRangeArr.length; j++) {
					if (i == j) {
						continue;
					}

					String result[] = adjustDateRange(dateRangeArr[j],
							rangeStr, highRange, lowRange);

					/* Apply the results */
					rangeStr[0] = result[0];
					rangeStr[1] = result[1];
					dateRangeArr[j] = result[2];
				}

				dateRangeArr[i] = (rangeStr[0] + "-" + rangeStr[1]);

				/* Case for when this is a single date string */
			} else {
				int dateVal = Integer.parseInt(dateRangeArr[i].trim());
				for (int j = 0; j < dateRangeArr.length; j++) {
					if (i == j) {
						continue;
					}

					/* Case I: Date vs Date Range */
					if (dateRangeArr[j].contains("-")) {
						String rangeStr[] = dateRangeArr[j].split("-");
						int highRange = Integer.parseInt(rangeStr[1].trim());
						int lowRange = Integer.parseInt(rangeStr[0].trim());

						if ((dateVal <= highRange) && (dateVal >= lowRange)) {
							dateRangeArr[i] = "";
							break;
						}

						if (dateVal == highRange + 1) {
							dateRangeArr[j] = rangeStr[0] + "-"
									+ dateRangeArr[i];
							dateRangeArr[i] = "";
							break;
						}

						if (dateVal == lowRange + 1) {
							dateRangeArr[j] = dateRangeArr[i] + "-"
									+ rangeStr[1];
							dateRangeArr[i] = "";
							break;
						}
						/* Case II: Date vs Date */
					} else if (dateRangeArr[j].equals("") == false) {
						int compVal = Integer.parseInt(dateRangeArr[j].trim());
						if (dateVal == compVal) {
							dateRangeArr[i] = "";
							break;
						}
						if (dateVal == compVal + 1) {
							dateRangeArr[j] = (dateRangeArr[j] + "-" + dateRangeArr[i]);
							dateRangeArr[i] = "";
							break;
						}
						if (dateVal == compVal - 1) {
							dateRangeArr[j] = (dateRangeArr[i] + "-" + dateRangeArr[j]);
							dateRangeArr[i] = "";
							break;
						}
					}
				}
			}

		}

		for (int i = 0; i < dateRangeArr.length; i++) {
			if (dateRangeArr[i].equals("")) {
				continue;
			}
			dateRangeStr += dateRangeArr[i] + ",";
		}

		return (dateRangeStr.charAt(dateRangeStr.length() - 1) != ',' ? dateRangeStr
				: dateRangeStr.substring(0, dateRangeStr.length() - 1));
	}

	/**
	 * Adjusts an intermediate date range string by attempting to merge it with
	 * another date range string. Forgot exactly how this works...
	 * 
	 * @param compStr
	 *            - the date range string which we will attempt to merge to the
	 *            target date range string
	 * @param rangeStr
	 *            - an array containing info about the date range string to
	 *            merge to
	 * @param highRange
	 *            - the upper range of the target date range string
	 * @param lowRange
	 *            - the lower range of the target date range string
	 * @return
	 */
	private String[] adjustDateRange(String compStr, String rangeStr[],
			int highRange, int lowRange) {
		String newRangeArr[] = { rangeStr[0], rangeStr[1], compStr };

		/* Case I: Date Range vs Date Range */
		if (compStr.contains("-")) {
			String innerRangeStr[] = compStr.split("-");
			int innerHighRange = Integer.parseInt(innerRangeStr[1].trim());
			int innerLowRange = Integer.parseInt(innerRangeStr[0].trim());

			/* Check for overlap */
			if ((innerHighRange > lowRange) && (innerHighRange < highRange)) {
				if (innerLowRange < lowRange) {
					lowRange = innerLowRange;
					newRangeArr[0] = innerRangeStr[0];
				}
				newRangeArr[1] = rangeStr[1];
				newRangeArr[2] = "";
				return newRangeArr;
			}

			/* Check for overlap */
			if ((innerLowRange < highRange) && (innerLowRange > lowRange)) {
				if (innerHighRange > highRange) {
					highRange = innerHighRange;
					newRangeArr[1] = innerRangeStr[1];
				}
				newRangeArr[0] = rangeStr[0];
				newRangeArr[2] = "";
				return newRangeArr;
			}

			/* Consider near overlaps too */
			if (innerHighRange + 1 == lowRange) {
				lowRange = innerLowRange;
				newRangeArr[0] = innerRangeStr[0];
				newRangeArr[1] = rangeStr[1];
				newRangeArr[2] = "";
				return newRangeArr;
			}

			/* Consider near overlaps too */
			if (innerLowRange - 1 == highRange) {
				highRange = innerHighRange;
				newRangeArr[1] = innerRangeStr[1];
				newRangeArr[0] = rangeStr[0];
				newRangeArr[2] = "";
				return newRangeArr;
			}

			/* Case II: Date Range vs Date */
		} else if (compStr.equals("") == false) {
			int dateVal = Integer.parseInt(compStr.trim());
			if ((dateVal >= lowRange) && (dateVal <= highRange)) {
				newRangeArr[0] = rangeStr[0];
				newRangeArr[1] = rangeStr[1];
				newRangeArr[2] = "";
				return newRangeArr;
			}
			if (dateVal == lowRange - 1) {
				lowRange = dateVal;
				newRangeArr[0] = compStr;
				newRangeArr[1] = rangeStr[1];
				newRangeArr[2] = "";
				return newRangeArr;
			}
			if (dateVal == highRange + 1) {
				highRange = dateVal;
				newRangeArr[0] = rangeStr[0];
				newRangeArr[1] = compStr;
				newRangeArr[2] = "";
				return newRangeArr;
			}
		}
		return newRangeArr;
	}

	/**
	 * Creates a List of HistData objects classified according to their tags
	 * instead of dates. This method attempts to merge together HistData objects
	 * with different dates but having the same tag sets.
	 * 
	 * @param tagCentricList
	 *            - an empty List of HistData objects which will be classified
	 *            by tag
	 * @param baseList
	 *            - the original List of HistData objects classified by date
	 */
	private void createTagCentricList(List<HistData> tagCentricList,
			List<HistData> baseList) {
		if ((baseList == null) || (baseList.size() == 0)) {
			System.out
					.println("[createTagCentricList] ERROR: Invalid baseList!");
			return;
		}

		if (tagCentricList == null) {
			System.out.println("Invalid tagCentricList!");
			return;
		}

		for (int i = 0; i < baseList.size(); i++) {
			String insTagset = baseList.get(i).tagset;
			boolean hasMerged = false;
			for (int j = 0; j < tagCentricList.size(); j++) {
				String compTagset = tagCentricList.get(j).tagset;

				if (insTagset == null) {
					System.out.println("inb4:" + baseList.get(i).date);
					baseList.get(i).tagset = getDefaultFileTag(baseList.get(i).date);
					insTagset = baseList.get(i).tagset;
				}

				if (insTagset.equals(compTagset)) {
					/* Merge instead of adding */
					dfm.mergeHistData(baseList.get(i), tagCentricList.get(j));
					tagCentricList.get(j).hasChanged = true;
					hasMerged = true;
					System.out.println("Set to hasChanged: "
							+ tagCentricList.get(j).tagset + "/"
							+ tagCentricList.get(j).date);
				}
			}
			if (!hasMerged) {
				HistData hd = baseList.get(i);

				if (insTagset == null) {
					System.out.println("inb4:" + baseList.get(i).date);
					baseList.get(i).tagset = getDefaultFileTag(baseList.get(i).date);
					insTagset = baseList.get(i).tagset;
				}

				System.out.println("Set to hasChanged: " + hd.tagset + "/"
						+ hd.date);
				hd.hasChanged = true;
				hd.date = "UNKNOWN";
				tagCentricList.add(hd);
			}
		}
	}

	/**
	 * Generates a list of tags given a List of HistData objects
	 * 
	 * @param dataList
	 *            - the List of HistData objects to be used as reference
	 * @return a List of Strings containing tags
	 */
	private static List<String> generateTagList(List<HistData> dataList) {
		List<String> parsedTagList = new ArrayList<String>();

		if ((dataList == null) || (dataList.size() == 0)) {
			System.out.println("[generateTagList] ERROR: Invalid dataList!");
			return null;
		}

		for (int i = 0; i < dataList.size(); i++) {
			parsedTagList.add(dataList.get(i).tagset);
		}

		return parsedTagList;
	}

	/* [BEGIN] This section retained for testing purposes */
	// public static void main(String[] args) {
	// FileSystemInterface fsi = new FileSystemInterface();
	// TrafficDataAggregator tda = new TrafficDataAggregator();
	//
	// tda.pushTagFileUpdate();
	// DataFileManager dfm = new DataFileManager();
	// int result;
	//
	// result = tda.generateHistDataFile(tda.defaultHistDataDir);
	// if (result != STATUS_OK) {
	// System.out.println("ERROR: Failed to regen Hist Data File!");
	// return;
	// }
	//
	// System.out.println("========================================================================================================");
	// List<HistData> tagCentricList = new ArrayList<HistData>();
	//
	// HistDataFileInfo hdInfo = fsi.getHistDataFileInfo("TrafficData.hist",
	// false);
	// if (hdInfo != null) {
	// hdInfo.printInfo();
	// }
	//
	// try {
	// /* Attempts to load the HistData file into the provided list */
	// /*fsi.loadHistDataFile("Example.txt", tagCentricList, false);*/
	// fsi.loadHistDataFile("TrafficData.hist", tagCentricList, false);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// for (int i = 0; i < tagCentricList.size(); i++) {
	// HistData hd = tagCentricList.get(i);
	// System.out.println("> Tagset: " + hd.tagset);
	// for (int j = 0; j < hd.dataList.size(); j++) {
	// LineInfo li = hd.dataList.get(j);
	//
	// if ((hd.tagset.contains("Weekday|Wednesday, Weather|Clear|Hot")) &&
	// (li.timestamp.equals("1200"))) {
	// /* System.out.println(">" + li.timestamp + ":" +
	// FileSystemInterface.decompressString(li.lineDataStr));*/
	// dfm.extractLineData(li.lineDataStr);
	// }
	// }
	// }
	// System.out.println("========================================================================================================");
	// for (int i = 0; i < 10; i++) {
	// LineDataList ldList = fsi.loadTrafficDataFile("TrafficRec.txt");
	// ldList.timestamp = "0" + i +"00";
	// result = dfm.savePartialData("TrafficData.part", ldList);
	// if (result != STATUS_OK) {
	// return;
	// }
	// }
	// for (int i = 0; i < 10; i++) {
	// LineDataList ldList = fsi.loadTrafficDataFile("TrafficRec.txt");
	// ldList.timestamp = "1" + i +"00";
	// result = dfm.savePartialData("TrafficData.part", ldList);
	// if (result != STATUS_OK) {
	// return;
	// }
	// }
	// System.out.println("========================================================================================================");
	// result = tda.updateHistDataFile("TrafficData.part",
	// "Weekday|Wednesday, Weather|Clear|Hot", "20130926", true);
	// if (result != STATUS_OK) {
	// return;
	// }
	// tda.offloadCollectedData("TrafficData.part", "", fsi);
	// }
	/* [END] This section retained for testing purposes */
}
