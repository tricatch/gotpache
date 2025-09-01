
# gotpache(갖바치 - 갖신'의 장인)

Development utility - act as apache proxypass and generates its own ssl certificates for sites.

Gotpache는 개발/테스트환경에서 apache의 ProxyPass 기능과 SSL인증서를 간편하게 사용하기 위해서 개발한 프로그램입니다.
apache/nginx의 복잡한 기능을 가지고 있지 않지만, ProxyPass와 유사한 기능과 SSL인증서를 동적으로 생성하는 기능을 제공합니다.
개발/테스트환경에서 프로그램 실행만으로 복잡한 인증서문제를 해결할 수 있습니다.
또한 최근 HTTPS를 적용하면서 강화되는 samesite와 같은 보안 이슈를 gotpache를 통해 간단하게 구성하고 테스트할 수 있습니다. 
<br>

### Release Note

#### v0.5.1
* TLS 1.3, +1.2 

#### v0.5.0
* HTTP 1.1 지원 
* JDK 21 이상 

#### v0.4.0
* Websocket 지원
  
#### v0.3.1
* gotpache-keytool 적용
* 루트인증서 자동생성 기능 삭제
  * gotpache-keytool을 사용해서 인증성 생성

#### v0.3.0
* http://{ip}:36912/gotca.cer 루트인증서 다운로드 추가
* http header 추가/삭제
  * add ? header: [ "X-Frame-Options: DENY" ]
  * remove("--") ? header: [ "--X-Frame-Options" ]


#### v0.2.2
* 생성 또는 사용할 인증서 파일정보 yml 파일에 설정

#### v0.2.0
* AntPathMatcher를 통해 다양한 URL 패턴으로 호스트 분기 설정설정
* YML 설정파일을 통해 가독성 있는 호스트 분기 설정
* 127.0.0.1로 설정된 도메인만 활성화

#### v0.1.1
* 요청하는 Domain의 SSL 인증서 동적생성
* URL 경로에 따른 호스트 분기

## How to work
<img src="https://velog.velcdn.com/images/tricatch/post/68b20b75-5de0-4aff-b53c-01b4c0b48c18/image.png" />

<br>
<br>

## 사용방법
1. gotpache.x.x.x.zip은 다운로드 받아 압축을 해제
2. gotpache.bat 또는 gotpache.sh를 실행(JAVA_HOME 설정 필요)
3. conf/proxypass.yml파일에 ca 파일명 및 alias를 설정하여 새로 생성하거나 기존 ROOT 인증서 사용가능
4. cer 파일을 더블클릭하여 시스템에 ROOT 인증서 설치(구글링-ROOT 인증서 설치)
   * Windows : https://learn.microsoft.com/en-us/skype-sdk/sdn/articles/installing-the-trusted-root-certificate
5. 로컬 hosts 파일에서 도메인 변경( ex. 127.0.0.1 foo.kr, 127.0.0.1 goo.kr)
6. conf/proxypass.yml - URL 패턴에 따른 서버 분기 설정(로컬/원격 관계없음)
7. 브라우저에서 https://foo.kr 접속 (실시간으로 foo.kr 인증서 생성)
8. 브라우저에서 https://goo.kr 접속 (실시간으로 goo.kr 인증서 생성)

<br>
<br>

## 로컬 HTTPS 구성
* https://velog.io/@tricatch/posts
* 로컬 개발환경
* 로컬 개발환경 + 모바일(iOS, Android 브라우저, 앱)

<br>
<br> 

## 소스관련
코드를 계속업데이트하기 힘들지도 몰라서 최대한 단순하게 작성했습니다.
지금 개발환경에서 사용하는데 큰 문제가 없어서 HTTP 1.0만 구현하고 있습니다.
상위버전은 아직 계획이 없고, 필요하신 분들은 자유롭게 수정해서 사용하셔도 됩니다.
