package com.example.busschedule.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.busschedule.model.AppDayOfWeek
import com.example.busschedule.model.BusSchedule
import com.example.busschedule.util.formatAs12Hour
import com.example.busschedule.util.formatTimeNicely
import com.example.busschedule.util.parseTimeForPicker
import com.example.busschedule.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    val weeklySchedule = themeViewModel.weeklySchedule.collectAsState().value
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(AppDayOfWeek.entries.toList()) { day ->
                val schedule = weeklySchedule.dailySchedule[day] ?: BusSchedule()
                DayScheduleRow(day = day, schedule = schedule) { newSchedule ->
                    themeViewModel.updateScheduleForDay(day, newSchedule)
                }
            }
        }
    }
}

@Composable
fun DayScheduleRow(day: AppDayOfWeek, schedule: BusSchedule, onScheduleChange: (BusSchedule) -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(day.displayName, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Leave Home: ${formatTimeNicely(schedule.homeToSchoolTime)}", modifier = Modifier.weight(1f))
            Button(onClick = {
                val (hour, minute) = parseTimeForPicker(schedule.homeToSchoolTime)
                TimePickerDialog(context, { _, h, m ->
                    val formatted = formatAs12Hour(h, m)
                    onScheduleChange(schedule.copy(homeToSchoolTime = formatted))
                }, hour, minute, false).show()
            }) {
                Text("Change")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Leave School: ${formatTimeNicely(schedule.schoolToHomeTime)}", modifier = Modifier.weight(1f))
            Button(onClick = {
                val (hour, minute) = parseTimeForPicker(schedule.schoolToHomeTime)
                TimePickerDialog(context, { _, h, m ->
                    val formatted = formatAs12Hour(h, m)
                    onScheduleChange(schedule.copy(schoolToHomeTime = formatted))
                }, hour, minute, false).show()
            }) {
                Text("Change")
            }
        }
    }
}