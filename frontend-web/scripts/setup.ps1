$ErrorActionPreference = "Stop"

if (-not (Test-Path ".env.local")) {
    Copy-Item ".env.example" ".env.local"
    Write-Host "Se creó .env.local desde .env.example" -ForegroundColor Green
}

npm install
npm run typecheck
Write-Host "Frontend preparado. Ejecuta: npm run dev" -ForegroundColor Green
