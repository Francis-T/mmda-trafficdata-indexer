package com.atlach.TrafficDataAggregator.DataObjects;

import java.math.BigDecimal;

import com.atlach.TrafficDataAggregator.Constants;

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
 * <b>LineData Object</b> </br>Object used to represent Traffic Data for a
 * single Line (i.e. segment of road)
 * 
 * @author francis
 * 
 */
public class LineData {
	/* Fields */
	public byte locNameCode;
	public byte trafficSB;
	public byte trafficNB;

	/* Constructor */
	public LineData(short loc, byte sb, byte nb) {
		locNameCode = (byte) (loc - 128);
		trafficSB = sb;
		trafficNB = nb;
	}

	public BigDecimal round(float d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Float.toString(d));
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd;
	}

	public float getSBTrafficAve() {
		byte b[] = new byte[4];
		b[0] = (byte) (trafficSB & 0x03);
		b[1] = (byte) ((trafficSB & 0x0C) >> 2);
		b[2] = (byte) ((trafficSB & 0x30) >> 4);
		b[3] = (byte) ((trafficSB & 0xC0) >> 6);

		int weighedSum = 0;
		float finDiv = 0.0f;
		for (int j = 0; j < b.length; j++) {
			weighedSum += b[j] * (j + 1);
			if (b[j] != 0) {
				finDiv += (float) (j + 1);
			}
		}

		return (weighedSum / finDiv);
	}

	public float getNBTrafficAve() {
		byte b[] = new byte[4];
		b[0] = (byte) (trafficNB & 0x03);
		b[1] = (byte) ((trafficNB & 0x0C) >> 2);
		b[2] = (byte) ((trafficNB & 0x30) >> 4);
		b[3] = (byte) ((trafficNB & 0xC0) >> 6);

		int weighedSum = 0;
		float finDiv = 0.0f;
		for (int j = 0; j < b.length; j++) {
			weighedSum += b[j] * (j + 1);
			if (b[j] != 0) {
				finDiv += (float) (j + 1);
			}
		}

		return (weighedSum / finDiv);
	}

	public String toString() {
		return ("loc: " + Constants.locStr[locNameCode + 128] + ", sb:"
				+ round(getSBTrafficAve(), 2) +
				// "(" + (String.format("%6s",
				// Integer.toBinaryString(trafficSB)).replace(' ', '0')) + ")" +
				", nb:" + round(getNBTrafficAve(), 2) +
		// "(" + (String.format("%6s",
		// Integer.toBinaryString(trafficNB)).replace(' ', '0')) +
		")");
	}
}
