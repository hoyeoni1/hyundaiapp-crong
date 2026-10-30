"""시연용 경부고속도로(판교→양재, 북행) 경로 파일을 만든다.

    python tools/demo/make-route.py > tools/demo/gyeongbu-north.csv

- 1초 간격, 100 km/h(27.8 m/s) 기준 좌표. 웨이포인트는 지도에서 눈으로 잡은 근사값이다.
- mark 열: 시연 스크립트(tools/demo-drive.sh)가 이 시점에 맞춰 콘솔 값을 바꾼다.
  - announce-<id>: 위험 구간 1 km 전(앱이 음성 안내를 해야 하는 지점)
  - brake-<id>: 위험 구간 중심 100 m 전(브레이크 → 크루즈 해제 → 기록됨)
  - jam-<id>: 위험 구간 중심 900 m 전(정체 시연 시작: 저속 후 해제 → 기록 안 됨)
- 위험 구간 중심은 앱 시드 데이터(FakeSegmentRiskRepository.SEED)와 같은 좌표다.
"""
import math

WAYPOINTS = [
    (37.3880, 127.1130),  # 판교IC 남쪽
    (37.4176, 127.0991),  # seed-a 판교JC 합류
    (37.4280, 127.0800),
    (37.4362, 127.0640),  # seed-b 서초IC 부근
    (37.4500, 127.0530),
    (37.4630, 127.0420),  # seed-c 양재IC 진출
    (37.4720, 127.0390),
]
SEGMENTS = {"a": (37.4176, 127.0991), "b": (37.4362, 127.0640), "c": (37.4630, 127.0420)}
# 구간별로 보여 줄 장면: a = 안내 + 브레이크 해제(기록), b = 정체 해제(기록 안 함), c = 안내만
SCENES = {"a": ["announce", "brake"], "b": ["jam"], "c": ["announce"]}
OFFSET_M = {"announce": 1000, "brake": 100, "jam": 900}
SPEED = 100 / 3.6
R = 6371000.0


def dist(p, q):
    la1, lo1, la2, lo2 = map(math.radians, (*p, *q))
    a = math.sin((la2 - la1) / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin((lo2 - lo1) / 2) ** 2
    return 2 * R * math.asin(math.sqrt(a))


def bearing(p, q):
    la1, lo1, la2, lo2 = map(math.radians, (*p, *q))
    y = math.sin(lo2 - lo1) * math.cos(la2)
    x = math.cos(la1) * math.sin(la2) - math.sin(la1) * math.cos(la2) * math.cos(lo2 - lo1)
    return (math.degrees(math.atan2(y, x)) + 360) % 360


def main():
    # 웨이포인트를 따라 누적 거리 → 좌표
    legs, total = [], 0.0
    for p, q in zip(WAYPOINTS, WAYPOINTS[1:]):
        d = dist(p, q)
        legs.append((total, d, p, q))
        total += d
    along_seg = {}
    for sid, c in SEGMENTS.items():
        start = 0.0
        for p, q in zip(WAYPOINTS, WAYPOINTS[1:]):
            if dist(p, c) < 1:
                along_seg[sid] = start
            start += dist(p, q)
        if dist(WAYPOINTS[-1], c) < 1:
            along_seg[sid] = start
    marks = {}
    for sid, scenes in SCENES.items():
        for s in scenes:
            marks[round((along_seg[sid] - OFFSET_M[s]) / SPEED)] = f"{s}-{sid}"

    print("t,lat,lon,heading,mark")
    for t in range(int(total / SPEED) + 1):
        s = t * SPEED
        for start, d, p, q in legs:
            if s <= start + d or (start, d, p, q) == legs[-1]:
                f = min(1.0, (s - start) / d)
                lat = p[0] + (q[0] - p[0]) * f
                lon = p[1] + (q[1] - p[1]) * f
                print(f"{t},{lat:.6f},{lon:.6f},{bearing(p, q):.0f},{marks.get(t, '')}")
                break


if __name__ == "__main__":
    main()
