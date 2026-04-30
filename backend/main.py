from contextlib import asynccontextmanager
from datetime import datetime, timedelta

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from pydantic import BaseModel

try:
    from .alerts import (
        check_caregiver_alerts,
        create_alert,
        create_reminder,
        get_active_alerts,
        get_all_reminders,
        get_recent_conversations,
        get_upcoming_reminders,
        mark_reminder_sent,
        maybe_create_message_alert,
        resolve_alert,
        start_scheduler,
        stop_scheduler,
    )
    from .llm_handler import get_llm_handler
    from .memory_db import ChatMessage, PatientMemory, get_session, init_db, upsert_patient_memory
except ImportError:
    from alerts import (
        check_caregiver_alerts,
        create_alert,
        create_reminder,
        get_active_alerts,
        get_all_reminders,
        get_recent_conversations,
        get_upcoming_reminders,
        mark_reminder_sent,
        maybe_create_message_alert,
        resolve_alert,
        start_scheduler,
        stop_scheduler,
    )
    from llm_handler import get_llm_handler
    from memory_db import ChatMessage, PatientMemory, get_session, init_db, upsert_patient_memory


class ChatRequest(BaseModel):
    user_id: str
    message: str


class FamilyMemberInput(BaseModel):
    name: str
    relationship: str


class AppointmentInput(BaseModel):
    date: str
    description: str


class PatientMemoryCreate(BaseModel):
    patient_name: str
    age: int
    medications: list[str]
    family_members: list[FamilyMemberInput]
    appointments: list[AppointmentInput]


class ReminderCreate(BaseModel):
    reminder_type: str
    message: str
    scheduled_time: str


class AlertCreate(BaseModel):
    alert_type: str
    message: str
    severity: str = "info"


@asynccontextmanager
async def lifespan(app: FastAPI):
    load_dotenv()
    init_db()
    seed_demo_data()
    start_scheduler()
    yield
    stop_scheduler()


def seed_demo_data():
    session = get_session()
    existing = session.query(PatientMemory).filter(PatientMemory.user_id == "user_001").first()
    if not existing:
        demo_memory = {
            "patient_name": "Raju",
            "age": 72,
            "medications": ["aspirin 8am", "metformin 1pm"],
            "family_members": [
                {"name": "Priya", "relationship": "daughter"},
                {"name": "Arjun", "relationship": "son"}
            ],
            "appointments": [
                {"date": "2026-05-01T10:00:00", "description": "Doctor checkup"}
            ]
        }
        upsert_patient_memory(session, "user_001", demo_memory)

    has_reminders = session.query(PatientMemory).filter(PatientMemory.user_id == "user_001").first()
    session.close()

    if has_reminders:
        existing_reminders = get_all_reminders("user_001")
        if not existing_reminders:
            create_reminder(
                "user_001",
                reminder_type="medication",
                message="Good morning. Please take your blood pressure pill.",
                scheduled_time=datetime.utcnow() + timedelta(minutes=5),
            )
            create_reminder(
                "user_001",
                reminder_type="appointment",
                message="You have a doctor checkup tomorrow at 10am.",
                scheduled_time=datetime.utcnow() + timedelta(hours=2),
            )


app = FastAPI(title="Voice-Based AI Companion API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def root():
    return {"status": "ok", "message": "Voice-Based AI Companion API"}


@app.get("/health")
def health():
    return {"status": "healthy"}


@app.get("/patients")
def list_patients():
    session = get_session()
    patients = session.query(PatientMemory).order_by(PatientMemory.patient_name.asc()).all()
    result = [patient.to_dict() for patient in patients]
    session.close()
    return result


def _get_patient_memory_or_404(user_id: str):
    session = get_session()
    pm = session.query(PatientMemory).filter(PatientMemory.user_id == user_id).first()
    if not pm:
        session.close()
        raise HTTPException(status_code=404, detail="Patient memory not found")
    result = pm.to_dict()
    session.close()
    return result


@app.get("/memory/{user_id}")
def get_memory(user_id: str):
    return _get_patient_memory_or_404(user_id)


@app.get("/patients/{user_id}/memory")
def get_patient_memory_alias(user_id: str):
    return _get_patient_memory_or_404(user_id)


def _save_memory(user_id: str, data: PatientMemoryCreate):
    session = get_session()
    patient = upsert_patient_memory(
        session,
        user_id,
        {
            "patient_name": data.patient_name,
            "age": data.age,
            "medications": data.medications,
            "family_members": [member.model_dump() for member in data.family_members],
            "appointments": [appointment.model_dump() for appointment in data.appointments],
        },
    )
    result = patient.to_dict()
    session.close()
    return result


@app.put("/memory/{user_id}")
def update_memory(user_id: str, data: PatientMemoryCreate):
    return _save_memory(user_id, data)


@app.put("/patients/{user_id}/memory")
def update_memory_alias(user_id: str, data: PatientMemoryCreate):
    return _save_memory(user_id, data)


def _chat_for_user(user_id: str, message: str):
    session = get_session()
    pm = session.query(PatientMemory).filter(PatientMemory.user_id == user_id).first()
    if not pm:
        session.close()
        raise HTTPException(status_code=404, detail="Patient memory not found")

    patient_memory = pm.to_dict()

    history = session.query(ChatMessage).filter(
        ChatMessage.user_id == user_id
    ).order_by(ChatMessage.created_at.desc()).limit(10).all()
    conversation_history = [{"role": m.role, "content": m.content} for m in reversed(history)]

    user_msg = ChatMessage(user_id=user_id, role="user", content=message)
    session.add(user_msg)
    session.commit()

    maybe_create_message_alert(user_id, pm.patient_name, message)

    llm = get_llm_handler()
    response_text = llm.chat(patient_memory, message, conversation_history)

    assistant_msg = ChatMessage(user_id=user_id, role="assistant", content=response_text)
    session.add(assistant_msg)
    session.commit()

    session.close()
    return {"reply": response_text}


@app.post("/chat")
def chat(request: ChatRequest):
    return _chat_for_user(request.user_id, request.message)


@app.post("/patients/{user_id}/chat")
def chat_alias(user_id: str, request: ChatRequest):
    effective_user_id = request.user_id or user_id
    if effective_user_id != user_id:
        raise HTTPException(status_code=400, detail="user_id in body must match URL")
    return _chat_for_user(user_id, request.message)


@app.get("/alerts/{user_id}")
def get_alerts(user_id: str):
    return get_active_alerts(user_id)


@app.get("/patients/{user_id}/alerts")
def get_alerts_alias(user_id: str):
    return get_active_alerts(user_id)


@app.post("/alerts/{user_id}")
def create_alert_endpoint(user_id: str, request: AlertCreate):
    return create_alert(user_id, request.alert_type, request.message, request.severity)


@app.post("/patients/{user_id}/alerts")
def create_alert_alias(user_id: str, request: AlertCreate):
    return create_alert(user_id, request.alert_type, request.message, request.severity)


@app.post("/alerts/{alert_id}/resolve")
def resolve_alert_endpoint(alert_id: int):
    if resolve_alert(alert_id):
        return {"status": "resolved"}
    raise HTTPException(status_code=404, detail="Alert not found")


@app.get("/reminders/{user_id}")
def get_reminders(user_id: str, upcoming_only: bool = False):
    if upcoming_only:
        return get_upcoming_reminders(user_id)
    return get_all_reminders(user_id)


@app.get("/patients/{user_id}/reminders")
def get_reminders_alias(user_id: str, upcoming_only: bool = False):
    if upcoming_only:
        return get_upcoming_reminders(user_id)
    return get_all_reminders(user_id)


@app.post("/reminders/{user_id}")
def create_reminder_endpoint(user_id: str, request: ReminderCreate):
    scheduled_dt = datetime.fromisoformat(request.scheduled_time)
    return create_reminder(user_id, request.reminder_type, request.message, scheduled_dt)


@app.post("/patients/{user_id}/reminders")
def create_reminder_alias(user_id: str, request: ReminderCreate):
    scheduled_dt = datetime.fromisoformat(request.scheduled_time)
    return create_reminder(user_id, request.reminder_type, request.message, scheduled_dt)


@app.post("/reminders/{reminder_id}/sent")
def mark_reminder_sent_endpoint(reminder_id: int):
    if mark_reminder_sent(reminder_id):
        return {"status": "marked"}
    raise HTTPException(status_code=404, detail="Reminder not found")


@app.get("/conversations/{user_id}")
def get_conversations(user_id: str, limit: int = 10):
    return get_recent_conversations(user_id, limit=limit)


@app.get("/patients/{user_id}/check-alerts")
def check_alerts_endpoint(user_id: str):
    return check_caregiver_alerts(user_id)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
