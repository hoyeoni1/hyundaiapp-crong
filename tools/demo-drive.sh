#!/usr/bin/env bash
# 크루즈브레이커 시연 시나리오(PRD 7절, 판정 규칙 v2 docs/disengage-rules.md). 2차 PT·시연 영상용.
#
# usage:
#   bash tools/demo-drive.sh          # 고정 위치 시연(지금 앱으로 동작): 출발 → 500m 앞 안내 → 해제 기록 → 저속 해제는 기록 안 함 → 정차 요약
#   bash tools/demo-drive.sh route    # 경로 재생 시연(앱 위치 재생 모드가 생긴 뒤): 판교→양재를 달리며 같은 장면
#   bash tools/demo-drive.sh reset    # 시작 상태로만 맞춤(P, 정차, 크루즈 대기)
#   bash tools/demo-drive.sh stop     # 위치 재생 중지 + 정차
#
# 전제:
#   - 에뮬레이터 실행, 크루즈브레이커 **디버그 빌드** 설치
#   - **앱을 켜면 S5 동의 화면 → 동의 2개 + [시작하기] → S2**. 동의 전에는 기록·안내가 돌지 않는다. 확정 순서는 docs/demo-scenario.md
#   - 차속 권한은 이 스크립트가 준다(pm grant). 차속을 모르면 앱이 속도 조건을 건너뛰어 저속 해제도 기록된다
#   - 고정 위치: 에뮬레이터 위치(판교) 앞 400m의 시드 "판교IC 진입로"가 해제 다발 구간이다. PT에서 시드 데이터임을 밝힌다
#   - 기록 확인: adb logcat -s CruiseRecord
#
# 환경변수:
#   PAUSE=1        장면마다 Enter를 기다림(발표자가 설명하며 넘길 때. 경로 재생은 계속 흐르므로 route에서는 쓰지 않는다)
#   ANNOUNCE_WAIT=8  고정 위치: 크루즈 작동 후 안내(배너·음성)를 보여 주는 시간(초)
#   SLOW_HOLD=5    저속 장면에서 40 km/h로 달리는 시간(초). 작동 3초 이상 유지 조건보다 길게
#   USER_ID=10     에뮬레이터 사용자(Pleos 에뮬레이터는 10)
#   RATE=1         route: 경로 재생 배속(1 = 실제 100 km/h, 약 7분). 안내는 3분에 한 번뿐이라 2배속이면 c 안내가 안 나온다
#   NO_REPLAY=1    route: 앱 위치 재생 없이 콘솔 값만 주입(재생 모드가 없을 때 신호 순서만 확인)
#   ROUTE=tools/demo/gyeongbu-north.csv   (만들기: python tools/demo/make-route.py > ...)
#
# route 모드의 앱 쪽 계약(디버그 빌드에 아직 없음, 구현할 것):
#   - 이 스크립트가 경로 파일을 /sdcard/Android/data/com.hyundaiapp.cruisebreaker/files/demo/ 에 adb push 한다
#   - 시작: am broadcast -a com.hyundaiapp.cruisebreaker.DEMO_ROUTE -p com.hyundaiapp.cruisebreaker --es file <이름> --ef rate <배속>
#     → 앱은 CSV(t,lat,lon,heading,mark)를 t/rate 초마다 한 줄씩 읽어 NaviHelper 위치 대신 쓴다
#   - 중지: am broadcast -a com.hyundaiapp.cruisebreaker.DEMO_ROUTE_STOP -p com.hyundaiapp.cruisebreaker
#
# 콘솔 매핑(findings 6.2.1): full_autonomous → ACTIVATED(작동), ready → ENABLED(대기). 실차 해제 값은 재현 불가라
# ACTIVATED → ENABLED를 해제로 본다(CruiseStateInterpreter). 해제로 세는 조건: 작동 3초 이상 + 해제 순간 차속 ≥ 60 km/h
set -euo pipefail
cd "$(dirname "$0")/.."

A=${ADB:-adb}
PAUSE=${PAUSE:-0}
ANNOUNCE_WAIT=${ANNOUNCE_WAIT:-8}
SLOW_HOLD=${SLOW_HOLD:-5}
USER_ID=${USER_ID:-10}
RATE=${RATE:-1}
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
wait_s() { local n=$1; while [ "$n" -gt 0 ]; do printf '\r  %3d초 ' "$n"; sleep 1; n=$((n - 1)); done; printf '\r          \r'; }
engage() { vc 'driving-mode?ad_mode_state=full_autonomous'; }   # 크루즈 작동(ACTIVATED)
release() { vc 'driving-mode?ad_mode_state=ready'; }           # 크루즈 해제(ENABLED)

grant_speed() {
  "$A" shell pm grant --user "$USER_ID" "$PKG" android.car.permission.CAR_SPEED >/dev/null 2>&1 \
    || say "※ 차속 권한을 못 줬다(앱 설치·USER_ID 확인). 이대로면 저속 해제도 기록된다"
}

reset_state() {
  scene "시작 상태: 기어 P, 정차, 크루즈 대기"
  vc 'gear?gear=Park' 'controls?speed=0' 'controls?brake=0' 'driving-mode?ad_mode_state=ready' \
     'driving-mode?target_speed_mps=27.8'
  sleep 2
}

# 해제 기록 장면: 끼어들기 → 브레이크 → 해제(100 km/h 부근이라 기록됨) → 다시 작동
brake_release() {
  vc 'controls?brake=50' 'controls?speed=90'
  release
  sleep 3
  vc 'controls?brake=0' 'controls?speed=100'
  engage
}

# 저속 해제 장면: 40 km/h로 줄인 뒤 해제 → 차속 < 60 km/h라 기록하지 않는다(v2, 정차·정체 중 자동 해제를 빼는 조건)
slow_release() {
  vc 'controls?speed=40'
  wait_s "$SLOW_HOLD"
  release
  sleep 3
  vc 'controls?speed=100'
  engage
}

arrive() {
  scene "도착: 정차, 크루즈 해제, 기어 P → 주행 요약 화면(해제 다발 구간 통과·안내 횟수·내 기록)"
  # 먼저 세운다. 60km/h 이상에서 해제하면 도착도 해제로 기록된다(규칙 v2)
  vc 'controls?speed=0'
  sleep 2
  release
  sleep 1
  vc 'gear?gear=Park'
}

# 고정 위치: 위치는 그대로, 콘솔 값만 바꾼다(이민하 확인 순서와 같음)
fixed() {
  grant_speed
  reset_state
  scene "① 정차 중: 해제 다발 구간 목록 화면(S2)을 보여 준다(시드 데이터)"
  scene "② 출발: 기어 D, 100 km/h, 크루즈 작동 → \"500m 앞 크루즈 해제 잦은 구간\" 배너 + 음성"
  vc 'gear?gear=Drive' 'controls?speed=100'
  sleep 1
  engage
  wait_s "$ANNOUNCE_WAIT"
  scene "③ 끼어들기 → 브레이크 → 크루즈 해제. 100 km/h 부근 해제라 기록된다(logcat CruiseRecord)"
  brake_release
  say "  크루즈 다시 작동"
  sleep 4
  scene "④ 40 km/h로 줄인 뒤 크루즈 해제. 60 km/h 미만이라 기록하지 않는다(로그에 기록 없음)"
  slow_release
  say "  100 km/h로 크루즈 다시 작동"
  sleep 3
  arrive
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
# date +%s%3N은 GNU date(Git Bash·Linux) 전용. macOS 기본 date에서는 안 된다
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
      say "▶ 해제 다발 구간 $seg 500 m 전 — 음성 안내 + 배너 한 줄이 나와야 한다"
      ;;
    brake-*)
      say "▶ 구간 $seg: 끼어들기 → 브레이크 → 크루즈 해제. 이 해제는 기록된다(차속 ≥ 60 km/h)"
      brake_release
      say "  크루즈 다시 작동"
      ;;
    slow-*)
      say "▶ 구간 $seg: 40 km/h로 줄인 뒤 크루즈 해제. 60 km/h 미만이라 기록하지 않는다(로그 확인)"
      slow_release
      say "  100 km/h로 크루즈 다시 작동"
      ;;
  esac
}

route() {
  [ -f "$ROUTE" ] || { echo "경로 파일 없음: $ROUTE" >&2; exit 1; }
  grant_speed
  reset_state
  scene "① 정차 중: 해제 다발 구간 목록 화면(시드 데이터: 판교~양재 3곳)을 보여 준다"
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

  last_t=$(tail -n 1 "$ROUTE" | tr -d '\r' | cut -d, -f1)
  wait_until "$last_t"
  arrive
  replay_stop
}

case "${1:-}" in ""|route|reset|stop) ;; *) sed -n '2,/^set -euo/p' "$0" | sed '$d'; exit 1 ;; esac
bash tools/vconsole.sh up
case "${1:-}" in
  "") fixed ;;
  route) route ;;
  reset) reset_state ;;
  stop) replay_stop; vc 'controls?speed=0'; sleep 2; release; vc 'gear?gear=Park' ;;
  *) sed -n '2,/^set -euo/p' "$0" | sed '$d'; exit 1 ;;
esac
say "끝. 하트비트는 계속 돈다(그만두려면: bash tools/vconsole.sh down)"
