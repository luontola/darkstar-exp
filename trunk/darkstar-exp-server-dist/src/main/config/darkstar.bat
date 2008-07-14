@echo off
:: Copyright (c) 2008, Esko Luontola. All Rights Reserved.
::
:: Redistribution and use in source and binary forms, with or without modification,
:: are permitted provided that the following conditions are met:
::
::     * Redistributions of source code must retain the above copyright notice,
::       this list of conditions and the following disclaimer.
::     * Redistributions in binary form must reproduce the above copyright notice,
::       this list of conditions and the following disclaimer in the documentation
::       and/or other materials provided with the distribution.
::
:: THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
:: ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
:: WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
:: DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
:: ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
:: (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
:: LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
:: ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
:: (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
:: SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


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
    -Ddarkstar.exp.config.file="%DARKSTAR_HOME%\darkstar-exp.properties" ^
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

