# Copyright 2012 Archermind
#
LOCAL_PATH_TOP:= $(call my-dir)
SAMPLE_TYPE := so
include $(call all-subdir-makefiles)

LOCAL_PATH:= $(LOCAL_PATH_TOP)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	jni_register.c \
	sensor_grab.c \
	wi_remote.c \
	tp2all.c \
	touch_handle.c

LOCAL_CFLAGS += -O2 -Wall -DNDK
LOCAL_C_INCLUDES += $(LOCAL_PATH)/AmtRemote
LOCAL_SHARED_LIBRARIES := libamt_remote
LOCAL_LDLIBS += -llog -ldl -landroid
LOCAL_MODULE:= libwi_remote
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)
