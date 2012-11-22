#include <string.h>
#include "AirPlayCallBack.h"

#ifdef __ANDROID__
#undef LOG_TAG
#define LOG_TAG "AIRPLAY_CALLBACK"

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

int init_airplay_callback(struct airplay_callback *airplay_cb,
			struct airplay_callback *cb)
{
LOGD("%s:%d\n", __func__, __LINE__);
	if (cb->setRate != NULL &&
	    cb->IsPlayCompletion != NULL &&
	    cb->setVolume != NULL &&
	    cb->getVolume != NULL &&
	    cb->playVideo != NULL &&
	    cb->getTotalTime != NULL &&
	    cb->getCurrentPosition != NULL &&
	    cb->seekPosition != NULL &&
	    cb->stopVideo != NULL &&
	    cb->closeWindow != NULL &&
	    cb->showPhoto != NULL &&
	    cb->IsCaching != NULL &&
	    cb->IsPlaying != NULL &&
	    cb->IsPaused != NULL &&
	    cb->getCachPosition != NULL &&
	    cb->getDeviceId != NULL) {
		memcpy(airplay_cb, cb, sizeof(airplay_callback));
		return 0;
	}
LOGD("%s:%d\n", __func__, __LINE__);
	return -1;

}
