#######
LOCAL_PATH:= $(call my-dir)

#include $(call all-subdir-makefiles)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := AirPlayServer.cpp \
			HttpParser.cpp \
			md5.cpp \
			dnssd.c \
			utils.c \
			jni_register.cpp \
			AirPlayCallBack.cpp
LOCAL_CFLAGS += -Wall -O2 -D__ANDROID__ -DNDK
LOCAL_LDLIBS += -llog
#LOCAL_SHARED_LIBRARIES :=
LOCAL_STATIC_LIBRARIES := libmdnssd libplist
LOCAL_C_INCLUDES += $(LOCAL_PATH) \
		    $(LOCAL_PATH)/libplist-1.8/include \
		    $(LOCAL_PATH)/mdnsresponder/mDNSShared

LOCAL_MODULE    := libairplay
include $(BUILD_SHARED_LIBRARY)


##########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  mdnsresponder/mDNSShared/dnssd_clientlib.c  \
                    mdnsresponder/mDNSShared/dnssd_clientstub.c \
                    mdnsresponder/mDNSShared/dnssd_ipc.c

LOCAL_MODULE := libmdnssd
#LOCAL_MODULE_TAGS := optional

#LOCAL_CFLAGS := -O2 -g -W -Wall -D__ANDROID__ -D_GNU_SOURCE -DHAVE_IPV6 -DNOT_HAVE_SA_LEN -DUSES_NETLINK -DTARGET_OS_LINUX -fno-strict-aliasing -DHAVE_LINUX -DMDNS_UDS_SERVERPATH=\"/dev/socket/mdnsd\" -DMDNS_DEBUGMSGS=0
LOCAL_CFLAGS := -O2 -g -W -D__ANDROID__ -D_GNU_SOURCE -DHAVE_IPV6 -DNOT_HAVE_SA_LEN -DUSES_NETLINK -DTARGET_OS_LINUX -fno-strict-aliasing -DHAVE_LINUX -DMDNS_UDS_SERVERPATH=\"/dev/socket/mdnsd\" -DMDNS_DEBUGMSGS=0
#LOCAL_SYSTEM_SHARED_LIBRARIES := libc libcutils

include $(BUILD_STATIC_LIBRARY)

##########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES := libplist-1.8/src/base64.c \
			libplist-1.8/src/bplist.c \
			libplist-1.8/src/bytearray.c \
			libplist-1.8/src/hashtable.c \
			libplist-1.8/src/plist.c \
			libplist-1.8/src/ptrarray.c \
			libplist-1.8/src/xplist.c

LOCAL_MODULE := libplist
LOCAL_STATIC_LIBRARIES := libcnary libxml2
#LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libplist-1.8/libcnary/include \
		    $(LOCAL_PATH)/libplist-1.8/include \
		    $(LOCAL_PATH)/libxml2/include \
		    $(LOCAL_PATH)/icu4c/common

LOCAL_CFLAGS := -O2 -W
#LOCAL_SYSTEM_SHARED_LIBRARIES := libc libcutils

include $(BUILD_STATIC_LIBRARY)

##########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES := libplist-1.8/libcnary/cnary.c \
			libplist-1.8/libcnary/iterator.c \
			libplist-1.8/libcnary/list.c \
			libplist-1.8/libcnary/node.c \
			libplist-1.8/libcnary/node_iterator.c \
			libplist-1.8/libcnary/node_list.c

LOCAL_MODULE := libcnary
#LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libplist-1.8/libcnary/include

LOCAL_CFLAGS := -O2 -W
#LOCAL_SYSTEM_SHARED_LIBRARIES := libc libcutils

include $(BUILD_STATIC_LIBRARY)

##########################

include $(CLEAR_VARS)
common_SRC_FILES := \
	libxml2/SAX.c \
	libxml2/entities.c \
	libxml2/encoding.c \
	libxml2/error.c \
	libxml2/parserInternals.c \
	libxml2/parser.c \
	libxml2/tree.c \
	libxml2/hash.c \
	libxml2/list.c \
	libxml2/xmlIO.c \
	libxml2/xmlmemory.c \
	libxml2/uri.c \
	libxml2/valid.c \
	libxml2/xlink.c \
	libxml2/HTMLparser.c \
	libxml2/HTMLtree.c \
	libxml2/debugXML.c \
	libxml2/xpath.c \
	libxml2/xpointer.c \
	libxml2/xinclude.c \
	libxml2/nanohttp.c \
	libxml2/nanoftp.c \
	libxml2/DOCBparser.c \
	libxml2/catalog.c \
	libxml2/globals.c \
	libxml2/threads.c \
	libxml2/c14n.c \
	libxml2/xmlstring.c \
	libxml2/xmlregexp.c \
	libxml2/xmlschemas.c \
	libxml2/xmlschemastypes.c \
	libxml2/xmlunicode.c \
	libxml2/xmlreader.c \
	libxml2/relaxng.c \
	libxml2/dict.c \
	libxml2/SAX2.c \
	libxml2/legacy.c \
	libxml2/chvalid.c \
	libxml2/pattern.c \
	libxml2/xmlsave.c \
	libxml2/xmlmodule.c \
	libxml2/xmlwriter.c \
	libxml2/schematron.c

common_C_INCLUDES += \
	$(LOCAL_PATH)/libxml2/include

# For the device
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
#LOCAL_C_INCLUDES += $(common_C_INCLUDES) external/icu4c/common
LOCAL_C_INCLUDES += $(common_C_INCLUDES) $(LOCAL_PATH)/icu4c/common
LOCAL_STATIC_LIBRARIES += libicuuc
LOCAL_CFLAGS += -fvisibility=hidden

LOCAL_MODULE:= libxml2

include $(BUILD_STATIC_LIBRARY)


#################################

#
# Common definitions.
#

src_files := \
	icu4c/common/cmemory.c \
	icu4c/common/cstring.c \
	icu4c/common/cwchar.c \
	icu4c/common/icudataver.c \
	icu4c/common/icuplug.c \
	icu4c/common/locmap.c \
	icu4c/common/propsvec.c \
	icu4c/common/punycode.c \
	icu4c/common/putil.c \
	icu4c/common/uarrsort.c \
	icu4c/common/ubidi.c \
	icu4c/common/ubidiln.c \
	icu4c/common/ubidi_props.c \
	icu4c/common/ubidiwrt.c \
	icu4c/common/ucase.c \
	icu4c/common/ucasemap.c \
	icu4c/common/ucat.c \
	icu4c/common/uchar.c \
	icu4c/common/ucln_cmn.c \
	icu4c/common/ucmndata.c \
	icu4c/common/ucnv2022.c \
	icu4c/common/ucnv_bld.c \
	icu4c/common/ucnvbocu.c \
	icu4c/common/ucnv.c \
	icu4c/common/ucnv_cb.c \
	icu4c/common/ucnv_cnv.c \
	icu4c/common/ucnvdisp.c \
	icu4c/common/ucnv_err.c \
	icu4c/common/ucnv_ext.c \
	icu4c/common/ucnvhz.c \
	icu4c/common/ucnv_io.c \
	icu4c/common/ucnvisci.c \
	icu4c/common/ucnvlat1.c \
	icu4c/common/ucnv_lmb.c \
	icu4c/common/ucnvmbcs.c \
	icu4c/common/ucnvscsu.c \
	icu4c/common/ucnv_set.c \
	icu4c/common/ucnv_u16.c \
	icu4c/common/ucnv_u32.c \
	icu4c/common/ucnv_u7.c \
	icu4c/common/ucnv_u8.c \
	icu4c/common/udatamem.c \
	icu4c/common/udataswp.c \
	icu4c/common/uenum.c \
	icu4c/common/uhash.c \
	icu4c/common/uinit.c \
	icu4c/common/uinvchar.c \
	icu4c/common/ulist.c \
	icu4c/common/uloc.c \
	icu4c/common/uloc_tag.c \
	icu4c/common/umapfile.c \
	icu4c/common/umath.c \
	icu4c/common/umutex.c \
	icu4c/common/unames.c \
	icu4c/common/unorm_it.c \
	icu4c/common/uresbund.c \
	icu4c/common/ures_cnv.c \
	icu4c/common/uresdata.c \
	icu4c/common/usc_impl.c \
	icu4c/common/uscript.c \
	icu4c/common/ushape.c \
	icu4c/common/ustrcase.c \
	icu4c/common/ustr_cnv.c \
	icu4c/common/ustrfmt.c \
	icu4c/common/ustring.c \
	icu4c/common/ustrtrns.c \
	icu4c/common/ustr_wcs.c \
	icu4c/common/utf_impl.c \
	icu4c/common/utrace.c  \
	icu4c/common/utrie2_builder.c \
	icu4c/common/utrie.c \
	icu4c/common/utypes.c \
	icu4c/common/wintz.c


src_files += \
	icu4c/common/bmpset.cpp \
	icu4c/common/brkeng.cpp \
	icu4c/common/brkiter.cpp \
	icu4c/common/bytestream.cpp \
	icu4c/common/caniter.cpp \
	icu4c/common/chariter.cpp \
	icu4c/common/charstr.cpp \
	icu4c/common/dictbe.cpp \
	icu4c/common/dtintrv.cpp \
	icu4c/common/errorcode.cpp \
	icu4c/common/filterednormalizer2.cpp \
	icu4c/common/locavailable.cpp \
	icu4c/common/locbased.cpp \
	icu4c/common/locdispnames.cpp \
	icu4c/common/locid.cpp \
	icu4c/common/loclikely.cpp \
	icu4c/common/locresdata.cpp \
	icu4c/common/locutil.cpp \
	icu4c/common/mutex.cpp \
	icu4c/common/normalizer2.cpp \
	icu4c/common/normalizer2impl.cpp \
	icu4c/common/normlzr.cpp \
	icu4c/common/parsepos.cpp \
	icu4c/common/propname.cpp \
	icu4c/common/rbbi.cpp \
	icu4c/common/rbbidata.cpp \
	icu4c/common/rbbinode.cpp \
	icu4c/common/rbbirb.cpp \
	icu4c/common/rbbiscan.cpp \
	icu4c/common/rbbisetb.cpp \
	icu4c/common/rbbistbl.cpp \
	icu4c/common/rbbitblb.cpp \
	icu4c/common/resbund_cnv.cpp \
	icu4c/common/resbund.cpp \
	icu4c/common/ruleiter.cpp \
	icu4c/common/schriter.cpp \
	icu4c/common/serv.cpp \
	icu4c/common/servlk.cpp \
	icu4c/common/servlkf.cpp \
	icu4c/common/servls.cpp \
	icu4c/common/servnotf.cpp \
	icu4c/common/servrbf.cpp \
	icu4c/common/servslkf.cpp \
	icu4c/common/stringpiece.cpp \
	icu4c/common/triedict.cpp \
	icu4c/common/ubrk.cpp \
	icu4c/common/uchriter.cpp \
	icu4c/common/ucnvsel.cpp \
	icu4c/common/ucol_swp.cpp \
	icu4c/common/udata.cpp \
	icu4c/common/uhash_us.cpp \
	icu4c/common/uidna.cpp \
	icu4c/common/uiter.cpp \
	icu4c/common/unifilt.cpp \
	icu4c/common/unifunct.cpp \
	icu4c/common/uniset.cpp \
	icu4c/common/uniset_props.cpp \
	icu4c/common/unisetspan.cpp \
	icu4c/common/unistr_case.cpp \
	icu4c/common/unistr_cnv.cpp \
	icu4c/common/unistr.cpp \
	icu4c/common/unistr_props.cpp \
	icu4c/common/unormcmp.cpp \
	icu4c/common/unorm.cpp \
	icu4c/common/uobject.cpp \
	icu4c/common/uprops.cpp \
	icu4c/common/uset.cpp \
	icu4c/common/usetiter.cpp \
	icu4c/common/uset_props.cpp \
	icu4c/common/usprep.cpp \
	icu4c/common/ustack.cpp \
	icu4c/common/ustrenum.cpp \
	icu4c/common/utext.cpp \
	icu4c/common/util.cpp \
	icu4c/common/util_props.cpp \
	icu4c/common/utrie2.cpp \
	icu4c/common/uts46.cpp \
	icu4c/common/uvector.cpp \
	icu4c/common/uvectr32.cpp \
	icu4c/common/uvectr64.cpp



# This is the empty compiled-in icu data structure
# that we need to satisfy the linker.
src_files += icu4c/stubdata/stubdata.c

c_includes := \
	$(LOCAL_PATH)/icu4c/common \
	$(LOCAL_PATH)/icu4c/i18n

# We make the ICU data directory relative to $ANDROID_ROOT on Android, so both
# device and sim builds can use the same codepath, and it's hard to break one
# without noticing because the other still works.
local_cflags := '-DICU_DATA_DIR_PREFIX_ENV_VAR="ANDROID_ROOT"'
local_cflags += '-DICU_DATA_DIR="/usr/icu"'

local_cflags += -D_REENTRANT -DU_COMMON_IMPLEMENTATION -O3 -fvisibility=hidden -DHAVE_ANDROID_OS
local_ldlibs := -lm


#
# Build for the target (device).
#

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(src_files)
LOCAL_C_INCLUDES := $(c_includes)
#                    abi/cpp/include
LOCAL_CFLAGS := $(local_cflags) -DPIC -fPIC
LOCAL_RTTI_FLAG := -frtti
#LOCAL_SHARED_LIBRARIES += libgabi++
LOCAL_LDLIBS += $(local_ldlibs)
#LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libicuuc
include $(BUILD_STATIC_LIBRARY)

