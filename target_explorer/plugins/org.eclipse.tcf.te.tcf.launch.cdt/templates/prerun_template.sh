#!/bin/sh
INITIALDIR=`pwd`

# Run script with a different user
case x$1 in x-u__)
    USERID=$2
    shift 2
    echo "Switching to user \"$USERID\" using \"sudo -u\"."
    sudo -H -u $USERID /bin/sh $0 $@
    exitcode=$?
    if [ $exitcode -ne 0 ]; then
        echo -e "The remote launch failed. Possible reasons:\n  -\"sudo\" is missing on the target.\n  - The user \"$USERID\" does not exist on the target."
    fi
    rm -f "$0"
    exit $exitcode
    ;;
esac

# Commands
{0}

# Launch app
{1}

# Self-destruction
cd "$INITIALDIR" && rm -f "$0" 2>/dev/null
exit 0
