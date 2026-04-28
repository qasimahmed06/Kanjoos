import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:intl/intl.dart';

void main() async {
  await Hive.initFlutter();
  await Hive.openBox('finance_data');
  runApp(const KanjoosApp());
}

String formatCurrency(double amount) {
  if (amount == 0) {
    return 'Rs. 0';
  }
  final sign = amount > 0 ? '+' : '';
  return '$sign Rs. ${amount.toStringAsFixed(0)}';
}

class KanjoosApp extends StatelessWidget {
  const KanjoosApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Kanjoos',
      theme: ThemeData(
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.teal,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
        textTheme: GoogleFonts.poppinsTextTheme(
          ThemeData(brightness: Brightness.dark).textTheme,
        ),
      ),
      home: const HomeScreen(),
    );
  }
}

class Transaction {
  final double amount;
  final bool isAddition;
  final DateTime date;

  Transaction({
    required this.amount,
    required this.isAddition,
    required this.date,
  });

  Map<String, dynamic> toMap() {
    return {
      'amount': amount,
      'isAddition': isAddition,
      'date': date.toIso8601String(),
    };
  }

  factory Transaction.fromMap(Map<dynamic, dynamic> map) {
    return Transaction(
      amount: (map['amount'] as num).toDouble(),
      isAddition: map['isAddition'] as bool,
      date: DateTime.parse(map['date'] as String),
    );
  }
}

class MonthData {
  final String monthYear;
  final List<Transaction> transactions;
  final double finalBalance;

  MonthData({
    required this.monthYear,
    required this.transactions,
    required this.finalBalance,
  });

  Map<String, dynamic> toMap() {
    return {
      'monthYear': monthYear,
      'transactions': transactions.map((t) => t.toMap()).toList(),
      'finalBalance': finalBalance,
    };
  }

  factory MonthData.fromMap(Map<dynamic, dynamic> map) {
    final rawTransactions = (map['transactions'] as List?) ?? const [];
    return MonthData(
      monthYear: map['monthYear'] as String,
      transactions: rawTransactions
          .map((t) => Transaction.fromMap(Map<dynamic, dynamic>.from(t as Map)))
          .toList(),
      finalBalance: (map['finalBalance'] as num).toDouble(),
    );
  }
}

class FinanceManager {
  static const String _boxName = 'finance_data';
  static const String _currentMonthKey = 'current_month';
  static const String _currentBalanceKey = 'current_balance';
  static const String _monthHistoryKey = 'month_history';

  static Future<Box> _getBox() async {
    return await Hive.openBox(_boxName);
  }

  static Future<String> _getCurrentMonthKey() async {
    final now = DateTime.now();
    return DateFormat('MMMM-yyyy').format(now);
  }

  static Future<List<Transaction>> getCurrentMonthTransactions() async {
    final box = await _getBox();
    final monthKey = await _getCurrentMonthKey();
    final stored = box.get('$_currentMonthKey:$monthKey', defaultValue: []) as List;
    return stored
      .map((t) => Transaction.fromMap(Map<dynamic, dynamic>.from(t as Map)))
        .toList();
  }

  static Future<double> getCurrentBalance() async {
    final box = await _getBox();
    return box.get(_currentBalanceKey, defaultValue: 0.0);
  }

  static Future<void> addTransaction(double amount, bool isAddition) async {
    final box = await _getBox();
    final monthKey = await _getCurrentMonthKey();

    // Get current transactions
    final stored =
        box.get('$_currentMonthKey:$monthKey', defaultValue: []) as List;
    final transactions = stored
      .map((t) => Transaction.fromMap(Map<dynamic, dynamic>.from(t as Map)))
        .toList();

    // Add new transaction
    final newTransaction = Transaction(
      amount: amount,
      isAddition: isAddition,
      date: DateTime.now(),
    );
    transactions.add(newTransaction);

    // Update stored transactions
    await box.put(
      '$_currentMonthKey:$monthKey',
      transactions.map((t) => t.toMap()).toList(),
    );

    // Update current balance
    final currentBalance = await getCurrentBalance();
    final newBalance = isAddition ? currentBalance + amount : currentBalance - amount;
    await box.put(_currentBalanceKey, newBalance);
  }

  static Future<void> resetMonthlyBalance() async {
    final box = await _getBox();
    final monthKey = await _getCurrentMonthKey();

    // Get current transactions
    final stored =
        box.get('$_currentMonthKey:$monthKey', defaultValue: []) as List;
    final transactions = stored
      .map((t) => Transaction.fromMap(Map<dynamic, dynamic>.from(t as Map)))
        .toList();

    // Get current balance
    final currentBalance = await getCurrentBalance();

    // Store month data in history
    final monthData = MonthData(
      monthYear: monthKey,
      transactions: transactions,
      finalBalance: currentBalance,
    );

    // Get existing history
    final historyStored =
        box.get(_monthHistoryKey, defaultValue: []) as List;
    final history = historyStored
      .map((m) => MonthData.fromMap(Map<dynamic, dynamic>.from(m as Map)))
        .toList();

    // Add new month data
    history.add(monthData);

    // Save history
    await box.put(
      _monthHistoryKey,
      history.map((m) => m.toMap()).toList(),
    );

    // Reset current month
    await box.put('$_currentMonthKey:$monthKey', []);
    await box.put(_currentBalanceKey, 0.0);
  }

  static Future<List<MonthData>> getMonthHistory() async {
    final box = await _getBox();
    final historyStored =
        box.get(_monthHistoryKey, defaultValue: []) as List;
    return historyStored
      .map((m) => MonthData.fromMap(Map<dynamic, dynamic>.from(m as Map)))
        .toList();
  }

  static Future<void> checkAndResetMonth() async {
    final box = await _getBox();
    final now = DateTime.now();
    final lastCheckKey = 'last_month_check';

    final lastCheckStr = box.get(lastCheckKey, defaultValue: '');
    final lastCheck = lastCheckStr.isNotEmpty
        ? DateTime.parse(lastCheckStr)
        : DateTime(now.year, now.month);

    // Check if month has changed
    if (lastCheck.month != now.month || lastCheck.year != now.year) {
      await resetMonthlyBalance();
    }

    // Update last check
    await box.put(lastCheckKey, now.toIso8601String());
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  double _currentBalance = 0.0;
  List<Transaction> _currentTransactions = [];
  List<MonthData> _monthHistory = [];
  String _selectedMonthYear = '';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    await FinanceManager.checkAndResetMonth();
    final balance = await FinanceManager.getCurrentBalance();
    final transactions = await FinanceManager.getCurrentMonthTransactions();
    final history = await FinanceManager.getMonthHistory();
    final currentMonth = await FinanceManager._getCurrentMonthKey();

    if (!mounted) {
      return;
    }

    setState(() {
      _currentBalance = balance;
      _currentTransactions = transactions;
      _monthHistory = history;
      _selectedMonthYear = currentMonth;
    });
  }

  void _showAddSubtractDialog(bool isAddition) {
    final controller = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: Colors.grey[900],
        title: Text(
          isAddition ? 'Add Amount' : 'Subtract Amount',
          style: GoogleFonts.poppins(
            fontWeight: FontWeight.w600,
            fontSize: 18,
          ),
        ),
        content: TextField(
          controller: controller,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          style: GoogleFonts.poppins(),
          decoration: InputDecoration(
            hintText: 'Enter amount',
            hintStyle: GoogleFonts.poppins(color: Colors.grey[600]),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(
                color: Colors.teal,
                width: 2,
              ),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(
              'Cancel',
              style: GoogleFonts.poppins(color: Colors.grey[400]),
            ),
          ),
          TextButton(
            onPressed: () async {
              final amount = double.tryParse(controller.text);
              if (amount != null && amount > 0) {
                await FinanceManager.addTransaction(amount, isAddition);
                await _loadData();
                Navigator.pop(context);
              }
            },
            child: Text(
              'OK',
              style: GoogleFonts.poppins(
                color: Colors.teal,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showMonthDetails(MonthData monthData) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.grey[900],
      builder: (context) => Container(
        padding: const EdgeInsets.all(16),
        height: MediaQuery.of(context).size.height * 0.6,
        child: Column(
          children: [
            Text(
              monthData.monthYear,
              style: GoogleFonts.poppins(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.5,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Final Balance: ${formatCurrency(monthData.finalBalance)}',
              style: GoogleFonts.poppins(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: monthData.finalBalance >= 0
                    ? Color(0xFF4CAF50)
                    : Color(0xFFEF5350),
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: ListView.builder(
                itemCount: monthData.transactions.length,
                itemBuilder: (context, index) {
                  final transaction = monthData.transactions[index];
                  return ListTile(
                    leading: Icon(
                        transaction.isAddition ? Icons.add_circle : Icons.remove_circle,
                        color:
                          transaction.isAddition ? Color(0xFF4CAF50) : Color(0xFFEF5350),
                        size: 24,
                    ),
                    title: Text(
                      '${transaction.isAddition ? '+' : '-'} Rs. ${transaction.amount.toStringAsFixed(0)}',
                      style: GoogleFonts.poppins(
                        fontWeight: FontWeight.w500,
                        fontSize: 14,
                      ),
                    ),
                    subtitle: Text(
                      DateFormat('MMM d, hh:mm a').format(transaction.date),
                                          style: GoogleFonts.poppins(fontSize: 12),
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

  void _showCurrentMonthDetails() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.grey[900],
      builder: (context) => Container(
        padding: const EdgeInsets.all(16),
        height: MediaQuery.of(context).size.height * 0.6,
        child: Column(
          children: [
            Text(
              _selectedMonthYear,
              style: GoogleFonts.poppins(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.5,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Current Balance: ${formatCurrency(_currentBalance)}',
              style: GoogleFonts.poppins(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: _currentBalance >= 0
                    ? Color(0xFF4CAF50)
                    : Color(0xFFEF5350),
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: _currentTransactions.isEmpty
                  ? Center(
                      child: Text(
                        'No transactions yet',
                        style: GoogleFonts.poppins(color: Colors.grey[500]),
                      ),
                    )
                  : ListView.builder(
                      itemCount: _currentTransactions.length,
                      itemBuilder: (context, index) {
                        final transaction = _currentTransactions[index];
                        return ListTile(
                          leading: Icon(
                            transaction.isAddition
                              ? Icons.add_circle
                              : Icons.remove_circle,
                            color: transaction.isAddition
                              ? Color(0xFF4CAF50)
                              : Color(0xFFEF5350),
                            size: 24,
                          ),
                          title: Text(
                            '${transaction.isAddition ? '+' : '-'} Rs. ${transaction.amount.toStringAsFixed(0)}',
                            style: GoogleFonts.poppins(
                              fontWeight: FontWeight.w500,
                              fontSize: 14,
                            ),
                          ),
                          subtitle: Text(
                            DateFormat('MMM d, hh:mm a')
                                .format(transaction.date),
                                                      style: GoogleFonts.poppins(fontSize: 12),
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

  @override
  Widget build(BuildContext context) {
    final isPositive = _currentBalance >= 0;

    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Kanjoos',
          style: GoogleFonts.poppins(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.5,
          ),
        ),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
      ),
      body: Column(
        children: [
          // Main Balance Display
          Expanded(
            child: Center(
              child: GestureDetector(
                onTap: _showCurrentMonthDetails,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      'Current Balance',
                      style: GoogleFonts.poppins(
                        fontSize: 18,
                        fontWeight: FontWeight.w300,
                        color: Colors.grey[400],
                        letterSpacing: 1.2,
                      ),
                    ),
                    const SizedBox(height: 24),
                    Text(
                      formatCurrency(_currentBalance),
                      style: GoogleFonts.poppins(
                        fontSize: 72,
                        fontWeight: FontWeight.w700,
                        color: isPositive ? Color(0xFF4CAF50) : Color(0xFFEF5350),
                        letterSpacing: -1,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      '(tap to see transactions)',
                      style: GoogleFonts.poppins(
                        fontSize: 12,
                        color: Colors.grey[500],
                        fontStyle: FontStyle.italic,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Add and Subtract Buttons
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                FloatingActionButton.extended(
                  onPressed: () => _showAddSubtractDialog(true),
                  backgroundColor: Color(0xFF4CAF50),
                  icon: const Icon(Icons.add),
                  label: Text(
                    'Add',
                    style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
                  ),
                ),
                FloatingActionButton.extended(
                  onPressed: () => _showAddSubtractDialog(false),
                  backgroundColor: Color(0xFFEF5350),
                  icon: const Icon(Icons.remove),
                  label: Text(
                    'Subtract',
                    style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
                  ),
                ),
              ],
            ),
          ),

          // Monthly History
          SizedBox(
            height: 140,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Text(
                    'Monthly History',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 0.5,
                    ),
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: _monthHistory.length + 1,
                    itemBuilder: (context, index) {
                      if (index == _monthHistory.length) {
                        // Current month card
                        return Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: GestureDetector(
                            onTap: _showCurrentMonthDetails,
                            child: Card(
                              elevation: 4,
                              color: Colors.grey[850],
                              child: Container(
                                width: 120,
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(
                                    color: Colors.teal,
                                    width: 2,
                                  ),
                                ),
                                child: Column(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  crossAxisAlignment: CrossAxisAlignment.center,
                                  children: [
                                    Text(
                                      _selectedMonthYear,
                                      style: GoogleFonts.poppins(
                                        fontWeight: FontWeight.w600,
                                        fontSize: 12,
                                      ),
                                      textAlign: TextAlign.center,
                                    ),
                                    Text(
                                      '(Current)',
                                      style: GoogleFonts.poppins(
                                        fontSize: 11,
                                        color: Colors.grey[500],
                                        fontWeight: FontWeight.w400,
                                      ),
                                    ),
                                    Text(
                                      _currentBalance == 0
                                          ? 'Rs. 0'
                                          : '${_currentBalance > 0 ? '+' : ''}Rs. ${_currentBalance.toStringAsFixed(0)}',
                                      style: GoogleFonts.poppins(
                                        fontWeight: FontWeight.w700,
                                        fontSize: 14,
                                        color: _currentBalance >= 0
                                            ? Color(0xFF4CAF50)
                                            : Color(0xFFEF5350),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      }

                      final monthData = _monthHistory[index];
                      return Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: GestureDetector(
                          onTap: () => _showMonthDetails(monthData),
                          child: Card(
                            elevation: 4,
                            color: Colors.grey[850],
                            child: Container(
                              width: 120,
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Column(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                crossAxisAlignment: CrossAxisAlignment.center,
                                children: [
                                  Text(
                                    monthData.monthYear,
                                    style: GoogleFonts.poppins(
                                      fontWeight: FontWeight.w600,
                                      fontSize: 12,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    monthData.finalBalance == 0
                                        ? 'Rs. 0'
                                        : '${monthData.finalBalance > 0 ? '+' : ''}Rs. ${monthData.finalBalance.toStringAsFixed(0)}',
                                    style: GoogleFonts.poppins(
                                      fontWeight: FontWeight.w700,
                                      fontSize: 14,
                                      color: monthData.finalBalance >= 0
                                          ? Color(0xFF4CAF50)
                                          : Color(0xFFEF5350),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}
