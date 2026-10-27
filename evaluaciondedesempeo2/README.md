# InsightHR - Executive Performance Suite

Sistema integral de evaluación de desempeño 360° con análisis de Inteligencia Artificial, diseñado para la gestión estratégica de talento humano.

## 🚀 Características Principales
- **Dashboard Estratégico:** Visualización de métricas en tiempo real.
- **Análisis Insight AI:** Generación de recomendaciones y sentimiento basados en comentarios mediante procesamiento de lenguaje natural.
- **Seguridad Industrial:** Hashing de contraseñas y autenticación mediante Tokens JWT.
- **Reportes Ejecutivos:** Generación automatizada de PDFs con resultados detallados.

## 🛠️ Tecnologías Utilizadas
- **Android:** Kotlin, Retrofit2, MPAndroidChart.
- **Backend:** FastAPI (Python), SQLAlchemy, JWT.
- **IA:** Motor de análisis semántico integrado (`ai_engine.py`).
- **Base de Datos:** SQLite.

## 🔐 Seguridad
Las llaves de servicios externos (EmailJS) y secretos de JWT se gestionan mediante variables de entorno en el archivo `.env`. Consulte `.env.example` para la configuración.

---

### 🤖 Declaración de Uso de IA
Este proyecto fue desarrollado con el apoyo de modelos de Inteligencia Artificial para:
- La lógica del motor de análisis de sentimientos en `ai_engine.py`.
- La estructuración de las cabeceras de seguridad JWT.
- La generación de plantillas para los reportes estratégicos en PDF.
- Optimización de consultas SQL y refactorización de código en Android.

---
© 2026 InsightHR Business Suite - Confidencial.
