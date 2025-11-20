### 1. 교실 뒤쪽 아이들을 위한 '실시간 수업 자막 & 요약 보드'

**[문제의 본질: 물리적/인지적 소외]**
교실 뒤에 앉은 학생, ADHD가 있는 학생, 혹은 한국어가 서툰 다문화 학생은 선생님 말이 "웅웅"거리는 소음으로 들립니다. 수업을 **'듣는 것'** 자체가 안 되는데 내용을 이해할 수 있을까요?

선생님이 수업을 위한 방(우리 사이트의 방)을 만들어.
그리고 학생들은 거기에 들어가.(자신의 언어를 선택할 수 있음)

그래서 선생님의 말을 그 언어대로 자막으로 보여줘.

이로 인해서 멀리있거나, 외국인 학생들도 실시간 수업의 흐름을 따라갈 수 있음.

# MVP

1. 수업방 만들기
2. 수업방 실시간 처리
3. 학생이 수업 참여하기
4. 학생의 언어대로 번역되기(자막)
5. 선생님 말 저장까지 해야함.

이걸 위한 개발 계획서를 정리해줘

---

**실시간 수업 자막 & 요약 보드 (MVP) 개발 계획서**

이 계획서는 **선생님의 음성을 인식하여 텍스트로 변환하고, 이를 선택된 학생 언어로 실시간 번역하여 보여주며, 최종적으로 모든 기록을 저장**하는 핵심 기능을 중심으로 구성됩니다.

**1. 프로젝트 개요 및 목표**
**항목내용프로젝트명**실시간 수업 자막 & 요약 보드 (Classroom Live Caption & Summary Board)**목표**물리적/인지적 소외 학생을 위한 실시간 수업 접근성 확보**MVP 핵심 기능
**1. 수업 방 생성 및 접속 (선생님/학생 구분)2. 실시간 음성 인식 및 자막 출력 (한국어)3. 학생별 선택 언어로 실시간 번역 및 표시4. 수업 내용(원문)의 영구 저장**주요 기술 스택**
WebSockets (실시간 통신), Google Cloud Speech-to-Text API, Google Cloud Translation API (or equivalent), Firebase/Firestore (
데이터베이스)

**2. 기능별 개발 상세 계획 (MVP)**

**2.1. 사용자 인증 및 수업 방 관리 (Backend & Frontend)**
**기능 ID기능 명선생님 (Host)학생 (Client)비고F1.1수업 방 생성**수업명, 고유 접속 코드(UUID) 생성 후 Firestore에 저장N/A`addDoc` (Firestore)**F1.2수업 참여
**생성된 방 접속 및 마이크 접근 권한 확인방 코드 입력, 접속, 희망 자막 언어 선택`getDoc` (Firestore), **언어 선택 필수F1.3사용자 식별**선생님/학생 역할, 사용자 ID(`userId`)
표시선생님/학생 역할, 사용자 ID(`userId`) 표시Firestore의 `users` 컬렉션에 세션 정보 기록**F1.4방 목록 확인**본인이 생성한 방 목록 조회N/AFirestore `query`

**2.2. 실시간 음성 인식 및 번역 (Core Tech & Backend)**
**기능 ID기능 명선생님 (Host)학생 (Client)비고F2.1마이크 입력수업 시작 버튼**을 눌러 마이크 접근 및 녹음 시작N/A브라우저 `getUserMedia` API**F2.2실시간 STT**녹음된 음성
데이터를 Backend 서버로 전송N/AWebSockets, 스트리밍 **STT API** (예: Google) 사용**F2.3원문 자막 생성**STT API로부터 받은 한국어 텍스트를 WebSocket으로 모든
클라이언트에게 전송수신된 한국어 텍스트를 '원문' 자막으로 표시WebSockets Broadcast**F2.4실시간 번역**번역이 필요한 학생의 언어 설정을 기반으로 **Translation API**
호출N/ABackend에서 비동기 처리**F2.5번역 자막 표시**N/A수신된 번역 텍스트를 학생의 자막 언어 영역에 표시WebSockets (개별 전송/브로드캐스트 최적화 필요)

**2.3. 데이터 저장 (Persistence)**
**기능 ID기능 명선생님 (Host)학생 (Client)비고F3.1수업 기록 저장**수업 중 STT로 변환된 **최종 한국어 텍스트** (문장 단위)를 Firestore에 실시간 저장N/A`updateDoc` 또는
`addDoc` (수업 종료 후 일괄 저장 고려)**F3.2수업 종료수업 종료 버튼** 클릭 시 마이크/WebSocket 연결 종료 및 최종 기록 확정N/A**F3.3수업 기록 조회**종료된 수업의 원문 텍스트 전체
조회 및 다운로드 기능 (Scope Out for MVP)N/A

**3. 기술 스택 및 데이터 모델**

**3.1. 기술 스택**
**영역기술역할 및 사용 이유Frontend**HTML, Tailwind CSS, JavaScript사용자 인터페이스 구성 및 마이크 입력 처리**Backend/Server**Node.js (Express)
WebSocket 서버 구축, API 키 관리 및 비즈니스 로직 처리**Database**Firebase Firestore수업 방 정보, 사용자 세션 정보, 수업 기록 저장**Real-time**
WebSockets서버-클라이언트 간 실시간 양방향 통신 (음성/자막 전송)**AI/API**Google Cloud STT API한국어 음성 인식 (정확도 및 실시간 성능 우수)Google Cloud
Translation API학생 선택 언어로의 번역

**3.2. 데이터 모델 (Firestore)**

**A. `rooms` 컬렉션 (수업 방 정보)필드명타입설명**`roomId`String고유한 방 ID (Document ID)`roomName`String수업명`hostId`String선생님의
`userIdstatus`String`LOBBY`, `LIVE`, `ENDEDcreatedAt`Timestamp방 생성 시간`sessionUsers`Array현재 접속 중인 학생/선생님 ID 목록
**B. `sessions` 컬렉션 (수업 기록 저장)필드명타입설명**`sessionId`String고유한 세션 ID (Document ID, `roomId`와 동일할 수 있음)`roomId`String연결된
`roomIdtranscript`Array of Map시간 순서대로 저장되는 수업 원문 기록`transcript[i].timestamp`Timestamp발화 시작 시간`transcript[i].text`
String한국어 원문 텍스트`transcript[i].speakerId`String발화자 ID (선생님)

**4. 개발 단계 및 일정 (4주 기준)**
**주차단계주요 작업 내용결과물 (Artifact)1주차환경 구축 & 기본 UI/UX**Firebase 설정, Backend 서버(Node.js) 구축, 기본 UI/UX 디자인 (Tailwind), 수업 방
생성/참여 기능 구현수업 방 생성/접속 가능한 기본 웹 페이지**2주차실시간 통신 & STT 연동**WebSockets 서버-클라이언트 연동, 마이크 입력 및 데이터 전송 구현, Google STT API 연동 및
한국어 실시간 자막 출력선생님이 말하면 모든 클라이언트에게 한국어 자막이 실시간으로 표시**3주차다국어 번역 & 데이터 저장**학생 언어 선택 기능 구현, Translation API 연동 로직 구현 및 학생에게
개별 번역 자막 전송, 수업 원문(한국어) Firestore 저장학생별로 선택한 언어 자막 확인 가능, 수업 내용 DB 저장 확인**4주차테스트 및 개선**실시간 성능 테스트 및 최적화 (지연 시간), 버그 수정,
사용자 인증(Host/Client) 및 데이터 보안 규칙 검토안정적인 MVP 서비스 릴리스 준비

**5. 주요 기술적 과제 및 고려 사항**

1. **실시간 지연 최소화 (Latency):** 음성 인식 및 번역에는 필연적으로 지연 시간이 발생합니다. WebSockets을 이용해 지연 시간을 1초 이내로 줄이는 것이 핵심 과제입니다.
2. **API 비용 관리:** STT 및 Translation API는 사용량에 따라 비용이 발생합니다. 스트리밍 STT 설정 최적화 및 불필요한 API 호출 방지가 중요합니다.
3. **마이크 입력 처리:** 브라우저 환경에서 마이크 접근 권한, 오디오 샘플링 속도, 포맷 등을 일관성 있게 처리해야 합니다.
4. **보안:** `__app_id`, `__firebase_config`, `__initial_auth_token`을 활용하여 Firebase 인증 및 Firestore 보안 규칙을 설정하고, API Key는
   반드시 Backend 서버에서 관리하여 클라이언트에 노출되지 않도록 해야 합니다.
   이 개발 계획서를 바탕으로 핵심 기능을 먼저 구현하시고, 이후에 요약 기능, 기록 다운로드 등 추가 기능을 확장해 나가는 것을 추천드립니다. 프로젝트가 성공적으로 진행되기를 응원합니다!

프로젝트 목표:

1. 교수: 수업방 생성 → URL 제공
2. 학생: 들어와서 언어 선택하면 실시간 자막 번역 제공

Spring Boot + Java로 아래 API의 컨트롤러/서비스/DTO/Entity와 기본 골격을 만들어줘.

API:

1. POST /rooms → 방 생성, roomId 반환
2. GET /rooms/{roomId} → 방 정보 조회
3. POST /rooms/{roomId}/transcript → 실시간 STT 텍스트 저장
4. GET /rooms/{roomId}/transcript → 전체 자막 조회
5. POST /rooms/{roomId}/pdf → PDF 업로드
6. GET /rooms/{roomId}/pdf?lang=ko → 번역된 PDF 반환

조건:

- JPA 기반
- MySQL 사용
- DTO와 Entity 분리
- 모든 응답은 JSON

너는 Spring Boot 전문가 백엔드 개발자다. 나는 '교수-학생 간 실시간 자막 번역 시스템'의 백엔드 골격을 구축해야 한다.

요구사항:

1. **언어 및 스펙:** Java 17, Spring Boot 3.x, Spring Data JPA (H2 DB 사용 가정), Spring WebSocket/STOMP를 사용한다.
2. **핵심 기능:**
    - **방 생성 API:** `POST /rooms`
    - **실시간 소통:** WebSocket/STOMP를 이용한 자막 송수신 채널 구축.

---

**[요청 코드 및 상세 스펙]**

### 1. Entity: `Room.java`

- 필수 필드: `id` (Long, PK), `roomName` (String), `professorId` (String), `authCode` (String, 6자리 랜덤 생성)
- **요청:** Lombok(`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`)을 사용하여 코드를 간결하게 작성하고, JPA Entity로 선언한다.

### 2. DTOs: `RoomRequest.java`, `RoomResponse.java`, `SubtitleMessage.java`

- **`RoomRequest`:** 방 생성 요청 시 사용 (필드: `roomName`, `professorId`)
- **`RoomResponse`:** 방 생성 후 응답 시 사용 (필드: `roomId`, `roomName`, `accessUrl` (WebSocket 경로), `authCode`)
- **`SubtitleMessage`:** 실시간 자막 송수신 시 사용 (필드: `sourceLanguage`, `targetLanguage`, `originalText`, `translatedText`)
- **요청:** 모든 DTO는 Lombok을 사용하여 작성한다.

### 3. Repository: `RoomRepository.java`

- **요청:** `Room` Entity를 위한 기본적인 Spring Data JPA Repository 인터페이스를 작성한다.

### 4. Service: `RoomService.java`

- **기능:** `createRoom(RoomRequest request)` 메서드를 구현한다.
    - `authCode`를 6자리 임의 문자열로 생성하고, `Room` Entity를 저장한다.
    - `accessUrl`은 `/ws/lecture/{roomId}` 형식으로 Response에 포함한다.
- **요청:** `@Service` 어노테이션을 사용하고, `RoomRepository`를 생성자 주입한다.

### 5. Controller: `RoomController.java`

- **기능:** `POST /rooms` 엔드포인트를 구현하여 `RoomService.createRoom`을 호출하고 `ResponseEntity<RoomResponse>`를 반환한다.
- **요청:** `@RestController`, `@RequestMapping("/rooms")` 어노테이션을 사용한다.

### 6. WebSocket Config: `WebSocketConfig.java`

- **기능:** `@EnableWebSocketMessageBroker`를 활성화하고 `WebSocketMessageBrokerConfigurer`를 구현한다.
    - 브로커 접두사: `/sub`
    - 애플리케이션(Controller) 접두사: `/pub`
    - STOMP 엔드포인트: `/ws/lecture` (SockJS와 CORS 허용 패턴 설정)
- **요청:** `@Configuration` 어노테이션을 사용한다.

### 7. WebSocket Handler: `SubtitleHandler.java`

- **기능:** `@MessageMapping("/lecture/{roomId}")`를 구현한다.
    - 교수가 보낸 `SubtitleMessage`를 받는다.
    - **[TODO 주석]** 실제 번역 로직이 들어갈 위치를 명시한다.
    - `SimpMessagingTemplate`를 사용하여 구독 주소(`/sub/rooms/{roomId}`)로 번역된 메시지를 브로드캐스트한다. (번역 로직은 임시 텍스트로 대체)
- **요청:** `@Controller` 어노테이션을 사용하고, `SimpMessagingTemplate`를 주입받는다.

---

**최종 요청:** 위 7개 파일의 코드를 **각 파일별로 구분하여** 모두 제공해라. 그리고 `build.gradle`에 필요한 최소한의 의존성(Spring Web, JPA, H2, Lombok,
WebSocket) 설정도 추가해 줘.
