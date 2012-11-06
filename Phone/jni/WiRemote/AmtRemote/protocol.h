#ifndef _PROTOCOL_H
#define _PROTOCOL_H

#include "amt_remote.h"
#include "log.h"

#define PROTOCOL_CONTROL	1
#define PROTOCOL_TOUCH		2
#define PROTOCOL_KEY		3
#define PROTOCOL_MOUSE		4
#define PROTOCOL_LOCATION	5
#define PROTOCOL_TEST		6
#define PROTOCOL_SENSOR		7

#define MAX_MULTI_TOUCH		8
#define MAX_SENSOR_TYPE		13

#define CONTROL_CMD_UDP_PORT	1
#define CONTROL_CMD_SENSOR		2
#define CONTROL_CMD_LOCAION		3
#define CONTROL_CMD_KEY			4
#define CONTROL_CMD_MOUSE		5
#define CONTROL_CMD_TOUCH		6

#define DIRECT_REQUEST	1
#define DIRECT_RESPONSE	2

#pragma pack(1)

struct control_data
{
	short cmd;
	short direct;
	union
	{
		char b8[32];
		short b16[16];
		int b32[8];
	} argv;
	int ret;
};

struct sensor_data
{
	short sensor_type;
	float data[3];
};

struct mouse_data
{
	short button;
	short press;
	int x;
	int y;
};

struct touch_data
{
	int num;
	int x[MAX_MULTI_TOUCH];
	int y[MAX_MULTI_TOUCH];
	int press[MAX_MULTI_TOUCH];
};

struct key_data
{
	int keycode;
	int keypress;
};

struct protocol_event
{
	short type;
	union
	{
		struct control_data control;
		struct sensor_data sensor[MAX_SENSOR_TYPE];
		struct mouse_data mouse;
		struct touch_data touch;
		struct key_data key;
		char test[32];
	} packet;
};
#pragma pack()

struct protocol_handle
{
	void *data;
	struct amt_log_handle *log;
	int (*cmd_response)(void *arg, unsigned short cmd, int retval);
	int (*update_udp_port)(void *arg, unsigned short port);
	int (*update_test)(void *arg, char *test);
	int (*cmd_ack)(void *arg, struct protocol_event *event);

	int (*sensor_control)(void *arg, int sensor, int on);
	int (*sensor_delay)(void *arg, int sensor, int delay);
	int (*sensor_data)(void *arg, int num, struct amt_sensor_data *data);

	int (*mouse_data)(void *arg, int x, int y, int button, int press);
	int (*touch_data)(void *arg, int num, int *x, int *y, int *press);
	int (*key_data)(void *arg, int code, int press);
};

int recv_packet(struct protocol_handle *handle, struct protocol_event *event);
void cmd_set_udp_port(struct protocol_handle *handle, struct protocol_event *event, unsigned short port);
void cmd_set_sensor_control(struct protocol_handle *handle, struct protocol_event *event, int sensor, int on);
void cmd_set_sensor_delay(struct protocol_handle *handle, struct protocol_event *event, int sensor, int delay);
void data_set_test(struct protocol_handle *handle, struct protocol_event *event, char *test);
void data_set_sensor_data(struct protocol_handle *handle, struct protocol_event *event, int num, struct amt_sensor_data *data);
void data_set_mouse_data(struct protocol_handle *handle, struct protocol_event *event, int x, int y, int button, int press);
void data_set_touch_data(struct protocol_handle *handle, struct protocol_event *event, int num, int *x, int *y, int *press);
void data_set_key_data(struct protocol_handle *handle, struct protocol_event *event, int code, int press);
#endif

