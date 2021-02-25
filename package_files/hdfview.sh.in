#!/bin/bash

# File Name: hdfview.sh
# This script file is used to execute the hdfview utility

# Set up default variable values if not supplied by the user.
# ... hdfview.root property is for the install location
# ...... default location is system property user.dir
# ... hdfview.workdir property is for the working location to find files
# ...... default location is system property user.home
#

prg=$0
if [ ! -e "$prg" ]; then
  case $prg in
    (*/*) exit 1;;
    (*) prg=$(command -v -- "$prg") || exit;;
  esac
fi
dir=$(
  cd -P -- "$(dirname -- "$prg")" && pwd -P
) || exit

export INSTALLDIR=$dir
export JAVABIN=$INSTALLDIR/lib/runtime/bin
export JAVAOPTS=-Xmx1024M

#"$JAVABIN/java" "$JAVAOPTS" -Djava.library.path="$INSTALLDIR/lib/app" -Dhdfview.root="$INSTALLDIR/lib/app" -jar "$INSTALLDIR/lib/app/HDFView.jar" "$@"

# Default invocation when using modules
"$JAVABIN/java" "$JAVAOPTS" -Djava.library.path="$INSTALLDIR/lib/app:$INSTALLDIR/lib/app/ext" -Dhdfview.root="$INSTALLDIR/lib/app" -cp "$INSTALLDIR/lib/app/*" hdf.view.HDFView "$@"
