# Copyright 2012 Archermind
#
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES += \
	log.c \
	communicator.c \
	server.c \
	client.c \
	protocol.c

LOCAL_CFLAGS += -O2 -Wall -DNDK -fPIC
#LOCAL_LDLIBS += -llog -ldl
LOCAL_MODULE:= libamt_remote
LOCAL_MODULE_TAGS := optional
include $(BUILD_STATIC_LIBRARY)
