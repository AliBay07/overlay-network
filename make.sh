#!/bin/bash

if [ -z "$1" ]; then
    FILE="graphs/config1.txt"
else
    FILE="graphs/$1"
fi

if [ ! -f "$FILE" ]; then
    echo "File $FILE doesn't exist."
    exit 1
fi

if [ -f "log/log.txt" ]; then
    rm log/log.txt
fi

NUM_LINES=$(wc -l < "$FILE")

export CP=.:jars/amqp-client-5.16.0.jar:jars/slf4j-api-1.7.36.jar:jars/slf4j-simple-1.7.36.jar

javac -cp "$CP" -d bin src/main/java/*.java

for ((i=0; i<= NUM_LINES; i++)); do
    gnome-terminal -- java -cp "$CP:bin" PhysicalNode "$i" "$FILE"
done

if [ "$2" = "-g" ]; then
    python3 graph-generator/graph-generator.py "$FILE"
    for ((i=0; i<= NUM_LINES; i++)); do
        gnome-terminal -- java -cp "$CP:bin" VirtualNode "$i" "$FILE" true
    done
else
    for ((i=0; i<= NUM_LINES; i++)); do
        gnome-terminal -- java -cp "$CP:bin" VirtualNode "$i" "$FILE"
    done
fi

