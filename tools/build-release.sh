#!/usr/bin/env bash
# 제출용 release APK를 만들고 확인한 뒤 apk/SamgakGorae_<앱>_release.apk로 복사한다(팀 문서 10절).
#
# usage (저장소 루트에서, 두 저장소 공통):
#   bash tools/build-release.sh          # 확인을 모두 통과해야 복사
#   ALLOW_FAKE=1 bash tools/build-release.sh   # 서버 키 없이(Fake 데이터) 만들어 보기만 할 때. 제출용 아님
#
# local.properties에 필요한 값(저장소에 올리지 않음):
#   supabase.url=...  supabase.publishableKey=...    서버 연결(없으면 조용히 Fake로 빌드되므로 막는다)
#   supabase.demo=true(또는 비움)                     제출 빌드는 시연 데이터 모드(PRD 7절)
#   release.storeFile=... release.storePassword=... release.keyAlias=... release.keyPassword=...   서명
#
# 전제: JAVA_HOME(예: export JAVA_HOME="/e/AndroidStudio/jbr"), local.properties의 sdk.dir
set -euo pipefail
cd "$(dirname "$0")/.."

prop() { sed -n "s/^$1=//p" local.properties 2>/dev/null | tail -1 | tr -d '\r'; }
fail() { echo "✗ $*" >&2; exit 1; }

case "$(sed -n 's/.*applicationId = "com.hyundaiapp.\([a-z]*\)".*/\1/p' app/build.gradle.kts)" in
  chachelin) APP=Chachelin ;;
  crong) APP=CRONG ;;
  *) fail "applicationId를 찾지 못함" ;;
esac
OUT="apk/SamgakGorae_${APP}_release.apk"

[ -f local.properties ] || fail "local.properties 없음"
if [ -z "$(prop supabase.url)" ] || [ -z "$(prop supabase.publishableKey)" ]; then
  [ "${ALLOW_FAKE:-0}" = 1 ] || fail "supabase.url / supabase.publishableKey 없음 → 서버 없이 Fake로 빌드됨. 키를 넣거나 ALLOW_FAKE=1"
  echo "! 서버 키 없음: Fake 데이터 빌드(제출용 아님)"
fi
if [ -n "$(prop supabase.url)" ]; then
  # 형식이 틀리면 앱이 서버 없이(Fake) 뜬다(core 0.5.2). 제출 전에 여기서 잡는다
  case "$(prop supabase.url)" in https://*) ;; *) fail "supabase.url은 https://로 시작해야 함(앞 공백·따옴표 확인)" ;; esac
  case "$(prop supabase.publishableKey)" in sb_publishable_*) ;; *) fail "supabase.publishableKey는 sb_publishable_로 시작해야 함(구형 anon 키 아님, 앞 공백·따옴표 확인)" ;; esac
fi
[ -n "$(prop release.storeFile)" ] || fail "release.storeFile 없음 → 서명되지 않은 APK가 나옴"

./gradlew -q assembleRelease

BC=$(find app/build/generated/source/buildConfig/release -name BuildConfig.java | head -1)
grep -q 'SUPABASE_SIMULATED = true' "$BC" || fail "release가 시연 데이터 모드가 아님(supabase.demo=false?)"
APK=app/build/outputs/apk/release/app-release.apk
[ -f "$APK" ] || fail "$APK 없음(서명 설정 확인)"

SDK=$(prop sdk.dir)
APKSIGNER=$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -1)apksigner
[ -x "$APKSIGNER" ] || APKSIGNER="${APKSIGNER}.bat"
if [ -e "$APKSIGNER" ]; then
  "$APKSIGNER" verify "$APK" || fail "서명 확인 실패"
else
  echo "! apksigner를 찾지 못해 서명 확인을 건너뜀"
fi

mkdir -p apk
cp "$APK" "$OUT"
echo "✓ $OUT ($(du -h "$OUT" | cut -f1)) — 시연 데이터 모드, 서명됨"
[ "${ALLOW_FAKE:-0}" = 1 ] && echo "! ALLOW_FAKE 빌드는 커밋하지 말 것"
exit 0
