#ifndef _BUFFER_CLIENT_HEAD
#define _BUFFER_CLIENT_HEAD

#define AUDIO_BUFFER	0
#define IMAGE_BUFFER	1

void init_buffer_client(void);
void signal_client_buffer(void);
void set_audio_buffer(const void *data, int size);
int get_audio_buffer(void **data);
int release_audio_buffer(void);
void set_image_buffer(const void *data, int size);
int get_image_buffer(void **data);
int release_image_buffer(void);

#ifndef WIN32
#include <jni.h>
void init_surface(JNIEnv *env, jobject jsurface);
void deinit_surface(void);
#endif
#endif
