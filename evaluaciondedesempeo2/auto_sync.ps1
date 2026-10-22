# Script de Auto-Sincronizacion para InsightHR
Write-Host "Iniciando monitor de cambios en InsightHR..." -ForegroundColor Cyan
Write-Host "Presiona Ctrl+C para detener." -ForegroundColor Gray

while($true) {
    # Verifica si hay cambios en la carpeta
    $status = git status --porcelain

    if ($status) {
        $timestamp = Get-Date -Format "HH:mm:ss"
        Write-Host "[$timestamp] Cambios detectados, sincronizando..." -ForegroundColor Yellow

        # Ejecuta los comandos de Git
        git add .
        git commit -m "Auto-save: $timestamp"
        git push origin main

        Write-Host "[$timestamp] OK: Todo subido a GitHub." -ForegroundColor Green
    }

    # Espera 30 segundos antes de volver a revisar
    Start-Sleep -Seconds 30
}
