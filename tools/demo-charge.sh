#!/usr/bin/env bash
# 차슐랭 시연 시나리오(PRD 7절)를 Virtual Console로 재생한다. 2차 PT·시연 영상용.
#
# usage:
#   bash tools/demo-charge.sh success   # 인증 충전: 충전구 열림 → SoC 40→80 → 닫힘 → 인증 카드 + 리뷰 화면
#   bash tools/demo-charge.sh fail      # 충전 실패: 충전구 열림 → SoC 그대로 → 닫힘 → "충전 실패를 기록했어요"
#   bash tools/demo-charge.sh all       # success → fail 순서로
#   bash tools/demo-charge.sh reset     # 시작 상태로만 맞춤(P, 정차, 충전구 닫힘, SoC 40)
#
# 환경변수:
#   PAUSE=1     단계마다 Enter를 기다림(발표자가 설명하며 넘길 때)
#   STEP=3      SoC를 한 단계 올리는 간격(초). 디버그 빌드 인증 기준(10초)보다 길게 유지
#   FAIL_HOLD=35  실패 시나리오에서 충전구를 열어 두는 시간(초). 디버그 빌드 실패 기준(30초)보다 길게
#   SOC_FROM=40 SOC_TO=80 SOC_STEP=10
#
# 전제:
#   - 에뮬레이터 실행, 차슐랭 **디버그 빌드** 설치·실행(ChargeRules.DEMO: 인증 10초, 실패 30초. 시연 데이터 모드 release도 같음)
#   - **앱을 켜면 S7 동의 화면 → 동의 2개 + 차종 + [시작하기] → S1**. 동의 전에는 기록 서비스가 돌지 않아 아무 화면도 안 바뀐다
#   - 앱을 켜고 5초쯤 뒤에 돌린다(신호 연결 시간). 확정 순서는 docs/demo-scenario.md
#   - 에뮬레이터 위치는 판교 시흥동 고정 → 충전은 시드 충전소 st-003(시흥동 주민센터)에서 일어난다
#   - 디버그 빌드는 화면의 충전 시간·kW에 시연 배속 ×120을 곱한다(ChargeRules.DEMO.demoTimeScale, 15초 = 30분).
#     PT에서 "SoC는 콘솔 주입, 시간·kW는 시연 배속"이라고 밝힌다
#   - adb가 PATH에 없으면 ADB=<adb 경로>
#   - 앱 화면을 녹화할 때는 이 스크립트 출력 창을 화면 밖에 둔다
set -euo pipefail
cd "$(dirname "$0")/.."

PAUSE=${PAUSE:-0}
STEP=${STEP:-3}
FAIL_HOLD=${FAIL_HOLD:-35}
SOC_FROM=${SOC_FROM:-40}
SOC_TO=${SOC_TO:-80}
SOC_STEP=${SOC_STEP:-10}

vc() { bash tools/vconsole.sh put "$@" >/dev/null; }
say() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
step() {
  say "$*"
  if [ "$PAUSE" = 1 ]; then read -r -p "  Enter로 진행 " _; fi
}
wait_s() { local n=$1; while [ "$n" -gt 0 ]; do printf '\r  %3d초 ' "$n"; sleep 1; n=$((n - 1)); done; printf '\r          \r'; }

reset_state() {
  step "시작 상태: 기어 P, 정차, 충전구 닫힘, SoC ${SOC_FROM}%"
  vc 'gear?gear=Park' 'controls?speed=0' 'ev-battery?field=charge_port_open&value=false' \
     "ev-battery?field=soc&value=${SOC_FROM}"
  sleep 2
}

success() {
  reset_state
  step "① 충전구 열기 → 앱이 충전 기록 시작(충전 중 화면)"
  vc 'ev-battery?field=charge_port_open&value=true'
  wait_s "$STEP"
  local soc=$SOC_FROM
  while [ "$soc" -lt "$SOC_TO" ]; do
    soc=$((soc + SOC_STEP)); [ "$soc" -gt "$SOC_TO" ] && soc=$SOC_TO
    say "② SoC ${soc}% — 화면의 SoC·계산 평균 속도가 오른다"
    vc "ev-battery?field=soc&value=${soc}"
    wait_s "$STEP"
  done
  step "③ 충전구 닫기 → 인증 카드(${SOC_FROM}% → ${SOC_TO}%) + 별점 화면. 별 5개, 태그 2개 누르고 [완료]"
  vc 'ev-battery?field=charge_port_open&value=false'
}

fail() {
  reset_state
  step "④ 충전구 열기 → 충전이 시작되지 않는 상황(SoC 그대로)"
  vc 'ev-battery?field=charge_port_open&value=true'
  say "   ${FAIL_HOLD}초 동안 SoC를 바꾸지 않는다(실패 기준 30초)"
  wait_s "$FAIL_HOLD"
  step "⑤ 충전구 닫기 → \"충전 실패를 기록했어요\" → 충전소 상세에 \"최근 실패\" 증가"
  vc 'ev-battery?field=charge_port_open&value=false'
}

case "${1:-}" in success|fail|all|reset) ;; *) sed -n '2,/^set -euo/p' "$0" | sed '$d'; exit 1 ;; esac
bash tools/vconsole.sh up
case "${1:-}" in
  success) success ;;
  fail) fail ;;
  all) success; step "다음: 실패 시나리오"; fail ;;
  reset) reset_state ;;
  *) sed -n '2,/^set -euo/p' "$0" | sed '$d'; exit 1 ;;
esac
say "끝. 하트비트는 계속 돈다(그만두려면: bash tools/vconsole.sh down)"
