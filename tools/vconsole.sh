#!/usr/bin/env bash
# Virtual Console(에뮬레이터 차량 값) 조작 도우미. findings 6.1 참고.
# usage:
#   tools/vconsole.sh up                      # adb 포트 포워딩 + 하트비트(백그라운드) 시작
#   tools/vconsole.sh put '<path?query>' ...  # 값 바꾸기. 예: put 'ev-battery?field=soc&value=55'
#   tools/vconsole.sh state                   # 현재 값 전체(JSON)
#   tools/vconsole.sh down                    # 하트비트 중지
# 환경변수: ADB(기본: PATH의 adb), CONSOLE_URL(기본: http://localhost:8088)
#
# 자주 쓰는 값
#   충전구     'ev-battery?field=charge_port_open&value=true'
#   SoC        'ev-battery?field=soc&value=60'
#   기어       'gear?gear=Drive'   (Park/Reverse/Neutral/Drive)
#   차속       'controls?speed=100'
#   브레이크   'controls?brake=50'
#   크루즈     'driving-mode?ad_mode_state=full_autonomous'  (작동=ACTIVATED) / 'ready' (=ENABLED)
#   크루즈속도 'driving-mode?target_speed_mps=27.8'
set -euo pipefail
A=${ADB:-adb}
URL=${CONSOLE_URL:-http://localhost:8088}
PIDFILE="${TMPDIR:-/tmp}/vconsole-heartbeat.pid"

case "${1:-}" in
  up)
    "$A" forward tcp:8088 tcp:8088 >/dev/null
    if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
      echo "하트비트 이미 실행 중"; exit 0
    fi
    # 콘솔은 1초마다 세션 신호가 없으면 쓰기를 503(emulator_inactive)으로 막는다
    ( while true; do curl -s -X POST "$URL/browser/session?client=cli" >/dev/null || true; sleep 1; done ) &
    echo $! > "$PIDFILE"
    echo "준비 완료: $URL (브라우저로 열어도 됨)"
    ;;
  put)
    shift
    for req in "$@"; do
      code=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$URL/vehicle/$req")
      echo "PUT $req -> $code"
      if [ "$code" = "503" ]; then echo "  → 'tools/vconsole.sh up' 먼저 실행" >&2; fi
    done
    ;;
  state)
    curl -s "$URL/vehicle/state"; echo
    ;;
  down)
    [ -f "$PIDFILE" ] && kill "$(cat "$PIDFILE")" 2>/dev/null || true
    rm -f "$PIDFILE"
    echo "하트비트 중지"
    ;;
  *)
    sed -n '2,20p' "$0"; exit 1
    ;;
esac
