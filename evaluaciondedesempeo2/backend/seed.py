import os
import random
from dotenv import load_dotenv
from sqlalchemy.orm import Session
from database import SessionLocal, engine, Base
from models import Usuario, Evaluacion
from passlib.context import CryptContext
from datetime import datetime, timedelta

load_dotenv()
pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")

def seed_data():
    db = SessionLocal()
    Base.metadata.create_all(bind=engine)

    print("🌱 Iniciando carga de datos de ejemplo...")

    # 1. Crear Colaboradores (si no existen)
    empleados_data = [
        {"nombre": "Carlos Ruiz", "email": "carlos@insighthr.com", "rol": "EMPLOYEE"},
        {"nombre": "Ana Lopez", "email": "ana@insighthr.com", "rol": "EMPLOYEE"},
        {"nombre": "Beatriz Garcia", "email": "beatriz@insighthr.com", "rol": "EMPLOYEE"},
        {"nombre": "David Vera", "email": "david@insighthr.com", "rol": "EMPLOYEE"},
        {"nombre": "Elena Sanz", "email": "elena@insighthr.com", "rol": "EMPLOYEE"}
    ]

    usuarios_creados = []
    for emp in empleados_data:
        user = db.query(Usuario).filter(Usuario.email == emp["email"]).first()
        if not user:
            user = Usuario(
                nombre=emp["nombre"],
                email=emp["email"],
                hashed_password=pwd_context.hash("demo123"),
                rol=emp["rol"],
                is_active=True
            )
            db.add(user)
            db.flush() # Para obtener el ID
            print(f"✅ Usuario creado: {emp['nombre']}")
        usuarios_creados.append(user)

    db.commit()

    # 2. Crear Evaluaciones de ejemplo
    comentarios = [
        "Excelente liderazgo y gran proactividad en todos los proyectos.",
        "Falta mejorar en los plazos de entrega, suele llegar tarde a las reuniones.",
        "Buen nivel técnico, maneja la herramienta con destreza pero le falta comunicación.",
        "Ha cometido algún error técnico, pero tiene gran proactividad para resolverlos.",
        "Su debilidad es la comunicación con el equipo, aunque es excelente en lo técnico.",
        "Gran capacidad de aprendizaje y proactivo ante los retos del tiempo.",
        "Demuestra gran liderazgo, aunque debe mejorar la gestión de plazos."
    ]

    # Asegurarnos de tener un evaluador (el admin suele ser el ID 1)
    admin = db.query(Usuario).filter(Usuario.rol == "ADMIN").first()
    evaluador_id = admin.id if admin else 1

    for user in usuarios_creados:
        # Verificar si ya tiene evaluaciones para no duplicar masivamente
        count = db.query(Evaluacion).filter(Evaluacion.evaluado_id == user.id).count()
        if count < 2:
            for _ in range(3 - count):
                metricas = {
                    "obj_cumplimiento": random.randint(60, 100),
                    "puntualidad_asistencia": random.randint(50, 95),
                    "calidad_trabajo": random.randint(70, 100),
                    "productividad": random.randint(65, 95),
                    "trabajo_equipo": random.randint(60, 100),
                    "conocimiento_habilidad": random.randint(75, 100),
                    "comunicacion": random.randint(50, 90),
                    "adaptabilidad": random.randint(70, 100),
                    "iniciativa": random.randint(80, 100),
                    "responsabilidad": random.randint(85, 100),
                    "actitud_profesional": random.randint(80, 100),
                    "desarrollo_profesional": random.randint(70, 100),
                }

                promedio = sum(metricas.values()) / len(metricas)

                ev = Evaluacion(
                    evaluado_id=user.id,
                    evaluador_id=evaluador_id,
                    tipo_evaluacion="Auditoría de Semilla",
                    relacion_evaluador="Administración",
                    comentarios_texto=random.choice(comentarios),
                    puntuacion_promedio=round(promedio, 1),
                    fecha=datetime.utcnow() - timedelta(days=random.randint(1, 30)),
                    **metricas
                )
                db.add(ev)
            print(f"📊 Evaluaciones creadas para: {user.nombre}")

    db.commit()
    db.close()
    print("🚀 Seed completado exitosamente.")

if __name__ == "__main__":
    seed_data()
