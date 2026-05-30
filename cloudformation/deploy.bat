@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM ClassTrack - Script de despliegue para Windows
REM Uso: Abrir PowerShell como administrador y ejecutar .\deploy.bat
REM ─────────────────────────────────────────────────────────────────────────────

setlocal EnableDelayedExpansion

REM ── CONFIGURACION ─────────────────────────────────────────────────────────
set AWS_REGION=us-east-1
set DB_PASSWORD=postgres
set ADMIN_CIDR=54.144.54.234/32
set PROJECT_ROOT=%~dp0..

echo =============================================
echo   ClassTrack AWS Deployment (Windows)
echo =============================================

for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
echo Account: %ACCOUNT_ID%
echo Region:  %AWS_REGION%

REM ── PASO 1: INFRA STACK ───────────────────────────────────────────────────
echo.
echo [1/5] Desplegando stack de infraestructura...
echo       Esto puede tardar 10-15 minutos...

aws cloudformation deploy ^
  --template-file "%PROJECT_ROOT%\cloudformation\classtrack-infra.yaml" ^
  --stack-name classtrack-infra ^
  --region %AWS_REGION% ^
  --capabilities CAPABILITY_NAMED_IAM ^
  --parameter-overrides DBPassword="%DB_PASSWORD%" AdminCidr="%ADMIN_CIDR%"

if %errorlevel% neq 0 (
    echo ERROR: Fallo el despliegue del stack de infraestructura
    exit /b 1
)

REM ── PASO 2: OBTENER OUTPUTS ──────────────────────────────────────────────
echo.
echo [2/5] Obteniendo outputs del stack...

for /f "tokens=*" %%i in ('aws cloudformation list-exports --region %AWS_REGION% --query "Exports[?Name==''classtrack-ECRRepositoryUri''].Value" --output text') do set ECR_URI=%%i
for /f "tokens=*" %%i in ('aws cloudformation list-exports --region %AWS_REGION% --query "Exports[?Name==''classtrack-RDSEndpoint''].Value" --output text') do set RDS_ENDPOINT=%%i

echo       ECR URI:      %ECR_URI%
echo       RDS Endpoint: %RDS_ENDPOINT%

REM ── PASO 3: BUILD Y PUSH IMAGEN ──────────────────────────────────────────
echo.
echo [3/5] Construyendo y subiendo imagen Docker...

aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com

cd "%PROJECT_ROOT%"
docker build -t classtrack-api .
docker tag classtrack-api:latest %ECR_URI%:latest
docker push %ECR_URI%:latest

echo       Imagen subida correctamente.

REM ── PASO 4: SCHEMA SQL ───────────────────────────────────────────────────
echo.
echo [4/5] Ejecutando schema.sql en RDS...
echo       Host: %RDS_ENDPOINT%
echo.
echo       Si psql no esta instalado, ejecuta schema.sql manualmente con DBeaver o pgAdmin.
echo       Conexion: Host=%RDS_ENDPOINT% Puerto=5432 DB=classtrack Usuario=postgres
echo.
set /p SKIP_SCHEMA="Presiona ENTER para ejecutar con psql, o escribe 'skip' para omitir: "

if not "%SKIP_SCHEMA%"=="skip" (
    set PGPASSWORD=%DB_PASSWORD%
    psql -h %RDS_ENDPOINT% -U postgres -d classtrack -f "%PROJECT_ROOT%\src\main\resources\schema.sql"
)

REM ── PASO 5: APP STACK ────────────────────────────────────────────────────
echo.
echo [5/5] Desplegando stack de aplicacion...

aws cloudformation deploy ^
  --template-file "%PROJECT_ROOT%\cloudformation\classtrack-app.yaml" ^
  --stack-name classtrack-app ^
  --region %AWS_REGION% ^
  --parameter-overrides ^
    ImageUri="%ECR_URI%:latest" ^
    DBPassword="%DB_PASSWORD%"

echo.
echo =============================================
echo   DESPLIEGUE COMPLETADO
echo =============================================
echo.
echo   Para obtener la IP publica:
echo   aws ecs list-tasks --cluster classtrack-cluster --region %AWS_REGION%
echo   aws ecs describe-tasks --cluster classtrack-cluster --region %AWS_REGION% --tasks [TASK_ARN]
echo.
echo   Swagger: http://[PUBLIC_IP]:8080/swagger-ui.html
echo =============================================

endlocal
