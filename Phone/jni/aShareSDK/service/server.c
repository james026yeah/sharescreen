#include <string.h>
#include <pthread.h>
#include <fcntl.h>
#include <sys/epoll.h>
#include "listop.h"
#include "server.h"
#include "hal.h"
#include "common.h"
#include "service.h"

#define DEBUG_PERFORMANCE
static struct server_protocol g_protocol;
static struct ishare_service g_service;

static int alloc_client_id(void)
{
	static char increase_id = 0;
	return increase_id++;
}

static int create_thread(void *(*fn)(void *), void *arg, pthread_t *ret_tid)
{
	int ret;
	pthread_t tid;
	ret = pthread_create(&tid, NULL, fn, arg);
	*ret_tid = tid;
	return ret;
}

static uint64_t timestamp()
{
    struct timeval time;
    uint64_t timestamp;

    gettimeofday(&time, NULL);

    /* timestamp is micro secend */
    timestamp = (uint64_t)time.tv_sec * 1000 + time.tv_usec / 1000;
    return timestamp;
}

static int image_process_time(char *buf)
{
	struct jpeg_frame *jpeg;
	uint64_t current_time, interval;
	jpeg = (void *)((int)buf + sizeof(struct mm_data));
	current_time = timestamp();
	interval = current_time - jpeg->timestamp;
	return (int)interval;
}

static int init_server(void)
{
	int ret;

	LOGD("%s: %d\n", __func__, __LINE__);
	g_service.flag = SRV_TYPE_IMAGE_AUDIO;
	g_service.status = STATUS_STOP;
	g_service.client_num = 0;
	g_service.client_start_num = 0;
	g_service.epoll_send = epoll_create(MAX_CLIENT);
	g_service.rotation = 0;
	for(ret = 0; ret < MSG_MAX; ret++)
		g_service.pending[ret] = 0;
	init_service_list(&g_service);
	pthread_mutex_init(&g_service.service_mutex, NULL);
	pthread_mutex_init(&g_service.send_mutex, NULL);
	pthread_cond_init(&g_service.send_cond, NULL);
	memset(g_service.key, 0, MAX_KEY_SIZE);
	ret = g_protocol.init_protocol();
	if(ret < 0)
	{
		LOGD("---------------------------------------");
		LOGD("init_protocol return error!\n");
		return -1;
	}
	ret = init_hal_socket(&g_service);
	if(ret < 0)
	{
		g_protocol.deinit_protocol();
		return -1;
	}
	LOGI("init_service OK!\n");
	return 0;
}

static int stop_server(void);
static int deinit_server(void)
{
	if(g_service.status != STATUS_STOP)
		stop_server();
	close(g_service.epoll_send);
	hal_send_control(g_service.flag, GRAB_STOP);
	deinit_hal_socket(&g_service);
	g_protocol.deinit_protocol();
	return 1;
}

static int server_get_client_list(struct ctl_client *clients)
{
	int ret = 0;
	pthread_mutex_lock(&g_service.service_mutex);
	if(g_service.client_num)
	{
		struct ishare_client *pos;
		list_for_each_entry(pos, &g_service.clients, list)
		{
			if(clients)
			{
				clients[ret].id = pos->id;
				strcpy(clients[ret].name, pos->name);
			}
			ret++;
		}
	}
	pthread_mutex_unlock(&g_service.service_mutex);

	return ret;
}

static int server_get_client_status(char client_id)
{
	int ret = 0;
	struct ishare_client *client;
	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, client_id, NULL);
	if(client)
		ret = client->status;
	pthread_mutex_unlock(&g_service.service_mutex);
	return ret;
}

static int start_client_transfer(char client_id)
{
	struct ishare_client *client;

	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, client_id, NULL);
	if(!client)
	{
		pthread_mutex_unlock(&g_service.service_mutex);
		return -1;
	}
	if(client->status != STATUS_START)
	{
		client->status = STATUS_START;

		if(g_service.client_start_num++ == 0)
		{
			LOGD("start grab\n");
			hal_send_control(g_service.flag, GRAB_START);
			//START GRAB
		}
	}
	pthread_mutex_unlock(&g_service.service_mutex);

	return 1;
}

static int stop_client_transfer(char client_id)
{
	struct ishare_client *client;

	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, client_id, NULL);
	if(!client)
	{
		pthread_mutex_unlock(&g_service.service_mutex);
		return -1;
	}

	if(client->status != STATUS_STOP)
	{
		client->status = STATUS_STOP;

		if(--g_service.client_start_num == 0)
		{
			LOGD("stop grab\n");
			hal_send_control(g_service.flag, GRAB_PAUSE);
			//STOP GRAB
		}
	}
	pthread_mutex_unlock(&g_service.service_mutex);

	return 1;
}

static int compare_key(char *key)
{
	if(g_service.key[0] == 0)
		return 0;
	return strcmp(g_service.key, key);
}

static int add_client(char *name, char *key, int sock)
{
	struct ishare_client *client, *temp;
	char id;
	struct epoll_event ev;

	LOGD("add client: %s\n", name);
	if(compare_key(key) != 0)
	{
		LOGE("%s password error\n", __func__);
		return -2;
	}
	temp = malloc(sizeof(struct ishare_client));
	if(!temp)
	{
		LOGE("add Client: No memory\n");
		return -1;
	}

	pthread_mutex_lock(&g_service.service_mutex);
	do {
		id = alloc_client_id();
		client = find_client_from_list(&g_service, id, NULL);
		if(client == NULL)
			break;
	} while(1);
	temp->id = id;
	temp->sock = sock;
	temp->status = STATUS_STOP;
	strcpy(temp->name, name);

	add_client_list(&g_service, temp);
	g_service.client_num++;
	pthread_mutex_unlock(&g_service.service_mutex);
	status_changed(name);
	ev.data.fd = sock;
	ev.events = EPOLLOUT;
	epoll_ctl(g_service.epoll_send, EPOLL_CTL_ADD, sock, &ev);
	return id;
}

static void delete_msg(struct send_msg *msg)
{
	g_service.pending[msg->type]--;
	free(msg->data);
	free(msg);
}

static void del_client_msg(struct list_head *head)
{
	struct send_msg *msg, *msg_next;
	list_for_each_entry_safe(msg, msg_next, head, sendlist)
		delete_msg(msg);
}

static int __del_client(char client_id, int is_protocol)
{
	struct ishare_client *client;
	struct epoll_event ev;
	struct list_head del;
	char name[MAX_NAME_SIZE];

	stop_client_transfer(client_id);
	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, client_id, NULL);
	if(!client)
	{
		pthread_mutex_unlock(&g_service.service_mutex);
		return -1;
	}

	LOGD("del client: %s\n", client->name);
	strcpy(name, client->name);
	ev.data.fd = client->sock;
	ev.events = EPOLLOUT;
	epoll_ctl(g_service.epoll_send, EPOLL_CTL_DEL, client->sock, &ev);
	del_client_list(&g_service, client, &del);
	del_client_msg(&del);
	g_service.client_num--;
	pthread_mutex_unlock(&g_service.service_mutex);
	status_changed(name);
	if(!is_protocol)
{
LOGD("g_protocol.control_client start");
		g_protocol.control_client(client->id, CTL_CLIENT_REMOVE, client->sock);
LOGD("g_protocol.control_client end");
}
	free(client);

	return 1;
}

static int del_client(char client_id)
{
	return __del_client(client_id, 1);
}

static int disconnect_client(char *name)
{
	char client_id = -1;
	struct ishare_client *client;
	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, -1, name);
	if(!client)
	{
		pthread_mutex_unlock(&g_service.service_mutex);
		return -1;
	}
	client_id = client->id;
	pthread_mutex_unlock(&g_service.service_mutex);
	return __del_client(client_id, 0);
}

static int set_server_rotation(int rotation)
{
	pthread_mutex_lock(&g_service.service_mutex);
	g_service.rotation = rotation;
	pthread_mutex_unlock(&g_service.service_mutex);
	LOGI("%s rotation = %d\n", __func__, rotation);
	return 1;
}

int get_server_rotation(void)
{
	return g_service.rotation;
}

static int set_server_key(char *key)
{
	pthread_mutex_lock(&g_service.service_mutex);
	if(strcmp(key, "") == 0)
		g_service.key[0] = 0;
	else
		strcpy(g_service.key, key);
	pthread_mutex_unlock(&g_service.service_mutex);
	return 1;
}

static int set_client_media_type(char client_id, char type)
{
	struct ishare_client *client;
	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, client_id, NULL);

	if(!client)
	{
		pthread_mutex_unlock(&g_service.service_mutex);
		return -1;
	}
	client->mediatype = type;
	pthread_mutex_unlock(&g_service.service_mutex);

	return 1;
}

static int server_set_client_info(char client_id, int width, int height, int dpi)
{
	struct ishare_client *client;
	pthread_mutex_lock(&g_service.service_mutex);
	client = find_client_from_list(&g_service, client_id, NULL);

	if(!client)
	{
		pthread_mutex_unlock(&g_service.service_mutex);
		return -1;
	}
	client->width = width;
	client->height = height;
	client->dpi = dpi;
	pthread_mutex_unlock(&g_service.service_mutex);

	return 1;
}

static int start_server(void)
{
	pthread_mutex_lock(&g_service.service_mutex);
	if(g_service.status != STATUS_START)
	{
		create_thread(sync_send_thread, NULL, &g_service.send_tid);
		g_protocol.start_protocol();
		g_service.status = STATUS_START;
	}
	pthread_mutex_unlock(&g_service.service_mutex);

	return 1;
}

static void async_signal(void)
{
	pthread_mutex_lock(&g_service.send_mutex);
	pthread_cond_signal(&g_service.send_cond);
	pthread_mutex_unlock(&g_service.send_mutex);
}

static int stop_server(void)
{
	struct ishare_client *client;
	int real_stop = 0;
	LOGD("stop_server start\n");
	pthread_mutex_lock(&g_service.service_mutex);
	if(g_service.status != STATUS_STOP)
	{
		real_stop = 1;
		g_service.status = STATUS_STOP;
		async_signal(); //wakeup send thread to exit itself;
		while(!list_empty(&g_service.clients))
		{
			client = get_first_client(&g_service);
			pthread_mutex_unlock(&g_service.service_mutex);
			__del_client(client->id, 0);
			pthread_mutex_lock(&g_service.service_mutex);
		}
		pthread_mutex_unlock(&g_service.service_mutex);
		LOGD("stop protocol start");
		g_protocol.stop_protocol();
		LOGD("stop protocol end");
	}
	else
		pthread_mutex_unlock(&g_service.service_mutex);
	if(real_stop)
	{	//send_thread need service_mutex
		LOGD("wait g_service.send_id start");
		pthread_join(g_service.send_tid, NULL);
		LOGD("wait g_service.send_id end");
	}

	LOGD("stop_server end\n");
	return 1;
}

static int get_server_status(void)
{
	return g_service.status;
}

static int set_server_media_type(int type)
{
	g_service.flag = type;
	return 1;
}

static int get_server_media_type(void)
{
	return g_service.flag;
}

static void list_del_msg(struct send_msg *msg)
{
	del_msg_list(&g_service, msg);
	delete_msg(msg);
}

static void drop_packet(int type)
{
	struct send_msg *msg;
	struct jpeg_frame *jpeg;
	uint64_t current_time, interval;
	switch(type)
	{
		case MSG_IMAGE:
			do {
				if(list_empty(&g_service.list_msg[type]))
					break;
				msg = get_first_msg(&g_service, type);
				jpeg = (void *)((int)msg->data + sizeof(struct mm_data));
				current_time = timestamp();
				interval = current_time - jpeg->timestamp;
				if(interval <= DROP_PACKET_TIME)
					break;
				LOGD("drop image packet: packet time: %lld ms, current time: %lld ms\n", jpeg->timestamp, current_time);
				list_del_msg(msg);
			} while(1);
			break;
		default:
			break;
	}
}

static struct send_msg *async_send_packet(char clientId, int type, void *buf, int size)
{
	int ret = 0;
	struct send_msg *msg;
	struct ishare_client *client = NULL;
	msg = malloc(sizeof(struct send_msg));
	if(!msg)
		return NULL;
	msg->type = type;
	msg->data = buf;
	msg->size = size;
	INIT_LIST_HEAD(&msg->clients);
	pthread_mutex_lock(&g_service.service_mutex);
	drop_packet(type);
	if(type == MSG_CTRL)
	{
		client = find_client_from_list(&g_service, clientId, NULL);
		if(client)
			ret = add_msg_list(&g_service, client, msg);
	}
	else
		ret = add_msg_list(&g_service, NULL, msg);

	if(ret)
		g_service.pending[type]++;
	pthread_mutex_unlock(&g_service.service_mutex);

	if(ret)
		async_signal();
	else
		free(msg);
	return msg;
}

int async_send_control(char client_id, void *buf, int size)
{
	async_send_packet(client_id, MSG_CTRL, buf, size);
	return 1;
}

int async_send_audio(void *buf, int size)
{
	async_send_packet(-1, MSG_AUDIO, buf, size);
	return 1;
}

int async_send_image(void *buf, int size)
{
	async_send_packet(-1, MSG_IMAGE, buf, size);
	return 1;
}

static inline int send_epoll_ready(int sock, struct epoll_event *ev, int nfds, int *bitmap, int *remain)
{
	int i;
	for(i = 0; i < nfds; i++)
	{
		if(ev[i].data.fd != sock)
			continue;
		if(bitmap[i] == 1)
			return 0;
		bitmap[i] = 1;
		(*remain)--;
		return 1;
	}
	return 0;
}

static int msg_pending()
{
	int i;
	for(i = 0; i < MSG_MAX; i++)
	{
		if(!(list_empty(&g_service.list_msg[i])))
		{
			return 1;
		}
	}
	return 0;
}

static int send_packet(struct ishare_client *client, struct send_msg *msg)
{
	int err = 0;
	switch(msg->type)
	{
		case MSG_CTRL:
			err = g_protocol.send_raw_data(client->sock, msg->data, msg->size);
			break;
		case MSG_AUDIO:
#if 1
			{
				uint64_t current_time, interval;
				//int audio_head_size = sizeof(struct mm_data) + sizeof(struct audio_info);
				struct audio_info *ai = (struct audio_info *)((char *)msg->data + sizeof(struct mm_data));
				current_time = timestamp();
				interval = current_time - ai->timestamp;
				LOGD("Audio delay time = %d\n", (int)interval);
				err = msg->size;
//				save_audio(((char *)msg->data + audio_head_size), msg->size - audio_head_size);
			}
#endif
			//err = g_protocol.send_audio_data(client->sock, msg->data, msg->size, client->id);
			break;
		case MSG_IMAGE:
			err = image_process_time(msg->data);
#ifndef DEBUG_PERFORMANCE
			if(err > 50)
#endif
				LOGD("PERFORMANCE: %s send image delay = %dms\n", __func__, err);
			err = g_protocol.send_image_data(client->sock, msg->data, msg->size, client->id);
		default:
			break;
	}
	return err;
}

static void __sync_send(void)
{
	int i;
	struct ishare_client *client, *client_next;
	struct send_msg *msg, *msg_next;
	struct epoll_event ev_send[MAX_CLIENT];
	int bitmap[MAX_CLIENT], nfds, remain;
	do
	{
		nfds = epoll_wait(g_service.epoll_send, ev_send, MAX_CLIENT, 20);
		if(nfds == -1) //timeout
			continue;
		remain = nfds;
		memset(bitmap, 0, nfds * sizeof(int));
		for(i = 0; i < MSG_MAX; i++)
		{
			pthread_mutex_lock(&g_service.service_mutex);
			list_for_each_entry_safe(msg, msg_next, &g_service.list_msg[i], sendlist)
			{
				list_for_each_entry_safe(client, client_next, &msg->clients, msg_send[i])
				{
					int err;
					if(send_epoll_ready(client->sock, ev_send, nfds, bitmap, &remain))
					{
						err = send_packet(client, msg);
						if(err == msg->size)
						{
							del_msg_list_client(&g_service, client, msg);
							if(list_empty(&msg->clients))
								delete_msg(msg);
						}
						if(remain <= 0)
							break;
					}
				}
				if(remain <= 0)
					break;
			}
			pthread_mutex_unlock(&g_service.service_mutex);
			if(remain <= 0)
				break;
		}
	} while(msg_pending());
}

void *sync_send_thread(void *arg)
{
	arg = arg; //warning
	do {
		int pending;
		pthread_mutex_lock(&g_service.send_mutex);
		pthread_mutex_lock(&g_service.service_mutex);
		pending = msg_pending();
		pthread_mutex_unlock(&g_service.service_mutex);
		if(!pending)
		{
			LOGD("Waiting send signal\n");
			pthread_cond_wait(&g_service.send_cond, &g_service.send_mutex);
			LOGD("Get send signal\n");
		}
		pthread_mutex_unlock(&g_service.send_mutex);
		if(g_service.status == STATUS_START)
			__sync_send();
	} while(g_service.status == STATUS_START);

	pthread_exit("sync_send exit");
	return NULL;
}

void get_server_service(struct service_server *server)
{
	server->add_client = add_client;
	server->del_client = del_client;
	server->start_client_transfer = start_client_transfer;
	server->stop_client_transfer = stop_client_transfer;
	server->set_client_media_type = set_client_media_type;
	server->set_client_info = server_set_client_info;
	server->async_send_control = async_send_control;
}

void set_server_protocol(struct server_protocol *protocol)
{
	memcpy(&g_protocol, protocol, sizeof(struct server_protocol));
}

void set_server_control(struct service_control *control)
{
	control->init_service = init_server;
	control->deinit_service = deinit_server;
	control->start_service = start_server;
	control->stop_service = stop_server;
	control->get_service_status = get_server_status;
	control->set_media_type = set_server_media_type;
	control->get_media_type = get_server_media_type;
	control->set_rotation = set_server_rotation;
	control->set_key = set_server_key;
	control->get_client_list = server_get_client_list;
	control->get_client_status = server_get_client_status;
	control->disconnect_client = disconnect_client;
}

