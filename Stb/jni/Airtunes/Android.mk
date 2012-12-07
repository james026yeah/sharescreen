## libairunes
include $(call all-subdir-makefiles)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := AirTunesServer.cpp \
		   airtuneslib/DllLibShairport.cpp \
		   airtuneslib/EndianSwap.cpp \
		   Airtunes_jni.cpp\
		  airtuneslib/libshairport-1.2.0.20310/src/socketlib.c \
		  airtuneslib/libshairport-1.2.0.20310/src/alac.c \
		  airtuneslib/libshairport-1.2.0.20310/src/shairport.c \
		  airtuneslib/openssl/crypto/bio/bio_lib.c \
		  airtuneslib/libshairport-1.2.0.20310/src/hairtunes.c

LOCAL_CFLAGS += -Wall -O2 -D__ANDROID__ -DNDK
LOCAL_LDLIBS += -llog 
#LOCAL_STATIC_LIBRARIES := libshairport_static
#LOCAL_SHARED_LIBRARIES := libshairport 
LOCAL_STATIC_LIBRARIES += libgcc
#LOCAL_STATIC_LIBRARIES+= libstlport_shared.so

LOCAL_C_INCLUDES += $(LOCAL_PATH) \
		    $(LOCAL_PATH)/airtuneslib/libshairport-1.2.0.20310/src \
		    $(LOCAL_PATH)/airtuneslib/openssl/include \
		    $(LOCAL_PATH)/airtuneslib \
		    $(LOCAL_PATH)/airtuneslib/libshairport-1.2.0.20310/src \
		    $(LOCAL_PATH)/airtuneslib/openssl/include \
		    $(LOCAL_PATH)/airtuneslib/openssl/crypto \
		    $(LOCAL_PATH)/airtuneslib/openssl \
            $(LOCAL_PATH)/airtuneslib

#include $(BUILD_SHARED_LIBRARY)

##############################
## libshairport
#include $(CLEAR_VARS)

#LOCAL_SRC_FILES :=  \
#LOCAL_SRC_FILES +=  \


##LOCAL_CFLAGS += -Wall -O2 -D__ANDROID__ -DNDK
##LOCAL_LDLIBS += -llog
#LOCAL_SHARED_LIBRARIES := libcrypto \
#			  libssl 

#LOCAL_STATIC_LIBRARIES :
LOCAL_STATIC_LIBRARIES +=libcrypto_static\
                         libssl_static
#LOCAL_C_INCLUDES += $(LOCAL_PATH) \


#LOCAL_MODULE := libshairport_static
#include $(BUILD_STATIC_LIBRARY)
LOCAL_MODULE := libairtunes
include $(BUILD_SHARED_LIBRARY)
######################################
#####openssl###
include $(LOCAL_PATH)/airtuneslib/openssl/Android.mk
