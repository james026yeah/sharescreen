# Copyright 2012 Archermind
#

LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVE := $(LOCAL_PATH)
HW_ENCODE_PATH := $(LOCAL_PATH)/hw_encode/Android.mk
include $(HW_ENCODE_PATH)
# ---------------------------------------------------------------------------------
#  #           Libjpeg-turbo android 1.1.9 sw encode
# ---------------------------------------------------------------------------------
#
LOCAL_PATH := $(LOCAL_PATH_SAVE)
include $(CLEAR_VARS)

LIB_PATH := sw_encode
LOCAL_SRC_FILES := \
	$(LIB_PATH)/jpeg_enc.c \
	$(LIB_PATH)/sw_encode.c \
	$(LIB_PATH)/sw_decode.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/sw_encode/inc \
	$(LOCAL_PATH)/sw_encode/inc/android \
	$(LOCAL_PATH)/service \
	$(LOCAL_PATH)/utils

LOCAL_CFLAGS := -DBMP_SUPPORTED -DGIF_SUPPORTED -DPPM_SUPPORTED -DTARGA_SUPPORTED \
	-DANDROID -DANDROID_TILE_BASED_DECODE -DENABLE_ANDROID_NULL_CONVERT \
	-DANDROID_RGB -DTWO_FILE_COMMANDLINE -DNDK \
	-D_ANDROID_ -D_DEBUG -fno-short-enums -O2 -Wall

LOCAL_LDLIBS := -llog \
	$(LOCAL_PATH)/sw_encode/libs/libjpeg.a \
	$(LOCAL_PATH)/sw_encode/libs/libsimd.a

LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= libjpeg-turbo
include $(BUILD_SHARED_LIBRARY)

# ---------------------------------------------------------------------------------
#  #           Ashare SDK library v0.1
# ---------------------------------------------------------------------------------
#
LOCAL_PATH:= $(LOCAL_PATH_SAVE)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	protocol/communicator.c \
	protocol/protocol.c \
	protocol/protocol_client.c \
	protocol/protocol_server.c \
	service/buffer_client.c \
	service/client.c \
	service/display.c \
	service/hal.c \
	service/native_surface.c \
	service/server.c \
	service/service.c \
	utils/listop.c \
	utils/utils.c \
	sw_encode/jpeg_enc.c \
	sw_encode/sw_encode.c \
	aShareStick_jni.c 
	#display/surface.cpp

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/display/skia/include \
	$(LOCAL_PATH)/sw_encode/inc \
	$(LOCAL_PATH)/sw_encode \
	$(LOCAL_PATH)/utils \
	$(LOCAL_PATH)/service \
	$(LOCAL_PATH)/protocol \
	$(LOCAL_PATH)/display \
	$(LOCAL_PATH)/display/skia/include \
	$(LOCAL_PATH)/display/skia/include/core \
	$(LOCAL_PATH)/display/skia/include/images

LOCAL_CFLAGS += -O2 -Wall -DNDK -DSW_ENCODE -DANDROID \
	-DSKIA_DECODE -DPHONE_NOT_SKIA
LOCAL_CPPFLAGS := -DNDK -DSK_BUILD_FOR_ANDROID_NDK  -fPIC

LOCAL_SHARED_LIBRARIES := libjpeg-turbo
LOCAL_LDLIBS += -llog -ldl -landroid \
	#$(LOCAL_PATH)/display/skia/libs/libskia.so

LOCAL_MODULE:= libashare_sdk
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)
