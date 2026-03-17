import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_samsung_health_plugin_method_channel.dart';

abstract class FlutterSamsungHealthPluginPlatform extends PlatformInterface {
  /// Constructs a FlutterSamsungHealthPluginPlatform.
  FlutterSamsungHealthPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterSamsungHealthPluginPlatform _instance =
      MethodChannelFlutterSamsungHealthPlugin();

  /// The default instance of [FlutterSamsungHealthPluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterSamsungHealthPlugin].
  static FlutterSamsungHealthPluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterSamsungHealthPluginPlatform] when
  /// they register themselves.
  static set instance(FlutterSamsungHealthPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> connect() {
    throw UnimplementedError('connect() has not been implemented.');
  }

  Future<String?> requestPermissions() {
    throw UnimplementedError('requestPermissions() has not been implemented.');
  }

  Future<bool> checkPermissions() {
    throw UnimplementedError('checkPermissions() has not been implemented.');
  }

  Future<String?> requestStepPermission() {
    throw UnimplementedError(
      'requestStepPermission() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readSteps() {
    throw UnimplementedError('readSteps() has not been implemented.');
  }

  Future<int?> readHeartRate() {
    throw UnimplementedError('readHeartRate() has not been implemented.');
  }

  Future<List<dynamic>?> readSleep() {
    throw UnimplementedError('readSleep() has not been implemented.');
  }

  Future<int?> readWaterIntake() {
    throw UnimplementedError('readWaterIntake() has not been implemented.');
  }

  Future<int?> readBloodOxygen() {
    throw UnimplementedError('readBloodOxygen() has not been implemented.');
  }

  Future<double?> readBloodGlucose() {
    throw UnimplementedError('readBloodGlucose() has not been implemented.');
  }

  Future<Map<String, dynamic>?> readBloodPressure() {
    throw UnimplementedError('readBloodPressure() has not been implemented.');
  }

  Future<Map<String, dynamic>?> readBodyComposition() {
    throw UnimplementedError('readBodyComposition() has not been implemented.');
  }

  Future<double?> readFloorsClimbed() {
    throw UnimplementedError('readFloorsClimbed() has not been implemented.');
  }

  // Phase 4: Usage
  Future<double?> readSkinTemperature() {
    throw UnimplementedError('readSkinTemperature() has not been implemented.');
  }

  Future<double?> readBodyTemperature() {
    throw UnimplementedError('readBodyTemperature() has not been implemented.');
  }

  Future<List<dynamic>?> readNutrition() {
    throw UnimplementedError('readNutrition() has not been implemented.');
  }

  Future<double?> readExercise() {
    throw UnimplementedError('readExercise() has not been implemented.');
  }

  // Phase 3: History
  // All history methods return List<dynamic> (List of Maps)
  Future<List<dynamic>?> readStepsHistory() {
    throw UnimplementedError('readStepsHistory() has not been implemented.');
  }

  Future<List<dynamic>?> readHeartRateHistory() {
    throw UnimplementedError(
      'readHeartRateHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readSleepHistory() {
    throw UnimplementedError('readSleepHistory() has not been implemented.');
  }

  Future<List<dynamic>?> readWaterIntakeHistory() {
    throw UnimplementedError(
      'readWaterIntakeHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readBloodOxygenHistory() {
    throw UnimplementedError(
      'readBloodOxygenHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readBloodGlucoseHistory() {
    throw UnimplementedError(
      'readBloodGlucoseHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readBloodPressureHistory() {
    throw UnimplementedError(
      'readBloodPressureHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readBodyCompositionHistory() {
    throw UnimplementedError(
      'readBodyCompositionHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readFloorsClimbedHistory() {
    throw UnimplementedError(
      'readFloorsClimbedHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readSkinTemperatureHistory() {
    throw UnimplementedError(
      'readSkinTemperatureHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readBodyTemperatureHistory() {
    throw UnimplementedError(
      'readBodyTemperatureHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readNutritionHistory() {
    throw UnimplementedError(
      'readNutritionHistory() has not been implemented.',
    );
  }

  Future<List<dynamic>?> readExerciseHistory() {
    throw UnimplementedError('readExerciseHistory() has not been implemented.');
  }

  // Device Management
  Future<List<dynamic>?> getConnectedDevices() {
    throw UnimplementedError('getConnectedDevices() has not been implemented.');
  }

  // Auto Sync Control
  Future<String?> startAutoSync(
      {String? apiUrl, String? macAddress, String? userToken}) {
    throw UnimplementedError('startAutoSync() has not been implemented.');
  }

  Future<String?> stopAutoSync() {
    throw UnimplementedError('stopAutoSync() has not been implemented.');
  }
}
