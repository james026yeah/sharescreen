#ifndef __ISHARE_PROTOCOL_SERVER_H
#define __ISHARE_PROTOCOL_SERVER_H

#include "common.h"
#include "service.h"

struct client_info
{
	SOCKET_t sock;
	int id;
};

enum server_state
{
	SER_STATE_NULL,
	SER_STATE_IDLE,
	SER_STATE_STARTING,
	SER_STATE_START,
	SER_STATE_STOPING,
};

struct ptl_server
{
	char listen_running;
	PHANDLE_t tid;
	int state;
	struct client_info ci[SOCK_BACKLOG];
	int server_sock;
	int client_sock; //pre-communication
	char *send_buf;

	EVENT_t sync_event;
	MUTEX_t sync_mutex;

	struct service_server service_cb;
};
#endif
