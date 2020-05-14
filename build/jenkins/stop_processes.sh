#!/bin/sh

echo "Stopping netxmsd and nxagentd if they are running"

if test "x$BUILD_PREFIX" = "x"; then
	echo "ERROR: build prefix not set"
	exit 1
fi

BINDIR="$BUILD_PREFIX/bin"
USER=`whoami`
NEED_SLEEP=no

if ps -ae -o pid,user,args | grep -v grep | grep $USER | grep $BINDIR/netxmsd; then
    pid=`ps -ae -o pid,user,args | grep -v grep | grep $USER | grep $BINDIR/netxmsd | xargs | cut -d ' ' -f 1`
    kill $pid
    NEED_SLEEP=yes
fi

if ps -ae -o pid,user,args | grep -v grep | grep $USER | grep $BINDIR/nxagentd; then
    if [ -f $BUILD_PREFIX/agent.pid ]; then
        pid=`cat $BUILD_PREFIX/agent.pid`
    else
        pid=`ps -ae -o pid,user,args | grep -v grep | grep $USER | grep $BINDIR/nxagentd | xargs | cut -d ' ' -f 1`
    fi
    kill $pid
    NEED_SLEEP=yes
fi

if test "x$NEED_SLEEP" = "xyes"; then
    count=1
    while ps -ae -o pid,user,args | grep -v grep | grep $USER | grep $BINDIR/netxmsd; do
        if test $count -gt 20; then
            echo "netxmsd is running, but should be already stopped"
            exit 1
        fi
	count=$((count+1))
        sleep 5
    done
fi

exit 0
