/*
 * Many concepts and protocol specification in this code are taken
 * from Airplayer. https://github.com/PascalW/Airplayer
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

#include "AirPlayServer.h"
#include "AirPlayCallBack.h"
#define HAS_AIRPLAY
#ifdef HAS_AIRPLAY

#ifdef __ANDROID__
#undef LOG_TAG
#define LOG_TAG "AIRPLAY_SERVER"

#ifdef NDK
#include <android/log.h>
#else
#include <utils/Log.h>
#endif

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG , __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  ,  LOG_TAG , __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  ,  LOG_TAG , __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG , __VA_ARGS__)

#else
#define LOGV(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGD(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGI(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGW(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGE(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGE(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#endif

#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include "md5.h"
//#include "DllLibPlist.h"
#include <plist/plist.h>
#ifdef TARGET_WINDOWS
#define close closesocket
#endif

#define INVALID_SOCKET -1
#define RECEIVEBUFFER 1024

#define AIRPLAY_STATUS_OK                  200
#define AIRPLAY_STATUS_SWITCHING_PROTOCOLS 101
#define AIRPLAY_STATUS_NEED_AUTH           401
#define AIRPLAY_STATUS_NOT_FOUND           404
#define AIRPLAY_STATUS_METHOD_NOT_ALLOWED  405
#define AIRPLAY_STATUS_NOT_IMPLEMENTED     501
#define AIRPLAY_STATUS_NO_RESPONSE_NEEDED  1000

CAirPlayServer *CAirPlayServer::ServerInstance = NULL;
int CAirPlayServer::m_isPlaying = 0;
struct airplay_callback airplay_cb;

#define EVENT_NONE     -1
#define EVENT_PLAYING   0
#define EVENT_PAUSED    1
#define EVENT_LOADING   2
#define EVENT_STOPPED   3
const char *eventStrings[] = {"playing", "paused", "loading", "stopped"};

#define PLAYBACK_INFO  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
"<plist version=\"1.0\">\r\n"\
"<dict>\r\n"\
"<key>duration</key>\r\n"\
"<real>%f</real>\r\n"\
"<key>loadedTimeRanges</key>\r\n"\
"<array>\r\n"\
"\t\t<dict>\r\n"\
"\t\t\t<key>duration</key>\r\n"\
"\t\t\t<real>%f</real>\r\n"\
"\t\t\t<key>start</key>\r\n"\
"\t\t\t<real>0.0</real>\r\n"\
"\t\t</dict>\r\n"\
"</array>\r\n"\
"<key>playbackBufferEmpty</key>\r\n"\
"<true/>\r\n"\
"<key>playbackBufferFull</key>\r\n"\
"<false/>\r\n"\
"<key>playbackLikelyToKeepUp</key>\r\n"\
"<true/>\r\n"\
"<key>position</key>\r\n"\
"<real>%f</real>\r\n"\
"<key>rate</key>\r\n"\
"<real>%d</real>\r\n"\
"<key>readyToPlay</key>\r\n"\
"<true/>\r\n"\
"<key>seekableTimeRanges</key>\r\n"\
"<array>\r\n"\
"\t\t<dict>\r\n"\
"\t\t\t<key>duration</key>\r\n"\
"\t\t\t<real>%f</real>\r\n"\
"\t\t\t<key>start</key>\r\n"\
"\t\t\t<real>0.0</real>\r\n"\
"\t\t</dict>\r\n"\
"</array>\r\n"\
"</dict>\r\n"\
"</plist>\r\n"

#define PLAYBACK_INFO_NOT_READY  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
"<plist version=\"1.0\">\r\n"\
"<dict>\r\n"\
"<key>readyToPlay</key>\r\n"\
"<false/>\r\n"\
"</dict>\r\n"\
"</plist>\r\n"

#define SERVER_INFO  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
"<plist version=\"1.0\">\r\n"\
"<dict>\r\n"\
"<key>deviceid</key>\r\n"\
"<string>%s</string>\r\n"\
"<key>features</key>\r\n"\
"<integer>119</integer>\r\n"\
"<key>model</key>\r\n"\
"<string>AppleTV2,1</string>\r\n"\
"<key>protovers</key>\r\n"\
"<string>1.0</string>\r\n"\
"<key>srcvers</key>\r\n"\
"<string>"AIRPLAY_SERVER_VERSION_STR"</string>\r\n"\
"</dict>\r\n"\
"</plist>\r\n"

#define EVENT_INFO "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\r\n"\
"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n\r\n"\
"<plist version=\"1.0\">\r\n"\
"<dict>\r\n"\
"<key>category</key>\r\n"\
"<string>video</string>\r\n"\
"<key>state</key>\r\n"\
"<string>%s</string>\r\n"\
"</dict>\r\n"\
"</plist>\r\n"\

#define AUTH_REALM "AirPlay"
#define AUTH_REQUIRED "WWW-Authenticate: Digest realm=\""  AUTH_REALM  "\", nonce=\"%s\"\r\n"
pthread_mutex_t CAirPlayServer::M_STOP_LOCK;

bool CAirPlayServer::StartServer(int port, bool nonlocal, struct airplay_callback *cb)
{
  LOGI("Start AirPlay Server\n");
  StopServer(true);

  ServerInstance = new CAirPlayServer(port, nonlocal, cb);

  if (ServerInstance->Initialize())
  {
    ServerInstance->Create();
    return true;
  }
  else
    return false;
}

void *CAirPlayServer::Run(void *arg)
{
  ServerInstance->Process();
  return (void *)(0);
}

void CAirPlayServer::Create(void)
{

  pthread_t pid = 0;
  pthread_attr_t attr;
  pthread_attr_init(&attr);

//#if !defined(TARGET_ANDROID) // http://code.google.com/p/android/issues/detail?id=7808
//  if (stacksize > PTHREAD_STACK_MIN)
//    pthread_attr_setstacksize(&attr, stacksize);
//#endif
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
  if (pthread_create(&pid, &attr, (void*(*)(void*))&Run, NULL) != 0) {
    printf("thread cread error!\n");
  }
  pthread_attr_destroy(&attr);
}

bool CAirPlayServer::SetCredentials(bool usePassword, const CStdString& password)
{
  bool ret = false;

  if (ServerInstance)
  {
    ret = ServerInstance->SetInternalCredentials(usePassword, password);
  }
  return ret;
}

bool CAirPlayServer::SetInternalCredentials(bool usePassword, const CStdString& password)
{
  m_usePassword = usePassword;
  m_password = password;
  return true;
}

void CAirPlayServer::StopServer(bool bWait)
{
  if (ServerInstance)
  {
    //ServerInstance->StopThread(bWait);
    ServerInstance->m_bStop = true;
    pthread_mutex_lock(&M_STOP_LOCK);
    if (bWait)
    {
      delete ServerInstance;
      ServerInstance = NULL;
    }
    pthread_mutex_unlock(&M_STOP_LOCK);
  }
}

CAirPlayServer::CAirPlayServer(int port, bool nonlocal, struct airplay_callback *cb)
{
  pthread_mutex_init(&M_STOP_LOCK,NULL);
  m_port = port;
  m_nonlocal = nonlocal;
  m_ServerSocket = INVALID_SOCKET;
  m_usePassword = false;
  init_airplay_callback(&airplay_cb, cb);
}

void CAirPlayServer::Process()
{
  m_bStop = false;

  pthread_mutex_lock(&M_STOP_LOCK);
  while (!m_bStop)
  {
	int             max_fd = 0;
    fd_set          rfds;
    struct timeval  to     = {1, 0};
    FD_ZERO(&rfds);

    FD_SET(m_ServerSocket, &rfds);
    max_fd = m_ServerSocket;

    for (unsigned int i = 0; i < m_connections.size(); i++)
    {
      FD_SET(m_connections[i].m_socket, &rfds);
      if (m_connections[i].m_socket > max_fd)
        max_fd = m_connections[i].m_socket;
    }

    int res = select(max_fd+1, &rfds, NULL, NULL, &to);
    if (res < 0)
    {
      LOGE("AIRPLAY Server: Select failed");
      sleep(1);
      Initialize();
    }
    else if (res > 0)
    {
      for (int i = m_connections.size() - 1; i >= 0; i--)
      {
        int socket = m_connections[i].m_socket;
        if (FD_ISSET(socket, &rfds))
        {
          char buffer[RECEIVEBUFFER] = {};
          int  nread = 0;
          nread = recv(socket, (char*)&buffer, RECEIVEBUFFER, 0);
          if (nread > 0)
          {
            CStdString sessionId;
            m_connections[i].PushBuffer(this, buffer, nread, sessionId, m_reverseSockets);
          }
          if (nread <= 0)
          {
            LOGI("AIRPLAY Server: Disconnection detected");
            m_connections[i].Disconnect();
            m_connections.erase(m_connections.begin() + i);
          }
        }
      }

      if (FD_ISSET(m_ServerSocket, &rfds))
      {
        LOGD("AIRPLAY Server: New connection detected");
        CTCPClient newconnection;
        newconnection.m_socket = accept(m_ServerSocket, &newconnection.m_cliaddr, &newconnection.m_addrlen);

        if (newconnection.m_socket == INVALID_SOCKET)
          LOGE("AIRPLAY Server: Accept of new connection failed");
        else
        {
          LOGI("AIRPLAY Server: New connection added");
          m_connections.push_back(newconnection);
        }
      }
    }

  }

  Deinitialize();
  pthread_mutex_lock(&M_STOP_LOCK);
}

bool CAirPlayServer::Initialize()
{
  Deinitialize();

  struct sockaddr_in myaddr;
  memset(&myaddr, 0, sizeof(myaddr));

  myaddr.sin_family = AF_INET;
  myaddr.sin_port = htons(m_port);

  if (m_nonlocal)
    myaddr.sin_addr.s_addr = INADDR_ANY;
  else
    inet_pton(AF_INET, "127.0.0.1", &myaddr.sin_addr.s_addr);

  m_ServerSocket = socket(PF_INET, SOCK_STREAM, 0);

  if (m_ServerSocket == INVALID_SOCKET)
  {

    LOGE("AIRPLAY Server: Failed to create serversocket");
    return false;
  }

  if (bind(m_ServerSocket, (struct sockaddr*)&myaddr, sizeof myaddr) < 0)
  {
    LOGE("AIRPLAY Server: Failed to bind serversocket");
    close(m_ServerSocket);
    return false;
  }

  if (listen(m_ServerSocket, 10) < 0)
  {
    LOGE("AIRPLAY Server: Failed to set listen");
    close(m_ServerSocket);
    return false;
  }

  LOGI("AIRPLAY Server: Successfully initialized");
  return true;
}

void CAirPlayServer::Deinitialize()
{
  LOGI("CAirPlayServer::Deinitialize!\n");
  for (unsigned int i = 0; i < m_connections.size(); i++)
  m_connections[i].Disconnect();

  m_connections.clear();
  m_reverseSockets.clear();

  if (m_ServerSocket != INVALID_SOCKET)
  {
    shutdown(m_ServerSocket, SHUT_RDWR);
    close(m_ServerSocket);
    m_ServerSocket = INVALID_SOCKET;
  }
}

CAirPlayServer::CTCPClient::CTCPClient()
{
  m_socket = INVALID_SOCKET;
  m_httpParser = new HttpParser();

  m_addrlen = sizeof(struct sockaddr);
  //m_pLibPlist = new DllLibPlist();

  m_bAuthenticated = false;
  m_lastEvent = EVENT_NONE;
}

CAirPlayServer::CTCPClient::CTCPClient(const CTCPClient& client)
{
  Copy(client);
  m_httpParser = new HttpParser();
  //m_pLibPlist = new DllLibPlist();
}

CAirPlayServer::CTCPClient::~CTCPClient()
{
//  if (m_pLibPlist->IsLoaded())
//  {
//    m_pLibPlist->Unload();
//  }
//  delete m_pLibPlist;
  delete m_httpParser;
}

CAirPlayServer::CTCPClient& CAirPlayServer::CTCPClient::operator=(const CTCPClient& client)
{
  Copy(client);
  m_httpParser = new HttpParser();
//  m_pLibPlist = new DllLibPlist();
  return *this;
}

void CAirPlayServer::CTCPClient::PushBuffer(CAirPlayServer *host, const char *buffer,
                                            int length, CStdString &sessionId, std::map<CStdString,
                                            int> &reverseSockets)
{
  HttpParser::status_t status = m_httpParser->addBytes(buffer, length);

  if (status == HttpParser::Done)
  {
    // Parse the request
    CStdString responseHeader;
    CStdString responseBody;
    CStdString reverseHeader;
    CStdString reverseBody;
    int status = ProcessRequest(responseHeader, responseBody, reverseHeader, reverseBody, sessionId);
    CStdString statusMsg = "OK";
    int reverseSocket = INVALID_SOCKET;

    switch(status)
    {
      case AIRPLAY_STATUS_NOT_IMPLEMENTED:
        statusMsg = "Not Implemented";
        break;
      case AIRPLAY_STATUS_SWITCHING_PROTOCOLS:
        statusMsg = "Switching Protocols";
        reverseSockets[sessionId] = m_socket;//save this socket as reverse http socket for this sessionid
        break;
      case AIRPLAY_STATUS_NEED_AUTH:
        statusMsg = "Unauthorized";
        break;
      case AIRPLAY_STATUS_NOT_FOUND:
        statusMsg = "Not Found";
        break;
      case AIRPLAY_STATUS_METHOD_NOT_ALLOWED:
        statusMsg = "Method Not Allowed";
        break;
    }

    // Prepare the response
    CStdString response;
    const time_t ltime = time(NULL);
    char *date = asctime(gmtime(&ltime)); //Fri, 17 Dec 2010 11:18:01 GMT;
    date[strlen(date) - 1] = '\0'; // remove \n
    response.Format("HTTP/1.1 %d %s\nDate: %s\r\n", status, statusMsg.c_str(), date);
    if (responseHeader.size() > 0)
    {
      response += responseHeader;
    }

    if (responseBody.size() > 0)
    {
      response.Format("%sContent-Length: %d\r\n", response.c_str(), responseBody.size());
    }
    response += "\r\n";

    if (responseBody.size() > 0)
    {
      response += responseBody;
    }

    // Send the response
    //don't send response on AIRPLAY_STATUS_NO_RESPONSE_NEEDED
    if (status != AIRPLAY_STATUS_NO_RESPONSE_NEEDED)
    {
      send(m_socket, response.c_str(), response.size(), 0);
    }

    // Send event status per reverse http socket (play, loading, paused)
    // if we have a reverse header and a reverse socket
    if (reverseHeader.size() > 0 && reverseSockets.find(sessionId) != reverseSockets.end())
    {
      //search the reverse socket to this sessionid
      response.Format("POST /event HTTP/1.1\r\n");
      reverseSocket = reverseSockets[sessionId]; //that is our reverse socket
      response += reverseHeader;
    }
    response += "\r\n";

    if (reverseBody.size() > 0)
    {
      response += reverseBody;
    }

    if (reverseSocket != INVALID_SOCKET)
    {
      send(reverseSocket, response.c_str(), response.size(), 0);//send the event status on the eventSocket
    }

    // We need a new parser...
    delete m_httpParser;
    m_httpParser = new HttpParser;
  }
}

void CAirPlayServer::CTCPClient::Disconnect()
{
  if (m_socket != INVALID_SOCKET)
  {
    //CSingleLock lock (m_critSection); TODO:cyher What does this lock for ?
    shutdown(m_socket, SHUT_RDWR);
    close(m_socket);
    m_socket = INVALID_SOCKET;
    delete m_httpParser;
    m_httpParser = NULL;
  }
}

void CAirPlayServer::CTCPClient::Copy(const CTCPClient& client)
{
  m_socket            = client.m_socket;
  m_cliaddr           = client.m_cliaddr;
  m_addrlen           = client.m_addrlen;
  m_httpParser        = client.m_httpParser;
  m_authNonce         = client.m_authNonce;
  m_bAuthenticated    = client.m_bAuthenticated;
}


void CAirPlayServer::CTCPClient::ComposeReverseEvent( CStdString& reverseHeader,
                                                      CStdString& reverseBody,
                                                      CStdString sessionId,
                                                      int state)
{

  if ( m_lastEvent != state )
  { 
    switch(state)
    {
      case EVENT_PLAYING:
      case EVENT_LOADING:
      case EVENT_PAUSED:
      case EVENT_STOPPED:      
        reverseBody.Format(EVENT_INFO, eventStrings[state]);
        LOGD("AIRPLAY: sending event: %s", eventStrings[state]);
        break;
    }
    reverseHeader = "Content-Type: text/x-apple-plist+xml\r\n";
    reverseHeader.Format("%sContent-Length: %d",reverseHeader.c_str(),reverseBody.size());
    reverseHeader.Format("%sx-apple-session-id: %s\r\n",reverseHeader.c_str(),sessionId.c_str());
    m_lastEvent = state;
  }
}

void CAirPlayServer::CTCPClient::ComposeAuthRequestAnswer(CStdString& responseHeader, CStdString& responseBody)
{
  CStdString randomStr;
  int16_t random=rand();
  randomStr.Format("%i", random);
  m_authNonce=XBMC::XBMC_MD5::GetMD5(randomStr);
  responseHeader.Format(AUTH_REQUIRED,m_authNonce);
  responseBody.clear();
}


//as of rfc 2617
CStdString calcResponse(const CStdString& username,
                        const CStdString& password,
                        const CStdString& realm,
                        const CStdString& method,
                        const CStdString& digestUri,
                        const CStdString& nonce)
{
  CStdString response;
  CStdString HA1;
  CStdString HA2;

  HA1 = XBMC::XBMC_MD5::GetMD5(username + ":" + realm + ":" + password);
  HA2 = XBMC::XBMC_MD5::GetMD5(method + ":" + digestUri);
  response = XBMC::XBMC_MD5::GetMD5(HA1.ToLower() + ":" + nonce + ":" + HA2.ToLower());
  return response.ToLower();
}

// Splits the string input into pieces delimited by delimiter.
// if 2 delimiters are in a row, it will include the empty string between them.
// added MaxStrings parameter to restrict the number of returned substrings (like perl and python)
int SplitString(const CStdString& input, const CStdString& delimiter, CStdStringArray &results, unsigned int iMaxStrings = 0 )
{
  int iPos = -1;
  int newPos = -1;
  int sizeS2 = delimiter.GetLength();
  int isize = input.GetLength();

  results.clear();

  std::vector<unsigned int> positions;

  newPos = input.Find (delimiter, 0);

  if ( newPos < 0 )
  {
    results.push_back(input);
    return 1;
  }

  while ( newPos > iPos )
  {
    positions.push_back(newPos);
    iPos = newPos;
    newPos = input.Find (delimiter, iPos + sizeS2);
  }

  // numFound is the number of delimeters which is one less
  // than the number of substrings
  unsigned int numFound = positions.size();
  if (iMaxStrings > 0 && numFound >= iMaxStrings)
    numFound = iMaxStrings - 1;

  for ( unsigned int i = 0; i <= numFound; i++ )
  {
    CStdString s;
    if ( i == 0 )
    {
      if ( i == numFound )
        s = input;
      else
        s = input.Mid( i, positions[i] );
    }
    else
    {
      int offset = positions[i - 1] + sizeS2;
      if ( offset < isize )
      {
        if ( i == numFound )
          s = input.Mid(offset);
        else if ( i > 0 )
          s = input.Mid( positions[i - 1] + sizeS2,
                         positions[i] - positions[i - 1] - sizeS2 );
      }
    }
    results.push_back(s);
  }
  // return the number of substrings
  return results.size();
}


//helper function
//from a string field1="value1", field2="value2" it parses the value to a field
CStdString getFieldFromString(const CStdString &str, const char* field)
{
  CStdString tmpStr;
  CStdStringArray tmpAr1;
  CStdStringArray tmpAr2;

  SplitString(str, ",", tmpAr1);

  for(unsigned int i = 0;i<tmpAr1.size();i++)
  {
    if (tmpAr1[i].Find(field) != -1)
    {
      if (SplitString(tmpAr1[i], "=", tmpAr2) == 2)
      {
        tmpAr2[1].Remove('\"');//remove quotes
        return tmpAr2[1];
      }
    }
  }
  return "";
}

bool CAirPlayServer::CTCPClient::checkAuthorization(const CStdString& authStr,
                                                    const CStdString& method,
                                                    const CStdString& uri)
{
  bool authValid = true;

  CStdString username;

  if (authStr.empty())
    return false;

  //first get username - we allow all usernames for airplay (usually it is AirPlay)
  username = getFieldFromString(authStr, "username");
  if (username.empty())
  {
    authValid = false;
  }

  //second check realm
  if (authValid)
  {
    if (getFieldFromString(authStr, "realm") != AUTH_REALM)
    {
      authValid = false;
    }
  }

  //third check nonce
  if (authValid)
  {
    if (getFieldFromString(authStr, "nonce") != m_authNonce)
    {
      authValid = false;
    }
  }

  //forth check uri
  if (authValid)
  {
    if (getFieldFromString(authStr, "uri") != uri)
    {
      authValid = false;
    }
  }

  //last check response
  if (authValid)
  {
     CStdString realm = AUTH_REALM;
     CStdString ourResponse = calcResponse(username, ServerInstance->m_password, realm, method, uri, m_authNonce);
     CStdString theirResponse = getFieldFromString(authStr, "response");
     if (!theirResponse.Equals(ourResponse, false))
     {
       authValid = false;
       LOGD("AirAuth: response mismatch - our: %s theirs: %s",ourResponse.c_str(), theirResponse.c_str());
     }
     else
     {
       LOGD("AirAuth: successfull authentication from AirPlay client");
     }
  }
  m_bAuthenticated = authValid;
  return m_bAuthenticated;
}

void Encode(CStdString& strURLData)
{
  CStdString strResult;

  /* wonder what a good value is here is, depends on how often it occurs */
  strResult.reserve( strURLData.length() * 2 );

  for (int i = 0; i < (int)strURLData.size(); ++i)
  {
    int kar = (unsigned char)strURLData[i];
    //if (kar == ' ') strResult += '+'; // obsolete
    if (isalnum(kar) || strchr("-_.!()" , kar) ) // Don't URL encode these according to RFC1738
    {
      strResult += kar;
    }
    else
    {
      CStdString strTmp;
      strTmp.Format("%%%02.2x", kar);
      strResult += strTmp;
    }
  }
  strURLData = strResult;
}

int CAirPlayServer::CTCPClient::ProcessRequest( CStdString& responseHeader,
                                                CStdString& responseBody,
                                                CStdString& reverseHeader,
                                                CStdString& reverseBody,
                                                CStdString& sessionId)
{
  CStdString method = m_httpParser->getMethod();
  CStdString uri = m_httpParser->getUri();
  CStdString queryString = m_httpParser->getQueryString();
  CStdString body = m_httpParser->getBody();
  CStdString contentType = m_httpParser->getValue("content-type");
  sessionId = m_httpParser->getValue("x-apple-session-id");
  CStdString authorization = m_httpParser->getValue("authorization");
  int status = AIRPLAY_STATUS_OK;
  bool needAuth = false;

  if (ServerInstance->m_usePassword && !m_bAuthenticated)
  {
    needAuth = true;
  }

  int startQs = uri.Find('?');
  if (startQs != -1)
  {
    uri = uri.Left(startQs);
  }

  // This is the socket which will be used for reverse HTTP
  // negotiate reverse HTTP via upgrade
  if (uri == "/reverse")
  {
    status = AIRPLAY_STATUS_SWITCHING_PROTOCOLS;
    responseHeader = "Upgrade: PTTH/1.0\r\nConnection: Upgrade\r\n";
  }

  // The rate command is used to play/pause media.
  // A value argument should be supplied which indicates media should be played or paused.
  // 0.000000 => pause
  // 1.000000 => play
  else if (uri == "/rate")
  {
      const char* found = strstr(queryString.c_str(), "value=");
      int rate = found ? (int)(atof(found + strlen("value=")) + 0.5f) : 0;

      LOGD("AIRPLAY: got request %s with rate %i", uri.c_str(), rate);

      if (needAuth && !checkAuthorization(authorization, method, uri))
      {
        status = AIRPLAY_STATUS_NEED_AUTH;
      }
      else if (rate == 0)
      {
	if(!airplay_cb.IsPlayCompletion() && airplay_cb.IsPlaying())
        {
	  airplay_cb.setRate(0);
          ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_PAUSED);
        }
      }
      else
      {
	if(!airplay_cb.IsPlayCompletion() && airplay_cb.IsPaused())
        {
	  airplay_cb.setRate(1);
          ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_PLAYING);
        }
      }
  }
  
  // The volume command is used to change playback volume.
  // A value argument should be supplied which indicates how loud we should get.
  // 0.000000 => silent
  // 1.000000 => loud
  else if (uri == "/volume")
  {
      const char* found = strstr(queryString.c_str(), "volume=");
      float volume = found ? (float)strtod(found + strlen("volume="), NULL) : 0;

      LOGD("AIRPLAY: got request %s with volume %f", uri.c_str(), volume);

      if (needAuth && !checkAuthorization(authorization, method, uri))
      {
        status = AIRPLAY_STATUS_NEED_AUTH;
      }
      else if (volume >= 0 && volume <= 1)
      {
        int oldVolume = airplay_cb.getVolume();
        volume *= 100;
        if(oldVolume != (int)volume)
        {
	  airplay_cb.setVolume(volume);
        }
      }
  }


  // Contains a header like format in the request body which should contain a
  // Content-Location and optionally a Start-Position
  else if (uri == "/play")
  {
    CStdString location;
    float position = 0.0;
    m_lastEvent = EVENT_NONE;

    LOGD("AIRPLAY: got request %s", uri.c_str());

    if (needAuth && !checkAuthorization(authorization, method, uri))
    {
      status = AIRPLAY_STATUS_NEED_AUTH;
    }
    else if (contentType == "application/x-apple-binary-plist")
    {
      CAirPlayServer::m_isPlaying++;    
      
      if (1 /* m_pLibPlist->Load()*/)
      {
//        m_pLibPlist->EnableDelayedUnload(false);

        const char* bodyChr = m_httpParser->getBody();
        plist_t dict = NULL;
        plist_from_bin(bodyChr, m_httpParser->getContentLength(), &dict);

        if (plist_dict_get_size(dict))
        {
          plist_t tmpNode = plist_dict_get_item(dict, "Start-Position");
          if (tmpNode)
          {
            double tmpDouble = 0.0;
            plist_get_real_val(tmpNode, &tmpDouble);
            position = (float)tmpDouble;
          }

          tmpNode = plist_dict_get_item(dict, "Content-Location");
          if (tmpNode)
          {
            char *tmpStr = NULL;
            plist_get_string_val(tmpNode, &tmpStr);
            location=tmpStr;
LOGD("location = %s", tmpStr);
#ifdef TARGET_WINDOWS
            plist_free_string_val(tmpStr);
#else
            free(tmpStr);
#endif
          }

          if (dict)
          {
            plist_free(dict);
          }
        }
        else
        {
          LOGE("Error parsing plist");
        }
//        m_pLibPlist->Unload();
      }
    }
    else
    {
      CAirPlayServer::m_isPlaying++;        
      // Get URL to play
      int start = body.Find("Content-Location: ");
      if (start == -1)
        return AIRPLAY_STATUS_NOT_IMPLEMENTED;
      start += strlen("Content-Location: ");
      int end = body.Find('\n', start);
      location = body.Mid(start, end - start);

      start = body.Find("Start-Position");
      if (start != -1)
      {
        start += strlen("Start-Position: ");
        int end = body.Find('\n', start);
        CStdString positionStr = body.Mid(start, end - start);
        position = (float)atof(positionStr.c_str());
      }
    }

    if (status != AIRPLAY_STATUS_NEED_AUTH)
    {
      CStdString userAgent="AppleCoreMedia/1.0.0.8F455 (AppleTV; U; CPU OS 4_3 like Mac OS X; de_de)";
      Encode(userAgent);
      //location += "|User-Agent=" + userAgent;

      airplay_cb.playVideo(location, strlen(location), position * 100.0f);
      ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_PLAYING);
    }
  }

  // Used to perform seeking (POST request) and to retrieve current player position (GET request).
  // GET scrub seems to also set rate 1 - strange but true
  else if (uri == "/scrub")
  {
    if (needAuth && !checkAuthorization(authorization, method, uri))
    {
      status = AIRPLAY_STATUS_NEED_AUTH;
    }
    else if (method == "GET")
    {
      LOGD("AIRPLAY: got GET request %s", uri.c_str());
      if (airplay_cb.getTotalTime())
      {
        float position = (airplay_cb.getCurrentPosition())/1000;
	float duration =  (airplay_cb.getTotalTime())/1000;
        responseBody.Format("duration: %f\r\nposition: %f", duration, position);
      }
      else
      {
        status = AIRPLAY_STATUS_METHOD_NOT_ALLOWED;
      }
    }
    else
    {
      const char* found = strstr(queryString.c_str(), "position=");

      if (found)
      {
        float position = atof(found + strlen("position="));
	airplay_cb.seekPosition(position*1000);
        LOGD("AIRPLAY: got POST request %s with pos %f", uri.c_str(), position*1000);
      }
    }
  }

  // Sent when media playback should be stopped
  else if (uri == "/stop")
  {
    LOGD("AIRPLAY: got request %s", uri.c_str());
    if (needAuth && !checkAuthorization(authorization, method, uri))
    {
      status = AIRPLAY_STATUS_NEED_AUTH;
    }
    else
    {
      if (IsPlaying()) //only stop player if we started him
      {
        airplay_cb.stopVideo();
        CAirPlayServer::m_isPlaying--;
      }
      else //if we are not playing and get the stop request - we just wanna stop picture streaming
      {
	airplay_cb.closeWindow();
      }
      ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_STOPPED);
    }
  }

  // RAW JPEG data is contained in the request body
  else if (uri == "/photo")
  {
    LOGD("AIRPLAY: got request %s", uri.c_str());
    if (needAuth && !checkAuthorization(authorization, method, uri))
    {
      status = AIRPLAY_STATUS_NEED_AUTH;
    }
    else if (m_httpParser->getContentLength() > 0)
    {
      airplay_cb.showPhoto(m_httpParser->getBody(), m_httpParser->getContentLength());
    }
  }

  else if (uri == "/playback-info")
  {
    float position = 0.0f;
    float duration = 0.0f;
    float cachePosition = 0.0f;
    bool playing = false;

    LOGD("AIRPLAY: got request %s", uri.c_str());

    if (needAuth && !checkAuthorization(authorization, method, uri))
    {
      status = AIRPLAY_STATUS_NEED_AUTH;
    }
    else if (!airplay_cb.IsPlayCompletion())
    {
      if (airplay_cb.getTotalTime())
      {
        position = (airplay_cb.getCurrentPosition())/1000;
        duration = (airplay_cb.getTotalTime())/1000;
        playing = !airplay_cb.IsPaused();
        cachePosition = airplay_cb.getCachPosition();
      }

      responseBody.Format(PLAYBACK_INFO, duration, cachePosition, position, (playing ? 1 : 0), duration);
      responseHeader = "Content-Type: text/x-apple-plist+xml\r\n";

      if (airplay_cb.IsCaching())
      {
        ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_LOADING);
      }
      else if (playing)
      {
        ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_PLAYING);
      }
      else
      {
        ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_PAUSED);
      }
    }
    else
    {
      responseBody.Format(PLAYBACK_INFO_NOT_READY, duration, cachePosition, position, (playing ? 1 : 0), duration);
      responseHeader = "Content-Type: text/x-apple-plist+xml\r\n";     
      ComposeReverseEvent(reverseHeader, reverseBody, sessionId, EVENT_STOPPED);
    }
  }

  else if (uri == "/server-info")
  {
    LOGD("AIRPLAY: got request %s", uri.c_str());
    responseBody.Format(SERVER_INFO, airplay_cb.getDeviceId());
    responseHeader = "Content-Type: text/x-apple-plist+xml\r\n";
  }

  else if (uri == "/slideshow-features")
  {
    // Ignore for now.
  }

  else if (uri == "/authorize")
  {
    // DRM, ignore for now.
  }
  
  else if (uri == "/setProperty")
  {
    status = AIRPLAY_STATUS_NOT_FOUND;
  }

  else if (uri == "/getProperty")
  {
    status = AIRPLAY_STATUS_NOT_FOUND;
  }  

  else if (uri == "200") //response OK from the event reverse message
  {
    status = AIRPLAY_STATUS_NO_RESPONSE_NEEDED;
  }
  else
  {
    LOGE("AIRPLAY Server: unhandled request [%s]\n", uri.c_str());
    status = AIRPLAY_STATUS_NOT_IMPLEMENTED;
  }

  if (status == AIRPLAY_STATUS_NEED_AUTH)
  {
    ComposeAuthRequestAnswer(responseHeader, responseBody);
  }

  return status;
}

#endif
