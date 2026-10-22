import random

class AIEngine:
    def analizar_comentarios(self, texto: str, puntuacion: float):
        texto_lower = texto.lower()

        # Análisis de sentimiento simple (Simulado)
        if any(word in texto_lower for word in ["excelente", "gran", "liderazgo", "proactivo"]):
            sentimiento = "Positivo"
        elif any(word in texto_lower for word in ["mejorar", "falta", "tarde", "error", "debilidad"]):
            sentimiento = "Constructivo"
        else:
            sentimiento = "Neutral"

        # Identificación de áreas de oportunidad
        areas = []
        if "comunicación" in texto_lower: areas.append("Comunicación Asertiva")
        if "tiempo" in texto_lower or "plazos" in texto_lower: areas.append("Gestión del Tiempo")
        if "técnico" in texto_lower or "herramienta" in texto_lower: areas.append("Habilidades Técnicas")
        if not areas: areas.append("Desarrollo de Liderazgo General")

        # Recomendaciones de capacitación
        catalogo_cursos = {
            "Comunicación Asertiva": ["Taller de Comunicación Efectiva", "Inteligencia Emocional en el Trabajo"],
            "Gestión del Tiempo": ["Metodologías Ágiles", "Productividad y Foco"],
            "Habilidades Técnicas": ["Curso Avanzado de la Herramienta X", "Certificación en Tecnologías Y"],
            "Desarrollo de Liderazgo General": ["Liderazgo para Equipos Híbridos", "Design Thinking"]
        }

        recomendaciones = []
        for area in areas:
            recomendaciones.extend(catalogo_cursos.get(area, []))

        return {
            "sentimiento": sentimiento,
            "areas_oportunidad": areas,
            "recomendaciones": recomendaciones,
            "score_analisis": round(puntuacion * 0.9, 2) # Factor de ajuste IA
        }

ai_engine = AIEngine()
