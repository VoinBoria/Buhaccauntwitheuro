LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
}

    // Додайте функцію для оновлення валюти
private fun updateCurrency(newCurrency: String) {
sharedPreferences.edit().putString("SELECTED_CURRENCY", newCurrency).apply()
val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_CURRENCY")
@@ -131,6 +130,10 @@
val updateExpensesIntent = Intent("com.example.homeaccountingapp.UPDATE_EXPENSES")
LocalBroadcastManager.getInstance(this).sendBroadcast(updateExpensesIntent)

        // Створення та відправка broadcast для оновлення доходів
        val updateIncomeIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIncomeIntent)

// Створення та відправка broadcast для оновлення валюти
val updateCurrencyIntent = Intent("com.example.homeaccountingapp.UPDATE_CURRENCY")
LocalBroadcastManager.getInstance(this).sendBroadcast(updateCurrencyIntent)
@@ -235,41 +238,39 @@
)

if (showCurrencyDialog) {
            CurrencySelectionDialog(onCurrencySelected = { currency ->
                updateCurrency(currency)  // Виклик функції для оновлення валюти
                sharedPreferences.edit().putBoolean("IS_FIRST_LAUNCH", false).apply()
                showCurrencyDialog = false
                selectedCurrency = currency

                // Load standard categories
                viewModel.reloadStandardCategories()
                incomes = viewModel.standardIncomeCategories.associateWith { 0.0 }
                expenses = viewModel.standardExpenseCategories.associateWith { 0.0 }
            })
            CurrencySelectionDialog(
                onCurrencySelected = { currency ->
                    updateCurrency(currency)  // Виклик функції для оновлення валюти
                    sharedPreferences.edit().putBoolean("IS_FIRST_LAUNCH", false).apply()
                    showCurrencyDialog = false
                    selectedCurrency = currency

                    // Load standard categories
                    viewModel.reloadStandardCategories()
                    incomes = viewModel.standardIncomeCategories.associateWith { 0.0 }
                    expenses = viewModel.standardExpenseCategories.associateWith { 0.0 }
                },
                onDismiss = {
                    showCurrencyDialog = false // Закриття діалогу без вибору валюти
                }
            )
}
}
}
@Composable
fun CurrencySelectionDialog(onCurrencySelected: (String) -> Unit) {
fun CurrencySelectionDialog(
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
val currencies = listOf("₴", "€", "$")
var selectedCurrency by remember { mutableStateOf(currencies[0]) }
    val context = LocalContext.current

AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Виберіть валюту")
        },
        onDismissRequest = onDismiss,
        title = { Text(text = "Виберіть валюту") },
text = {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Gray.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .border(1.dp, Color.Gray)
                    .padding(16.dp)
            ) {
            Column {
currencies.forEach { currency ->
Row(
verticalAlignment = Alignment.CenterVertically,
@@ -279,7 +280,7 @@
selected = (selectedCurrency == currency),
onClick = { selectedCurrency = currency }
)
                            .padding(horizontal = 16.dp)
                            .padding(16.dp)
) {
RadioButton(
selected = (selectedCurrency == currency),
@@ -291,16 +292,26 @@
}
},
confirmButton = {
            Button(
                onClick = {
                    onCurrencySelected(selectedCurrency)
                }
            ) {
            Button(onClick = {
                // Збережіть нову валюту в SharedPreferences
                val sharedPreferences = context.getSharedPreferences("com.serhio.homeaccountingapp.PREFERENCES", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("SELECTED_CURRENCY", selectedCurrency).apply()

                // Надіслати broadcast для оновлення валюти
                val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_CURRENCY")
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)

                onCurrencySelected(selectedCurrency)
                onDismiss() // Закриваємо діалог після збереження
            }) {
Text("Зберегти")
}
},
        dismissButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
)
}
// Функція Splash Screen
@@ -327,6 +338,7 @@
class MainViewModel(application: Application) : AndroidViewModel(application) {
private val sharedPreferencesExpense = application.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
private val sharedPreferencesIncome = application.getSharedPreferences("IncomePrefs", Context.MODE_PRIVATE)
    private val sharedPreferencesCurrency = application.getSharedPreferences("com.serhio.homeaccountingapp.PREFERENCES", Context.MODE_PRIVATE)
private val gson = Gson()

private val _expenses = MutableLiveData<Map<String, Double>>()
@@ -341,23 +353,30 @@
private val _incomeCategories = MutableLiveData<List<String>>()
val incomeCategories: LiveData<List<String>> = _incomeCategories

    private val _selectedCurrency = MutableLiveData<String>()
    val selectedCurrency: LiveData<String> = _selectedCurrency

// Списки стандартних категорій
val standardExpenseCategories = listOf("Аренда", "Комунальні послуги", "Транспорт", "Розваги", "Продукти", "Одяг", "Здоров'я", "Освіта", "Інші")
val standardIncomeCategories = listOf("Зарплата", "Премія", "Подарунки", "Пасивний дохід")

init {
loadStandardCategories()
        _selectedCurrency.value = sharedPreferencesCurrency.getString("SELECTED_CURRENCY", "₴") ?: "₴"
}

fun reloadStandardCategories() {
_expenseCategories.value = standardExpenseCategories
_incomeCategories.value = standardIncomeCategories
saveCategories(sharedPreferencesExpense, standardExpenseCategories)
saveCategories(sharedPreferencesIncome, standardIncomeCategories)
}

fun updateCategories(newCategories: List<String>) {
_incomeCategories.value = newCategories
saveCategories(sharedPreferencesIncome, newCategories)
}

fun addExpenseCategory(newCategory: String) {
val currentCategories = _expenseCategories.value ?: emptyList()
if (newCategory !in currentCategories) {
@@ -366,6 +385,7 @@
saveCategories(sharedPreferencesExpense, updatedCategories)
}
}

fun addIncomeCategory(newCategory: String) {
val currentCategories = _incomeCategories.value ?: emptyList()
if (newCategory !in currentCategories) {
@@ -530,442 +550,454 @@
fun refreshCategories() {
loadStandardCategories()
}

    fun updateCurrency(newCurrency: String) {
        _selectedCurrency.value = newCurrency
        sharedPreferencesCurrency.edit().putString("SELECTED_CURRENCY", newCurrency).apply()
        sendUpdateBroadcast()
    }

    private fun sendUpdateBroadcast() {
        val context = getApplication<Application>().applicationContext
        val updateCurrencyIntent = Intent("com.example.homeaccountingapp.UPDATE_CURRENCY")
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateCurrencyIntent)
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
onNavigateToMainActivity: () -> Unit,
onNavigateToIncomes: () -> Unit,
onNavigateToExpenses: () -> Unit,
onNavigateToIssuedOnLoan: () -> Unit,
onNavigateToBorrowed: () -> Unit,
onNavigateToAllTransactionIncome: () -> Unit,
onNavigateToAllTransactionExpense: () -> Unit,
onNavigateToBudgetPlanning: () -> Unit,
onNavigateToTaskActivity: () -> Unit,
viewModel: MainViewModel = viewModel(),
onIncomeCategoryClick: (String) -> Unit,
onExpenseCategoryClick: (String) -> Unit,
incomes: Map<String, Double>,
expenses: Map<String, Double>,
totalIncomes: Double,
totalExpenses: Double,
selectedCurrency: String, // Додаємо цей параметр
onSettingsClick: () -> Unit // Додаємо цей параметр
) {
val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
val scope = rememberCoroutineScope()
var showExpenses by remember { mutableStateOf(false) }
var showIncomes by remember { mutableStateOf(false) }
var showAddExpenseTransactionDialog by remember { mutableStateOf(false) }
var showAddIncomeTransactionDialog by remember { mutableStateOf(false) }
var showMessage by remember { mutableStateOf(false) }

LaunchedEffect(Unit) {
showExpenses = false
showIncomes = false
}

val expenses by viewModel.expenses.observeAsState(initial = emptyMap())
val incomes by viewModel.incomes.observeAsState(initial = emptyMap())
val expenseCategories by viewModel.expenseCategories.observeAsState(initial = emptyList())
val incomeCategories by viewModel.incomeCategories.observeAsState(initial = emptyList())

val totalExpenses = expenses.values.sum()
val totalIncomes = incomes.values.sum()
val balance = totalIncomes + totalExpenses

val showWarning = balance < 0
val showSuccess = balance > 0

var messagePhase by remember { mutableStateOf(0) }

LaunchedEffect(balance) {
showMessage = showWarning || showSuccess
if (showMessage) {
messagePhase = 1
delay(3000) // Повідомлення висить в середині екрану 3 секунди
messagePhase = 2
delay(1000) // Час для анімації спуску
messagePhase = 3
delay(2000) // Повідомлення висить внизу 2 секунди
showMessage = false
messagePhase = 0
}
}

val context = LocalContext.current
val pagerState = rememberPagerState()

ModalNavigationDrawer(
drawerState = drawerState,
drawerContent = {
DrawerContent(
onNavigateToMainActivity = { scope.launch { drawerState.close(); onNavigateToMainActivity() } },
onNavigateToIncomes = { scope.launch { drawerState.close(); onNavigateToIncomes() } },
onNavigateToExpenses = { scope.launch { drawerState.close(); onNavigateToExpenses() } },
onNavigateToIssuedOnLoan = { scope.launch { drawerState.close(); onNavigateToIssuedOnLoan() } },
onNavigateToBorrowed = { scope.launch { drawerState.close(); onNavigateToBorrowed() } },
onNavigateToAllTransactionIncome = { scope.launch { drawerState.close(); onNavigateToAllTransactionIncome() } },
onNavigateToAllTransactionExpense = { scope.launch { drawerState.close(); onNavigateToAllTransactionExpense() } },
onNavigateToBudgetPlanning = { scope.launch { drawerState.close(); onNavigateToBudgetPlanning() } },
onNavigateToTaskActivity = { scope.launch { drawerState.close(); onNavigateToTaskActivity() } }
)
}
) {
Scaffold(
topBar = {
TopAppBar(
title = { Text("Домашня бухгалтерія", color = Color.White) },
navigationIcon = {
IconButton(onClick = { scope.launch { drawerState.open() } }) {
Icon(
imageVector = Icons.Default.Menu,
contentDescription = "Меню",
tint = Color.White
)
}
},
actions = {
IconButton(onClick = onSettingsClick) {
Icon(
imageVector = Icons.Default.Settings,
contentDescription = "Налаштування",
tint = Color.White
)
}
},
colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
)
},
content = { innerPadding ->
Box(
modifier = Modifier
.fillMaxSize()
.paint(
painter = painterResource(id = R.drawable.background_app),
contentScale = ContentScale.Crop
)
.padding(innerPadding)
) {
BoxWithConstraints {
val isWideScreen = maxWidth > 600.dp

if (isWideScreen) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 16.dp, vertical = 32.dp),
horizontalArrangement = Arrangement.SpaceBetween
) {
Column(
modifier = Modifier
.widthIn(max = 400.dp),  // Обмеження ширини контейнера
horizontalAlignment = Alignment.CenterHorizontally
) {
Box(
modifier = Modifier
.widthIn(max = 400.dp) // Обмеження ширини кнопки
.padding(vertical = 8.dp)
.clip(RoundedCornerShape(10.dp))
.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
.border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
) {
ExpandableButtonWithAmount(
text = "Доходи: ",
amount = totalIncomes,
gradientColors = listOf(
Color.Transparent,
Color.Transparent
),
isExpanded = showIncomes,
onClick = { showIncomes = !showIncomes },
textColor = Color(0xFF00FF00), // Яскравий зелений колір тексту
fontWeight = FontWeight.Bold,  // Жирний шрифт
fontSize = 18.sp // Збільшення шрифту
)
}

if (showIncomes) {
Column(
modifier = Modifier
.heightIn(max = 200.dp) // Обмеження висоти списку
.verticalScroll(rememberScrollState())
) {
IncomeList(incomes = incomes, selectedCurrency = selectedCurrency, onCategoryClick = onIncomeCategoryClick)
}
}
Spacer(modifier = Modifier.height(16.dp))

Box(
modifier = Modifier
.widthIn(max = 400.dp) // Обмеження ширини кнопки
.padding(vertical = 8.dp)
.clip(RoundedCornerShape(10.dp))
.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
.border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
) {
ExpandableButtonWithAmount(
text = "Витрати: ",
amount = totalExpenses,
gradientColors = listOf(
Color.Transparent,
Color.Transparent
),
isExpanded = showExpenses,
onClick = { showExpenses = !showExpenses },
textColor = Color(0xFFFF0000), // Яскравий червоний колір тексту
fontWeight = FontWeight.Bold,  // Жирний шрифт
fontSize = 18.sp // Збільшення шрифту
)
}

if (showExpenses) {
Column(
modifier = Modifier
.heightIn(max = 200.dp) // Обмеження висоти списку
.verticalScroll(rememberScrollState())
) {
ExpensesList(expenses = expenses, selectedCurrency = selectedCurrency, onCategoryClick = onExpenseCategoryClick)
}
}
}

Box(
modifier = Modifier
.widthIn(max = 400.dp) // Обмеження ширини контейнера для діаграм
.padding(vertical = 8.dp)
.clip(RoundedCornerShape(10.dp))
.background(Color.Transparent, RoundedCornerShape(10.dp)) // Прозорий фон
.border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
) {
IncomeExpenseChart(
incomes = incomes,
expenses = expenses,
totalIncomes = totalIncomes,
totalExpenses = totalExpenses
)
}
}
} else {
Column(
modifier = Modifier
.fillMaxSize()
.padding(horizontal = 16.dp, vertical = 32.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.SpaceBetween
) {
Column(
modifier = Modifier
.fillMaxWidth()
.widthIn(max = 600.dp),  // Обмеження ширини контейнера
horizontalAlignment = Alignment.CenterHorizontally
) {
Box(
modifier = Modifier
.widthIn(max = 400.dp) // Обмеження ширини кнопки
.padding(vertical = 8.dp)
.clip(RoundedCornerShape(10.dp))
.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
.border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
) {
ExpandableButtonWithAmount(
text = "Доходи: ",
amount = totalIncomes,
gradientColors = listOf(
Color.Transparent,
Color.Transparent
),
isExpanded = showIncomes,
onClick = { showIncomes = !showIncomes },
textColor = Color(0xFF00FF00), // Яскравий зелений колір тексту
fontWeight = FontWeight.Bold,  // Жирний шрифт
fontSize = 18.sp // Збільшення шрифту
)
}

if (showIncomes) {
Column(
modifier = Modifier
.heightIn(max = 200.dp) // Обмеження висоти списку
.verticalScroll(rememberScrollState())
) {
IncomeList(incomes = incomes, selectedCurrency = selectedCurrency, onCategoryClick = onIncomeCategoryClick)
}
}
Spacer(modifier = Modifier.height(16.dp))

Box(
modifier = Modifier
.widthIn(max = 400.dp) // Обмеження ширини кнопки
.padding(vertical = 8.dp)
.clip(RoundedCornerShape(10.dp))
.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
.border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
) {
ExpandableButtonWithAmount(
text = "Витрати: ",
amount = totalExpenses,
gradientColors = listOf(
Color.Transparent,
Color.Transparent
),
isExpanded = showExpenses,
onClick = { showExpenses = !showExpenses },
textColor = Color(0xFFFF0000), // Яскравий червоний колір тексту
fontWeight = FontWeight.Bold,  // Жирний шрифт
fontSize = 18.sp // Збільшення шрифту
)
}

if (showExpenses) {
Column(
modifier = Modifier
.heightIn(max = 200.dp) // Обмеження висоти списку
.verticalScroll(rememberScrollState())
) {
ExpensesList(expenses = expenses, selectedCurrency = selectedCurrency, onCategoryClick = onExpenseCategoryClick)
}
}

Spacer(modifier = Modifier.height(32.dp))

// Діаграми доходів та витрат
Box(
modifier = Modifier
.widthIn(max = 400.dp) // Обмеження ширини контейнера для діаграм
.padding(vertical = 8.dp)
.clip(RoundedCornerShape(10.dp))
.background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
.border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
) {
IncomeExpenseChart(
incomes = incomes,
expenses = expenses,
totalIncomes = totalIncomes,
totalExpenses = totalExpenses
)
}
}
}
}
}

// Відображення залишку та кнопок
Box(
modifier = Modifier
.fillMaxSize()
.padding(start = 16.dp, bottom = 15.dp)
.zIndex(0f), // Встановлення нижчого zIndex для залишку
contentAlignment = Alignment.BottomStart
) {
BalanceDisplay(balance = balance, selectedCurrency = selectedCurrency)
}

Box(
modifier = Modifier
.fillMaxSize()
.zIndex(0f) // Встановлення нижчого zIndex для кнопок додавання транзакцій
) {
}

if (showAddExpenseTransactionDialog) {
AddTransactionDialog(
categories = expenseCategories,
onDismiss = { showAddExpenseTransactionDialog = false },
onSave = { transaction: Transaction -> // Явно вказуємо тип параметра
viewModel.saveExpenseTransaction(context, transaction)
viewModel.refreshExpenses()
showAddExpenseTransactionDialog = false
},
onAddCategory = { newCategory ->
viewModel.addExpenseCategory(newCategory) // Виклик методу для додавання нової категорії
}
)
}

if (showAddIncomeTransactionDialog) {
IncomeAddIncomeTransactionDialog(
categories = incomeCategories,
onDismiss = { showAddIncomeTransactionDialog = false },
onSave = { incomeTransaction ->
viewModel.saveIncomeTransaction(context, incomeTransaction)
viewModel.refreshIncomes()
showAddIncomeTransactionDialog = false
},
onAddCategory = { newCategory ->
viewModel.addIncomeCategory(newCategory)
} // Додано параметр onAddCategory
)
}

// Повідомлення
AnimatedVisibility(
visible = showMessage,
enter = slideInVertically(
initialOffsetY = { fullHeight -> fullHeight }
),
exit = slideOutVertically(
targetOffsetY = { fullHeight -> fullHeight }
),
modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
) {
Box(
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 16.dp)
.background(
if (showWarning) Color(0x80B22222) else Color(0x8000B22A),
RoundedCornerShape(8.dp)
)
.padding(16.dp)
) {
Text(
text = if (showWarning) "Вам потрібно менше витрачати" else "Ви на вірному шляху",
style = MaterialTheme.typography.bodyLarge.copy(
color = Color.White,
fontWeight = FontWeight.Bold
)
)
}
}

Row(
modifier = Modifier
.align(Alignment.BottomEnd)
.padding(end = 16.dp, bottom = 16.dp)
.offset(y = (-16).dp)
) {
FloatingActionButton(
onClick = { showAddIncomeTransactionDialog = true },
containerColor = Color.LightGray, // Змінив колір на більш білий
modifier = Modifier.padding(end = 16.dp)
) {
Text("+", color = Color.Black, style = MaterialTheme.typography.bodyLarge) // Плюсик всередині чорний
}

FloatingActionButton(
onClick = { showAddExpenseTransactionDialog = true },
containerColor = Color(0xFFe6194B) // Зробити ще темніший сірий колір
) {
Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge) // Плюсик всередині білий
}
}
}
}
)
}
}
private fun Modifier.backgroundWithImage(
painter: androidx.compose.ui.graphics.painter.Painter,
contentScale: ContentScale
): Modifier {
return this.then(
Modifier.paint(
painter = painter,
contentScale = contentScale
)
)
}
