#!/bin/bash

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.." || exit 1
PROJECT_DIR=$PWD

DOCKER_IMAGE="$1"
OS_ARCH="$2"

echo "Building docker container..."
echo " dockerImage: $DOCKER_IMAGE"
echo " osArch: $OS_ARCH"

DOCKERFILE="setup/Dockerfile.linux-generic"
if [[ "$OS_ARCH" == *"linux_musl"* ]]; then
  DOCKERFILE="setup/Dockerfile.linux_musl-generic"
fi

docker build -f "$DOCKERFILE" --progress=plain --build-arg "FROM_IMAGE=${DOCKER_IMAGE}" -t "tokyocabinet-${OS_ARCH}" "$PROJECT_DIR/setup" || exit 1