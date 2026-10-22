# InsightHR - Sistema de Evaluación de Desempeño 360°

## 📝 Descripción
InsightHR es una plataforma integral para la gestión y auditoría del capital humano. Permite realizar evaluaciones de desempeño basadas en 12 ejes estratégicos, visualizando los resultados mediante gráficos de radar dinámicos.

## 🏗️ Arquitectura
*   **App Android (Kotlin):** Interfaz para supervisores y administradores para realizar auditorías en tiempo real.
*   **Backend (Python/FastAPI o Flask):** API REST que gestiona la lógica de negocio, base de datos y procesamiento de IA.
*   **Panel Web (JS/Dashboard):** Visualización analítica de KPIs y generación de reportes PDF.

## 🚀 Configuración del Backend
1. Navegar a la carpeta: `cd evaluaciondedesempeo2/backend`
2. Instalar dependencias: `pip install -r requirements.txt`
3. Configurar variables de entorno: Crear un archivo `.env` basado en `.env.example`.
4. Iniciar servidor: `python main.py`

## 📱 Configuración de la App Android
*   Abrir `app/src/main/java/com/example/evaluaciondedesempeo/api/ApiClient.kt`.
*   Cambiar la constante `BASE_URL` por la IP local de tu computadora (ejemplo: `http://192.168.1.XX:8000/api/`).

## 🌐 Panel Web
*   Abrir el archivo `web/index.html` en cualquier navegador moderno.
*   Asegurarse de que el backend esté corriendo para visualizar los datos reales.

## 🛠️ Tecnologías
*   Android SDK (Kotlin)
*   Python 3.x
*   SQLite / SQLAlchemy
*   Chart.js (Visualización)
*   Retrofit 2 (Networking)
