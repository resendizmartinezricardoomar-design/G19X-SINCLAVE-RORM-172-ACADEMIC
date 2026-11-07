import os
from dotenv import load_dotenv

# Cargar variables de entorno al inicio
load_dotenv()

SECRET_KEY = os.getenv("SECRET_KEY")
if not SECRET_KEY:
    raise RuntimeError("Falta SECRET_KEY en el .env (ver .env.example)")

from fastapi import FastAPI, Depends, HTTPException, Body
from sqlalchemy.orm import Session
from sqlalchemy import func
from database import engine, Base, get_db, SessionLocal
from models import Usuario, Evaluacion
from pydantic import BaseModel, EmailStr, Field
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from passlib.context import CryptContext
from jose import JWTError, jwt
from datetime import datetime, timedelta
from fastapi.security import OAuth2PasswordBearer
import uvicorn, random, logging, io, json, urllib.request, secrets
from ai_engine import ai_engine
from fpdf import FPDF

# Configuración de Seguridad
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 600
pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="api/login")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

Base.metadata.create_all(bind=engine)

# Constantes para el reporte
METRIC_LABELS = {
    "obj_cumplimiento": "Objetivos", "puntualidad_asistencia": "Puntualidad",
    "calidad_trabajo": "Calidad", "productividad": "Productividad",
    "trabajo_equipo": "Equipo", "conocimiento_habilidad": "Habilidad",
    "comunicacion": "Comunicación", "adaptabilidad": "Adaptabilidad",
    "iniciativa": "Iniciativa", "responsabilidad": "Responsabilidad",
    "actitud_profesional": "Actitud", "desarrollo_profesional": "Desarrollo"
}

# --- CONFIGURACIÓN EMAILJS ---
EMAILJS_SERVICE_ID = os.getenv("EMAILJS_SERVICE_ID")
EMAILJS_TEMPLATE_ID = os.getenv("EMAILJS_TEMPLATE_ID")
EMAILJS_PUBLIC_KEY = os.getenv("EMAILJS_PUBLIC_KEY")
EMAILJS_PRIVATE_KEY = os.getenv("EMAILJS_PRIVATE_KEY")

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

async def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    credentials_exception = HTTPException(
        status_code=401,
        detail="No se pudo validar las credenciales",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    user = db.query(Usuario).filter(Usuario.email == email).first()
    if user is None:
        raise credentials_exception
    return user

def requiere_admin(user: Usuario):
    if user.rol != "ADMIN":
        raise HTTPException(status_code=403, detail="Acceso denegado: Se requiere rol de Administrador")

def enviar_correo_emailjs(email_destino, codigo):
    url = "https://api.emailjs.com/api/v1.0/email/send"
    data = {
        "service_id": EMAILJS_SERVICE_ID,
        "template_id": EMAILJS_TEMPLATE_ID,
        "user_id": EMAILJS_PUBLIC_KEY,
        "accessToken": EMAILJS_PRIVATE_KEY,
        "template_params": {
            "to_email": email_destino,
            "codigo": codigo
        }
    }
    try:
        payload = json.dumps(data).encode('utf-8')
        req = urllib.request.Request(url, data=payload, headers={
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0'
        })
        # Aumentamos el timeout para evitar cortes
        with urllib.request.urlopen(req, timeout=20) as response:
            res_body = response.read().decode('utf-8')
            if response.status == 200:
                logger.info(f"✅ EmailJS OK: {res_body}")
                return True
            else:
                logger.error(f"❌ EmailJS Error {response.status}: {res_body}")
                return False
    except Exception as e:
        logger.error(f"❌ Fallo crítico al enviar correo: {str(e)}")
        # Siempre mostramos el código en consola como plan B
        print(f"\n🔑 [CÓDIGO GENERADO] Para {email_destino}: {codigo}\n")
        return False

# --- ACTUALIZACIÓN DE SEGURIDAD PARA EL ADMIN ---
def create_default_admin():
    email = os.getenv("ADMIN_EMAIL")
    password = os.getenv("ADMIN_PASSWORD")
    if not email or not password:
        logger.warning("Sin ADMIN_EMAIL/ADMIN_PASSWORD en el .env: no se crea administrador")
        return

    db = SessionLocal()
    try:
        admin = db.query(Usuario).filter(Usuario.email == email).first()
        if not admin:
            hashed = pwd_context.hash(password)
            new_admin = Usuario(
                nombre="Administrador Maestro", email=email,
                hashed_password=hashed, rol="ADMIN", is_active=True
            )
            db.add(new_admin); db.commit()
            print("\n⭐ ADMIN CREADO CON ÉXITO\n")
        else:
            if not admin.is_active:
                admin.is_active = True; db.commit()
            if not str(admin.hashed_password).startswith("$pbkdf2-sha256$"):
                admin.hashed_password = pwd_context.hash(password)
                db.commit()
                print("\n🔐 SEGURIDAD DE ADMIN ACTUALIZADA A PBKDF2\n")
    finally: db.close()

create_default_admin()

# --- MODELOS ---
class LoginRequest(BaseModel):
    email: str
    password: str

class UserCreate(BaseModel):
    nombre: str
    email: EmailStr
    password: str

class VerifyRequest(BaseModel):
    email: str
    codigo: str

Indicador = Field(ge=0, le=100)

class EvaluacionCreate(BaseModel):
    evaluado_id: int
    tipo_evaluacion: str
    relacion_evaluador: str
    obj_cumplimiento: float = Indicador
    puntualidad_asistencia: float = Indicador
    calidad_trabajo: float = Indicador
    productividad: float = Indicador
    trabajo_equipo: float = Indicador
    conocimiento_habilidad: float = Indicador
    comunicacion: float = Indicador
    adaptabilidad: float = Indicador
    iniciativa: float = Indicador
    responsabilidad: float = Indicador
    actitud_profesional: float = Indicador
    desarrollo_profesional: float = Indicador
    comentarios_texto: str = ""

app = FastAPI(title="InsightHR Unified Master API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# --- RUTAS DE USUARIO ---

@app.post("/api/login")
async def login(req: LoginRequest, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    u = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if u and pwd_context.verify(req.password, u.hashed_password):
        if not u.is_active: raise HTTPException(status_code=403, detail="Cuenta no activa")
        token = create_access_token({"sub": u.email, "rol": u.rol, "id": u.id})
        return {
            "access_token": token,
            "user": {"id": u.id, "nombre": u.nombre, "rol": u.rol, "email": u.email}
        }
    raise HTTPException(status_code=401, detail="Credenciales incorrectas")

@app.post("/api/usuarios")
async def registrar_u(req: UserCreate, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    v = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if v and not v.is_active:
        db.delete(v); db.commit()
    elif v: raise HTTPException(status_code=400, detail="El correo ya existe")

    codigo_v = str(secrets.randbelow(900000) + 100000)
    hashed = pwd_context.hash(req.password)
    u = Usuario(
        nombre=req.nombre.strip(),
        email=email_clean,
        hashed_password=hashed,
        rol="EMPLOYEE",
        is_active=False,
        verification_code=codigo_v,
        verification_expires=datetime.utcnow() + timedelta(minutes=15),
        verification_attempts=0
    )
    db.add(u); db.commit()
    enviar_correo_emailjs(email_clean, codigo_v)
    return {"status": "success"}

@app.post("/api/verificar")
async def verificar_cuenta(req: VerifyRequest, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    u = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if not u: raise HTTPException(status_code=404)

    if (u.verification_attempts or 0) >= 5 or not u.verification_expires or u.verification_expires < datetime.utcnow():
        raise HTTPException(status_code=400, detail="Código vencido o demasiados intentos; regístrate de nuevo")

    if u.verification_code == req.codigo.strip():
        u.is_active = True
        u.verification_code = None
        u.verification_expires = None
        u.verification_attempts = 0
        db.commit()
        return {"status": "verified"}
    else:
        u.verification_attempts = (u.verification_attempts or 0) + 1
        db.commit()
        raise HTTPException(status_code=400, detail="Código incorrecto")

@app.get("/api/empleados")
def get_empleados(db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    is_admin = current_user.rol == "ADMIN"
    usuarios = db.query(Usuario).all()
    res = []
    for u in usuarios:
        if is_admin or u.id == current_user.id:
            avg = db.query(func.avg(Evaluacion.puntuacion_promedio)).filter(Evaluacion.evaluado_id == u.id).scalar()
            res.append({"id": u.id, "nombre": u.nombre, "rol": u.rol, "email": u.email, "score": round(float(avg or 0.0), 1)})
        else:
            # Para empleados viendo a otros: ocultar email y score
            res.append({"id": u.id, "nombre": u.nombre, "rol": u.rol, "email": None, "score": 0.0})
    return res

@app.delete("/api/usuarios/{user_id}")
async def eliminar_u(user_id: int, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    requiere_admin(current_user)
    u = db.query(Usuario).filter(Usuario.id == user_id).first()
    if u:
        db.query(Evaluacion).filter(Evaluacion.evaluado_id == user_id).delete()
        db.delete(u); db.commit()
    return {"status": "ok"}

# --- RUTAS DE EVALUACIÓN ---

@app.get("/api/evaluaciones")
def listar_evaluaciones(db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    requiere_admin(current_user)
    evs = db.query(Evaluacion).order_by(Evaluacion.fecha.desc()).all()
    return [{
        "fecha": e.fecha.strftime("%Y-%m-%d") if e.fecha else "",
        "evaluado_id": e.evaluado_id,
        "evaluado_nombre": e.evaluado.nombre if e.evaluado else "Desconocido",
        "puntuacion_promedio": e.puntuacion_promedio
    } for e in evs]

@app.post("/api/evaluaciones")
async def crear_ev(data: EvaluacionCreate, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    requiere_admin(current_user)
    if data.evaluado_id == current_user.id:
        raise HTTPException(status_code=400, detail="No puedes evaluarte a ti mismo")

    valores = data.model_dump()
    db_ev = Evaluacion(**valores, evaluador_id=current_user.id)
    indicadores = [
        data.obj_cumplimiento, data.puntualidad_asistencia, data.calidad_trabajo,
        data.productividad, data.trabajo_equipo, data.conocimiento_habilidad,
        data.comunicacion, data.adaptabilidad, data.iniciativa,
        data.responsabilidad, data.actitud_profesional, data.desarrollo_profesional
    ]
    db_ev.puntuacion_promedio = round(sum(indicadores) / len(indicadores), 1)
    db.add(db_ev); db.commit()
    return {"status": "ok"}

def calcular_resumen(uid: int, db: Session):
    metrics = list(METRIC_LABELS.keys())
    labels = ["Obj", "Punt", "Calid", "Prod", "Equip", "Skill", "Comu", "Adap", "Inic", "Resp", "Actit", "Desa"]
    scores = []

    total_evals = db.query(Evaluacion).filter(Evaluacion.evaluado_id == uid).count()

    for m in metrics:
        val = db.query(func.avg(getattr(Evaluacion, m))).filter(Evaluacion.evaluado_id == uid).scalar()
        scores.append(round(float(val or 0.0), 1))

    last_ev = db.query(Evaluacion).filter(Evaluacion.evaluado_id == uid).order_by(Evaluacion.fecha.desc()).first()
    analisis_ia = None
    if last_ev:
        analisis_ia = ai_engine.analizar_comentarios(last_ev.comentarios_texto or "Sin comentarios", last_ev.puntuacion_promedio or 0.0)

    avg_total = round(sum(scores) / len(scores), 1) if scores else 0.0

    kpis = {
        "Eficiencia Operativa": round(sum(scores[0:4]) / 4, 1) if scores else 0.0,
        "Competencias Blandas": round(sum(scores[4:8]) / 4, 1) if scores else 0.0,
        "Potencial y Crecimiento": round(sum(scores[8:12]) / 4, 1) if scores else 0.0
    }

    return {
        "labels": labels,
        "scores": scores,
        "analisis_ia": analisis_ia,
        "kpis": kpis,
        "total_evals": total_evals,
        "avg_total": avg_total
    }

@app.get("/api/evaluaciones/resumen/{uid}")
def resumen(uid: int, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    if current_user.rol != "ADMIN" and current_user.id != uid:
        raise HTTPException(status_code=403, detail="No autorizado para ver este resumen")
    return calcular_resumen(uid, db)

@app.get("/api/reporte/{usuario_id}")
async def download_report(usuario_id: int, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    if current_user.rol != "ADMIN" and current_user.id != usuario_id:
        raise HTTPException(status_code=403, detail="No autorizado para descargar este reporte")

    u = db.query(Usuario).filter(Usuario.id == usuario_id).first()
    if not u: raise HTTPException(status_code=404)

    data = calcular_resumen(u.id, db)

    pdf = FPDF()
    pdf.add_page()

    # Encabezado
    pdf.set_font("Arial", "B", 18)
    pdf.set_text_color(30, 58, 138) # Indigo
    pdf.cell(190, 15, "INSIGHTHR - REPORTE ESTRATÉGICO", ln=True, align="C")
    pdf.set_font("Arial", "", 10)
    pdf.set_text_color(100, 100, 100)
    pdf.cell(190, 5, f"Generado el: {datetime.now().strftime('%Y-%m-%d %H:%M')}", ln=True, align="C")
    pdf.ln(10)

    # Información General
    pdf.set_fill_color(248, 250, 252)
    pdf.set_text_color(0, 0, 0)
    pdf.set_font("Arial", "B", 12)
    pdf.cell(190, 10, f" Colaborador: {u.nombre}", ln=True, fill=True)
    pdf.set_font("Arial", "", 11)
    pdf.cell(190, 8, f" Email: {u.email}", ln=True, fill=True)
    pdf.cell(190, 8, f" Rol: {u.rol}", ln=True, fill=True)
    pdf.cell(190, 8, f" Evaluaciones promediadas: {data['total_evals']}", ln=True, fill=True)
    pdf.ln(10)

    # Score Global
    pdf.set_fill_color(99, 102, 241)
    pdf.set_text_color(255, 255, 255)
    pdf.set_font("Arial", "B", 14)
    pdf.cell(190, 15, f" SCORE GLOBAL: {data['avg_total']} / 100", ln=True, align="C", fill=True)
    pdf.ln(10)

    # KPIs
    pdf.set_text_color(30, 58, 138)
    pdf.set_font("Arial", "B", 12)
    pdf.cell(190, 10, "KPIs ESTRATÉGICOS", ln=True)
    pdf.set_text_color(0, 0, 0)
    pdf.set_font("Arial", "", 11)
    for kpi, valor in data["kpis"].items():
        pdf.cell(130, 8, f" - {kpi}:", border="B")
        pdf.cell(60, 8, f"{valor} / 100", border="B", ln=True, align="R")
    pdf.ln(10)

    # Tabla de Indicadores
    pdf.set_text_color(30, 58, 138)
    pdf.set_font("Arial", "B", 12)
    pdf.cell(190, 10, "DETALLE DE INDICADORES (PROMEDIO)", ln=True)
    pdf.set_fill_color(241, 245, 249)
    pdf.set_text_color(100, 116, 139)
    pdf.set_font("Arial", "B", 10)
    pdf.cell(130, 8, " INDICADOR", border=1, fill=True)
    pdf.cell(60, 8, " PUNTAJE", border=1, fill=True, ln=True, align="C")

    pdf.set_text_color(0, 0, 0)
    pdf.set_font("Arial", "", 10)
    metrics_list = list(METRIC_LABELS.values())
    for i, label in enumerate(metrics_list):
        pdf.cell(130, 8, f" {label}", border=1)
        pdf.cell(60, 8, f"{data['scores'][i]}", border=1, ln=True, align="C")
    pdf.ln(10)

    # Análisis IA
    if data["analisis_ia"]:
        pdf.set_text_color(30, 58, 138)
        pdf.set_font("Arial", "B", 12)
        pdf.cell(190, 10, "INSIGHT DE INTELIGENCIA ARTIFICIAL", ln=True)
        pdf.set_text_color(0, 0, 0)
        pdf.set_font("Arial", "B", 10)
        pdf.cell(190, 8, f"Sentimiento Predominante: {data['analisis_ia']['sentimiento']}", ln=True)
        pdf.ln(2)
        pdf.set_font("Arial", "B", 10)
        pdf.cell(190, 8, "Cursos y Capacitaciones Recomendadas:", ln=True)
        pdf.set_font("Arial", "", 10)
        for curso in data["analisis_ia"]["recomendaciones"]:
            pdf.cell(190, 7, f" > {curso}", ln=True)

    pdf_content = pdf.output(dest='S')
    output = io.BytesIO(pdf_content)
    return StreamingResponse(output, media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename=Reporte_InsightHR_{u.id}.pdf"})

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
