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
if exist %BIN% (
    echo Suppression de l'ancien dossier %BIN%...
    rmdir /s /q %BIN%
)
mkdir %BIN%

rem =====================================
rem Compiler toutes les classes du framework
rem =====================================
echo.
echo ===============================
echo Compilation du framework...
echo ===============================
dir /s /b %SRC%\*.java > sources.txt
javac -classpath "%JAKARTA_JAR%" -d %BIN% @sources.txt

if errorlevel 1 (
    echo.
    echo ❌ Erreur de compilation !
    del /q sources.txt
    pause
    exit /b 1
)

del /q sources.txt

rem =====================================
rem Créer le JAR
rem =====================================
echo.
echo ===============================
echo Creation du JAR...
echo ===============================
if exist %FRAMEWORK_JAR% del /q %FRAMEWORK_JAR%
jar cf %FRAMEWORK_JAR% -C %BIN% .

if errorlevel 1 (
    echo.
    echo ❌ Erreur lors de la creation du JAR !
    pause
    exit /b 1
)

echo.
echo ✅ framework.jar genere avec succes dans %FRAMEWORK_JAR%
pause
