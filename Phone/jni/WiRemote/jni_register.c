#include <jni.h>
#include <unistd.h>
#include <android/log.h>
#include "wi_remote.h"

#undef LOG_TAG
#define LOG_TAG "WIREMOTE_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)
#define  JNIDEFINE(fname) Java_com_archermind_ashare_wiremote_natives_WiRemoteAgent_##fname
#define VIRTUAL_SERVER_WIDTH	4096
#define VIRTUAL_SERVER_HEIGHT	4096

static JavaVM *g_javavm = NULL;
static char j2c_string[128];
static jclass jni_class;
static jmethodID jni_method;
static float x_scale, y_scale;
static int initialed = 0;

static char *jstring2cstring(JNIEnv *env, jstring string)
{
	int len = (*env)->GetStringLength(env, string);
	(*env)->GetStringUTFRegion(env, string, 0, len, j2c_string);
	LOGD("%s %s\n", __func__, j2c_string);
	return j2c_string;
}

jint JNIDEFINE(init)(JNIEnv *env, jobject obj, int width, int height) 
{
	jni_class = obj;
	jni_method = 0;
	if(!g_javavm)
		(*env)->GetJavaVM(env, &g_javavm);
	if(!initialed){
		init_wiremote();
		initialed = 1;
	}
	LOGD("width = %d, height = %d\n", width, height);
	x_scale = (float)VIRTUAL_SERVER_WIDTH/height;
	y_scale = (float)VIRTUAL_SERVER_HEIGHT/width;
	LOGD("x_scale = %f, y_scale = %f\n", x_scale, y_scale);
	jni_method = (*env)->GetStaticMethodID(env, jni_class, "callback", "(Ljava/lang/String;)V");
	return 0;
}

jint JNIDEFINE(deinit)(JNIEnv *env, jobject obj) 
{
	LOGD("deinit wi_remote\n");
	if(initialed){
		deinit_wiremote();
		initialed = 0;
	}
	return 0;
}

jint JNIDEFINE(connectServer)(JNIEnv *env, jobject obj, jstring string)
{
	int len;
	char *cstring;
	cstring = jstring2cstring(env, string);
	len = strlen(cstring);
	if(len >= 7 && len <=15)
		connect_server(cstring);
	else
		LOGD("ip address error!\n");
	return 0;
}

jint JNIDEFINE(gyroMouseControl)(JNIEnv *env, jobject obj, int enable)
{
	LOGD("IN %s\n", __func__);
	gyro_mouse_cotrol(enable);
	return 0;
}

jint JNIDEFINE(setKeyEvent)(JNIEnv *env, jobject obj, int press, jint code)
{
	LOGD("IN %s\n", __func__);
	send_key_event(code, press);
	/*MsgKey_st key;
	env = env;
	obj = obj;
	LOGD("IN %s, keycode = %d, type = %d\n", __func__, code, press);
	key.type = press? RMT_INPUT_DOWN: RMT_INPUT_UP;
	key.code = code;
	key_input(&key);
	*/
	return 0;
}

void touch_handle(int x1, int y1, int x2, int y2, int action);
jint JNIDEFINE(setTouchEvent)(JNIEnv *env, jobject obj, int x1, int y1, int x2, int y2, int action)
{
	LOGD("###########################################\n");
	LOGD("real value :(x1, y1) = (%d, %d)\n", x1, y1);
	LOGD("real value :(x2, y2) = (%d, %d)\n", x2, y2);
	x1 *= x_scale;
        y1 *= y_scale;
	x2 *= x_scale;
	y2 *= y_scale;
	LOGD("-------------------------------------------\n");
	LOGD("scale value :(x1, y1) = (%d, %d)\n", x1, y1);
	LOGD("scale value :(x2, y2) = (%d, %d)\n", x2, y2);
	LOGD("###########################################\n");
	LOGD("\n");

	touch_handle(x1, y1, x2, y2, action);
		
	/*MsgMulTouch_st touch;
	env = env;
	obj = obj;
	LOGD("IN %s\n", __func__);
	touch.num = 1;
	touch.pXY[0].type = press? RMT_INPUT_DOWN: RMT_INPUT_UP;
	touch.pXY[0].x = x;
	touch.pXY[0].y = y;
	LOGD("%s touch type = %d\n, x = %d, y = %d\n", __func__, press, x, y);
	return touch_input(&touch);
	*/
	return 0;
}

void tp2all(int x1, int y1, int x2, int y2, int action);
jint JNIDEFINE(mouseEvent)(JNIEnv *env, jobject obj, int x1, int y1, int x2, int y2, int action)
{
	tp2all(x1, y1, x2, y2, action);
	return 0;
}

void notify_jni(const char *msg)
{
	JNIEnv *env;
	(*g_javavm)->AttachCurrentThread(g_javavm, &env, NULL);
	(*env)->CallStaticVoidMethod(env, jni_class, jni_method, (*env)->NewStringUTF(env, msg));
	(*g_javavm)->DetachCurrentThread(g_javavm);
}

