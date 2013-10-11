package com.atlach.TrafficDataAggregator;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 * <b>WeatherDataManager Class</b> </br>Handles all interactions with the
 * Web-based Weather Data provider (WorldWeatherOnline.com)
 * 
 * @author francis
 * 
 */
public class WeatherDataManager {
	private final String BASE_SERVER_URL = "http://api.worldweatheronline.com/"
			+ "free/v1/weather.ashx";
	private final String KEY_VAL = "XXXXXXXXXXXXXXXXXXXXXXXXX";
	private static final int STATUS_OK = 0;

	/*****************************************************************************************/
	/** PUBLIC METHODS **/
	/*****************************************************************************************/
	/**
	 * Sends a weather data request to WorldWeatherOnline.com
	 * 
	 * @param location
	 *            - the location for which weather data should be obtained
	 * @param numOfDays
	 *            - the number of days of predicted weather data to obtain
	 * @return a string containing the JSON response
	 */
	public String sendWeatherDataRequest(String location, int numOfDays) {
		String respStr = "";

		try {
			HttpURLConnection urlConn = getServerConnection("q=" + location
					+ "&format=json&num_of_days=" + Integer.toString(numOfDays)
					+ "&key=" + this.KEY_VAL);
			if (sendHttpRequest(urlConn) == STATUS_OK) {
				respStr = receiveJSONResponse(urlConn);
			}

		} catch (IOException e) {
			System.out.println("IOException occurred!");
			e.printStackTrace();
			return "";
		}

		return respStr;
	}

	/**
	 * Extracts the current weather condition from the received JSON response
	 * 
	 * @param jsonString
	 *            - the JSON response string
	 * @return a string containing a description of the current weather
	 *         condition in tag format
	 */
	public String extractCurrentWeatherCondition(String jsonString) {
		String jsonStr = jsonString;
		String condStr = "Unknown";

		try {
			// Create the JSON object from the returned text
			JSONObject jsonObject = new JSONObject(jsonStr);

			JSONObject data = jsonObject.getJSONObject("data");
			JSONArray currentCond = data.getJSONArray("current_condition");

			if (currentCond.length() != 1) {
				System.out
						.println("[extractCurrentWeatherCondition] Invalid length!");
				return condStr;
			}
			JSONObject currentCondChild = currentCond.getJSONObject(0);

			JSONArray weatherDesc = currentCondChild
					.getJSONArray("weatherDesc");
			if (weatherDesc.length() != 1) {
				System.out
						.println("[extractCurrentWeatherCondition] Invalid length!");
				return condStr;
			}

			JSONObject weatherDescChild = weatherDesc.getJSONObject(0);
			condStr = getSimplifiedWeatherCond(weatherDescChild.getString(
					"value").toLowerCase());

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return condStr;
	}

	/*****************************************************************************************/
	/** PRIVATE METHODS **/
	/*****************************************************************************************/
	/**
	 * Simplifies the received weather condition information into Tag format
	 * 
	 * @param jsonCondStr
	 *            - the weather condition string extracted from the JSON
	 *            response
	 * @return a string containing a description of the current weather
	 *         condition in tag format
	 */
	private String getSimplifiedWeatherCond(String jsonCondStr) {
		String condStr = jsonCondStr;

		if (jsonCondStr.contains("cloudy") || jsonCondStr.contains("overcast")
				|| jsonCondStr.contains("mist") || jsonCondStr.contains("fog")) {
			condStr = "Overcast";
			if (jsonCondStr.contains("partly")) {
				condStr += "|Cool";
			} else {
				condStr += "|Cold";
			}
			return condStr;
		}

		if (jsonCondStr.contains("rain") || jsonCondStr.contains("drizzle")) {
			condStr = "Rain";
			if (jsonCondStr.contains("thunder")) {
				condStr += "|Storm";
			} else if (jsonCondStr.contains("moderate")) {
				condStr += "|Moderate";
			} else if (jsonCondStr.contains("heavy")) {
				condStr += "|Heavy";
			} else if (jsonCondStr.contains("light")) {
				condStr += "|Light";
			}
			return condStr;
		}

		if (jsonCondStr.contains("snow") || jsonCondStr.contains("ice")
				|| jsonCondStr.contains("blizzard")) {
			condStr = "Snow";
			if (jsonCondStr.contains("thunder")) {
				condStr += "|Storm";
			} else if (jsonCondStr.contains("moderate")) {
				condStr += "|Moderate";
			} else if (jsonCondStr.contains("heavy")) {
				condStr += "|Heavy";
			} else if (jsonCondStr.contains("light")) {
				condStr += "|Light";
			}
			return condStr;
		}

		if (jsonCondStr.contains("thundery outbreaks")) {
			condStr = "Rain|Storm";
			return condStr;
		}

		if (jsonCondStr.contains("clear")) {
			condStr = "Clear";
			return condStr;
		}

		return "Unknown";
	}

	/**
	 * Gets an Http server connection given the requested parameters
	 * 
	 * @param requestSuffix
	 *            - the request suffix string
	 * @return an HttpUrlConnection object
	 * @throws IOException
	 */
	private HttpURLConnection getServerConnection(String requestSuffix)
			throws IOException {
		URL url;
		HttpURLConnection urlConn;

		url = new URL(BASE_SERVER_URL + "?" + requestSuffix);

		System.out.println("GET Request URL: " + url.toString());

		urlConn = (HttpURLConnection) url.openConnection();
		urlConn.setRequestMethod("GET");
		urlConn.setDoOutput(true);
		urlConn.setDoInput(true);
		urlConn.setRequestProperty("Accept", "application/json");

		return urlConn;
	}

	/**
	 * Prints a received error response. Mostly used for debugging purposes.
	 * 
	 * @param conn
	 *            - the HttpUrlConnection object to be used
	 * @throws IOException
	 */
	private void printErrorResponse(HttpURLConnection conn) throws IOException {
		DataInputStream input = new DataInputStream(conn.getErrorStream());

		String str;

		// FIXME Uses deprecated method --Francis
		System.out.println("Response: " + conn.getResponseMessage());
		System.out.println("Response code: " + conn.getResponseCode());
		while (null != ((str = input.readLine()))) {
			// Debug
			System.out.println("> " + str);
		}

		input.close();
	}

	/**
	 * Sends an Http Request using the provided Http Connection
	 * 
	 * @param urlConn
	 *            - the HttpUrlConnection object to be used
	 * @return an integer indicating the exit status for this method
	 * @throws IOException
	 */
	private int sendHttpRequest(HttpURLConnection urlConn) throws IOException {

		urlConn.connect();

		return STATUS_OK;
	}

	/**
	 * Obtains the received JSON Response
	 * 
	 * @param conn
	 *            - the HttpUrlConnection object to be used
	 * @return the received JSON Response string
	 * @throws IOException
	 */
	private String receiveJSONResponse(HttpURLConnection conn)
			throws IOException {
		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
			printErrorResponse(conn);
			return null;
		}

		DataInputStream input = new DataInputStream(conn.getInputStream());
		String str;
		String fullStr = "";

		// FIXME Uses deprecated method --Francis
		while (null != ((str = input.readLine()))) {
			fullStr += str;
		}

		return fullStr;
	}
}
