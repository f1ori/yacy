@Echo Off
title YaCy

REM setting startup type for proper restart
if not exist DATA md DATA
echo . >DATA\yacy.noconsole

If %1.==CPGEN. GoTo :CPGEN

Rem Generating the proper classpath unsing loops and labels
Set CLASSPATH=htroot
For %%X in (lib/*.jar) Do Call %0 CPGEN lib\%%X

REM Please change the "javastart" settings in the web-interface "Basic Configuration" -> "Advanced" 
set jmx=
set jms=
set javacmd=-Xmx600m -Xms180m
set priolvl=10
set priority=/BELOWNORMAL
if exist DATA\SETTINGS\httpProxy.conf GoTo :RENAMEINDEX
if exist DATA\SETTINGS\yacy.conf GoTo :GETSTARTOPTS

:STARTJAVA
set javacmd=%javacmd% -XX:-UseGCOverheadLimit -XX:+UseAdaptiveSizePolicy -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -Dfile.encoding=UTF-8
Rem Starting YaCy
Echo Generated classpath:%CLASSPATH%
Echo JRE Parameters:%javacmd%
Echo Priority:%priority%
Echo ***************************************************************************
Echo.
Echo If you see a message like "javaw" not found, you probably have to install Java.
Echo.
Echo You can download Java at http://java.com/
Echo.
Echo ***************************************************************************
start %priority% javaw %javacmd% -classpath %CLASSPATH% net.yacy.yacy
Echo You can close the console safely now.

GoTo :END

Rem PUBLIC is now freeworld (r4575)
:RENAMEINDEX
for /F "tokens=1,2 delims==" %%i in (DATA\SETTINGS\httpProxy.conf) do (
    if "%%i"=="network.unit.name" set networkname=%%j
)
if not defined networkname set networkname=PUBLIC
cd DATA\INDEX
ren PUBLIC %networkname%
cd ..
cd ..

Rem This target is used to read java runtime parameters out of the yacy config file
:GETSTARTOPTS
for /F "tokens=1,2 delims==" %%i in (DATA\SETTINGS\yacy.conf) do (
	if "%%i"=="javastart_Xmx" set jmx=%%j
	if "%%i"=="javastart_Xms" set jms=%%j
	if "%%i"=="javastart_priority" set priolvl=%%j
)
if defined jmx set javacmd=-%jmx%
if defined jms set javacmd=-%jms% %javacmd%
if defined priolvl (
	if %priolvl% == 20 set priority=/LOW
	if %priolvl% == 10 set priority=/BELOWNORMAL
)

GoTo :STARTJAVA

Rem This target is used to concatenate the classpath parts 
:CPGEN
Set CLASSPATH=%CLASSPATH%;%2

Rem Target needed to jump to the end of the file
:END
