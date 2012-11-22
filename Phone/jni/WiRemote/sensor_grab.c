#include <unistd.h>
#include <pthread.h>
#include <android/sensor.h>
#include <android/looper.h>
#include <android/log.h>
#include "common.h"
#include "sensor_grab.h"

#undef	LOG_TAG
#define	LOG_TAG "SENSOR_GRAB"

#define	SENSOR_TYPE_MAX		SENSOR_TYPE_AMBIENT_TEMPERATURE

#define	LOOPER_ID_SENSOR	2
#define	LOOPER_TIMEOUT		3

static int g_init_flag = 0;
static int g_sensor_num = 0;
static pthread_t g_tid = 0;
static ASensorManager *g_manager = NULL;
static ASensorList g_list = NULL;
static ASensorEventQueue *g_queue = NULL;
static struct sensor_grab_callback g_cb = {NULL};

static void sensor_handle_event(ASensorEvent *event, int count)
{
	int i;
	struct sensor_event s_event[SENSOR_TYPE_MAX];
	for(i = 0; i < count; i++)
	{
		s_event[i].type = event[i].type;
		s_event[i].data[0] = event[i].data[0];
		s_event[i].data[1] = event[i].data[1];
		s_event[i].data[2] = event[i].data[2];
		LOGV("%s type = %d, x = %f, y = %f, z = %f\n", __func__, event[i].type, event[i].data[0], event[i].data[1], event[i].data[2]);
	}
	if(g_cb.sensor_data_cb)
	{
		g_cb.sensor_data_cb(s_event, count);
	}
}

static void *sensor_poll_thread(void *arg)
{
	ALooper *looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
	g_queue = ASensorManager_createEventQueue(g_manager, looper, LOOPER_ID_SENSOR, NULL, NULL);

	while(g_init_flag)
	{
		int ident;
		int events;
		struct android_poll_source* source;

		while((ident = ALooper_pollAll(LOOPER_TIMEOUT, NULL, &events, (void**)&source)) >= 0)
		{
			if(ident == LOOPER_ID_SENSOR)
			{
				int num_event;
				ASensorEvent event[SENSOR_TYPE_MAX];
				while((num_event = ASensorEventQueue_getEvents(g_queue, event, g_sensor_num)) > 0)
				{
					sensor_handle_event(event, num_event);
				}
			}
		}
	}
	ASensorManager_destroyEventQueue(g_manager, g_queue);
	g_queue = NULL;
	pthread_exit("exit sensor_poll_thread");
	return NULL;
}

int sensor_grab_init(struct sensor_grab_callback *callback)
{
	int i;
	if(g_init_flag > 0)
		return g_sensor_num;
	g_manager = ASensorManager_getInstance();
	g_sensor_num = ASensorManager_getSensorList(g_manager, &g_list);
	g_init_flag = 1;
	pthread_create(&g_tid, NULL, sensor_poll_thread, NULL);
	memset(&g_cb, 0, sizeof(struct sensor_grab_callback));
	if(callback)
	{
		memcpy(&g_cb, callback, sizeof(struct sensor_grab_callback));
	}
	while(!g_queue)
		usleep(20000);
	for(i = 0; i < g_sensor_num; i++)
		LOGD("sensor name = %s, sensor vendor = %s\n", ASensor_getName(g_list[i]), ASensor_getVendor(g_list[i]));
	return g_sensor_num;
}

void sensor_grab_deinit(void)
{
	if(g_init_flag > 0)
		g_init_flag = 0;
	pthread_join(g_tid, NULL);
	g_queue = NULL;
}

static ASensor const *get_sensor(int sensor)
{
	int i;
	if((sensor > SENSOR_TYPE_MAX) || (sensor < 1))
		return NULL;
	if(g_init_flag < 1)
		return NULL;
	for(i = 0; i < g_sensor_num; i++)
	{
		if(ASensor_getType(g_list[i]) == sensor)
			return g_list[i];
	}
	return NULL;
}

int sensor_grab_enable(int sensor)
{
	const ASensor *asensor = get_sensor(sensor);
	if(!asensor)
		return -1;
	return ASensorEventQueue_enableSensor(g_queue, asensor);
}

int sensor_grab_disable(int sensor)
{
	const ASensor *asensor = get_sensor(sensor);
	if(!asensor)
		return -1;
	return ASensorEventQueue_disableSensor(g_queue, asensor);
}

int sensor_grab_set_delay(int sensor, int delay)
{
	const ASensor *asensor = get_sensor(sensor);
	if(!asensor)
		return -1;
	return ASensorEventQueue_setEventRate(g_queue, asensor, delay);;
}

