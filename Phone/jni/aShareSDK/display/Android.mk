LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

USE_SURFACE_WRAPPER := true

# for surface API
ifeq ($(USE_SURFACE_WRAPPER),true)
LOCAL_SRC_FILES += surface.cpp

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/skia/include/core \
	$(LOCAL_PATH)/skia/include/images

LOCAL_LDLIBS += $(LOCAL_PATH)/skia/libs/libskia.so \
				-landroid -llog
endif

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/../platform_jpeg/libjpeg_turbo \
	$(LOCAL_PATH)/../platform_jpeg/libjpeg_turbo/inc \
	$(LOCAL_PATH)/../platform_jpeg/libjpeg_turbo/inc/android
LOCAL_CFLAGS += -DSKIA_DECODE

LOCAL_SHARED_LIBRARIES += libjpeg-turbo

# Optional tag would mean it doesn't get installed by default
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false
LOCAL_CPPFLAGS := -DNDK -DSK_BUILD_FOR_ANDROID_NDK -fPIC
LOCAL_MODULE:= libishare-sys4.0

include $(BUILD_SHARED_LIBRARY)
