@echo off
:: Copyright (c) 2008, Esko Luontola. All Rights Reserved.
::
:: This file is part of Darkstar EXP.
::
:: Darkstar EXP is free software: you can redistribute it and/or modify it
:: under the terms of the GNU General Public License version 2 as published by
:: the Free Software Foundation and distributed hereunder to you.
::
:: Darkstar EXP is distributed in the hope that it will be useful, but WITHOUT
:: ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
:: FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
:: more details.
::
:: You should have received a copy of the GNU General Public License along
:: with this program.  If not, see <http://www.gnu.org/licenses/>.


:: Startup script for Darkstar EXP. Loads dynamically all JAR files
:: in the library directories.
if %2"" == "" goto :help


:: Configure application parameters and paths
set APP_LIBRARY_DIR=%1
set APP_CONFIG_FILE=%2

set JAVA=java
if not "%JAVA_HOME%" == "" (
    set JAVA=%JAVA_HOME%\bin\java
)
if "%DARKSTAR_HOME%" == "" (
    set DARKSTAR_HOME=.
)
set NATIVE_LIBRARY_DIR=%DARKSTAR_HOME%\lib\win32-x86


:: Custom JVM options
set VMOPTIONS=
for /f "usebackq delims=" %%G in ("%DARKSTAR_HOME%\darkstar.vmoptions") do (call :add_to_vmoptions %%G)


:: Add all JARs in library dirs to classpath
set CP=
for /f %%G in ('dir /b "%DARKSTAR_HOME%\lib\*.jar"') do (call :add_to_classpath "%DARKSTAR_HOME%\lib\%%G")
for /f %%G in ('dir /b "%APP_LIBRARY_DIR%\*.jar"')   do (call :add_to_classpath "%APP_LIBRARY_DIR%\%%G")


:: Start up Darkstar Server
"%JAVA%" %VMOPTIONS% ^
    -Djava.library.path="%NATIVE_LIBRARY_DIR%" ^
    -Djava.util.logging.config.file="%DARKSTAR_HOME%\darkstar-logging.properties" ^
    -cp %CP% ^
    com.sun.sgs.impl.kernel.Kernel "%APP_CONFIG_FILE%"
goto :eof


:: These tricks are needed for Windows's FOR loops to work as expected. See http://www.ss64.com/nt/for.html

:add_to_vmoptions
    set VMOPTIONS=%VMOPTIONS% %1
    goto :eof

:add_to_classpath
    set CP=%CP%;%1
    goto :eof

:help
    echo Usage: darkstar APP_LIBRARY_DIR APP_CONFIG_FILE
    echo Starts up the Darkstar Server with the specified application.
    echo All libraries used by the application need to be as JAR files
    echo in the specified library directory.
    echo.
    echo Optional environmental variables:
    echo     DARKSTAR_HOME    Install path of Darkstar Server (default: .)
    echo     JAVA_HOME        Java Runtime Environment to use
    goto :eof

