package com.atlach.TrafficDataIndexer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.atlach.TrafficDataAggregator.TrafficDataAggregatorMain;

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
 * <b>TrafficDataIndexerUI Class</b> </br>Provides a Graphical User Interface
 * for the TrafficDataIndexer and the TrafficDataAggregator. The old name was
 * retained for historical purposes.
 * 
 * @author francis
 * 
 */
public class TrafficDataIndexerUI extends JFrame implements ActionListener,
		TrafficDataIndexerNotifier {
	private TrafficDataIndexerMain trafficIndexer = null;
	private TrafficDataAggregatorMain trafficAggregator = null;
	private JPanel mPanel = null;
	private JButton mStartButton = null;
	private JButton mRegenButton = null;
	private JButton mStopButton = null;
	private JLabel mLabel = null;
	private JLabel mStatusLabel = null;
	private String filename;
	private String timestamp;

	/**
	 * 
	 */
	private static final long serialVersionUID = -789431375631112798L;

	public TrafficDataIndexerUI() {
		mPanel = new JPanel();
		mLabel = new JLabel("Traffic Data Indexer v1.3");
		mStatusLabel = new JLabel("---");
		mStatusLabel.setPreferredSize(new Dimension(390, 30));
		mStatusLabel.setHorizontalAlignment(JLabel.CENTER);

		mStartButton = new JButton("Start");
		mStartButton.addActionListener(this);
		mStartButton.setPreferredSize(new Dimension(100, 20));
		mStopButton = new JButton("Stop");
		mStopButton.setPreferredSize(new Dimension(100, 20));
		mStopButton.addActionListener(this);
		mRegenButton = new JButton("Regen");
		mRegenButton.addActionListener(this);
		mRegenButton.setPreferredSize(new Dimension(100, 20));

		mPanel.add(mLabel);
		mPanel.add(mStatusLabel);
		mPanel.add(mStartButton);
		mPanel.add(mRegenButton);
		mPanel.add(mStopButton);

		// Window Listeners
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (trafficIndexer != null) {
					trafficIndexer.stop();
				}

				if (trafficAggregator != null) {
					trafficAggregator.stopOperation();
				}
				System.exit(0);
			} // windowClosing
		});
		this.add(mPanel);
	}

	/**
	 * Starts the Traffic Data Indexer
	 */
	public void startTrafficDataIndexer() {
		if (trafficIndexer == null) {
			trafficIndexer = new TrafficDataIndexerMain(this, true);
		}
		trafficIndexer.start();
	}

	@Override
	public void onStatusUpdate(String s) {
		// TODO Auto-generated method stub
		mStatusLabel.setText(s);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mStartButton) {
			if (trafficIndexer == null) {
				trafficIndexer = new TrafficDataIndexerMain(this, true);
			}
			trafficIndexer.start();
		} else if (e.getSource() == mRegenButton) {
			if (trafficAggregator == null) {
				trafficAggregator = new TrafficDataAggregatorMain();
			}

			trafficAggregator.runGenerateHistDataTask("Traffic_Record");
		} else if (e.getSource() == mStopButton) {
			if (trafficIndexer == null) {
				return;
			}
			trafficIndexer.stop();
			mStatusLabel.setText("---");
		}
	}

	@Override
	public void onUpdateDone(String message) {
		if (trafficAggregator == null) {
			trafficAggregator = new TrafficDataAggregatorMain();
		}

		trafficAggregator.runPushDataTask(filename, timestamp);

	}

	@Override
	public void onTrafficDataFileSaved(String filename, String timestamp) {
		this.filename = filename;
		this.timestamp = timestamp;
	}

	/**
	 * Runs the GUI
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		TrafficDataIndexerUI idxrUI = new TrafficDataIndexerUI();

		idxrUI.setTitle("Traffic Data Indexer v1.3");
		idxrUI.setSize(400, 150);
		idxrUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		idxrUI.setVisible(true);
	}
}
