#ifndef _HAIRTUNES_H_
#define _HAIRTUNES_H_
int __shairport_hairtunes_init(char *pAeskey, char *pAesiv, char *pFmtpstr, int pCtrlPort, int pTimingPort,
         int pDataPort, char *pRtpHost, char*pPipeName, char *pLibaoDriver, char *pLibaoDeviceName, char *pLibaoDeviceId);
void __shairport_hairtunes_setvolume(float vol);

void __shairport_hairtunes_set_metadata(const char *buffer, unsigned int size);
void __shairport_hairtunes_set_metadata_coverart(const char *buffer, unsigned int size);
void __shairport_hairtunes_set_progress(const char *buffer, unsigned int size);
void __shairport_hairtunes_set_play_status(int state);
void __shairport_hairtunes_set_volume(float vol);
void __shairport_hairtunes_set_device_tab(const char *deviceId, int size);

void __shairport_hairtunes_flush(void);
void __shairport_hairtunes_cleanup(void);

// default buffer size
#define BUFFER_FRAMES  320

#endif 
