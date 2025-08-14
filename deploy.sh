#!/bin/bash

# Find the new JAR file in the build directory
BUILD_PATH=$(ls ./build/libs/*.jar | grep -v 'plain')
JAR_NAME=$(basename $BUILD_PATH)
echo "> New JAR file: $JAR_NAME"

# Set the deployment path on the server
DEPLOY_PATH=/home/ubuntu/

# Check if the application is currently running
echo "> Checking current PID"
CURRENT_PID=$(pgrep -f $JAR_NAME)

if [ -z $CURRENT_PID ]
then
  sleep 1
  echo "> No running application found."
else
  echo "> Stopping running application. PID: $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
fi

echo "> Deploying new application..."
cp $BUILD_PATH $DEPLOY_PATH

echo "> Starting new application JAR"
# Run the new JAR file in the background
nohup java -jar $DEPLOY_PATH$JAR_NAME > /dev/null 2>&1 &
