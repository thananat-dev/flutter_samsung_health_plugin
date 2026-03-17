import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_samsung_health_plugin_platform_interface.dart';

/// An implementation of [FlutterSamsungHealthPluginPlatform] that uses method channels.
class MethodChannelFlutterSamsungHealthPlugin
    extends FlutterSamsungHealthPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_samsung_health_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<String?> connect() async {
    final version = await methodChannel.invokeMethod<String>('connect');
    return version;
  }

  @override
  Future<String?> requestPermissions() async {
    final version = await methodChannel.invokeMethod<String>(
      'requestPermissions',
    );
    return version;
  }

  @override
  Future<bool> checkPermissions() async {
    final result = await methodChannel.invokeMethod<bool>('checkPermissions');
    return result ?? false;
  }

  @override
  Future<String?> requestStepPermission() async {
    final version = await methodChannel.invokeMethod<String>(
      'requestStepPermission',
    );
    return version;
  }

  @override
  Future<List<dynamic>?> readSteps() async {
    final steps = await methodChannel.invokeListMethod<dynamic>('readSteps');
    return steps;
  }

  @override
  Future<int?> readHeartRate() async {
    final heartRate = await methodChannel.invokeMethod<int>('readHeartRate');
    return heartRate;
  }

  @override
  Future<List<dynamic>?> readSleep() async {
    final sleep = await methodChannel.invokeListMethod<dynamic>('readSleep');
    return sleep;
  }

  @override
  Future<int?> readWaterIntake() async {
    final water = await methodChannel.invokeMethod<int>('readWaterIntake');
    return water;
  }

  @override
  Future<int?> readBloodOxygen() async {
    final oxygen = await methodChannel.invokeMethod<int>('readBloodOxygen');
    return oxygen;
  }

  @override
  Future<double?> readBloodGlucose() async {
    final glucose = await methodChannel.invokeMethod<double>(
      'readBloodGlucose',
    );
    return glucose;
  }

  @override
  Future<Map<String, dynamic>?> readBloodPressure() async {
    final bp = await methodChannel.invokeMapMethod<String, dynamic>(
      'readBloodPressure',
    );
    return bp;
  }

  @override
  Future<Map<String, dynamic>?> readBodyComposition() async {
    final composition = await methodChannel.invokeMapMethod<String, dynamic>(
      'readBodyComposition',
    );
    return composition;
  }

  @override
  Future<double?> readFloorsClimbed() async {
    final floors = await methodChannel.invokeMethod<double>(
      'readFloorsClimbed',
    );
    return floors;
  }

  // Phase 4
  @override
  Future<double?> readSkinTemperature() async {
    final temp = await methodChannel.invokeMethod<double>(
      'readSkinTemperature',
    );
    return temp;
  }

  @override
  Future<double?> readBodyTemperature() async {
    final temp = await methodChannel.invokeMethod<double>(
      'readBodyTemperature',
    );
    return temp;
  }

  @override
  Future<List<dynamic>?> readNutrition() async {
    final nutrition = await methodChannel.invokeListMethod<dynamic>(
      'readNutrition',
    );
    return nutrition;
  }

  @override
  Future<double?> readExercise() async {
    final val = await methodChannel.invokeMethod<double>('readExercise');
    return val;
  }

  // Phase 3: History
  @override
  Future<List<dynamic>?> readStepsHistory() async {
    return await methodChannel.invokeListMethod<dynamic>('readStepsHistory');
  }

  @override
  Future<List<dynamic>?> readHeartRateHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readHeartRateHistory',
    );
  }

  @override
  Future<List<dynamic>?> readSleepHistory() async {
    return await methodChannel.invokeListMethod<dynamic>('readSleepHistory');
  }

  @override
  Future<List<dynamic>?> readWaterIntakeHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readWaterIntakeHistory',
    );
  }

  @override
  Future<List<dynamic>?> readBloodOxygenHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readBloodOxygenHistory',
    );
  }

  @override
  Future<List<dynamic>?> readBloodGlucoseHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readBloodGlucoseHistory',
    );
  }

  @override
  Future<List<dynamic>?> readBloodPressureHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readBloodPressureHistory',
    );
  }

  @override
  Future<List<dynamic>?> readBodyCompositionHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readBodyCompositionHistory',
    );
  }

  @override
  Future<List<dynamic>?> readFloorsClimbedHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readFloorsClimbedHistory',
    );
  }

  @override
  Future<List<dynamic>?> readSkinTemperatureHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readSkinTemperatureHistory',
    );
  }

  @override
  Future<List<dynamic>?> readBodyTemperatureHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readBodyTemperatureHistory',
    );
  }

  @override
  Future<List<dynamic>?> readNutritionHistory() async {
    return await methodChannel.invokeListMethod<dynamic>(
      'readNutritionHistory',
    );
  }

  @override
  Future<List<dynamic>?> readExerciseHistory() async {
    return await methodChannel.invokeListMethod<dynamic>('readExerciseHistory');
  }

  @override
  Future<List<dynamic>?> getConnectedDevices() async {
    return await methodChannel.invokeListMethod<dynamic>('getConnectedDevices');
  }

  @override
  Future<String?> startAutoSync(
      {String? apiUrl, String? macAddress, String? userToken}) async {
    return await methodChannel.invokeMethod<String>('startAutoSync', {
      'apiUrl': apiUrl,
      'macAddress': macAddress,
      'userToken': userToken,
    });
  }

  @override
  Future<String?> stopAutoSync() async {
    return await methodChannel.invokeMethod<String>('stopAutoSync');
  }
}
