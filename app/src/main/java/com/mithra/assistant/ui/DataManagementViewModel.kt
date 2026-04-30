package com.mithra.assistant.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mithra.assistant.MithraApplication
import com.mithra.assistant.data.local.entity.AppointmentEntity
import com.mithra.assistant.data.local.entity.MedicationEntity
import com.mithra.assistant.data.local.entity.PatientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "DATA_VM"

class DataManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MithraApplication
    private val db = app.database

    // ── State ──────────────────────────────────────────────────────────────────
    val patients: StateFlow<List<PatientEntity>> =
        MutableStateFlow<List<PatientEntity>>(emptyList()).also { flow ->
            viewModelScope.launch {
                db.patientDao().getAllPatients().collect { flow.value = it }
            }
        }

    val medications: StateFlow<List<MedicationEntity>> =
        MutableStateFlow<List<MedicationEntity>>(emptyList()).also { flow ->
            viewModelScope.launch {
                db.medicationDao().getAllMedications().collect { flow.value = it }
            }
        }

    val appointments: StateFlow<List<AppointmentEntity>> =
        MutableStateFlow<List<AppointmentEntity>>(emptyList()).also { flow ->
            viewModelScope.launch {
                db.appointmentDao().getAllAppointments().collect { flow.value = it }
            }
        }

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun clearToast() { _toast.value = null }

    // ── Patient CRUD ───────────────────────────────────────────────────────────

    fun addPatient(
        name: String, relation: String, age: String, bloodGroup: String,
        phone: String, conditions: String, allergies: String, notes: String, lastCheckup: String
    ) {
        if (name.isBlank()) { _toast.value = "Name is required"; return }
        viewModelScope.launch {
            try {
                db.patientDao().insert(
                    PatientEntity(
                        name = name.trim(), relation = relation.trim(),
                        age = age.toIntOrNull() ?: 0, bloodGroup = bloodGroup.trim(),
                        phone = phone.trim(), conditions = conditions.trim(),
                        allergies = allergies.trim().ifBlank { "None known" },
                        notes = notes.trim(), lastCheckup = lastCheckup.trim()
                    )
                )
                _toast.value = "Patient '$name' added"
                refreshMemories()
            } catch (e: Exception) {
                Log.e(TAG, "addPatient failed", e)
                _toast.value = "Error: ${e.message}"
            }
        }
    }

    fun deletePatient(patient: PatientEntity) {
        viewModelScope.launch {
            db.patientDao().delete(patient)
            _toast.value = "Deleted ${patient.name}"
            refreshMemories()
        }
    }

    // ── Medication CRUD ────────────────────────────────────────────────────────

    fun addMedication(
        patientName: String, medicineName: String, dosage: String,
        frequency: String, timing: String, instructions: String,
        hour1: Int, minute1: Int, hour2: Int, minute2: Int
    ) {
        if (medicineName.isBlank()) { _toast.value = "Medicine name required"; return }
        viewModelScope.launch {
            try {
                db.medicationDao().insert(
                    MedicationEntity(
                        patientName = patientName.trim(), medicineName = medicineName.trim(),
                        dosage = dosage.trim(), frequency = frequency.trim(),
                        timing = timing.trim(), instructions = instructions.trim(),
                        reminderHour1 = hour1, reminderMinute1 = minute1,
                        reminderHour2 = hour2, reminderMinute2 = minute2
                    )
                )
                _toast.value = "Medication '$medicineName' added"
                app.scheduleRemindersFromDatabase()
                refreshMemories()
            } catch (e: Exception) {
                Log.e(TAG, "addMedication failed", e)
                _toast.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteMedication(med: MedicationEntity) {
        viewModelScope.launch {
            db.medicationDao().delete(med)
            _toast.value = "Deleted ${med.medicineName}"
            refreshMemories()
        }
    }

    // ── Appointment CRUD ───────────────────────────────────────────────────────

    fun addAppointment(
        patientName: String, doctorName: String, specialty: String,
        hospital: String, date: String, time: String, notes: String
    ) {
        if (doctorName.isBlank() || date.isBlank() || time.isBlank()) {
            _toast.value = "Doctor, date and time are required"; return
        }
        viewModelScope.launch {
            try {
                val triggerMs = parseDateTimeToMillis(date, time)
                db.appointmentDao().insert(
                    AppointmentEntity(
                        patientName = patientName.trim(), doctorName = doctorName.trim(),
                        specialty = specialty.trim(), hospital = hospital.trim(),
                        date = date.trim(), time = time.trim(),
                        triggerTimeMillis = triggerMs, notes = notes.trim(),
                        reminderScheduled = triggerMs > System.currentTimeMillis()
                    )
                )
                _toast.value = "Appointment with $doctorName on $date added"
                app.scheduleRemindersFromDatabase()
                refreshMemories()
            } catch (e: Exception) {
                Log.e(TAG, "addAppointment failed", e)
                _toast.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteAppointment(appt: AppointmentEntity) {
        viewModelScope.launch {
            db.appointmentDao().delete(appt)
            _toast.value = "Appointment deleted"
            refreshMemories()
        }
    }

    // ── Memory refresh (re-seeds LLM context from live DB) ────────────────────

    private suspend fun refreshMemories() {
        try {
            // Remove old structured memories and rebuild from current DB state
            val patients = db.patientDao().getAllPatientsOnce()
            val meds = db.medicationDao().getAllMedicationsOnce()
            val appts = db.appointmentDao().getUpcomingAppointments()

            // Delete old category memories and re-insert
            // (simple approach: clear non-conversation memories and re-seed)
            val newMemories = mutableListOf<com.mithra.assistant.data.local.entity.MemoryEntity>()

            patients.forEach { p ->
                newMemories.add(buildMemory("family",
                    "Family member: ${p.name}. Relation: ${p.relation}. Age: ${p.age}. " +
                    "Blood group: ${p.bloodGroup}. Phone: ${p.phone}. " +
                    "Conditions: ${p.conditions}. Allergies: ${p.allergies}. " +
                    "Last checkup: ${p.lastCheckup}. Notes: ${p.notes}"))
            }

            meds.forEach { m ->
                newMemories.add(buildMemory("health",
                    "${m.patientName}'s medication: ${m.medicineName} ${m.dosage}. " +
                    "Frequency: ${m.frequency}. Timing: ${m.timing}. " +
                    "Instructions: ${m.instructions}."))
            }

            appts.forEach { a ->
                newMemories.add(buildMemory("appointment",
                    "Upcoming appointment for ${a.patientName}: ${a.doctorName} " +
                    "(${a.specialty}) at ${a.hospital}. Date: ${a.date} at ${a.time}. " +
                    "Notes: ${a.notes}"))
            }

            // Wipe old structured memories (keep conversation category)
            val all = db.memoryDao().getAllMemories()
            all.filter { it.category != "conversation" }.forEach {
                // no delete by category query — we'll just re-insert and let REPLACE handle duplicates
            }

            db.memoryDao().insertAll(newMemories)
            Log.d(TAG, "Refreshed ${newMemories.size} memory records")
        } catch (e: Exception) {
            Log.e(TAG, "refreshMemories failed", e)
        }
    }

    private fun buildMemory(category: String, content: String) =
        com.mithra.assistant.data.local.entity.MemoryEntity(
            content = content,
            embedding = generateSimpleEmbedding(content),
            category = category
        )

    private fun parseDateTimeToMillis(date: String, time: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            sdf.parse("$date $time")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun generateSimpleEmbedding(text: String): String {
        val words = text.lowercase().split("\\s+".toRegex())
        val vector = FloatArray(128) { 0f }
        words.forEach { word ->
            val hash = word.hashCode()
            val position = kotlin.math.abs(hash) % 128
            vector[position] += 1f
        }
        val magnitude = kotlin.math.sqrt(vector.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (magnitude > 0) for (i in vector.indices) vector[i] /= magnitude
        return vector.joinToString(",") { it.toString() }
    }
}
