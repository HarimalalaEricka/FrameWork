@echo off
rem =====================================
rem Variables
rem =====================================
set SRC=src
set BIN=bin
set LIB=lib
set FRAMEWORK_JAR=%LIB%\framework.jar
set JAKARTA_JAR=%LIB%\servlet-api.jar

rem =====================================
rem Nettoyer ancien bin
rem =====================================
if exist %BIN% rmdir /s /q %BIN%
mkdir %BIN%

rem =====================================
rem Compiler toutes les classes du framework
rem =====================================
echo Compilation du framework...
javac -classpath "%JAKARTA_JAR%" -d %BIN% %SRC%\com\framework\*.java
if errorlevel 1 (
    echo Erreur de compilation!
    pause
    exit /b 1
)

rem =====================================
rem Créer le JAR
rem =====================================
if exist %FRAMEWORK_JAR% del /q %FRAMEWORK_JAR%
jar cvf %FRAMEWORK_JAR% -C %BIN% .
if errorlevel 1 (
    echo Erreur lors de la creation du JAR!
    pause
    exit /b 1
)

echo framework.jar genere avec succes dans %FRAMEWORK_JAR%
pause
