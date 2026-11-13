#!/usr/bin/env bash
# 시연 영상용 실제 구동 녹화. 화면 조작(터치)·차량 값 주입·화면 녹화·주입 시각 기록을 한 번에 한다.
# 장면 구성은 docs/demo-video-scenario.md, 앱 동작 순서는 docs/demo-scenario.md(확정판)와 같다.
#
# usage:
#   bash tools/demo-video.sh crong        # 크롱: kr1(동의·목록) → drive(내비 위 안내 ~ 주차 뒤 요약, KR2·KR3)
#   bash tools/demo-video.sh chachelin    # 차슐랭: cr1(동의·홈) → cr2(인증 충전·리뷰) → cr3(충전 실패 대기·실패 기록·고르기·길안내)
#
# 결과 (OUT 아래 앱별 폴더):
#   <구간>.mp4      에뮬레이터 전체 화면 녹화(2560x1440, 소리 없음 — adb screenrecord는 소리를 못 담는다)
#   events.tsv      구간 이름, 녹화 시작부터의 초, 한 일. 편집에서 자를 위치·배속 구간의 기준
#                   ("녹화 시작" 줄에는 PC 시각(유닉스 ms)도 적는다)
#   assets/*.png    블렌더 인포테인먼트 화면에 붙일 그림(각 녹화의 첫 장면)
#
# 전제:
#   - Pleos 에뮬레이터 실행, 대상 앱 설치(제출 release 또는 debug). 서버 키가 든 빌드면 실제 서버에 시연 기록이 올라간다
#   - **첫 실행 동의부터 찍는다.** 앱이 이미 동의된 상태면 먼저 앱의 [동의 철회]를 눌러 첫 실행 상태로 돌린다
#     (pm clear와 달리 그 설치가 서버에 올린 기록도 함께 지워진다)
#   - 녹화가 끝나면 다시 [동의 철회]를 눌러 이번 실행이 서버에 올린 기록을 지운다(WITHDRAW=0이면 남긴다)
#   - Git Bash에서 실행. 파이프(| tail)로 받지 말 것(하트비트가 출력을 붙잡는다)
#   - 음성 안내(TTS)는 PC 스피커로만 나온다. 소리까지 담으려면 PC 화면 녹화 도구를 같이 돌린다
#
# 환경변수:
#   OUT=build/demo-video   결과 폴더(저장소에 올리지 않는다, .gitignore의 build/)
#   USER_ID=10             에뮬레이터 사용자(Pleos 에뮬레이터는 10)
#   ADB=adb
#   KEEP_DATA=0            1이면 동의 상태를 그대로 두고 동의 구간(kr1·cr1)을 건너뛴다
#   PM_CLEAR=0             1이면 [동의 철회] 대신 pm clear로 지운다(화면이 꼬였을 때. 서버 기록은 남는다)
#   WITHDRAW=1             0이면 끝난 뒤 동의를 철회하지 않는다(앱에서 결과를 더 보려 할 때)
#   ANNOUNCE_WAIT=8 SLOW_HOLD=5          크롱: demo-drive.sh와 같은 뜻
#   STEP=3 FAIL_HOLD=35                  차슐랭: demo-charge.sh와 같은 뜻
#   DWELL=2.5              화면이 바뀐 뒤 보여 주는 시간(초)
set -euo pipefail
cd "$(dirname "$0")/.."

A=${ADB:-adb}
USER_ID=${USER_ID:-10}
OUT=${OUT:-build/demo-video}
KEEP_DATA=${KEEP_DATA:-0}
PM_CLEAR=${PM_CLEAR:-0}
WITHDRAW=${WITHDRAW:-1}
ANNOUNCE_WAIT=${ANNOUNCE_WAIT:-8}
SLOW_HOLD=${SLOW_HOLD:-5}
STEP=${STEP:-3}
FAIL_HOLD=${FAIL_HOLD:-35}
DWELL=${DWELL:-2.5}
DEV_DIR=/sdcard/demo-video
UI_XML=$(mktemp)
trap 'rm -f "$UI_XML"; rec_stop quiet || true' EXIT

APP=""          # crong | chachelin
PKG=""
SEG=""          # 지금 녹화 중인 구간
SEG_T0=0

# adb 호출. Git Bash가 /sdcard 같은 기기 경로를 Windows 경로로 바꾸지 않게 한다
# (전체에 걸면 vconsole.sh의 curl -o /dev/null 이 깨진다)
dev() { MSYS_NO_PATHCONV=1 "$A" "$@"; }
say() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
die() {
  say "실패: $*" >&2
  # 지금 화면에 보이는 글자(무엇을 기다리다 멈췄는지 알 수 있게)
  [ -s "$UI_XML" ] && grep -oE ' (text|content-desc)="[^"]+"' "$UI_XML" | head -25 | tr '
' ' ' >&2 && echo >&2
  exit 1
}
now_ms() { date +%s%3N; }
vc() { bash tools/vconsole.sh put "$@" >/dev/null; }

# ---------------------------------------------------------------- 화면 읽기·터치
ui_dump() {
  local i
  for i in 1 2 3 4 5; do
    if dev shell uiautomator dump "$DEV_DIR/ui.xml" 2>/dev/null | grep -q dumped; then
      dev shell cat "$DEV_DIR/ui.xml" | tr -d '\r' | sed 's/></>\n</g' > "$UI_XML"
      return 0
    fi
    sleep 0.5   # 화면이 움직이는 중이면 실패한다
  done
  return 1
}

# 글자(text) 또는 설명(content-desc)이 정확히 같은 n번째(기본 1) 요소의 가운데 좌표. ui_dump 뒤에 부른다
find_xy() {
  local want=$1 nth=${2:-1}
  awk -v want="$want" -v nth="$nth" '
    {
      t = ""; d = ""
      if (match($0, / text="[^"]*"/))         t = substr($0, RSTART + 7, RLENGTH - 8)
      if (match($0, / content-desc="[^"]*"/)) d = substr($0, RSTART + 15, RLENGTH - 16)
      if (t != want && d != want) next
      if (!match($0, / bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"/)) next
      b = substr($0, RSTART + 9, RLENGTH - 10)
      gsub(/[\[\]]/, " ", b); gsub(/,/, " ", b); split(b, p, " ")
      if (++n == nth) { printf "%d %d\n", (p[1] + p[3]) / 2, (p[2] + p[4]) / 2; exit }
    }' "$UI_XML"
}

# 앞부분이 같은 첫 요소(목록에서 "시흥동 주민센터 (시연)"처럼 뒤가 달라질 수 있는 이름)
find_xy_prefix() {
  local want=$1
  awk -v want="$want" '
    {
      t = ""
      if (match($0, / text="[^"]*"/)) t = substr($0, RSTART + 7, RLENGTH - 8)
      if (index(t, want) != 1) next
      if (!match($0, / bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"/)) next
      b = substr($0, RSTART + 9, RLENGTH - 10)
      gsub(/[\[\]]/, " ", b); gsub(/,/, " ", b); split(b, p, " ")
      printf "%d %d\n", (p[1] + p[3]) / 2, (p[2] + p[4]) / 2; exit
    }' "$UI_XML"
}

has_text() { ui_dump && [ -n "$(find_xy "$1")" ]; }

# 글자가 보일 때까지 기다린다(기본 20초)
wait_text() {
  local want=$1 limit=${2:-20} t0
  t0=$(date +%s)
  until has_text "$want"; do
    [ $(( $(date +%s) - t0 )) -ge "$limit" ] && die "\"$want\" 화면이 ${limit}초 안에 안 나왔다(구간 ${SEG:-준비})"
    sleep 0.5
  done
}

tap_xy() { dev shell input tap "$1" "$2"; }

# 글자를 찾아 누른다. tap_text <글자> [몇 번째]
tap_text() {
  local want=$1 nth=${2:-1} xy i
  for i in 1 2 3 4 5 6; do
    ui_dump && xy=$(find_xy "$want" "$nth") && [ -n "$xy" ] && break
    sleep 0.5
  done
  [ -n "${xy:-}" ] || die "누를 \"$want\"(${nth}번째)을 못 찾았다(구간 ${SEG:-준비})"
  # shellcheck disable=SC2086
  tap_xy $xy
  mark "누름: $want"
}

tap_prefix() {
  local want=$1 xy i
  for i in 1 2 3 4 5 6; do
    ui_dump && xy=$(find_xy_prefix "$want") && [ -n "$xy" ] && break
    sleep 0.5
  done
  [ -n "${xy:-}" ] || die "누를 \"$want…\"을 못 찾았다(구간 ${SEG:-준비})"
  # shellcheck disable=SC2086
  tap_xy $xy
  mark "누름: $want"
}

# 목록을 한 화면쯤 위로 넘긴다(앱 창 가운데에서)
scroll_down() { dev shell input swipe 1700 1000 1700 400 600; sleep 1; }

# ---------------------------------------------------------------- 녹화·기록
mark() {
  [ -n "$SEG" ] || return 0
  local t
  t=$(awk -v a="$(now_ms)" -v b="$SEG_T0" 'BEGIN { printf "%.1f", (a - b) / 1000 }')
  printf '%s\t%s\t%s\n' "$SEG" "$t" "$*" >> "$OUT/$APP/events.tsv"
  say "  $SEG +${t}s  $*"
}

rec_start() {
  SEG=$1
  dev shell rm -f "$DEV_DIR/$SEG.mp4"
  dev shell screenrecord --bit-rate 16000000 --time-limit 175 "$DEV_DIR/$SEG.mp4" >/dev/null 2>&1 &
  sleep 1.5     # 녹화가 실제로 돌기 시작할 때까지
  SEG_T0=$(now_ms)
  mark "녹화 시작 (PC 시각 ${SEG_T0} ms — 따로 녹음한 소리와 맞출 때 쓴다)"
}

rec_stop() {
  [ -n "$SEG" ] || return 0
  [ "${1:-}" = quiet ] || mark "녹화 끝"
  local seg=$SEG
  SEG=""
  dev shell pkill -INT screenrecord >/dev/null 2>&1 || true
  sleep 2       # mp4 마무리(moov) 쓰는 시간
  dev pull "$DEV_DIR/$seg.mp4" "$OUT/$APP/$seg.mp4" >/dev/null
  say "저장: $OUT/$APP/$seg.mp4"
}

shot() {
  mkdir -p "$OUT/assets"
  dev exec-out screencap -p > "$OUT/assets/$1.png"
  say "화면 그림: $OUT/assets/$1.png"
}

# ---------------------------------------------------------------- 앱 준비
launch() { dev shell am start --user "$USER_ID" -n "$PKG/.MainActivity" >/dev/null; }
open_maps() { dev shell am start --user "$USER_ID" -a android.intent.action.MAIN -c android.intent.category.APP_MAPS >/dev/null; }

# 길안내가 켜져 있으면 끈다(차슐랭 길안내 뒤 내비에 경로가 남는다)
end_guidance() {
  open_maps; sleep 2
  if has_text "안내 종료"; then tap_text "안내 종료"; sleep 1; fi
}

# 지금 화면이 첫 실행 동의인지, 동의 뒤 첫 화면($1)인지 기다려서 알려 준다
wait_first_screen() {
  local main=$1 t0
  t0=$(date +%s)
  while :; do
    if ui_dump; then
      [ -n "$(find_xy "첫 실행 동의")" ] && { echo consent; return 0; }
      [ -n "$(find_xy "$main")" ] && { echo main; return 0; }
    fi
    [ $(( $(date +%s) - t0 )) -ge 25 ] && die "앱 첫 화면이 안 떴다($PKG)"
    sleep 0.5
  done
}

prepare() {
  APP=$1; PKG=$2
  local main=$3
  dev shell pm path --user "$USER_ID" "$PKG" >/dev/null 2>&1 || die "$PKG 가 설치돼 있지 않다"
  mkdir -p "$OUT/$APP"
  : > "$OUT/$APP/events.tsv"
  dev shell mkdir -p "$DEV_DIR"
  bash tools/vconsole.sh up
  end_guidance
  dev shell am force-stop --user "$USER_ID" "$PKG"
  [ "$KEEP_DATA" = 1 ] && return 0
  if [ "$PM_CLEAR" = 1 ]; then
    dev shell pm clear --user "$USER_ID" "$PKG" >/dev/null
    return 0
  fi
  launch
  if [ "$(wait_first_screen "$main")" = main ]; then
    say "이미 동의된 상태 → 동의 철회로 첫 실행 상태로 돌린다"
    "withdraw_$APP"
    sleep 3     # 서버 삭제 요청이 나갈 시간
  fi
  dev shell am force-stop --user "$USER_ID" "$PKG"
}

# ================================================================ 크롱
engage() { vc 'driving-mode?ad_mode_state=full_autonomous'; }   # 크루즈 작동(ACTIVATED)
release() { vc 'driving-mode?ad_mode_state=ready'; }           # 크루즈 해제(ENABLED)

# S2(근처 해제 다발 구간)에서: 설정 → 동의 철회 → 철회
withdraw_crong() {
  tap_text "설정"
  wait_text "동의 철회"
  tap_text "동의 철회"
  wait_text "동의를 철회할까요?"
  tap_text "철회"
  wait_text "첫 실행 동의"
}

crong() {
  prepare crong com.hyundaiapp.crong "근처 해제 다발 구간"
  # 앱을 켜기 전에 차속 권한을 먼저 준다. 앱을 켠 뒤에 주면 다시 켜기 전까지 차속이 비어 저속 해제도 기록된다
  dev shell pm grant --user "$USER_ID" "$PKG" android.car.permission.CAR_SPEED >/dev/null 2>&1 \
    || say "※ 차속 권한을 못 줬다. 이대로면 40 km/h 해제도 기록된다"
  dev shell pm grant --user "$USER_ID" "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  vc 'gear?gear=Park' 'controls?speed=0' 'controls?brake=0' 'driving-mode?ad_mode_state=ready' \
     'driving-mode?target_speed_mps=27.8'
  open_maps; sleep 2
  launch

  if [ "$KEEP_DATA" != 1 ]; then
    # ---- kr1: 첫 실행 동의 → 해제 다발 구간 목록
    wait_text "첫 실행 동의"
    sleep 1
    shot crong_consent
    rec_start kr1
    sleep "$DWELL"
    tap_text "동의" 1; sleep 1
    tap_text "동의" 1; sleep 1.2     # 누른 항목은 "동의함"으로 바뀌어 남은 "동의"가 첫 번째가 된다
    tap_text "시작하기"
    wait_text "근처 해제 다발 구간"
    mark "S2 목록(거리 확인 중)"
    sleep "$DWELL"
    # 동의 직후 첫 조회 때는 위치를 아직 못 받아 "거리 확인 중"이다. 목록은 화면에 다시 들어올 때 새로 조회하므로
    # 설정에 다녀오면 가까운 순 + 거리로 바뀐다(편집에서 이 사이를 잘라도 되고, 설정 화면을 보여 줘도 된다)
    tap_text "설정"
    wait_text "동의 철회"
    mark "S4 설정"
    sleep "$DWELL"
    tap_text "뒤로"
    wait_text "0.4 km" 30
    mark "S2 목록(가까운 순, 0.4 km)"
    sleep 5
    rec_stop
  else
    wait_text "근처 해제 다발 구간"
  fi

  # ---- drive: 내비를 앞에 두고 출발 → 500m 앞 안내(KR2) → 해제 2번 → 주차 뒤 요약(KR3)
  open_maps; sleep 3
  shot crong_navi
  rec_start drive
  sleep "$DWELL"
  vc 'gear?gear=Drive' 'controls?speed=100'; mark "기어 D, 100 km/h"
  sleep 1
  engage; mark "크루즈 작동 → 500m 앞 안내(배너 10초·음성)"
  sleep "$ANNOUNCE_WAIT"
  vc 'controls?brake=50' 'controls?speed=90'; release; mark "브레이크, 90 km/h에서 해제(기록됨)"
  sleep 3
  vc 'controls?brake=0' 'controls?speed=100'; engage; mark "다시 작동 100 km/h"
  sleep 4
  vc 'controls?speed=40'; mark "40 km/h로 감속"
  sleep "$SLOW_HOLD"
  release; mark "40 km/h에서 해제(기록 안 함)"
  sleep 3
  vc 'controls?speed=100'; engage; mark "다시 작동 100 km/h"
  sleep 3
  vc 'controls?speed=0'; mark "정차"
  sleep 2
  release
  sleep 1
  vc 'gear?gear=Park'; mark "기어 P → 주행 요약"
  wait_text "최근 주행 요약"
  mark "S3 요약"
  sleep 5
  tap_text "해제 다발 구간 목록으로"
  wait_text "근처 해제 다발 구간"
  sleep 4
  rec_stop
  if [ "$WITHDRAW" = 1 ]; then
    withdraw_crong
    say "동의 철회: 이번 실행이 서버에 올린 기록 삭제를 요청했다"
  fi
}

# ================================================================ 차슐랭
port() { vc "ev-battery?field=charge_port_open&value=$1"; }
soc() { vc "ev-battery?field=soc&value=$1"; }

# 왼쪽 막대가 보이는 화면(홈·목록·내 기록)에서: 내 기록 → 동의 철회 → 철회
withdraw_chachelin() {
  tap_text "내 기록"
  wait_text "동의 철회"
  tap_text "동의 철회"
  wait_text "동의를 철회할까요?"
  tap_text "철회"
  wait_text "첫 실행 동의"
}

chachelin() {
  prepare chachelin com.hyundaiapp.chachelin "충전구를 열면 자동으로 기록해요"
  dev shell pm grant --user "$USER_ID" "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
  vc 'gear?gear=Park' 'controls?speed=0'
  port false; soc 40
  launch

  if [ "$KEEP_DATA" != 1 ]; then
    # ---- cr1: 첫 실행 동의 → 홈
    wait_text "첫 실행 동의"
    sleep 1
    shot chachelin_consent
    rec_start cr1
    sleep "$DWELL"
    tap_text "동의" 1; sleep 1
    tap_text "동의" 1; sleep 1      # 누른 항목은 "동의함"으로 바뀌어 남은 "동의"가 첫 번째가 된다
    tap_text "아이오닉 5"; sleep 1.2
    tap_text "시작하기"
    wait_text "충전구를 열면 자동으로 기록해요"
    mark "홈"
    sleep 4
    rec_stop
  else
    wait_text "충전구를 열면 자동으로 기록해요"
  fi
  sleep 3   # 앱을 켠 직후에는 기록 서비스가 SoC를 아직 못 받는다(확정 시나리오: 5초 뒤)

  # ---- cr2: 충전구 열림 → 자동 기록 → 40→80% → 인증 충전 완료 → 3초 리뷰 → 홈(내 충전 기록 1건)
  shot chachelin_home
  rec_start cr2
  sleep "$DWELL"
  port true; mark "충전구 열림"
  wait_text "충전 기록 중"
  mark "S3 충전 기록 중"
  sleep "$STEP"
  local s
  for s in 50 60 70 80; do
    soc "$s"; mark "SoC $s%"
    sleep "$STEP"
  done
  port false; mark "충전구 닫힘"
  wait_text "인증 충전 완료"
  mark "S4 인증 충전 완료"
  sleep "$DWELL"
  tap_text "5점"; sleep 0.8
  tap_text "빠름"; sleep 0.6
  tap_text "대기 없음"; sleep 1
  tap_text "평가 보내기"
  wait_text "1건"
  mark "홈: 내 충전 기록 1건"
  sleep 4
  rec_stop

  # ---- cr3: 충전 실패(시간은 가는데 SoC는 그대로) → 자동 기록 → 내 기록 → 목록 → 상세 비교 → 길안내
  # 실패 대기도 그대로 찍는다. 편집에서 이 구간만 빨리 감는다
  soc 40; sleep 1
  rec_start cr3
  sleep "$DWELL"
  port true; mark "충전구 열림(SoC 40% 그대로)"
  wait_text "충전 기록 중"
  mark "S3 충전 기록 중(40%)"
  say "실패 기준을 넘길 때까지 ${FAIL_HOLD}초 기다린다"
  sleep "$FAIL_HOLD"
  shot chachelin_charging40
  port false; mark "충전구 닫힘(충전 안 됨)"
  wait_text "충전 실패를 기록했어요"
  mark "S5 충전 실패 기록"
  sleep 3
  tap_text "닫기"
  wait_text "2건"
  mark "홈: 내 충전 기록 2건"
  sleep "$DWELL"
  tap_text "내 기록"
  wait_text "동의 철회"
  mark "S6 내 충전 기록"
  sleep 2.5
  tap_text "충전소"
  wait_text "근처 충전소"
  mark "S1 근처 충전소(점수순)"
  sleep 3
  tap_prefix "시흥동 주민센터"
  wait_text "길안내"
  mark "S2 시흥동 주민센터 상세(방금 실패 반영)"
  sleep 3
  tap_text "뒤로"
  wait_text "근처 충전소"
  sleep 1
  tap_prefix "대왕판교IC 휴게소"
  wait_text "길안내"
  mark "S2 대왕판교IC 휴게소 상세(별점은 높은데 점수 최하위)"
  sleep 3
  tap_text "뒤로"
  wait_text "근처 충전소"
  sleep 1
  tap_prefix "판교 테크노밸리 E-pit"
  wait_text "길안내"
  mark "S2 1위 충전소 상세"
  sleep 2.5
  tap_text "길안내"
  wait_text "길안내 시작"
  sleep 1.2
  tap_text "길안내 시작"
  mark "내비 경로"
  sleep 7
  rec_stop
  end_guidance
  if [ "$WITHDRAW" = 1 ]; then
    launch
    wait_text "길안내"        # 길안내를 누른 충전소 상세로 돌아온다
    withdraw_chachelin
    say "동의 철회: 이번 실행이 서버에 올린 기록 삭제를 요청했다"
  fi
}

case "${1:-}" in
  crong) crong ;;
  chachelin) chachelin ;;
  *) sed -n '2,/^set -euo/p' "$0" | sed '$d'; exit 1 ;;
esac
say "끝. 결과: $OUT/$APP  (하트비트 중지: bash tools/vconsole.sh down)"
