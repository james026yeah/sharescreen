#ifndef __AIRPLAYCALLBACK_H__
#define __AIRPLAYCALLBACK_H__

#ifdef __cplusplus
extern "C" {
#endif
struct airplay_callback {
	void (*setRate)(int rate);//0:pause;1:playing
	bool (*IsPlayCompletion)(void);

	void (*setVolume)(int volume);
	int (*getVolume)(void);

	void (*playVideo)(const char *url, int length, float startPositon);

	float (*getTotalTime)(void);
	float (*getCurrentPosition)(void);
	void (*seekPosition)(float position);

	void (*stopVideo)(void);
	void (*closeWindow)(void);

	void (*showPhoto)(const char *data, int length);

	bool (*IsCaching)(void);
	bool (*IsPlaying)(void);
	bool (*IsPaused)(void);

	float (*getCachPosition)(void);
	char* (*getDeviceId)(void);//get MAC address
};

int init_airplay_callback(struct airplay_callback *airplay_cb,
			struct airplay_callback *cb);

#ifdef __cplusplus
}
#endif
#endif
