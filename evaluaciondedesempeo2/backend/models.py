from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime, Boolean
from sqlalchemy.orm import relationship
import datetime
from database import Base

class Usuario(Base):
    __tablename__ = "usuarios"
    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    rol = Column(String) # 'ADMIN', 'EMPLOYEE'

    # Campos para verificación por correo
    is_active = Column(Boolean, default=False)
    verification_code = Column(String, nullable=True)

class Evaluacion(Base):
    __tablename__ = "evaluaciones"
    id = Column(Integer, primary_key=True, index=True)
    evaluado_id = Column(Integer, ForeignKey("usuarios.id"))
    evaluador_id = Column(Integer, ForeignKey("usuarios.id"))

    # Nuevos campos requeridos por el frontend
    tipo_evaluacion = Column(String)
    relacion_evaluador = Column(String)

    # Métricas detalladas (0-100)
    obj_cumplimiento = Column(Float, default=0)
    puntualidad_asistencia = Column(Float, default=0)
    calidad_trabajo = Column(Float, default=0)
    productividad = Column(Float, default=0)
    trabajo_equipo = Column(Float, default=0)
    conocimiento_habilidad = Column(Float, default=0)
    comunicacion = Column(Float, default=0)
    adaptabilidad = Column(Float, default=0)
    iniciativa = Column(Float, default=0)
    responsabilidad = Column(Float, default=0)
    actitud_profesional = Column(Float, default=0)
    desarrollo_profesional = Column(Float, default=0)

    puntuacion_promedio = Column(Float)
    comentarios_texto = Column(String)
    fecha = Column(DateTime, default=datetime.datetime.utcnow)

    evaluado = relationship("Usuario", foreign_keys=[evaluado_id])
    evaluador = relationship("Usuario", foreign_keys=[evaluador_id])
