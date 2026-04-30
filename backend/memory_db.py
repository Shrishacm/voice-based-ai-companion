import json
from datetime import datetime
from pathlib import Path

from sqlalchemy import Column, DateTime, Integer, String, Text, create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

Base = declarative_base()


class PatientMemory(Base):
    __tablename__ = "patient_memories"

    user_id = Column(String, primary_key=True)
    patient_name = Column(String, nullable=False)
    age = Column(Integer, nullable=False)
    medications = Column(Text, nullable=False)
    family_members = Column(Text, nullable=False)
    appointments = Column(Text, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self):
        family_members = json.loads(self.family_members)
        return {
            "user_id": self.user_id,
            "patient_name": self.patient_name,
            "age": self.age,
            "medications": json.loads(self.medications),
            "family_members": family_members,
            "family_names": [member["name"] for member in family_members],
            "appointments": json.loads(self.appointments),
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    @classmethod
    def from_dict(cls, data):
        return cls(
            user_id=data["user_id"],
            patient_name=data["patient_name"],
            age=data["age"],
            medications=json.dumps(data["medications"]),
            family_members=json.dumps(data["family_members"]),
            appointments=json.dumps(data["appointments"]),
        )


class Reminder(Base):
    __tablename__ = "reminders"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String, nullable=False)
    reminder_type = Column(String, nullable=False)
    message = Column(String, nullable=False)
    scheduled_time = Column(DateTime, nullable=False)
    sent = Column(Integer, default=0)
    created_at = Column(DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            "id": self.id,
            "user_id": self.user_id,
            "reminder_type": self.reminder_type,
            "message": self.message,
            "scheduled_time": self.scheduled_time.isoformat() if self.scheduled_time else None,
            "sent": bool(self.sent),
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


class Alert(Base):
    __tablename__ = "alerts"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String, nullable=False)
    alert_type = Column(String, nullable=False)
    message = Column(String, nullable=False)
    severity = Column(String, default="info")
    created_at = Column(DateTime, default=datetime.utcnow)
    resolved = Column(Integer, default=0)

    def to_dict(self):
        return {
            "id": self.id,
            "user_id": self.user_id,
            "alert_type": self.alert_type,
            "message": self.message,
            "severity": self.severity,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "resolved": bool(self.resolved),
        }


class ChatMessage(Base):
    __tablename__ = "chat_messages"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String, nullable=False)
    role = Column(String, nullable=False)
    content = Column(Text, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            "id": self.id,
            "user_id": self.user_id,
            "role": self.role,
            "content": self.content,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


ENGINE = None
SessionLocal = None


def init_db(db_path=None):
    global ENGINE, SessionLocal
    if db_path is None:
        db_path = Path(__file__).resolve().parent / "memory.db"
    ENGINE = create_engine(f"sqlite:///{db_path}", connect_args={"check_same_thread": False})
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=ENGINE)
    Base.metadata.create_all(bind=ENGINE)
    return ENGINE, SessionLocal


def get_session():
    if SessionLocal is None:
        init_db()
    return SessionLocal()


def upsert_patient_memory(session, user_id: str, data: dict) -> PatientMemory:
    patient_memory = session.query(PatientMemory).filter(PatientMemory.user_id == user_id).first()
    if patient_memory is None:
        patient_memory = PatientMemory(
            user_id=user_id,
            patient_name=data["patient_name"],
            age=data["age"],
            medications=json.dumps(data["medications"]),
            family_members=json.dumps(data["family_members"]),
            appointments=json.dumps(data["appointments"]),
        )
        session.add(patient_memory)
    else:
        patient_memory.patient_name = data["patient_name"]
        patient_memory.age = data["age"]
        patient_memory.medications = json.dumps(data["medications"])
        patient_memory.family_members = json.dumps(data["family_members"])
        patient_memory.appointments = json.dumps(data["appointments"])
        patient_memory.updated_at = datetime.utcnow()
    session.commit()
    session.refresh(patient_memory)
    return patient_memory
