# AWS 배포 가이드 - Aegis Backend

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      GitHub Actions                          │
│  main push → Build → ECR Push → ECS 자동 배포                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                         AWS VPC                              │
│  ┌──────────────────┐     ┌───────────────────────────┐     │
│  │ ElastiCache      │ ←── │  EC2 (ECS Container)      │     │
│  │ Redis (Private)  │     │  ├─ Spring Boot (:8080)   │     │
│  │ aegis-redis      │     │  └─ FastAPI (:8000) 예정  │     │
│  │ :6379            │     └─────────────┬─────────────┘     │
│  └──────────────────┘                   │                   │
│                                         │                   │
│  ┌──────────────────┐                   │                   │
│  │   RDS MySQL      │ ←─────────────────┘                   │
│  │   (Private)      │                                       │
│  │   aegis-mysql    │                                       │
│  │   :3306          │                                       │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

## 필요한 GitHub Secrets (총 5개)

| Secret 이름 | 설명 |
|-------------|------|
| `AWS_ACCESS_KEY_ID` | IAM 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | IAM 시크릿 키 |
| `DB_URL` | `jdbc:mysql://[RDS엔드포인트]:3306/aegis?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `admin` |
| `DB_PASSWORD` | RDS 비밀번호 |

---

# PART 1: AWS 인프라 생성

---

## 1. VPC 생성

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

**VPC 엔드포인트** 섹션:
- **없음** 선택

3. 맨 아래 주황색 **VPC 생성** 버튼 클릭
4. "VPC 생성 중..." 화면에서 2-3분 대기
5. 모든 항목에 녹색 체크 표시되면 완료

> **메모**: VPC ID `vpc-________________`

---

## 2. 보안 그룹 생성

### 2.1 보안 그룹 페이지 이동
1. 좌측 메뉴 스크롤 → **보안** 섹션 → **보안 그룹** 클릭
2. 우측 상단 주황색 **보안 그룹 생성** 버튼 클릭

### 2.2 RDS용 보안 그룹 생성

**기본 세부 정보** 섹션:
- **보안 그룹 이름**: `aegis-rds-sg`
- **설명**: `RDS MySQL`
- **VPC**: 드롭다운에서 **aegis-vpc** 선택

**인바운드 규칙** 섹션:
1. **규칙 추가** 버튼 클릭
2. 첫 번째 규칙:
   - **유형**: 드롭다운에서 `MYSQL/Aurora` 선택
   - **소스**: 드롭다운에서 `사용자 지정` 선택 → `10.0.0.0/16` 입력

**아웃바운드 규칙** 섹션:
- 기본값 유지 (모든 트래픽 허용)

3. 맨 아래 주황색 **보안 그룹 생성** 버튼 클릭

> **메모**: RDS 보안 그룹 ID `sg-________________`

### 2.3 ECS용 보안 그룹 생성

1. 다시 우측 상단 **보안 그룹 생성** 버튼 클릭

**기본 세부 정보** 섹션:
- **보안 그룹 이름**: `aegis-ecs-sg`
- **설명**: `ECS containers`
- **VPC**: 드롭다운에서 **aegis-vpc** 선택

**인바운드 규칙** 섹션:
1. **규칙 추가** 버튼 클릭 (4번 반복해서 4개 규칙 생성)

| 유형 | 포트 범위 | 소스 |
|------|----------|------|
| HTTP | 80 | `Anywhere-IPv4` (0.0.0.0/0) |
| 사용자 지정 TCP | 8080 | `Anywhere-IPv4` (0.0.0.0/0) |
| 사용자 지정 TCP | 8000 | `Anywhere-IPv4` (0.0.0.0/0) |
| SSH | 22 | `내 IP` 선택 |

2. 맨 아래 주황색 **보안 그룹 생성** 버튼 클릭

> **메모**: ECS 보안 그룹 ID `sg-________________`

---

## 3. RDS 생성

### 3.1 RDS 페이지 이동
1. 상단 검색창 클릭 → `RDS` 입력 → **RDS** 클릭
2. 좌측 메뉴 **데이터베이스** 클릭
3. 주황색 **데이터베이스 생성** 버튼 클릭

### 3.2 설정 입력

**데이터베이스 생성 방식 선택** 섹션:
- [x] **표준 생성** 선택

**엔진 옵션** 섹션:
- **엔진 유형**: MySQL 로고 클릭
- **에디션**: MySQL Community
- **엔진 버전**: `MySQL 8.0.35` (또는 최신 8.0.x)

**템플릿** 섹션:
- [x] **프리 티어** 선택 (중요! 비용 절약)

**설정** 섹션:
- **DB 인스턴스 식별자**: `aegis-mysql`
- **마스터 사용자 이름**: `admin`
- **자격 증명 관리**: **자체 관리** 선택
- **마스터 암호**: 원하는 비밀번호 입력 (메모 필수!)
- **마스터 암호 확인**: 같은 비밀번호 다시 입력

**인스턴스 구성** 섹션:
- **DB 인스턴스 클래스**: `db.t3.micro` (프리 티어 선택 시 자동)

**스토리지** 섹션:
- **스토리지 유형**: `범용 SSD (gp2)`
- **할당된 스토리지**: `20` GiB
- [ ] **스토리지 자동 조정 활성화** 체크 해제

**연결** 섹션:
- **컴퓨팅 리소스**: `EC2 컴퓨팅 리소스에 연결 안 함` 선택
- **네트워크 유형**: `IPv4`
- **Virtual Private Cloud(VPC)**: 드롭다운에서 **aegis-vpc** 선택
- **DB 서브넷 그룹**: `새 DB 서브넷 그룹 생성` (자동)
- **퍼블릭 액세스**: `아니요` 선택
- **VPC 보안 그룹(방화벽)**:
  - `기존 항목 선택` 선택
  - **default** 보안 그룹의 X 클릭하여 제거
  - 드롭다운에서 `aegis-rds-sg` 선택
- **가용 영역**: `기본 설정 없음`

**데이터베이스 인증** 섹션:
- [x] **암호 인증** 선택

**모니터링** 섹션:
- [ ] **향상된 모니터링 활성화** 체크 해제 (비용 절약)

**추가 구성** 섹션 (클릭해서 펼치기):
- **초기 데이터베이스 이름**: `aegis`
- [ ] **자동 백업 활성화** 체크 해제 (프리 티어에서 비용 절약)
- [ ] **암호화 활성화** 체크 해제
- [ ] **마이너 버전 자동 업그레이드 사용** 체크 해제

3. 맨 아래 주황색 **데이터베이스 생성** 버튼 클릭
4. **5-10분 대기** (상태가 "생성 중" → "사용 가능"으로 변경될 때까지)

### 3.3 RDS 엔드포인트 확인
1. 데이터베이스 목록에서 `aegis-mysql` 클릭
2. **연결 & 보안** 탭 클릭
3. **엔드포인트** 복사

> **메모**: RDS 엔드포인트 `aegis-mysql.xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com`
> **메모**: RDS 비밀번호 `________________`

---

## 4. ECR 리포지토리 생성

### 4.1 ECR 페이지 이동
1. 상단 검색창 클릭 → `ECR` 입력 → **Elastic Container Registry** 클릭
2. 좌측 메뉴 **프라이빗 레지스트리** → **리포지토리** 클릭
3. 주황색 **리포지토리 생성** 버튼 클릭

### 4.2 설정 입력

**일반 설정** 섹션:
- **표시 여부 설정**: `프라이빗` 선택
- **리포지토리 이름**: `aegis/backend`

**이미지 스캔 설정** 섹션:
- [x] **푸시 시 스캔** 체크

**암호화 설정** 섹션:
- **암호화 구성**: `AES-256` (기본값)

3. 맨 아래 주황색 **리포지토리 생성** 버튼 클릭

### 4.3 ECR URI 확인
1. 생성된 리포지토리 목록에서 **URI** 열 확인

> **메모**: ECR URI `123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend`
> **메모**: AWS 계정 ID `123456789012` (URI 맨 앞 12자리 숫자)

---

## 5. 첫 번째 Docker 이미지 푸시 (수동 - 필수!)

> **중요**: ECS 태스크 정의를 만들려면 ECR에 이미지가 최소 1개 있어야 합니다.

### 5.1 AWS CLI 설치 확인
```bash
aws --version
```
설치 안 되어 있으면: https://aws.amazon.com/cli/ 에서 설치

### 5.2 AWS CLI 설정
```bash
aws configure
```
- AWS Access Key ID: (IAM 사용자 키 - 9단계에서 생성)
- AWS Secret Access Key: (IAM 사용자 시크릿 키)
- Default region name: `ap-northeast-2`
- Default output format: `json`

> IAM 사용자가 없으면 먼저 9단계를 진행하세요.

### 5.3 ECR 로그인 및 푸시
```bash
# 1. ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com

# 2. 프로젝트 폴더로 이동
cd /path/to/SPB_SERVER

# 3. 이미지 빌드
docker build -t aegis/backend .

# 4. 태그 지정
docker tag aegis/backend:latest [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend:latest

# 5. 푸시
docker push [AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend:latest
```

> `[AWS_ACCOUNT_ID]`를 본인 AWS 계정 ID(12자리)로 변경

### 5.4 푸시 확인
1. AWS 콘솔 → ECR → `aegis/backend` 리포지토리 클릭
2. **이미지** 탭에서 `latest` 태그 이미지 확인

---

## 6. ECS 클러스터 생성

### 6.1 ECS 페이지 이동
1. 상단 검색창 클릭 → `ECS` 입력 → **Elastic Container Service** 클릭
2. 좌측 메뉴 **클러스터** 클릭
3. 주황색 **클러스터 생성** 버튼 클릭

### 6.2 설정 입력

**클러스터 구성** 섹션:
- **클러스터 이름**: `aegis-cluster`

**인프라** 섹션:
- [ ] **AWS Fargate (서버리스)** 체크 해제
- [x] **Amazon EC2 인스턴스** 체크

**Amazon EC2 인스턴스** 펼쳐서:

**Auto Scaling 그룹(ASG)** 섹션:
- **프로비저닝 모델**: `온디맨드` 선택
- **컨테이너 인스턴스 Amazon Machine Image(AMI)**: `Amazon Linux 2023` 선택
- **EC2 인스턴스 유형**: 드롭다운에서 `t3.small` 선택
- **원하는 용량**: `1`
- **SSH 키 페어**:
  - 기존 키 페어가 있으면 선택
  - 없으면 **새 키 페어 생성** 클릭 → 이름 입력 → 생성 → .pem 파일 다운로드

**네트워킹** 섹션:
- **VPC**: 드롭다운에서 **aegis-vpc** 선택
- **서브넷**:
  - `aegis-subnet-public1-ap-northeast-2a` 체크
  - `aegis-subnet-public2-ap-northeast-2c` 체크
  - (private 서브넷은 체크 해제!)
- **보안 그룹**:
  - **기존 보안 그룹 사용** 선택
  - 드롭다운에서 `aegis-ecs-sg` 선택
- **퍼블릭 IP 자동 할당**: `켜기` 선택

**모니터링** 섹션:
- [ ] **Container Insights 사용** 체크 해제 (비용 절약)

3. 맨 아래 주황색 **생성** 버튼 클릭
4. **3-5분 대기** (클러스터 + EC2 인스턴스 생성)

### 6.3 EC2 인스턴스 확인
1. 상단 검색창 → `EC2` → **인스턴스** 클릭
2. `ECS Instance - aegis-cluster` 인스턴스가 `실행 중` 상태인지 확인
3. **퍼블릭 IPv4 주소** 메모

> **메모**: EC2 퍼블릭 IP `________________`

---

## 7. ECS 태스크 정의 생성

### 7.1 태스크 정의 페이지 이동
1. ECS 콘솔 → 좌측 메뉴 **태스크 정의** 클릭
2. 주황색 **새 태스크 정의 생성** 버튼 클릭

### 7.2 설정 입력

**태스크 정의 구성** 섹션:
- **태스크 정의 패밀리**: `aegis-backend-task`

**인프라 요구 사항** 섹션:
- **시작 유형**:
  - [ ] AWS Fargate 체크 해제
  - [x] Amazon EC2 인스턴스 체크
- **네트워크 모드**: `bridge` 선택
- **CPU**: `0.5 vCPU` 선택
- **메모리**: `1 GB` 선택
- **태스크 역할**: 비워두기 (없음)
- **태스크 실행 역할**:
  - 드롭다운에서 `새 역할 생성` 선택
  - (이미 있으면 `ecsTaskExecutionRole` 선택)

**컨테이너 - 1** 섹션:
- **이름**: `aegis-backend`
- **이미지 URI**: `[AWS_ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend:latest`
- **필수 컨테이너**: `예` 선택

**포트 매핑** 섹션:
- **컨테이너 포트**: `8080`
- **호스트 포트**: `8080`
- **프로토콜**: `TCP`
- **앱 프로토콜**: `HTTP`

**환경 변수** 섹션 (중요!):
1. **환경 변수 추가** 버튼 3번 클릭해서 3개 추가

| 키 | 값 유형 | 값 |
|----|---------|-----|
| `DB_URL` | 값 | `jdbc:mysql://[RDS_ENDPOINT]:3306/aegis?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | 값 | `admin` |
| `DB_PASSWORD` | 값 | `[RDS 비밀번호]` |

> `[RDS_ENDPOINT]`와 `[RDS 비밀번호]`를 실제 값으로 변경!

**HealthCheck** 섹션 (펼치기 - 중요! 롤백 방지):
- **Command**: `CMD-SHELL, curl -f http://localhost:8080/actuator/health || exit 1`
- **Interval**: `30`
- **Timeout**: `10`
- **Start period**: `120` ← **핵심! Spring Boot 시작 시간 확보**
- **Retries**: `3`

**Docker 구성** 섹션:
- 기본값 유지

3. 맨 아래 주황색 **생성** 버튼 클릭

---

## 8. ECS 서비스 생성

### 8.1 서비스 생성 페이지 이동
1. ECS 콘솔 → **클러스터** → `aegis-cluster` 클릭
2. **서비스** 탭 클릭
3. 주황색 **생성** 버튼 클릭

### 8.2 설정 입력

**환경** 섹션:
- **컴퓨팅 옵션**: `시작 유형` 선택
- **시작 유형**: `EC2` 선택

**배포 구성** 섹션:
- **애플리케이션 유형**: `서비스` 선택
- **태스크 정의**:
  - **패밀리**: 드롭다운에서 `aegis-backend-task` 선택
  - **개정**: `LATEST` (자동)
- **서비스 이름**: `aegis-backend-service`
- **서비스 유형**: `복제본`
- **원하는 태스크**: `1`

**배포 옵션** 섹션 (펼치기):
- **배포 유형**: `롤링 업데이트`
- **최소 실행 태스크 %**: `0` (첫 배포 시 중요!)
- **최대 실행 태스크 %**: `100`
- [ ] **배포 실패 감지** 체크 해제 (첫 배포 시 롤백 방지)

**네트워킹**, **로드 밸런싱** 섹션:
- 모두 기본값 유지 (건드리지 않음)

3. 맨 아래 주황색 **생성** 버튼 클릭
4. **2-5분 대기**

### 8.3 서비스 상태 확인
1. `aegis-backend-service` 클릭
2. **태스크** 탭 클릭
3. 태스크 상태가 `RUNNING`인지 확인
4. 태스크를 클릭하면 로그 확인 가능

### 8.4 접속 테스트
```bash
# 터미널에서
curl http://[EC2_PUBLIC_IP]:8080/actuator/health
```

또는 브라우저에서:
```
http://[EC2_PUBLIC_IP]:8080/actuator/health
http://[EC2_PUBLIC_IP]:8080/swagger-ui.html
```

> `{"status":"UP"}` 응답이 오면 성공!

---

# PART 2: GitHub Actions CI/CD 설정

---

## 9. IAM 사용자 생성 (GitHub Actions용)

### 9.1 IAM 페이지 이동
1. 상단 검색창 → `IAM` 입력 → **IAM** 클릭
2. 좌측 메뉴 **사용자** 클릭
3. 우측 상단 **사용자 생성** 버튼 클릭

### 9.2 설정 입력

**사용자 세부 정보 지정** 단계:
- **사용자 이름**: `github-actions-deployer`
- [ ] **AWS Management Console에 대한 사용자 액세스 권한 제공** 체크 해제
- **다음** 버튼 클릭

**권한 설정** 단계:
- [x] **직접 정책 연결** 선택
- 검색창에서 다음 정책 검색하여 체크:
  - [x] `AmazonEC2ContainerRegistryFullAccess`
  - [x] `AmazonECS_FullAccess`
- **다음** 버튼 클릭

**검토 및 생성** 단계:
- **사용자 생성** 버튼 클릭

### 9.3 액세스 키 생성
1. 생성된 `github-actions-deployer` 사용자 클릭
2. **보안 자격 증명** 탭 클릭
3. **액세스 키** 섹션 → **액세스 키 만들기** 클릭
4. 사용 사례 선택:
   - [x] **Command Line Interface(CLI)** 선택
   - [x] **위의 권장 사항을 이해하고...** 체크
   - **다음** 클릭
5. 설명 태그 (선택사항): `github-actions`
6. **액세스 키 만들기** 클릭
7. **반드시 저장!** (이 화면을 벗어나면 다시 볼 수 없음)
   - **Access Key ID**: 복사해서 메모
   - **Secret Access Key**: **표시** 클릭 → 복사해서 메모

> **메모**: Access Key ID `AKIA________________`
> **메모**: Secret Access Key `________________________________________`

---

## 10. GitHub Secrets 설정

### 10.1 GitHub 리포지토리 이동
1. GitHub에서 `SPB_SERVER` 리포지토리 페이지 열기
2. **Settings** 탭 클릭 (⚙️ 아이콘)
3. 좌측 메뉴 스크롤 → **Secrets and variables** 클릭 → **Actions** 클릭
4. **New repository secret** 버튼 클릭

### 10.2 Secrets 추가 (총 5개)

**Secret 1:**
- **Name**: `AWS_ACCESS_KEY_ID`
- **Secret**: IAM Access Key ID 붙여넣기
- **Add secret** 클릭

**Secret 2:**
- **Name**: `AWS_SECRET_ACCESS_KEY`
- **Secret**: IAM Secret Access Key 붙여넣기
- **Add secret** 클릭

**Secret 3:**
- **Name**: `DB_URL`
- **Secret**: `jdbc:mysql://aegis-mysql.xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com:3306/aegis?useSSL=false&allowPublicKeyRetrieval=true`
- **Add secret** 클릭

**Secret 4:**
- **Name**: `DB_USERNAME`
- **Secret**: `admin`
- **Add secret** 클릭

**Secret 5:**
- **Name**: `DB_PASSWORD`
- **Secret**: RDS 비밀번호 붙여넣기
- **Add secret** 클릭

### 10.3 확인
총 5개의 Secrets가 등록되었는지 확인:
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- DB_URL
- DB_USERNAME
- DB_PASSWORD

---

## 11. 자동 배포 테스트

### 11.1 코드 푸시
```bash
cd /path/to/SPB_SERVER
git add .
git commit -m "ci: Setup AWS ECS deployment"
git push origin main
```

### 11.2 GitHub Actions 확인
1. GitHub 리포지토리 → **Actions** 탭 클릭
2. **CI/CD** 워크플로우 실행 확인
3. 클릭하면 상세 로그 확인 가능
4. 모든 단계에 ✅ 녹색 체크가 뜨면 성공

### 11.3 ECS 배포 확인
1. AWS ECS 콘솔 → `aegis-cluster` → `aegis-backend-service`
2. **배포** 탭에서 새 배포가 진행 중인지 확인
3. **태스크** 탭에서 새 태스크가 `RUNNING` 상태인지 확인

### 11.4 최종 테스트
```bash
curl http://[EC2_PUBLIC_IP]:8080/actuator/health
```

---

# PART 3: ElastiCache Redis 설정

---

## 12. ElastiCache Redis 보안 그룹 생성

### 12.1 보안 그룹 페이지 이동
1. 상단 검색창 클릭 → `VPC` 입력 → **VPC** 클릭
2. 좌측 메뉴 **보안** 섹션 → **보안 그룹** 클릭
3. 우측 상단 주황색 **보안 그룹 생성** 버튼 클릭

### 12.2 Redis용 보안 그룹 생성

**기본 세부 정보** 섹션:
- **보안 그룹 이름**: `aegis-redis-sg`
- **설명**: `ElastiCache Redis`
- **VPC**: 드롭다운에서 **aegis-vpc** 선택

**인바운드 규칙** 섹션:
1. **규칙 추가** 버튼 클릭
2. 첫 번째 규칙:
   - **유형**: 드롭다운에서 `사용자 지정 TCP` 선택
   - **포트 범위**: `6379`
   - **소스**: 드롭다운에서 `사용자 지정` 선택 → `10.0.0.0/16` 입력

**아웃바운드 규칙** 섹션:
- 기본값 유지 (모든 트래픽 허용)

3. 맨 아래 주황색 **보안 그룹 생성** 버튼 클릭

> **메모**: Redis 보안 그룹 ID `sg-________________`

---

## 13. ElastiCache 서브넷 그룹 생성

### 13.1 ElastiCache 페이지 이동
1. 상단 검색창 클릭 → `ElastiCache` 입력 → **ElastiCache** 클릭
2. 좌측 메뉴 **서브넷 그룹** 클릭
3. 우측 상단 주황색 **서브넷 그룹 생성** 버튼 클릭

### 13.2 설정 입력

**서브넷 그룹 세부 정보** 섹션:
- **이름**: `aegis-redis-subnet-group`
- **설명**: `Redis subnet group for Aegis`
- **VPC ID**: 드롭다운에서 **aegis-vpc** 선택

**서브넷 선택** 섹션:
1. **가용 영역** 드롭다운에서 `ap-northeast-2a` 선택
2. **서브넷 ID** 드롭다운에서 **프라이빗 서브넷** 선택 (`aegis-subnet-private1-ap-northeast-2a`)
3. **서브넷 추가** 버튼 클릭
4. 다시 **가용 영역** 드롭다운에서 `ap-northeast-2b` 선택
5. **서브넷 ID** 드롭다운에서 **프라이빗 서브넷** 선택 (`aegis-subnet-private2-ap-northeast-2b`)
6. **서브넷 추가** 버튼 클릭

> **참고**: Redis는 VPC 내부에서만 접근하므로 프라이빗 서브넷에 배치합니다.

3. 맨 아래 주황색 **생성** 버튼 클릭

---

## 14. ElastiCache 캐시 생성

### 14.1 ElastiCache 캐시 생성 페이지 이동
1. 좌측 메뉴 **캐시** 클릭
2. 우측 상단 주황색 **캐시 생성** 버튼 클릭

### 14.2 배포 옵션 및 생성 방법 선택

**배포 옵션** 섹션:
- [x] **노드 기반 캐시** 선택

**생성 방법** 섹션:
- [x] **간편한 생성** 선택

### 14.3 구성 선택

**구성** 섹션:
- [x] **데모** 선택
  - `cache.t4g.micro` (0.5 GiB 메모리)
  - 비용 절약을 위해 가장 작은 노드 선택

### 14.4 클러스터 정보

**이름** 섹션:
- **이름**: `aegis-redis`

**설명** 섹션:
- 기본값 유지 또는 원하는 설명 입력

### 14.5 연결 설정

**네트워크 유형** 섹션:
- [x] **IPv4** 선택

**서브넷 그룹** 섹션:
- **기존 서브넷 그룹 선택** 선택
- 드롭다운에서 `aegis-redis-subnet-group` 선택

### 14.6 생성

1. 맨 아래 주황색 **생성** 버튼 클릭
2. **5-10분 대기** (상태가 "생성 중" → "사용 가능"으로 변경될 때까지)

### 14.7 보안 그룹 수정

> 간편한 생성은 default 보안 그룹을 사용합니다. 생성 후 수정이 필요합니다.

1. **캐시** 목록에서 `aegis-redis` 클릭
2. 우측 상단 **수정** 버튼 클릭
3. **보안 그룹** 섹션에서:
   - **default** 보안 그룹 제거 (X 클릭)
   - 드롭다운에서 `aegis-redis-sg` 선택
4. **변경 사항 저장** 버튼 클릭

### 14.8 엔드포인트 확인
1. **캐시** 목록에서 `aegis-redis` 클릭
2. **캐시 세부 정보** 에서 **구성 엔드포인트** 확인
3. 엔드포인트 복사 (포트 `:6379` 제외)

> **메모**: 엔드포인트 `clustercfg.aegis-redis.xxxxxx.apn2.cache.amazonaws.com`

> **참고**: 간편한 생성은 클러스터 모드가 활성화되므로 구성 엔드포인트를 사용합니다

---

## 15. ECS 태스크 정의에 Redis 환경 변수 추가

### 15.1 태스크 정의 새 개정 생성
1. ECS 콘솔 → 좌측 메뉴 **태스크 정의** 클릭
2. `aegis-backend-task` 클릭
3. 최신 개정 선택 → **새 개정 생성** 버튼 클릭

### 15.2 환경 변수 추가

**컨테이너 - 1** 섹션에서 `aegis-backend` 클릭

**환경 변수** 섹션에 2개 추가:

| 키 | 값 유형 | 값 |
|----|---------|-----|
| `REDIS_HOST` | 값 | `aegis-redis.xxxxxx.0001.apn2.cache.amazonaws.com` |
| `REDIS_PORT` | 값 | `6379` |

3. 맨 아래 주황색 **생성** 버튼 클릭

### 15.3 ECS 서비스 업데이트
1. ECS 콘솔 → **클러스터** → `aegis-cluster` 클릭
2. **서비스** 탭에서 `aegis-backend-service` 체크
3. **업데이트** 버튼 클릭
4. **태스크 정의 개정**에서 최신 개정 선택
5. **서비스 업데이트** 버튼 클릭

---

## 16. GitHub Secrets 추가

### 16.1 GitHub Secrets 페이지 이동
1. GitHub에서 `SPB_SERVER` 리포지토리 → **Settings** 탭
2. 좌측 메뉴 **Secrets and variables** → **Actions** 클릭
3. **New repository secret** 버튼 클릭

### 16.2 Secrets 추가 (3개)

**Secret 1:**
- **Name**: `REDIS_HOST`
- **Secret**: Redis 엔드포인트 붙여넣기 (예: `aegis-redis.xxxxxx.0001.apn2.cache.amazonaws.com`)
- **Add secret** 클릭

**Secret 2:**
- **Name**: `REDIS_PORT`
- **Secret**: `6379`
- **Add secret** 클릭

**Secret 3:**
- **Name**: `JWT_SECRET`
- **Secret**: 256비트 이상의 시크릿 키 입력 (예: 64자 이상의 랜덤 문자열)
- **Add secret** 클릭

> JWT 시크릿 생성 예시: `openssl rand -base64 64` 명령어로 생성

### 16.3 확인
총 8개의 Secrets가 등록되었는지 확인:
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- DB_URL
- DB_USERNAME
- DB_PASSWORD
- REDIS_HOST
- REDIS_PORT
- JWT_SECRET

---

## 17. GitHub Actions 워크플로우 수정

### 17.1 `.github/workflows/cicd.yml` 수정

태스크 정의 업데이트 부분에 Redis 환경 변수 추가:

```yaml
- name: Update ECS task definition
  id: task-def
  uses: aws-actions/amazon-ecs-render-task-definition@v1
  with:
    task-definition: ${{ steps.download-task-def.outputs.task-definition }}
    container-name: aegis-backend
    image: ${{ steps.build-image.outputs.image }}
    environment-variables: |
      DB_URL=${{ secrets.DB_URL }}
      DB_USERNAME=${{ secrets.DB_USERNAME }}
      DB_PASSWORD=${{ secrets.DB_PASSWORD }}
      REDIS_HOST=${{ secrets.REDIS_HOST }}
      REDIS_PORT=${{ secrets.REDIS_PORT }}
      JWT_SECRET=${{ secrets.JWT_SECRET }}
```

---

# PART 4: FastAPI 추가 (나중에)

같은 EC2에 FastAPI를 추가할 때:

## 1. ECR 리포지토리 추가 생성
- 리포지토리 이름: `aegis/ai`

## 2. 태스크 정의 수정
- 기존 태스크 정의에 컨테이너 추가
- 컨테이너 이름: `aegis-ai`
- 포트: `8000`

## 3. GitHub Actions 수정
- FastAPI 빌드/푸시 단계 추가
- 또는 별도 워크플로우 파일 생성

---

# PART 5: Route 53 도메인 연결

---

## 18. Elastic IP 할당 (필수 선행)

> **중요**: EC2 퍼블릭 IP는 인스턴스가 재시작되면 바뀝니다. 도메인을 연결하려면 고정 IP(Elastic IP)가 필요합니다.

### 18.1 Elastic IP 페이지 이동
1. 상단 검색창 클릭 → `EC2` 입력 → **EC2** 클릭
2. 좌측 메뉴 스크롤 → **네트워크 및 보안** 섹션 → **탄력적 IP** 클릭
3. 우측 상단 주황색 **탄력적 IP 주소 할당** 버튼 클릭

### 18.2 설정 입력

**네트워크 경계 그룹** 섹션:
- **리전**: `ap-northeast-2` (기본값)

**퍼블릭 IPv4 주소 풀** 섹션:
- [x] **Amazon의 IPv4 주소 풀** 선택 (기본값)

**태그** 섹션:
- **키**: `Name`
- **값**: `aegis-eip`

3. 맨 아래 주황색 **할당** 버튼 클릭

### 18.3 EC2 인스턴스에 연결
1. 방금 생성된 Elastic IP를 선택 (체크박스)
2. 우측 상단 **작업** 드롭다운 → **탄력적 IP 주소 연결** 클릭
3. **리소스 유형**: `인스턴스` 선택
4. **인스턴스**: 드롭다운에서 ECS EC2 인스턴스 선택 (`ECS Instance - aegis-cluster`)
5. **프라이빗 IP 주소**: 자동 선택됨 (기본값)
6. 주황색 **연결** 버튼 클릭

> **메모**: Elastic IP `________________`
> **주의**: 이제부터 이 IP가 EC2의 고정 퍼블릭 IP입니다. 기존 퍼블릭 IP는 무효화됩니다.

---

## 19. 도메인 등록 (Route 53)

> 이미 가지고 있는 도메인이 있으면 19단계를 건너뛰고 20단계로 이동하세요.

### 19.1 Route 53 페이지 이동
1. 상단 검색창 클릭 → `Route 53` 입력 → **Route 53** 클릭
2. 좌측 메뉴 **등록된 도메인** 클릭
3. 주황색 **도메인 등록** 버튼 클릭

### 19.2 도메인 검색

**도메인 이름 선택** 섹션:
1. 검색창에 원하는 도메인 입력 (예시):
   - `aegis-fire.com`
   - `aegis-119.com`
   - `aegis-rescue.com`
   - `aegis-api.com`
2. **검색** 버튼 클릭
3. 사용 가능한 도메인 확인 (✅ 표시)
4. 원하는 도메인 옆 **선택** 버튼 클릭
5. **체크아웃으로 진행** 버튼 클릭

> **참고**: `.com` 도메인은 연간 약 $13 (약 17,000원), `.net`은 약 $11입니다.
> `.click`, `.link` 등은 $3~5로 저렴합니다.

### 19.3 연락처 정보 입력

**등록자 연락처** 섹션:
- 이름, 이메일, 전화번호 등 입력 (실제 정보)
- **개인 정보 보호**: `활성화` 선택 (WHOIS에 개인정보 숨김)

### 19.4 결제 및 등록
1. 약관 동의 체크
2. **주문 완료** 버튼 클릭
3. 이메일 인증 메일 확인 → 링크 클릭
4. **등록 완료까지 최대 10~30분** 소요

> **메모**: 등록한 도메인 `________________`

---

## 20. 호스팅 영역 생성

> 19단계에서 Route 53으로 도메인을 등록했다면, 호스팅 영역이 **자동 생성**됩니다.
> 이 경우 20단계를 건너뛰고 21단계로 이동하세요.
>
> 외부에서 구매한 도메인(가비아, Namecheap 등)을 사용하는 경우에만 이 단계가 필요합니다.

### 20.1 호스팅 영역 페이지 이동
1. Route 53 콘솔 → 좌측 메뉴 **호스팅 영역** 클릭
2. 주황색 **호스팅 영역 생성** 버튼 클릭

### 20.2 설정 입력

**도메인 이름** 섹션:
- 구매한 도메인 입력 (예: `aegis-fire.com`)

**설명** 섹션:
- `Aegis project` (선택사항)

**유형** 섹션:
- [x] **퍼블릭 호스팅 영역** 선택

3. 주황색 **호스팅 영역 생성** 버튼 클릭

### 20.3 네임서버 설정 (외부 도메인인 경우만)

> Route 53에서 도메인을 등록한 경우 이 단계는 불필요합니다.

1. 생성된 호스팅 영역 클릭
2. **NS** 레코드의 값 4개 확인 (예시):
   ```
   ns-1234.awsdns-12.org.
   ns-567.awsdns-34.net.
   ns-890.awsdns-56.co.uk.
   ns-12.awsdns-78.com.
   ```
3. 도메인 구매한 사이트(가비아 등)에 로그인
4. 도메인 관리 → 네임서버 설정
5. 위 4개의 네임서버를 등록
6. **네임서버 전파까지 최대 24~48시간** 소요 (보통 1~2시간)

---

## 21. DNS 레코드 생성 (도메인 → EC2 연결)

### 21.1 호스팅 영역 이동
1. Route 53 콘솔 → 좌측 메뉴 **호스팅 영역** 클릭
2. 도메인 이름 클릭 (예: `aegis-fire.com`)

### 21.2 API 서브도메인 레코드 생성

1. 주황색 **레코드 생성** 버튼 클릭

**레코드 이름** 섹션:
- 입력창에 `api` 입력 → 결과: `api.aegis-fire.com`

**레코드 유형** 섹션:
- **A** 선택 (IPv4 주소로 라우팅)

**값** 섹션:
- 18단계에서 할당한 **Elastic IP** 입력 (예: `3.35.xxx.xxx`)

**TTL(초)** 섹션:
- `300` (기본값)

**라우팅 정책** 섹션:
- **단순 라우팅** 선택 (기본값)

2. 주황색 **레코드 생성** 버튼 클릭

### 21.3 (선택) 루트 도메인 레코드 생성

> API와 프론트엔드를 같은 도메인에서 운영하려면 루트 도메인도 설정합니다.

1. 다시 **레코드 생성** 버튼 클릭
2. **레코드 이름**: 비워두기 (루트 도메인 = `aegis-fire.com`)
3. **레코드 유형**: `A`
4. **값**: 같은 Elastic IP 입력
5. **레코드 생성** 클릭

### 21.4 전파 확인

**DNS 전파는 보통 1~5분** 걸립니다. 터미널에서 확인:

```bash
# DNS 조회 확인
nslookup api.aegis-fire.com

# 실제 접속 테스트
curl http://api.aegis-fire.com:8080/actuator/health
```

> `{"status":"UP"}` 응답이 오면 성공!

---

## 22. 최종 접속 URL 정리

도메인 연결 후 접속 가능한 URL:

| 용도 | URL |
|------|-----|
| **헬스체크** | `http://api.aegis-fire.com:8080/actuator/health` |
| **Swagger UI** | `http://api.aegis-fire.com:8080/swagger-ui.html` |
| **API 베이스** | `http://api.aegis-fire.com:8080/api/` |

---

## 23. (선택) HTTPS 적용 - ACM + ALB

> 현재 구조(EC2 직접 노출)에서는 HTTPS를 위해 **ALB(Application Load Balancer)**가 필요합니다.
> HTTPS가 당장 불필요하면 이 단계는 건너뛰어도 됩니다.
> 나중에 프론트엔드 배포 시 HTTPS가 필수이므로, 그때 진행해도 됩니다.

### 23.1 ACM 인증서 발급
1. 상단 검색창 → `ACM` 입력 → **Certificate Manager** 클릭
2. **인증서 요청** 버튼 클릭
3. **퍼블릭 인증서 요청** 선택 → **다음**
4. **도메인 이름**: `*.aegis-fire.com` 입력 (와일드카드)
5. **다른 이름 추가**: `aegis-fire.com` (루트 도메인)
6. **검증 방법**: `DNS 검증` 선택 (Route 53 사용 시 가장 간편)
7. **요청** 버튼 클릭
8. 인증서 목록에서 방금 생성한 인증서 클릭
9. **Route 53에서 레코드 생성** 버튼 클릭 → **레코드 생성** 클릭
10. **5~30분 대기** (상태: "검증 보류 중" → "발급됨")

### 23.2 ALB 생성
1. 상단 검색창 → `EC2` → 좌측 메뉴 **로드 밸런서** 클릭
2. **로드 밸런서 생성** → **Application Load Balancer** 선택
3. 설정:
   - **이름**: `aegis-alb`
   - **체계**: `인터넷 경계`
   - **VPC**: `aegis-vpc`
   - **서브넷**: 퍼블릭 서브넷 2개 선택
   - **보안 그룹**: `aegis-ecs-sg` 선택
4. **리스너**:
   - HTTP:80 → HTTPS:443으로 리디렉션 (액션에서 설정)
   - HTTPS:443 → 대상 그룹 (아래에서 생성)
5. **인증서**: ACM에서 발급받은 인증서 선택
6. **대상 그룹 생성**:
   - **이름**: `aegis-backend-tg`
   - **대상 유형**: `인스턴스`
   - **포트**: `8080`
   - **헬스체크 경로**: `/actuator/health`
   - EC2 인스턴스를 대상으로 등록
7. **로드 밸런서 생성** 클릭

### 23.3 Route 53 레코드 수정
1. Route 53 → 호스팅 영역 → 도메인 클릭
2. 기존 `api` A 레코드 선택 → **레코드 편집**
3. **별칭** 토글 켜기
4. **트래픽 라우팅 대상**: `Application/Classic Load Balancer 에 대한 별칭`
5. **리전**: `아시아 태평양(서울)`
6. **로드 밸런서**: `aegis-alb` 선택
7. **저장**

HTTPS 적용 후 URL:

| 용도 | URL |
|------|-----|
| **Swagger UI** | `https://api.aegis-fire.com/swagger-ui.html` |
| **API 베이스** | `https://api.aegis-fire.com/api/` |

> 포트 번호 없이 깔끔하게 접속 가능!

---

# 메모 정리

```
VPC ID: vpc-________________
RDS 보안 그룹 ID: sg-________________
ECS 보안 그룹 ID: sg-________________
RDS 엔드포인트: aegis-mysql.________________.ap-northeast-2.rds.amazonaws.com
RDS 비밀번호: ________________
AWS 계정 ID (12자리): ________________
ECR URI: ________________.dkr.ecr.ap-northeast-2.amazonaws.com/aegis/backend
EC2 퍼블릭 IP: ________________
Elastic IP: ________________
등록 도메인: ________________
API URL: api.________________
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
[ECS 서비스가 새 태스크 배포]
        ↓
[EC2에서 새 컨테이너 실행]
        ↓
[헬스체크 통과 후 배포 완료]
```