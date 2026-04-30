import os
import smtplib
from datetime import datetime, timedelta
from email.message import EmailMessage

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

try:
    from .memory_db import Alert, ChatMessage, PatientMemory, Reminder, get_session
except ImportError:
    from memory_db import Alert, ChatMessage, PatientMemory, Reminder, get_session


CONFUSION_KEYWORDS = (
    "i don't remember",
    "where am i",
    "i am lost",
    "i'm lost",
    "who are you",
    "i am confused",
)

SCHEDULER = None


def check_caregiver_alerts(user_id: str) -> list:
    session = get_session()
    alerts = []

    pending_reminders = session.query(Reminder).filter(
        Reminder.user_id == user_id,
        Reminder.sent == 0,
        Reminder.scheduled_time <= datetime.utcnow()
    ).all()

    for reminder in pending_reminders:
        alerts.append({
            "type": "missed_reminder",
            "message": f"Medication reminder pending: {reminder.message}",
            "severity": "warning"
        })

    recent_alerts = session.query(Alert).filter(
        Alert.user_id == user_id,
        Alert.resolved == 0,
        Alert.created_at >= datetime.utcnow() - timedelta(hours=24)
    ).all()

    for alert in recent_alerts:
        alerts.append({
            "type": alert.alert_type,
            "message": alert.message,
            "severity": alert.severity
        })

    session.close()
    return alerts


def send_email_notification(subject: str, body: str) -> bool:
    smtp_host = os.getenv("SMTP_HOST")
    smtp_port = os.getenv("SMTP_PORT")
    smtp_username = os.getenv("SMTP_USERNAME")
    smtp_password = os.getenv("SMTP_PASSWORD")
    smtp_to = os.getenv("CAREGIVER_EMAIL")
    smtp_from = os.getenv("SMTP_FROM", smtp_username or "")

    if not all([smtp_host, smtp_port, smtp_to, smtp_from]):
        return False

    message = EmailMessage()
    message["Subject"] = subject
    message["From"] = smtp_from
    message["To"] = smtp_to
    message.set_content(body)

    try:
        with smtplib.SMTP(smtp_host, int(smtp_port), timeout=20) as smtp:
            if os.getenv("SMTP_USE_TLS", "true").lower() == "true":
                smtp.starttls()
            if smtp_username and smtp_password:
                smtp.login(smtp_username, smtp_password)
            smtp.send_message(message)
        return True
    except Exception:
        return False


def create_alert(user_id: str, alert_type: str, message: str, severity: str = "info", deliver: bool = True) -> dict:
    session = get_session()
    alert = Alert(
        user_id=user_id,
        alert_type=alert_type,
        message=message,
        severity=severity,
    )
    session.add(alert)
    session.commit()
    alert_dict = alert.to_dict()
    patient = session.query(PatientMemory).filter(PatientMemory.user_id == user_id).first()
    patient_name = patient.patient_name if patient else user_id
    session.close()

    if deliver:
        send_email_notification(
            subject=f"Caregiver alert for {patient_name}",
            body=f"Alert type: {alert_type}\nSeverity: {severity}\nMessage: {message}",
        )

    return alert_dict


def get_active_alerts(user_id: str) -> list:
    session = get_session()
    alerts = session.query(Alert).filter(
        Alert.user_id == user_id,
        Alert.resolved == 0
    ).order_by(Alert.created_at.desc()).all()
    result = [a.to_dict() for a in alerts]
    session.close()
    return result


def resolve_alert(alert_id: int) -> bool:
    session = get_session()
    alert = session.query(Alert).filter(Alert.id == alert_id).first()
    if alert:
        alert.resolved = 1
        session.commit()
        session.close()
        return True
    session.close()
    return False


def get_upcoming_reminders(user_id: str) -> list:
    session = get_session()
    reminders = session.query(Reminder).filter(
        Reminder.user_id == user_id,
        Reminder.sent == 0,
        Reminder.scheduled_time > datetime.utcnow()
    ).order_by(Reminder.scheduled_time.asc()).all()
    result = [r.to_dict() for r in reminders]
    session.close()
    return result


def create_reminder(user_id: str, reminder_type: str, message: str, scheduled_time: datetime) -> dict:
    session = get_session()
    reminder = Reminder(
        user_id=user_id,
        reminder_type=reminder_type,
        message=message,
        scheduled_time=scheduled_time,
    )
    session.add(reminder)
    session.commit()
    reminder_dict = reminder.to_dict()
    session.close()
    return reminder_dict


def mark_reminder_sent(reminder_id: int) -> bool:
    session = get_session()
    reminder = session.query(Reminder).filter(Reminder.id == reminder_id).first()
    if reminder:
        reminder.sent = 1
        session.commit()
        session.close()
        return True
    session.close()
    return False


def get_all_reminders(user_id: str) -> list:
    session = get_session()
    reminders = session.query(Reminder).filter(
        Reminder.user_id == user_id
    ).order_by(Reminder.scheduled_time.desc()).all()
    result = [r.to_dict() for r in reminders]
    session.close()
    return result


def get_recent_conversations(user_id: str, limit: int = 10) -> list:
    session = get_session()
    messages = session.query(ChatMessage).filter(
        ChatMessage.user_id == user_id
    ).order_by(ChatMessage.created_at.desc()).limit(limit).all()
    result = [message.to_dict() for message in reversed(messages)]
    session.close()
    return result


def maybe_create_message_alert(user_id: str, patient_name: str, message: str) -> dict | None:
    lowered = message.lower().strip()
    if lowered == "[patient_silent]":
        return create_alert(
            user_id,
            alert_type="patient_silent",
            message=f"{patient_name} has been silent or hard to understand for repeated turns.",
            severity="warning",
        )

    if any(keyword in lowered for keyword in CONFUSION_KEYWORDS):
        return create_alert(
            user_id,
            alert_type="confusion",
            message=f"{patient_name} sounded confused and said: {message}",
            severity="high",
        )
    return None


def process_due_reminders() -> int:
    session = get_session()
    now = datetime.utcnow()
    reminders = session.query(Reminder).filter(
        Reminder.sent == 0,
        Reminder.scheduled_time <= now,
    ).order_by(Reminder.scheduled_time.asc()).all()

    processed = 0
    for reminder in reminders:
        reminder.sent = 1
        session.add(
            Alert(
                user_id=reminder.user_id,
                alert_type="reminder_due",
                message=reminder.message,
                severity="info",
            )
        )
        patient = session.query(PatientMemory).filter(PatientMemory.user_id == reminder.user_id).first()
        patient_name = patient.patient_name if patient else reminder.user_id
        send_email_notification(
            subject=f"Reminder triggered for {patient_name}",
            body=f"Reminder type: {reminder.reminder_type}\nScheduled: {reminder.scheduled_time.isoformat()}\nMessage: {reminder.message}",
        )
        processed += 1

    session.commit()
    session.close()
    return processed


def create_morning_reminder() -> None:
    session = get_session()
    patient = session.query(PatientMemory).filter(PatientMemory.user_id == "user_001").first()

    if patient:
        import json
        meds = json.loads(patient.medications)
        if meds:
            med_list = ", ".join([str(m) for m in meds if m])
            message = f"Good morning {patient.patient_name}! Time to take {med_list}."
        else:
            message = f"Good morning {patient.patient_name}! Time to take your medications."
    else:
        message = "Good morning! Time to take your medications."

    now = datetime.utcnow()
    reminder_time = now.replace(hour=9, minute=0, second=0, microsecond=0)
    if now.hour >= 9:
        reminder_time = reminder_time + timedelta(days=1)

    reminder = Reminder(
        user_id="user_001",
        reminder_type="morning_medication",
        message=message,
        scheduled_time=reminder_time,
    )
    session.add(reminder)
    session.commit()
    session.close()


def start_scheduler() -> None:
    global SCHEDULER
    if SCHEDULER is not None:
        return

    timezone = os.getenv("APP_TIMEZONE", "UTC")
    SCHEDULER = BackgroundScheduler(timezone=timezone)
    SCHEDULER.add_job(
        process_due_reminders,
        trigger="interval",
        minutes=1,
        id="due-reminder-scan",
        replace_existing=True,
    )
    SCHEDULER.add_job(
        create_morning_reminder,
        trigger=CronTrigger(hour=9, minute=0),
        id="morning-reminder-9am",
        replace_existing=True,
    )
    SCHEDULER.start()


def stop_scheduler() -> None:
    global SCHEDULER
    if SCHEDULER is not None:
        SCHEDULER.shutdown(wait=False)
        SCHEDULER = None
