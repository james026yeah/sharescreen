/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  * See the License for the specific language governing permissions and * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include "surface.h"
#include "common.h"
#include "buffer_client.h"
#include <unistd.h>

#define  JNIDEFINE(fname) Java_archermind_ashare_NativeAshare_##fname

extern int start_server(void);
extern int stop_server(void);
extern int start_client_external(void);
extern int stop_client(void);
extern int invite_client(char *ip);
extern int start_client_1(char *ip);
int g_surface_ready;

int g_angle; //ratation, 0, 90, 180, 270
/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/HelloJni/HelloJni.java
 */
static char j2c_string[128];

static char *jstring2cstring(JNIEnv *env, jstring string)
{
	int len = (*env)->GetStringLength(env, string);
	(*env)->GetStringUTFRegion(env, string, 0, len, j2c_string);
	LOGD("%s %s\n", __func__, j2c_string);
	return j2c_string;
}

/* Phone端 */
static JavaVM *g_javavm_phone;
static jobject g_object_cb_phone;
void JNIDEFINE(shareScreen)(JNIEnv *env, jobject obj, jobject object_cb, jstring remoteHostIp)
{
	env = env;
	obj = obj;
	LOGD("IN %s\n", __func__);

	(*env)->GetJavaVM(env, &g_javavm_phone);
	g_object_cb_phone = (*env)->NewGlobalRef(env, object_cb);

	start_server();
	invite_client(jstring2cstring(env, remoteHostIp));
	return;
}

void JNIDEFINE(stopShare)(JNIEnv *env, jobject obj)
{
	env = env;
	obj = obj;
	LOGD("IN %s\n", __func__);

	stop_server();
	return;
}

void JNIDEFINE(setRotate)(JNIEnv *env, jobject obj, jint rotate)
{
	env = env;
	obj = obj;
	LOGD("IN %s\n", __func__);

	g_angle = rotate;
	//In fact, g_angle should be projected and synchronized with LOCK machise
	return;
}

void _callback_notify_status(int connected)
{
	JNIEnv *env;

	(*g_javavm_phone)->AttachCurrentThread(g_javavm_phone, &env, NULL);
	jclass cb_class = (*env)->GetObjectClass(env, g_object_cb_phone);
	jmethodID m_id_callback = (*env)->GetMethodID(env, cb_class, "onConnectionStatusChanged", "(I)V");
	(*env)->CallVoidMethod(env, g_object_cb_phone, m_id_callback, connected);
	(*g_javavm_phone)->DetachCurrentThread(g_javavm_phone);

	return;
}

/* Stb */
static JavaVM *g_javavm_stb;
static jobject g_object_cb_stb;
void JNIDEFINE(initAShareService)(JNIEnv *env, jclass obj, jobject object_cb)
{
	obj = obj; env = env;
	LOGD("IN %s\n", __func__);

	(*env)->GetJavaVM(env, &g_javavm_stb);
	g_object_cb_stb = (*env)->NewGlobalRef(env, object_cb);

	start_client_1("");
	start_client_external();
	g_surface_ready = 0;
	return;
}
void JNIDEFINE(deinitAShareService)(JNIEnv *env, jobject obj)
{
	env = env; obj = obj;
	LOGD("IN %s\n", __func__);
	stop_client();
	return;
}
void JNIDEFINE(startDisplay)(JNIEnv *env, jobject obj, jobject jsurface)
{
	env = env; obj = obj;

	init_surface(env, jsurface);
	g_surface_ready = 1;
	return;
}
void JNIDEFINE(stopDisplay)(JNIEnv *env, jobject obj)
{
	env = env; obj = obj;
	//TODO
	return;
}

void _callback_notify_status_client(int connected)
{
	LOGD("IN %s\n", __func__);
	JNIEnv *env;
	(*g_javavm_stb)->AttachCurrentThread(g_javavm_stb, &env, NULL);
	jclass cb_class = (*env)->GetObjectClass(env, g_object_cb_stb);
	jmethodID m_id_callback = (*env)->GetMethodID(env, cb_class, "onConnectionStatusChanged", "(I)V");
	(*env)->CallVoidMethod(env, g_object_cb_stb, m_id_callback, connected);
	(*g_javavm_stb)->DetachCurrentThread(g_javavm_stb);

	return;
}
