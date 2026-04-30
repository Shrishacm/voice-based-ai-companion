package com.mithra.assistant.data

import android.content.Context
import android.util.Log
import com.mithra.assistant.data.local.AppDatabase
import com.mithra.assistant.data.local.entity.*
import kotlinx.coroutines.runBlocking
import java.util.Calendar

private const val TAG = "VOICE_SEEDER"

class DatabaseSeeder(private val context: Context, private val database: AppDatabase) {

    private val SEED_VERSION_KEY = "seed_version"
    private val CURRENT_SEED_VERSION = 4 // bumped from 3 → 4 for new table structure

    fun seedIfNeeded() {
        val prefs = context.getSharedPreferences("mithra_prefs", Context.MODE_PRIVATE)
        val currentVersion = prefs.getInt(SEED_VERSION_KEY, 0)

        if (currentVersion < CURRENT_SEED_VERSION) {
            Log.d(TAG, "Resetting database from version $currentVersion to $CURRENT_SEED_VERSION")
            runBlocking {
                try {
                    database.clearAllTables()
                    Log.d(TAG, "All tables cleared")
                    seedStructuredData()
                    seedMemories()
                    prefs.edit().putInt(SEED_VERSION_KEY, CURRENT_SEED_VERSION).apply()
                    Log.d(TAG, "Database seeded with Raju's full data")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to seed database", e)
                }
            }
        }
    }

    // ─── Structured tables ──────────────────────────────────────────────────────

    private suspend fun seedStructuredData() {
        // Patient
        database.patientDao().insert(
            PatientEntity(
                name = "Raju",
                relation = "Patient",
                age = 72,
                bloodGroup = "B+",
                phone = "9876543210",
                conditions = "Dementia",
                allergies = "None known",
                notes = "Requires constant supervision and medication reminders. Dementia diagnosed June 2024.",
                lastCheckup = "2026-04-10"
            )
        )
        Log.d(TAG, "Seeded 1 patient")

        // Medications
        database.medicationDao().insert(
            MedicationEntity(
                patientName = "Raju",
                medicineName = "Aspirin",
                dosage = "75mg",
                frequency = "Twice daily",
                timing = "8:00 AM, 10:00 PM",
                instructions = "Take with water",
                reminderHour1 = 8,
                reminderMinute1 = 0,
                reminderHour2 = 22,
                reminderMinute2 = 0
            )
        )
        database.medicationDao().insert(
            MedicationEntity(
                patientName = "Raju",
                medicineName = "Pantab",
                dosage = "40mg",
                frequency = "Once daily",
                timing = "7:30 AM",
                instructions = "Before food",
                reminderHour1 = 7,
                reminderMinute1 = 30
            )
        )
        Log.d(TAG, "Seeded 2 medications")

        // Appointment — May 20, 2026 at 10:00 AM
        val apptCal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 20, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        database.appointmentDao().insert(
            AppointmentEntity(
                patientName = "Raju",
                doctorName = "Dr. Ramesh Bhat",
                specialty = "Neurology",
                hospital = "NIMHANS, Bangalore",
                date = "2026-05-20",
                time = "10:00 AM",
                triggerTimeMillis = apptCal.timeInMillis,
                notes = "Dementia progression review and medication assessment",
                reminderScheduled = true
            )
        )
        Log.d(TAG, "Seeded 1 appointment")
    }

    // ─── Memory / embedding table for LLM context retrieval ─────────────────────

    private suspend fun seedMemories() {
        val contents = mutableListOf<Pair<String, String>>()

        // Patient info
        contents.add("family" to
            "Family member: Raju. Relation: Patient. Age: 72. Blood group: B+. Phone: 9876543210.")

        // Health
        contents.add("health" to
            "Raju has Dementia. Diagnosed: 2024-06-15. Medications: Aspirin 75mg at 8:00 AM and 10:00 PM daily, Pantab 40mg before food at 7:30 AM. Allergies: None known. Last checkup: 2026-04-10. Notes: Requires constant supervision and medication reminders.")

        // Appointment
        contents.add("appointment" to
            "Upcoming appointment: Raju with Dr. Ramesh Bhat (Neurology) at NIMHANS, Bangalore. Date: 2026-05-20 at 10:00 AM. Notes: Dementia progression review and medication assessment.")

        // Emergency
        contents.add("emergency" to "Ambulance: 108")
        contents.add("emergency" to "Primary Hospital: Manipal Hospital, HAL Airport Road - 080-25023344")
        contents.add("emergency" to "Family Doctor: Dr. Ramesh Bhat (General Physician): 9876543220")
        contents.add("emergency" to "Insurance Provider: Star Health Insurance, Policy #: SHC/2025/BLR/004521, Valid till Dec 2026")
        contents.add("emergency" to "Star Health Helpline: 1800-425-2255")
        contents.add("emergency" to "Family Blood Bank: Indian Red Cross Society: 080-22222222")

        contents.forEach { (category, content) ->
            database.memoryDao().insert(
                MemoryEntity(
                    content = content,
                    embedding = generateSimpleEmbedding(content),
                    category = category
                )
            )
        }
        Log.d(TAG, "Seeded ${contents.size} memory records")
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
        if (magnitude > 0) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
        return vector.joinToString(",") { it.toString() }
    }
}
