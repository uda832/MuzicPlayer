#!/usr/local/bin/bash

echo "" >> build_log.log &&
date >> build_log.log &&
./gradlew :app:assembleDebug --configure-on-demand --daemon --parallel >> build_log.log
adb push ./app/build/outputs/apk/app-debug.apk /data/local/tmp/com.example.ud4.muzicplayer
adb shell pm install -r "/data/local/tmp/com.example.ud4.muzicplayer"
