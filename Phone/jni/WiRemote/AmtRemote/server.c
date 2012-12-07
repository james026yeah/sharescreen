#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <errno.h>
#include "log.h"
#include "communicator.h"
#include "protocol.h"

struct amt_server
{
	struct amt_event_base *event_base;
	SOCKET sock_tcp, sock_udp;
	unsigned short tcp_port, udp_port;
	struct amt_server_callback cb;
	struct amt_log_handle log_handle;
	struct protocol_handle protocol;

	pthread_mutex_t cmd_mutex;
	pthread_cond_t cmd_cond;
	unsigned short last_cmd;
	int last_ret;
	int cond_count;
};

static void event_read_cb(void *arg)
{
	int size;
	struct sockaddr addr;
	struct protocol_event packet;
	struct amt_event *event = arg;
	struct amt_server *server = (struct amt_server *)event->base;
	size = amt_event_buffer_read(event, &packet, sizeof(struct protocol_event), &addr);
	if(size <= 0)
	{
		LOGH(&server->log_handle, "socket error\n");
		amt_event_del_safe(event);
	}
	else if(size == sizeof(struct protocol_event))
		recv_packet(&server->protocol, &packet);
}

static void event_listen_cb(void *arg)
{
	struct sockaddr addr;
	SOCKET new_sock;
	struct protocol_event packet;
	struct amt_event *new_event;
	struct amt_event *event = arg;
	struct amt_server *server = (struct amt_server *)event->base;
	new_sock = amt_sock_accept(event->sock, &addr);
	new_event = amt_event_set(event->base, new_sock, TYPE_TCP);
	amt_event_add(*event->base, new_event, event_read_cb, new_event);
	LOGH(&server->log_handle, "%s accept ip: %s\n", __func__, amt_get_ip(&addr));

	cmd_set_udp_port(&server->protocol, &packet, server->udp_port);
	amt_event_buffer_write(new_event, &packet, sizeof(struct protocol_event), NULL);
}

static int update_test(void *arg, char *test)
{
	struct amt_server *server = arg;
	if(server->cb.update_test)
		server->cb.update_test(test);
	return RETURN_NORMAL;
}

static int __sensor_data(void *arg, unsigned int num, struct amt_sensor_data *data)
{
	struct amt_server *server = arg;
	if(server->cb.sensor_data)
		server->cb.sensor_data(num, data);
	return RETURN_NORMAL;
}

static int __mouse_data(void *arg, int x, int y, int button, int press)
{
	struct amt_server *server = arg;
	if(server->cb.mouse_data)
		server->cb.mouse_data(x, y, button, press);
	return RETURN_NORMAL;
}

static int __touch_data(void *arg, unsigned int num, int *x, int *y, int *press)
{
	struct amt_server *server = arg;
	if(server->cb.touch_data)
		server->cb.touch_data(num, x, y, press);
	return RETURN_NORMAL;
}

static int __key_data(void *arg, int code, int press)
{
	struct amt_server *server = arg;
	if(server->cb.key_data)
		server->cb.key_data(code, press);
	return RETURN_NORMAL;
}

static int cmd_response(void *arg, unsigned short cmd, int retval)
{
	struct amt_server *server = arg;
	LOGD(&server->log_handle, "%s: cmd = %d, ret = %d\n", __func__, cmd, retval);
	pthread_mutex_lock(&server->cmd_mutex);
	if(cmd == server->last_cmd)
	{
		if(retval != RETURN_ERROR)
			server->last_ret = retval;
		server->cond_count++;
		pthread_cond_signal(&server->cmd_cond);
	}
	pthread_mutex_unlock(&server->cmd_mutex);
	return RETURN_NORMAL;
}

static void init_protocol(struct amt_server *server)
{
	server->protocol.data = server;
	server->protocol.log = &server->log_handle;
	server->protocol.update_test = update_test;
	server->protocol.cmd_response = cmd_response;
	server->protocol.sensor_data = __sensor_data;
	server->protocol.mouse_data = __mouse_data;
	server->protocol.touch_data = __touch_data;
	server->protocol.key_data = __key_data;
}

struct amt_handle *init_server_sock(struct amt_server_callback *cb)
{
	struct amt_event *event;
	int port = SERVER_PORT;
	struct amt_server *a_server;
	struct amt_handle *a_handle;

	a_handle= malloc(sizeof(struct amt_handle));
	if(!a_handle)
		return NULL;
	a_server = malloc(sizeof(struct amt_server));
	memset(a_server, 0, sizeof(struct amt_server));
	if(!a_server)
	{
		free(a_handle);
		return NULL;
	}

	if(cb)
	{
		memcpy(&a_server->cb, cb, sizeof(struct amt_server_callback));
		if(cb->log_cb)
			amt_log_register(&a_server->log_handle, cb->log_cb);
	}

	if(a_server->cb.log_cb)
		amt_log_control(&a_server->log_handle, CB_LOGA); //enable init log

	init_protocol(a_server);
	communicator_init();
	a_server->event_base = amt_event_base_init();
	do
	{
		a_server->sock_tcp = listen_tcp_port(port);
		if(a_server->sock_tcp > 0)
		{
			LOGD(&a_server->log_handle, "%s open tcp port = %d\n", __func__, port);
			break;
		}
		else
			LOGD(&a_server->log_handle, "%s can't open tcp port = %d\n", __func__, port);
	} while(++port);
	a_server->tcp_port = port;
	event = amt_event_set(&a_server->event_base, a_server->sock_tcp, TYPE_TCP);
	amt_event_add(a_server->event_base, event, event_listen_cb, event);
	
	while(++port)
	{
		a_server->sock_udp = listen_udp_port(port);
		if(a_server->sock_udp > 0)
		{
			LOGD(&a_server->log_handle, "%s open udp port = %d\n", __func__, port);
			break;
		}
		else
			LOGD(&a_server->log_handle, "%s can't open udp port = %d", __func__, port);
	}
	a_server->udp_port = port;
	event = amt_event_set(&a_server->event_base, a_server->sock_udp, TYPE_UDP);
	amt_event_add(a_server->event_base, event, event_read_cb, event);

	pthread_mutex_init(&a_server->cmd_mutex, NULL);
	pthread_cond_init(&a_server->cmd_cond, NULL);
	amt_event_base_loop(a_server->event_base);
	a_handle->type = AMT_SERVER;
	a_handle->point = a_server;

	if(a_server->cb.log_cb)
		amt_log_control(&a_server->log_handle, CB_LOGA); //disable init log

	return a_handle;
}

void deinit_server_sock(struct amt_handle *handle)
{
	struct amt_server *server;
	if(handle->type != AMT_SERVER)
		return;
	server = handle->point;
	amt_event_base_deinit(server->event_base);
	free(handle->point);
	free(handle);
	communicator_deinit();
}

void control_server_log(struct amt_handle *handle, int tag_on)
{
	struct amt_server *server;
	if(handle->type != AMT_SERVER)
		return;
	server = handle->point;
	if(server->cb.log_cb)
		amt_log_control(&server->log_handle, tag_on);
}

static int write_filter(SOCKET sock, void *data)
{
	struct amt_server *server = data;
	if(server->sock_tcp == sock)
		return 1;
	if(server->sock_udp == sock)
		return 1;
	return 0;
}

static int send_command_common(struct amt_handle *handle, struct protocol_event *event)
{
	int count, ret;
	struct amt_server *server;
	struct timeval now;
	struct timespec outtime;
	if(handle->type != AMT_SERVER)
		return RETURN_ERROR;
	server = handle->point;

	pthread_mutex_lock(&server->cmd_mutex);
	server->last_ret = RETURN_ERROR;
	server->cond_count = 0;
	server->last_cmd = event->packet.control.cmd;
	count = amt_event_buffer_write_all(server->event_base, event, sizeof(struct protocol_event), NULL, write_filter, server);
	gettimeofday(&now, NULL);
	outtime.tv_sec = now.tv_sec + 3;
	outtime.tv_nsec = now.tv_usec * 1000;
	while(count > server->cond_count)
	{
		ret = pthread_cond_timedwait(&server->cmd_cond, &server->cmd_mutex, &outtime);
		if(ret == ETIMEDOUT)
			break;
	}
	ret = server->last_ret;
	pthread_mutex_unlock(&server->cmd_mutex);
	return ret;
}

int sensor_server_control(struct amt_handle *handle, int sensor, int on)
{
	struct protocol_event packet;
	struct amt_server *server;
	if(handle->type != AMT_SERVER)
		return RETURN_ERROR;
	server = handle->point;
	cmd_set_sensor_control(&server->protocol, &packet, sensor, on);
	return send_command_common(handle, &packet);
}

int sensor_server_delay(struct amt_handle *handle, int sensor, int delay)
{
	struct protocol_event packet;
	struct amt_server *server;
	if(handle->type != AMT_SERVER)
		return RETURN_ERROR;
	server = handle->point;
	cmd_set_sensor_delay(&server->protocol, &packet, sensor, delay);
	return send_command_common(handle, &packet);
}

