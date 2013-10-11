package com.atlach.TrafficDataIndexer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
 * <b>TrafficDataManager Class</b> </br>Used for managing the update of Traffic
 * Data from the MMDA Website
 * 
 * @author francis
 * @author zara
 * 
 */
public class TrafficDataManager {
	private TrafficDataIndexerNotifier eventIndexerNotif = null;
	private boolean shouldStopReading = false;

	public static final int STATUS_OK = 0;
	public static final int STATUS_FAILED = -1;

	private ArrayList<MonitoredLocation> monitoredLocList = null;

	public TrafficDataManager(TrafficDataIndexerEvent evM,
			TrafficDataIndexerNotifier evN) {
		System.out.println("TrafficDataManager Constructor called.");
		monitoredLocList = new ArrayList<MonitoredLocation>();
		eventIndexerNotif = evN;
	}

	/*****************************************************************************************/
	/** PUBLIC METHODS **/
	/*****************************************************************************************/
	/**
	 * Obtains the traffic data for all known lines monitored by MMDA
	 * 
	 * @return an ArrayList of MonitoredLocation objects containing Traffic Data
	 * @throws Exception
	 */
	public ArrayList<MonitoredLocation> getAllLineTrafficData()
			throws Exception {
		String areaName[] = { "EDSA", "COMMONWEALTH", "QUEZON AVE", "ESPANA",
				"C5", "ORTIGAS", "MARCOS HIGHWAY", "ROXAS BLVD", "SLEX" };
		String lvNames[] = { "edsa", "commonwealth", "quezon-ave", "espana",
				"c5", "ortigas", "marcos-highway", "roxas-blvd", "slex" };

		monitoredLocList = new ArrayList<MonitoredLocation>();

		shouldStopReading = false;

		for (int i = 0; i < lvNames.length; i++) {
			extractLineTrafficData(lvNames[i], areaName[i]);
			if (shouldStopReading) {
				break;
			}
		}

		if (shouldStopReading == false) {
			/* Call Save Data To File here as well */
			saveTrafficDataToFile();
		}

		return monitoredLocList;
	}

	/**
	 * Stops all ongoing read operations by setting a flag
	 */
	public void stopReadOperations() {
		shouldStopReading = true;
	}

	/*****************************************************************************************/
	/** PRIVATE METHODS **/
	/*****************************************************************************************/
	/**
	 * Extracts line traffic data from the given MMDA Website Line View URL
	 * 
	 * @param lineViewUrl
	 *            - the line view URL from which traffic data will be extracted
	 * @param areaName
	 *            - the name of the area to associate with traffic data obtained
	 * @return an integer indicating the exit status for this method
	 * @throws IOException
	 */
	private int extractLineTrafficData(String lineViewUrl, String areaName)
			throws IOException {
		System.out
				.println("extractLineTrafficData() called for " + lineViewUrl);
		eventIndexerNotif.onStatusUpdate("Downloading line view for "
				+ lineViewUrl + "...");

		URL url = new URL("http://mmdatraffic.interaksyon.com/line-view-"
				+ lineViewUrl + ".php");
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(60000);
		connection.setReadTimeout(30000);

		BufferedReader rd = null;

		try {
			rd = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String line;
			String lineName = "";
			int lineCond = 0;
			int lineCondSB = 0;
			int lineCondNB = 0;
			boolean isGettingSB = true;

			while ((line = rd.readLine()) != null) {
				if (shouldStopReading) {
					System.out
							.println("[extractLineTrafficData] Read Interrupted.");
					break;
				}

				String pattern = new String("line-name\"><p>");
				if ((line.contains(pattern) == true)
						&& line.contains("<a") == true) {

					int startIdx = line.indexOf("<p>") + 3;
					int endIdx = line.indexOf("<a");

					if ((endIdx >= line.length()) || (startIdx > endIdx)
							|| (startIdx < 0) || (endIdx < 0)) {

					} else {
						// balintawak, kaingin road etc etc
						lineName = line.substring(startIdx, endIdx)
								.replace("��", "n").replace("ñ", "n");
						continue;
					}
				}

				if (line.contains("<div class=\"light\">") == true) {
					lineCond = 1;
				} else if (line.contains("<div class=\"mod\">") == true) {
					lineCond = 2;
				} else if (line.contains("<div class=\"heavy\">") == true) {
					lineCond = 3;
				}

				if (lineCond != 0) {
					if (isGettingSB) {
						lineCondSB = lineCond;
						isGettingSB = false;
						lineCond = 0;
					} else if (!isGettingSB) {
						lineCondNB = lineCond;
						isGettingSB = true;
						lineCond = 0;
					}
				}

				if ((lineCondSB != 0) && (lineCondNB != 0)
						&& (lineName.equals("") == false)) {

					MonitoredLocation tempmonitoredLocList = new MonitoredLocation(
							lineName, areaName, lineCondSB, lineCondNB);
					monitoredLocList.add(tempmonitoredLocList);

					/* Nullify */
					lineName = "";
					lineCond = 0;
					lineCondSB = 0;
					lineCondNB = 0;
				}
			}
		} catch (SocketTimeoutException sockEx) {
			/* This timed out unfortunately */
			System.out.println("Connection Timed Out.");
			eventIndexerNotif.onStatusUpdate("Connection Timed Out for "
					+ lineViewUrl);
		} finally {
			if (rd != null) {
				rd.close();
			}
		}
		return STATUS_OK;
	}

	/**
	 * Saves collected traffic data to a Raw Traffic Data File. This outer
	 * method handles exceptions resulting from the actual save operation in
	 * SaveToFile(). </br> </br>The general format for this file is as follows:
	 * </br><i>[Area], [Location], [Southbound Traffic], [Northbound
	 * Traffic]</i> </br>... </br> </br>*where 1=light traffic, 2=moderate
	 * traffic, 3=heavy traffic
	 * 
	 * @return
	 */
	private int saveTrafficDataToFile() {
		System.out.println("saveTrafficDataToFile() called.");
		eventIndexerNotif.onStatusUpdate("Saving Data to File...");
		String timestamp = "";
		String minuteSection = "";

		timestamp = new SimpleDateFormat("yyyyMMdd_HH").format(Calendar
				.getInstance().getTime());
		int minute = (Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).get(
				Calendar.MINUTE) / 15) * 15;
		minuteSection = Integer.toString(minute);

		/* Save to file */
		try {
			saveToFile("mmda_traffic.txt", monitoredLocList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String hourTime = new SimpleDateFormat("HH").format(Calendar
				.getInstance(TimeZone.getTimeZone("GMT+8")).getTime());

		eventIndexerNotif.onStatusUpdate("File Saved (" + timestamp
				+ minuteSection + "_TrafficRec.txt).");
		eventIndexerNotif.onTrafficDataFileSaved("mmda_traffic.txt",
				(hourTime + "00"));

		return STATUS_OK;
	}

	/**
	 * Saves collected traffic data to a Raw Traffic Data File. This inner
	 * method performs the actual save operation
	 * 
	 * @param saveFileString
	 *            - the name of the save file target
	 * @param locList
	 *            - an ArrayList of MonitoredLocation objects
	 * @throws IOException
	 */
	private void saveToFile(String saveFileString,
			ArrayList<MonitoredLocation> locList) throws IOException {
		BufferedWriter fileWriter = null;

		Path saveFile = Paths.get(saveFileString);

		try {
			Files.createFile(saveFile);
		} catch (FileAlreadyExistsException e) {
			/* Not really a problem */
		}

		try {
			fileWriter = Files.newBufferedWriter(saveFile,
					Charset.forName("UTF-8"), StandardOpenOption.WRITE);

			Iterator<MonitoredLocation> iter = locList.iterator();

			while (iter.hasNext()) {
				if (shouldStopReading) {
					System.out.println("[saveToFile] Write Interrupted.");
					break;
				}
				MonitoredLocation tempLocInfoObj = iter.next();

				fileWriter.write(tempLocInfoObj.generateWriteableString()
						+ "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fileWriter != null) {
				fileWriter.close();
			}
		}
	}

	/*****************************************************************************************/
	/** INTERNAL CLASSES **/
	/*****************************************************************************************/
	/**
	 * <b>MonitoredLocation Object</b> </br>Represents locations which the MMDA
	 * has Traffic Data for
	 * 
	 * @author francis
	 * 
	 */
	public class MonitoredLocation {
		public String name = "";
		public String area = "";
		public int conditionSB = 0;
		public int conditionNB = 0;

		public MonitoredLocation(String n, String a, int cSB, int cNB) {
			name = n;
			area = a;
			conditionSB = cSB;
			conditionNB = cNB;
		}

		public String generateWriteableString() {
			return ("" + area + ", " + name + ", " + conditionSB + ", "
					+ conditionNB + ", ");
		}
	}
}
