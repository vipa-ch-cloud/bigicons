#!/usr/bin/env bash
# Превью (drawable/widget_preview_CxR, layout/widget_preview_CxR) лежат в репозитории и здесь не генерируются.
# Генерирует провайдеры виджетов, XML и манифест. Запуск из корня проекта.
set -e
P=app/src/main; K=$P/kotlin/dev/bigicons/app
SIZES="1x1 1x2 2x1 2x2 2x3 3x2 3x3 4x1 1x4 4x2 4x3 4x4"
rm -f $P/res/xml/widget_*.xml $K/widget/Widget[0-9]x[0-9].kt
{
echo '<?xml version="1.0" encoding="utf-8"?>'
echo '<resources>'
echo '    <string name="app_name">BigIcons</string>'
echo '    <string name="widget_desc">Увеличенная иконка приложения в стиле вашего лаунчера</string>'
for s in $SIZES; do c=${s%x*}; r=${s#*x}; echo "    <string name=\"widget_label_$s\">Иконка ${c}×${r}</string>"; done
echo '</resources>'
} > $P/res/values/strings.xml
for s in $SIZES; do c=${s%x*}; r=${s#*x}
cat > $P/res/xml/widget_$s.xml <<EOF
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="$((70*c-30))dp"
    android:minHeight="$((70*r-30))dp"
    android:minResizeWidth="$((70*c-30))dp"
    android:minResizeHeight="$((70*r-30))dp"
    android:maxResizeWidth="250dp"
    android:maxResizeHeight="250dp"
    android:targetCellWidth="$c"
    android:targetCellHeight="$r"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_icon"
    android:previewImage="@drawable/widget_preview_$s"
    android:previewLayout="@layout/widget_preview_$s"
    android:configure="dev.bigicons.app.ConfigActivity"
    android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable"
    android:description="@string/widget_desc" />
EOF
printf 'package dev.bigicons.app.widget\n\nclass Widget%s : BigIconProvider()\n' $s > $K/widget/Widget$s.kt
done
{
cat <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".BigIconsApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.BigIcons">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ConfigActivity"
            android:exported="true"
            android:label="Настройка виджета"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.SystemReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".widget.PackageReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.PACKAGE_ADDED" />
                <action android:name="android.intent.action.PACKAGE_REPLACED" />
                <action android:name="android.intent.action.PACKAGE_REMOVED" />
                <action android:name="android.intent.action.PACKAGE_CHANGED" />
                <data android:scheme="package" />
            </intent-filter>
        </receiver>

EOF
for s in $SIZES; do cat <<EOF
        <receiver
            android:name=".widget.Widget$s"
            android:exported="true"
            android:label="@string/widget_label_$s">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_$s" />
        </receiver>

EOF
done
echo '    </application>'; echo '</manifest>'
} > $P/AndroidManifest.xml
