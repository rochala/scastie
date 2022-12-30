#!/usr/bin/env bash
set -e
set -o pipefail

echo "[0/7] Deploying Metals"
echo "[1/7] Removing old previous image"
docker rmi scalacenter/scastie-metals-runner:previous || true

echo "[2/7] Tagging running image as the new previous"
docker tag scalacenter/scastie-metals-runner:latest scalacenter/scastie-metals-runner:previous || true

echo "[3/7] Fetching new latest from repository"
docker pull scalacenter/scastie-metals-runner:latest

echo "[4/7] Stopping scastie-metals container"
docker ps -q --filter "ancestor=scalacenter/scastie-metals-runner:latest" | xargs docker stop

echo "[5/7] Removing scastie-metals container"
docker ps -a -q --filter "status=exited" --filter "ancestor=scalacenter/scastie-metals-runner:latest" | xargs docker rm

echo "[6/7] Starting Metals docker container"

./start-metals.sh

echo "######################################################"

echo "Docker images:"
docker images

echo "Docker containers:"

docker ps -a

echo "######################################################"


echo "[7/7] Scastie Metals deployed successfully"

exit