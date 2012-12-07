/*
 * Many concepts and protocol specification in this code are taken
 * from Shairport, by James Laird.
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

#if !defined(TARGET_WINDOWS)
#pragma GCC diagnostic ignored "-Wwrite-strings"
#endif

#include <sys/stat.h>
#include <fcntl.h>

#include <unistd.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "Airtunes_jni.h"
#include "AirTunesServer.h"
#include <shairport.h>
//#ifdef HAS_AIRPLAY
//#include "network/AirPlayServer.h"
//#endif

#ifdef HAS_AIRTUNES

#include "StdString.h"
//#include "network/Zeroconf.h"
//#include "ApplicationMessenger.h"
//#include "filesystem/PipeFile.h"
//#include "Application.h"
//#include "cores/paplayer/BXAcodec.h"

//#include "MusicInfoTag.h"

//#include "FileItem.h"
//#include "GUIInfoManager.h"
//#include "guilib/GUIWindowManager.h"
//#include "utils/Variant.h"
//#include "settings/AdvancedSettings.h"
//#include "utils/EndianSwap.h"
//#include "URL.h"

#include <map>
#include <string>
#include <EndianSwap.h>

#include <math.h>
#define DLL_ON_OFF 0  //1==on ,,0==off
//using namespace XFILE;

//#if defined(TARGET_WINDOWS)
//DllLibShairplay *CAirTunesServer::m_pLibShairplay = NULL;
//#else
DllLibShairport *CAirTunesServer::m_pLibShairport = NULL;
//#endif by gao
CAirTunesServer *CAirTunesServer::ServerInstance = NULL;
CStdString CAirTunesServer::m_macAddress;  //
CStdString m_strMacAddress;

pthread_mutex_t airtunes_mutex;

//parse daap metadata - thx to project MythTV
std::map<std::string, std::string> decodeDMAP(const char *buffer, unsigned int size)
{
LOGE("Enter %s:%d",__func__,__LINE__); 
  std::map<std::string, std::string> result;
  unsigned int offset = 8;
  while (offset < size)
  {
    std::string tag;
    tag.append(buffer + offset, 4);
    offset += 4;
    uint32_t length = Endian_SwapBE32(*(uint32_t *)(buffer + offset));
    offset += sizeof(uint32_t);
    std::string content;
    content.append(buffer + offset, length);//possible fixme - utf8?
    offset += length;
    result[tag] = content;
  }
  return result;
}

void CAirTunesServer::SetMetadataFromBuffer(const char *buffer, unsigned int size)
{
LOGE("Enter %s:%d",__func__,__LINE__);
 // MUSIC_INFO::CMusicInfoTag tag;
  std::map<std::string, std::string> metadata = decodeDMAP(buffer, size);
  if(metadata["asal"].length())
  {
   // tag.SetAlbum(metadata["asal"]);//album
  }
  if(metadata["minm"].length())
  {	  
    //tag.SetTitle(metadata["minm"]);//title
  }
  if(metadata["asar"].length())
  {	  
    //tag.SetArtist(metadata["asar"]);//artist
  }
  //CApplicationMessenger::Get().SetCurrentSongTag(tag);*/
}

void CAirTunesServer::SetCoverArtFromBuffer(const char *buffer, unsigned int size)
{
LOGD("Enter %s:%d",__func__,__LINE__);
//  XFILE::CFile tmpFile;
  const char *tmpFileName = "special://temp/airtunes_album_thumb.jpg";

  if(!size)
    return;

  /*if (tmpFile.OpenForWrite(tmpFileName, true))
  {
    int writtenBytes=0;
    writtenBytes = tmpFile.Write(buffer, size);
    tmpFile.Close();

    if(writtenBytes)
    {
      //reset to empty before setting the new one
      //else it won't get refreshed because the name didn't change
      g_infoManager.SetCurrentAlbumThumb("");
      g_infoManager.SetCurrentAlbumThumb(tmpFileName);
      update the ui
      CGUIMessage msg(GUI_MSG_NOTIFY_ALL,0,0,GUI_MSG_REFRESH_THUMBS);
      g_windowManager.SendThreadMessage(msg);
    }
  }*xx*/
}

#if defined(TARGET_WINDOWS)
#define RSA_KEY " \
-----BEGIN RSA PRIVATE KEY-----\
MIIEpQIBAAKCAQEA59dE8qLieItsH1WgjrcFRKj6eUWqi+bGLOX1HL3U3GhC/j0Qg90u3sG/1CUt\
wC5vOYvfDmFI6oSFXi5ELabWJmT2dKHzBJKa3k9ok+8t9ucRqMd6DZHJ2YCCLlDRKSKv6kDqnw4U\
wPdpOMXziC/AMj3Z/lUVX1G7WSHCAWKf1zNS1eLvqr+boEjXuBOitnZ/bDzPHrTOZz0Dew0uowxf\
/+sG+NCK3eQJVxqcaJ/vEHKIVd2M+5qL71yJQ+87X6oV3eaYvt3zWZYD6z5vYTcrtij2VZ9Zmni/\
UAaHqn9JdsBWLUEpVviYnhimNVvYFZeCXg/IdTQ+x4IRdiXNv5hEewIDAQABAoIBAQDl8Axy9XfW\
BLmkzkEiqoSwF0PsmVrPzH9KsnwLGH+QZlvjWd8SWYGN7u1507HvhF5N3drJoVU3O14nDY4TFQAa\
LlJ9VM35AApXaLyY1ERrN7u9ALKd2LUwYhM7Km539O4yUFYikE2nIPscEsA5ltpxOgUGCY7b7ez5\
NtD6nL1ZKauw7aNXmVAvmJTcuPxWmoktF3gDJKK2wxZuNGcJE0uFQEG4Z3BrWP7yoNuSK3dii2jm\
lpPHr0O/KnPQtzI3eguhe0TwUem/eYSdyzMyVx/YpwkzwtYL3sR5k0o9rKQLtvLzfAqdBxBurciz\
aaA/L0HIgAmOit1GJA2saMxTVPNhAoGBAPfgv1oeZxgxmotiCcMXFEQEWflzhWYTsXrhUIuz5jFu\
a39GLS99ZEErhLdrwj8rDDViRVJ5skOp9zFvlYAHs0xh92ji1E7V/ysnKBfsMrPkk5KSKPrnjndM\
oPdevWnVkgJ5jxFuNgxkOLMuG9i53B4yMvDTCRiIPMQ++N2iLDaRAoGBAO9v//mU8eVkQaoANf0Z\
oMjW8CN4xwWA2cSEIHkd9AfFkftuv8oyLDCG3ZAf0vrhrrtkrfa7ef+AUb69DNggq4mHQAYBp7L+\
k5DKzJrKuO0r+R0YbY9pZD1+/g9dVt91d6LQNepUE/yY2PP5CNoFmjedpLHMOPFdVgqDzDFxU8hL\
AoGBANDrr7xAJbqBjHVwIzQ4To9pb4BNeqDndk5Qe7fT3+/H1njGaC0/rXE0Qb7q5ySgnsCb3DvA\
cJyRM9SJ7OKlGt0FMSdJD5KG0XPIpAVNwgpXXH5MDJg09KHeh0kXo+QA6viFBi21y340NonnEfdf\
54PX4ZGS/Xac1UK+pLkBB+zRAoGAf0AY3H3qKS2lMEI4bzEFoHeK3G895pDaK3TFBVmD7fV0Zhov\
17fegFPMwOII8MisYm9ZfT2Z0s5Ro3s5rkt+nvLAdfC/PYPKzTLalpGSwomSNYJcB9HNMlmhkGzc\
1JnLYT4iyUyx6pcZBmCd8bD0iwY/FzcgNDaUmbX9+XDvRA0CgYEAkE7pIPlE71qvfJQgoA9em0gI\
LAuE4Pu13aKiJnfft7hIjbK+5kyb3TysZvoyDnb3HOKvInK7vXbKuU4ISgxB2bB3HcYzQMGsz1qJ\
2gG0N5hvJpzwwhbhXqFKA4zaaSrw622wDniAK5MlIE0tIAKKP4yxNGjoD2QYjhBGuhvkWKY=\
-----END RSA PRIVATE KEY-----"

void CAirTunesServer::AudioOutputFunctions::audio_set_metadata(void *cls, void *session, const void *buffer, int buflen)
{
#error 444444444444444444444444444444
  CAirTunesServer::SetMetadataFromBuffer((char *)buffer, buflen);
}

void CAirTunesServer::AudioOutputFunctions::audio_set_coverart(void *cls, void *session, const void *buffer, int buflen)
{
#error 5555555555555555555555555555555555555555555
  CAirTunesServer::SetCoverArtFromBuffer((char *)buffer, buflen);
}

void* CAirTunesServer::AudioOutputFunctions::audio_init(void *cls, int bits, int channels, int samplerate)
{
#error 66666666666666666666666666666666666
LOGE("Enter %s:%d",__func__,__LINE__); 
/*  XFILE::CPipeFile *pipe=(XFILE::CPipeFile *)cls;
  pipe->OpenForWrite(XFILE::PipesManager::GetInstance().GetUniquePipeName());
  pipe->SetOpenThreashold(300);

  BXA_FmtHeader header;
  strncpy(header.fourcc, "BXA ", 4);
  header.type = BXA_PACKET_TYPE_FMT;
  header.bitsPerSample = bits;
  header.channels = channels;
  header.sampleRate = samplerate;
  header.durationMs = 0;

  if (pipe->Write(&header, sizeof(header)) == 0)
    return 0;

//  ThreadMessage tMsg = { TMSG_MEDIA_STOP };
//  CApplicationMessenger::Get().SendMessage(tMsg, true);

//  CFileItem item;
  item.SetPath(pipe->GetName());
  item.SetMimeType("audio/x-xbmc-pcm");

  ThreadMessage tMsg2 = { TMSG_GUI_ACTIVATE_WINDOW, WINDOW_VISUALISATION, 0 };
  CApplicationMessenger::Get().SendMessage(tMsg2, true);

  CApplicationMessenger::Get().PlayFile(item);*/

  return "XBMC-AirTunes";//session
}

void  CAirTunesServer::AudioOutputFunctions::audio_set_volume(void *cls, void *session, float volume)
{
#error 7777777777777777777777777
LOGE("Enter %s:%d",__func__,__LINE__);
  //volume from -144 - 0
  float volPercent = 1 - volume/-144;
//  g_application.SetVolume(volPercent, false);//non-percent volume 0.0-1.0
}

void  CAirTunesServer::AudioOutputFunctions::audio_process(void *cls, void *session, const void *buffer, int buflen)
{
#error 888888888888888888888888888888888
LOGE("Enter %s:%d",__func__,__LINE__); 
  #define NUM_OF_BYTES 64
//  XFILE::CPipeFile *pipe=(XFILE::CPipeFile *)cls;
  int sentBytes = 0;
  unsigned char buf[NUM_OF_BYTES];

  while (sentBytes < buflen)
  {
    int n = (buflen - sentBytes < NUM_OF_BYTES ? buflen - sentBytes : NUM_OF_BYTES);
    memcpy(buf, (char*) buffer + sentBytes, n);

    if (pipe->Write(buf, n) == 0)
      return;

    sentBytes += n;
  }
}

void  CAirTunesServer::AudioOutputFunctions::audio_flush(void *cls, void *session)
{
#error 999999999999999999999999999999
LOGE("Enter %s:%d",__func__,__LINE__);
 //XFILE::CPipeFile *pipe=(XFILE::CPipeFile *)cls;
 //pipe->Flush();
}

void  CAirTunesServer::AudioOutputFunctions::audio_destroy(void *cls, void *session)
{
#error 1010101010101101010
LOGE("Enter %s:%d",__func__,__LINE__); 
//  XFILE::CPipeFile *pipe=(XFILE::CPipeFile *)cls;
 // pipe->SetEof();
 // pipe->Close();

  //fix airplay video for ios5 devices
  //on ios5 when airplaying video
  //the client first opens an airtunes stream
  //while the movie is loading
  //in that case we don't want to stop the player here
  //because this would stop the airplaying video
#ifdef HAS_AIRPLAY
  if (!CAirPlayServer::IsPlaying())
#endif
  {
   // ThreadMessage tMsg = { TMSG_MEDIA_STOP };
  //  CApplicationMessenger::Get().SendMessage(tMsg, true);
   // CLog::Log(LOGDEBUG, "AIRTUNES: AirPlay not running - stopping player");
  }
}

void shairplay_log(int level, const char *msg)
{
#error 121212121212121212
LOGE("Enter %s:%d",__func__,__LINE__);
  int xbmcLevel = LOGINFO;
  switch(level)
  {
    case RAOP_LOG_EMERG:    // system is unusable 
      xbmcLevel = LOGFATAL;
      break;
    case RAOP_LOG_ALERT:    // action must be taken immediately
    case RAOP_LOG_CRIT:     // critical conditions
      xbmcLevel = LOGSEVERE;
      break;
    case RAOP_LOG_ERR:      // error conditions
      xbmcLevel = LOGERROR;
      break;
    case RAOP_LOG_WARNING:  // warning conditions
      xbmcLevel = LOGWARNING;
      break;
    case RAOP_LOG_NOTICE:   // normal but significant condition
      xbmcLevel = LOGNOTICE;
      break;
    case RAOP_LOG_INFO:     // informational
      xbmcLevel = LOGINFO;
      break;
    case RAOP_LOG_DEBUG:    // debug-level messages
      xbmcLevel = LOGDEBUG;
      break;
    default:
      break;
  }
    //CLog::Log(xbmcLevel, "AIRTUNES: %s", msg);
}

#else

/*struct ao_device_xbmc
{
//  XFILE::CPipeFile *pipe;
};*/

//audio output interface
void CAirTunesServer::AudioOutputFunctions::ao_initialize(void)
{
LOGE("Enter %s:%d",__func__,__LINE__);
}

void mergeBuf(unsigned char *buf,int length)
{
#define WRITE_BUF_SIZE 4096
	unsigned char wbuf[WRITE_BUF_SIZE];
	static int mergeBytes = 0;
	static int flag = 0;
	int first,second;

	if (!flag)
	{
		memset(wbuf,0,WRITE_BUF_SIZE);
		flag = 1;
	}
	if( (mergeBytes + length) < WRITE_BUF_SIZE )
	{
		memcpy(wbuf+mergeBytes,(char*)buf,length);
		mergeBytes += length;
	}
	else 
	{
		second = mergeBytes + length - WRITE_BUF_SIZE;
		first = length - second;

		memcpy(wbuf+mergeBytes,(char*)buf,first);
		writeBuf(wbuf,WRITE_BUF_SIZE);

		mergeBytes = 0;
		memcpy(wbuf+mergeBytes,(char*)(buf+first),second);
		mergeBytes += second;
	}
		
}	

int CAirTunesServer::AudioOutputFunctions::ao_play(ao_device *device, char *output_samples, uint32_t num_bytes)
{
  /*if (!device)
  {
    LOGE("return 0");
    return 0;
  }*/

  /*if (num_bytes && g_application.m_pPlayer)
    g_application.m_pPlayer->SetCaching(CACHESTATE_NONE);*///TODO

  //ao_device_xbmc* device_xbmc = (ao_device_xbmc*) device;

/*#define NUM_OF_BYTES 64
  unsigned int sentBytes = 0;
  unsigned char buf[NUM_OF_BYTES];*/
  /*unsigned char mybuf[40960];
  char path[] = "/sdcard/mymusic";
  int fd;
  fd = open(path,O_RDONLY,S_IRWXU | S_IRUSR | S_IWUSR | S_IXUSR);
  if (fd < 0)
  {
	  LOGE("open error");
	  LOGE("%s------\n", strerror(errno));
	  return -1;
  }*/
  
  /*while (sentBytes < num_bytes)
  {
    int n = (num_bytes - sentBytes < NUM_OF_BYTES ? num_bytes - sentBytes : NUM_OF_BYTES);
    memcpy(buf, (char*) output_samples + sentBytes, n);

//xx    if (device_xbmc->pipe->Write(buf, n) == 0)
//xx      return 0;
    //read(fd,mybuf,40960);
    //writeBuf(buf,n);
    mergeBuf(buf,n);
    //write(fd,buf,n);
    sentBytes += n;
  }*/
  //close(fd);
  writeBuf((unsigned char*)output_samples,num_bytes);
  return 1;
}

int CAirTunesServer::AudioOutputFunctions::ao_default_driver_id(void)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  return 0;
}

ao_device* CAirTunesServer::AudioOutputFunctions::ao_open_live(int driver_id, ao_sample_format *format,
    ao_option *option)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  /*ao_device_xbmc* device = new ao_device_xbmc();

  device->pipe = new XFILE::CPipeFile;
  device->pipe->OpenForWrite(XFILE::PipesManager::GetInstance().GetUniquePipeName());
  device->pipe->SetOpenThreashold(300);

  BXA_FmtHeader header;
  strncpy(header.fourcc, "BXA ", 4);
  header.type = BXA_PACKET_TYPE_FMT;
  header.bitsPerSample = format->bits;
  header.channels = format->channels;
  header.sampleRate = format->rate;
  header.durationMs = 0;

  if (device->pipe->Write(&header, sizeof(header)) == 0)
    return 0;*/
  
  /*ThreadMessage tMsg = { TMSG_MEDIA_STOP };
  CApplicationMessenger::Get().SendMessage(tMsg, true);

  CFileItem item;
  item.SetPath(device->pipe->GetName());
  item.SetMimeType("audio/x-xbmc-pcm");

  if (ao_get_option(option, "artist"))
    item.GetMusicInfoTag()->SetArtist(ao_get_option(option, "artist"));

  if (ao_get_option(option, "album"))
    item.GetMusicInfoTag()->SetAlbum(ao_get_option(option, "album"));

  if (ao_get_option(option, "name"))
    item.GetMusicInfoTag()->SetTitle(ao_get_option(option, "name"));*/
  if (ao_get_option(option,"artist"))
		  LOGD("############################artist = %s",ao_get_option(option, "artist"));
  if (ao_get_option(option, "album"))
		  LOGD("############################album = %s",ao_get_option(option, "album"));
  if (ao_get_option(option, "name"))
		  LOGD("############################name = %s",ao_get_option(option, "name"));

  /*ThreadMessage tMsg2 = { TMSG_GUI_ACTIVATE_WINDOW, WINDOW_VISUALISATION, 0 };
  CApplicationMessenger::Get().SendMessage(tMsg2, true);

  CApplicationMessenger::Get().PlayFile(item);

  return (ao_device*) device;*/
	return NULL;
}

int CAirTunesServer::AudioOutputFunctions::ao_close(ao_device *device)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  /*ao_device_xbmc* device_xbmc = (ao_device_xbmc*) device;
  device_xbmc->pipe->SetEof();
  device_xbmc->pipe->Close();
  delete device_xbmc->pipe;

  //fix airplay video for ios5 devices
  //on ios5 when airplaying video
  //the client first opens an airtunes stream
  //while the movie is loading
  //in that case we don't want to stop the player here
  //because this would stop the airplaying video
#ifdef HAS_AIRPLAY
  if (!CAirPlayServer::IsPlaying())
#endif
  {
    ThreadMessage tMsg = { TMSG_MEDIA_STOP };
    CApplicationMessenger::Get().SendMessage(tMsg, true);
    CLog::Log(LOGDEBUG, "AIRTUNES: AirPlay not running - stopping player");
  }

  delete device_xbmc;*xin.xu*/

  return 0;
}

void CAirTunesServer::AudioOutputFunctions::ao_set_metadata(const char *buffer, unsigned int size)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  CAirTunesServer::SetMetadataFromBuffer(buffer, size);
}

void CAirTunesServer::AudioOutputFunctions::ao_set_metadata_coverart(const char *buffer, unsigned int size)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  CAirTunesServer::SetCoverArtFromBuffer(buffer, size);
}

/* -- Device Setup/Playback/Teardown -- */
int CAirTunesServer::AudioOutputFunctions::ao_append_option(ao_option **options, const char *key, const char *value)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  ao_option *op, *list;

  op = (ao_option*) calloc(1,sizeof(ao_option));
  if (op == NULL) return 0;

  op->key = strdup(key);
  op->value = strdup(value?value:"");
  op->next = NULL;

  if ((list = *options) != NULL)
  {
    list = *options;
    while (list->next != NULL)
      list = list->next;
    list->next = op;
  }
  else
  {
    *options = op;
  }

  return 1;
}

void CAirTunesServer::AudioOutputFunctions::ao_free_options(ao_option *options)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  ao_option *rest;

  while (options != NULL)
  {
    rest = options->next;
    free(options->key);
    free(options->value);
    free(options);
    options = rest;
  }
}

char* CAirTunesServer::AudioOutputFunctions::ao_get_option(ao_option *options, const char* key)
{
LOGD("Enter %s:%d",__func__,__LINE__);

  while (options != NULL)
  {
    if (strcmp(options->key, key) == 0)
      return options->value;
    options = options->next;
  }

  return NULL;
}

int shairport_log(const char* msg, size_t msgSize)
{
  /*if( g_advancedSettings.m_logEnableAirtunes)
  {
    xx CLog::Log(LOGDEBUG, "AIRTUNES: %s", msg);
  }*xx*/
  return 1;
}
#endif

bool CAirTunesServer::StartServer(int port, bool nonlocal, bool usePassword, const CStdString &password/*=""*/)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  bool success = false;
  CStdString pw = password;
  //xx CNetworkInterface *net = g_application.getNetwork().GetFirstConnectedInterface();
  StopServer(true);

  /*if (net)
  {
    m_macAddress = net->GetMacAddress();
    m_macAddress.Replace(":","");
    while (m_macAddress.size() < 12)
    {
      m_macAddress = CStdString("0") + m_macAddress;
    }
  }
  else
  {
    m_macAddress = "000102030405";
  }*xx*/
	//added by xx

  if (!usePassword)
  {
    pw.Empty();
  }

  ServerInstance = new CAirTunesServer(port, nonlocal);
  if (ServerInstance->Initialize(password))
  {
//#ifndef TARGET_WINDOWS
    ServerInstance->Create();
//#endif by gao
    success = true;
  }

 /* if (success)
  {
    CStdString appName;
    appName.Format("%s@%s", m_macAddress.c_str(), "AirTunesServer");//g_infoManager.GetLabel(SYSTEM_FRIENDLY_NAME).c_str());

    std::map<std::string, std::string> txt;
    txt["cn"] = "0,1";
    txt["ch"] = "2";
    txt["ek"] = "1";
    txt["et"] = "0,1";
    txt["sv"] = "false";
    txt["tp"] = "UDP";
    txt["sm"] = "false";
    txt["ss"] = "16";
    txt["sr"] = "44100";
    txt["pw"] = "false";
    txt["vn"] = "3";
    txt["da"] = "true";
    txt["vs"] = "130.14";
    txt["md"] = "0,1,2";
    txt["txtvers"] = "1";

    //xx CZeroconf::GetInstance()->PublishService("servers.airtunes", "_raop._tcp", appName, port, txt);
  } register in jmdns by gao*/

  return success;
}

void *CAirTunesServer::Run(void *arg)
{
LOGD("Enter %s:%d",__func__,__LINE__);
   ServerInstance->Process();
   return (void *)(0);
}

void CAirTunesServer::Create(void)
{
LOGD("Enter %s:%d",__func__,__LINE__);
   pthread_t pid = 0;
   pthread_attr_t attr;
   pthread_attr_init(&attr);

   pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED); 
   if (pthread_create(&pid, &attr, (void*(*)(void*))&Run, NULL) != 0)
   {
	   LOGD("thread create error!\n");
   }
   pthread_attr_destroy(&attr);
}
void CAirTunesServer::StopServer(bool bWait)
{
LOGD("Enter %s:%d",__func__,__LINE__);

  pthread_mutex_lock(&airtunes_mutex);
  if (ServerInstance)
  {
//#if !defined(TARGET_WINDOWS)
#if DLL_ON_OFF
    if (m_pLibShairport->IsLoaded())
    {
      m_pLibShairport->shairport_exit();
    }
#endif
//#endif by gao
    //ServerInstance->StopThread(bWait);
    ServerInstance->m_stop = true;
    ServerInstance->Deinitialize();
    if (bWait)
    {
      delete ServerInstance;
      ServerInstance = NULL;
    }

    //xx CZeroconf::GetInstance()->RemoveService("servers.airtunes");
  }
  pthread_mutex_unlock(&airtunes_mutex);
}

CAirTunesServer::CAirTunesServer(int port, bool nonlocal) //xx : CThread("AirTunesServer")
{
LOGD("Enter %s:%d",__func__,__LINE__);
  m_port = port;
/*#if defined(TARGET_WINDOWS)
  m_pLibShairplay = new DllLibShairplay();
  m_pPipe         = new XFILE::CPipeFile;  
#else*/
#if DLL_ON_OFF
  m_pLibShairport = new DllLibShairport();
#endif
//#endif by gao
}

CAirTunesServer::~CAirTunesServer()
{
LOGD("Enter %s:%d",__func__,__LINE__);
/*#if defined(TARGET_WINDOWS)
  if (m_pLibShairplay->IsLoaded())
  {
    m_pLibShairplay->Unload();
  }
  delete m_pLibShairplay;
  delete m_pPipe;
#else by gao*/
#if DLL_ON_OFF
  if (m_pLibShairport->IsLoaded())
  {
    m_pLibShairport->Unload();
  }
  delete m_pLibShairport;
#endif
//#endif
}

void CAirTunesServer::Process()
{
LOGD("Enter %s:%d",__func__,__LINE__);
  
  pthread_mutex_lock(&airtunes_mutex);
  ServerInstance->m_stop = false;

//#if !defined(TARGET_WINDOWS)
#if DLL_ON_OFF
  while (!ServerInstance->m_stop && m_pLibShairport->shairport_is_running())
  {
    m_pLibShairport->shairport_loop();
  }
#else
  while (!ServerInstance->m_stop)
  {
    shairport_loop();
  }
#endif
//#endif by gao
  pthread_mutex_unlock(&airtunes_mutex);
}

//get host mac address
int GetMac( const char *ifname, char *mac )                                                                 
{  
    int sock, ret;
    struct ifreq ifr;
    sock = socket( AF_INET, SOCK_STREAM, 0 );                     
    if ( sock < 0 )                                      
    {       
        perror( "socket" );  
	return -1;
    }
    memset( &ifr, 0, sizeof(ifr) ); 
    strcpy( ifr.ifr_name, ifname );
    ret = ioctl( sock, SIOCGIFHWADDR, &ifr, sizeof(ifr) );                                                           
    if ( ret == 0 )
    {       
        memcpy( mac, ifr.ifr_hwaddr.sa_data, 6 );                                                                    
    }
    else 
    {       
        perror( "ioctl" );                                         
    }
    close( sock );                                                   
    return ret;                                 
}                      

bool CAirTunesServer::Initialize(const CStdString &password)
{
LOGD("Enter %s:%d",__func__,__LINE__);
  Deinitialize();
/*#if defined(TARGET_WINDOWS)
  if (m_pLibShairplay->Load())
  {

    raop_callbacks_t ao;
    ao.cls                  = m_pPipe;
    ao.audio_init           = AudioOutputFunctions::audio_init;
    ao.audio_set_volume     = AudioOutputFunctions::audio_set_volume;
    ao.audio_set_metadata   = AudioOutputFunctions::audio_set_metadata;
    ao.audio_set_coverart   = AudioOutputFunctions::audio_set_coverart;
    ao.audio_process        = AudioOutputFunctions::audio_process;
    ao.audio_flush          = AudioOutputFunctions::audio_flush;
    ao.audio_destroy        = AudioOutputFunctions::audio_destroy;
    m_pLibShairplay->EnableDelayedUnload(false);
    m_pRaop = m_pLibShairplay->raop_init(1, &ao, RSA_KEY);//1 - we handle one client at a time max
    ret = m_pRaop != NULL;    

    if(ret)
    {
      char macAdr[6];    
      unsigned short port = (unsigned short)m_port;
      
      m_pLibShairplay->raop_set_log_level(m_pRaop, RAOP_LOG_WARNING);
      if(g_advancedSettings.m_logEnableAirtunes)
      {
        m_pLibShairplay->raop_set_log_level(m_pRaop, RAOP_LOG_DEBUG);
      }

      m_pLibShairplay->raop_set_log_callback(m_pRaop, shairplay_log);

      CNetworkInterface *net = g_application.getNetwork().GetFirstConnectedInterface();

      if (net)
      {
        net->GetMacAddressRaw(macAdr);
      }

      ret = m_pLibShairplay->raop_start(m_pRaop, &port, macAdr, 6, password.c_str()) >= 0;
    }
  }

#else */ 
  int numArgs = 3;

  int ret;
  char switchMac[13];
  char mac[6];
  char ifname[IFNAMSIZ];

  CStdString hwStr;
  CStdString pwStr;
  CStdString portStr;
  
  memset(switchMac, 0 , 6);
  strcpy( ifname, "eth0" );
  memset( mac, 0, sizeof(mac) );
  ret = GetMac( ifname, mac);
  if ( ret == 0 )
       LOGD("get mac addresss success!");
  else  
       LOGD("Can't get mac address!");

  sprintf(switchMac, "%02x%02x%02x%02x%02x%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  
  hwStr.Format("--mac=%s",switchMac);
  pwStr.Format("--password=%s",password.c_str());
  portStr.Format("--server_port=%d",m_port);

  if (!password.empty())
  {
    numArgs++;
  }
  LOGD("AirTunesServer Initialize MAC = %s!", m_strMacAddress.c_str());
  char *argv[] = { "--apname=XBMC", (char*) portStr.c_str(), (char*) m_strMacAddress.c_str(), (char *)pwStr.c_str(), NULL };
#if DLL_ON_OFF
  if (m_pLibShairport->Load())
#endif
  {
    struct AudioOutput ao;
    ao.ao_initialize = AudioOutputFunctions::ao_initialize;
    ao.ao_play = AudioOutputFunctions::ao_play;
    ao.ao_default_driver_id = AudioOutputFunctions::ao_default_driver_id;
    ao.ao_open_live = AudioOutputFunctions::ao_open_live;
    ao.ao_close = AudioOutputFunctions::ao_close;
    ao.ao_append_option = AudioOutputFunctions::ao_append_option;
    ao.ao_free_options = AudioOutputFunctions::ao_free_options;
    ao.ao_get_option = AudioOutputFunctions::ao_get_option;
/*#ifdef HAVE_STRUCT_AUDIOOUTPUT_AO_SET_METADATA
    ao.ao_set_metadata = AudioOutputFunctions::ao_set_metadata;
    ao.ao_set_metadata_coverart = AudioOutputFunctions::ao_set_metadata_coverart;
#endif by gao*/
    //xx struct printfPtr funcPtr;
    //funcPtr.extprintf = shairport_log;
#if DLL_ON_OFF
    m_pLibShairport->EnableDelayedUnload(false);
    m_pLibShairport->shairport_set_ao(&ao);
    //xx m_pLibShairport->shairport_set_printf(&funcPtr);
    m_pLibShairport->shairport_main(numArgs, argv);
    EnableDelayedUnload(false);
#else
    
    shairport_set_ao(&ao);
    //xx m_pLibShairport->shairport_set_printf(&funcPtr);
    shairport_main(numArgs, argv);
#endif
    ret = true;
    LOGD("AirTunesServer Initialize Success!");
  }
//#endif 
  return ret;
}

void CAirTunesServer::Deinitialize()
{
LOGD("Enter %s:%d",__func__,__LINE__);
/*#if defined(TARGET_WINDOWS)
  if (m_pLibShairplay && m_pLibShairplay->IsLoaded())
  {
    m_pLibShairplay->raop_stop(m_pRaop);
    m_pLibShairplay->raop_destroy(m_pRaop);
    m_pLibShairplay->Unload();
  }
#else by gao*/
#if DLL_ON_OFF
  if (m_pLibShairport && m_pLibShairport->IsLoaded())
  {
    m_pLibShairport->shairport_exit();
    m_pLibShairport->Unload();
  }
#else
  if (m_pLibShairport)
  {
    shairport_exit();
  }
#endif
//#endif by gao
}

#endif

