#include <jni.h>
#include <unistd.h>
#include <android/log.h>
#include "Airtunes_jni.h"
#include "AirTunesServer.h"

#undef LOG_TAG
#define LOG_TAG "AIRTUNES_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_archermind_airtunes_NativeAirtunes
 * Method:    init
 * Signature: ()I
 */

static JavaVM *g_javavm;
static jobject g_notify_obj;
extern CStdString m_strMacAddress;

void setPlayStatus(int state)
{
	JNIEnv *env;
	g_javavm->AttachCurrentThread(&env, NULL);
	jclass notifier_class = env->GetObjectClass(g_notify_obj);
	jmethodID m_id_callback = env->GetMethodID(notifier_class, "setPlayStatus", "(I)V");
	env->CallVoidMethod(g_notify_obj, m_id_callback, state);
	g_javavm->DetachCurrentThread();
	return;
}

void setVolume(float vol)
{
   JNIEnv *env;
   g_javavm->AttachCurrentThread(&env, NULL);
   jclass notifier_class = env->GetObjectClass(g_notify_obj);
   jmethodID m_id_callback = env->GetMethodID(notifier_class, "setVolume", "(F)V");
   env->CallVoidMethod(g_notify_obj, m_id_callback, vol);
   g_javavm->DetachCurrentThread();
   return;
}

void setDeviceTab(const char *apple_deviceId, int length)
{
  JNIEnv *env;
  g_javavm->AttachCurrentThread(&env, NULL);
  jclass notifier_class = env->GetObjectClass(g_notify_obj);
  jmethodID m_id_callback = env->GetMethodID(notifier_class, "setDeviceTab", "(Ljava/lang/String;I)V");
  env->CallVoidMethod(g_notify_obj, m_id_callback, env->NewStringUTF(apple_deviceId), length);
  g_javavm->DetachCurrentThread();
  return;
}

void setAlbum(const char *album_name, int length)
{
  JNIEnv *env;
  g_javavm->AttachCurrentThread(&env, NULL);
  jclass notifier_class = env->GetObjectClass(g_notify_obj);
  jmethodID m_id_callback = env->GetMethodID(notifier_class, "setAlbum", "(Ljava/lang/String;I)V");
  env->CallVoidMethod(g_notify_obj, m_id_callback, env->NewStringUTF(album_name), length);
  g_javavm->DetachCurrentThread();
  return;
}

void setTitle(const char *song_name, int length)
{
  JNIEnv *env;
  g_javavm->AttachCurrentThread(&env, NULL);
  jclass notifier_class = env->GetObjectClass(g_notify_obj);
  jmethodID m_id_callback = env->GetMethodID(notifier_class, "setTitle", "(Ljava/lang/String;I)V");
  env->CallVoidMethod(g_notify_obj, m_id_callback, env->NewStringUTF(song_name), length);
  g_javavm->DetachCurrentThread();
  return;
}

void setArtist(const char *singer_name, int length)
{
  JNIEnv *env;
  g_javavm->AttachCurrentThread(&env, NULL);
  jclass notifier_class = env->GetObjectClass(g_notify_obj);
  jmethodID m_id_callback = env->GetMethodID(notifier_class, "setArtist", "(Ljava/lang/String;I)V");
  env->CallVoidMethod(g_notify_obj, m_id_callback, env->NewStringUTF(singer_name), length);
  g_javavm->DetachCurrentThread();
  return;
}

void setAlbumThumb(const char *data, int length)
{
  JNIEnv *env;
  g_javavm->AttachCurrentThread(&env, NULL);
  jclass notifier_class = env->GetObjectClass(g_notify_obj);
  jmethodID m_id_callback = env->GetMethodID(notifier_class, "setAlbumThumb", "([BI)V");
  if (env->EnsureLocalCapacity(2) < 0) 
  {
		  LOGD("out of memory erro %s !\n", __func__);
		  return;
  }
  jbyteArray bytes = env->NewByteArray(length);
  env->SetByteArrayRegion(bytes, 0, length, (jbyte*)data);
  env->CallVoidMethod(g_notify_obj, m_id_callback, bytes, length);
  env->DeleteLocalRef(bytes);
  g_javavm->DetachCurrentThread();
  return;
}

void writeBuf(unsigned char *data, int length)
{
    JNIEnv *env;
    jmethodID m_id_callback;
    LOGD("xuyafei.......... %s be call!\n", __func__);
    g_javavm->AttachCurrentThread(&env, NULL);
    if (g_notify_obj)
    {
      jclass notifier_class = env->GetObjectClass(g_notify_obj);
      m_id_callback = env->GetMethodID(notifier_class, "writeBuf", "([BI)V");
    }
    if (env->EnsureLocalCapacity(2) < 0) {
        return; /* out of memory error */
    }
    jbyteArray bytes = env->NewByteArray(length);
    env->SetByteArrayRegion(bytes, 0, length, (jbyte*)data);
    env->CallVoidMethod(g_notify_obj, m_id_callback, bytes, length);
    env->DeleteLocalRef(bytes);
    g_javavm->DetachCurrentThread();
    return;
}
 
JNIEXPORT jint JNICALL Java_archermind_airtunes_NativeAirtunes_doCallBackWork(JNIEnv * env, jclass obj, jobject notifier_obj)
{
    LOGD("This %s be call!\n", __func__);
    env->GetJavaVM(&g_javavm);
    g_notify_obj = env->NewGlobalRef(notifier_obj);
    return 1;
}
 

JNIEXPORT jint JNICALL Java_archermind_airtunes_NativeAirtunes_startAirtunes
  (JNIEnv *env, jclass obj,jstring jstrMac,jint nPort)
{
	LOGD("IN %s\n", __func__);
	char j2c_MacAddressString[128];
	//int listenPort = 49152;
    int listenPort = nPort;
        
    int len = env->GetStringLength(jstrMac);
    env->GetStringUTFRegion(jstrMac, 0, len, j2c_MacAddressString);
	
 	m_strMacAddress.Format("--mac=%s",j2c_MacAddressString);
 	LOGD("AirTunesServer Initialize MACddd = %s!", m_strMacAddress.c_str());
    CStdString password = "";
    bool usePassword = false;

    if (CAirTunesServer::StartServer(listenPort, true, usePassword, password)) {
		LOGD("StartServer Success !");
    }

	return 0;
}

JNIEXPORT jint JNICALL Java_archermind_airtunes_NativeAirtunes_stopAirtunes
  (JNIEnv *, jclass)
{
	LOGD("IN %s\n", __func__);
    CAirTunesServer::StopServer(true);

	return 0;
}
#ifdef __cplusplus
}// end of extern "C"
#endif
