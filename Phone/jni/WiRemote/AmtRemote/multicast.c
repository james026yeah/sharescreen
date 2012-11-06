#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "log.h"
#include "communicator.h"

#define MULTICAST_IP	"239.255.255.223"
#define MULTICAST_PORT	11223
#define MAX_TTL	6

struct amt_multicast
{
	struct amt_event_base *event_base;
	SOCKET sock_server, sock_client;
	struct amt_event *event_client;
	struct sockaddr sock_addr;
	struct amt_multicast_callback cb;
	struct amt_log_handle log_handle;
};

static void event_read_cb(void *arg)
{
	int size;
	struct sockaddr addr;
	char buffer[1500];
	struct amt_event *event = arg;
	struct amt_multicast *multicast = (struct amt_multicast*)event->base;
	size = amt_event_buffer_read(event, buffer, 1500, &addr);
	if(size <= 0)
	{
		LOGH(&multicast->log_handle, "socket error\n");
	}
	else
	{
		int num;
		int self = 0;
		char dst_ip[32];
		struct in_addr *addr_list = get_local_ip(&num);
		strncpy(dst_ip, amt_get_ip(&addr), 16);

		while(num--)
		{
			if(strncmp(inet_ntoa(addr_list[num]), dst_ip, strlen(dst_ip)) == 0)
			{
				self = 1;
				break;
			}
		}
		if(!self && multicast->cb.recv_multicast)
			multicast->cb.recv_multicast(buffer, size, dst_ip);
	}
}

void add_multicast_interface(struct amt_handle *handle, char *ip)
{
	struct amt_event *event;
	struct amt_multicast *multicast;
	if(handle->type != AMT_MULTICAST)
		return;
	multicast = handle->point;
	multicast->sock_server = multicast_create_sock_server(ip, MULTICAST_IP, MULTICAST_PORT, MAX_TTL);
	event = amt_event_set(&multicast->event_base, multicast->sock_server, TYPE_UDP);
	amt_event_add(multicast->event_base, event, event_read_cb, event);
}

struct amt_handle *init_multicast_sock(struct amt_multicast_callback *cb)
{
	struct amt_event *event;
	struct amt_multicast *a_multicast;
	struct amt_handle *a_handle;

	a_handle= malloc(sizeof(struct amt_handle));
	if(!a_handle)
		return NULL;
	a_multicast = malloc(sizeof(struct amt_multicast));
	memset(a_multicast, 0, sizeof(struct amt_multicast));
	if(!a_multicast)
	{
		free(a_handle);
		return NULL;
	}

	communicator_init();
	amt_set_sockaddr(&a_multicast->sock_addr, MULTICAST_IP, MULTICAST_PORT);
	a_multicast->event_base = amt_event_base_init();

	a_multicast->sock_client = multicast_create_sock_client(MAX_TTL);
	event = amt_event_set(&a_multicast->event_base, a_multicast->sock_client, TYPE_UDP);
	amt_event_add(a_multicast->event_base, event, event_read_cb, event);
	a_multicast->event_client = event;

	if(cb)
	{
		memcpy(&a_multicast->cb, cb, sizeof(struct amt_multicast_callback));
		if(cb->log_cb)
			amt_log_register(&a_multicast->log_handle, cb->log_cb);
	}
	amt_event_base_loop(a_multicast->event_base);
	a_handle->type = AMT_MULTICAST;
	a_handle->point = a_multicast;
	return a_handle;
}

void deinit_multicast_sock(struct amt_handle *handle)
{
	struct amt_multicast *multicast;
	if(handle->type != AMT_MULTICAST)
		return;
	multicast = handle->point;
	amt_event_base_deinit(multicast->event_base);
	free(handle->point);
	free(handle);
	communicator_deinit();
}

void control_multicast_log(struct amt_handle *handle, int tag_on)
{
	struct amt_multicast *multicast;
	if(handle->type != AMT_MULTICAST)
		return;
	multicast = handle->point;
	if(multicast->cb.log_cb)
		amt_log_control(&multicast->log_handle, tag_on);
}

void send_multicast(struct amt_handle *handle, char *buf, int size)
{
	struct amt_multicast *multicast;
	if(handle->type != AMT_MULTICAST)
		return;
	multicast = handle->point;
	amt_event_buffer_write_sync(multicast->event_client, buf, size, &multicast->sock_addr);
}

