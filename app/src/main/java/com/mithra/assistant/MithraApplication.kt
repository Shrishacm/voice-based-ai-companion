package com.mithra.assistant

import android.app.Application
import android.util.Log
import com.mithra.assistant.data.DatabaseSeeder
import com.mithra.assistant.data.local.AppDatabase
import com.mithra.assistant.network.MemoryRetriever
import com.mithra.assistant.service.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "VOICE_BOOT"

class MithraApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val memoryRetriever: MemoryRetriever by lazy {
        MemoryRetriever(database.memoryDao())
    }

    val seeder: DatabaseSeeder by lazy {
        DatabaseSeeder(this, database)
    }

    val reminderManager: ReminderManager by lazy {
        ReminderManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MithraAssistant initializing")
        seeder.seedIfNeeded()
        scheduleRemindersFromDatabase()
    }

    /**
     * Reads medications and appointments from SQLite and schedules their alarms.
     * Called on every launch so alarms are restored after device restart.
     */
    fun scheduleRemindersFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ── Medications ────────────────────────────────────────────────
                val medications = database.medicationDao().getAllMedicationsOnce()
                medications.filter { it.isActive }.forEachIndexed { index, med ->
                    val baseId = (med.id * 10).toLong()
                    if (med.reminderHour1 >= 0) {
                        reminderManager.scheduleMedicineReminder(
                            id = baseId,
                            title = "${med.medicineName} ${med.dosage} – ${med.instructions.ifBlank { med.timing }}",
                            hour = med.reminderHour1,
                            minute = med.reminderMinute1,
                            isRecurring = true
                        )
                    }
                    if (med.reminderHour2 >= 0) {
                        reminderManager.scheduleMedicineReminder(
                            id = baseId + 1,
                            title = "${med.medicineName} ${med.dosage} (evening)",
                            hour = med.reminderHour2,
                            minute = med.reminderMinute2,
                            isRecurring = true
                        )
                    }
                }
                Log.d(TAG, "Scheduled reminders for ${medications.size} medications")

                // ── Appointments ───────────────────────────────────────────────
                val appointments = database.appointmentDao().getUpcomingAppointments()
                appointments.forEach { appt ->
                    val reminderId = 5000L + appt.id
                    // Remind 1 hour before
                    val reminderAt = appt.triggerTimeMillis - 60 * 60 * 1000L
                    if (reminderAt > System.currentTimeMillis()) {
                        reminderManager.scheduleAppointmentReminder(
                            id = reminderId,
                            title = "Appointment with ${appt.doctorName} at ${appt.hospital} – ${appt.time}",
                            triggerTimeMillis = reminderAt
                        )
                    }
                }
                Log.d(TAG, "Scheduled reminders for ${appointments.size} appointments")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule reminders from DB", e)
            }
        }
    }
}
