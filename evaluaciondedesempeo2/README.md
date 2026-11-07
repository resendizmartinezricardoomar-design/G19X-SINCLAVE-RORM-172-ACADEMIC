# InsightHR - Sistema de Gestión de Desempeño

## Reglas de Roles

### Administrador (ADMIN)
- **Acceso Total**: Puede ver el dashboard general con el ranking de todos los empleados.
- **Gestión de Talento**: Puede ver, registrar y eliminar cualquier colaborador.
- **Comparativa**: Puede comparar el desempeño de cualquier par de empleados.
- **Auditorías**: Puede ver el historial completo de evaluaciones y crear nuevas auditorías para cualquier empleado.
- **Reportes**: Puede descargar el reporte PDF de cualquier colaborador.

### Colaborador (EMPLOYEE)
- **Vista Personal**: Solo puede ver sus propios resultados en el dashboard.
- **Mi Equipo**: Puede ver la lista de sus compañeros (nombre y rol), pero sin acceso a sus puntajes ni correos electrónicos detallados.
- **Restricciones**: No tiene acceso a las secciones de "Comparativa" ni "Auditorías".
- **Reportes**: Solo puede descargar su propio reporte PDF.

## Primer arranque

1. **Configuración de entorno**:
   - Ve a la carpeta `backend/`.
   - Crea un archivo `.env` basado en `.env.example`.
   - Asegúrate de llenar `SECRET_KEY`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`.

2. **Instalación de dependencias**:
   - Ejecuta `pip install -r requirements.txt`.

3. **Datos de ejemplo**:
   - Ejecuta `python seed.py`. Esto creará automáticamente 5 colaboradores activos y varias evaluaciones de ejemplo para que el sistema tenga datos visualizables de inmediato.

4. **Inicio del sistema**:
   - Ejecuta `python main.py`. 
   - El administrador maestro se creará automáticamente en el primer arranque usando las credenciales del `.env`.
   - **Nota sobre EmailJS**: Si no configuras las llaves de EmailJS, los códigos de activación para nuevos registros se imprimirán directamente en la consola del terminal del backend.
