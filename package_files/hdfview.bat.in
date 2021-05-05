@ECHO OFF

rem File Name: hdfview.bat
rem This batch file is used to execute the hdfview utility
rem ... hdfview.root property is for the install location
rem ...... default location is system property user.dir
rem ... hdfview.workdir property is for the working location to find files
rem ...... default location is system property user.home
rem

setlocal enabledelayedexpansion
pushd %~dp0

rem Adjust the following variable to match your environment
set INSTALLDIR=%cd%\\..
set PATH=%INSTALLDIR%\\app;%PATH%
set JAVABIN=%INSTALLDIR%\\runtime\\bin

rem start "HDFView" "%JAVABIN%\\javaw.exe" %JAVAOPTS% -Xmx1024M -Djava.library.path="%INSTALLDIR%\\app" -Dhdfview.root="%INSTALLDIR%\\app" -jar "%INSTALLDIR%\\app\\HDFView.jar" %*

rem Default invocation when using modules
start "HDFView" "%JAVABIN%\\javaw.exe" %JAVAOPTS% -Xmx1024M -Djava.library.path="%INSTALLDIR%\\app;%INSTALLDIR%\\app\\ext" -Dhdfview.root="%INSTALLDIR%\\app" -cp "%INSTALLDIR%\\app\\*" hdf.view.HDFView %*

exit /b 0