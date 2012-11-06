#ifdef WIN32
#include <WinBase.h>
#elif defined LINUX
#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <errno.h>
#endif

#include "communicator.h"

/************************************************************/
/* Thread Interface Portion									*/
/************************************************************/
int __create_thread(PHANDLE_t *tid, THREAD_FUNC fn, void *arg)
{
#ifdef WIN32
	PHANDLE_t handle;
	handle = CreateThread(NULL, 0, fn, NULL, 0, NULL);
	*tid = handle;
	return ESuccess;
#elif defined LINUX
	int ret;
	PHANDLE_t handle;

	ret = pthread_create(&handle, NULL, fn, arg);
	*tid = handle;
	return (ret == 0) ? ESuccess : EFailed;
#endif
}

int __exit_thread(void)
{
#ifdef WIN32
	ExitThread(0);
	return ESuccess;
#elif defined LINUX
	pthread_exit("listen thread abort");
	return ESuccess;
#endif
}

int __wait_thread(PHANDLE_t handle)
{
#ifdef WIN32
	return ESuccess;
#elif defined LINUX
	return pthread_join(handle, NULL);
#endif
}

int __wait_event(EVENT_t *ev, MUTEX_t *lock, int timeout)
{
#ifdef WIN32
	return (WAIT_OBJECT_0 != WaitForSingleObject(*ev, timeout)) ? EFailed : ESuccess;
#elif defined LINUX
	int ret;
	struct timeval now;
	struct timespec outtime;

	gettimeofday(&now, NULL);
	outtime.tv_sec = now.tv_sec + timeout / 1000;
	outtime.tv_nsec = now.tv_usec * 1000 + timeout % 1000;

	MUTEX_LOCK(lock);
	ret = pthread_cond_timedwait(ev, lock, &outtime);
	MUTEX_UNLOCK(lock);
	return (ret < 0 || ret == ETIMEDOUT) ? EFailed : ESuccess;
#endif
}

/** wait till cond's value get to val, max loop time is limited */
int __check_event(EVENT_t *ev, MUTEX_t *lock, int *cond, int timeout)
{
#ifdef WIN32
	return (WAIT_OBJECT_0 != WaitForSingleObject(*ev, timeout)) ? EFailed : ESuccess;
#elif defined LINUX
	struct timeval now;
	struct timespec outtime;
	int ret;

	LOGD("%s: start, timeout = %d\n", __func__, timeout);
	MUTEX_LOCK(lock);
	if (*cond <= 0)
	{
		gettimeofday(&now, NULL);
		outtime.tv_sec = now.tv_sec + timeout / 1000;
		outtime.tv_nsec = now.tv_usec * 1000 + timeout % 1000;
		ret = pthread_cond_timedwait(ev, lock, &outtime);
		if (0 != ret)
		{
			LOGD("%s: failed return %d, cond = %d\n", __func__, ret, *cond);
			MUTEX_UNLOCK(lock);
			return EFailed;
		}
	}

	*cond -= 1;
	if (*cond == -1)
	{
		LOGD("--------------------------------------");
		LOGD("           event error!               ");
		LOGD("--------------------------------------");
	}

	LOGD("%s: success, cond-- = %d\n", __func__, *cond);
	MUTEX_UNLOCK(lock);
	return ESuccess;
#endif
}

int __create_event(EVENT_t *ev, MUTEX_t *lock)
{
#ifdef WIN32
	*ev = CreateEvent(NULL, TRUE, FALSE, NULL);
	lock = lock;
	return (*ev) ? ESuccess : EFailed;
#elif defined LINUX
	MUTEX_INIT(lock);
	pthread_cond_init(ev, NULL);
	return ESuccess;
#endif
}

int __init_event(EVENT_t *ev, MUTEX_t *lock, int *cond)
{
#ifdef WIN32
	*ev = CreateEvent(NULL, TRUE, FALSE, NULL);
	lock = lock;
	return (*ev) ? ESuccess : EFailed;
#elif defined LINUX
	MUTEX_INIT(lock);
	*cond = 0;
	pthread_cond_init(ev, NULL);
	return ESuccess;
#endif
}

int __destroy_event(EVENT_t *ev, MUTEX_t *lock)
{
#ifdef WIN32
	//TODO
	return ESuccess;
#elif defined LINUX
	MUTEX_DEL(lock);
	ev = ev;
	return ESuccess;
#endif
}

int __deinit_event(EVENT_t *ev, MUTEX_t *lock, int *cond)
{
#ifdef WIN32
	//TODO
	return ESuccess;
#elif defined LINUX
	MUTEX_DEL(lock);
	*cond = 0;
	ev = ev;
	return ESuccess;
#endif
}

int __send_event(EVENT_t *ev, MUTEX_t *lock)
{
#ifdef WIN32
	return (SetEvent(*ev) == 0) ? EFailed : ESuccess;
#elif defined LINUX
	int ret;
	MUTEX_LOCK(lock);
	ret = pthread_cond_signal(ev);
	MUTEX_UNLOCK(lock);
	return (ret != 0) ? EFailed : ESuccess;
#endif
}

int __set_event(EVENT_t *ev, MUTEX_t *lock, int *cond)
{
#ifdef WIN32
	return (SetEvent(*ev) == 0) ? EFailed : ESuccess;
#elif defined LINUX
	int ret;
	MUTEX_LOCK(lock);
	*cond += 1;
	LOGD("%s: cond++ = %d\n", __func__, *cond);
	ret = pthread_cond_signal(ev);
	MUTEX_UNLOCK(lock);
	return (ret != 0) ? EFailed : ESuccess;
#endif
}

int __reset_event(EVENT_t *ev)
{
#ifdef WIN32
	ResetEvent(*ev);
#endif
	ev = ev;
	return ESuccess;
}

int __clear_event(EVENT_t *ev)
{
#ifdef WIN32
	ResetEvent(*ev);
#endif
	ev = ev;
	return ESuccess;
}

/************************************************************/
/* Socket Interface Portion									*/
/************************************************************/
int __init_socket(void)
{
#ifdef WIN32
	int ret;
	WSADATA wsaData;
	int iVersionHigh = 2;
	int iVersionLow = 2;
	WSAData *wsa = NULL;

	ret = WSAStartup(MAKEWORD(iVersionHigh,iVersionLow), ( NULL == wsa ) ? &wsaData : wsa);
	return (ret == 0) ? ESuccess : EFailed;
#else
	return ESuccess;
#endif
}

int __clear_socket(void)
{
#ifdef WIN32
	int ret;
	ret = WSACleanup();
	return (ret == 0) ? ESuccess : EFailed;
#else
	return ESuccess;
#endif
}

int __create_socket(SOCKET_t *fd)
{
	SOCKET_t sock;

	if (EFailed == __init_socket())
		return EFailed;

#ifdef WIN32
	sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (sock == INVALID_SOCKET)
	{
		LOGE("create socket failure. errcode is %d\n", WSAGetLastError());
	}
#elif defined LINUX
	sock = socket(AF_INET, SOCK_STREAM, 0);
	if (sock < 0)
	{
		LOGE("create socket failure: \"%s\"\n", strerror(errno));
	}
#endif
	*fd = sock;
	return (sock < 0) ? EFailed : ESuccess;
}

int __close_socket(SOCKET_t fd)
{
	int ret;

	LOGD("%s\n", __func__);
#ifdef WIN32
	ret = closesocket(fd);
	if (ret == SOCKET_ERROR )
	{
		LOGE("ERR: close socket failure. errcode is %d\n", WSAGetLastError());
	}

	return (ret == SOCKET_ERROR) ? EFailed : ESuccess;
#elif defined LINUX
	ret = close(fd);
	if (ret < 0)
	{
		LOGE("%s: %d failure: %s\n", __func__, __LINE__, strerror(errno));
	}

	return (ret < 0) ? EFailed : ESuccess;
#endif
}

int __shutdown_socket(SOCKET_t fd, int how)
{
	int ret;

	LOGD("%s\n", __func__);
	ret = shutdown(fd, how);
#ifdef WIN32
	if (SOCKET_ERROR == ret)
	{
		LOGE("shutdown socket failure error code = %d\n", WSAGetLastError());
		return EFailed;
	}
	return ESuccess;
#elif defined LINUX

	if (ret < 0)
	{
		LOGE("shutdown socket failure \"%s\"\n", strerror(errno));
		return EFailed;
	}

	return ESuccess;
#endif
}

int __set_sock_noblock(SOCKET_t fd)
{
#ifdef WIN32
	unsigned long ul = 1;
	if (SOCKET_ERROR == ioctlsocket(fd, FIONBIO, (u_long *)&ul))
	{
		LOGE("ioctlsocket error\n");
		return EFailed;
	}
	return ESuccess;
#elif defined LINUX
	int flag;

	if((flag = fcntl(fd, F_GETFL, 0)) < 0)
	{
		LOGE("fcntl F_GETFL Error\n");
	}
	flag |= O_NONBLOCK;
	if(fcntl(fd, F_SETFL, flag) < 0)
	{
		LOGE("fcntl F_SETFL Error\n");
	}
	return ESuccess;
#endif
}

int __set_sock_block(SOCKET_t fd)
{
#ifdef WIN32
	unsigned long ul = 0;
	if (SOCKET_ERROR == ioctlsocket(fd, FIONBIO, (u_long *)&ul))
	{
		LOGE("ioctlsocket error\n");
		return EFailed;
	}
	return ESuccess;
#elif defined LINUX
	int flag;

	if((flag = fcntl(fd, F_GETFL, 0)) < 0)
	{
		LOGE("fcntl F_GETFL Error\n");
	}
	flag &= ~O_NONBLOCK;
	if(fcntl(fd, F_SETFL, flag) < 0)
	{
		LOGE("fcntl F_SETFL Error\n");
	}
	return ESuccess;
#endif
}

int __setsockopt(SOCKET_t sock, int level, int optname,	const void *optval, SOCKLEN_t optlen)
{
#if WIN32
	//TODO PC server
	return ESuccess;
#elif defined LINUX
	int ret = setsockopt(sock, level, optname, optval, optlen);
	if (ret < 0)
	{
		LOGE("setsockopt failure: \"%s\"\n", strerror(errno));
		return EFailed;
	}

	return ESuccess;
#endif
}

int __bind_socket(SOCKET_t sock, const struct sockaddr *addr, SOCKLEN_t addrlen)
{
#if WIN32
	//TODO PC server
	return ESuccess;
#elif defined LINUX
	int ret = bind(sock, addr, addrlen);
	if (ret < 0)
	{
		LOGE("bind socket failure: \"%s\"\n", strerror(errno));
		return EFailed;
	}

	return ESuccess;
#endif
}

int __listen_socket(SOCKET_t sock, int backlog)
{
#if WIN32
	//TODO PC server
	return ESuccess;
#elif defined LINUX
	int ret = listen(sock, backlog);
	if (ret < 0)
	{
		LOGE("create listen queue failure: \"%s\"\n", strerror(errno));
		return EFailed;
	}

	return ESuccess;
#endif
}

int __accept_client(SOCKET_t fd)
{
	struct sockaddr_in addr;
	SOCKLEN_t addrlen = sizeof(addr);
	int clientfd;

	clientfd = accept(fd, (struct sockaddr*)&addr, &addrlen);

#ifdef WIN32
	if (clientfd < 0)
	{
		LOGE("ERR: accept socket failure. errcode is %d\n", WSAGetLastError());
		return EFailed;
	}
#elif defined LINUX
	if( clientfd < 0)
	{
		LOGE("Sock accept error. %s\n", strerror(errno));
		return EFailed;
	}
#endif
	return clientfd;
}

int __open_server_socket(SOCKET_t *sockfd, int port)
{
	SOCKET_t sock;
	IN_ADDR_T_t addr = htonl(INADDR_ANY);
	SOCKADDR_IN_t addr_in;
	int flag = 1;
	int len = sizeof(int);
	int ret;

	LOGD("%s\n", __func__);

	memset(&addr, 0, sizeof(addr));
	addr_in.sin_family = AF_INET;
	addr_in.sin_port = htons((uint16_t)port);
	addr_in.sin_addr.s_addr = addr;

	ret = __create_socket(&sock);
	if( ret == EFailed )
	{
		LOGE("create sock failure.\n");
		return EFailed;
	}

	if (__setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &flag, len) == EFailed)
	{
		LOGE("setsockopt SO_REUSEADDR failure\n");
		goto SERVER_SOCK_FAIL;
	}

	if(__bind_socket(sock, (struct sockaddr *)&addr_in, sizeof(addr_in)) == EFailed)
	{
		LOGE("bind sock failure. %s\n", strerror(errno));
		goto SERVER_SOCK_FAIL;
	}

	if(__listen_socket(sock, SOCK_BACKLOG) == EFailed)
	{
		LOGE("create listen queue failure. %s\n", strerror(errno));
		goto SERVER_SOCK_FAIL;
	}

	LOGE("Create server socket successfully. sockfd = %d\n", sock);
	*sockfd = sock;
	return ESuccess;

SERVER_SOCK_FAIL:
	LOGE("%s: create server socket failure\n", __func__);
	__close_socket(sock);
	return EFailed;
}

static int __connect_server_internal(SOCKET_t fd, char *srvAddr, int port)
{
	SOCKADDR_IN_t addr;
	int ret;

	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = inet_addr(srvAddr);
	addr.sin_port = htons(port);

	LOGD("%s: server ip = %s, port = %d\n", __func__, srvAddr, port);
	ret = connect(fd, (struct sockaddr *)&addr, sizeof(addr));
#ifdef WIN32
	if (0 == ret)
	{
		int iNodelay = 1;
		ret = setsockopt(fd, IPPROTO_TCP ,
			TCP_NODELAY, (char*)&iNodelay, sizeof(iNodelay));
		if (SOCKET_ERROR == ret)
			goto CONNECT_INT_FAIL;
	}
	else if (SOCKET_ERROR == ret)
	{
		LOGE("ERR: connect socket failure. errcode is %d\n", WSAGetLastError());
		goto CONNECT_INT_FAIL;
	}
#elif defined LINUX
	if (ret != 0) {
		LOGE("connect failure, %s\n", strerror(errno));
		goto CONNECT_INT_FAIL;
	}
#endif
	return ESuccess;

CONNECT_INT_FAIL:
	__close_socket(fd);
	__clear_socket();
	return EFailed;
}

int __connect_server(char* srvAddr, SOCKET_t *client_sock, int port)
{
	int ret;
	char len;

	if (!srvAddr && strlen(srvAddr) > MAX_IPV4_SIZE)
		return EFailed;

	len = strlen(srvAddr);
	if (len > 16)
	{
		LOGE("ERR: ip address is more than 16 characters\n");
		return EFailed;
	}

	if (__create_socket(client_sock) == EFailed)
		return EFailed;

	__set_sock_block(*client_sock);

	ret = __connect_server_internal(*client_sock, srvAddr, port);

	if (ret == EFailed)
	{
		LOGE("connect failure!\n");
		return EFailed;
	}
	return ESuccess;
}

int __recv_socket(SOCKET_t fd, void *pBuf,int iLen, int iFlags)
{
	char *p;
	int iLeft, iRead;
	int iCnt, retry;

	if (NULL == pBuf || 0 == iLen)
	{
		return -1;
	}

	iLeft = iLen;
	p = (char *)pBuf;
	iCnt = 0;
	retry = 10;

	while (iLeft > 0 && retry > 0) {

		iRead = recv(fd, (char *)p, iLeft, iFlags);
		if (iRead == 0 && iCnt == 0) {
			LOGE("recvData error [socket disconnected]\n");
			return -1;
		}

		if(iRead < 0) {
#ifdef WIN32
			int err = WSAGetLastError();
			if (err == WSAEWOULDBLOCK || err == WSAEINTR){
#elif defined LINUX
			if (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR){
#endif
				//LOGD("recvData retry iLeft = %d ... \n", iLeft);
				__msleep(1);
				retry--;
				continue;
			}
			else {
#ifdef WIN32
				LOGE("recvData error = %d\n", WSAGetLastError());
#elif defined LINUX
				LOGE("recvData error = %s\n", strerror(errno));
#endif
				return -1;
			}
		}
		else {
			iLeft -= iRead;
			p += iRead;
		}

		iCnt++;
	}

	return iLen - iLeft;
}

int __send_socket(SOCKET_t fd, void *pBuf, int iLen, int iFlags)
{
	char *p;
	int iSend, iLeft;

	iLeft = iLen;
	p = (char *)pBuf;

	if (NULL == pBuf || 0 == iLen)
	{
		return -1;
	}

	while(iLeft > 0)
	{
#if 0
		if(iLeft > MAX_SEND_MTU)
			iSend = send(fd, p, MAX_SEND_MTU, iFlags);
		else
			iSend = send(fd, p, iLeft, iFlags);
#else
		iSend = send(fd, p, iLeft, iFlags);
#endif
		if(iSend < 0) {
#ifdef WIN32
			int err = WSAGetLastError();
			if (err == WSAEWOULDBLOCK || err == WSAEINTR) {
				//LOGD("sendData retry iLeft = %d ... \n", iLeft);
				continue;
			}
#elif defined LINUX
			if (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR) {
				//LOGD("sendData retry iLeft = %d ... \n", iLeft);
				continue;
			}
#endif
			else {
				//LOGE("sendData error %s\n", strerror(errno));
				return -1;
			}
		}
		else {
			iLeft -= iSend;
			p +=iSend;
		}
	}

	return iLen;
}

/************************************************************/
/* Other Interface Portion									*/
/************************************************************/

u64 __get_system_mstime(void)
{
	u64 mstime = 0;

#ifdef WIN32
//	LPSYSTEMTIME st;
//	LPFILETIME ft;
	SYSTEMTIME st;
	FILETIME ft;
	u64 temp;

	GetLocalTime(&st);
	SystemTimeToFileTime(&st, &ft);

	mstime = ft.dwLowDateTime;
	temp = ft.dwHighDateTime;
	mstime += temp << 32;
	mstime /= 10 * 1000; //convert 100ns to ms
#elif defined LINUX
	struct timeval curr_time;

	if (gettimeofday(&curr_time, 0) != 0)
	{
		LOGE("ERROR: gettimeofday failed\n");
		return -1;
	}

	mstime = (u64)curr_time.tv_sec * 1000 + curr_time.tv_usec / 1000;
#endif
	return mstime;
}

void __msleep(int msec)
{
#ifdef WIN32
	Sleep(msec);
#elif defined LINUX
	usleep(msec * 1000);
#endif
}
