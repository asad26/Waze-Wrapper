# Waze-Wrapper
OMI Wrapper for sending Waze data to sand box

Dependencies: Java, Maven, OMI Node


Steps for running this wrapper:
	
1 - Clone project folder: git clone https://github.com/asad26/Waze-Wrapper

2 - In resources/config.properties, add your consumer_key and consumer_secret from your Brussels Smart City APIs account

3 - Extract o-mi-node-0.9.2-warp10.zip and run OMI node from o-mi-node-0.9.2-warp10/bin directory

4 - Run following commands in the Wrapper project directory

	  mvn clean package
	  java -jar target\WrapperOmi-0.0.1-SNAPSHOT-jar-with-dependencies.jar api.wrapper.WrapperOmi.Main
	
5 - Access OMI sand box using localhost:8080 
