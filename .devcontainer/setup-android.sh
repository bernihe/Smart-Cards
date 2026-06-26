#!/bin/bash
set -e

echo "==> Step 1: Environment Priming (Rule 1)..."
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools

curl -sS https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip
unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools
mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
rm /tmp/cmdline-tools.zip

echo "==> Step 2: Invisible License Injection (Rule 4)..."
sudo mkdir -p /licenses
sudo chmod 777 /licenses
echo -e "89aa815992f028b1583d3557ef013af33d5174f8\n24333f8a63b6825ea9c5514f83c2829b004d1fee\nfa4c735d1f3b14a22e861d4c4ca64508c31312c1" > /licenses/android-sdk-license

mkdir -p $ANDROID_HOME/licenses
cp /licenses/android-sdk-license $ANDROID_HOME/licenses/android-sdk-license

echo "==> Step 3: Registering Global Environment Paths..."
echo "export ANDROID_HOME=$ANDROID_HOME" >> $HOME/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> $HOME/.bashrc
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

echo "==> Step 4: Structuring Missing Gradle Wrapper Execution Script..."
gradle wrapper --gradle-version 9.4.1

echo "==> Step 5: Resolving Target Framework Elements (Target SDK 36)..."
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-36" "build-tools;36.0.0"

chmod +x gradlew
echo "==> Cloud Environment Priming Complete!"
