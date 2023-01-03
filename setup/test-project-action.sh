#!/bin/sh

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.." || exit 1
PROJECT_DIR=$PWD

# fix path on stupid macos
if [ -x /usr/libexec/path_helper ]; then
	eval `/usr/libexec/path_helper -s`
fi

# make temp .m2 to cache dependencies
mkdir -p /project/.temp-m2
ln -s /project/.temp-m2 $HOME/.m2

mvn clean
mvn test