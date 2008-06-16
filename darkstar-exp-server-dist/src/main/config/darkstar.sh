#!/bin/sh
# Copyright (c) 2008, Esko Luontola. All Rights Reserved.
#
# This file is part of Darkstar EXP.
#
# Darkstar EXP is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 as published by
# the Free Software Foundation and distributed hereunder to you.
#
# Darkstar EXP is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
# more details.
#
# You should have received a copy of the GNU General Public License along
# with this program.  If not, see <http://www.gnu.org/licenses/>.


# Startup script for Darkstar EXP. Loads dynamically all JAR files
# in the library directories.
if [ $# -ne 2 ]; then
    echo "Usage: darkstar APP_LIBRARY_DIR APP_CONFIG_FILE"
    echo "Starts up the Darkstar Server with the specified application."
    echo "All libraries used by the application need to be as JAR files"
    echo "in the specified library directory."
    echo
    echo "Optional environmental variables:"
    echo "    DARKSTAR_HOME    Install path of Darkstar Server (default: .)"
    echo "    JAVA_HOME        Java Runtime Environment to use"
    exit 1
fi


# Configure application parameters and paths
APP_LIBRARY_DIR="$1"
APP_CONFIG_FILE="$2"

JAVA=java
if [ -n "$JAVA_HOME" ]; then
    JAVA=$JAVA_HOME/bin/java
fi
if [ -z "$DARKSTAR_HOME" ]; then
    DARKSTAR_HOME="."
fi


# Figure out what platform we're running on and set the platform and
# pathsep variables appropriately.  Here are the supported platforms:
#
# OS		Hardware	Platform	Path Separator
# --------	--------	--------------	--------------
# Mac OS X	PowerPC		macosx-ppc	:
# Mac OS X	Intel x86	macosx-x86	:
# Solaris	Intel x86	solaris-x86	:
# Solaris	Sparc		solaris-sparc	:
# Linux		Intel x86	linux-x86	:
# Linux		Intel x86_64	linux-x86_64	:
# Windows	Intel x86	win32-x86	;
#
platform=unknown
os=`uname -s`
case $os in
    Darwin)
	pathsep=":"
	mach=`uname -p`
	case $mach in
	    powerpc)
		platform=macosx-ppc;;
	    i386)
	    	platform=macosx-x86;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    SunOS)
	pathsep=":"
	mach=`uname -p`
	case $mach in
	    i386)
	    	platform=solaris-x86;;
	    sparc)
	    	platform=solaris-sparc;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    Linux)
	pathsep=":"
	mach=`uname -m`;
	case $mach in
	    i686)
		platform=linux-x86;;
	    x86_64)
		platform=linux-x86_64;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    CYGWIN*)
	pathsep=";"
	mach=`uname -m`;
	case $mach in
	    i686)
		platform=win32-x86;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    *)
	echo Unknown operating system: $os;
	exit 1;
esac
NATIVE_LIBRARY_DIR=$DARKSTAR_HOME/lib/$platform


# Custom JVM options (comments start with ";")
VMOPTIONS=`sed -e 's/;.*$//' "$DARKSTAR_HOME/darkstar.vmoptions"`


# Add all JARs in library dirs to classpath
CP=
for FILE in $DARKSTAR_HOME/lib/*.jar; do
    CP=$CP$pathsep$FILE
done
for FILE in $APP_LIBRARY_DIR/*.jar; do
    CP=$CP$pathsep$FILE
done


# Start up Darkstar Server
"$JAVA" $VMOPTIONS \
    -Djava.library.path="$NATIVE_LIBRARY_DIR" \
    -Djava.util.logging.config.file="$DARKSTAR_HOME\darkstar-logging.properties" \
    -cp $CP \
    com.sun.sgs.impl.kernel.Kernel "$APP_CONFIG_FILE"
