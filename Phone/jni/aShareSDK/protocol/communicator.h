#ifndef _ISHARE_COMMUNICATOR_H
#define _ISHARE_COMMUNICATOR_H

#include "protocol.h"

extern int __init_socket(void);
extern int __clear_socket(void);
extern int __create_socket(SOCKET_t *fd);
extern int __close_socket(SOCKET_t fd);
extern int __shutdown_socket(SOCKET_t fd, int how);

extern int __bind_socket(SOCKET_t sock, const struct sockaddr *addr, SOCKLEN_t addrlen);
extern int __listen_socket(SOCKET_t sock, int backlog);
extern int __open_server_socket(SOCKET_t *sockfd, int port);
extern int __connect_server(char* srvAddr, SOCKET_t *client_sock, int port);
extern int __accept_client(SOCKET_t fd);
extern int __set_sock_block(SOCKET_t sock);
extern int __set_sock_noblock(SOCKET_t sock);
extern int __setsockopt(SOCKET_t sock, int level, int optname,	const void *optval, SOCKLEN_t optlen);

extern int __send_socket(SOCKET_t sock, void *pBuf, int iLen, int iFlags);
extern int __recv_socket(SOCKET_t sock, void *pBuf, int iLen, int iFlags);

extern int __create_thread(PHANDLE_t *tid, THREAD_FUNC fn, void *arg);
extern int __exit_thread(void);
extern int __wait_thread(PHANDLE_t handle);

/* legacy event I/F */
extern int __create_event(EVENT_t *ev, MUTEX_t *lock);
extern int __wait_event(EVENT_t *ev, MUTEX_t *lock, int timeout);
extern int __send_event(EVENT_t *ev, MUTEX_t *lock);
extern int __reset_event(EVENT_t *ev);
extern int __destroy_event(EVENT_t *ev, MUTEX_t *lock);

extern int __init_event(EVENT_t *ev, MUTEX_t *lock, int *cond);
extern int __check_event(EVENT_t *ev, MUTEX_t *lock, int *cond, int timeout);
extern int __set_event(EVENT_t *ev, MUTEX_t *lock, int *cond);
extern int __clear_event(EVENT_t *ev);
extern int __deinit_event(EVENT_t *ev, MUTEX_t *lock, int *cond);


extern u64 __get_system_mstime(void);
extern void __msleep(int msec);

#endif //_ISHARE_COMMUNICATOR_H
