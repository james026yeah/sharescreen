#include <jni.h>
#include <unistd.h>
#include <android/log.h>
#include "jni_register.h"
#include "AirPlayServer.h"
#include "AirPlayCallBack.h"
#include "dnssd.h"
#include "string"
#undef LOG_TAG
#define LOG_TAG "AIRPLAY_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

static JavaVM *g_javavm;
static jobject g_notify_obj;
static char j2c_string[128];

char* jstringTostring(JNIEnv* env, jstring jstr)  
{          
    int len = env->GetStringLength(jstr);
    env->GetStringUTFRegion(jstr, 0, len, j2c_string);
    LOGD("%s %s\n", __func__, j2c_string);
    return j2c_string;
}
    
bool IsPlayCompletion()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jboolean rate;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "IsPlayCompletion", "()Z");
    rate = env->CallIntMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return rate;
}

void setRate(int rate)
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "setRate", "(I)V");
    env->CallVoidMethod(g_notify_obj, m_id_callback, rate);
    g_javavm->DetachCurrentThread();
    return;
}

void setVolume(int volume)
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "setVolume", "(I)V");
    env->CallVoidMethod(g_notify_obj, m_id_callback, volume);
    g_javavm->DetachCurrentThread();
    return;
}

int getVolume()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jint volume;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "getVolume", "()I");
    volume = env->CallIntMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return volume;
}

void playVideo(const char * url,int length,float startPositon)
{
    LOGD("This %s be call!\n", __func__);
    LOGD("startPositon = %f\n", startPositon);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "playVideo", "(Ljava/lang/String;IF)V");
    env->CallVoidMethod(g_notify_obj, m_id_callback, env->NewStringUTF(url), length, startPositon);
    g_javavm->DetachCurrentThread();
    return;
}

float getTotalTime()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jfloat totaltime;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "getTotalTime", "()F");
    totaltime = env->CallIntMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return totaltime;
}

float getCurrentPosition()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jfloat currentposition;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "getCurrentPosition", "()F");
    currentposition = env->CallFloatMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return currentposition;
}

void seekPosition(float position)
{
    LOGD("This %s be call!\n", __func__);
    LOGD("seekPosition = %f\n", position);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "seekPosition", "(F)V");
    env->CallVoidMethod(g_notify_obj, m_id_callback, position);
    g_javavm->DetachCurrentThread();
    return;
}

void stopVideo()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "stopVideo", "()V");
    env->CallVoidMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return;
}

void closeWindow()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "closeWindow", "()V");
    env->CallVoidMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return;
}

void showPhoto(const char *data, int length)
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "showPhoto", "([BI)V");
    if (env->EnsureLocalCapacity(2) < 0) {
        LOGD("out of memory erro %s !\n", __func__); 
        return; /* out of memory error */
    }
    LOGD("jmethodID %s be call!\n", __func__);
    jbyteArray bytes = env->NewByteArray(length);
    LOGD("jbyteArray %s be call!\n", __func__);
    env->SetByteArrayRegion(bytes, 0, length, (jbyte*)data);
    LOGD("SetByteArrayRegion %s be call!\n", __func__);
    env->CallVoidMethod(g_notify_obj, m_id_callback, bytes, length);
    LOGD("CallVoidMethod %s be call!\n", __func__);
    env->DeleteLocalRef(bytes);
    g_javavm->DetachCurrentThread();
    return;
}

bool IsCaching()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jboolean  iscaching;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "IsCaching", "()Z");
    iscaching = env->CallBooleanMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return iscaching;
}

bool IsPlaying()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jboolean  isplaying;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "IsPlaying", "()Z");
    isplaying = env->CallBooleanMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return isplaying;
}

bool IsPaused()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jboolean ispaused;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "IsPaused", "()Z");
    ispaused = env->CallBooleanMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return ispaused;
}

float getCachPosition()
{
    LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jfloat cachposition;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "getCachPosition", "()F");
    cachposition = env->CallFloatMethod(g_notify_obj, m_id_callback);
    g_javavm->DetachCurrentThread();
    return cachposition;
}

char* getDeviceId()
{
   /* LOGD("This %s be call!\n", __func__);
    JNIEnv *env;
    jstring deviceid;
    char * str;
    g_javavm->AttachCurrentThread(&env, NULL);
    jclass notifier_class = env->GetObjectClass(g_notify_obj);
    jmethodID m_id_callback = env->GetMethodID(notifier_class, "getDeviceId", "()Ljava/lang/String");
    LOGD("jmethodID %s be call!\n", __func__);
    deviceid = (jstring)env->CallObjectMethod(g_notify_obj, m_id_callback);
    LOGD("deviceid %s be call!\n", __func__);
    jstringTostring(env, deviceid);
    LOGD("jstringTostring %s be call!\n", __func__);
    g_javavm->DetachCurrentThread();
    return j2c_string;*/
	return "11:22:33:44:55:66";
}

JNIEXPORT jint JNICALL JNIDEFINE(doCallBackWork)(JNIEnv * env, jclass obj, jobject notifier_obj)

{
    LOGD("This %s be call!\n", __func__);
    env->GetJavaVM(&g_javavm);
    g_notify_obj = env->NewGlobalRef(notifier_obj);
    getDeviceId();
    return 1;
}

JNIEXPORT jboolean JNICALL JNIDEFINE(startService)(JNIEnv *, jclass)
{
    LOGD("IN %s\n", __func__);
    struct airplay_callback cb;
    int listenPort = 36667;
    CStdString password = "";
    bool usePassword = false;
    const char *name = "AppleTV";
    unsigned short raop_port = 36667;
    const char hwaddr[] = { 0x48, 0x5d, 0x60, 0x7c, 0xee, 0x22 };
    dnssd_t *dnssd;
    cb.closeWindow = closeWindow;
    cb.getCachPosition = getCachPosition;
    cb.getCurrentPosition = getCurrentPosition;
    cb.IsPlayCompletion = IsPlayCompletion;
    cb.getTotalTime = getTotalTime;
    cb.getVolume = getVolume;
    cb.IsCaching = IsCaching;
    cb.IsPaused = IsPaused;
    cb.IsPlaying = IsPlaying;
    cb.playVideo = playVideo;
    cb.seekPosition = seekPosition;
    cb.setRate = setRate;
    cb.setVolume = setVolume;
    cb.showPhoto = showPhoto;
    cb.stopVideo  = stopVideo;
    cb.getDeviceId = &getDeviceId;
    if (CAirPlayServer::StartServer(listenPort, true, &cb)) {
            CAirPlayServer::SetCredentials(usePassword, password);
            return true;
    }
    return false;
}
JNIEXPORT void JNICALL JNIDEFINE(stopService)(JNIEnv *, jclass)
{
    LOGD("This %s be call!\n", __func__);
    CAirPlayServer::StopServer(true);
    return;
}

#ifdef __cplusplus
}// end of extern "C"
#endif
