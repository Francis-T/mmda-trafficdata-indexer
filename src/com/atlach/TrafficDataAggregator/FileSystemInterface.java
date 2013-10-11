package com.atlach.TrafficDataAggregator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.atlach.TrafficDataAggregator.DataObjects.*;

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
 * <b>FileSystemInterface Class</b> </br> Handles all interactions involving the
 * various files used by the program and the filesystem itself
 * 
 * @author francis
 * 
 */
public class FileSystemInterface {
	public static final int STATUS_OK = 0;
	public static final int STATUS_FAILED = -1;

	/*****************************************************************************************/
	/** PUBLIC METHODS **/
	/*****************************************************************************************/
	/**
	 * Extracts a particular tag string for the given date
	 * 
	 * @param tagsetFileName
	 *            - the name of the tag file to use
	 * @param dateStr
	 *            - the date to extract a tag for
	 * @return a String containing the extracted tag
	 * @throws IOException
	 */
	public String getTagForDate(String tagsetFileName, String dateStr)
			throws IOException {
		File tagsFile = new File(tagsetFileName);
		String tag = "";

		/* Safety just in case this file does not exist */
		if (!tagsFile.exists()) {
			return null;
		}

		/* Initialize the streams to null */
		FileInputStream tagsFileStream = null;
		BufferedReader rd = null;

		try {
			tagsFileStream = new FileInputStream(tagsFile);

			String line = "";
			// Wrap a BufferedReader around the InputStream
			rd = new BufferedReader(new InputStreamReader(tagsFileStream));

			// Read response until the end
			while ((line = rd.readLine()) != null) {
				String lineStr[] = line.split(":");

				/*
				 * Normally, the length of the line should be 2. Otherwise, we
				 * might be dealing with a malformed line. Therefore, we should
				 * skip it.
				 */
				if (lineStr.length != 2) {
					continue;
				}

				/*
				 * Check if we've encountered the date whose tags we want to
				 * retrieve
				 */
				if (lineStr[0].contains(dateStr)) {
					tag = lineStr[1].trim();
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (rd != null) {
				rd.close();
			}
			if (tagsFileStream != null) {
				tagsFileStream.close();
			}
		}

		return tag;
	}

	/**
	 * Loads the tagset file and parses the stored tag info into a List of
	 * TagInfo objects.
	 * 
	 * @param tagsetFileName
	 *            - the name of the tag file
	 * @return a List of TagInfo objects
	 * @throws IOException
	 */
	public List<TagInfo> loadTagsetFile(String tagsetFileName)
			throws IOException {
		File tagsFile = new File(tagsetFileName);

		/* Safety just in case this file does not exist */
		if (!tagsFile.exists()) {
			return null;
		}

		/* Initialize the tag info list */
		List<TagInfo> tagInfoList = new ArrayList<TagInfo>();

		/* Create the file input stream */
		FileInputStream tagsFileStream = new FileInputStream(tagsFile);

		/*
		 * Clear the tag list first so we don't accidentally add multiple tags
		 * to an existing tag list.
		 */
		tagInfoList.clear();

		String line = "";
		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				tagsFileStream));

		// Read response until the end
		while ((line = rd.readLine()) != null) {
			String lineStr[] = line.split(":");

			/*
			 * Normally, the length of the line should be 2. Otherwise, we might
			 * be dealing with a malformed line. Therefore, we should skip it.
			 */
			if (lineStr.length != 2) {
				continue;
			}

			/* Add a new TagInfo object to the list */
			tagInfoList.add(new TagInfo(lineStr[0].trim(), lineStr[1].trim()));
		}

		if (tagInfoList.size() == 0) {
			tagInfoList.clear();
			tagInfoList = null;
		}

		tagsFileStream.close();

		return tagInfoList;
	}

	/**
	 * Saves tag information to the specified tag file
	 * 
	 * @param tagFileName
	 *            - the name of the tag file
	 * @param tagInfoList
	 *            - a List of TagInfo objects containing the tags to be written
	 *            to the tag file
	 * @return an integer indicating the exit status for this method
	 * @throws IOException
	 */
	public int saveTagFile(String tagFileName, List<TagInfo> tagInfoList)
			throws IOException {
		File tagFile = new File(tagFileName);

		if (!tagFile.exists()) {
			tagFile.createNewFile();
		}

		/* Initialize the stream handlers */
		FileOutputStream fOut = null;
		BufferedWriter wr = null;

		try {
			/*
			 * Initialize the file output stream to append to the existing
			 * partFile
			 */
			fOut = new FileOutputStream(tagFile);
			wr = new BufferedWriter(new OutputStreamWriter(fOut));

			for (int i = 0; i < tagInfoList.size(); i++) {
				wr.write(tagInfoList.get(i).date);
				wr.write(":");
				wr.write(tagInfoList.get(i).tagset);
				wr.newLine();
			}

			wr.flush();
		} catch (IOException e) {
			return STATUS_FAILED;
		} finally {
			if (wr != null) {
				wr.close();
			}
			if (fOut != null) {
				fOut.close();
			}
		}
		return STATUS_OK;
	}

	/**
	 * Checks the usability of the data in a part file by counting the number of
	 * entries currently recorded in the currently-running Traffic Data Part
	 * File. This is mostly used to verify if we have enough data to describe
	 * the traffic conditions for this day.
	 * 
	 * @param partFileName
	 *            - the name of the Part File
	 * @return an integer indicating the number of distinct entries in the Part
	 *         File
	 * @throws IOException
	 */
	public int checkPartFileTimestamps(String partFileName) throws IOException {
		/*
		 * We're going to base the sanity of the part file's data on the number
		 * of complete records contained within it.
		 */
		int recordCount = 0;
		File partFile = new File(partFileName);

		if (!partFile.exists()) {
			System.out
					.println("[checkPartFileTimestamps] ERROR: Part File Not Found!");
			return recordCount;
		}

		BufferedReader rd = null;
		FileInputStream fInp = null;

		try {
			fInp = new FileInputStream(partFile);
			rd = new BufferedReader(new InputStreamReader(fInp));

			String line = "";
			String timestamp = "";
			while ((line = rd.readLine()) != null) {
				/* Split the string */
				String lineStr[] = line.split(":");
				if (lineStr.length != 2) {
					continue;
				}

				/*
				 * If the new timestamp value would be different from the
				 * previous one, increase the record counter by one.
				 */
				if (lineStr[0].equals(timestamp) == false) {
					recordCount++;
				}

				/* Store timestamp value */
				timestamp = lineStr[1];
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (rd != null) {
				rd.close();
			}
			if (fInp != null) {
				fInp.close();
			}
		}

		return recordCount;
	}

	/**
	 * Extracts header information from a Historical Data File without actually
	 * extracting any of the content data
	 * 
	 * @param histDataFileName
	 *            - the name of the Hist Data File
	 * @param isCompressed
	 *            - a boolean indicating whether the Hist Data File is in
	 *            DEFLATER compressed format
	 * @return a HistDataFileInfo object containing info about the Hist Data
	 *         File
	 */
	public HistDataFileInfo getHistDataFileInfo(String histDataFileName,
			boolean isCompressed) {
		File histDataFile = new File(histDataFileName);

		/* Safety just in case this file does not exist */
		if (!histDataFile.exists()) {
			return null;
		}

		FileInputStream fInp = null;
		InflaterInputStream iInp = null;
		HistDataFileInfo hdFileInfo = null;

		try {
			BufferedReader rd = null;
			fInp = new FileInputStream(histDataFile);

			// Wrap a BufferedReader around the InputStream
			if (isCompressed) {
				iInp = new InflaterInputStream(fInp);
				rd = new BufferedReader(new InputStreamReader(iInp));
			} else {
				rd = new BufferedReader(new InputStreamReader(fInp));
			}

			String line = "";
			String datesCovered = "";
			List<String> tagList = new ArrayList<String>();
			boolean isLiftingTags = false;
			boolean hasFoundContentTag = false;

			// Read response until the end
			while ((line = rd.readLine()) != null) {
				/* Check for the DatesCovered tag */
				if (line.contains("[DatesCovered]")) {
					String lineStr[] = line.split("]");

					datesCovered = lineStr[1].trim();
					continue;
				}

				/* Check for the TagIndexStart tag */
				if (line.contains("[TagIndexStart]")) {
					isLiftingTags = true;
					continue;
				}

				/* Check for the TagIndexEnd tag */
				if (line.contains("[TagIndexEnd]")) {
					isLiftingTags = false;
					continue;
				}

				/*
				 * If we are currently in the tag index, assume that we can copy
				 * each line as a tag to the taglist
				 */
				if (isLiftingTags) {
					tagList.add(line.trim());
				}

				/* Check for the Content tag */
				if (line.contains("[Content]")) {
					hasFoundContentTag = true;
					continue;
				}

				/* Quit reading once we hit the body of the file */
				if ((line.length() > 0) && (line.charAt(0) == '>')) {
					break;
				}
			}

			if (!hasFoundContentTag) {
				System.out
						.println("WARNING: [Content] tag not found for this HistData file!");
			} else {
				System.out
						.println("INFO: [Content] tag found for this HistData file.");
			}

			/* Close the input streams */
			if (fInp != null) {
				fInp.close();
			}
			if (iInp != null) {
				iInp.close();
			}

			/* Create the HistDataFileInfo object */
			hdFileInfo = new HistDataFileInfo(datesCovered, tagList);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return hdFileInfo;
	}

	/**
	 * Loads the content data for a particular tag from the given Historical
	 * Data file
	 * 
	 * @param histDataFileName
	 *            - the name of the Hist Data file
	 * @param targetTagStr
	 *            - the tag whose contents should be loaded from the Hist Data
	 *            file
	 * @param isCompressed
	 *            - a boolean indicating whether the Hist Data File is in
	 *            DEFLATER compressed format
	 * @return a HistData object containing the list of Line Data for the
	 *         specified tag
	 * @throws IOException
	 */
	public HistData loadHistDataTagFromFile(String histDataFileName,
			String targetTagStr, boolean isCompressed) throws IOException {
		File loadFile = new File(histDataFileName);
		HistData histData = null;
		String line = "";

		if (!loadFile.exists()) {
			System.out
					.println("[loadHistDataTagFromFile] ERROR: Hist Data File Not Found!");
			return null;
		}

		BufferedReader rd = null;
		FileInputStream fInp = null;
		InflaterInputStream iInp = null;

		try {
			fInp = new FileInputStream(loadFile);

			if (isCompressed) {
				iInp = new InflaterInputStream(fInp);
				rd = new BufferedReader(new InputStreamReader(iInp));
			} else {
				rd = new BufferedReader(new InputStreamReader(fInp));
			}

			boolean shouldExtractData = false;

			while ((line = rd.readLine()) != null) {
				// System.out.println("Decompressed Out: " + line);
				/* Check if we are dealing with the tag indicator */
				if ((line.length() > 0) && (line.charAt(0) == '>')) {
					/* Extract the tagset value */
					String tagStr = line.substring(3, line.length() - 1);

					/*
					 * If the correct tag has been found, flag data extraction
					 * to begin
					 */
					if (tagStr.equals(targetTagStr)) {
						shouldExtractData = true;
						histData = new HistData("UNKNOWN", tagStr);
						continue;
					} else {
						shouldExtractData = false;
					}
				}

				/* Skip over things until we reach the correct tag */
				if (!shouldExtractData) {
					continue;
				}

				String lineStr[] = line.split(":");
				if (lineStr.length != 2) {
					continue;
				}

				/* Create a temporary LineInfo object */
				LineInfo lineInfo = new LineInfo(lineStr[0], lineStr[1]);

				/*
				 * Some kind of mechanism to insert this properly to the data
				 * list
				 */
				DataFileManager.insertToDataList(lineInfo, histData.dataList);
			}

			/* If histData is still null, then the tag has not been found */
			if (histData == null) {
				System.out
						.println("[loadHistDataTagFromFile] Warning: Tag not found! ("
								+ targetTagStr + ")");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			/* Close the input streams we used */
			if (iInp != null) {
				iInp.close();
			}
			if (fInp != null) {
				fInp.close();
			}
		}

		return histData;
	}

	/**
	 * Loads the all content data from the specified Hist Data File into a
	 * (supposedly) empty List of HistData objects
	 * 
	 * @param histDataFileName
	 *            - the name of the Hist Data file
	 * @param histDataList
	 *            - an empty list of HistData objects; this is where the
	 *            information retrieved from the Hist Data file will be stored.
	 * @param isCompressed
	 *            - a boolean indicating whether the Hist Data File is in
	 *            DEFLATER compressed format
	 * @throws IOException
	 */
	public void loadHistDataFile(String histDataFileName,
			List<HistData> histDataList, boolean isCompressed)
			throws IOException {
		File loadFile = new File(histDataFileName);
		String line = "";

		if (!loadFile.exists()) {
			return;
		}

		if (histDataList == null) {
			histDataList = new ArrayList<HistData>();
		}

		BufferedReader rd = null;
		FileInputStream fInp = null;
		InflaterInputStream iInp = null;

		try {
			fInp = new FileInputStream(loadFile);

			if (isCompressed) {
				iInp = new InflaterInputStream(fInp);
				rd = new BufferedReader(new InputStreamReader(iInp));
			} else {
				rd = new BufferedReader(new InputStreamReader(fInp));
			}

			boolean isFileBodyReached = false;
			boolean isNewHistDataObject = true;
			HistData hd = null;

			while ((line = rd.readLine()) != null) {
				// System.out.println("Decompressed Out: " + line);
				/* Skip over things until we reach the file body */
				if (line.contains("[Content]")) {
					isFileBodyReached = true;
					continue;
				}
				if (!isFileBodyReached) {
					continue;
				}

				/* Check if we are dealing with the date indicator */
				if ((line.length() > 0) && (line.charAt(0) == '>')) {
					/* If hd has content, then save it to the histDataList */
					if (hd != null) {
						if (isNewHistDataObject) {
							histDataList.add(hd);
						}
					}

					/* Nullify hd */
					hd = null;

					/* Extract the tagset value */
					String tagStr = line.substring(3, line.length() - 1);

					/* Locate this tagset in the histDataList */
					for (int i = 0; i < histDataList.size(); i++) {
						if (histDataList.get(i).tagset.equals(tagStr)) {
							hd = histDataList.get(i);
							isNewHistDataObject = false;
						}
					}
					if (hd == null) {
						hd = new HistData("UNKNOWN", tagStr);
						isNewHistDataObject = true;
					}
				}

				String lineStr[] = line.split(":");
				if (lineStr.length != 2) {
					continue;
				}

				LineInfo lineInfo = new LineInfo(lineStr[0], lineStr[1]);

				/*
				 * Some kind of mechanism to insert this properly to the data
				 * list
				 */
				DataFileManager.insertToDataList(lineInfo, hd.dataList);
			}

			/* If hd has content, then save it to the histDataList */
			if (hd != null) {
				histDataList.add(hd);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			/* Close the input streams we used */
			if (iInp != null) {
				iInp.close();
			}
			if (fInp != null) {
				fInp.close();
			}
		}

		return;
	}

	/**
	 * Prepares the HistData file to be written on by writing the relevant
	 * header information to the file first prior to allowing any Historical
	 * Data content to be written.
	 * 
	 * @param histDataFileName
	 *            - the name of the Hist Data File
	 * @param fileInfo
	 *            - a HistDataFileInfo object containing the HistData file
	 *            header information to be written
	 * @param shouldCompress
	 *            - a boolean indicating whether the Hist Data File is in
	 *            DEFLATER
	 * @throws IOException
	 */
	public void prepareHistDataFile(String histDataFileName,
			HistDataFileInfo fileInfo, boolean shouldCompress)
			throws IOException {
		File saveFile = new File(histDataFileName);

		FileOutputStream fOut = null;
		DeflaterOutputStream dOut = null;
		BufferedWriter wr = null;
		File tempFile = null;

		if (fileInfo == null) {
			System.out
					.println("[prepareHistDataFile] ERROR: HistDataFileInfo is NULL!");
			return;
		}

		if ((fileInfo.tagList == null) || (fileInfo.tagList.size() == 0)) {
			System.out
					.println("[prepareHistDataFile] ERROR: There seem to be no new tags to "
							+ "add to the file.");
			return;
		}

		try {
			HistDataFileInfo oldHistInfo = getHistDataFileInfo(
					histDataFileName, shouldCompress);

			List<String> distinctTagList = new ArrayList<String>();
			if ((oldHistInfo != null) && (oldHistInfo.tagList != null)) {

				/* Filter out redundant tags from oldHistInfo */
				for (int i = 0; i < oldHistInfo.tagList.size(); i++) {
					boolean isDistinct = true;
					String tagStr = oldHistInfo.tagList.get(i);

					for (int j = 0; j < fileInfo.tagList.size(); j++) {
						if (fileInfo.tagList.get(j).equals(tagStr)) {
							isDistinct = false;
							break;
						}
					}
					if (isDistinct) {
						System.out.println("Tag is distinct: " + tagStr);
						distinctTagList.add(tagStr);
					}
				}
			}
			distinctTagList.addAll(fileInfo.tagList);

			if (!saveFile.exists()) {
				saveFile.createNewFile();
			} else {
				/* Offload file content to a temp file first */
				if (shouldCompress) {
					tempFile = createTempDecompressedFile("temp_hdf.txt",
							saveFile, false);
				} else {
					tempFile = createTempUncompressedFile("temp_hdf.txt",
							saveFile, false);
				}
			}

			fOut = new FileOutputStream(saveFile);

			/* Compression option */
			dOut = null;
			if (shouldCompress) {
				dOut = new DeflaterOutputStream(fOut);
				wr = new BufferedWriter(new OutputStreamWriter(dOut));
			} else {
				wr = new BufferedWriter(new OutputStreamWriter(fOut));
			}

			/*
			 * Assume that the datesCovered string has been pre-processed and,
			 * therefore, no longer requires any handling from us here.
			 */
			wr.write("[DatesCovered]" + fileInfo.datesCovered);
			wr.newLine();
			wr.write("[TagIndexStart]");
			wr.newLine();

			for (int i = 0; i < distinctTagList.size(); i++) {
				wr.write(distinctTagList.get(i));
				wr.newLine();
			}

			wr.write("[TagIndexEnd]");
			wr.newLine();
			wr.newLine();
			wr.write("[Content]");
			wr.newLine();

			if (tempFile != null) {
				writeInputFromTempFile(tempFile, wr, "NULL");
				wr.flush();
				/* Delete the tempFile since we no longer need it */
				tempFile.delete();
			} else {
				wr.newLine();
				wr.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (wr != null) {
				wr.close();
			}
			if (dOut != null) {
				dOut.finish();
				dOut.close();
			}
			if (fOut != null) {
				fOut.close();
			}
		}
	}

	/**
	 * Loads information from the specified Raw Traffic Data File into a
	 * LineDataList
	 * 
	 * @param dataFileName
	 *            - the name of the Raw Traffic Data File to be used
	 * @return a LineDataList containing Traffic Data
	 */
	public LineDataList loadTrafficDataFile(String dataFileName) {
		LineDataList dataList = null;
		File dataFile = new File(dataFileName);
		try {
			dataList = parseTrafficFileContents(dataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dataList;
	}

	/**
	 * Loads information from the specified Raw Traffic Data File into a
	 * LineDataList
	 * 
	 * @param dataFile
	 *            - a File object pertaining to the Raw Traffic Data File to be
	 *            used
	 * @return a LineDataList containing Traffic Data
	 */
	public LineDataList loadTrafficDataFile(File dataFile) {
		LineDataList dataList = null;
		try {
			dataList = parseTrafficFileContents(dataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dataList;
	}

	/**
	 * Loads information from a Traffic Part Data File into a List of LineInfo
	 * objects.
	 * 
	 * @param partFileName
	 *            - the name of the Part File
	 * @return a List of LineInfo objects containing info from the Part File
	 * @throws IOException
	 */
	public List<LineInfo> loadPartDataFile(String partFileName)
			throws IOException {
		File partFile = new File(partFileName);
		List<LineInfo> dataList = new ArrayList<LineInfo>();

		if (!partFile.exists()) {
			System.out
					.println("[loadPartDataFile] ERROR: Part File Not Found!");
			return null;
		}

		BufferedReader rd = null;
		FileInputStream fInp = null;

		try {
			fInp = new FileInputStream(partFile);
			rd = new BufferedReader(new InputStreamReader(fInp));

			String line = "";
			while ((line = rd.readLine()) != null) {
				String lineStr[] = line.split(":");
				if (lineStr.length != 2) {
					continue;
				}

				/* Create a temporary LineInfo object */
				LineInfo lineInfo = new LineInfo(lineStr[0], lineStr[1]);

				/*
				 * Some kind of mechanism to insert this properly to the data
				 * list
				 */
				DataFileManager.insertToDataList(lineInfo, dataList);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (rd != null) {
				rd.close();
			}
			if (fInp != null) {
				fInp.close();
			}
		}

		return dataList;
	}

	/**
	 * Saves a Timestamp and LineData String pair into a running Part File.
	 * 
	 * @param partFileName
	 *            - the name of the Part File
	 * @param timestamp
	 *            - the Timestamp for this Line Data
	 * @param lineDataStr
	 *            - the Line Data String
	 * @return an integer indicating the exit status for this method
	 * @throws IOException
	 */
	public int saveDataToPartFile(String partFileName, String timestamp,
			String lineDataStr) throws IOException {
		File partFile = new File(partFileName);

		if (!partFile.exists()) {
			partFile.createNewFile();
		}

		/* Initialize the stream handlers */
		FileOutputStream fOut = null;
		BufferedWriter wr = null;

		try {
			/*
			 * Initialize the file output stream to append to the existing
			 * partFile
			 */
			fOut = new FileOutputStream(partFile, true);
			wr = new BufferedWriter(new OutputStreamWriter(fOut));

			/* Write the timestamp */
			wr.write(timestamp);
			wr.write(":");

			/* Write the compressed line date string */
			wr.write(lineDataStr);
			wr.newLine();

			wr.flush();
		} catch (IOException e) {
			return STATUS_FAILED;
		} finally {
			if (wr != null) {
				wr.close();
			}
			if (fOut != null) {
				fOut.close();
			}
		}

		return STATUS_OK;
	}

	/**
	 * Saves data from a List of LineInfo objects into a Hist Data File. This is
	 * mostly used for updating a previously generated Hist Data File with a new
	 * day's data.
	 * 
	 * @param histDataFileName
	 *            - the name of the Hist Data File
	 * @param lineInfoList
	 *            - the List of LineInfo objects containing information to be
	 *            written to the Hist Data File
	 * @param tagStr
	 *            - the tag to associate with this List of Line Info objects
	 * @param shouldWriteTag
	 *            - a boolean indicating whether we should write the tag as part
	 *            of the content data
	 * @param shouldCompress
	 *            - a boolean indicating whether the Hist Data File is in
	 *            DEFLATER
	 * @throws IOException
	 */
	public void saveDataToFile(String histDataFileName,
			List<LineInfo> lineInfoList, String tagStr, boolean shouldWriteTag,
			boolean shouldCompress) throws IOException {
		File saveFile = new File(histDataFileName);

		if (!saveFile.exists()) {
			return;
		}

		FileOutputStream fOut = null;
		DeflaterOutputStream dOut = null;
		BufferedWriter wr = null;
		File tempFile = null;

		try {

			/*
			 * Need to inflate the existing file's contents to an external file
			 * first if we want to append to the compressed file
			 */
			if (shouldCompress) {
				tempFile = createTempDecompressedFile("temp.txt", saveFile,
						true);
			} else {
				tempFile = createTempUncompressedFile("temp.txt", saveFile,
						true);
			}

			fOut = new FileOutputStream(saveFile);

			/* Compression option */
			if (shouldCompress) {
				dOut = new DeflaterOutputStream(fOut);
				wr = new BufferedWriter(new OutputStreamWriter(dOut));

				/*
				 * Write data back from our tempFile to our saveFile using the
				 * writer
				 */
				writeInputFromTempFile(tempFile, wr, tagStr);
				/* Delete the file since we no longer need it */
				tempFile.delete();
			} else {
				wr = new BufferedWriter(new OutputStreamWriter(fOut));

				/*
				 * Write data back from our tempFile to our saveFile using the
				 * writer
				 */
				writeInputFromTempFile(tempFile, wr, tagStr);
				/* Delete the file since we no longer need it */
				tempFile.delete();
			}

			Iterator<LineInfo> iter = lineInfoList.iterator();
			boolean isFirstLine = true;

			while (iter.hasNext()) {
				LineInfo tempLineInfoObj = iter.next();

				if (isFirstLine && shouldWriteTag) {
					wr.write("> [" + tagStr + "]");
					wr.newLine();
					isFirstLine = false;
				}

				wr.write(tempLineInfoObj.timestamp + ":"
						+ tempLineInfoObj.lineDataStr);
				wr.newLine();
				wr.flush();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (wr != null) {
				wr.close();
			}
			if (dOut != null) {
				dOut.finish();
				dOut.close();
			}
			if (fOut != null) {
				fOut.close();
			}
		}
	}

	/*****************************************************************************************/
	/** PRIVATE METHODS **/
	/*****************************************************************************************/
	/**
	 * Creates a temporary decompressed file where we will store header and
	 * content info from the intermediate Hist Data file while writing in new
	 * data. </br></br> <b>NOTE:</b> There might be a better way of achieving
	 * this effect by simply using the underlying filesystem's file "copy"
	 * command. However, since we do not necessarily know what system we will be
	 * running in, this workaround was used instead.
	 * 
	 * @param tempFileName
	 *            - the name of the Temp File
	 * @param originalFile
	 *            - the name of the Original Compressed HistData file
	 * @param shouldCopyHeader
	 *            - a boolean indicating whether we should copy the header info
	 * @return a File object referring to the created Temp File
	 * @throws IOException
	 */
	private File createTempDecompressedFile(String tempFileName,
			File originalFile, boolean shouldCopyHeader) throws IOException {
		File tempFile = new File(tempFileName);

		FileInputStream fInp = null;
		InflaterInputStream iInp = null;
		FileOutputStream fOut = null;
		BufferedWriter wr = null;
		BufferedReader rd = null;

		try {
			if (!tempFile.exists()) {
				tempFile.createNewFile();
			} else {
				tempFile.delete();
				tempFile.createNewFile();
			}

			fInp = new FileInputStream(originalFile);

			iInp = new InflaterInputStream(fInp);
			fOut = new FileOutputStream(tempFile);
			wr = new BufferedWriter(new OutputStreamWriter(fOut));
			rd = new BufferedReader(new InputStreamReader(iInp));

			String line = "";
			boolean hasReadContentTag = false;
			while ((line = rd.readLine()) != null) {
				if ((!shouldCopyHeader) && (!hasReadContentTag)) {
					if (line.contains("[Content]")) {
						hasReadContentTag = true;
					}
					continue;
				}
				wr.write(line);
				wr.newLine();
				wr.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fInp != null) {
				fInp.close();
			}
			if (iInp != null) {
				iInp.close();
			}
			if (fOut != null) {
				fOut.close();
			}
			if (wr != null) {
				wr.close();
			}
			if (wr != null) {
				rd.close();
			}
		}

		return tempFile;
	}

	/**
	 * 
	 * Creates a temporary file where we will store header and content info from
	 * the intermediate Hist Data file while writing in new data. This method
	 * specifically targets uncompressed Hist Data files. </br></br>
	 * <b>NOTE:</b> There might be a better way of achieving this effect by
	 * simply using the underlying filesystem's file "copy" command. However,
	 * since we do not necessarily know what system we will be running in, this
	 * workaround was used instead.
	 * 
	 * @param tempFileName
	 *            - the name of the Temp File
	 * @param originalFile
	 *            - the name of the Original HistData file
	 * @param shouldCopyHeader
	 *            - a boolean indicating whether we should copy the header info
	 * @return a File object referring to the created Temp File
	 */
	private File createTempUncompressedFile(String tempFileName,
			File originalFile, boolean shouldCopyHeader) throws IOException {
		File tempFile = new File(tempFileName);

		FileInputStream fInp = null;
		FileOutputStream fOut = null;
		BufferedWriter wr = null;
		BufferedReader rd = null;

		try {
			if (!tempFile.exists()) {
				tempFile.createNewFile();
			} else {
				tempFile.delete();
				tempFile.createNewFile();
			}

			fInp = new FileInputStream(originalFile);

			fOut = new FileOutputStream(tempFile);
			wr = new BufferedWriter(new OutputStreamWriter(fOut));
			rd = new BufferedReader(new InputStreamReader(fInp));

			String line = "";
			boolean hasReadContentTag = false;
			while ((line = rd.readLine()) != null) {
				if ((!shouldCopyHeader) && (!hasReadContentTag)) {
					if (line.contains("[Content]")) {
						hasReadContentTag = true;
					}
					continue;
				}
				wr.write(line);
				wr.newLine();
				wr.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fInp != null) {
				fInp.close();
			}
			if (fOut != null) {
				fOut.close();
			}
			if (wr != null) {
				wr.close();
			}
			if (wr != null) {
				rd.close();
			}
		}

		return tempFile;
	}

	/**
	 * Writes the contents of the generated Hist Data temporary file back to the
	 * intermediate Hist Data file while skipping the particular tags which have
	 * just been updated.
	 * 
	 * @param tempFile
	 *            - the name of the Temp File
	 * @param writeTarget
	 *            - BufferedWriter object to use for writing
	 * @param tagSkipStr
	 *            - a String specifying which tag should be skipped
	 * @throws IOException
	 */
	private void writeInputFromTempFile(File tempFile,
			BufferedWriter writeTarget, String tagSkipStr) throws IOException {
		if (!tempFile.exists()) {
			return;
		}

		FileInputStream fInp = null;
		BufferedReader rd = null;

		try {
			/* Stream input from our tempfile */
			fInp = new FileInputStream(tempFile);
			rd = new BufferedReader(new InputStreamReader(fInp));

			String line = "";
			boolean isSkipping = false;

			while ((line = rd.readLine()) != null) {

				/* Skip read-write over the tag being currently written */
				if ((line.length() > 0) && (line.charAt(0) == '>')) {
					String lineTag = line.substring(line.indexOf('[') + 1,
							line.lastIndexOf(']'));

					System.out.println("skip str: [" + tagSkipStr + "]");
					System.out.println("line tag: [" + lineTag + "]");

					if ((tagSkipStr != null) && (lineTag.equals(tagSkipStr))) {
						System.out
								.println("[writeInputFromTempFile] Skipping ["
										+ tagSkipStr + "]...");
						isSkipping = true;
					} else {
						isSkipping = false;
					}
				}

				if (isSkipping) {
					continue;
				}

				writeTarget.write(line);
				writeTarget.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			fInp.close();
			rd.close();
		}
	}

	/**
	 * Parses the contents of a Raw Traffic Data File into a LineDataList object
	 * 
	 * @param dataFile
	 *            - a File object pertaining to the Raw Traffic Data File to be
	 *            used
	 * @return a LineDataList object containing Traffic Data information for
	 *         each line
	 * @throws IOException
	 */
	private LineDataList parseTrafficFileContents(File dataFile)
			throws IOException {
		if (dataFile.exists() == false) {
			return null;
		}

		FileInputStream tripFileStream = new FileInputStream(dataFile);

		/* Parse the file into a LineDataList */
		LineDataList ld = new LineDataList();

		String tempLocStr;
		short tempLoc;
		String tempCondStr;
		byte tempCondSB;
		byte tempCondNB;

		String line = "";
		int lineNum = 1;

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				tripFileStream, "UTF-8"));

		short prevLoc = -1;
		// Read response until the end
		while ((line = rd.readLine()) != null) {

			/* Skip lines with less than four fields */
			if (line.split(",").length != 5) {
				System.out
						.println("[parseTrafficFileContents] Invalid line length encountered: "
								+ line.split(",").length + "!");
				continue;
			}

			tempLocStr = (getCSVField(line, 0, lineNum) + ", " + getCSVField(
					line, 1, lineNum)).replace("��", "n").replace("ñ", "n");
			tempLoc = matchToLocIndex(tempLocStr);
			if ((tempLoc > Constants.locStr.length) || (tempLoc < 0)) {
				System.out
						.println("[parseTrafficFileContents] Error: Invalid location code: "
								+ tempLoc + "!");
				System.out.println("[parseTrafficFileContents] 		> LocStr: "
						+ tempLocStr + "!");
				System.out.println("[parseTrafficFileContents] 		> Line: "
						+ line + "!");
				continue;
			}

			tempCondStr = getCSVField(line, 2, lineNum);

			if ((tempCondStr == null) || (tempCondStr.equals(""))) {
				tempCondStr = "0";
			}
			tempCondSB = (byte) (Integer.parseInt(tempCondStr));

			if ((tempCondStr == null) || (tempCondStr.equals(""))) {
				tempCondStr = "0";
			}
			tempCondNB = (byte) (Integer.parseInt(tempCondStr));

			if ((tempLoc - prevLoc) > 1) {
				while ((tempLoc - prevLoc) != 0) {
					/*
					 * Pre-increment the value of prevLoc (since we generally
					 * start this at -1)
					 */
					prevLoc++;
					/* Add the missing LineData object */
					LineData ldt = new LineData(prevLoc, (byte) (0), (byte) (0));
					ld.data.add(ldt);

					if ((prevLoc + 1) == tempLoc) {
						break;
					}
				}
			}

			LineData ldt = new LineData(tempLoc, tempCondSB, tempCondNB);
			ld.data.add(ldt);
			lineNum++;
			prevLoc = tempLoc;
		}

		tripFileStream.close();

		/*
		 * Check if the final LineDataList is empty. - This would be the case if
		 * the file were empty in the first place
		 */
		if (ld.data.size() == 0) {
			return null;
		}

		return ld; /* Return the filled LineDataList */
	}

	/**
	 * Attempts to match a location string to an index based on the array of
	 * locations stored in Constants.java
	 * 
	 * @param matchLocStr
	 *            - the location string to match
	 * @return the matched index or a failed exit status (-1)
	 */
	private short matchToLocIndex(String matchLocStr) {
		for (int i = 0; i < Constants.locStr.length; i++) {
			if (Constants.locStr[i].contains(matchLocStr) == true) {
				return ((short) i);
			}
		}
		return STATUS_FAILED;
	}

	/**
	 * Gets a particular CSV field from a given line. This method is mostly used
	 * for reading CSV files.
	 * 
	 * @param line
	 *            - line containing CSV fields
	 * @param field
	 *            - index of the CSV field to obtain
	 * @param lineNum
	 *            - (Optional) used to indicate the line number for this line in
	 *            the parsed CSV file
	 * @return a string containing the value of the specified CSV field
	 */
	private String getCSVField(String line, int field, int lineNum) {
		int startIdx = 0;
		int endIdx = 0;
		int fieldIdx = 0;

		for (fieldIdx = 0; fieldIdx <= field; fieldIdx++) {
			/* Update indices */
			startIdx = (endIdx == 0) ? endIdx : endIdx + 1;
			endIdx = line.indexOf(",", startIdx);

			/* Perform sanity checks on our start and end indices */
			if ((endIdx >= line.length()) || (startIdx > endIdx)
					|| (startIdx < 0) || (endIdx < 0)) {
				System.out.println("[getCSVField] Possibly incorrect indices ("
						+ startIdx + ", " + endIdx + ") at line #" + lineNum);
				return "";
			}
		}

		/*
		 * The ff condition should be true: fieldIdx == field+1; otherwise,
		 * something went wrong
		 */
		if (fieldIdx != field + 1) {
			System.out.println("[getCSVField] Something went wrong!");
			return "";
		}

		return line.substring(startIdx, endIdx).trim();
	}
}
