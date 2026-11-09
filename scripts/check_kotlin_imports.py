#!/usr/bin/env python3
"""Impede imports duplicados em Kotlin (compilação falha com conflicting import)."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
files = sorted((root / "app" / "src").rglob("*.kt"))
errors = []
for path in files:
    seen = {}
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        stripped = line.strip()
        if not stripped.startswith("import "):
            continue
        if stripped in seen:
            errors.append(f"{path.relative_to(root)}:{number}: {stripped} duplicado (linha {seen[stripped]})")
        else:
            seen[stripped] = number
if errors:
    raise SystemExit("\n".join(errors))
print(f"IMPORTS KOTLIN OK: {len(files)} arquivos analisados, nenhuma importação duplicada")
