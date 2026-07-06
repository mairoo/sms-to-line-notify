# 핀코인 입금알리미 — Android Management API 배포 가이드

전용 단말(키오스크)로 `kr.co.pincoin.paynotify` 를 배포하기 위한 MDM 설정.
[Android Management API](https://developers.google.com/android/management) 를 직접 연동하는 방식(무료)이며,
상용 EMM 없이 코드/REST 호출만으로 단말을 등록·잠금·관리한다.

## 정책 요약 (`policy.json`)

| 항목 | 설정 | 의도 |
|---|---|---|
| `installType: KIOSK` | 이 앱만 실행되는 전용 모드 | 홈/다른 앱 차단, 자동 실행 |
| `permissionGrants` (SMS) | `RECEIVE_SMS`, `READ_SMS` = GRANT | 권한 팝업 없이 자동 승인 |
| `kioskCustomization` | 내비/상태바/설정 잠금 | 사용자가 못 빠져나감 |
| `factoryResetDisabled` 등 | 초기화·계정추가·USB·개발자옵션 차단 | 변조/이탈 방지 |
| `untrustedAppsPolicy: DISALLOW_INSTALL` | 외부 APK 설치 금지 | 다른 앱 못 넣음 |
| `stayOnPluggedModes` | 충전 중 화면 유지 | 상시 켜진 카운터 단말 |
| `autoUpdateMode: HIGH_PRIORITY` | 앱 자동 업데이트 | 원격 무중단 배포 |

> ⚠️ 계정 리스크 차단: 이 방식은 실제 Workspace 사용자 계정이 아니라
> **EMM이 발급한 기기 전용 관리형 Play 계정**으로 등록된다. Gmail/Drive/SSO 접근이 없어
> 단말을 넘겨도 계정으로 할 수 있는 일이 없다.

## 사전 준비 (1회)

1. **GCP 프로젝트**에서 Android Management API 활성화
   ```
   gcloud services enable androidmanagement.googleapis.com
   ```
2. **서비스 계정** 생성 + 키 발급 (백엔드가 API 호출에 사용)
3. **Enterprise 생성** — 관리형 Play Accounts 엔터프라이즈 등록
   (signup URL 흐름 또는 `enterprises.create`). 결과로 `enterprises/LC0xxxxxxx` 이름을 받는다.
4. **비공개 앱 게시** — 관리형 Google Play(Play Console 비공개 앱 또는 게시 iframe)에
   `kr.co.pincoin.paynotify` 를 올려야 policy 의 packageName 이 설치 가능해진다.

## 정책 적용

`{enterpriseId}` 를 채우고 policy 를 생성/갱신한다 (REST):

```
PATCH https://androidmanagement.googleapis.com/v1/enterprises/{enterpriseId}/policies/paynotify-kiosk
Authorization: Bearer $(gcloud auth print-access-token)
Content-Type: application/json

<policy.json 내용>
```

> API 는 알 수 없는 필드를 거부하므로, 먼저
> [API Explorer](https://developers.google.com/android/management/reference/rest/v1/enterprises.policies/patch)
> 에서 검증하면 오타/필드명을 바로 잡을 수 있다.

## 단말 등록 (기기마다)

1. **Enrollment token 생성** — 위 정책을 참조:
   ```
   POST https://androidmanagement.googleapis.com/v1/enterprises/{enterpriseId}/enrollmentTokens
   {
     "policyName": "enterprises/{enterpriseId}/policies/paynotify-kiosk",
     "duration": "3600s"
   }
   ```
   응답의 `qrCode` 필드(JSON 문자열)를 QR 이미지로 렌더링.
2. **공장초기화된 새 단말** → 최초 설정 환영화면을 **6번 탭** → QR 스캐너 실행 → QR 스캔
   (또는 계정 입력란에 `afw#setup`).
3. 단말이 **fully-managed(디바이스 오너)** 로 프로비저닝 → 정책 자동 적용 → 앱 자동 설치·키오스크 진입.
4. 대량이면 **zero-touch enrollment** 로 개봉 즉시 자동 등록(제거 불가)하도록 리셀러 등록.

## 운영

- **상태 확인**: `enterprises.devices.list` / Pub/Sub 알림으로 온라인·앱버전·마지막 동기화 수신
- **원격 조치**: `enterprises.devices.patch` 로 `LOCK` / `RESET_PASSWORD`, `enterprises.devices.delete` 로 등록 해제·와이프
   → 미납/오남용/분실 시 즉시 무력화

## 남은 캐비앳

- **배터리 최적화 예외 (유지 필수)**: 단말이 항상 충전 상태가 아니므로 Doze/앱 대기에서
  백그라운드 SMS 리시버가 죽을 수 있다. 따라서 `MainActivity` 의 배터리 최적화 예외 요청
  (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)은 **제거하지 말고 유지**한다. AMAPI 에 직접
  화이트리스트 필드는 없으므로 이 앱 내 예외 요청 흐름이 포워딩 신뢰성의 핵심이다.
- **FRP(공장초기화 보호)**: 강한 도난 방지가 필요하면 zero-touch 등록으로 기기를 조직에 묶는다.
