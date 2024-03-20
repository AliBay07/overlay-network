#!/bin/bash

export CP=.:jars/amqp-client-5.16.0.jar:jars/slf4j-api-1.7.36.jar:jars/slf4j-simple-1.7.36.jar

javac -cp $CP -d bin src/main/java/*.java

gnome-terminal -- java -cp $CP:bin PhysicalNode 2

gnome-terminal -- java -cp $CP:bin PhysicalNode 1

gnome-terminal -- java -cp $CP:bin PhysicalNode 4

gnome-terminal -- java -cp $CP:bin PhysicalNode 0 4

