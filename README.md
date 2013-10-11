<h3>MMDA Traffic Data Indexer and Aggregator</h3>
<p>
This repository contains the MMDA Traffic Data Indexer and Aggregator backend software used by the 
Viaje Android App (http://appcircus.com/apps/viaje-2) for obtaining both the current and historical
traffic data information. The program itself is ran periodically in Viaje's statistics server in
order to generate the relevant information.
</p>
<h5>What does it do?</h5>
<p>
Basically, it reads traffic information from the Metro Manila Development Authority's (MMDA) 
<a href="mmdatraffic.interaksyon.com">traffic monitoring website</a>, packages it into a simpler
format usable by our app, and generates a data file containing traffic information in our
statistics server. Along the way, it also updates a much larger historical data file whenever
appropriate. The information contained in both these files are then relayed to our app, which
then uses it to incorporate current or historical traffic data information to the user.
<br/>
<br/>
Simply put, it generates current and historical traffic data information the raw data in MMDA's website.
</p>
<h5>How can I try this out?</h5>
<p>
There are three ways you can do this. You can either...
<ul>...Download the Viaje app from the Android Playstore to see how the data put into action</ul>
<ul>...Follow the links below to see samples of the generated traffic data files.
  <ul>+ Current Traffic Data File (Indexer-generated, once every 15 mins): http://54.251.186.10:8090/mmda_traffic.txt</ul>
  <ul>+ Historical Data File (Aggregator-generated, once every day): http://54.251.186.10:8090/TrafficData.hist</ul>
</ul>
<ul>...Deploy your own! Simply clone the repository, build it using Eclipse, and then generate a runnable JAR.</ul>
</p>
<h5>Can I use this code? Can I modify it? Are there any licensing issues I should know about?</h5>
<p>
Yes, you can freely use/modify/distribute this code so long as you agree with the Terms and Conditions of the 
GNU General Public License version 3 -- a copy of which was provided 
<a href="https://github.com/Francis-T/mmda-trafficdata-indexer/blob/master/COPYING">here</a>
for your perusal.
</p>
