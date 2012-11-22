LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
$(warning $(LOCAL_PATH))

common_libmmjpeg_cflags := -fno-short-enums -D_ANDROID_ -D_DEBUG
common_inculde_path := \
	$(LOCAL_PATH)/qcom_hw_jpeg/inc \
	$(LOCAL_PATH)/qcom_hw_jpeg/src \
	$(LOCAL_PATH)/qcom_hw_jpeg/src/os \
	$(LOCAL_PATH)/../ \
	$(LOCAL_PATH)/../jpeg \
	$(LOCAL_PATH)/../jpeg/android \
	$(LOCAL_PATH)/../sw_encode \
	$(LOCAL_PATH)/../service \
	$(LOCAL_PATH)/../utils \
	$(LOCAL_PATH)/../common \
	$(LOCAL_PATH)/../display

LOCAL_LDLIBS    := $(LOCAL_PATH)/qcom_hw_jpeg/libs/libmmjpeg.so
LOCAL_SRC_FILES := \
	qcom_hw_jpeg/hw_encode/hw_encode.c            \
	qcom_hw_jpeg/hw_encode/rgb2yuv.c

LOCAL_CFLAGS := $(common_libmmjpeg_cflags) -O2 -Wall -DNDK -fPIC
LOCAL_C_INCLUDES += $(common_inculde_path)
LOCAL_LDLIBS += -llog
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= libqcom_jpeg
include $(BUILD_SHARED_LIBRARY)

# ---------------------------------------------------------------------------------
#  #           Qualcomm jpeg2 codec support
# ---------------------------------------------------------------------------------
#
include $(CLEAR_VARS)
mm-jpeg-inc2 += $(LOCAL_PATH)/qcom_hw_jpeg2/inc
mm-jpeg-inc2 += $(LOCAL_PATH)/qcom_hw_jpeg2/src
mm-jpeg-inc2 += $(LOCAL_PATH)/qcom_hw_jpeg2/src/os

common_libmmjpeg_cflags := -fno-short-enums
common_libmmjpeg_cflags += -D_ANDROID_
common_libmmjpeg_cflags += -D_DEBUG

LOCAL_LDLIBS    := $(LOCAL_PATH)/qcom_hw_jpeg2/libs/libmmjpeg.so
LOCAL_SRC_FILES := qcom_hw_jpeg2/hw_encode/hw_encode.c            \
		   qcom_hw_jpeg2/hw_encode/rgb2yuv.c

LOCAL_CFLAGS        := $(common_libmmjpeg_cflags) -O2 -Wall -DNDK -fPIC
LOCAL_C_INCLUDES    := $(mm-jpeg-inc2) $(LOCAL_PATH)/../ \
						$(LOCAL_PATH)/../jpeg \
						$(LOCAL_PATH)/../jpeg/android
LOCAL_C_INCLUDES += $(common_inculde_path)
LOCAL_LDLIBS += -llog
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= libqcom_jpeg2
include $(BUILD_SHARED_LIBRARY)

# ---------------------------------------------------------------------------------
#  #           Other platform codec support(e.g Samsung)
# ---------------------------------------------------------------------------------
#
include $(CLEAR_VARS)
s3c-jpeg-inc += $(LOCAL_PATH)/samsung_hw_jpeg/inc

LOCAL_LDLIBS    := $(LOCAL_PATH)/samsung_hw_jpeg/libs/libs3cjpeg.so
LOCAL_C_INCLUDES:= $(s3c-jpeg-inc) $(LOCAL_PATH)/../
LOCAL_C_INCLUDES += $(common_inculde_path)
LOCAL_SRC_FILES	:= samsung_hw_jpeg/hw_encode/hw_jpeg.cpp	\
		   ../utils/utils.c

LOCAL_CFLAGS	+= -O2 -Wall -DNDK -fPIC
LOCAL_LDLIBS += -llog
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE	:= libsamsung_jpeg
include $(BUILD_SHARED_LIBRARY)
