#!/bin/bash
set -e

SCRIPT_DIR=$( dirname "$0" )
if [ -z "$PROJECT_ROOT_PATH" ]; then
  PROJECT_ROOT_PATH=`cd $SCRIPT_DIR/../.. ; pwd`
fi

if [ -z "$REVISION" ]; then
  if [ ! -z "$SHARED_VARS_FILE" ] && [ -f "$SHARED_VARS_FILE" ]; then
    . $SHARED_VARS_FILE
    export $(cut -d= -f1 $SHARED_VARS_FILE)
  fi
fi
if [ -z "$REVISION" ]; then
  echo "\$REVISION is not set" >&2
  exit 1
fi
if [ -z "$MVN_STAGES" ]; then
  MVN_STAGES="clean install"
fi
if [ -z "$SETTINGS_XML" ]; then
  SETTINGS_XML=$PROJECT_ROOT_PATH/.m2/settings.xml
fi

sh $SCRIPT_DIR/print-environment.sh "build-core"

cd $PROJECT_ROOT_PATH/core-parent

mvn versions:set -DnewVersion=$REVISION -Drevision=$REVISION

mvn -s $SETTINGS_XML \
    --batch-mode \
    $MVN_STAGES \
    -Drevision=$REVISION \
    -Dskip.assemble-zip \
    $MVN_ADDITIONAL_OPTS

mvn versions:revert -Drevision=$REVISION

cd $PROJECT_ROOT_PATH