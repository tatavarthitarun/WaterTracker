package com.tatav.watertracker

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.watertracker.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import kotlinx.coroutines.delay

@AndroidEntryPoint
class WaterIntakeWidget : AppWidgetProvider() {

    @Inject
    lateinit var repository: WaterIntakeRepository

    companion object {
        const val ACTION_ADD_WATER = "com.example.watertracker.ACTION_ADD_WATER"
        const val DEFAULT_AMOUNT = 1.0f // 1L in liters
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_ADD_WATER) {
            // Add water using the repository
            CoroutineScope(Dispatchers.IO).launch {
                repository.addWaterIntake(LocalDate.now(), DEFAULT_AMOUNT)
            }

            // Show a toast to indicate success
            Toast.makeText(context, R.string.water_added, Toast.LENGTH_SHORT).show()

            // Update all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                intent.component
            )

            // Create a temporary view for animation
            val views = RemoteViews(context.packageName, R.layout.water_intake_widget)
            views.setInt(R.id.btn_add_water, "setBackgroundResource", R.drawable.widget_button_pressed)
            appWidgetManager.updateAppWidget(appWidgetIds, views)

            // Reset the button background after a short delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(200) // 200ms delay
                val resetViews = RemoteViews(context.packageName, R.layout.water_intake_widget)
                appWidgetManager.updateAppWidget(appWidgetIds, resetViews)
            }

            // Trigger update
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Create an Intent to launch the MainActivity when widget is clicked
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntentFlag()
        )

        // Create an Intent for the action button
        val addWaterIntent = Intent(context, WaterIntakeWidget::class.java).apply {
            action = ACTION_ADD_WATER
        }
        val addWaterPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            addWaterIntent,
            PendingIntentFlag()
        )

        // Get the layout for the widget and attach click listeners
        val views = RemoteViews(context.packageName, R.layout.water_intake_widget)
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        views.setOnClickPendingIntent(R.id.btn_add_water, addWaterPendingIntent)

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun PendingIntentFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Toast.makeText(context, R.string.widget_added, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaterTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                navigateToHistory = {
                                    navController.navigate("history")
                                }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                onNavigateUp = {
                                    navController.navigateUp()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C9B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFE4FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF4D616C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E6F2),
    onSecondaryContainer = Color(0xFF081E27),
    background = Color(0xFFF8FDFF),
    onBackground = Color(0xFF001F2A),
    surface = Color(0xFFF8FDFF),
    onSurface = Color(0xFF001F2A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FCAFF),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004D70),
    onPrimaryContainer = Color(0xFFBFE4FF),
    secondary = Color(0xFFB4CAD6),
    onSecondary = Color(0xFF1F333D),
    secondaryContainer = Color(0xFF354A54),
    onSecondaryContainer = Color(0xFFD0E6F2),
    background = Color(0xFF001F2A),
    onBackground = Color(0xFFBFE4FF),
    surface = Color(0xFF001F2A),
    onSurface = Color(0xFFBFE4FF)
)

@Composable
fun WaterTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Motivational greeting for Jovin
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hey Tarun!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stay hydrated and keep crushing your goals!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Center the main water intake card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TodayWaterIntakeCard(
                        date = uiState.todayDate,
                        amount = uiState.todayAmount,
                        onIncreaseClick = { viewModel.increaseWaterIntake() },
                        onDecreaseClick = { viewModel.decreaseWaterIntake() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Records as horizontal scroll
                Text(
                    text = "Recent Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (uiState.recentIntakes.isEmpty()) {
                    Text(
                        text = "No recent records available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.recentIntakes) { intake ->
                            Card(
                                modifier = Modifier.width(200.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = intake.date.format(DateTimeFormatter.ofPattern("MMM d")),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${intake.amount} L",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayWaterIntakeCard(
    date: java.time.LocalDate,
    amount: Float,
    onIncreaseClick: () -> Unit,
    onDecreaseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$amount L",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Water Intake",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FloatingActionButton(
                    onClick = onDecreaseClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease Water Intake"
                    )
                }

                FloatingActionButton(
                    onClick = onIncreaseClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase Water Intake"
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water Intake History") }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.allIntakes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn {
                    items(uiState.allIntakes) { intake ->
                        WaterIntakeItem(waterIntake = intake)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun WaterIntakeItem(waterIntake: WaterIntake) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = waterIntake.date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "${waterIntake.amount} L",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WaterIntakeRepository
) : ViewModel() {

    private val _todayIntake = MutableStateFlow<WaterIntake?>(null)
    private val _recentIntakes = MutableStateFlow<List<WaterIntake>>(emptyList())

    // UI state
    data class HomeUiState(
        val todayIntake: WaterIntake? = null,
        val todayAmount: Float = 0f,
        val todayDate: LocalDate = LocalDate.now(),
        val recentIntakes: List<WaterIntake> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            // Combine today's intake and recent intakes into UI state
            combine(
                repository.getWaterIntakeForToday(),
                repository.getRecentWaterIntakes(7)
            ) { todayIntake, recentIntakes ->
                HomeUiState(
                    todayIntake = todayIntake,
                    todayAmount = todayIntake?.amount ?: 0f,
                    todayDate = LocalDate.now(),
                    recentIntakes = recentIntakes,
                    isLoading = false
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState()
            ).collect {
                _uiState.value = it
            }
        }
    }

    fun increaseWaterIntake(amount: Float = 1.0f) {
        viewModelScope.launch {
            repository.addWaterIntake(LocalDate.now(), amount)
        }
    }

    fun decreaseWaterIntake(amount: Float = 1.0f) {
        val currentAmount = _uiState.value.todayAmount
        if (currentAmount - amount >= 0) {
            viewModelScope.launch {
                repository.addWaterIntake(LocalDate.now(), -amount)
            }
        }
    }
}


@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WaterIntakeRepository
) : ViewModel() {

    data class HistoryUiState(
        val allIntakes: List<WaterIntake> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        CoroutineScope(Dispatchers.IO).launch {
        repository.getAllWaterIntakes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            ).collect { intakes ->
                _uiState.value = HistoryUiState(
                    allIntakes = intakes,
                    isLoading = false
                )
            }}
    }
}
@Entity(tableName = "water_intake")
data class WaterIntakeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val amount: Float // in liters
)


@Dao
interface WaterIntakeDao {
    @Query("SELECT * FROM water_intake WHERE date = :date")
    fun getWaterIntakeForDate(date: LocalDate): Flow<WaterIntakeEntity?>

    @Query("SELECT * FROM water_intake ORDER BY date DESC")
    fun getAllWaterIntakes(): Flow<List<WaterIntakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterIntake(waterIntakeEntity: WaterIntakeEntity): Long

    @Update
    suspend fun updateWaterIntake(waterIntakeEntity: WaterIntakeEntity): Int

    @Query("SELECT * FROM water_intake ORDER BY date DESC LIMIT :limit")
    fun getRecentWaterIntakes(limit: Int): Flow<List<WaterIntakeEntity>>

    @Query("DELETE FROM water_intake WHERE id = :id")
    suspend fun deleteWaterIntake(id: Long): Int

    @Query("SELECT SUM(amount) FROM water_intake")
    fun getTotalWaterIntake(): Flow<Float?>
}
class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }
}

class WaterIntakeRepository @Inject constructor(
    private val waterIntakeDao: WaterIntakeDao
) {
    fun getWaterIntakeForToday(): Flow<WaterIntake?> {
        return waterIntakeDao.getWaterIntakeForDate(LocalDate.now())
            .map { it?.toWaterIntake() }
    }

    fun getWaterIntakeForDate(date: LocalDate): Flow<WaterIntake?> {
        return waterIntakeDao.getWaterIntakeForDate(date)
            .map { it?.toWaterIntake() }
    }

    fun getAllWaterIntakes(): Flow<List<WaterIntake>> {
        return waterIntakeDao.getAllWaterIntakes()
            .map { list -> list.map { it.toWaterIntake() } }
    }

    fun getRecentWaterIntakes(limit: Int): Flow<List<WaterIntake>> {
        return waterIntakeDao.getRecentWaterIntakes(limit)
            .map { list -> list.map { it.toWaterIntake() } }
    }

    suspend fun addWaterIntake(date: LocalDate, amountToAdd: Float) {
        // Get the current intake entity using first() which completes with the first emission
        val currentIntake = waterIntakeDao.getWaterIntakeForDate(date).firstOrNull()

        if (currentIntake != null) {
            // Update existing record
            val updatedEntity = WaterIntakeEntity(
                id = currentIntake.id,
                date = currentIntake.date,
                amount = maxOf(0f, currentIntake.amount + amountToAdd) // Prevent negative values
            )
            waterIntakeDao.updateWaterIntake(updatedEntity)
        } else {
            // Create new record
            val newEntity = WaterIntakeEntity(
                date = date,
                amount = maxOf(0f, amountToAdd) // Ensure positive amount
            )
            waterIntakeDao.insertWaterIntake(newEntity)
        }
    }
}


@Database(entities = [WaterIntakeEntity::class], version = 1, exportSchema = false)
@TypeConverters(DateConverters::class)
abstract class WaterIntakeDatabase : RoomDatabase() {
    abstract fun waterIntakeDao(): WaterIntakeDao

    companion object {
        @Volatile
        private var INSTANCE: WaterIntakeDatabase? = null

        fun getDatabase(context: Context): WaterIntakeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterIntakeDatabase::class.java,
                    "water_intake_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


private fun WaterIntakeEntity.toWaterIntake(): WaterIntake {
    return WaterIntake(
        id = id,
        date = date,
        amount = amount
    )
}

private fun WaterIntake.toEntity(): WaterIntakeEntity {
    return WaterIntakeEntity(
        id = id,
        date = date,
        amount = amount
    )
}



data class WaterIntake(
    val id: Long = 0,
    val date: LocalDate,
    val amount: Float // in liters
)

@HiltAndroidApp
class WaterTrackerApplication : Application()


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWaterIntakeDatabase(@ApplicationContext context: Context): WaterIntakeDatabase {
        return WaterIntakeDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWaterIntakeDao(database: WaterIntakeDatabase): WaterIntakeDao {
        return database.waterIntakeDao()
    }

    @Provides
    @Singleton
    fun provideWaterIntakeRepository(waterIntakeDao: WaterIntakeDao): WaterIntakeRepository {
        return WaterIntakeRepository(waterIntakeDao)
    }
}