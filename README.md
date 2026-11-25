<div align="center">

# 📍 모이미 (Moimi)

**약속 잡기부터 실시간 위치 공유, 채팅, 길찾기까지 한 번에 하는 모임 관리 안드로이드 앱**

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk_26-3DDC84?logo=android&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-Express_5-339933?logo=nodedotjs&logoColor=white)
![Socket.io](https://img.shields.io/badge/Socket.io-010101?logo=socketdotio&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-Mongoose-47A248?logo=mongodb&logoColor=white)
![Firebase](https://img.shields.io/badge/FCM-FFCA28?logo=firebase&logoColor=black)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?logo=amazons3&logoColor=white)

</div>

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 👥 **모임 관리** | 모임 생성, 이메일·링크로 초대, 참석 여부·출석 체크 |
| 🗓️ **일정** | 주간 캘린더, 일정 추가·상세, 참여 코드로 합류, 일정 알림 (AlarmManager) |
| 💬 **실시간 채팅** | Socket.io 기반 단체·1:1 채팅방, 읽음 처리 |
| 📡 **실시간 위치 공유** | 모임 멤버끼리 위치 공유·요청, 포그라운드 `LocationService` |
| 🧭 **길찾기 · 장소 검색** | TMAP API로 경로·POI 검색, 주변 맛집 추천 |
| 🌤️ **날씨** | OpenWeatherMap으로 약속 장소 날씨 표시 |
| 🔔 **푸시 알림** | FCM 알림, 알림함(읽음·삭제) |
| 👤 **마이페이지** | 프로필 이미지 업로드 (S3), 계정 찾기 |

## 🏗️ 아키텍처

```
[Android App (Kotlin)]
   ├─ Retrofit ───────────▶ [Express 5 REST API] ──▶ MongoDB (Mongoose)
   ├─ Socket.io client ───▶ [Socket.io: 채팅·위치]      └─▶ AWS S3 (프로필 이미지)
   └─ FCM ◀─────────────── firebase-admin (푸시)
```

### 서버 API

`/auth` (회원가입·로그인, JWT·bcrypt) · `/users` · `/meetings` · `/schedules` · `/chat` · `/invites` · `/notifications`

## 🧱 폴더 구조

```
app/src/main/java/com/moimiApp/moimi/
├─ *Activity.kt     # Main, Meeting, Schedule, ChatRoom, LocationShare, Route, Restaurant ...
├─ *Adapter.kt      # RecyclerView 어댑터
├─ LocationService.kt, SocketHandler.kt, RetrofitClient.kt
└─ MyFirebaseMessagingService.kt
server/
├─ routes/   # auth, user, meeting, schedule, chat, invite, notification
├─ models/   # User, Meeting, Schedule, Plan, Message, Notification, InviteLink
└─ index.js  # Express + Socket.io
```

## 🚀 실행

**서버**
```bash
cd server
npm install
node index.js    # .env에 MongoDB URI, JWT 시크릿, AWS·Firebase 자격 증명 필요
```

**앱**: Android Studio에서 열고 `Constants.kt`의 서버 주소와 TMAP·날씨 API 키를 설정한 뒤 실행합니다.
