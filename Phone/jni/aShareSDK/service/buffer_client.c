#include <string.h>
#include <pthread.h>
#include "list.h"
#include "buffer_client.h"
#include "native_surface.h"
#include "common.h"

struct buffer_common {
	struct list_head list;
	void *data;
	int size;
};

static struct list_head buffer_head[2];
static pthread_mutex_t buffer_mutex[2];
static pthread_cond_t buffer_cond[2];
static struct buffer_common *last_buffer[2];
static int pending_num[2];

#ifndef WIN32

void init_surface(JNIEnv *env, jobject jsurface)
{
	LOGD("%s call surface_start\n", __func__);
	surface_start(env, jsurface);
}

void deinit_surface(void)
{
	surface_stop();
}
#endif
void init_buffer_client(void)
{
	int i;
	for(i = 0; i < 2; i++)
	{
		INIT_LIST_HEAD(&buffer_head[i]);
		pthread_mutex_init(&buffer_mutex[i], NULL);
		pthread_cond_init(&buffer_cond[i], NULL);
		last_buffer[i] = NULL;
		pending_num[i] = 0;
	}
}

#define MAX_BUFFER_NUM	2
static uint64_t timestamp()
{
	struct timeval time;
	uint64_t timestamp;

	gettimeofday(&time, NULL);

	/*  timestamp is micro secend */
	timestamp = (uint64_t)time.tv_sec * 1000 + time.tv_usec / 1000;
	return timestamp;
}

static uint64_t last_time;

static int set_common_buffer(int type, const void *data, int size)
{
	struct buffer_common *buf;
	buf = malloc(sizeof(struct buffer_common));
	if(!buf)
		return -1;
	buf->data = malloc(size);
	if(!buf->data)
	{
		free(buf);
		return -1;
	}
	memcpy(buf->data, data, size);
	buf->size = size;
	pthread_mutex_lock(&buffer_mutex[type]);
	list_add_tail(&buf->list, &buffer_head[type]);
	pending_num[type]++;
	if(pending_num[type] > MAX_BUFFER_NUM)
	{
		struct buffer_common *buf;
		buf = list_entry(buffer_head[type].next, typeof(*buf), list);
		list_del(&buf->list);
		pending_num[type]--;
		free(buf->data);
		free(buf);
	}
	else
		last_time = timestamp();
	pthread_cond_signal(&buffer_cond[type]);
	pthread_mutex_unlock(&buffer_mutex[type]);
	return 1;
}

static struct buffer_common *get_common_buffer(int type)
{
	struct buffer_common *buf;
	pthread_mutex_lock(&buffer_mutex[type]);
	if(list_empty(&buffer_head[type]))
		pthread_cond_wait(&buffer_cond[type], &buffer_mutex[type]);
	if(list_empty(&buffer_head[type]))
		buf = NULL;
	else
	{
		buf = list_entry(buffer_head[type].next, typeof(*buf), list);
		list_del(&buf->list);
		last_buffer[type] = buf;
	}
	pthread_mutex_unlock(&buffer_mutex[type]);
//	LOGD("%s time = %d\n", __func__, (int)(timestamp() - last_time));
	return buf;
}

static void free_common_buffer(int type)
{
	free(last_buffer[type]->data);
	free(last_buffer[type]);
	last_buffer[type] = NULL;
	pending_num[type]--;
}

void signal_client_buffer(void)
{
	pthread_mutex_lock(&buffer_mutex[AUDIO_BUFFER]);
	pthread_cond_signal(&buffer_cond[AUDIO_BUFFER]);
	pthread_mutex_unlock(&buffer_mutex[AUDIO_BUFFER]);
	pthread_mutex_lock(&buffer_mutex[IMAGE_BUFFER]);
	pthread_cond_signal(&buffer_cond[IMAGE_BUFFER]);
	pthread_mutex_unlock(&buffer_mutex[IMAGE_BUFFER]);
}

void set_audio_buffer(const void *data, int size)
{
	set_common_buffer(AUDIO_BUFFER, data, size);
}

int get_audio_buffer(void **data)
{
	struct buffer_common *buf = get_common_buffer(AUDIO_BUFFER);
	if(!buf)
		return -1;
	*data = buf->data;
	return buf->size;
}

int release_audio_buffer(void)
{
	free_common_buffer(AUDIO_BUFFER);
	return 1;
}

void set_image_buffer(const void *data, int size)
{
	set_common_buffer(IMAGE_BUFFER, data, size);
}

int get_image_buffer(void **data)
{
	struct buffer_common *buf = get_common_buffer(IMAGE_BUFFER);
	if(!buf)
		return -1;
	*data = buf->data;
	return buf->size;
}

int release_image_buffer(void)
{
	free_common_buffer(IMAGE_BUFFER);
	return 1;
}

