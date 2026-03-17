import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_samsung_health_plugin/flutter_samsung_health_plugin.dart';
import 'dart:math';
import 'dart:convert';
import 'dart:io';
import 'package:permission_handler/permission_handler.dart';
import 'package:http/http.dart' as http;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        fontFamily: 'Roboto',
        primaryColor: const Color(0xFF2E8B57),
        scaffoldBackgroundColor: const Color(0xFFF2F4F8),
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          elevation: 0,
          titleTextStyle: TextStyle(
            color: Colors.black,
            fontWeight: FontWeight.bold,
            fontSize: 20,
          ),
          iconTheme: IconThemeData(color: Colors.black),
        ),
      ),
      home: const DashboardScreen(),
    );
  }
}

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen>
    with WidgetsBindingObserver {
  final _plugin = FlutterSamsungHealthPlugin();
  bool _isConnected = false;
  String _status = 'Connecting...';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _initConnection();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _isConnected) {
      _exportJson();
    }
  }

  Future<void> _initConnection() async {
    setState(() => _status = 'Connecting...');
    try {
      final res = await _plugin.connect();
      if (res == 'CONNECTED') {
        // Check if permissions are already granted
        final alreadyGranted = await _plugin.checkPermissions();
        if (alreadyGranted) {
          setState(() {
            _isConnected = true;
            _status = 'Connected';
          });
          _exportJson(); // Auto-sync on launch
        } else {
          // Only request if not already granted
          final permRes = await _plugin.requestPermissions();
          if (permRes == 'PERMISSION_GRANTED' ||
              permRes == 'PERMISSION_PARTIALLY_GRANTED') {
            setState(() {
              _isConnected = true;
              _status = 'Connected';
            });
            _exportJson(); // Auto-sync after permission grant
          } else {
            setState(() => _status = 'Permission Denied: $permRes');
          }
        }
      } else {
        setState(() => _status = 'Connection Failed');
      }
    } catch (e) {
      setState(() => _status = 'Error: $e');
    }
  }

  Future<void> _exportJson() async {
    setState(() => _status = 'Syncing Data...');
    try {
      // Fetch all data
      final devices = await _plugin.getConnectedDevices();

      final steps = await _plugin.readSteps();
      final stepsHistory = await _plugin.readStepsHistory();

      final hr = await _plugin.readHeartRate();
      final hrHistory = await _plugin.readHeartRateHistory();

      final sleep = await _plugin.readSleep();
      final sleepHistory = await _plugin.readSleepHistory();

      final spo2 = await _plugin.readBloodOxygen();
      final spo2History = await _plugin.readBloodOxygenHistory();

      final glucose = await _plugin.readBloodGlucose();
      final glucoseHistory = await _plugin.readBloodGlucoseHistory();

      final bp = await _plugin.readBloodPressure();
      final bpHistory = await _plugin.readBloodPressureHistory();

      final water = await _plugin.readWaterIntake();
      final waterHistory = await _plugin.readWaterIntakeHistory();

      final skintemp = await _plugin.readSkinTemperature();
      final skintempHistory = await _plugin.readSkinTemperatureHistory();

      // Construct separate models
      final deviceModel = {
        "api_route": "/api/devices",
        "timestamp": DateTime.now().toIso8601String(),
        "devices": devices ?? [],
      };

      final stepsModel = {
        "api_route": "/api/steps",
        "timestamp": DateTime.now().toIso8601String(),
        "current": steps ?? [],
        "history": stepsHistory ?? [],
      };

      final heartRateModel = {
        "api_route": "/api/heart_rate",
        "timestamp": DateTime.now().toIso8601String(),
        "latest_bpm": hr,
        "history": hrHistory ?? [],
      };

      final sleepModel = {
        "api_route": "/api/sleep",
        "timestamp": DateTime.now().toIso8601String(),
        "today": sleep ?? [],
        "history": sleepHistory ?? [],
      };

      final spo2Model = {
        "api_route": "/api/spo2",
        "timestamp": DateTime.now().toIso8601String(),
        "latest": spo2,
        "history": spo2History ?? [],
      };

      final glucoseModel = {
        "api_route": "/api/glucose",
        "timestamp": DateTime.now().toIso8601String(),
        "latest": glucose,
        "history": glucoseHistory ?? [],
      };

      final bpModel = {
        "api_route": "/api/blood_pressure",
        "timestamp": DateTime.now().toIso8601String(),
        "latest": bp,
        "history": bpHistory ?? [],
      };

      final skinTempModel = {
        "api_route": "/api/skin_temperature",
        "timestamp": DateTime.now().toIso8601String(),
        "latest": skintemp,
        "history": skintempHistory ?? [],
      };

      final waterIntakeModel = {
        "api_route": "/api/water_intake",
        "timestamp": DateTime.now().toIso8601String(),
        "latest": water,
        "history": waterHistory ?? [],
      };

      // Print each model separately (for debugging)
      print("--- START EXPORT ---");
      print("DEVICE JSON: ${jsonEncode(deviceModel)}");
      print("--- END EXPORT ---");

      // Fetch additional data for API mapping
      final bodyComp = await _plugin.readBodyComposition();

      // Get MAC address from first watch device
      String? watchMacAddress;
      if (devices != null && devices.isNotEmpty) {
        for (var device in devices) {
          if (device['address'] != null) {
            watchMacAddress = device['address'] as String;
            break;
          }
        }
      }

      // Calculate step count
      int stepValue = 0;
      if (steps != null) {
        for (var item in steps) {
          stepValue += (item['count'] as num?)?.toInt() ?? 0;
        }
      }

      // Extract BP values
      double sbpValue = 0;
      double dbpValue = 0;
      if (bp != null) {
        sbpValue = (bp['systolic'] as num?)?.toDouble() ?? 0;
        dbpValue = (bp['diastolic'] as num?)?.toDouble() ?? 0;
      }

      // Extract body fat
      double bodyFatValue = 0;
      if (bodyComp != null) {
        bodyFatValue = (bodyComp['body_fat'] as num?)?.toDouble() ?? 0;
      }

      // Extract temperature (split int and decimal)
      double tempValue = (skintemp as num?)?.toDouble() ?? 0;
      int tempIntValue = tempValue.floor();
      int tempFloatValue = ((tempValue - tempIntValue) * 10).round();

      // Create API payload
      final apiPayload = {
        "watchMacAddress": watchMacAddress ?? "UNKNOWN",
        "watchHealthData": [
          {
            "heartValue": (hr as num?)?.toInt() ?? 0,
            "hrvValue": 0, // Not available from SDK
            "cvrrValue": 0, // Not available from SDK
            "stepValue": stepValue,
            "DBPValue": dbpValue.toInt(),
            "bodyFatFloatValue": bodyFatValue,
            "OOValue": (spo2 as num?)?.toInt() ?? 0,
            "bodyFatIntValue": bodyFatValue.toInt(),
            "tempIntValue": tempIntValue,
            "tempFloatValue": tempFloatValue,
            "startTime": DateTime.now().millisecondsSinceEpoch ~/ 1000,
            "SBPValue": sbpValue.toInt(),
            "respiratoryRateValue": 0, // Not available from SDK
          },
        ],
      };

      print("API PAYLOAD: ${jsonEncode(apiPayload)}");

      // Send to API
      const apiUrl = 'https://your-domain.com/api/saveReleepHealthData';
      try {
        final response = await http.post(
          Uri.parse(apiUrl),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode(apiPayload),
        );
        print("API Response: ${response.statusCode} - ${response.body}");
        if (response.statusCode == 200 || response.statusCode == 201) {
          setState(() => _status = 'Synced to API ✓');
        } else {
          setState(() => _status = 'API Error: ${response.statusCode}');
        }
      } catch (apiError) {
        print("API POST Error: $apiError");
        setState(() => _status = 'API Failed: $apiError');
      }
    } catch (e) {
      setState(() => _status = 'Sync Failed: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Samsung Health Dashboard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.upload_file),
            onPressed: _isConnected ? _exportJson : null,
            tooltip: 'Export JSON',
          ),
        ],
      ),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            color: _isConnected
                ? Colors.green.withOpacity(0.1)
                : Colors.red.withOpacity(0.1),
            width: double.infinity,
            child: Text(
              _status,
              textAlign: TextAlign.center,
              style: TextStyle(
                color: _isConnected ? Colors.green[800] : Colors.red[800],
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          Expanded(
            child: GridView.count(
              crossAxisCount: 2,
              padding: const EdgeInsets.all(16),
              mainAxisSpacing: 16,
              crossAxisSpacing: 16,
              children: [
                _buildMenuCard(
                  context,
                  'Steps',
                  Icons.directions_walk,
                  Colors.orange,
                  const StepsScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Heart Rate',
                  Icons.favorite,
                  Colors.red,
                  const HeartRateScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Sleep',
                  Icons.bedtime,
                  Colors.indigo,
                  const SleepScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Water',
                  Icons.local_drink,
                  Colors.blue,
                  const WaterScreen(),
                ),
                _buildMenuCard(
                  context,
                  'SpO2',
                  Icons.bloodtype,
                  Colors.purple,
                  const BloodOxygenScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Glucose',
                  Icons.monitor_weight, // Approximate icon
                  Colors.teal,
                  const BloodGlucoseScreen(),
                ),
                _buildMenuCard(
                  context,
                  'BP',
                  Icons.favorite_border,
                  Colors.pink,
                  const BloodPressureScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Body Comp',
                  Icons.accessibility_new,
                  Colors.brown,
                  const BodyCompositionScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Floors',
                  Icons.stairs,
                  Colors.orangeAccent,
                  const FloorsScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Skin Temp',
                  Icons.thermostat,
                  Colors.deepOrange,
                  const SkinTemperatureScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Body Temp',
                  Icons.thermostat,
                  Colors.redAccent,
                  const BodyTemperatureScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Nutrition',
                  Icons.restaurant,
                  Colors.green,
                  const NutritionScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Exercise',
                  Icons.fitness_center,
                  Colors.blueGrey,
                  const ExerciseScreen(),
                ),
                _buildMenuCard(
                  context,
                  'Devices',
                  Icons.watch,
                  Colors.black,
                  const ConnectedDevicesScreen(),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ... existing _buildMenuCard ...

  // ... existing Screen classes ...

  Widget _buildMenuCard(
    BuildContext context,
    String title,
    IconData icon,
    Color color,
    Widget page,
  ) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: () {
          Navigator.push(context, MaterialPageRoute(builder: (_) => page));
        },
        borderRadius: BorderRadius.circular(16),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircleAvatar(
              radius: 30,
              backgroundColor: color.withOpacity(0.1),
              child: Icon(icon, size: 32, color: color),
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
          ],
        ),
      ),
    );
  }
}

class SkinTemperatureScreen extends StatefulWidget {
  const SkinTemperatureScreen({super.key});

  @override
  State<SkinTemperatureScreen> createState() => _SkinTemperatureScreenState();
}

class _SkinTemperatureScreenState extends State<SkinTemperatureScreen> {
  double _temp = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readSkinTemperature();
      final history =
          await FlutterSamsungHealthPlugin().readSkinTemperatureHistory();
      if (mounted) {
        setState(() {
          _temp = val ?? 0;
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Skin Temperature')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.thermostat, size: 80, color: Colors.deepOrange),
          const SizedBox(height: 10),
          Text(
            '$_temp °C',
            style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
          ),
          const Text('LATEST', style: TextStyle(color: Colors.grey)),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  trailing: Text(
                    '${item['value']} °C',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class BodyTemperatureScreen extends StatefulWidget {
  const BodyTemperatureScreen({super.key});

  @override
  State<BodyTemperatureScreen> createState() => _BodyTemperatureScreenState();
}

class _BodyTemperatureScreenState extends State<BodyTemperatureScreen> {
  double _temp = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readBodyTemperature();
      final history =
          await FlutterSamsungHealthPlugin().readBodyTemperatureHistory();
      if (mounted) {
        setState(() {
          _temp = val ?? 0;
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Body Temperature')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.thermostat, size: 80, color: Colors.redAccent),
          const SizedBox(height: 10),
          Text(
            '$_temp °C',
            style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
          ),
          const Text('LATEST', style: TextStyle(color: Colors.grey)),
          const Divider(height: 40),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  trailing: Text(
                    '${item['value']} °C',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class NutritionScreen extends StatefulWidget {
  const NutritionScreen({super.key});

  @override
  State<NutritionScreen> createState() => _NutritionScreenState();
}

class _NutritionScreenState extends State<NutritionScreen> {
  double _calories = 0;
  double _vitA = 0;
  double _vitC = 0;
  double _calcium = 0;
  double _iron = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final list = await FlutterSamsungHealthPlugin().readNutrition();
      final history = await FlutterSamsungHealthPlugin().readNutritionHistory();

      double c = 0, va = 0, vc = 0, ca = 0, fe = 0;
      if (list != null) {
        for (var item in list) {
          c += (item['calories'] as num?)?.toDouble() ?? 0.0;
          va += (item['vitamin_a'] as num?)?.toDouble() ?? 0.0;
          vc += (item['vitamin_c'] as num?)?.toDouble() ?? 0.0;
          ca += (item['calcium'] as num?)?.toDouble() ?? 0.0;
          fe += (item['iron'] as num?)?.toDouble() ?? 0.0;
        }
      }

      if (mounted) {
        setState(() {
          _calories = c;
          _vitA = va;
          _vitC = vc;
          _calcium = ca;
          _iron = fe;
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nutrition')),
      body: SingleChildScrollView(
        child: Column(
          children: [
            const SizedBox(height: 20),
            const Icon(Icons.restaurant, size: 80, color: Colors.green),
            const SizedBox(height: 10),
            Text(
              '${_calories.toInt()}',
              style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
            ),
            const Text('CALORIES TODAY', style: TextStyle(color: Colors.grey)),
            const SizedBox(height: 20),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      const Text(
                        "Vitamins & Minerals",
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const Divider(),
                      _row('Vitamin A', '${_vitA.toStringAsFixed(1)} µg'),
                      _row('Vitamin C', '${_vitC.toStringAsFixed(1)} mg'),
                      _row('Calcium', '${_calcium.toStringAsFixed(1)} mg'),
                      _row('Iron', '${_iron.toStringAsFixed(1)} mg'),
                    ],
                  ),
                ),
              ),
            ),
            const Divider(height: 40),
            const Text(
              'History (Last 7 Days)',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            SizedBox(
              height: 300,
              child: ListView.builder(
                itemCount: _history.length,
                itemBuilder: (context, index) {
                  final item = _history[index];
                  return ListTile(
                    title: Text(
                      item['date']?.toString().substring(0, 10) ?? '',
                    ),
                    trailing: Text(
                      '${(item['value'] as num).toInt()} kcal',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _row(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }
}

class ExerciseScreen extends StatefulWidget {
  const ExerciseScreen({super.key});

  @override
  State<ExerciseScreen> createState() => _ExerciseScreenState();
}

class _ExerciseScreenState extends State<ExerciseScreen> {
  double _calories = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readExercise();
      final history = await FlutterSamsungHealthPlugin().readExerciseHistory();
      if (mounted) {
        setState(() {
          _calories = val ?? 0;
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Exercise')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.fitness_center, size: 80, color: Colors.blueGrey),
          const SizedBox(height: 10),
          Text(
            '${_calories.toInt()}',
            style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
          ),
          const Text('CALORIES TODAY', style: TextStyle(color: Colors.grey)),
          const Divider(height: 40),
          const Padding(
            padding: EdgeInsets.all(8.0),
            child: Text(
              'Recent Sessions',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['title'] ?? 'Exercise'),
                  subtitle: Text(item['date']?.toString() ?? ''),
                  trailing: Text('${item['duration_mins']} mins'),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class StepsScreen extends StatefulWidget {
  const StepsScreen({super.key});

  @override
  State<StepsScreen> createState() => _StepsScreenState();
}

class _StepsScreenState extends State<StepsScreen> {
  int _steps = 0;
  double _calories = 0;
  final int _goal = 6000;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final list = await FlutterSamsungHealthPlugin().readSteps();
      final history = await FlutterSamsungHealthPlugin().readStepsHistory();

      int s = 0;
      double c = 0;
      if (list != null) {
        for (var item in list) {
          s += (item['count'] as num?)?.toInt() ?? 0;
          c += (item['calories'] as num?)?.toDouble() ?? 0.0;
        }
      }

      if (mounted) {
        setState(() {
          _steps = s;
          _calories = c;
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Steps')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          Center(
            child: Stack(
              alignment: Alignment.center,
              children: [
                CustomPaint(
                  size: const Size(200, 200),
                  painter: CircularStepPainter(
                    progress: _steps / _goal,
                    color: Colors.orange,
                    backgroundColor: Colors.grey.shade300,
                  ),
                ),
                Column(
                  children: [
                    const Icon(
                      Icons.directions_walk,
                      size: 40,
                      color: Colors.orange,
                    ),
                    Text(
                      '$_steps',
                      style: const TextStyle(
                        fontSize: 40,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Text(
                      'STEPS TODAY',
                      style: TextStyle(color: Colors.grey),
                    ),
                    Text(
                      '${_calories.toInt()} kcal',
                      style: const TextStyle(
                        color: Colors.deepOrange,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const Divider(),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                // History items now maintain 'count' and 'calories' keys
                final count = item['count'] ?? item['value'] ?? 0;
                final cals = item['calories'] ?? 0;

                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  trailing: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        '$count steps',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '${(cals as num).toInt()} kcal',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class HeartRateScreen extends StatefulWidget {
  const HeartRateScreen({super.key});

  @override
  State<HeartRateScreen> createState() => _HeartRateScreenState();
}

class _HeartRateScreenState extends State<HeartRateScreen> {
  int _bpm = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readHeartRate();
      final history = await FlutterSamsungHealthPlugin().readHeartRateHistory();
      if (mounted)
        setState(() {
          _bpm = val ?? 0;
          _history = history ?? [];
        });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Heart Rate')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.favorite, size: 100, color: Colors.red),
          const SizedBox(height: 10),
          Text(
            '$_bpm',
            style: const TextStyle(fontSize: 60, fontWeight: FontWeight.bold),
          ),
          const Text(
            'BPM (Last Reading)',
            style: TextStyle(color: Colors.grey),
          ),
          const Divider(height: 40),
          const Text(
            'Recent Readings',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString() ?? ''),
                  trailing: Text(
                    '${item['value']} bpm',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class SleepScreen extends StatefulWidget {
  const SleepScreen({super.key});

  @override
  State<SleepScreen> createState() => _SleepScreenState();
}

class _SleepScreenState extends State<SleepScreen> {
  String _duration = '0h 0m';
  String _bedtime = '--:--';
  String _wakeup = '--:--';
  int _awakeMins = 0;
  int _lightMins = 0;
  int _deepMins = 0;
  int _remMins = 0;
  int _sleepScore = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final list = await FlutterSamsungHealthPlugin().readSleep();
      final history = await FlutterSamsungHealthPlugin().readSleepHistory();

      int dur = 0;
      int awake = 0, light = 0, deep = 0, rem = 0, score = 0;
      DateTime? minS;
      DateTime? maxE;

      if (list != null) {
        for (var item in list) {
          dur += (item['duration_mins'] as num?)?.toInt() ?? 0;
          awake += (item['awake_mins'] as num?)?.toInt() ?? 0;
          light += (item['light_mins'] as num?)?.toInt() ?? 0;
          deep += (item['deep_mins'] as num?)?.toInt() ?? 0;
          rem += (item['rem_mins'] as num?)?.toInt() ?? 0;
          score = (item['sleep_score'] as num?)?.toInt() ?? score;
          final s = item['start_time'] as String?;
          final e = item['end_time'] as String?;
          if (s != null) {
            final dt = DateTime.parse(
              s,
            ).add(const Duration(hours: 7)); // Convert to Thai time (UTC+7)
            if (minS == null || dt.isBefore(minS)) minS = dt;
          }
          if (e != null) {
            final dt = DateTime.parse(
              e,
            ).add(const Duration(hours: 7)); // Convert to Thai time (UTC+7)
            if (maxE == null || dt.isAfter(maxE)) maxE = dt;
          }
        }
      }

      final hours = dur ~/ 60;
      final mins = dur % 60;

      String fmt(DateTime? d) {
        if (d == null) return '--:--';
        return "${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}";
      }

      if (mounted)
        setState(() {
          _duration = '${hours}h ${mins}m';
          _bedtime = fmt(minS);
          _wakeup = fmt(maxE);
          _awakeMins = awake;
          _lightMins = light;
          _deepMins = deep;
          _remMins = rem;
          _sleepScore = score;
          _history = history ?? [];
        });
    } catch (_) {}
  }

  String _formatMins(int mins) {
    final h = mins ~/ 60;
    final m = mins % 60;
    if (h > 0) return '${h}h ${m}m';
    return '${m}m';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sleep')),
      body: SingleChildScrollView(
        child: Column(
          children: [
            const SizedBox(height: 20),
            // Sleep Score Circle
            if (_sleepScore > 0)
              Stack(
                alignment: Alignment.center,
                children: [
                  SizedBox(
                    width: 120,
                    height: 120,
                    child: CircularProgressIndicator(
                      value: _sleepScore / 100,
                      strokeWidth: 10,
                      backgroundColor: Colors.grey[300],
                      valueColor: AlwaysStoppedAnimation<Color>(
                        _sleepScore >= 80
                            ? Colors.green
                            : _sleepScore >= 60
                                ? Colors.orange
                                : Colors.red,
                      ),
                    ),
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '$_sleepScore',
                        style: const TextStyle(
                          fontSize: 36,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const Text('Score', style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                ],
              )
            else
              const Icon(Icons.bedtime, size: 80, color: Colors.indigo),
            const SizedBox(height: 10),
            Text(
              _duration,
              style: const TextStyle(fontSize: 48, fontWeight: FontWeight.bold),
            ),
            const Text('TOTAL SLEEP', style: TextStyle(color: Colors.grey)),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Column(
                  children: [
                    const Text(
                      "Bedtime",
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(_bedtime),
                  ],
                ),
                const SizedBox(width: 40),
                Column(
                  children: [
                    const Text(
                      "Wake Up",
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(_wakeup),
                  ],
                ),
              ],
            ),
            const Divider(height: 30),
            // Sleep Stages
            const Text(
              'Sleep Stages',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 10),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _buildStageCard('AWAKE', _awakeMins, Colors.orange),
                  _buildStageCard('LIGHT', _lightMins, Colors.lightBlue),
                  _buildStageCard('DEEP', _deepMins, Colors.indigo),
                  _buildStageCard('REM', _remMins, Colors.purple),
                ],
              ),
            ),
            const Divider(height: 30),
            const Text(
              'History (Last 7 Days)',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                final val = (item['value'] as num?)?.toInt() ?? 0;
                final h = val ~/ 60;
                final m = val % 60;
                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  trailing: Text(
                    '${h}h ${m}m',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStageCard(String label, int mins, Color color) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        children: [
          Icon(
            label == 'AWAKE'
                ? Icons.visibility
                : label == 'LIGHT'
                    ? Icons.light_mode
                    : label == 'DEEP'
                        ? Icons.nights_stay
                        : Icons.auto_awesome,
            color: color,
            size: 24,
          ),
          const SizedBox(height: 4),
          Text(
            _formatMins(mins),
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: color,
              fontSize: 16,
            ),
          ),
          Text(label, style: TextStyle(fontSize: 10, color: color)),
        ],
      ),
    );
  }
}

class WaterScreen extends StatefulWidget {
  const WaterScreen({super.key});

  @override
  State<WaterScreen> createState() => _WaterScreenState();
}

class _WaterScreenState extends State<WaterScreen> {
  int _water = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readWaterIntake();
      final history =
          await FlutterSamsungHealthPlugin().readWaterIntakeHistory();
      if (mounted)
        setState(() {
          _water = val ?? 0;
          _history = history ?? [];
        });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Water Intake')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.local_drink, size: 100, color: Colors.blue),
          const SizedBox(height: 10),
          Text(
            '$_water',
            style: const TextStyle(fontSize: 60, fontWeight: FontWeight.bold),
          ),
          const Text('ml TODAY', style: TextStyle(color: Colors.grey)),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  trailing: Text(
                    '${item['value']} ml',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class CircularStepPainter extends CustomPainter {
  final double progress;
  final Color color;
  final Color backgroundColor;

  CircularStepPainter({
    required this.progress,
    required this.color,
    required this.backgroundColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = min(size.width / 2, size.height / 2);
    const strokeWidth = 20.0;

    final backgroundPaint = Paint()
      ..color = backgroundColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    canvas.drawCircle(center, radius - strokeWidth / 2, backgroundPaint);

    final progressPaint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    final startAngle = -pi / 2;
    final sweepAngle = 2 * pi * (progress > 1.0 ? 1.0 : progress);

    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius - strokeWidth / 2),
      startAngle,
      sweepAngle,
      false,
      progressPaint,
    );
  }

  @override
  bool shouldRepaint(covariant CircularStepPainter oldDelegate) {
    return oldDelegate.progress != progress || oldDelegate.color != color;
  }
}

class BloodOxygenScreen extends StatefulWidget {
  const BloodOxygenScreen({super.key});

  @override
  State<BloodOxygenScreen> createState() => _BloodOxygenScreenState();
}

class _BloodOxygenScreenState extends State<BloodOxygenScreen> {
  int _spo2 = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readBloodOxygen();
      final history =
          await FlutterSamsungHealthPlugin().readBloodOxygenHistory();
      if (mounted)
        setState(() {
          _spo2 = val ?? 0;
          _history = history ?? [];
        });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Blood Oxygen (SpO2)')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.bloodtype, size: 100, color: Colors.purple),
          const SizedBox(height: 20),
          Text(
            '$_spo2%',
            style: const TextStyle(fontSize: 60, fontWeight: FontWeight.bold),
          ),
          const Text(
            'LATEST READING',
            style: TextStyle(fontSize: 16, color: Colors.grey),
          ),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString() ?? ''),
                  trailing: Text(
                    '${item['value']}%',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class BloodGlucoseScreen extends StatefulWidget {
  const BloodGlucoseScreen({super.key});

  @override
  State<BloodGlucoseScreen> createState() => _BloodGlucoseScreenState();
}

class _BloodGlucoseScreenState extends State<BloodGlucoseScreen> {
  double _glucose = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readBloodGlucose();
      final history =
          await FlutterSamsungHealthPlugin().readBloodGlucoseHistory();
      if (mounted)
        setState(() {
          _glucose = val ?? 0;
          _history = history ?? [];
        });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Blood Glucose')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.monitor_weight, size: 100, color: Colors.teal),
          const SizedBox(height: 20),
          Text(
            '$_glucose',
            style: const TextStyle(fontSize: 60, fontWeight: FontWeight.bold),
          ),
          const Text(
            'mmol/L',
            style: TextStyle(fontSize: 20, color: Colors.grey),
          ),
          const Text(
            'LATEST READING',
            style: TextStyle(fontSize: 14, color: Colors.grey),
          ),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString() ?? ''),
                  trailing: Text(
                    '${item['value']} mmol/L',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class BloodPressureScreen extends StatefulWidget {
  const BloodPressureScreen({super.key});

  @override
  State<BloodPressureScreen> createState() => _BloodPressureScreenState();
}

class _BloodPressureScreenState extends State<BloodPressureScreen> {
  double _systolic = 0;
  double _diastolic = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readBloodPressure();
      final history =
          await FlutterSamsungHealthPlugin().readBloodPressureHistory();
      if (mounted) {
        setState(() {
          if (val != null) {
            _systolic = (val['systolic'] as num).toDouble();
            _diastolic = (val['diastolic'] as num).toDouble();
          }
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Blood Pressure')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.favorite_border, size: 100, color: Colors.pink),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '${_systolic.toInt()}',
                style: const TextStyle(
                  fontSize: 60,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const Text(
                '/',
                style: TextStyle(fontSize: 40, color: Colors.grey),
              ),
              Text(
                '${_diastolic.toInt()}',
                style: const TextStyle(
                  fontSize: 60,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const Text(
            'mmHg',
            style: TextStyle(fontSize: 20, color: Colors.grey),
          ),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                final s = item['systolic'] ?? 0;
                final d = item['diastolic'] ?? 0;
                return ListTile(
                  title: Text(item['date']?.toString() ?? ''),
                  trailing: Text(
                    '${s}/${d} mmHg',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class BodyCompositionScreen extends StatefulWidget {
  const BodyCompositionScreen({super.key});

  @override
  State<BodyCompositionScreen> createState() => _BodyCompositionScreenState();
}

class _BodyCompositionScreenState extends State<BodyCompositionScreen> {
  double _weight = 0;
  double _bodyFat = 0;
  double _muscle = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readBodyComposition();
      final history =
          await FlutterSamsungHealthPlugin().readBodyCompositionHistory();
      if (mounted) {
        setState(() {
          if (val != null) {
            _weight = (val['weight'] as num).toDouble();
            _bodyFat = (val['body_fat'] as num).toDouble();
            _muscle = (val['muscle_mass'] as num).toDouble();
          }
          _history = history ?? [];
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Body Composition')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.accessibility_new, size: 80, color: Colors.brown),
          const SizedBox(height: 20),
          _buildRow('Weight', '$_weight', 'kg'),
          const Divider(),
          _buildRow('Body Fat', '$_bodyFat', '%'),
          const Divider(),
          _buildRow('Muscle', '$_muscle', 'kg'),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  subtitle: Text(
                    'Fat: ${item['body_fat']}% | Muscle: ${item['muscle_mass']}kg',
                  ),
                  trailing: Text(
                    '${item['weight']}kg',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRow(String label, String value, String unit) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w500),
          ),
          Row(
            children: [
              Text(
                value,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(width: 4),
              Text(
                unit,
                style: const TextStyle(fontSize: 16, color: Colors.grey),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class FloorsScreen extends StatefulWidget {
  const FloorsScreen({super.key});

  @override
  State<FloorsScreen> createState() => _FloorsScreenState();
}

class _FloorsScreenState extends State<FloorsScreen> {
  double _floors = 0;
  List<dynamic> _history = [];

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      final val = await FlutterSamsungHealthPlugin().readFloorsClimbed();
      final history =
          await FlutterSamsungHealthPlugin().readFloorsClimbedHistory();
      if (mounted)
        setState(() {
          _floors = val ?? 0;
          _history = history ?? [];
        });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Floors Climbed')),
      body: Column(
        children: [
          const SizedBox(height: 20),
          const Icon(Icons.stairs, size: 100, color: Colors.orangeAccent),
          const SizedBox(height: 20),
          Text(
            '${_floors.toInt()}',
            style: const TextStyle(fontSize: 60, fontWeight: FontWeight.bold),
          ),
          const Text(
            'FLOORS',
            style: TextStyle(fontSize: 20, color: Colors.grey),
          ),
          const Divider(height: 40),
          const Text(
            'History (Last 7 Days)',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _history.length,
              itemBuilder: (context, index) {
                final item = _history[index];
                return ListTile(
                  title: Text(item['date']?.toString().substring(0, 10) ?? ''),
                  trailing: Text(
                    '${(item['value'] as num).toInt()} floors',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class ConnectedDevicesScreen extends StatefulWidget {
  const ConnectedDevicesScreen({super.key});

  @override
  State<ConnectedDevicesScreen> createState() => _ConnectedDevicesScreenState();
}

class _ConnectedDevicesScreenState extends State<ConnectedDevicesScreen> {
  Map<String, List<dynamic>> _groupedDevices = {};
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    try {
      if (Platform.isAndroid) {
        await Permission.bluetoothConnect.request();
      }
      final devices = await FlutterSamsungHealthPlugin().getConnectedDevices();
      if (mounted) {
        setState(() {
          _groupDevices(devices ?? []);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _groupDevices(List<dynamic> devices) {
    _groupedDevices = {};
    for (var device in devices) {
      final group = device['group']?.toString() ?? 'Other';
      if (!_groupedDevices.containsKey(group)) {
        _groupedDevices[group] = [];
      }
      _groupedDevices[group]!.add(device);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Connected Devices')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _groupedDevices.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.watch_off, size: 64, color: Colors.grey[400]),
                      const SizedBox(height: 16),
                      Text(
                        "No devices found",
                        style: TextStyle(fontSize: 18, color: Colors.grey[600]),
                      ),
                    ],
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: _groupedDevices.length,
                  itemBuilder: (context, index) {
                    final groupName = _groupedDevices.keys.elementAt(index);
                    final devices = _groupedDevices[groupName]!;

                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.symmetric(
                            vertical: 8.0,
                            horizontal: 4.0,
                          ),
                          child: Text(
                            groupName,
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                              color: Colors.blueAccent,
                            ),
                          ),
                        ),
                        ...devices
                            .map(
                              (device) => Card(
                                elevation: 2,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                margin: const EdgeInsets.only(bottom: 12),
                                child: Padding(
                                  padding: const EdgeInsets.all(16.0),
                                  child: Row(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Container(
                                        padding: const EdgeInsets.all(12),
                                        decoration: BoxDecoration(
                                          color: Colors.blue.withOpacity(0.1),
                                          borderRadius:
                                              BorderRadius.circular(12),
                                        ),
                                        child: Icon(
                                          _getIconForGroup(groupName),
                                          color: Colors.blue,
                                          size: 32,
                                        ),
                                      ),
                                      const SizedBox(width: 16),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              device['customName'] ??
                                                  'Unknown Device',
                                              style: const TextStyle(
                                                fontSize: 18,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                            const SizedBox(height: 4),
                                            Text(
                                              "${device['manufacturer']} • ${device['model']}",
                                              style: TextStyle(
                                                color: Colors.grey[700],
                                                fontSize: 14,
                                              ),
                                            ),
                                            const SizedBox(height: 8),
                                            Container(
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                horizontal: 8,
                                                vertical: 4,
                                              ),
                                              decoration: BoxDecoration(
                                                color: Colors.grey[200],
                                                borderRadius:
                                                    BorderRadius.circular(
                                                  4,
                                                ),
                                              ),
                                              child: Text(
                                                "UUID: ${device['uuid']}",
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.grey[600],
                                                  fontFamily: 'Monospace',
                                                ),
                                              ),
                                            ),
                                            const SizedBox(height: 8),
                                            Container(
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                horizontal: 8,
                                                vertical: 4,
                                              ),
                                              decoration: BoxDecoration(
                                                color: Colors.grey[200],
                                                borderRadius:
                                                    BorderRadius.circular(
                                                  4,
                                                ),
                                              ),
                                              child: Text(
                                                "Device group: ${device['group']}",
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.grey[600],
                                                  fontFamily: 'Monospace',
                                                ),
                                              ),
                                            ),
                                            const SizedBox(height: 8),
                                            const SizedBox(height: 8),
                                            if (device['address'] != null)
                                              Container(
                                                padding:
                                                    const EdgeInsets.symmetric(
                                                  horizontal: 8,
                                                  vertical: 4,
                                                ),
                                                decoration: BoxDecoration(
                                                  color: Colors.green[50],
                                                  borderRadius:
                                                      BorderRadius.circular(4),
                                                  border: Border.all(
                                                    color: Colors.green[200]!,
                                                  ),
                                                ),
                                                child: Text(
                                                  "Mac: ${device['address']}",
                                                  style: TextStyle(
                                                    fontSize: 10,
                                                    color: Colors.green[800],
                                                    fontFamily: 'Monospace',
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                              ),
                                            const SizedBox(height: 8),
                                            Container(
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                horizontal: 8,
                                                vertical: 4,
                                              ),
                                              decoration: BoxDecoration(
                                                color: Colors.grey[200],
                                                borderRadius:
                                                    BorderRadius.circular(
                                                  4,
                                                ),
                                              ),
                                              child: Text(
                                                "Device Type: ${device['type']}",
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.grey[600],
                                                  fontFamily: 'Monospace',
                                                ),
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            )
                            .toList(),
                        const SizedBox(height: 16),
                      ],
                    );
                  },
                ),
    );
  }

  IconData _getIconForGroup(String group) {
    switch (group.toUpperCase()) {
      case 'WATCH':
        return Icons.watch;
      case 'BAND':
        return Icons.watch_outlined; // approximate
      case 'MOBILE':
        return Icons.smartphone;
      case 'RING':
        return Icons.radio_button_unchecked;
      case 'ACCESSORY':
        return Icons.devices_other;
      default:
        return Icons.device_unknown;
    }
  }
}
