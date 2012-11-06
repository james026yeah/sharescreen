#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "log.h"
#include "communicator.h"
#include "protocol.h"

struct amt_client
{
	struct amt_event_base *event_base;
	SOCKET sock_tcp, sock_udp;
	struct amt_event *event_tcp, *event_udp;
	struct sockaddr sock_udp_addr;
	struct amt_client_callback cb;
	struct amt_log_handle log_handle;
	struct protocol_handle protocol;
	char server_ip[32];
};

static void event_read_cb(void *arg)
{
	int size;
	struct sockaddr addr;
	struct protocol_event packet;
	struct amt_event *event = arg;
	struct amt_client *client = (struct amt_client*)event->base;
	size = amt_event_buffer_read(event, &packet, sizeof(struct protocol_event), &addr);
	if(size <= 0)
	{
		client->event_tcp = NULL;
		amt_event_del_safe(event);
		amt_event_del_safe(client->event_udp);
		client->event_udp = NULL;
		LOGH(&client->log_handle, "socket error\n");
	}
	else
		recv_packet(&client->protocol, &packet);
}

static int __cmd_ack(void *arg, struct protocol_event *event)
{
	struct amt_client *client = arg;
	amt_event_buffer_write_sync(client->event_tcp, event, sizeof(struct protocol_event), NULL);
	return RETURN_NORMAL;
}

static int __update_udp_port(void *arg, unsigned short port)
{
	struct amt_event *event;
	struct amt_client *client = arg;
	client->sock_udp = create_udp_sock();
	amt_set_sockaddr(&client->sock_udp_addr, client->server_ip, port);
	event = amt_event_set(&client->event_base, client->sock_udp, TYPE_UDP);
	amt_event_add(client->event_base, event, event_read_cb, event);
	client->event_udp = event;

	LOGD(&client->log_handle, "%s: %d\n", __func__, port);
	return RETURN_NORMAL;
}

static int __sensor_control(void *arg, int sensor, int on)
{
	int ret = RETURN_ERROR;
	struct amt_client *client = arg;
	LOGD(&client->log_handle, "%s: sensor = %d, on = %d\n", __func__, sensor, on);
	if(client->cb.sensor_control)
		ret = client->cb.sensor_control(sensor, on);
	return ret;
}

static int __sensor_delay(void *arg, int sensor, int delay)
{
	int ret = RETURN_NORMAL;
	struct amt_client *client = arg;
	LOGD(&client->log_handle, "%s: sensor = %d, delay = %d\n", __func__, sensor, delay);
	if(client->cb.sensor_delay)
		ret = client->cb.sensor_delay(sensor, delay);
	return ret;
}

static void init_protocol(struct amt_client *client)
{
	client->protocol.data = client;
	client->protocol.log = &client->log_handle;
	client->protocol.update_udp_port = __update_udp_port;
	client->protocol.sensor_control = __sensor_control;
	client->protocol.sensor_delay = __sensor_delay;
	client->protocol.cmd_ack = __cmd_ack;
}

struct amt_handle *init_client_sock(struct amt_client_callback *cb)
{
	struct amt_client *a_client;
	struct amt_handle *a_handle;

	a_handle= malloc(sizeof(struct amt_handle));
	if(!a_handle)
		return NULL;
	a_client = malloc(sizeof(struct amt_client));
	memset(a_client, 0, sizeof(struct amt_client));
	if(!a_client)
	{
		free(a_handle);
		return NULL;
	}

	communicator_init();
	a_client->event_base = amt_event_base_init();

	if(cb)
	{
		memcpy(&a_client->cb, cb, sizeof(struct amt_client_callback));
		if(cb->log_cb)
			amt_log_register(&a_client->log_handle, cb->log_cb);
	}
	init_protocol(a_client);
	amt_event_base_loop(a_client->event_base);
	a_handle->type = AMT_CLIENT;
	a_handle->point = a_client;
	return a_handle;
}

void deinit_client_sock(struct amt_handle *handle)
{
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	amt_event_base_deinit(client->event_base);
	free(handle->point);
	free(handle);
	communicator_deinit();
}

int connect_client2server(struct amt_handle *handle, char *ip, int port)
{
	struct amt_event *event;
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return -1;
	client = handle->point;
	client->sock_tcp = connect_tcp_addr(ip, port);
	if(client->sock_tcp <= 0)
	{
		LOGE(&client->log_handle, "%s error\n", __func__);
		return -1;
	}
	strncpy(client->server_ip, ip, 16);
	event = amt_event_set(&client->event_base, client->sock_tcp, TYPE_TCP);
	amt_event_add(client->event_base, event, event_read_cb, event);
	client->event_tcp = event;
	return 0;
}

void control_client_log(struct amt_handle *handle, int tag_on)
{
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	if(client->cb.log_cb)
		amt_log_control(&client->log_handle, tag_on);
}

static void __data_buffer_send_common_sync(struct amt_client *client, void *data, int size, int type)
{
	if(!client->event_tcp)
		return;
	if((type == TYPE_TCP) || (!client->event_udp))
		amt_event_buffer_write_sync(client->event_tcp, data, size, NULL);
	else
		amt_event_buffer_write_sync(client->event_udp, data, size, &client->sock_udp_addr);
}

static void __data_client_send_test(struct amt_handle *handle, char *test, int type)
{
	struct protocol_event packet;
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	data_set_test(&client->protocol, &packet, test);
	__data_buffer_send_common_sync(client, &packet, sizeof(struct protocol_event), type);
}

void data_client_send_test(struct amt_handle *handle, char *test)
{
	__data_client_send_test(handle, test, TYPE_TCP);
}

void data_client_send_test_udp(struct amt_handle *handle, char *test)
{
	__data_client_send_test(handle, test, TYPE_UDP);
}

static void __sensor_client_send_data(struct amt_handle *handle, int num, struct amt_sensor_data *sensor, int type)
{
	struct protocol_event packet;
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	data_set_sensor_data(&client->protocol, &packet, num, sensor);
	__data_buffer_send_common_sync(client, &packet, sizeof(struct protocol_event), type);
}

void sensor_client_send_data(struct amt_handle *handle, int num, struct amt_sensor_data *sensor)
{
	__sensor_client_send_data(handle, num, sensor, TYPE_TCP);
}

void sensor_client_send_data_udp(struct amt_handle *handle, int num, struct amt_sensor_data *sensor)
{
	__sensor_client_send_data(handle, num, sensor, TYPE_UDP);
}

static void __mouse_client_send_data(struct amt_handle *handle, int x, int y, int button, int press, int type)
{
	struct protocol_event packet;
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	data_set_mouse_data(&client->protocol, &packet, x, y, button, press);
	__data_buffer_send_common_sync(client, &packet, sizeof(struct protocol_event), type);
}

void mouse_client_send_data(struct amt_handle *handle, int x, int y, int button, int press)
{
	__mouse_client_send_data(handle, x, y, button, press, TYPE_TCP);
}

void mouse_client_send_data_udp(struct amt_handle *handle, int x, int y, int button, int press)
{
	__mouse_client_send_data(handle, x, y, button, press, TYPE_UDP);
}

static void __touch_client_send_data(struct amt_handle *handle, int num, int *x, int *y, int *press, int type)
{
	struct protocol_event packet;
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	data_set_touch_data(&client->protocol, &packet, num, x, y, press);
	__data_buffer_send_common_sync(client, &packet, sizeof(struct protocol_event), type);
}

void touch_client_send_data(struct amt_handle *handle, int num, int *x, int *y, int *press)
{
	__touch_client_send_data(handle, num, x, y, press, TYPE_TCP);
}

void touch_client_send_data_udp(struct amt_handle *handle, int num, int *x, int *y, int *press)
{
	__touch_client_send_data(handle, num, x, y, press, TYPE_UDP);
}

static void __key_client_send_data(struct amt_handle *handle, int code, int press, int type)
{
	struct protocol_event packet;
	struct amt_client *client;
	if(handle->type != AMT_CLIENT)
		return;
	client = handle->point;
	data_set_key_data(&client->protocol, &packet, code, press);
	__data_buffer_send_common_sync(client, &packet, sizeof(struct protocol_event), type);
}


void key_client_send_data(struct amt_handle *handle, int code, int press)
{
	__key_client_send_data(handle, code, press, TYPE_TCP);
}

