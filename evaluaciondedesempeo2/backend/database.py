import os
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# Configuración de la base de datos
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DB = f"sqlite:///{os.path.join(BASE_DIR, 'evaluacion.db')}"

# Prioriza DATABASE_URL del entorno (.env), si no existe usa SQLite local
SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL", DEFAULT_DB)

connect_args = {"check_same_thread": False} if SQLALCHEMY_DATABASE_URL.startswith("sqlite") else {}

engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
