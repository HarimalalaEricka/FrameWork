@echo off
REM =====================================
REM Variables
REM =====================================
set SRC=src
set BIN=bin
set LIB=lib
set FRAMEWORK_JAR=%LIB%\framework.jar
set JAKARTA_JAR=%LIB%\servlet-api.jar
set REFLECTIONS_JAR=%LIB%\reflections-0.9.10.jar
set GUAVA_JAR=%LIB%\guava-31.1-jre.jar
set JAVASSIST_JAR=%LIB%\javassist-3.29.2-GA.jar

REM =====================================
REM Nettoyer ancien bin
REM =====================================
if exist "%BIN%" (
    echo Suppression de l'ancien dossier %BIN%...
    rmdir /S /Q "%BIN%"
)
mkdir "%BIN%"

REM =====================================
REM Compiler toutes les classes du framework
REM =====================================
echo.
echo ===============================
echo Compilation du framework...
echo ===============================

REM Créer un fichier temporaire avec toutes les sources
dir /S /B "%SRC%\*.java" > sources.txt

javac -classpath "%JAKARTA_JAR%;%REFLECTIONS_JAR%;%GUAVA_JAR%;%JAVASSIST_JAR%" -d "%BIN%" @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Erreur de compilation !
    del sources.txt
    pause
    exit /b 1
)

del sources.txt

REM =====================================
REM Créer le JAR
REM =====================================
echo.
echo ===============================
echo Creation du JAR...
echo ===============================

if exist "%FRAMEWORK_JAR%" (
    del "%FRAMEWORK_JAR%"
)

jar cf "%FRAMEWORK_JAR%" -C "%BIN%" .

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Erreur lors de la creation du JAR !
    pause
    exit /b 1
)

echo.
echo ✅ framework.jar généré avec succès dans %FRAMEWORK_JAR%
pause
