#ifndef _SERVER_H
#define _SERVER_H
#include <pthread.h>
#include "list.h"
#include "service.h"

#define MAX_CLIENT		255
#define	DROP_PACKET_TIME	100

enum SERVICE_MSG_TYPE
{
	MSG_CTRL = 0,
	MSG_AUDIO,
	MSG_IMAGE,
	MSG_MAX
};

struct send_msg
{
	struct list_head clients;
	struct list_head sendlist;
	struct list_head peer;
	int type;
	void *data;
	int size;
};

struct ishare_client
{
	struct list_head list;
	struct list_head msg_send[MSG_MAX];
	struct list_head msg_peer[MSG_MAX];
	int sock;
	char id;
	char mediatype;
	int status;
	int width;
	int height;
	int dpi;
	char name[MAX_NAME_SIZE + 1];
};

struct ishare_service
{
	struct list_head clients;
	struct list_head list_msg[MSG_MAX];
	int pending[MSG_MAX];
	int client_num;
	int client_start_num;
	pthread_mutex_t service_mutex;
	pthread_mutex_t send_mutex;
	pthread_cond_t send_cond;
	int flag;
	int status;
	int epoll_send;
	int rotation;
	pthread_t send_tid;
	char key[MAX_KEY_SIZE + 1];
};

int async_send_control(char client_id, void *buf, int size);
int async_send_audio(void *buf, int size);
int async_send_image(void *buf, int size);
void *sync_send_thread(void *arg);
int get_server_rotation(void);

void get_server_service(struct service_server *server);
void set_server_protocol(struct server_protocol *protocol);
void set_server_control(struct service_control *control);
#endif

