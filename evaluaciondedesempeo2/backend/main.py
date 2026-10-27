import os
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
from dotenv import load_dotenv
import uvicorn, random, logging, io, json, urllib.request
from ai_engine import ai_engine
from fpdf import FPDF

# Cargar variables de entorno si existen
load_dotenv()

# Configuración de Seguridad
SECRET_KEY = os.getenv("SECRET_KEY", "insight_super_secret_key_9988")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 600
pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="api/login")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

Base.metadata.create_all(bind=engine)

# --- CONFIGURACIÓN EMAILJS ---
EMAILJS_SERVICE_ID = os.getenv("EMAILJS_SERVICE_ID", "service_rm6zifg")
EMAILJS_TEMPLATE_ID = os.getenv("EMAILJS_TEMPLATE_ID", "template_j7y4f2o")
EMAILJS_PUBLIC_KEY = os.getenv("EMAILJS_PUBLIC_KEY", "Coo1K1QyCwKASIl5B")
EMAILJS_PRIVATE_KEY = os.getenv("EMAILJS_PRIVATE_KEY", "409pIDJDDB6qjIKSi93gB")

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
    db = SessionLocal()
    try:
        admin = db.query(Usuario).filter(Usuario.email == "admin@insight.com").first()
        if not admin:
            hashed = pwd_context.hash("admin123")
            new_admin = Usuario(
                nombre="Administrador Maestro", email="admin@insight.com",
                hashed_password=hashed, rol="ADMIN", is_active=True
            )
            db.add(new_admin); db.commit()
            print("\n⭐ ADMIN CREADO CON ÉXITO\n")
        else:
            if not admin.is_active:
                admin.is_active = True; db.commit()
            if not str(admin.hashed_password).startswith("$pbkdf2-sha256$"):
                admin.hashed_password = pwd_context.hash("admin123")
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

    codigo_v = str(random.randint(100000, 999999))
    hashed = pwd_context.hash(req.password)
    u = Usuario(nombre=req.nombre.strip(), email=email_clean, hashed_password=hashed, rol="EMPLOYEE", is_active=False, verification_code=codigo_v)
    db.add(u); db.commit()
    enviar_correo_emailjs(email_clean, codigo_v)
    print(f"🔑 CÓDIGO GENERADO: {codigo_v}")
    return {"status": "success"}

@app.post("/api/verificar")
async def verificar_cuenta(req: VerifyRequest, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    u = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if not u: raise HTTPException(status_code=404)
    if u.verification_code == req.codigo.strip():
        u.is_active = True; u.verification_code = None; db.commit()
        return {"status": "verified"}
    raise HTTPException(status_code=400, detail="Código incorrecto")

@app.get("/api/empleados")
def get_empleados(db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    usuarios = db.query(Usuario).all()
    res = []
    for u in usuarios:
        avg = db.query(func.avg(Evaluacion.puntuacion_promedio)).filter(Evaluacion.evaluado_id == u.id).scalar()
        res.append({"id": u.id, "nombre": u.nombre, "rol": u.rol, "email": u.email, "score": round(float(avg or 0.0), 1)})
    return res

@app.delete("/api/usuarios/{user_id}")
async def eliminar_u(user_id: int, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    if current_user.rol != "ADMIN":
        raise HTTPException(status_code=403, detail="No autorizado")
    u = db.query(Usuario).filter(Usuario.id == user_id).first()
    if u:
        db.query(Evaluacion).filter(Evaluacion.evaluado_id == user_id).delete()
        db.delete(u); db.commit()
    return {"status": "ok"}

# --- RUTAS DE EVALUACIÓN ---

@app.get("/api/evaluaciones")
def listar_evaluaciones(db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    evs = db.query(Evaluacion).order_by(Evaluacion.fecha.desc()).all()
    return [{
        "fecha": e.fecha.strftime("%Y-%m-%d") if e.fecha else "",
        "evaluado_id": e.evaluado_id,
        "evaluado_nombre": e.evaluado.nombre if e.evaluado else "Desconocido",
        "puntuacion_promedio": e.puntuacion_promedio
    } for e in evs]

@app.post("/api/evaluaciones")
async def crear_ev(data: EvaluacionCreate, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
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

@app.get("/api/evaluaciones/resumen/{uid}")
def resumen(uid: int, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    metrics = ["obj_cumplimiento", "puntualidad_asistencia", "calidad_trabajo", "productividad", "trabajo_equipo", "conocimiento_habilidad", "comunicacion", "adaptabilidad", "iniciativa", "responsabilidad", "actitud_profesional", "desarrollo_profesional"]
    labels = ["Obj", "Punt", "Calid", "Prod", "Equip", "Skill", "Comu", "Adap", "Inic", "Resp", "Actit", "Desa"]
    scores = []
    for m in metrics:
        val = db.query(func.avg(getattr(Evaluacion, m))).filter(Evaluacion.evaluado_id == uid).scalar()
        scores.append(round(float(val or 0.0), 1))
    last_ev = db.query(Evaluacion).filter(Evaluacion.evaluado_id == uid).order_by(Evaluacion.fecha.desc()).first()
    analisis_ia = None
    if last_ev:
        analisis_ia = ai_engine.analizar_comentarios(last_ev.comentarios_texto or "Sin comentarios", last_ev.puntuacion_promedio or 0.0)
    kpis = {
        "Eficiencia Operativa": round(sum(scores[0:4]) / 4, 1) if scores else 0.0,
        "Competencias Blandas": round(sum(scores[4:8]) / 4, 1) if scores else 0.0,
        "Potencial y Crecimiento": round(sum(scores[8:12]) / 4, 1) if scores else 0.0
    }
    return {"labels": labels, "scores": scores, "analisis_ia": analisis_ia, "kpis": kpis}

@app.get("/api/reporte/{usuario_id}")
async def download_report(usuario_id: int, db: Session = Depends(get_db), current_user: Usuario = Depends(get_current_user)):
    u = db.query(Usuario).filter(Usuario.id == usuario_id).first()
    if not u: raise HTTPException(status_code=404)
    avg = db.query(func.avg(Evaluacion.puntuacion_promedio)).filter(Evaluacion.evaluado_id == u.id).scalar() or 0.0
    pdf = FPDF()
    pdf.add_page()
    pdf.set_font("Arial", "B", 16)
    pdf.cell(190, 10, "REPORTE ESTRATÉGICO DE DESEMPEÑO", ln=True, align="C")
    pdf.ln(10)
    pdf.set_font("Arial", "", 12)
    pdf.cell(190, 10, f"Colaborador: {u.nombre}", ln=True)
    pdf.cell(190, 10, f"Email: {u.email}", ln=True)
    pdf.ln(10)
    pdf.set_fill_color(230, 230, 230)
    pdf.cell(190, 15, f"SCORE GLOBAL: {round(float(avg), 1)} / 100", ln=True, fill=True)
    pdf_content = pdf.output(dest='S')
    output = io.BytesIO(pdf_content)
    return StreamingResponse(output, media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename=Reporte_InsightHR_{u.id}.pdf"})

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
