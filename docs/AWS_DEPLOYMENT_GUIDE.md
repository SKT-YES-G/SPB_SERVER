# AWS 배포 가이드 - Aegis Backend

## 전체 아키텍처

```
┌──────────────────────────────────────────────────────────────────────┐
│                        GitHub Actions                                │
│  main push → Build → ECR Push → ECS Fargate 자동 배포                │
└──────────────────────────────────────────────────────────────────────┘
                                ↓
┌──────────────────────────────────────────────────────────────────────┐
│                           AWS VPC                                    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Public Subnets                            │    │
│  │  ┌─────────────────────────────────────────────────────┐    │    │
│  │  │              ALB (aegis-alb)                         │    │    │
│  │  │  HTTPS:443 ─┬─ /api/*     → Spring Boot TG (:8080) │    │    │
│  │  │             ├─ /ai/*      → FastAPI TG (:8000)     │    │    │
│  │  │             └─ /*         → Frontend (S3/CloudFront)│    │    │
│  │  └─────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                   Private Subnets                            │    │
│  │                                                              │    │
│  │  ┌────────────────────┐    ┌────────────────────┐           │    │
│  │  │  Fargate Service 1 │    │  Fargate Service 2 │           │    │
│  │  │  Spring Boot :8080 │    │  FastAPI :8000      │           │    │
│  │  │  (aegis-backend)   │    │  (aegis-fastapi)    │           │    │
│  │  └────────┬───────────┘    └────────┬───────────┘           │    │
│  │           │                         │                        │    │
│  │  ┌────────┴─────────────────────────┴───────────┐           │    │
│  │  │              ElastiCache Redis               │           │    │
│  │  │              aegis-redis (:6379)              │           │    │
│  │  └──────────────────────────────────────────────┘           │    │
│  │           │                                                  │    │
│  │  ┌────────┴─────────────────────────────────────┐           │    │
│  │  │              RDS MySQL 8.0                    │           │    │
│  │  │              aegis-mysql (:3306)              │           │    │
│  │  └──────────────────────────────────────────────┘           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Route 53: aegis-xxx.xxx                                             │
│  ├─ aegis-xxx.xxx        → CloudFront (Frontend)                     │
│  └─ api.aegis-xxx.xxx    → ALB (Backend API + AI)                    │
└──────────────────────────────────────────────────────────────────────┘
```

## 진행 상황 체크리스트

| 단계 | 상태 | 설명 |
|------|------|------|
| 1. VPC 생성 | ✅ 완료 | aegis-vpc |
| 2. 보안 그룹 생성 | ✅ 완료 | 4개 생성됨 |
| 3. RDS 생성 | ✅ 완료 | aegis-mysql |
| 4. ElastiCache Redis 생성 | ✅ 완료 | aegis-redis |
| 5. ECR 리포지토리 생성 | ✅ 완료 | aegis/backend, aegis/ai |
| 6. 첫 Docker 이미지 푸시 | ✅ 완료 | |
| 7. ECS 클러스터 생성 | ✅ 완료 | aegis-cluster (Fargate) |
| 8. ECS 태스크 정의 생성 | ✅ 완료 | aegis-backend-task:10, aegis-fastapi-task:7 |
| 9. ECS 서비스 생성 | ✅ 완료 | 2개 서비스 RUNNING |
| 10. IAM 사용자 생성 | ✅ 완료 | github-actions-deployer |
| 11. GitHub Secrets 설정 | ✅ 완료 | |
| 12. GitHub Actions CI/CD | ✅ 완료 | |
| **13. ALB 보안 그룹 생성** | **** | |
| **14. ALB + 대상 그룹 생성** | ⬜ | |
| **15. ECS 서비스 재생성 (ALB 연결)** | ⬜ | |
| **16. 기존 EC2 리소스 정리** | ⬜ | 비용 절약! |
| 17. 도메인 등록 (Route 53) | ⬜ | |
| 18. ACM 인증서 발급 | ⬜ | |
| 19. ALB에 HTTPS 리스너 추가 | ⬜ | |
| 20. DNS 레코드 생성 | ⬜ | |

## 필요한 GitHub Secrets (총 10개)

| Secret 이름 | 설명 |
|-------------|------|
| `AWS_ACCESS_KEY_ID` | IAM 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | IAM 시크릿 키 |
| `DB_URL` | `jdbc:mysql://[RDS엔드포인트]:3306/aegis?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `admin` |
| `DB_PASSWORD` | RDS 비밀번호 |
| `REDIS_HOST` | ElastiCache Redis 엔드포인트 |
| `REDIS_PORT` | `6379` |
| `REDIS_SSL` | `true` (ElastiCache 전송 중 암호화 사용 시) |
| `JWT_SECRET` | JWT 시크릿 키 (256비트 이상) |
| `CORS_ORIGINS` | 허용 도메인 (예: `https://aegis-xxx.xxx`) |

---

# PART 1: AWS 인프라 생성 (✅ 완료)

> 이미 완료된 단계입니다. 참고용으로만 남겨둡니다.

---

## 1. VPC 생성 ✅

### 1.1 VPC 페이지 이동
1. https://console.aws.amazon.com 접속
2. 우측 상단에서 리전 **아시아 태평양 (서울) ap-northeast-2** 확인
3. 상단 검색창 클릭 → `VPC` 입력 → **VPC** 클릭

### 1.2 VPC 생성
1. 좌측 메뉴에서 **VPC** 클릭
2. 우측 상단 주황색 **VPC 생성** 버튼 클릭

### 1.3 설정 입력

**생성할 리소스** 섹션:
- [x] **VPC 등** 라디오 버튼 선택 (VPC만 아님!)

**이름 태그 자동 생성** 섹션:
- [x] **자동 생성** 체크됨 확인
- 입력창에 `aegis` 입력

**IPv4 CIDR 블록** 섹션:
- `10.0.0.0/16` 입력 (기본값)

**IPv6 CIDR 블록** 섹션:
- [x] **IPv6 CIDR 블록 없음** 선택

**테넌시** 섹션:
- **기본값** 선택 (기본값)

**가용 영역(AZ) 수** 섹션:
- **2** 선택

**퍼블릭 서브넷 수** 섹션:
- **2** 선택

**프라이빗 서브넷 수** 섹션:
- **2** 선택

**NAT 게이트웨이** 섹션:
- **1개의 AZ에서** 선택 (비용 절약)

> **중요**: Fargate 태스크가 프라이빗 서브넷에서 ECR 이미지를 풀하려면 NAT 게이트웨이가 필요합니다.

**VPC 엔드포인트** 섹션:
- **없음** 선택

3. 맨 아래 주황색 **VPC 생성** 버튼 클릭

---

## 2. 보안 그룹 생성 ✅

### 2.1 RDS용 보안 그룹: `aegis-rds-sg`

| 인바운드 규칙 유형 | 포트 | 소스 |
|------------------|------|------|
| MYSQL/Aurora | 3306 | `10.0.0.0/16` |

### 2.2 ECS용 보안 그룹: `aegis-ecs-sg`

| 인바운드 규칙 유형 | 포트 | 소스 |
|------------------|------|------|
| HTTP | 80 | `Anywhere-IPv4` |
| 사용자 지정 TCP | 8080 | `Anywhere-IPv4` |
| 사용자 지정 TCP | 8000 | `Anywhere-IPv4` |

### 2.3 Redis용 보안 그룹: `aegis-redis-sg`

| 인바운드 규칙 유형 | 포트 | 소스 |
|------------------|------|------|
| 사용자 지정 TCP | 6379 | `10.0.0.0/16` |

---

## 3. RDS 생성 ✅

- **DB 인스턴스 식별자**: `aegis-mysql`
- **엔진**: MySQL 8.0
- **인스턴스 클래스**: `db.t3.micro` (프리 티어)
- **초기 데이터베이스**: `aegis`
- **VPC**: `aegis-vpc`
- **보안 그룹**: `aegis-rds-sg`

---

## 4. ElastiCache Redis 생성 ✅

- **이름**: `aegis-redis`
- **노드 유형**: `cache.t4g.micro`
- **서브넷 그룹**: `aegis-redis-subnet-group` (프라이빗 서브넷)
- **보안 그룹**: `aegis-redis-sg`

---

## 5. ECR 리포지토리 생성 ✅

- `aegis/backend` (Spring Boot)
- `aegis/ai` (FastAPI)

---

## 6. 첫 번째 Docker 이미지 푸시 ✅

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com

# Spring Boot
docker build -t aegis/backend .
docker tag aegis/backend:latest [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend:latest
docker push [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend:latest

# FastAPI
docker build -t aegis/ai .
docker tag aegis/ai:latest [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/ai:latest
docker push [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/ai:latest
```

---

## 7. ECS 클러스터 생성 (Fargate) ✅

- **클러스터 이름**: `aegis-cluster`
- **인프라**: AWS Fargate (서버리스)

---

## 8. ECS 태스크 정의 생성 ✅

### Spring Boot: `aegis-backend-task`
- **시작 유형**: Fargate
- **CPU**: 0.5 vCPU / **메모리**: 1 GB
- **컨테이너**: `aegis-backend` (:8080)
- **환경 변수**: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, CORS_ORIGINS, SPRING_DATA_REDIS_HOST/PORT/SSL

### FastAPI: `aegis-fastapi-task`
- **시작 유형**: Fargate
- **CPU**: 0.5 vCPU / **메모리**: 1 GB
- **컨테이너**: `aegis-fastapi` (:8000)

---

## 9. ECS 서비스 생성 ✅ (ALB 없이 생성된 상태)

- `aegis-backend-service` — FARGATE, 1/1 태스크 실행 중
- `aegis-fastapi-task-service` — FARGATE, 1/1 태스크 실행 중

> **현재 문제**: ALB(로드 밸런서) 없이 서비스가 생성되어 외부에서 접근 불가.
> ECS 서비스는 생성 후 로드 밸런서를 추가할 수 없으므로, ALB 생성 후 서비스를 재생성해야 합니다.

---

# PART 2: GitHub Actions CI/CD 설정 (✅ 완료)

---

## 10. IAM 사용자 생성 ✅

- **사용자 이름**: `github-actions-deployer`
- **정책**: `AmazonEC2ContainerRegistryFullAccess`, `AmazonECS_FullAccess`

---

## 11. GitHub Secrets 설정 ✅

---

## 12. GitHub Actions CI/CD ✅

`main` 브랜치 push 시 자동 빌드/배포.

---


# PART 3: ALB 생성 및 Fargate 서비스 연결

> **왜 필요한가?**
> Fargate 태스크는 프라이빗 서브넷에서 실행되므로 외부에서 직접 접근할 수 없습니다.
> ALB(Application Load Balancer)가 앞에서 트래픽을 받아 Fargate 태스크로 전달해야 합니다.
>
> ```
> 사용자 → ALB (퍼블릭) → Fargate 태스크 (프라이빗)
> ```

---

## 13. ALB용 보안 그룹 생성

### 13.1 보안 그룹 페이지 이동
1. 상단 검색창 클릭 → `VPC` 입력 → **VPC** 클릭
2. 좌측 메뉴 스크롤 → **보안** 섹션 → **보안 그룹** 클릭
3. 우측 상단 주황색 **보안 그룹 생성** 버튼 클릭

### 13.2 설정 입력

**기본 세부 정보** 섹션:
- **보안 그룹 이름**: `aegis-alb-sg`
- **설명**: `ALB security group`
- **VPC**: 드롭다운에서 **aegis-vpc** 선택

**인바운드 규칙** 섹션:
1. **규칙 추가** 버튼 클릭 (2번 반복해서 2개 규칙 생성)

| # | 유형 | 포트 범위 | 소스 | 설명 |
|---|------|----------|------|------|
| 1 | HTTP | 80 | `Anywhere-IPv4` (0.0.0.0/0) | HTTP 트래픽 허용 |
| 2 | HTTPS | 443 | `Anywhere-IPv4` (0.0.0.0/0) | HTTPS 트래픽 허용 |

**아웃바운드 규칙** 섹션:
- 기본값 유지 (모든 트래픽 허용)

3. 맨 아래 주황색 **보안 그룹 생성** 버튼 클릭

> **메모**: ALB 보안 그룹 ID `sg-________________`

### 13.3 (선택) 기존 Fargate 보안 그룹 인바운드 규칙 수정

> 현재 `aegis-ecs-sg`는 어디서든(0.0.0.0/0) 8080/8000 접근을 허용합니다.
> 보안 강화를 원하면 소스를 `aegis-alb-sg`로 변경하여 ALB에서만 접근 가능하게 할 수 있습니다.
> 지금 당장은 건너뛰어도 됩니다.

---

## 14. ALB + 대상 그룹 생성

### 14.1 대상 그룹 생성 (Spring Boot)

1. 상단 검색창 → `EC2` 입력 → **EC2** 클릭
2. 좌측 메뉴 스크롤 → **로드 밸런싱** 섹션 → **대상 그룹** 클릭
3. 주황색 **대상 그룹 생성** 버튼 클릭

**기본 구성** 섹션:
- **대상 유형 선택**: `IP 주소` 선택

> **중요**: 반드시 `IP 주소`를 선택! Fargate는 `awsvpc` 네트워크 모드를 사용하므로 각 태스크가 고유 IP를 받습니다. `인스턴스`를 선택하면 Fargate와 연결이 안 됩니다.

- **대상 그룹 이름**: `aegis-backend-tg`
- **프로토콜**: `HTTP`
- **포트**: `8080`
- **IP 주소 유형**: `IPv4`
- **VPC**: 드롭다운에서 `aegis-vpc` 선택

**상태 검사** 섹션:
- **상태 검사 프로토콜**: `HTTP`
- **상태 검사 경로**: `/actuator/health`

> **핵심**: 이 경로로 ALB가 주기적으로 요청을 보내서 컨테이너가 살아있는지 확인합니다. Spring Boot의 Actuator 헬스체크 엔드포인트를 사용합니다.

**고급 상태 검사 설정** (클릭해서 펼치기):
- **정상 임계값**: `2` (2번 연속 성공하면 정상 판정)
- **비정상 임계값**: `3` (3번 연속 실패하면 비정상 판정)
- **제한 시간**: `10` 초
- **간격**: `30` 초
- **성공 코드**: `200`

4. 우측 하단 주황색 **다음** 클릭

**대상 등록** 단계:
- **아무것도 추가하지 않음!** (ECS 서비스가 자동으로 Fargate 태스크 IP를 등록합니다)

5. 우측 하단 주황색 **대상 그룹 생성** 클릭

### 14.2 대상 그룹 생성 (FastAPI)

1. 다시 주황색 **대상 그룹 생성** 버튼 클릭

**기본 구성** 섹션:
- **대상 유형 선택**: `IP 주소` 선택
- **대상 그룹 이름**: `aegis-fastapi-tg`
- **프로토콜**: `HTTP`
- **포트**: `8000`
- **IP 주소 유형**: `IPv4`
- **VPC**: `aegis-vpc` 선택

**상태 검사** 섹션:
- **상태 검사 프로토콜**: `HTTP`
- **상태 검사 경로**: `/health`

> FastAPI의 헬스체크 엔드포인트에 맞게 경로를 설정합니다. 만약 다른 경로를 사용한다면 그에 맞게 변경하세요.

**고급 상태 검사 설정**:
- Spring Boot와 동일하게 설정

4. **다음** 클릭
5. **대상 등록**: 아무것도 추가하지 않음
6. **대상 그룹 생성** 클릭

### 14.3 ALB 생성

1. 좌측 메뉴 **로드 밸런싱** 섹션 → **로드 밸런서** 클릭
2. 주황색 **로드 밸런서 생성** 클릭
3. **Application Load Balancer** 아래의 **생성** 클릭

**기본 구성** 섹션:
- **로드 밸런서 이름**: `aegis-alb`
- **체계**: `인터넷 경계` 선택 (외부에서 접근해야 하므로)
- **IP 주소 유형**: `IPv4`

**네트워크 매핑** 섹션:
- **VPC**: `aegis-vpc` 선택
- **매핑**: 가용 영역 2개 모두 체크하고, 각각 **퍼블릭 서브넷** 선택
  - `ap-northeast-2a` → `aegis-subnet-public1-ap-northeast-2a`
  - `ap-northeast-2c` → `aegis-subnet-public2-ap-northeast-2c`

> **중요**: 반드시 **퍼블릭** 서브넷을 선택! ALB는 외부 트래픽을 받아야 하므로 퍼블릭 서브넷에 배치합니다. 프라이빗 서브넷을 선택하면 외부에서 접근이 안 됩니다.

**보안 그룹** 섹션:
- **default** 보안 그룹이 선택되어 있으면 X 클릭하여 **제거**
- 드롭다운에서 `aegis-alb-sg` 선택 (13단계에서 생성한 것)

**리스너 및 라우팅** 섹션:

> 처음에는 HTTP:80만 설정합니다. HTTPS는 ACM 인증서 발급 후 추가합니다.

**리스너 1**:
- **프로토콜**: `HTTP`
- **포트**: `80`
- **기본 작업**: `대상 그룹으로 전달` 선택
- **대상 그룹**: 드롭다운에서 `aegis-backend-tg` 선택

> HTTP:80의 기본 대상을 Spring Boot로 설정합니다. FastAPI는 경로 규칙으로 분기합니다.

4. 맨 아래 주황색 **로드 밸런서 생성** 클릭
5. **2-3분 대기** (상태: "프로비저닝 중" → "활성")

### 14.4 ALB 리스너에 FastAPI 경로 규칙 추가

> ALB가 "활성" 상태가 된 후 진행합니다.

1. 로드 밸런서 목록에서 `aegis-alb` 클릭
2. 아래쪽 **리스너 및 규칙** 탭 클릭
3. `HTTP:80` 리스너 클릭
4. **규칙 관리** 드롭다운 → **규칙 추가** 클릭

**규칙 추가** 화면:

**Step 1 - 이름 및 태그**:
- **이름**: `fastapi-routing`
- **다음** 클릭

**Step 2 - 조건 정의**:
- **조건 추가** 클릭
- **규칙 조건 유형**: `경로` 선택
- **경로 패턴**: `/ai/*` 입력
- **확인** 클릭
- **다음** 클릭

**Step 3 - 작업 정의**:
- **작업 유형**: `대상 그룹으로 전달` 선택
- **대상 그룹**: `aegis-fastapi-tg` 선택
- **다음** 클릭

**Step 4 - 규칙 우선순위 설정**:
- **우선순위**: `1` 입력
- **다음** 클릭

**Step 5 - 검토 및 생성**:
- **생성** 클릭

> **결과**: 이제 ALB가 다음과 같이 라우팅합니다:
> - `/ai/*` 요청 → FastAPI (`aegis-fastapi-tg`)
> - 나머지 모든 요청 → Spring Boot (`aegis-backend-tg`) — 기본 규칙

### 14.5 ALB DNS 이름 확인

1. 좌측 메뉴 **로드 밸런서** → `aegis-alb` 클릭
2. **설명** 탭에서 **DNS 이름** 복사

> **메모**: ALB DNS `aegis-alb-xxxx.ap-northeast-2.elb.amazonaws.com`

---

## 15. ECS 서비스 재생성 (ALB 연결)

> **중요**: ECS 서비스는 생성 후 로드 밸런서 설정을 변경할 수 없습니다.
> 따라서 기존 서비스를 삭제하고 ALB가 연결된 새 서비스를 생성해야 합니다.

### 15.1 기존 서비스 삭제

#### Spring Boot 서비스 삭제

1. ECS 콘솔 → 좌측 메뉴 **클러스터** → `aegis-cluster` 클릭
2. **서비스** 탭 클릭
3. `aegis-backend-service` 왼쪽 체크박스 선택
4. 우측 상단 **삭제** 버튼 클릭
5. 확인 창에서 `delete` 입력 → **삭제** 클릭
6. **1-2분 대기** (실행 중인 태스크가 중지될 때까지)

#### FastAPI 서비스 삭제

1. 같은 방법으로 `aegis-fastapi-task-service` 선택
2. **삭제** → `delete` 입력 → **삭제** 클릭
3. **1-2분 대기**

> **참고**: 서비스를 삭제해도 태스크 정의, ECR 이미지, 클러스터는 그대로 남아있습니다. 서비스만 재생성하면 됩니다.

### 15.2 Spring Boot 서비스 재생성 (ALB 연결)

1. `aegis-cluster` 클러스터에서 **서비스** 탭 → 주황색 **생성** 버튼 클릭

**환경** 섹션:
- **컴퓨팅 옵션**: `시작 유형` 선택
- **시작 유형**: `FARGATE` 선택

**배포 구성** 섹션:
- **애플리케이션 유형**: `서비스` 선택
- **태스크 정의**:
  - **패밀리**: 드롭다운에서 `aegis-backend-task` 선택
  - **개정**: `LATEST` (자동으로 최신 개정 선택됨)
- **서비스 이름**: `aegis-backend-service`
- **서비스 유형**: `복제본`
- **원하는 태스크**: `1`

**네트워킹** 섹션:
- **VPC**: `aegis-vpc`
- **서브넷**: **프라이빗 서브넷** 2개 선택
  - [x] `aegis-subnet-private1-ap-northeast-2a`
  - [x] `aegis-subnet-private2-ap-northeast-2c`
  - (퍼블릭 서브넷은 체크 해제!)
- **보안 그룹**:
  - **기존 보안 그룹 사용** 선택
  - **default** 제거 (X 클릭)
  - 드롭다운에서 `aegis-ecs-sg` 선택
- **퍼블릭 IP**: `꺼짐`

> **핵심**: Fargate 태스크를 프라이빗 서브넷에 배치합니다. 외부 접근은 ALB를 통해서만 가능하고, ECR 이미지 풀이나 외부 API 호출은 NAT 게이트웨이를 통해 가능합니다.

**로드 밸런싱** 섹션:
- **로드 밸런서 유형**: `Application Load Balancer` 선택
- **Application Load Balancer**: `기존 로드 밸런서 사용` 선택
- **로드 밸런서**: 드롭다운에서 `aegis-alb` 선택
- **리스너**: `기존 리스너 사용` 선택
- **리스너**: 드롭다운에서 `80:HTTP` 선택
- **대상 그룹**: `기존 대상 그룹 사용` 선택
- **대상 그룹 이름**: 드롭다운에서 `aegis-backend-tg` 선택

**배포 옵션** 섹션 (펼치기):
- **배포 유형**: `롤링 업데이트`
- **최소 실행 태스크 %**: `100`
- **최대 실행 태스크 %**: `200`

2. 맨 아래 주황색 **생성** 버튼 클릭
3. **3-5분 대기**

### 15.3 FastAPI 서비스 재생성 (ALB 연결)

1. 다시 **서비스** 탭 → 주황색 **생성** 버튼 클릭

**환경** 섹션:
- **시작 유형**: `FARGATE` 선택

**배포 구성** 섹션:
- **태스크 정의**: `aegis-fastapi-task` 선택 (개정: LATEST)
- **서비스 이름**: `aegis-fastapi-service`
- **원하는 태스크**: `1`

**네트워킹** 섹션:
- Spring Boot 서비스와 **동일하게 설정**
  - 프라이빗 서브넷 2개, `aegis-ecs-sg`, 퍼블릭 IP 꺼짐

**로드 밸런싱** 섹션:
- **로드 밸런서 유형**: `Application Load Balancer` 선택
- **기존 로드 밸런서 사용**: `aegis-alb` 선택
- **리스너**: `80:HTTP` 선택
- **대상 그룹**: `기존 대상 그룹 사용` → `aegis-fastapi-tg` 선택

2. 맨 아래 주황색 **생성** 버튼 클릭
3. **3-5분 대기**

### 15.4 서비스 상태 확인

1. ECS 콘솔 → `aegis-cluster` → 각 서비스 클릭
2. **태스크** 탭에서 태스크 상태가 `RUNNING`인지 확인
3. **이벤트** 탭에서 `has reached a steady state` 메시지 확인

> 이 메시지가 나오면 서비스가 안정적으로 실행되고 있다는 뜻입니다.

### 15.5 ALB 대상 그룹 확인

1. EC2 콘솔 → 좌측 메뉴 **대상 그룹** 클릭
2. `aegis-backend-tg` 클릭 → **대상** 탭
3. 등록된 대상이 `healthy` 상태인지 확인
4. `aegis-fastapi-tg`도 같은 방법으로 확인

> **healthy**: 정상 (ALB ↔ Fargate 연결 성공)
> **unhealthy**: 비정상 (헬스체크 실패 — 헬스체크 경로, 보안 그룹 확인)
> **draining**: 빠지는 중 (이전 태스크가 종료되는 과정)

### 15.6 접속 테스트

```bash
# Spring Boot 헬스체크
curl http://aegis-alb-xxxx.ap-northeast-2.elb.amazonaws.com/actuator/health

# Swagger UI (브라우저에서)
http://aegis-alb-xxxx.ap-northeast-2.elb.amazonaws.com/swagger-ui/index.html

# FastAPI 헬스체크
curl http://aegis-alb-xxxx.ap-northeast-2.elb.amazonaws.com/ai/health
```

> `{"status":"UP"}` 응답이 오면 ALB 연결 성공!

### 15.7 트러블슈팅

**접속이 안 될 때 확인 순서:**

1. **ALB 상태 확인**: EC2 콘솔 → 로드 밸런서 → `aegis-alb` 상태가 `활성`인지
2. **대상 그룹 상태 확인**: 대상 그룹 → 대상 탭 → `healthy`인지
3. **보안 그룹 확인**:
   - `aegis-alb-sg`: 인바운드에 80, 443 허용되어 있는지
   - `aegis-ecs-sg`: 인바운드에 8080, 8000 허용되어 있는지
4. **서브넷 확인**: ALB는 퍼블릭 서브넷, Fargate는 프라이빗 서브넷인지
5. **ECS 태스크 로그**: ECS → 서비스 → 태스크 → 로그 탭에서 에러 확인

**대상 그룹이 unhealthy일 때:**
- 헬스체크 경로가 맞는지 확인 (Spring Boot: `/actuator/health`, FastAPI: `/health`)
- Fargate 보안 그룹이 ALB에서 오는 트래픽(8080/8000)을 허용하는지 확인
- 태스크 로그에서 컨테이너가 정상 기동되었는지 확인

---

## 16. 기존 EC2 리소스 정리 (비용 절약!)

> **중요**: Fargate로 전환했으므로 EC2 관련 리소스는 더 이상 필요 없습니다.
> EC2 인스턴스가 실행 중이면 **매일 비용이 발생**합니다!

### 16.1 ECS 컨테이너 인스턴스 등록 해제

> 클러스터에 등록된 EC2 컨테이너 인스턴스가 있으면 먼저 등록 해제합니다.

1. ECS 콘솔 → `aegis-cluster` 클릭
2. **인프라** 탭 클릭
3. **컨테이너 인스턴스** 목록에서 인스턴스 선택
4. **등록 해제** 클릭

### 16.2 Auto Scaling 그룹 삭제

1. 상단 검색창 → `EC2` 입력 → **EC2** 클릭
2. 좌측 메뉴 맨 아래 **Auto Scaling** 섹션 → **Auto Scaling 그룹** 클릭
3. `aegis-cluster` 관련 ASG 선택
4. **삭제** 클릭 → 확인

> ASG를 삭제하면 관리하던 EC2 인스턴스도 자동으로 종료됩니다.

### 16.3 EC2 인스턴스 종료 확인

1. EC2 콘솔 → 좌측 메뉴 **인스턴스** 클릭
2. `ECS Instance - aegis-cluster` 인스턴스가 `종료됨` 상태인지 확인
3. 만약 아직 `실행 중`이면:
   - 인스턴스 선택 → **인스턴스 상태** → **인스턴스 종료** 클릭

### 16.4 탄력적 IP 해제

> 사용하지 않는 탄력적 IP는 **비용이 발생**합니다! 반드시 해제하세요.

1. EC2 콘솔 → 좌측 메뉴 **네트워크 및 보안** → **탄력적 IP** 클릭
2. 할당된 탄력적 IP 선택
3. **작업** → **탄력적 IP 주소 연결 해제** 클릭 (인스턴스에 연결되어 있는 경우)
4. 다시 선택 → **작업** → **탄력적 IP 주소 릴리스** 클릭
5. **릴리스** 확인

### 16.5 키 페어 (선택사항)

- Fargate에서는 SSH를 사용하지 않으므로 키 페어는 필요 없습니다
- 삭제해도 되고, 나중을 위해 남겨둬도 비용은 없습니다

### 16.6 정리 확인

EC2 대시보드에서 다음을 확인:

| 리소스 | 정리 후 상태 |
|--------|-------------|
| 인스턴스(실행 중) | **0** |
| 로드 밸런서 | **1** (aegis-alb) |
| 보안 그룹 | 5개 (default, rds, ecs, redis, alb) |
| 볼륨 | **0** (인스턴스 종료 시 자동 삭제) |
| 탄력적 IP | **0** |
| Auto Scaling 그룹 | **0** |

---

# PART 4: Route 53 도메인 연결 + HTTPS

---

## 17. 도메인 등록 (Route 53)

> 이미 가지고 있는 도메인이 있으면 17단계를 건너뛰고 18단계로 이동하세요.

### 17.1 Route 53 페이지 이동
1. 상단 검색창 클릭 → `Route 53` 입력 → **Route 53** 클릭
2. 좌측 메뉴 **등록된 도메인** 클릭
3. 주황색 **도메인 등록** 버튼 클릭

### 17.2 도메인 검색

**도메인 이름 선택** 섹션:
1. 검색창에 원하는 도메인 입력 (예시):
   - `aegis-fire.com`
   - `aegis-119.com`
   - `aegis-rescue.com`
2. **검색** 버튼 클릭
3. 사용 가능한 도메인 확인 (체크 표시)
4. 원하는 도메인 옆 **선택** 버튼 클릭
5. **체크아웃으로 진행** 버튼 클릭

> **참고**: `.com` 도메인은 연간 약 $13 (약 17,000원), `.net`은 약 $11입니다.
> `.click`, `.link` 등은 $3~5로 저렴합니다.

### 17.3 연락처 정보 입력

**등록자 연락처** 섹션:
- 이름, 이메일, 전화번호 등 입력 (실제 정보)
- **개인 정보 보호**: `활성화` 선택 (WHOIS에 개인정보 숨김)

### 17.4 결제 및 등록
1. 약관 동의 체크
2. **주문 완료** 버튼 클릭
3. 이메일 인증 메일 확인 → 링크 클릭
4. **등록 완료까지 최대 10~30분** 소요

> **메모**: 등록한 도메인 `________________`

---

## 18. 호스팅 영역 생성

> 17단계에서 Route 53으로 도메인을 등록했다면, 호스팅 영역이 **자동 생성**됩니다.
> 이 경우 18단계를 건너뛰고 19단계로 이동하세요.
>
> 외부에서 구매한 도메인(가비아, Namecheap 등)을 사용하는 경우에만 이 단계가 필요합니다.

### 18.1 호스팅 영역 페이지 이동
1. Route 53 콘솔 → 좌측 메뉴 **호스팅 영역** 클릭
2. 주황색 **호스팅 영역 생성** 버튼 클릭

### 18.2 설정 입력

- **도메인 이름**: 구매한 도메인 입력 (예: `aegis-fire.com`)
- **설명**: `Aegis project` (선택사항)
- **유형**: **퍼블릭 호스팅 영역** 선택

3. 주황색 **호스팅 영역 생성** 버튼 클릭

### 18.3 네임서버 설정 (외부 도메인인 경우만)

> Route 53에서 도메인을 등록한 경우 이 단계는 불필요합니다.

1. 생성된 호스팅 영역 클릭
2. **NS** 레코드의 값 4개 확인
3. 도메인 구매한 사이트(가비아 등)에서 네임서버를 위 4개로 변경
4. **네임서버 전파까지 최대 24~48시간** 소요 (보통 1~2시간)

---

## 19. ACM 인증서 발급 (HTTPS)

> **중요**: ALB에서 HTTPS를 사용하려면 ACM 인증서가 필요합니다.
> 와일드카드로 발급하면 프론트엔드와 백엔드 모두 같은 인증서를 공유할 수 있습니다.

### 19.1 ACM 페이지 이동
1. 상단 검색창 → `ACM` 입력 → **Certificate Manager** 클릭
2. **인증서 요청** 버튼 클릭

### 19.2 설정 입력

**인증서 유형** 섹션:
- [x] **퍼블릭 인증서 요청** 선택 → **다음**

**도메인 이름** 섹션:
- **완전히 정규화된 도메인 이름**: `*.aegis-xxx.xxx` 입력 (와일드카드)
- **이 인증서에 다른 이름 추가** 클릭: `aegis-xxx.xxx` 입력 (루트 도메인)

> **핵심**: 와일드카드(`*`)로 발급하면 `api.aegis-xxx.xxx`, `www.aegis-xxx.xxx` 등 모든 서브도메인에서 사용 가능합니다.

**검증 방법** 섹션:
- [x] `DNS 검증` 선택

**키 알고리즘** 섹션:
- `RSA 2048` (기본값)

3. **요청** 버튼 클릭

### 19.3 DNS 검증
1. 인증서 목록에서 방금 생성한 인증서 클릭
2. **도메인** 섹션에서 **Route 53에서 레코드 생성** 버튼 클릭
3. **레코드 생성** 클릭
4. **5~30분 대기** (상태: "검증 보류 중" → "발급됨")

> **메모**: ACM 인증서 ARN `arn:aws:acm:ap-northeast-2:xxxx:certificate/xxxx`

---

## 20. ALB에 HTTPS 리스너 추가

### 20.1 HTTPS 리스너 추가

1. EC2 콘솔 → 좌측 메뉴 **로드 밸런서** → `aegis-alb` 클릭
2. **리스너 및 규칙** 탭 → **리스너 추가** 클릭

**리스너 세부 정보**:
- **프로토콜**: `HTTPS`
- **포트**: `443`

**기본 작업**:
- **대상 그룹으로 전달** 선택
- **대상 그룹**: `aegis-backend-tg` 선택

**보안 리스너 설정**:
- **보안 정책**: 기본값 유지
- **기본 SSL/TLS 서버 인증서**:
  - **인증서 소스**: `ACM에서` 선택
  - **인증서**: 19단계에서 발급받은 인증서 선택

3. **추가** 클릭

### 20.2 HTTPS 리스너에 FastAPI 경로 규칙 추가

> HTTP:80에 추가한 것과 동일한 규칙을 HTTPS:443에도 추가해야 합니다.

1. **리스너 및 규칙** 탭 → `HTTPS:443` 클릭
2. **규칙 관리** → **규칙 추가** 클릭

- **이름**: `fastapi-routing-https`
- **조건**: `경로` = `/ai/*`
- **작업**: `대상 그룹으로 전달` → `aegis-fastapi-tg`
- **우선순위**: `1`

3. **생성** 클릭

### 20.3 HTTP → HTTPS 리디렉션 설정

> 모든 HTTP 요청을 HTTPS로 자동 리디렉션합니다.

1. **리스너 및 규칙** 탭 → `HTTP:80` 클릭
2. **기본 규칙** 옆의 체크박스 선택 → **편집** 클릭 (또는 규칙 클릭 후 편집)
3. **작업** 섹션:
   - 기존 `대상 그룹으로 전달`을 **삭제**
   - `URL로 리디렉션` 선택
   - **프로토콜**: `HTTPS`
   - **포트**: `443`
   - **상태 코드**: `301 - 영구적으로 이동됨`
4. **변경 내용 저장** 클릭

> 이제 `http://`로 접속하면 자동으로 `https://`로 이동합니다.

---

## 21. DNS 레코드 생성 (도메인 → ALB 연결)

### 21.1 호스팅 영역 이동
1. Route 53 콘솔 → 좌측 메뉴 **호스팅 영역** 클릭
2. 도메인 이름 클릭 (예: `aegis-xxx.xxx`)

### 21.2 API 서브도메인 레코드 생성 (백엔드 + AI)

1. 주황색 **레코드 생성** 버튼 클릭

**레코드 이름** 섹션:
- 입력창에 `api` 입력 → 결과: `api.aegis-xxx.xxx`

**레코드 유형** 섹션:
- **A** 선택

**별칭** 토글:
- [x] **별칭** 켜기

**트래픽 라우팅 대상** 섹션:
- **Application/Classic Load Balancer에 대한 별칭** 선택
- **리전**: `아시아 태평양(서울) [ap-northeast-2]`
- **로드 밸런서**: 드롭다운에서 `aegis-alb` 선택 (`dualstack.aegis-alb-xxxx...`)

2. 주황색 **레코드 생성** 버튼 클릭

> **핵심**: Fargate에서는 Elastic IP 대신 ALB를 별칭(Alias)으로 연결합니다.

### 21.3 프론트엔드 도메인 레코드 생성

> 프론트엔드 배포 방식에 따라 설정이 달라집니다.

**방법 A: CloudFront + S3 (권장)**

1. **레코드 생성** 버튼 클릭
2. **레코드 이름**: 비워두기 (루트 도메인 = `aegis-xxx.xxx`)
3. **레코드 유형**: `A`
4. **별칭**: 켜기
5. **트래픽 라우팅 대상**: `CloudFront 배포에 대한 별칭` → CloudFront 배포 선택
6. **레코드 생성** 클릭

**방법 B: Vercel / Netlify 등 외부 호스팅**

1. **레코드 생성** 버튼 클릭
2. **레코드 이름**: 비워두기 (루트 도메인)
3. **레코드 유형**: `CNAME`
4. **값**: 외부 호스팅 서비스에서 제공하는 도메인 입력
5. **레코드 생성** 클릭

### 21.4 최종 DNS 구성 예시

| 레코드 이름 | 유형 | 라우팅 대상 | 용도 |
|------------|------|-----------|------|
| `aegis-xxx.xxx` | A (별칭) | CloudFront / Vercel | 프론트엔드 |
| `api.aegis-xxx.xxx` | A (별칭) | ALB (`aegis-alb`) | 백엔드 API + AI |

### 21.5 전파 확인

```bash
# DNS 조회 확인
nslookup api.aegis-xxx.xxx

# 실제 접속 테스트
curl https://api.aegis-xxx.xxx/actuator/health
curl https://api.aegis-xxx.xxx/ai/health
```

> `{"status":"UP"}` 응답이 오면 성공!

---

# PART 5: CORS 설정

---

## 22. CORS 설정 업데이트

프론트엔드와 백엔드가 다른 서브도메인을 사용하므로 CORS 설정이 필요합니다.

### 22.1 GitHub Secret 업데이트

1. GitHub 리포지토리 → **Settings** → **Secrets and variables** → **Actions**
2. `CORS_ORIGINS` Secret 수정 (없으면 새로 생성):
   - **Secret**: `https://aegis-xxx.xxx`

> 여러 도메인을 허용해야 하면 쉼표로 구분: `https://aegis-xxx.xxx,https://www.aegis-xxx.xxx`

### 22.2 배포 확인

CORS_ORIGINS가 변경되면 다음 배포 시 자동으로 반영됩니다.

```bash
# CORS 테스트
curl -H "Origin: https://aegis-xxx.xxx" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     https://api.aegis-xxx.xxx/api/auth/stations
```

---

## 23. 최종 접속 URL 정리

| 용도 | URL |
|------|-----|
| **프론트엔드** | `https://aegis-xxx.xxx` |
| **Spring Boot 헬스체크** | `https://api.aegis-xxx.xxx/actuator/health` |
| **Swagger UI** | `https://api.aegis-xxx.xxx/swagger-ui/index.html` |
| **Spring Boot API** | `https://api.aegis-xxx.xxx/api/` |
| **FastAPI (AI)** | `https://api.aegis-xxx.xxx/ai/` |

> HTTPS + 커스텀 도메인으로 포트 번호 없이 깔끔하게 접속 가능!

---

# 메모 정리

```
VPC ID: vpc-________________
RDS 보안 그룹 ID: sg-________________
ECS 보안 그룹 ID: sg-________________
ALB 보안 그룹 ID: sg-________________
Redis 보안 그룹 ID: sg-________________
RDS 엔드포인트: aegis-mysql.________________.ap-northeast-2.rds.amazonaws.com
RDS 비밀번호: ________________
Redis 엔드포인트: ________________
AWS 계정 ID (12자리): 338029836976
Backend ECR URI: 338029836976.dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend
AI ECR URI: 338029836976.dkr.ecr.ap-northeast-2.amazonaws.com/aegis/ai
ALB DNS: aegis-alb-________________.ap-northeast-2.elb.amazonaws.com
ACM 인증서 ARN: arn:aws:acm:________________
등록 도메인: ________________
프론트엔드 URL: https://________________
API URL: https://api.________________
IAM Access Key ID: AKIA________________
IAM Secret Access Key: ________________
```

---

## 배포 흐름 요약

```
[개발자가 main 브랜치에 push]
        ↓
[GitHub Actions 자동 실행]
        ↓
[Docker 이미지 빌드]
        ↓
[ECR에 이미지 푸시]
        ↓
[ECS 태스크 정의 업데이트]
        ↓
[ECS Fargate 서비스가 새 태스크 배포]
        ↓
[프라이빗 서브넷에서 컨테이너 실행]
        ↓
[ALB 헬스체크 통과 후 트래픽 전환]
        ↓
[Route 53 → ALB → Fargate 태스크로 요청 라우팅]
```

---

## EC2 vs Fargate 비교 (참고)

| 항목 | EC2 (이전) | Fargate (현재) |
|------|-----------|----------------|
| **서버 관리** | EC2 인스턴스 직접 관리 | AWS가 자동 관리 (서버리스) |
| **SSH 접속** | 가능 | 불가 (ECS Exec으로 대체) |
| **스케일링** | ASG 설정 필요 | 태스크 수만 조절하면 됨 |
| **고정 IP** | Elastic IP 사용 | ALB를 통해 접근 |
| **네트워크 모드** | bridge | awsvpc (ENI 할당) |
| **비용** | EC2 인스턴스 비용 | vCPU + 메모리 사용량 기반 |
| **보안** | EC2 + 컨테이너 모두 관리 | 컨테이너만 관리 |
| **패치/업데이트** | OS 패치 필요 | AWS가 자동 처리 |

### Fargate 디버깅: ECS Exec

SSH 접속이 불가하므로 컨테이너 내부 접근이 필요할 때:

```bash
# ECS Exec 활성화 (서비스 업데이트 시 --enable-execute-command 추가)
aws ecs update-service \
  --cluster aegis-cluster \
  --service aegis-backend-service \
  --enable-execute-command

# 컨테이너 접속
aws ecs execute-command \
  --cluster aegis-cluster \
  --task [TASK_ID] \
  --container aegis-backend \
  --interactive \
  --command "/bin/sh"
```

> **참고**: ECS Exec을 사용하려면 태스크 역할에 `ssmmessages` 관련 권한이 필요합니다.