#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "log.h"
#include "communicator.h"

#define SOCKET_NORMAL	1
#define SOCKET_STOP	2
#define SOCKET_ERR	3

struct send_msg
{
	struct list_head list;
	void *data;
	int size;
	struct sockaddr addr;
};

static int ref_count = 0;
int communicator_init(void)
{
	int ret = 0;
	if(ref_count++ > 0)
		return 0;
#ifdef WIN32
	WSADATA wsaData;
	ret = WSAStartup(MAKEWORD(2, 2), &wsaData);
#endif  /*   WIN32  */
	return ret;
}

void communicator_deinit(void)
{
	if(--ref_count > 0)
		return;
#ifdef WIN32
	WSACleanup();
#endif
}

SOCKET listen_tcp_port(int port)
{
    struct sockaddr_in addr;
    SOCKET sock;
    int one = 1;

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_ANY);

    if((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0)
	    return -1;
    if(setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char *)&one, sizeof(one)) < 0)
	{
		closesocket(sock);
	    return -1;
    }
    if(bind(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
	{
		closesocket(sock);
		return -1;
    }
    if(listen(sock, MAX_LISTEN) < 0)
	{
		closesocket(sock);
		return -1;
    }

    return sock;
}


SOCKET connect_tcp_addr(char *host, int port)
{
    struct hostent *hp;
    SOCKET sock;
    struct sockaddr_in addr;

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);

    if((addr.sin_addr.s_addr = inet_addr(host)) == -1)
    {
		if (!(hp = gethostbyname(host)))
			return -1;
	    addr.sin_addr.s_addr = *(unsigned long *)hp->h_addr;
    }

    if((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0)
		return -1;

    if(connect(sock, (struct sockaddr *)&addr, (sizeof(addr))) < 0)
	{
		closesocket(sock);
		return -1;
    }

    return sock;
}

SOCKET create_udp_sock(void)
{
	return socket(AF_INET, SOCK_DGRAM, 0); 
}

SOCKET listen_udp_port(int port)
{
    struct sockaddr_in addr;
    SOCKET sock;
    int one = 1;

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_ANY);

    if((sock = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
		return -1;
    if(setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char *)&one, sizeof(one)) < 0)
	{
		closesocket(sock);
		return -1;
	}
    if(bind(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0)
	{
		closesocket(sock);
		return -1;
	}

    return sock;
}

SOCKET amt_sock_accept(SOCKET sock, struct sockaddr *addr)
{
#ifdef WIN32
	int len;
#else
	socklen_t len;
#endif
	len = sizeof(struct sockaddr);
	memset(addr, 0, len);
	return accept(sock, addr, &len);
}

char *amt_get_ip(struct sockaddr *addr)
{
	struct sockaddr_in *addr_in = (struct sockaddr_in *)addr;
	return inet_ntoa(addr_in->sin_addr);
}

void amt_set_sockaddr(struct sockaddr *addr, char *ip, unsigned short port)
{
	struct sockaddr_in *addr_in = (struct sockaddr_in *)addr;
	memset(addr_in, 0, sizeof(struct sockaddr));
	addr_in->sin_family = AF_INET;
	addr_in->sin_port = htons(port);
	addr_in->sin_addr.s_addr = inet_addr(ip);
}

void close_socket(struct amt_event *event)
{
	if(event->sock > 0)
		closesocket(event->sock);
	event->sock = 0;
}

struct amt_event_base *amt_event_base_init(void)
{
	struct amt_event_base *base = malloc(sizeof(struct amt_event_base));
	if(!base)
		return NULL;
	INIT_LIST_HEAD(&base->head);
	pthread_mutex_init(&base->mutex, NULL);
	memset(&base->loop_tid, 0, sizeof(pthread_t));
	base->event_num = 0;
	base->exit_flag = 0;
	return base;
}

static void amt_event_buffer_send_one(struct amt_event *event)
{
	struct send_msg *msg = NULL;
	pthread_mutex_lock(&event->write_mutex);
	if(!list_empty(&event->write_list))
	{
		msg = list_entry(event->write_list.next, typeof(*msg), list);
		list_del(&msg->list);
	}
	pthread_mutex_unlock(&event->write_mutex);
	if(msg)
	{
		int ret;
		if(event->tcp_udp_type == TYPE_TCP)
			ret = send(event->sock, msg->data, msg->size, 0);
		else
			ret = sendto(event->sock, msg->data, msg->size, 0, &msg->addr, sizeof(struct sockaddr));
		free(msg->data);
		free(msg);
	}
}

static void amt_event_del_nolock(struct amt_event *event);

static void *loop_thread(void *arg)
{
	struct amt_event_base *base = arg;
	while(!base->exit_flag)
	{
		int ret;
		SOCKET max_sock = 0;
		fd_set rdfds, wrfds;
		struct amt_event *event, *event_next;
		struct timeval timeout = {0, SELECT_TIMEOUT_MS*1000};

		FD_ZERO(&rdfds);
		FD_ZERO(&wrfds);
		pthread_mutex_lock(&base->mutex);
		list_for_each_entry_safe(event, event_next, &base->head, list)
		{
			switch(event->status)
			{
				case SOCKET_ERR:
					amt_event_del_nolock(event);
					break;
				
				case SOCKET_NORMAL:
					FD_SET(event->sock, &rdfds);
					if(!list_empty(&event->write_list))
						FD_SET(event->sock, &wrfds);
					if(event->sock > max_sock)
						max_sock = event->sock;
					break;

				default:
					break;
			}
		}
		pthread_mutex_unlock(&base->mutex);
		ret = select(max_sock + 1, &rdfds, &wrfds, NULL, &timeout);
		if(ret > 0)
		{
			int count = 0;
			pthread_mutex_lock(&base->mutex);
			list_for_each_entry(event, &base->head, list)
			{
				pthread_mutex_unlock(&base->mutex);
				if((FD_ISSET(event->sock, &rdfds)) && (event->status == SOCKET_NORMAL))
				{
					if(event->read_cb)
						event->read_cb(event->data);
					count++;
				}
				if((FD_ISSET(event->sock, &wrfds)) && (event->status == SOCKET_NORMAL))
				{
					amt_event_buffer_send_one(event);
					count++;
				}
				pthread_mutex_lock(&base->mutex);
				if(count >= ret)
					break;
			}
			pthread_mutex_unlock(&base->mutex);
		}
	}
	pthread_exit("exit loop_thread");
	return NULL;
}

int amt_event_base_loop(struct amt_event_base *base)
{
	return pthread_create(&base->loop_tid, NULL, loop_thread, base);
}

static int amt_event_buffer_write_nolock(struct amt_event *event, void *data, int size, struct sockaddr *dst_addr)
{
	struct send_msg *msg = malloc(sizeof(struct send_msg));
	if(!msg)
		return -1;
	memset(msg, 0, sizeof(struct send_msg));
	msg->data = malloc(size);
	if(!msg->data)
	{
		free(msg);
		return -1;
	}
	memcpy(msg->data, data, size);
	msg->size = size;
	if(dst_addr)
		memcpy(&msg->addr, dst_addr, sizeof(struct sockaddr));
	pthread_mutex_lock(&event->write_mutex);
	list_add_tail(&msg->list, &event->write_list);
	pthread_mutex_unlock(&event->write_mutex);
	return size;
}

int amt_event_buffer_write(struct amt_event *event, void *data, int size, struct sockaddr *dst_addr)
{
	pthread_mutex_lock(&((*event->base)->mutex));
	if(event->status == SOCKET_NORMAL)
		size = amt_event_buffer_write_nolock(event, data, size, dst_addr);
	else
		size = 0;
	pthread_mutex_unlock(&((*event->base)->mutex));
	return size;
}

int amt_event_buffer_write_sync(struct amt_event *event, void *data, int size, struct sockaddr *dst_addr)
{
	int ret;
	if(event->status != SOCKET_NORMAL)
		return 0;
	if(event->tcp_udp_type == TYPE_TCP)
		ret = send(event->sock, data, size, 0);
	else
		ret = sendto(event->sock, data, size, 0, dst_addr, sizeof(struct sockaddr));
	return ret;
}

int amt_event_buffer_write_all(struct amt_event_base *base, void *data, int size, struct sockaddr *dst_addr, write_all_filter filter, void *arg)
{
	int count = 0;
	struct amt_event *event;
	pthread_mutex_lock(&base->mutex);
	list_for_each_entry(event, &base->head,list)
	{
		if(event->status != SOCKET_NORMAL)
			continue;
		if(filter && filter(event->sock, arg))
			continue;
		amt_event_buffer_write_nolock(event, data, size, dst_addr);
		count++;
	}
	pthread_mutex_unlock(&base->mutex);
	return count;
}

int amt_event_buffer_read(struct amt_event *event, void *data, int size, struct sockaddr *src_addr)
{
#ifdef WIN32
	int len;
#else
	socklen_t len;
#endif
	len = sizeof(struct sockaddr);
	if(src_addr)
		memset(src_addr, 0, len);
	if(event->tcp_udp_type == TYPE_TCP)
		return recv(event->sock, data, size, 0);
	else
		return recvfrom(event->sock, data, size, 0, src_addr, &len);
}

struct amt_event *amt_event_set(struct amt_event_base **base, SOCKET sock, int sock_type)
{
	struct amt_event *event = malloc(sizeof(struct amt_event));
	if(!event)
		return NULL;

	event->base = base;
	INIT_LIST_HEAD(&event->list);
	event->sock = sock;
	event->tcp_udp_type = (sock_type == TYPE_TCP)? TYPE_TCP: TYPE_UDP;
	INIT_LIST_HEAD(&event->write_list);
	pthread_mutex_init(&event->write_mutex, NULL);
	event->err_code = 0;
	event->status = SOCKET_NORMAL;
	return event;
}

void amt_event_add(struct amt_event_base *base, struct amt_event *event, amt_event_read_callback cb, void *data)
{
	event->read_cb = cb;
	event->data = data;
	pthread_mutex_lock(&base->mutex);
	list_add_tail(&event->list, &base->head);
	pthread_mutex_unlock(&base->mutex);
}

static void amt_event_del_nolock(struct amt_event *event)
{
	struct send_msg *msg, *msg_next;
	list_del(&event->list);

	pthread_mutex_lock(&event->write_mutex);
	list_for_each_entry_safe(msg, msg_next, &event->write_list, list)
	{
		list_del(&msg->list);
		free(msg->data);
		free(msg);
	}
	pthread_mutex_unlock(&event->write_mutex);
	close_socket(event);
	free(event);
}

void amt_event_del(struct amt_event *event)
{
	struct send_msg *msg, *msg_next;
	struct amt_event_base *base = *event->base;
	pthread_mutex_lock(&base->mutex);
	list_del(&event->list);
	pthread_mutex_unlock(&base->mutex);

	pthread_mutex_lock(&event->write_mutex);
	list_for_each_entry_safe(msg, msg_next, &event->write_list, list)
	{
		list_del(&msg->list);
		free(msg->data);
		free(msg);
	}
	pthread_mutex_unlock(&event->write_mutex);
	close_socket(event);
	free(event);
}

void amt_event_del_safe(struct amt_event *event)
{
	event->status = SOCKET_ERR;
}

void amt_event_base_deinit(struct amt_event_base *base)
{
	struct amt_event *event, *event_next;
	base->exit_flag = 1;
	pthread_join(base->loop_tid, NULL);
	pthread_mutex_lock(&base->mutex);
	list_for_each_entry_safe(event, event_next, &base->head, list)
		amt_event_del_nolock(event);
	pthread_mutex_unlock(&base->mutex);
	free(base);
}

static struct in_addr local_in_addr[32];
#ifdef WIN32
static struct in_addr *__get_local_ip_win32(int *count)
{
	int ret = 0;
	struct hostent *hp;
	char name[512];
	char **list;
	gethostname(name, 512);
	if(!(hp = gethostbyname(name)))
		return NULL;
	list = hp->h_addr_list;
	while(*list)
	{
		memcpy(&local_in_addr[ret], *list, sizeof(struct in_addr));
		list++;
		ret++;
	}
	if(count)
		*count = ret;
	return local_in_addr;
}
#else
static struct in_addr *__get_local_ip_linux(int *count)
{
	int socket_fd;
	struct ifreq *ifr;
	struct ifconf conf;
	char buff[2048];
	int num;
	int i, ret;

	socket_fd = socket(AF_INET, SOCK_DGRAM, 0);
	conf.ifc_len = 2048;
	conf.ifc_buf = buff;
	ioctl(socket_fd, SIOCGIFCONF, &conf);
	num = conf.ifc_len / sizeof(struct ifreq);
	ifr = conf.ifc_req;
	for(i = 0, ret = 0; i<num; i++)
	{
		struct sockaddr_in *sin = (struct sockaddr_in *)(&ifr->ifr_addr);
		ioctl(socket_fd, SIOCGIFFLAGS, ifr);
		if(((ifr->ifr_flags & IFF_LOOPBACK) == 0) && (ifr->ifr_flags & IFF_UP))
		{
			memcpy(&local_in_addr[ret], &sin->sin_addr, sizeof(struct in_addr));
			ret++;
		}
		ifr++;
	}
	close(socket_fd);
	if(count)
		*count = ret;
	return local_in_addr;
}
#endif

struct in_addr *get_local_ip(int *count)
{
#ifdef WIN32
	return __get_local_ip_win32(count);
#else
	return __get_local_ip_linux(count);
#endif
}

SOCKET multicast_create_sock_server(char *ip, char *group_ip, unsigned short port, int ttl)
{
	SOCKET sock;
	int ret, one = 1;
	struct in_addr addr;
	struct ip_mreq multicast_addr;
	unsigned char c_ttl = ttl;

	sock = listen_udp_port(port);
	if(sock < 0)
		return sock;

	memset((void *)&multicast_addr, 0, sizeof(struct ip_mreq));
	multicast_addr.imr_interface.s_addr = inet_addr(ip);
	multicast_addr.imr_multiaddr.s_addr = inet_addr(group_ip);
	ret = setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&multicast_addr, sizeof(struct ip_mreq));
	if(ret < 0)
	{
		closesocket(sock);
		return -1;
	}

	memset((void *)&addr, 0, sizeof(struct in_addr));
	addr.s_addr = inet_addr(ip);
	ret = setsockopt(sock, IPPROTO_IP, IP_MULTICAST_IF, (char *)&addr, sizeof(struct in_addr));
	if(ret < 0)
		return ret;

	ret = setsockopt(sock, IPPROTO_IP, IP_MULTICAST_TTL, (char *)&c_ttl, sizeof(char));
	ret = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, (char *)&one, sizeof(int));
	if(ret < 0)
		return ret;
	return sock;
}

SOCKET multicast_create_sock_client(int ttl)
{
	SOCKET sock;
	char c_ttl = ttl;
	sock = socket(AF_INET, SOCK_DGRAM, 0);
	if(sock < 0)
		return sock;
	setsockopt(sock, IPPROTO_IP, IP_MULTICAST_TTL, &c_ttl, sizeof(char));
	return sock;
}

