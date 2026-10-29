# 크루즈브레이커 (Cruise Breaker)

다른 차량들이 크루즈 컨트롤을 해제하는 지점을 모아, 운전자 개입이 반복되는 위험 구간을 진입 전에 알려 주는 Pleos Connect 앱.

- 핵심 신호: Vehicle SDK `DrivingMode` 크루즈 상태(`CruiseControlState`), 브레이크, 조향각
- 실측 메모: `../차량데이터_조사결과.md` 6.2.1 (크루즈 해제 값은 에뮬레이터로 재현 불가, 실차 값 문의 필요)
