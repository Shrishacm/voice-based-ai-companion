package com.mithra.assistant.data

data class FamilyMember(
    val name: String,
    val relation: String,
    val age: Int,
    val bloodGroup: String,
    val phone: String
)

data class HealthRecord(
    val memberName: String,
    val condition: String,
    val diagnosedDate: String,
    val medications: List<String>,
    val allergies: List<String>,
    val lastCheckup: String,
    val notes: String
)

data class AppointmentRecord(
    val memberName: String,
    val doctor: String,
    val specialty: String,
    val hospital: String,
    val date: String,
    val time: String,
    val notes: String
)

object DummyData {

    val familyMembers = listOf(
        FamilyMember("Raju", "Patient", 72, "B+", "9876543210")
    )

    val healthRecords = listOf(
        HealthRecord(
            memberName = "Raju",
            condition = "Dementia",
            diagnosedDate = "2024-06-15",
            medications = listOf(
                "Aspirin 75mg at 8:00 AM",
                "Aspirin 75mg at 10:00 PM",
                "Pantab before food"
            ),
            allergies = listOf("None known"),
            lastCheckup = "2026-04-10",
            notes = "Patient suffers from dementia. Must take Aspirin twice daily at 8 AM and 10 PM. Pantab to be taken before food. Requires constant supervision and medication reminders."
        )
    )

    val appointments = listOf(
        AppointmentRecord(
            memberName = "Raju",
            doctor = "Dr. Ramesh Bhat",
            specialty = "Neurology",
            hospital = "NIMHANS, Bangalore",
            date = "2026-05-20",
            time = "10:00 AM",
            notes = "Dementia progression review and medication assessment"
        )
    )

    val emergencyInfo = mapOf(
        "Family Blood Bank" to "Indian Red Cross Society: 080-22222222",
        "Ambulance" to "108",
        "Primary Hospital" to "Manipal Hospital, HAL Airport Road - 080-25023344",
        "Family Doctor" to "Dr. Ramesh Bhat (General Physician): 9876543220",
        "Insurance Provider" to "Star Health Insurance, Policy #: SHC/2025/BLR/004521, Valid till Dec 2026",
        "Insurance Emergency" to "Star Health Helpline: 1800-425-2255"
    )

    fun getAllSeedContent(): List<Pair<String, String>> {
        val allContent = mutableListOf<Pair<String, String>>()

        familyMembers.forEach { member ->
            allContent.add(
                "family" to
                "Family member: ${member.name}. Relation: ${member.relation}. Age: ${member.age}. Blood group: ${member.bloodGroup}. Phone: ${member.phone}."
            )
        }

        healthRecords.forEach { record ->
            val meds = record.medications.joinToString(", ")
            val allergies = record.allergies.joinToString(", ")
            allContent.add(
                "health" to
                "${record.memberName} has ${record.condition}. Diagnosed: ${record.diagnosedDate}. Medications: $meds. Allergies: $allergies. Last checkup: ${record.lastCheckup}. Notes: ${record.notes}"
            )
        }

        appointments.forEach { appt ->
            allContent.add(
                "appointment" to
                "Upcoming appointment: ${appt.memberName} with ${appt.doctor} (${appt.specialty}) at ${appt.hospital}. Date: ${appt.date} at ${appt.time}. Notes: ${appt.notes}"
            )
        }

        emergencyInfo.forEach { (key, value) ->
            allContent.add("emergency" to "$key: $value")
        }

        return allContent
    }
}
