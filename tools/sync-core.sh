#!/usr/bin/env bash
# 공통 부분(core/, tools/, docs/team-collaboration-plan.md)을 차슐랭(원본)에서 다른 앱 저장소로 복사한다.
# usage: tools/sync-core.sh <대상 저장소 경로> [--check]
#   --check: 복사하지 않고 두 저장소가 같은지만 확인(다르면 exit 1)
set -euo pipefail
src=$(cd "$(dirname "$0")/.." && pwd)
dst=${1:?usage: tools/sync-core.sh <대상 저장소 경로> [--check]}
mode=${2:-}
shared=(core tools docs/team-collaboration-plan.md)

grep -q 'rootProject.name = "Chachelin"' "$src/settings.gradle.kts" \
  || { echo "원본(차슐랭) 저장소에서 실행하세요: $src" >&2; exit 2; }
[ -f "$dst/settings.gradle.kts" ] || { echo "대상이 Gradle 저장소가 아닙니다: $dst" >&2; exit 2; }
dst=$(cd "$dst" && pwd)
[ "$src" != "$dst" ] || { echo "원본과 대상이 같습니다" >&2; exit 2; }

if [ "$mode" = "--check" ]; then
  status=0
  for p in "${shared[@]}"; do
    diff -r -q -x build "$src/$p" "$dst/$p" || status=1
  done
  [ $status -eq 0 ] && echo "일치: core $(cat "$src/core/VERSION")" || echo "불일치 — sync 필요" >&2
  exit $status
fi

for p in "${shared[@]}"; do
  rm -rf "${dst:?}/${p:?}"
  mkdir -p "$(dirname "$dst/$p")"
  cp -r "$src/$p" "$dst/$p"
  rm -rf "${dst:?}/${p:?}/build"
done
echo "복사 완료: core $(cat "$src/core/VERSION") → $dst"
