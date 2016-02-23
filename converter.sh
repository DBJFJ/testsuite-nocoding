#!/bin/sh

# setup basic paths
CWD=`pwd`
TSNC_HOME=`pwd`
XLT_HOME=`pwd`/../../xlt-4.5.5
export XLT_HOME
export TSNC_HOME

if [ -z "$XLT_CONFIG_DIR" ]; then
    XLT_CONFIG_DIR=$TSNC_HOME/config
    export XLT_CONFIG_DIR
fi

# setup Java class path
CLASSPATH="$TSNC_HOME"/classes:"$TSNC_HOME"/lib/*:"$XLT_HOME"/lib/*

# setup other Java options
JAVA_OPTIONS=
JAVA_OPTIONS="$JAVA_OPTIONS -Dcom.xceptance.xlt.home=\"$TSNC_HOME\""
JAVA_OPTIONS="$JAVA_OPTIONS -Dlog4j.configuration=\"file:$XLT_CONFIG_DIR/agentcontroller.properties\""
#JAVA_OPTIONS="$JAVA_OPTIONS -agentlib:jdwp=transport=dt_socket,address=localhost:6666,server=y,suspend=n"
JAVA_OPTIONS="$JAVA_OPTIONS -cp \"$CLASSPATH\""

# fix #35
JAVA_OPTIONS="$JAVA_OPTIONS -Djava.security.egd=file:/dev/./urandom"

# DEBUG
echo CLASSPATH: $CLASSPATH

# run Java
CMD="java $JAVA_OPTIONS com.xceptance.xlt.common.util.action.data.Jmeter.JmeterConverter"
ARGS=""
I=1
while [ $I -le $# ]; do
    eval x=\${$I}
    ARGS="$ARGS \"$x\""
    I=$((I+1))
done
eval $CMD "$ARGS"
