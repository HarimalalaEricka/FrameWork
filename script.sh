#!/bin/bash
# =====================================
# Variables
# =====================================
SRC="src"
BIN="bin"
LIB="lib"
FRAMEWORK_JAR="$LIB/framework.jar"
JAKARTA_JAR="$LIB/servlet-api.jar"

# =====================================
# Nettoyer ancien bin
# =====================================
if [ -d "$BIN" ]; then
    echo "Suppression de l'ancien dossier $BIN..."
    rm -rf "$BIN"
fi
mkdir -p "$BIN"

# =====================================
# Compiler toutes les classes du framework
# =====================================
echo
echo "==============================="
echo "Compilation du framework..."
echo "==============================="

find "$SRC" -name "*.java" > sources.txt
javac -classpath "$JAKARTA_JAR" -d "$BIN" @sources.txt

if [ $? -ne 0 ]; then
    echo
    echo "❌ Erreur de compilation !"
    rm -f sources.txt
    read -p "Appuyez sur Entrée pour quitter..."
    exit 1
fi

rm -f sources.txt

# =====================================
# Créer le JAR
# =====================================
echo
echo "==============================="
echo "Création du JAR..."
echo "==============================="

if [ -f "$FRAMEWORK_JAR" ]; then
    rm -f "$FRAMEWORK_JAR"
fi

jar cf "$FRAMEWORK_JAR" -C "$BIN" .

if [ $? -ne 0 ]; then
    echo
    echo "❌ Erreur lors de la création du JAR !"
    read -p "Appuyez sur Entrée pour quitter..."
    exit 1
fi

echo
echo "✅ framework.jar généré avec succès dans $FRAMEWORK_JAR"
read -p "Appuyez sur Entrée pour terminer..."
