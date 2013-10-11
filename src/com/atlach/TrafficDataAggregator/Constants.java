package com.atlach.TrafficDataAggregator;

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
 * <b>Constants Class</b> </br>Used to store constants which are used repeatedly
 * throughout the program
 * 
 * @author francis
 * 
 */
public class Constants {
	public static final String locStr[] = { "EDSA, Balintawak",
			"EDSA, Kaingin Road", "EDSA, Munoz", "EDSA, Bansalangin",
			"EDSA, North Ave.", "EDSA, Trinoma", "EDSA, Quezon Ave.",
			"EDSA, NIA Road", "EDSA, Timog", "EDSA, Kamuning",
			"EDSA, New York - Nepa Q-Mart", "EDSA, Monte De Piedad",
			"EDSA, Aurora Blvd.", "EDSA, Mc Arthur - Farmers",
			"EDSA, P. Tuazon", "EDSA, Main Ave.", "EDSA, Santolan",
			"EDSA, White Plains - Connecticut", "EDSA, Ortigas Ave.",
			"EDSA, SM Megamall", "EDSA, Shaw Blvd.", "EDSA, Reliance",
			"EDSA, Pioneer - Boni", "EDSA, Guadalupe", "EDSA, Orense",
			"EDSA, Kalayaan - Estrella", "EDSA, Buendia", "EDSA, Ayala Ave.",
			"EDSA, Arnaiz - Pasay Road", "EDSA, Magallanes", "EDSA, Malibay",
			"EDSA, Tramo", "EDSA, Taft Ave.", "EDSA, F.B. Harrison",
			"EDSA, Roxas Boulevard", "EDSA, Macapagal Ave.",
			"EDSA, Mall of Asia", "COMMONWEALTH, Batasan",
			"COMMONWEALTH, St. Peter's Church", "COMMONWEALTH, Ever Gotesco",
			"COMMONWEALTH, Diliman Preparatory School",
			"COMMONWEALTH, Zuzuarregi",
			"COMMONWEALTH, General Malvar Hospital",
			"COMMONWEALTH, Tandang Sora Eastside",
			"COMMONWEALTH, Tandang Sora Westside", "COMMONWEALTH, Central Ave",
			"COMMONWEALTH, Magsaysay Ave", "COMMONWEALTH, University Ave",
			"COMMONWEALTH, Philcoa", "QUEZON AVE, Elliptical Road",
			"QUEZON AVE, Agham Road", "QUEZON AVE, Bantayog Road",
			"QUEZON AVE, Edsa", "QUEZON AVE, SGT. Esguera",
			"QUEZON AVE, Scout Albano", "QUEZON AVE, Scout Borromeo",
			"QUEZON AVE, Scout Santiago", "QUEZON AVE, Timog",
			"QUEZON AVE, Scout Reyes", "QUEZON AVE, Scout Magbanua",
			"QUEZON AVE, Roces Avenue", "QUEZON AVE, Roosevelt Avenue",
			"QUEZON AVE, Dr. Garcia Sr.", "QUEZON AVE, Scout Chuatoco",
			"QUEZON AVE, G. Araneta Ave.", "QUEZON AVE, Sto. Domingo",
			"QUEZON AVE, Biak na Bato", "QUEZON AVE, Banawe",
			"QUEZON AVE, Cordillera", "QUEZON AVE, D. Tuazon",
			"QUEZON AVE, Speaker Perez", "QUEZON AVE, Apo Avenue",
			"QUEZON AVE, Kanlaon", "QUEZON AVE, Mayon",
			"ESPANA, Welcome Rotunda", "ESPANA, Bluementritt",
			"ESPANA, A. Maceda", "ESPANA, Antipolo", "ESPANA, Vicente Cruz",
			"ESPANA, Gov. Forbes - Lacson", "ESPANA, P.Noval", "ESPANA, Lerma",
			"C5, Tandang Sora", "C5, Capitol Hills",
			"C5, University of the Philippines", "C5, C.P. Garcia",
			"C5, Miriam College", "C5, Ateneo De Manila University",
			"C5, Xavierville", "C5, Aurora Boulevard", "C5, P. Tuazon",
			"C5, Bonny Serrano", "C5, Libis Flyover", "C5, Eastwood",
			"C5, Green Meadows", "C5, Ortigas Ave.", "C5, J. Vargas",
			"C5, Lanuza", "C5, Bagong Ilog", "C5, Kalayaan",
			"C5, Market! Market!", "ORTIGAS, Santolan", "ORTIGAS, Madison",
			"ORTIGAS, Roosevelt", "ORTIGAS, Club Filipino", "ORTIGAS, Wilson",
			"ORTIGAS, Connecticut", "ORTIGAS, La Salle Greenhills",
			"ORTIGAS, POEA", "ORTIGAS, EDSA Shrine", "ORTIGAS, San Miguel Ave",
			"ORTIGAS, Meralco Ave", "ORTIGAS, Medical City",
			"ORTIGAS, Lanuza Ave", "ORTIGAS, Greenmeadows Ave",
			"ORTIGAS, C5 Flyover", "MARCOS HIGHWAY, SM City Marikina",
			"MARCOS HIGHWAY, LRT-2 Station", "MARCOS HIGHWAY, Dona Juana",
			"MARCOS HIGHWAY, Amang Rodriguez",
			"MARCOS HIGHWAY, F. Mariano Ave",
			"MARCOS HIGHWAY, Robinson's Metro East",
			"MARCOS HIGHWAY, San Benildo School", "ROXAS BLVD, Anda Circle",
			"ROXAS BLVD, Finance Road", "ROXAS BLVD, U.N. Avenue",
			"ROXAS BLVD, Pedro Gil", "ROXAS BLVD, Rajah Sulayman",
			"ROXAS BLVD, Quirino", "ROXAS BLVD, Pablo Ocampo",
			"ROXAS BLVD, Buendia", "ROXAS BLVD, Edsa Extension",
			"ROXAS BLVD, Baclaran", "ROXAS BLVD, Airport Road",
			"ROXAS BLVD, Coastal Road", "SLEX, Magallanes", "SLEX, Nichols",
			"SLEX, C5 On-ramp", "SLEX, Merville Exit", "SLEX, Bicutan Exit",
			"SLEX, Sucat Exit", "SLEX, Alabang Exit" };
	public final static String base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	private Constants() {
		return;
	}
}
