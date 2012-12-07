#ifdef __ANDROID__
#undef LOG_TAG
#define LOG_TAG "AIRTUNES_SERVER"

#ifdef NDK
#include <android/log.h>
#else
#include <utils/Log.h>
#endif

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG , __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  ,  LOG_TAG , __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  ,  LOG_TAG , __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG , __VA_ARGS__)

#else
#define LOGV(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGD(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGI(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGW(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGE(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#define LOGE(x, ...) do { printf(x"\n",##__VA_ARGS__);} while(0)
#endif
