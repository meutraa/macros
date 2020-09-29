#!/usr/bin/env bash
#!/bin/bash

set -e

NAME="macros"
ID="host/lost/macros"
BTV="29.0.3"
PTV="30.0.0"
SV="android-29"
SDK="$HOME/sdk"

AAPT="$SDK/build-tools/$BTV/aapt2"
ZIPALIGN="$SDK/build-tools/$BTV/zipalign"
APKSIGNER="$SDK/build-tools/$BTV/apksigner"
PLATFORM="$SDK/platforms/$SV/android.jar"

mkdir -p obj
rm -f $NAME.unaligned.apk

echo "Compiling java"
javac -d obj -cp $PLATFORM -source 11 -target 11 MainActivity.java

echo "Optimizing with R8"
CLASSES=$(find obj -name "*.class" | tr '\n' ' ')
java -cp r8-1.6.84.jar com.android.tools.r8.R8 --release --output . --pg-conf proguard.txt  --min-api 29 --lib "${PLATFORM}" ${CLASSES}

echo "Creating apk"
$AAPT link -o $NAME.unaligned.apk --manifest AndroidManifest.xml -I $PLATFORM
zip -uj $NAME.unaligned.apk classes.dex
advzip -4 -i 256 -z $NAME.unaligned.apk

echo "Aligning and signing apk" 
$ZIPALIGN -f 4 $NAME.unaligned.apk $NAME.apk
$APKSIGNER sign --ks release.keystore \
    --v1-signing-enabled false \
    --v2-signing-enabled false \
    --v3-signing-enabled true \
    --ks-pass "pass:Toekabee" --key-pass "pass:Toekabee" $NAME.apk

echo "Cleaning..."
rm -rf obj
rm -f $NAME.unaligned.apk
rm -f classes.dex
