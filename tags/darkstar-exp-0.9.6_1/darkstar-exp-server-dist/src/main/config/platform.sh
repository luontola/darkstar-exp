#!/bin/sh
# Copyright 2007-2008 by Sun Microsystems, Inc. All rights reserved
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


# Figure out what platform we're running on and set the PLATFORM and
# PATHSEP variables appropriately.  Here are the supported platforms:
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
PLATFORM=unknown
os=`uname -s`
case $os in
    Darwin)
	PATHSEP=":"
	mach=`uname -p`
	case $mach in
	    powerpc)
		PLATFORM=macosx-ppc;;
	    i386)
	    	PLATFORM=macosx-x86;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    SunOS)
	PATHSEP=":"
	mach=`uname -p`
	case $mach in
	    i386)
	    	PLATFORM=solaris-x86;;
	    sparc)
	    	PLATFORM=solaris-sparc;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    Linux)
	PATHSEP=":"
	mach=`uname -m`;
	case $mach in
	    i686)
		PLATFORM=linux-x86;;
	    x86_64)
		PLATFORM=linux-x86_64;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    CYGWIN*)
	PATHSEP=";"
	mach=`uname -m`;
	case $mach in
	    i686)
		PLATFORM=win32-x86;;
	    *)
		echo Unknown hardware: $mach;
		exit 1;
	esac;;
    *)
	echo Unknown operating system: $os;
	exit 1;
esac
