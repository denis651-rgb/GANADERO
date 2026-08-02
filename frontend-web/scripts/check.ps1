$ErrorActionPreference = "Stop"
npm run typecheck
npm run lint
npm run test
npm run build
Write-Host "Verificación completa" -ForegroundColor Green
