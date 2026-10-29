#!/usr/bin/env bash
# usage: vtest.sh <label> <path?query> ...  — 각 PUT 후 2.5초간 들어온 PleosLsn 로그 출력
# 환경변수: ADB(기본: PATH의 adb), CONSOLE_URL(기본: http://localhost:8088)
A=${ADB:-adb}
CONSOLE_URL=${CONSOLE_URL:-http://localhost:8088}
body=$(mktemp)
trap 'rm -f "$body"' EXIT
while [ $# -ge 2 ]; do
  label="$1"; req="$2"; shift 2
  "$A" logcat -c
  code=$(curl -s -o "$body" -w '%{http_code}' -X PUT "$CONSOLE_URL$req")
  sleep 2.5
  echo "### $label  PUT $req -> $code $(head -c 160 "$body" | tr '\n' ' ')"
  "$A" logcat -d -s PleosLsn:I | grep -E "VEH|NAVI" | grep -v "onCurrentRoadSpeedLimit	0$" | cut -f2- | cut -c1-200 | sort | uniq -c | sort -rn | head -12
done
