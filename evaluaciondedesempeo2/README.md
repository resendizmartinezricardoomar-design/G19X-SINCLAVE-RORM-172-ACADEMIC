# InsightHR - Sistema de Gestión de Desempeño

Sistema integral de evaluación de desempeño 360° con análisis de Inteligencia Artificial para gestión de talento humano.

## 📂 Estructura del Proyecto
- `app/`: Aplicación móvil nativa (Android) desarrollada en Kotlin.
- `backend/`: API REST construida con **FastAPI** (Python).
- `web/`: Panel administrativo web (HTML, JavaScript y Tailwind CSS).

## 🛠️ Requisitos
- **Python:** 3.10 a 3.12 (Recomendado 3.12).
- **Android Studio:** Jellyfish o superior.

## 🚀 Guía de Inicio Rápido

### 1. Servidor Backend (API)
1. Navega a `backend/`.
2. Instala dependencias: `pip install -r requirements.txt`.
3. Configura el `.env` (usa `.env.example` como base).
4. Carga datos de prueba: `python seed.py`.
5. Inicia el servidor: `python main.py`. Quedará en `http://localhost:8000`.

### 2. Panel Administrativo Web
Para que el panel pueda realizar peticiones a la API (fetch), **debe servirse mediante HTTP**:

1. Ve a la carpeta web: `cd web`
2. Lanza el servidor: `python -m http.server 8080`
3. Abre en tu navegador: **http://localhost:8080**
*Nota: Ya no se debe abrir index.html directamente como archivo.*

### 3. Aplicación Android
1. Abre el proyecto en Android Studio.
2. Si usas un dispositivo físico, cambia la IP en `app/src/main/java/com/example/evaluaciondedesempeo/api/ApiClient.kt`.
3. Compila y ejecuta (Run).

## 🔑 Cuentas de Prueba
Usa estas credenciales una vez ejecutado el script `seed.py`:

| Rol | Correo | Contraseña | Acceso |
| :--- | :--- | :--- | :--- |
| **ADMIN** | *(Definido en tu .env)* | *(Definido en tu .env)* | Total (Dashboard, Auditorías, PDF) |
| **EMPLOYEE** | carlos@insighthr.com | demo123 | Solo vista personal y equipo |
| **EMPLOYEE** | ana@insighthr.com | demo123 | Solo vista personal y equipo |
| **EMPLOYEE** | beatriz@insighthr.com | demo123 | Solo vista personal y equipo |

## 🛣️ Catálogo de Rutas (API)
- `POST /api/login`: Autenticación.
- `GET /api/empleados`: Lista de colaboradores y puntajes.
- `POST /api/evaluaciones`: Creación de nuevas auditorías.
- `GET /api/reporte/{id}`: Descarga de reporte estratégico en PDF.

## 🤖 IA Engine
El sistema utiliza búsqueda de patrones semánticos en `ai_engine.py` para detectar sentimientos y sugerir capacitaciones basadas en los comentarios de las evaluaciones.

---
© 2026 InsightHR Business Suite - Confidencial.
