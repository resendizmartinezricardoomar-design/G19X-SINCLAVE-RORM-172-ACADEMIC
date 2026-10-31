# InsightHR - Executive Performance Suite

Sistema integral de evaluación de desempeño 360° con análisis de Inteligencia Artificial, diseñado para la gestión estratégica de talento humano.

## 📂 Estructura del Proyecto
El repositorio contiene la siguiente estructura:
- `evaluaciondedesempeo2/app/`: Aplicación móvil nativa (Android) desarrollada en Kotlin.
- `evaluaciondedesempeo2/backend/`: API REST construida con **FastAPI** (Python).
- `evaluaciondedesempeo2/web/`: Panel administrativo web (HTML, JavaScript y Tailwind CSS).

## 🛠️ Requisitos
- **Python:** 3.10 o superior.
- **Android Studio:** Jellyfish o superior.

## 🚀 Guía de Inicio Rápido

### 1. Servidor Backend (API)
1. Navega a `evaluaciondedesempeo2/backend`.
2. Crea un entorno virtual: `python -m venv .venv`.
3. Activa el entorno:
   - Windows: `.venv\Scripts\activate`
   - Linux/macOS: `source .venv/bin/activate`
4. Instala las dependencias: `pip install -r requirements.txt`.
5. **Configuración del .env**: Copia el archivo de ejemplo: `copy .env.example .env` (en Windows) o `cp .env.example .env` (en Linux).
6. Edita el archivo `.env` con tus secretos (EmailJS, contraseñas de admin, etc.).
7. Inicia el servidor: `python main.py`. Quedará disponible en `http://localhost:8000`.

### 2. Panel Administrativo Web
Simplemente abre el archivo `evaluaciondedesempeo2/web/index.html` en tu navegador. Asegúrate de que el backend esté corriendo para iniciar sesión.

### 3. Aplicación Android
1. Abre el proyecto en Android Studio.
2. **Configuración de IP**: Localiza el archivo `app/src/main/java/com/example/evaluaciondedesempeo/api/ApiClient.kt`.
3. Cambia `BASE_URL` por la IP local de tu computadora (ej: `http://192.168.1.50:8000/`).
4. Compila y ejecuta en tu dispositivo o emulador.

## 🛣️ Catálogo de Rutas (API)
- `POST /api/login`: Autenticación y obtención de Token JWT.
- `POST /api/usuarios`: Registro de nuevos empleados.
- `POST /api/verificar`: Validación de cuenta mediante código.
- `GET /api/empleados`: Lista de colaboradores y sus puntajes.
- `DELETE /api/usuarios/{id}`: Eliminación de usuario (Solo ADMIN).
- `GET /api/evaluaciones`: Historial de auditorías.
- `POST /api/evaluaciones`: Creación de nuevas evaluaciones.
- `GET /api/evaluaciones/resumen/{uid}`: Resumen de métricas y análisis de IA.
- `GET /api/reporte/{id}`: Descarga de reporte estratégico en PDF.

## 🤖 Funcionamiento del Análisis de IA
El motor de análisis en `ai_engine.py` funciona exclusivamente mediante la **búsqueda de palabras clave** en los comentarios. El sistema escanea el texto en busca de términos específicos para determinar el sentimiento (Positivo, Constructivo, Neutral), identificar áreas de oportunidad y sugerir cursos de capacitación. **No utiliza procesamiento de lenguaje natural (NLP) complejo ni modelos de lenguaje externos (LLM)**.

---
© 2026 InsightHR Business Suite - Confidencial.
