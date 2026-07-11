#!/bin/bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
PACKAGE="com.nhimz.vocabmaster"
ACTIVITY=".MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"
LOG_FILE="debug_log.txt"

echo "==================================="
echo "[0/4] Đang build APK mới..."
echo "==================================="
gradle assembleDebug -Dorg.gradle.jvmargs="-Xmx1536m" || { echo "Lỗi khi build APK! Vui lòng kiểm tra code."; exit 1; }

echo "==================================="
echo "[1/4] Đang cài đặt APK..."
echo "==================================="
adb install -r $APK || { echo "Lỗi cài đặt! Hãy kiểm tra lại kết nối ADB."; exit 1; }

echo "==================================="
echo "[2/4] Đang xóa sạch log rác cũ..."
echo "==================================="
adb logcat -c

echo "==================================="
echo "[3/4] Đang mở ứng dụng trên điện thoại..."
echo "==================================="
adb shell am start -n $PACKAGE/$ACTIVITY

echo "==================================="
echo "[4/4] ĐANG GHI LOG VÀO FILE: $LOG_FILE"
echo "👉 Hãy cầm máy lên và dùng app. Nếu app crash hoặc test xong, hãy quay lại đây bấm phím [ Ctrl + C ] để DỪNG."
echo "==================================="

# Lọc log theo package name và ghi thẳng vào file
adb logcat | grep --line-buffered $PACKAGE > $LOG_FILE
