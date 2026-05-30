#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# ClassTrack - Script de despliegue automatico en AWS
# Uso: bash deploy.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e

# ── CONFIGURACION ─────────────────────────────────────────────────────────────
AWS_REGION="us-east-1"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
DB_PASSWORD=""          # <-- completar antes de ejecutar
ADMIN_CIDR="0.0.0.0/0" # <-- reemplazar con tu IP: "190.x.x.x/32"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "============================================="
echo "  ClassTrack AWS Deployment"
echo "  Account: $ACCOUNT_ID"
echo "  Region:  $AWS_REGION"
echo "============================================="

# ── PASO 1: DESPLEGAR INFRA ───────────────────────────────────────────────────
echo ""
echo "[1/5] Desplegando stack de infraestructura..."
echo "      (VPC, ECR, RDS, IAM, Security Groups)"
echo "      Esto puede tardar 10-15 minutos por RDS..."

aws cloudformation deploy \
  --template-file "$PROJECT_ROOT/cloudformation/classtrack-infra.yaml" \
  --stack-name classtrack-infra \
  --region "$AWS_REGION" \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    DBPassword="$DB_PASSWORD" \
    AdminCidr="$ADMIN_CIDR"

echo "      Stack de infraestructura creado correctamente."

# ── PASO 2: OBTENER OUTPUTS ───────────────────────────────────────────────────
echo ""
echo "[2/5] Obteniendo outputs del stack..."

ECR_URI=$(aws cloudformation list-exports \
  --region "$AWS_REGION" \
  --query "Exports[?Name=='classtrack-ECRRepositoryUri'].Value" \
  --output text)

RDS_ENDPOINT=$(aws cloudformation list-exports \
  --region "$AWS_REGION" \
  --query "Exports[?Name=='classtrack-RDSEndpoint'].Value" \
  --output text)

echo "      ECR URI:      $ECR_URI"
echo "      RDS Endpoint: $RDS_ENDPOINT"

# ── PASO 3: BUILD Y PUSH IMAGEN DOCKER ───────────────────────────────────────
echo ""
echo "[3/5] Construyendo y subiendo imagen Docker a ECR..."

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

cd "$PROJECT_ROOT"
docker build -t classtrack-api .
docker tag classtrack-api:latest "$ECR_URI:latest"
docker push "$ECR_URI:latest"

echo "      Imagen subida correctamente a ECR."

# ── PASO 4: EJECUTAR SCHEMA SQL ───────────────────────────────────────────────
echo ""
echo "[4/5] Ejecutando schema.sql en RDS..."
echo "      Host: $RDS_ENDPOINT"

PGPASSWORD="$DB_PASSWORD" psql \
  -h "$RDS_ENDPOINT" \
  -U postgres \
  -d classtrack \
  -f "$PROJECT_ROOT/src/main/resources/schema.sql"

echo "      Schema ejecutado correctamente."

# ── PASO 5: DESPLEGAR APLICACION ─────────────────────────────────────────────
echo ""
echo "[5/5] Desplegando stack de aplicacion..."
echo "      (ECS Cluster, Task Definition, Service Fargate)"

aws cloudformation deploy \
  --template-file "$PROJECT_ROOT/cloudformation/classtrack-app.yaml" \
  --stack-name classtrack-app \
  --region "$AWS_REGION" \
  --parameter-overrides \
    ImageUri="$ECR_URI:latest" \
    DBPassword="$DB_PASSWORD"

echo ""
echo "============================================="
echo "  DESPLIEGUE COMPLETADO"
echo "============================================="
echo ""
echo "  Para obtener la IP publica de tu API:"
echo ""
echo "  aws ecs list-tasks --cluster classtrack-cluster --region $AWS_REGION"
echo "  aws ecs describe-tasks --cluster classtrack-cluster --region $AWS_REGION --tasks <TASK_ARN>"
echo ""
echo "  Swagger UI: http://<PUBLIC_IP>:8080/swagger-ui.html"
echo "  API Base:   http://<PUBLIC_IP>:8080/api/v1"
echo "============================================="
