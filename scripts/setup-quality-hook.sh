#!/bin/bash
# Instala el pre-commit hook de calidad de código.
# Ejecutar una sola vez tras clonar el repositorio:
#   chmod +x scripts/setup-quality-hook.sh && ./scripts/setup-quality-hook.sh

set -e

HOOK_DIR="$(git rev-parse --git-dir)/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"

cat > "$HOOK_FILE" <<'EOF'
#!/bin/bash

# Solo se re-stagean los archivos que YA formaban parte de este commit: `git add` sobre todo lo
# modificado arrastraría al commit el trabajo en curso que la persona dejó fuera a propósito
# (git add -p, un commit parcial).
STAGED_KOTLIN=$(git diff --cached --name-only --diff-filter=ACM -- '*.kt' '*.kts')

./gradlew formatAndAnalyze --quiet
FORMAT_EXIT=$?

if [ -n "$STAGED_KOTLIN" ]; then
    echo "$STAGED_KOTLIN" | tr '\n' '\0' | xargs -0 git add --
    echo "ℹ️  formatAndAnalyze corrigió el formato — el commit incluye los archivos corregidos."
fi

if [ $FORMAT_EXIT -ne 0 ]; then
    echo "❌ Commit bloqueado — hay errores que no se pueden auto-corregir. Revisa la salida anterior."
    exit 1
fi

exit 0
EOF

chmod +x "$HOOK_FILE"
echo "✅ pre-commit hook instalado en $HOOK_FILE"
