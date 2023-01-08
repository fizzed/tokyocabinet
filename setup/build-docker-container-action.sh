#!/bin/sh -l
# Use a shell as though we logged in

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.." || exit 1
PROJECT_DIR=$PWD

USERID=$(id -u ${USER})
USERNAME=${USER}

DOCKER_IMAGE="$1"
CONTAINER_NAME="$2"
BUILDOS=$3
BUILDARCH=$4

echo "Building docker container..."
echo " dockerImage: $DOCKER_IMAGE"
echo " containerName: $CONTAINER_NAME"

DOCKERFILE="setup/Dockerfile.linux-generic"
if [ $BUILDOS = "linux_musl" ]; then
  DOCKERFILE="setup/Dockerfile.linux_musl-generic"
fi

docker build -f "$DOCKERFILE" --progress=plain --build-arg "FROM_IMAGE=${DOCKER_IMAGE}" --build-arg USERID=${USERID} --build-arg USERNAME=${USERNAME} -t ${CONTAINER_NAME} "$PROJECT_DIR/setup" || exit 1