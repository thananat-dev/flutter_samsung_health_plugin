import 'flutter_samsung_health_plugin_platform_interface.dart';

class FlutterSamsungHealthPlugin {
  Future<String?> getPlatformVersion() {
    return FlutterSamsungHealthPluginPlatform.instance.getPlatformVersion();
  }

  Future<String?> connect() {
    return FlutterSamsungHealthPluginPlatform.instance.connect();
  }

  Future<String?> requestPermissions() {
    return FlutterSamsungHealthPluginPlatform.instance.requestPermissions();
  }

  Future<bool> checkPermissions() {
    return FlutterSamsungHealthPluginPlatform.instance.checkPermissions();
  }

  Future<String?> requestStepPermission() {
    return FlutterSamsungHealthPluginPlatform.instance.requestStepPermission();
  }

  Future<List<dynamic>?> readSteps() {
    return FlutterSamsungHealthPluginPlatform.instance.readSteps();
  }

  Future<int?> readHeartRate() {
    return FlutterSamsungHealthPluginPlatform.instance.readHeartRate();
  }

  Future<List<dynamic>?> readSleep() {
    return FlutterSamsungHealthPluginPlatform.instance.readSleep();
  }

  Future<int?> readWaterIntake() {
    return FlutterSamsungHealthPluginPlatform.instance.readWaterIntake();
  }

  Future<int?> readBloodOxygen() {
    return FlutterSamsungHealthPluginPlatform.instance.readBloodOxygen();
  }

  Future<double?> readBloodGlucose() {
    return FlutterSamsungHealthPluginPlatform.instance.readBloodGlucose();
  }

  Future<Map<String, dynamic>?> readBloodPressure() {
    return FlutterSamsungHealthPluginPlatform.instance.readBloodPressure();
  }

  Future<Map<String, dynamic>?> readBodyComposition() {
    return FlutterSamsungHealthPluginPlatform.instance.readBodyComposition();
  }

  Future<double?> readFloorsClimbed() {
    return FlutterSamsungHealthPluginPlatform.instance.readFloorsClimbed();
  }

  // Phase 4
  Future<double?> readSkinTemperature() {
    return FlutterSamsungHealthPluginPlatform.instance.readSkinTemperature();
  }

  Future<double?> readBodyTemperature() {
    return FlutterSamsungHealthPluginPlatform.instance.readBodyTemperature();
  }

  Future<List<dynamic>?> readNutrition() {
    return FlutterSamsungHealthPluginPlatform.instance.readNutrition();
  }

  Future<double?> readExercise() {
    return FlutterSamsungHealthPluginPlatform.instance.readExercise();
  }

  // Phase 3: History
  Future<List<dynamic>?> readStepsHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readStepsHistory();
  }

  Future<List<dynamic>?> readHeartRateHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readHeartRateHistory();
  }

  Future<List<dynamic>?> readSleepHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readSleepHistory();
  }

  Future<List<dynamic>?> readWaterIntakeHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readWaterIntakeHistory();
  }

  Future<List<dynamic>?> readBloodOxygenHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readBloodOxygenHistory();
  }

  Future<List<dynamic>?> readBloodGlucoseHistory() {
    return FlutterSamsungHealthPluginPlatform.instance
        .readBloodGlucoseHistory();
  }

  Future<List<dynamic>?> readBloodPressureHistory() {
    return FlutterSamsungHealthPluginPlatform.instance
        .readBloodPressureHistory();
  }

  Future<List<dynamic>?> readBodyCompositionHistory() {
    return FlutterSamsungHealthPluginPlatform.instance
        .readBodyCompositionHistory();
  }

  Future<List<dynamic>?> readFloorsClimbedHistory() {
    return FlutterSamsungHealthPluginPlatform.instance
        .readFloorsClimbedHistory();
  }

  Future<List<dynamic>?> readSkinTemperatureHistory() {
    return FlutterSamsungHealthPluginPlatform.instance
        .readSkinTemperatureHistory();
  }

  Future<List<dynamic>?> readBodyTemperatureHistory() {
    return FlutterSamsungHealthPluginPlatform.instance
        .readBodyTemperatureHistory();
  }

  Future<List<dynamic>?> readNutritionHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readNutritionHistory();
  }

  Future<List<dynamic>?> readExerciseHistory() {
    return FlutterSamsungHealthPluginPlatform.instance.readExerciseHistory();
  }

  Future<List<dynamic>?> getConnectedDevices() {
    return FlutterSamsungHealthPluginPlatform.instance.getConnectedDevices();
  }

  // Auto Sync Control
  Future<String?> startAutoSync(
      {String? apiUrl, String? macAddress, String? userToken}) {
    return FlutterSamsungHealthPluginPlatform.instance.startAutoSync(
        apiUrl: apiUrl, macAddress: macAddress, userToken: userToken);
  }

  Future<String?> stopAutoSync() {
    return FlutterSamsungHealthPluginPlatform.instance.stopAutoSync();
  }
}
