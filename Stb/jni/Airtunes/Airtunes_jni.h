/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_archermind_airtunes_NativeAirtunes */

#ifndef _Included_com_archermind_airtunes_NativeAirtunes
#define _Included_com_archermind_airtunes_NativeAirtunes
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     JNI_REGISTER
 * Method:    doCallBackWork
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jint JNICALL Java_archermind_airtunes_NativeAirtunes_doCallBackWork
  (JNIEnv * env, jclass obj, jobject notifier_obj);

void writeBuf(unsigned char *data, int length);

void setAlbum(const char *album_name, int length);  //album name
void setTitle(const char *song_name, int length);  //song name
void setArtist(const char *singer_name, int length); //singer name
void setAlbumThumb(const char *data, int length);  //album covert

void setPlayStatus(int state);//0-pause;1-playing
void setVolume(float vol);//-144:mutex; -30 —— :normal
void setDeviceTab(const char *apple_deviceId, int length); //apple device mac address
/*
 * Class:     com_archermind_airtunes_NativeAirtunes
 * Method:    startAirtunes
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_archermind_airtunes_NativeAirtunes_startAirtunes
  (JNIEnv *env, jclass obj,jstring jstrMac,jint nPort);

/*
 * Class:     com_archermind_airtunes_NativeAirtunes
 * Method:    stopAirtunes
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_archermind_airtunes_NativeAirtunes_stopAirtunes
  (JNIEnv *, jclass);
#ifdef __cplusplus
}
#endif
#endif
