#!/usr/bin/env bash

server="$1"

if [[ "$server" == "localhost" ]]; then
    pushd ../test/lib || exit
    java -jar Segmented-File-System-server.jar &
    serverPID="$!"
    popd || exit
fi

# Run this from the src directory.

# (Re)compile the code
rm -f segmentedfilesystem/*.class
javac segmentedfilesystem/*.java

# Run the client
java segmentedfilesystem.Main "$server"

if [[ $serverPID ]]; then
    kill "$serverPID"
fi
