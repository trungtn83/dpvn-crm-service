#!/bin/bash

# Build the project
./gradlew spotlessApply
./gradlew build

# Check if the build succeeded
if [ $? -eq 0 ]; then
    # If the build succeeded, copy the jar file
    scp "build/libs/dpvn-kiotviet-service-0.0.1-SNAPSHOT.jar" root@103.1.239.43:/apps/kiotviet
    echo "Build and Copy progress completed."
else
    # If the build failed, print an error message and exit
    echo "Build failed. Stopping script execution."
    exit 1
fi
