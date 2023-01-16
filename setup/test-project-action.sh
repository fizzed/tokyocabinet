#!/bin/sh -li
set -e
# shell w/ login & interactive, plus exit if any command fails

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.."
PROJECT_DIR=$PWD

# fix path on stupid macos
#if [ -x /usr/libexec/path_helper ]; then
#	eval `/usr/libexec/path_helper -s`
#fi

# are we in a container?, if our project starts with /project then we are
case $PROJECT_DIR in /project*)
  echo "We are in a container! (creating temp .m2)"
  # make temp .m2 to cache dependencies
  mkdir -p $PROJECT_DIR/.temp-m2
  ln -s /project/.temp-m2 $HOME/.m2
esac

env
ls -la

mvn test