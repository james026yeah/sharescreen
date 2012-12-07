#ifndef _COMMUNICATOR_H
#define _COMMUNICATOR_H

#ifdef WIN32
	#include <winsock2.h>
	#include <ws2tcpip.h>
	#include "pthread.h"
#else
	#include <sys/types.h>
	#include <sys/socket.h>
	#include <sys/ioctl.h>
	#include <netinet/in.h>
	#include <arpa/inet.h>
	#include <net/if.h>
	#include <netdb.h>
	#include <pthread.h>
	#define closesocket close
	#define SOCKET int
#endif
#include <unistd.h>
#include "list.h"

#define TYPE_TCP 1
#define TYPE_UDP 2

#define MAX_LISTEN		20
#define SELECT_TIMEOUT_MS	200
typedef void (*amt_event_read_callback)(void *arg);
typedef int (*write_all_filter)(SOCKET sock, void *data);

struct amt_event
{
	struct list_head list;
	struct amt_event_base **base;
	SOCKET sock;
	int tcp_udp_type;
	amt_event_read_callback read_cb;
	void *data;
	struct list_head write_list;
	pthread_mutex_t write_mutex;
	int status;
	int err_code;
};

struct amt_event_base
{
	struct list_head head;
	pthread_mutex_t mutex;
	pthread_t loop_tid;
	int event_num;
	int exit_flag;
};

int communicator_init(void);
void communicator_deinit(void);
SOCKET listen_tcp_port(int port);
SOCKET connect_tcp_addr(char *host, int port);
SOCKET create_udp_sock(void);
SOCKET listen_udp_port(int port);
SOCKET amt_sock_accept(SOCKET sock, struct sockaddr *addr);
char *amt_get_ip(struct sockaddr *addr);
void amt_set_sockaddr(struct sockaddr *addr, char *ip, unsigned short port);
void close_socket(struct amt_event *event);
struct amt_event_base *amt_event_base_init(void);
int amt_event_base_loop(struct amt_event_base *base);
int amt_event_buffer_write(struct amt_event *event, void *data, int size, struct sockaddr *dst_addr);
int amt_event_buffer_write_sync(struct amt_event *event, void *data, int size, struct sockaddr *dst_addr);
int amt_event_buffer_write_all(struct amt_event_base *base, void *data, int size, struct sockaddr *dst_addr, write_all_filter filter, void *arg);
int amt_event_buffer_read(struct amt_event *event, void *data, int size, struct sockaddr *src_addr);
struct amt_event *amt_event_set(struct amt_event_base **base, SOCKET sock, int sock_type);
void amt_event_add(struct amt_event_base *base, struct amt_event *event, amt_event_read_callback cb, void *data);
//void amt_event_del(struct amt_event *event);
void amt_event_del_safe(struct amt_event *event);
void amt_event_base_deinit(struct amt_event_base *base);
struct in_addr *get_local_ip(int *count);
SOCKET multicast_create_sock_server(char *ip, char *group_ip, unsigned short port, int ttl);
SOCKET multicast_create_sock_client(int ttl);
#endif

