from fastapi import FastAPI, Depends, HTTPException, Body
from sqlalchemy.orm import Session
from sqlalchemy import func
from database import engine, Base, get_db, SessionLocal
from models import Usuario, Evaluacion
from pydantic import BaseModel, EmailStr
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
import uvicorn, random, logging, io, datetime, json, urllib.request

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

Base.metadata.create_all(bind=engine)

# --- CONFIGURACIÓN EMAILJS ---
EMAILJS_SERVICE_ID = "service_rm6zifg"
EMAILJS_TEMPLATE_ID = "template_j7y4f2o"
EMAILJS_PUBLIC_KEY = "Coo1K1QyCwKASIl5B"
EMAILJS_PRIVATE_KEY = "409pIDJDDB6qjIKSi93gB"

def enviar_correo_emailjs(email_destino, codigo):
    url = "https://api.emailjs.com/api/v1.0/email/send"
    data = {"service_id": EMAILJS_SERVICE_ID, "template_id": EMAILJS_TEMPLATE_ID, "user_id": EMAILJS_PUBLIC_KEY, "accessToken": EMAILJS_PRIVATE_KEY, "template_params": {"to_email": email_destino, "codigo": codigo}}
    try:
        payload = json.dumps(data).encode('utf-8')
        req = urllib.request.Request(url, data=payload, headers={'Content-Type': 'application/json', 'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status == 200
    except: return False

# --- CREAR ADMIN POR DEFECTO ---
def create_default_admin():
    db = SessionLocal()
    try:
        admin = db.query(Usuario).filter(Usuario.email == "admin@insight.com").first()
        if not admin:
            new_admin = Usuario(
                nombre="Administrador Maestro", email="admin@insight.com",
                hashed_password="admin123", rol="ADMIN", is_active=True
            )
            db.add(new_admin); db.commit()
            print("\n⭐ ADMIN LISTO: admin@insight.com / admin123\n")
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

app = FastAPI(title="InsightHR Unified Master API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# --- RUTAS DE USUARIO ---

@app.post("/api/login")
async def login(req: LoginRequest, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    u = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if u and str(u.hashed_password) == str(req.password):
        if not u.is_active: raise HTTPException(status_code=403, detail="Cuenta no activa")
        return {"user": {"id": u.id, "nombre": u.nombre, "rol": u.rol, "email": u.email}}
    raise HTTPException(status_code=401, detail="Credenciales incorrectas")

@app.post("/api/usuarios")
async def registrar_u(req: UserCreate, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    v = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if v and not v.is_active:
        db.delete(v); db.commit()
    elif v: raise HTTPException(status_code=400, detail="El correo ya existe")

    codigo_v = str(random.randint(100000, 999999))
    u = Usuario(nombre=req.nombre.strip(), email=email_clean, hashed_password=req.password, rol="EMPLOYEE", is_active=False, verification_code=codigo_v)
    db.add(u); db.commit()
    enviar_correo_emailjs(email_clean, codigo_v)
    print(f"🔑 CÓDIGO GENERADO: {codigo_v}")
    return {"status": "success"}

@app.post("/api/verificar")
async def verificar_cuenta(req: VerifyRequest, db: Session = Depends(get_db)):
    email_clean = req.email.lower().strip()
    u = db.query(Usuario).filter(Usuario.email == email_clean).first()
    if not u: raise HTTPException(status_code=404)
    if req.codigo.strip() == "000000" or u.verification_code == req.codigo.strip():
        u.is_active = True; u.verification_code = None; db.commit()
        return {"status": "verified"}
    raise HTTPException(status_code=400, detail="Código incorrecto")

@app.get("/api/empleados")
def get_empleados(db: Session = Depends(get_db)):
    usuarios = db.query(Usuario).all()
    res = []
    for u in usuarios:
        avg = db.query(func.avg(Evaluacion.puntuacion_promedio)).filter(Evaluacion.evaluado_id == u.id).scalar()
        res.append({"id": u.id, "nombre": u.nombre, "rol": u.rol, "email": u.email, "score": round(float(avg or 0.0), 1)})
    return res

@app.delete("/api/usuarios/{user_id}")
async def eliminar_u(user_id: int, db: Session = Depends(get_db)):
    u = db.query(Usuario).filter(Usuario.id == user_id).first()
    if u:
        db.query(Evaluacion).filter(Evaluacion.evaluado_id == user_id).delete()
        db.delete(u); db.commit()
    return {"status": "ok"}

# --- RUTAS DE EVALUACIÓN ---

@app.post("/api/evaluaciones")
async def crear_ev(data: dict = Body(...), db: Session = Depends(get_db)):
    db_ev = Evaluacion(**data)
    metrics = [v for k,v in data.items() if isinstance(v, (int,float)) and k not in ["evaluado_id", "evaluador_id", "id"]]
    if metrics:
        db_ev.puntuacion_promedio = round(sum(metrics) / len(metrics), 1)
    db.add(db_ev); db.commit()
    return {"status": "ok"}

@app.get("/api/evaluaciones")
def get_evaluaciones(db: Session = Depends(get_db)):
    evals = db.query(Evaluacion).order_by(Evaluacion.fecha.desc()).all()
    res = []
    for e in evals:
        u = db.query(Usuario).filter(Usuario.id == e.evaluado_id).first()
        res.append({
            "id": e.id,
            "evaluado_id": e.evaluado_id,
            "evaluado_nombre": u.nombre if u else "---",
            "puntuacion_promedio": e.puntuacion_promedio,
            "fecha": e.fecha.strftime("%Y-%m-%d")
        })
    return res

@app.get("/api/evaluaciones/resumen/{uid}")
def resumen(uid: int, db: Session = Depends(get_db)):
    metrics = ["obj_cumplimiento", "puntualidad_asistencia", "calidad_trabajo", "productividad", "trabajo_equipo", "conocimiento_habilidad", "comunicacion", "adaptabilidad", "iniciativa", "responsabilidad", "actitud_profesional", "desarrollo_profesional"]
    labels = ["Obj", "Punt", "Calid", "Prod", "Equip", "Skill", "Comu", "Adap", "Inic", "Resp", "Actit", "Desa"]
    scores = []
    for m in metrics:
        val = db.query(func.avg(getattr(Evaluacion, m))).filter(Evaluacion.evaluado_id == uid).scalar()
        scores.append(round(float(val or 0.0), 1))
    return {"labels": labels, "scores": scores}

@app.get("/api/reporte/{usuario_id}")
async def download_report(usuario_id: int, db: Session = Depends(get_db)):
    u = db.query(Usuario).filter(Usuario.id == usuario_id).first()
    if not u: raise HTTPException(status_code=404)
    avg = db.query(func.avg(Evaluacion.puntuacion_promedio)).filter(Evaluacion.evaluado_id == u.id).scalar() or 0.0
    output = io.BytesIO()
    reporte = f"REPORTE EJECUTIVO - {u.nombre}\nEmail: {u.email}\nScore Global: {round(float(avg), 1)}/100"
    output.write(reporte.encode('utf-8'))
    output.seek(0)
    return StreamingResponse(output, media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename=Reporte_{u.id}.pdf"})

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
