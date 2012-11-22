#include "common.h"
#include "sensor_grab.h"
#include "amt_remote.h"
#include "wi_remote.h"

#undef  LOG_TAG
#define LOG_TAG "SENSOR_MANAGER"
struct amt_handle *handle;
static int gyro_mouse_enable = 0;
void notify_jni(const char *msg);
void notify_msg(const char *log)
{
	LOGD("%s %s", __func__, log);
	if(strncmp(log, "socket error", strlen("socket error")) == 0)
	{
		notify_jni(log);
		//TODO---------------------
	}
}

void log_cb(int tag, const char *log)
{
	if(tag == CB_LOGH)
	{
		notify_msg(log);
	}
	else
		LOGD("%s", log);
}

int sensor_control(int sensor, int on)
{
	if(on)
		sensor_grab_enable(sensor);
	else
		sensor_grab_disable(sensor);
	return RETURN_NORMAL;
}

int sensor_delay(int sensor, int delay)
{
	sensor_grab_set_delay(sensor, delay);
	return RETURN_NORMAL;
}

static void gyro_to_mouse(struct sensor_event *event)
{
	float x, y, z;
	x = event->data[0];
	y = event->data[1];
	z = event->data[2];
	y = -x * 9;
	x = -z * 16;
	send_mouse_move((int)x, (int)y);
}

static void sensor_data_cb(struct sensor_event *event, int count)
{
	int i;
	struct amt_sensor_data data[20];
	for(i = 0; i < count; i++)
	{
		if(gyro_mouse_enable && (event[i].type == SENSOR_TYPE_GYROSCOPE))
			gyro_to_mouse(&event[i]);
		data[i].sensor_type = event[i].type;
		data[i].data[0] = event[i].data[0];
		data[i].data[1] = event[i].data[1];
		data[i].data[2] = event[i].data[2];
	}
	sensor_client_send_data(handle, count, data);
//	while(count--)
//		LOGD("%s type = %d, x = %f, y = %f, z = %f\n", __func__, event[count].type, event[count].data[0], event[count].data[1], event[count].data[2]);
}

static int init_sensor()
{
	struct sensor_grab_callback sensor;
	sensor.sensor_data_cb = sensor_data_cb;
	sensor_grab_init(&sensor);
	return 0;
}

static int init_amt_remote(void)
{
	struct amt_client_callback cb;
	memset(&cb, 0, sizeof(struct amt_client_callback));
	cb.log_cb = log_cb;
	cb.sensor_control = sensor_control;
	cb.sensor_delay = sensor_delay;
	handle = init_client_sock(&cb);
	control_client_log(handle, CB_LOGA);
	return 0;
}

int connect_server(char *ip)
{
	return connect_client2server(handle, ip, SERVER_PORT);
}

int init_wiremote(void)
{
	init_sensor();
	init_amt_remote();
	return 0;
}

void deinit_wiremote(void)
{
	sensor_grab_deinit();
	deinit_client_sock(handle);
}

int send_multi_touch_event(int *x, int *y, int *press)
{
	LOGD("touch press1 = %d, press2 = %d\n", press[0], press[1]);
	if(press[0] || press[1])
	{
		LOGD("%s start at %d line\n", __func__, __LINE__);
		touch_client_send_data_udp(handle, 2, x, y, press);
	}
	else{
		LOGD("%s start at %d line\n", __func__, __LINE__);
		touch_client_send_data(handle, 2, x, y, press);
	}
	return 0;
}

int send_touch_event(int x, int y, int press)
{
	LOGD("touch press = %d\n", press & 0x1);
	if(press & 0x1)
		touch_client_send_data_udp(handle, 1, &x, &y, &press);
	else
		touch_client_send_data(handle, 1, &x, &y, &press);
	return 0;
}

int send_mouse_move(int x, int y)
{
	mouse_client_send_data_udp(handle, x, y, 0, 0);
	return 0;
}

int send_mouse_button(int button, int press)
{
	mouse_client_send_data(handle, 0, 0, button, press);
	return 0;
}

int send_key_event(int code, int press)
{
	key_client_send_data(handle, code, press);
	return 0;
}

int gyro_mouse_cotrol(int enable)
{
	if(enable)
	{
		gyro_mouse_enable = 1;
		sensor_control(SENSOR_TYPE_GYROSCOPE, 1);
		sensor_delay(SENSOR_TYPE_GYROSCOPE, 10);
	}
	else{
		gyro_mouse_enable = 0;
		sensor_control(SENSOR_TYPE_GYROSCOPE, 0);
	}
	return 0;
}
