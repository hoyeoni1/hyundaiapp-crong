#!/usr/bin/env bash
# 크루즈브레이커 시연 시나리오(PRD 7절): 앱의 위치 재생과 Virtual Console 값 주입을 시간에 맞춰 함께 돌린다.
#
# usage:
#   bash tools/demo-drive.sh          # 전체 시나리오(정차 → 주행 → 안내 → 해제 기록 → 정체 해제 제외 → 정차 요약)
#   bash tools/demo-drive.sh reset    # 시작 상태로만 맞춤(P, 정차, 크루즈 대기)
#   bash tools/demo-drive.sh stop     # 위치 재생 중지 + 정차
#
# 환경변수:
#   RATE=2        경로 재생 배속(1 = 실제 100 km/h, 427초). 2배속이면 약 3분 30초
#   JAM_HOLD=32   정체 장면에서 저속(30 km/h)을 유지하는 시간(초). 정체 기준(30초 평균 < 40 km/h)보다 길게
#   PAUSE=1       장면마다 Enter를 기다림(재생은 계속 흐르므로 영상 녹화 때는 쓰지 않는다)
#   NO_REPLAY=1   앱 위치 재생 없이 콘솔 값만 주입(앱 쪽 재생 모드가 없을 때 신호만 확인)
#   ROUTE=tools/demo/gyeongbu-north.csv   (만들기: python tools/demo/make-route.py > ...)
#
# 앱 쪽 계약(디버그 빌드에 구현할 것, PRD 5.7 "위치 재생 시연 모드"):
#   - 이 스크립트가 경로 파일을 /sdcard/Android/data/com.hyundaiapp.cruisebreaker/files/demo/ 에 adb push 한다
#   - 시작: am broadcast -a com.hyundaiapp.cruisebreaker.DEMO_ROUTE -p com.hyundaiapp.cruisebreaker --es file <이름> --ef rate <배속>
#     → 앱은 CSV(t,lat,lon,heading,mark)를 t/rate 초마다 한 줄씩 읽어 NaviHelper 위치 대신 쓴다
#   - 중지: am broadcast -a com.hyundaiapp.cruisebreaker.DEMO_ROUTE_STOP -p com.hyundaiapp.cruisebreaker
#
# 콘솔 매핑(findings 6.2.1): full_autonomous → ACTIVATED(작동), ready → ENABLED(대기). 실차 해제 값은 재현 불가라
# ACTIVATED → ENABLED를 해제로 본다(CruiseStateInterpreter).
set -euo pipefail
cd "$(dirname "$0")/.."

A=${ADB:-adb}
RATE=${RATE:-2}
JAM_HOLD=${JAM_HOLD:-32}
PAUSE=${PAUSE:-0}
NO_REPLAY=${NO_REPLAY:-0}
ROUTE=${ROUTE:-tools/demo/gyeongbu-north.csv}
PKG=com.hyundaiapp.cruisebreaker
DEVICE_DIR=/sdcard/Android/data/$PKG/files/demo

vc() { bash tools/vconsole.sh put "$@" >/dev/null; }
say() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
scene() {
  say "$*"
  if [ "$PAUSE" = 1 ]; then read -r -p "  Enter로 진행 " _; fi
}
engage() { vc 'driving-mode?ad_mode_state=full_autonomous'; }   # 크루즈 작동(ACTIVATED)
release() { vc 'driving-mode?ad_mode_state=ready'; }           # 크루즈 해제(ENABLED)

reset_state() {
  scene "시작 상태: 기어 P, 정차, 크루즈 대기"
  vc 'gear?gear=Park' 'controls?speed=0' 'controls?brake=0' 'driving-mode?ad_mode_state=ready' \
     'driving-mode?target_speed_mps=27.8'
  sleep 2
}

replay_start() {
  [ "$NO_REPLAY" = 1 ] && return 0
  "$A" shell mkdir -p "$DEVICE_DIR" >/dev/null
  "$A" push "$ROUTE" "$DEVICE_DIR/" >/dev/null
  "$A" shell am broadcast -a "$PKG.DEMO_ROUTE" -p "$PKG" --es file "$(basename "$ROUTE")" --ef rate "$RATE" >/dev/null
}

replay_stop() {
  [ "$NO_REPLAY" = 1 ] && return 0
  "$A" shell am broadcast -a "$PKG.DEMO_ROUTE_STOP" -p "$PKG" >/dev/null || true
}

# 경로 시작 후 경로 시각 t(초)가 될 때까지 기다린다(배속 반영)
wait_until() {
  local target_ms remain
  target_ms=$(awk -v t="$1" -v r="$RATE" 'BEGIN { printf "%d", t / r * 1000 }')
  while :; do
    remain=$(( (target_ms - ($(date +%s%3N) - START_MS)) / 1000 ))
    [ "$remain" -le 0 ] && break
    printf '\r  다음 장면까지 %3d초 ' "$remain"
    sleep 1
  done
  printf '\r                         \r'
}

on_mark() {
  local mark=$1 seg=${1#*-}
  case "$mark" in
    announce-*)
      say "▶ 위험 구간 $seg 1 km 전 — 음성 안내 + 큰 글씨 배너 한 줄이 나와야 한다"
      ;;
    brake-*)
      say "▶ 구간 $seg: 끼어들기 → 브레이크 → 크루즈 해제. 이 해제는 기록된다(차속 ≥ 60 km/h)"
      vc 'controls?brake=50' 'controls?speed=90'
      release
      sleep 3
      vc 'controls?brake=0' 'controls?speed=100'
      engage
      say "  크루즈 다시 작동"
      ;;
    jam-*)
      say "▶ 구간 $seg: 정체 — ${JAM_HOLD}초 동안 30 km/h로 달린 뒤 크루즈 해제. 정체 해제라 기록하지 않는다(로그 확인)"
      vc 'controls?speed=30'
      sleep "$JAM_HOLD"
      release
      sleep 3
      vc 'controls?speed=100'
      engage
      say "  정체 해소, 100 km/h로 크루즈 다시 작동"
      ;;
  esac
}

drive() {
  [ -f "$ROUTE" ] || { echo "경로 파일 없음: $ROUTE" >&2; exit 1; }
  reset_state
  scene "① 정차 중: 위험 구간 목록 화면(시드 데이터: 판교~양재 3곳)을 보여 준다"
  scene "② 출발: 기어 D, 100 km/h, 크루즈 작동. 위치 재생 ${RATE}배속 시작"
  vc 'gear?gear=Drive' 'controls?speed=100'
  sleep 1
  engage
  replay_start
  START_MS=$(date +%s%3N)

  local t mark last_t
  while IFS=, read -r t _ _ _ mark; do
    [ -n "$mark" ] || continue
    wait_until "$t"
    on_mark "$mark"
  done < <(tail -n +2 "$ROUTE" | tr -d '\r')

  last_t=$(tail -n 1 "$ROUTE" | cut -d, -f1)
  wait_until "$last_t"
  scene "③ 도착: 크루즈 해제, 정차, 기어 P → 주행 요약 화면(위험 구간 통과·안내 횟수·내 기록)"
  release
  vc 'controls?speed=0'
  sleep 2
  vc 'gear?gear=Park'
  replay_stop
}

bash tools/vconsole.sh up
case "${1:-}" in
  "") drive ;;
  reset) reset_state ;;
  stop) replay_stop; release; vc 'controls?speed=0' 'gear?gear=Park' ;;
  *) sed -n '2,25p' "$0"; exit 1 ;;
esac
say "끝. 하트비트는 계속 돈다(그만두려면: bash tools/vconsole.sh down)"
