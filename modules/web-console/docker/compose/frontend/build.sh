#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cd "$(dirname "$0")"

BUILD_DIR=$PWD/build
IMAGE_BUILD_CONTAINER=web-console-frontend-builder
IMAGE_BUILD_NAME=ignite/$IMAGE_BUILD_CONTAINER
IMAGE_NAME=ignite/web-console-frontend
CURRENT_USER=$(whoami)

echo "Step 1. Build frontend SPA"
mkdir -p $BUILD_DIR
sudo docker build -f=./DockerfileBuild -t $IMAGE_BUILD_NAME:latest ../../..
sudo docker run -it -v $BUILD_DIR:/opt/web-console-frontend/build --name $IMAGE_BUILD_CONTAINER $IMAGE_BUILD_NAME
sudo chown -R $CURRENT_USER $BUILD_DIR

echo "Step 2. Build NGINX container with SPA and proxy configuration"
sudo docker build -f=./Dockerfile -t $IMAGE_NAME:latest .

echo "Step 3. Cleanup"
sudo docker rm $IMAGE_BUILD_CONTAINER
sudo docker rmi $IMAGE_BUILD_NAME
rm -r $BUILD_DIR
