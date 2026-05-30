#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# ClassTrack - Eliminar todos los recursos AWS (evitar costos)
# Uso: bash destroy.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e

AWS_REGION="us-east-1"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "============================================="
echo "  ADVERTENCIA: Esto eliminara TODOS los"
echo "  recursos de ClassTrack en AWS."
echo "  Account: $ACCOUNT_ID | Region: $AWS_REGION"
echo "============================================="
read -p "  Escribe 'eliminar' para confirmar: " CONFIRM

if [ "$CONFIRM" != "eliminar" ]; then
  echo "  Cancelado."
  exit 0
fi

# Eliminar snapshots de RDS (evitar costos residuales)
echo ""
echo "[1/4] Eliminando snapshots de RDS..."
aws rds describe-db-snapshots \
  --region "$AWS_REGION" \
  --query "DBSnapshots[?DBInstanceIdentifier=='classtrack-db'].DBSnapshotIdentifier" \
  --output text | \
  xargs -I {} aws rds delete-db-snapshot \
    --db-snapshot-identifier {} \
    --region "$AWS_REGION" \
    2>/dev/null || echo "      No habia snapshots de RDS."

# Vaciar ECR antes de eliminar el stack (CloudFormation no puede eliminar repos con imagenes)
echo ""
echo "[2/4] Eliminando imagenes de ECR..."
aws ecr batch-delete-image \
  --repository-name classtrack-api \
  --region "$AWS_REGION" \
  --image-ids imageTag=latest \
  2>/dev/null || echo "      No habia imagenes en ECR."

# Eliminar stack de aplicacion
echo ""
echo "[3/4] Eliminando stack classtrack-app..."
aws cloudformation delete-stack \
  --stack-name classtrack-app \
  --region "$AWS_REGION"

aws cloudformation wait stack-delete-complete \
  --stack-name classtrack-app \
  --region "$AWS_REGION"

echo "      Stack classtrack-app eliminado."

# Eliminar stack de infraestructura
echo ""
echo "[4/4] Eliminando stack classtrack-infra..."
aws cloudformation delete-stack \
  --stack-name classtrack-infra \
  --region "$AWS_REGION"

aws cloudformation wait stack-delete-complete \
  --stack-name classtrack-infra \
  --region "$AWS_REGION"

echo ""
echo "============================================="
echo "  Todos los recursos eliminados correctamente"
echo "============================================="
