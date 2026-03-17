import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_samsung_health_plugin/flutter_samsung_health_plugin.dart';
import 'package:flutter_samsung_health_plugin/flutter_samsung_health_plugin_platform_interface.dart';
import 'package:flutter_samsung_health_plugin/flutter_samsung_health_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterSamsungHealthPluginPlatform
    with MockPlatformInterfaceMixin
    implements FlutterSamsungHealthPluginPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<String?> connect() => Future.value('CONNECTED');

  @override
  Future<String?> requestPermissions() => Future.value('PERMISSION_GRANTED');

  @override
  Future<String?> requestStepPermission() => Future.value('PERMISSION_GRANTED');

  @override
  Future<List<dynamic>?> readSteps() {
    return Future.value([
      {'count': 1000, 'calories': 45.0, 'start_time': '2023-01-01T10:00:00'},
      {'count': 500, 'calories': 22.5, 'start_time': '2023-01-01T11:00:00'},
    ]);
  }

  @override
  Future<int?> readHeartRate() {
    return Future.value(75);
  }

  @override
  Future<List<dynamic>?> readSleep() {
    return Future.value([
      {
        'start_time': '2023-01-01T23:00:00',
        'end_time': '2023-01-02T07:00:00',
        'duration_mins': 480,
      },
    ]);
  }

  @override
  Future<int?> readWaterIntake() {
    return Future.value(2000);
  }

  @override
  Future<int?> readBloodOxygen() {
    return Future.value(98);
  }

  @override
  Future<double?> readBloodGlucose() {
    return Future.value(90.0);
  }

  @override
  Future<Map<String, dynamic>?> readBloodPressure() {
    return Future.value({'systolic': 120.0, 'diastolic': 80.0});
  }

  @override
  Future<Map<String, dynamic>?> readBodyComposition() {
    return Future.value({'body_fat': 20.0, 'skeletal_muscle': 30.0});
  }

  @override
  Future<double?> readFloorsClimbed() {
    return Future.value(10.0);
  }

  @override
  Future<double?> readSkinTemperature() {
    return Future.value(36.5);
  }

  @override
  Future<double?> readBodyTemperature() {
    return Future.value(37.0);
  }

  @override
  Future<List<dynamic>?> readNutrition() {
    return Future.value([
      {
        'calories': 500.0,
        'vitamin_a': 10.0,
        'vitamin_c': 20.0,
        'calcium': 30.0,
        'iron': 5.0,
      },
    ]);
  }

  @override
  Future<double?> readExercise() => Future.value(0.0);

  @override
  Future<List<dynamic>?> readStepsHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readHeartRateHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readSleepHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readWaterIntakeHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readBloodOxygenHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readBloodGlucoseHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readBloodPressureHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readBodyCompositionHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readFloorsClimbedHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readSkinTemperatureHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readBodyTemperatureHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readNutritionHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> readExerciseHistory() => Future.value([]);

  @override
  Future<List<dynamic>?> getConnectedDevices() => Future.value([
        {
          'uuid': 'test-uuid',
          'customName': 'Test Device',
          'manufacturer': 'Samsung',
          'model': 'Galaxy Watch',
        },
      ]);

  @override
  Future<bool> checkPermissions() => Future.value(true);

  @override
  Future<String?> startAutoSync(
          {String? apiUrl, String? macAddress, String? userToken}) =>
      Future.value('AUTO_SYNC_STARTED');

  @override
  Future<String?> stopAutoSync() => Future.value('AUTO_SYNC_STOPPED');
}

void main() {
  final FlutterSamsungHealthPluginPlatform initialPlatform =
      FlutterSamsungHealthPluginPlatform.instance;

  test('$MethodChannelFlutterSamsungHealthPlugin is the default instance', () {
    expect(
      initialPlatform,
      isInstanceOf<MethodChannelFlutterSamsungHealthPlugin>(),
    );
  });

  test('getPlatformVersion', () async {
    FlutterSamsungHealthPlugin flutterSamsungHealthPlugin =
        FlutterSamsungHealthPlugin();
    MockFlutterSamsungHealthPluginPlatform fakePlatform =
        MockFlutterSamsungHealthPluginPlatform();
    FlutterSamsungHealthPluginPlatform.instance = fakePlatform;

    expect(await flutterSamsungHealthPlugin.getPlatformVersion(), '42');
  });
}
