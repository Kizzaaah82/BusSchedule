package com.example.busschedule.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busschedule.viewmodel.BusPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusInfoBottomSheet(
    bus: BusPosition,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🚌 Bus Info", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Route: ${bus.routeId ?: "Unknown"}")
            Text("Trip ID: ${bus.tripId ?: "Unknown"}")
            Text("Vehicle ID: ${bus.vehicleId}")
            Text("Label: ${bus.label ?: "Unknown"}")
            Text("Heading: ${bus.bearing?.toInt() ?: 0}°")

            // 🎯 Add Occupancy Info
            val occupancy = when (bus.occupancyStatus?.lowercase()) {
                "empty" -> "🟢 Plenty of seats"
                "many_seats_available" -> "🟢 Some seats"
                "few_seats_available" -> "🟡 Few seats left"
                "standing_room_only" -> "🟠 Standing room only"
                "crushed_standing_room_only" -> "🔴 Packed like sardines"
                "full" -> "❌ Full — wait for the next one"
                else -> "❓ Unknown occupancy"
            }
            Text("Occupancy: $occupancy")

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
        }
    }
}