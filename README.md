# flutter_samsung_health_plugin

A comprehensive Flutter plugin for integrating Samsung Health features into your application. This plugin allows you to connect to Samsung Health, request permissions, read various health metrics, and manage auto-sync functionality.

## Features

- **Connection Management**: Connect and disconnect from the Samsung Health Data SDK.
- **Permission Handling**: Request and check permissions for various health data types.
- **Real-time Data Access**: Read current health metrics including:
    - Steps and Step Count History
    - Heart Rate
    - Sleep Data
    - Blood Oxygen (SpO2)
    - Blood Glucose
    - Blood Pressure
    - Body Composition (BMI, Body Fat, etc.)
    - Water Intake
    - Floors Climbed
    - Skin and Body Temperature
    - Nutrition and Exercise data
- **Auto Sync**: Synchronize health data automatically with a specified backend API.
- **Device Information**: Get list of connected health devices.

## Getting Started

### Android Setup

1. **Prerequisites**: Ensure you have the Samsung Health app installed on your testing device.
2. **Permissions**: Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
<uses-permission android:name="android.permission.BODY_SENSORS" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

3. **Service Declaration**: Ensure the `HealthSyncService` is declared within the `<application>` tag:

```xml
<service
    android:name="com.example.flutter_samsung_health_plugin.HealthSyncService"
    android:exported="false"
    android:foregroundServiceType="health" />
```

## Usage

### Initialize and Connect

```dart
import 'package:flutter_samsung_health_plugin/flutter_samsung_health_plugin.dart';

final _plugin = FlutterSamsungHealthPlugin();

// Connect to Samsung Health
String? result = await _plugin.connect();
```

### Permission Management

```dart
// Check if permissions are granted
bool hasPermission = await _plugin.checkPermissions();

// Request basic permissions
await _plugin.requestPermissions();

// Request specific step count permission
await _plugin.requestStepPermission();
```

### Reading Health Data

```dart
// Read current steps
List<dynamic>? steps = await _plugin.readSteps();

// Read heart rate
int? heartRate = await _plugin.readHeartRate();

// Read sleep data
List<dynamic>? sleep = await _plugin.readSleep();
```

### Historical Data

The plugin supports reading historical data for most metrics:

```dart
List<dynamic>? stepHistory = await _plugin.readStepsHistory();
List<dynamic>? hrHistory = await _plugin.readHeartRateHistory();
```

### Auto Sync Control

You can start a foreground service to automatically sync data to your server:

```dart
await _plugin.startAutoSync(
  apiUrl: "https://your-api.com/sync",
  userToken: "your-user-token",
  macAddress: "device-mac-address",
);

// Stop auto sync
await _plugin.stopAutoSync();
```

## Supported Metrics

| Metric | Current Data | Historical Data |
|--------|--------------|-----------------|
| Steps | `readSteps()` | `readStepsHistory()` |
| Heart Rate | `readHeartRate()` | `readHeartRateHistory()` |
| Sleep | `readSleep()` | `readSleepHistory()` |
| SpO2 | `readBloodOxygen()` | `readBloodOxygenHistory()` |
| Blood Glucose | `readBloodGlucose()` | `readBloodGlucoseHistory()` |
| Blood Pressure | `readBloodPressure()` | `readBloodPressureHistory()` |
| Body Composition | `readBodyComposition()` | `readBodyCompositionHistory()` |
| Water Intake | `readWaterIntake()` | `readWaterIntakeHistory()` |
| Nutrition | `readNutrition()` | `readNutritionHistory()` |
| Exercise | `readExercise()` | `readExerciseHistory()` |

## Troubleshooting

- Ensure the **Samsung Health** app is up to date.
- Developer mode might be required in Samsung Health settings to access data during development.
- Check logcat for detailed error messages from the native side.
