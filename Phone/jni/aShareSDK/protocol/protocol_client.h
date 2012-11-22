#ifndef __ISHARE_PROTOCOL_CLIENT_H
#define __ISHARE_PROTOCOL_CLIENT_H

#include "common.h"
#include "service.h"

enum client_state
{
	CLT_STATE_NULL,
	CLT_STATE_IDLE,  //ptl init
	CLT_STATE_CONNECTING, //sock connected + match time
	CLT_STATE_CONNECT, //client info approved
	CLT_STATE_START, //control start
};

struct ptl_client
{
	char state;
	char timestamp;
	char send_running;
	char recv_running;
	char lstn_running;
	PHANDLE_t send_handle;
	PHANDLE_t recv_handle;
	PHANDLE_t listen_handle;

	char server_ip[MAX_IPV4_SIZE + 1];
	int port;

	SOCKET_t client_sockfd;
	SOCKET_t listen_sockfd;
	ClientInfo_st ci;
	char key_state;

	char client_id;
	char srv_type;
	int init_cond;
	int send_cond;

	MUTEX_t srv_mutex;
	MUTEX_t deque_mutex;
	MUTEX_t init_mutex;
	MUTEX_t send_mutex;
	MUTEX_t sync_mutex;

	EVENT_t init_event;
	EVENT_t send_event;
	EVENT_t sync_event;

	struct service_client service_cb;
	struct item * send_msg;
	void * image_data;
	void * audio_data;

	u64 mt_start;
	u64 mt_stop; //client mstime while match time ok
	char mt_status; //1: match time OK; 0: match time NG
	u64 mt_zero; //server mstime while match time ok
};
#endif
