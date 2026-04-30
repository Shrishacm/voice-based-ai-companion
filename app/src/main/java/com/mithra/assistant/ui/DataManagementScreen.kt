package com.mithra.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithra.assistant.data.local.entity.AppointmentEntity
import com.mithra.assistant.data.local.entity.MedicationEntity
import com.mithra.assistant.data.local.entity.PatientEntity


// ─── Color Palette ──────────────────────────────────────────────────────────────
private val BgDark   = Color(0xFF0F1117)
private val Surface1 = Color(0xFF1A1D27)
private val Surface2 = Color(0xFF22263A)
private val AccentTeal  = Color(0xFF00C9B1)
private val AccentBlue  = Color(0xFF3D8EFF)
private val AccentAmber = Color(0xFFFFAA00)
private val AccentRed   = Color(0xFFFF5C5C)
private val TextPrimary   = Color(0xFFE8EAF6)
private val TextSecondary = Color(0xFF9E9EB2)

// ─── Root Screen ────────────────────────────────────────────────────────────────

@Composable
fun DataManagementScreen(viewModel: DataManagementViewModel) {
    val patients     by viewModel.patients.collectAsState()
    val medications  by viewModel.medications.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val toast        by viewModel.toast.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Patients", "Medications", "Appointments")

    // Toast snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgDark)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface1)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column {
                    Text(
                        "Health Records",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Manage patient data, medications & appointments",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface1,
                contentColor = AccentTeal,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(AccentTeal)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) AccentTeal else TextSecondary,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> PatientsTab(patients, viewModel)
                1 -> MedicationsTab(medications, patients, viewModel)
                2 -> AppointmentsTab(appointments, patients, viewModel)
            }
        }
    }
}

// ─── Patients Tab ───────────────────────────────────────────────────────────────

@Composable
fun PatientsTab(patients: List<PatientEntity>, viewModel: DataManagementViewModel) {
    var showAddForm by remember { mutableStateOf(false) }

    // Form fields
    var name       by remember { mutableStateOf("") }
    var relation   by remember { mutableStateOf("") }
    var age        by remember { mutableStateOf("") }
    var blood      by remember { mutableStateOf("") }
    var phone      by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var allergies  by remember { mutableStateOf("") }
    var notes      by remember { mutableStateOf("") }
    var lastCheckup by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AddButton("Add Patient") { showAddForm = !showAddForm }
        }

        item {
            AnimatedVisibility(
                visible = showAddForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DataCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("New Patient")
                        MithraTextField("Full Name *", name) { name = it }
                        MithraTextField("Relation (e.g. Patient, Son)", relation) { relation = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MithraTextField("Age", age, Modifier.weight(1f), KeyboardType.Number) { age = it }
                            MithraTextField("Blood Group", blood, Modifier.weight(1f)) { blood = it }
                        }
                        MithraTextField("Phone", phone, keyboardType = KeyboardType.Phone) { phone = it }
                        MithraTextField("Medical Conditions", conditions) { conditions = it }
                        MithraTextField("Allergies", allergies) { allergies = it }
                        MithraTextField("Last Checkup (YYYY-MM-DD)", lastCheckup) { lastCheckup = it }
                        MithraTextField("Notes", notes) { notes = it }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showAddForm = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    viewModel.addPatient(name, relation, age, blood, phone, conditions, allergies, notes, lastCheckup)
                                    name = ""; relation = ""; age = ""; blood = ""; phone = ""
                                    conditions = ""; allergies = ""; notes = ""; lastCheckup = ""
                                    showAddForm = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                            ) { Text("Save", color = BgDark, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        if (patients.isEmpty()) {
            item { EmptyState("No patients yet. Add one above.") }
        } else {
            items(patients, key = { it.id }) { patient ->
                PatientCard(patient, onDelete = { viewModel.deletePatient(patient) })
            }
        }
    }
}

@Composable
fun PatientCard(patient: PatientEntity, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    DataCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(patient.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Chip(patient.relation, AccentBlue)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Age", "${patient.age} yrs")
            InfoRow("Blood Group", patient.bloodGroup)
            InfoRow("Phone", patient.phone)
            if (patient.conditions.isNotBlank()) InfoRow("Conditions", patient.conditions)
            if (patient.allergies.isNotBlank()) InfoRow("Allergies", patient.allergies)
            if (patient.lastCheckup.isNotBlank()) InfoRow("Last Checkup", patient.lastCheckup)
            if (patient.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(patient.notes, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
    if (showConfirm) {
        DeleteDialog(name = patient.name, onConfirm = { onDelete(); showConfirm = false }, onDismiss = { showConfirm = false })
    }
}

// ─── Medications Tab ─────────────────────────────────────────────────────────────

@Composable
fun MedicationsTab(medications: List<MedicationEntity>, patients: List<PatientEntity>, viewModel: DataManagementViewModel) {
    var showAddForm by remember { mutableStateOf(false) }
    var patientName  by remember { mutableStateOf("") }
    var medicineName by remember { mutableStateOf("") }
    var dosage       by remember { mutableStateOf("") }
    var frequency    by remember { mutableStateOf("") }
    var timing       by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var hour1        by remember { mutableStateOf("") }
    var minute1      by remember { mutableStateOf("") }
    var hour2        by remember { mutableStateOf("") }
    var minute2      by remember { mutableStateOf("") }

    LaunchedEffect(patients) {
        if (patientName.isBlank() && patients.isNotEmpty()) patientName = patients.first().name
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { AddButton("Add Medication") { showAddForm = !showAddForm } }

        item {
            AnimatedVisibility(visible = showAddForm, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                DataCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("New Medication")
                        MithraTextField("Patient Name *", patientName) { patientName = it }
                        MithraTextField("Medicine Name *", medicineName) { medicineName = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MithraTextField("Dosage (e.g. 75mg)", dosage, Modifier.weight(1f)) { dosage = it }
                            MithraTextField("Frequency", frequency, Modifier.weight(1f)) { frequency = it }
                        }
                        MithraTextField("Timing (e.g. 8:00 AM, 10:00 PM)", timing) { timing = it }
                        MithraTextField("Instructions (e.g. Before food)", instructions) { instructions = it }

                        Divider(color = Surface2)
                        Text("Alarm 1 (optional)", fontSize = 12.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MithraTextField("Hour (0-23)", hour1, Modifier.weight(1f), KeyboardType.Number) { hour1 = it }
                            MithraTextField("Minute", minute1, Modifier.weight(1f), KeyboardType.Number) { minute1 = it }
                        }
                        Text("Alarm 2 (optional)", fontSize = 12.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MithraTextField("Hour (0-23)", hour2, Modifier.weight(1f), KeyboardType.Number) { hour2 = it }
                            MithraTextField("Minute", minute2, Modifier.weight(1f), KeyboardType.Number) { minute2 = it }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showAddForm = false }, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) { Text("Cancel") }
                            Button(
                                onClick = {
                                    viewModel.addMedication(
                                        patientName, medicineName, dosage, frequency, timing, instructions,
                                        hour1.toIntOrNull() ?: -1, minute1.toIntOrNull() ?: 0,
                                        hour2.toIntOrNull() ?: -1, minute2.toIntOrNull() ?: 0
                                    )
                                    medicineName = ""; dosage = ""; frequency = ""; timing = ""
                                    instructions = ""; hour1 = ""; minute1 = ""; hour2 = ""; minute2 = ""
                                    showAddForm = false
                                },
                                Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                            ) { Text("Save & Schedule", color = BgDark, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        if (medications.isEmpty()) {
            item { EmptyState("No medications yet.") }
        } else {
            items(medications, key = { it.id }) { med ->
                MedicationCard(med, onDelete = { viewModel.deleteMedication(med) })
            }
        }
    }
}

@Composable
fun MedicationCard(med: MedicationEntity, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    DataCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Medication, null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(med.medicineName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Chip(med.dosage, AccentAmber)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            InfoRow("Patient", med.patientName)
            InfoRow("Frequency", med.frequency)
            InfoRow("Timing", med.timing)
            if (med.instructions.isNotBlank()) InfoRow("Instructions", med.instructions)
            if (med.reminderHour1 >= 0) {
                val t1 = "%02d:%02d".format(med.reminderHour1, med.reminderMinute1)
                val t2 = if (med.reminderHour2 >= 0) ", %02d:%02d".format(med.reminderHour2, med.reminderMinute2) else ""
                InfoRow("⏰ Alarms", "$t1$t2")
            }
            if (!med.isActive) {
                Spacer(Modifier.height(4.dp))
                Text("Inactive", color = AccentRed, fontSize = 11.sp)
            }
        }
    }
    if (showConfirm) {
        DeleteDialog(name = med.medicineName, onConfirm = { onDelete(); showConfirm = false }, onDismiss = { showConfirm = false })
    }
}

// ─── Appointments Tab ────────────────────────────────────────────────────────────

@Composable
fun AppointmentsTab(appointments: List<AppointmentEntity>, patients: List<PatientEntity>, viewModel: DataManagementViewModel) {
    var showAddForm  by remember { mutableStateOf(false) }
    var patientName  by remember { mutableStateOf("") }
    var doctorName   by remember { mutableStateOf("") }
    var specialty    by remember { mutableStateOf("") }
    var hospital     by remember { mutableStateOf("") }
    var date         by remember { mutableStateOf("") }
    var time         by remember { mutableStateOf("") }
    var notes        by remember { mutableStateOf("") }

    LaunchedEffect(patients) {
        if (patientName.isBlank() && patients.isNotEmpty()) patientName = patients.first().name
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { AddButton("Add Appointment") { showAddForm = !showAddForm } }

        item {
            AnimatedVisibility(visible = showAddForm, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                DataCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("New Appointment")
                        MithraTextField("Patient Name", patientName) { patientName = it }
                        MithraTextField("Doctor Name *", doctorName) { doctorName = it }
                        MithraTextField("Specialty (e.g. Neurology)", specialty) { specialty = it }
                        MithraTextField("Hospital / Clinic", hospital) { hospital = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MithraTextField("Date (YYYY-MM-DD) *", date, Modifier.weight(1f)) { date = it }
                            MithraTextField("Time (10:00 AM) *", time, Modifier.weight(1f)) { time = it }
                        }
                        MithraTextField("Notes", notes) { notes = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showAddForm = false }, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) { Text("Cancel") }
                            Button(
                                onClick = {
                                    viewModel.addAppointment(patientName, doctorName, specialty, hospital, date, time, notes)
                                    doctorName = ""; specialty = ""; hospital = ""; date = ""; time = ""; notes = ""
                                    showAddForm = false
                                },
                                Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) { Text("Save & Remind", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        if (appointments.isEmpty()) {
            item { EmptyState("No appointments yet.") }
        } else {
            items(appointments, key = { it.id }) { appt ->
                AppointmentCard(appt, onDelete = { viewModel.deleteAppointment(appt) })
            }
        }
    }
}

@Composable
fun AppointmentCard(appt: AppointmentEntity, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    DataCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(appt.doctorName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Text(appt.specialty, fontSize = 12.sp, color = TextSecondary)
                }
                IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            InfoRow("Patient", appt.patientName)
            InfoRow("Hospital", appt.hospital)
            InfoRow("Date & Time", "${appt.date}  ${appt.time}")
            if (appt.notes.isNotBlank()) InfoRow("Notes", appt.notes)
            if (appt.reminderScheduled) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, null, tint = AccentTeal, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reminder set (1hr before)", fontSize = 11.sp, color = AccentTeal)
                }
            }
        }
    }
    if (showConfirm) {
        DeleteDialog(name = "appointment with ${appt.doctorName}", onConfirm = { onDelete(); showConfirm = false }, onDismiss = { showConfirm = false })
    }
}

// ─── Shared Components ───────────────────────────────────────────────────────────

@Composable
fun DataCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun AddButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface2)
    ) {
        Icon(Icons.Default.Add, null, tint = AccentTeal)
        Spacer(Modifier.width(8.dp))
        Text(label, color = AccentTeal, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
    Divider(color = Surface2, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun MithraTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, color = TextSecondary) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentTeal,
            unfocusedBorderColor = Surface2,
            cursorColor = AccentTeal
        ),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = TextPrimary)
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Delete?", color = TextPrimary) },
        text = { Text("Remove $name permanently?", color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = AccentRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
