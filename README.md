# InsightHR - Executive Performance Suite

Sistema integral de evaluación de desempeño 360° con análisis de Inteligencia Artificial, diseñado para la gestión estratégica de talento humano.

## 📂 Estructura del Proyecto
- `evaluaciondedesempeo2/app/`: Aplicación móvil nativa en Kotlin (Android).
- `evaluaciondedesempeo2/backend/`: API REST construida con FastAPI (Python).
- `evaluaciondedesempeo2/web/`: Panel administrativo web (HTML/JS/Tailwind).

## 🛠️ Requisitos
- **Python:** 3.10 o superior.
- **Android Studio:** Jellyfish o superior.
- **Navegador Web:** Chrome, Edge o Firefox.

## 🚀 Instalación y Configuración

### 1. Backend (API)
Navegue a la carpeta del backend y configure el entorno:
```bash
cd evaluaciondedesempeo2/backend
python -m venv .venv
# En Windows:
.venv\Scripts\activate
# En Linux/macOS:
source .venv/bin/activate

pip install -r requirements.txt
```

**Configuración de Secretos:**
Copia el archivo de ejemplo y rellena tus credenciales (EmailJS y contraseñas):
```bash
cp .env.example .env
```
*Nota: Asegúrate de configurar `ADMIN_EMAIL`, `ADMIN_PASSWORD` y las llaves de `EmailJS`.*

**Iniciar Servidor:**
```bash
python main.py
```
El backend quedará disponible en `http://localhost:8000`.

### 2. Panel Web
No requiere instalación. Simplemente abre el archivo `evaluaciondedesempeo2/web/index.html` en tu navegador. 
*Importante: Asegúrate de que el backend esté corriendo.*

### 3. Aplicación Android
1. Abre la carpeta `evaluaciondedesempeo2/` en Android Studio.
2. **Cambio de IP:** Si pruebas en un dispositivo físico, abre `app/src/main/java/com/example/evaluaciondedesempeo/api/ApiClient.kt` y cambia la constante `BASE_URL` por la IP local de tu computadora (ej. `http://192.168.1.XX:8000/`).
3. Compila y ejecuta en tu dispositivo o emulador.

## 🛣️ Rutas de la API
- **Autenticación:**
  - `POST /api/login`: Inicio de sesión y obtención de JWT.
  - `POST /api/usuarios`: Registro de nuevos empleados.
  - `POST /api/verificar`: Validación de cuenta mediante código.
- **Gestión:**
  - `GET /api/empleados`: Lista de colaboradores y sus puntajes.
  - `DELETE /api/usuarios/{id}`: Eliminación de cuentas (Solo ADMIN).
- **Evaluaciones:**
  - `GET /api/evaluaciones`: Historial completo de auditorías.
  - `POST /api/evaluaciones`: Creación de nuevas evaluaciones.
  - `GET /api/evaluaciones/resumen/{uid}`: Resumen de métricas y análisis de IA.
  - `GET /api/reporte/{id}`: Descarga de reporte estratégico en PDF.

## 🤖 Motor de Análisis (ai_engine.py)
El sistema utiliza un motor de análisis basado en **búsqueda de palabras clave** y patrones semánticos predefinidos para determinar el sentimiento de los comentarios, identificar áreas de oportunidad y sugerir cursos de capacitación específicos. No utiliza un modelo de lenguaje (LLM) externo, lo que garantiza velocidad y privacidad.

---
© 2026 InsightHR Business Suite - Confidencial.
