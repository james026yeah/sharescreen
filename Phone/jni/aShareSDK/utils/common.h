#ifndef __ISHARE_COMMON_H_
#define __ISHARE_COMMON_H_

/***********************************/
/* Configuration overall           */
/***********************************/
#ifndef WIN32
#ifndef LINUX
#define LINUX
#endif
#endif

#ifdef LINUX
#ifndef __ANDROID__
#define __ANDROID__
#endif
#endif

/***********************************/
/* Debug Configuration             */
/***********************************/
#define _ISHARE_LOG_V
#define _ISHARE_LOG_D
#define _ISHARE_LOG_I
#define _ISHARE_LOG_W
#define _ISHARE_LOG_E

#define DEBUG_TIME_POINT 0

#ifdef _ISHARE_LOG_D
#define DEBUG_DELAY 2
#else
#define DEBUG_DELAY 1
#endif

/***********************************/
/* Socket definition               */
/***********************************/
typedef int SOCKLEN_t;
#ifdef WIN32
#include <Windows.h>
#include <WinSock2.h>
#pragma comment(lib,"ws2_32.lib")
typedef SOCKET SOCKET_t;
typedef SOCKADDR_IN SOCKADDR_IN_t;
typedef u_long IN_ADDR_T_t;
#elif defined LINUX
#include <arpa/inet.h>
typedef int SOCKET_t;
typedef struct sockaddr_in SOCKADDR_IN_t;
typedef in_addr_t IN_ADDR_T_t;
#endif

#define SOCK_SHUT_RD 0
#define SOCK_SHUT_WR 1
#define SOCK_SHUT_RDWR 2

/***********************************/
/* Mutex definition            	   */
/***********************************/
#ifdef WIN32
#define MUTEX_t CRITICAL_SECTION
#define MUTEX_INIT(lock) InitializeCriticalSection(lock)
#define MUTEX_DEL(lock) DeleteCriticalSection(lock)
#define MUTEX_LOCK(lock) EnterCriticalSection(lock)
#define MUTEX_UNLOCK(lock) LeaveCriticalSection(lock)
#elif defined LINUX
#include <pthread.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <errno.h>
#define MUTEX_t pthread_mutex_t
#define MUTEX_INIT(lock) pthread_mutex_init(lock, NULL)
#define MUTEX_DEL(lock) pthread_mutex_destroy(lock)
#define MUTEX_LOCK(lock) pthread_mutex_lock(lock)
#define MUTEX_UNLOCK(lock) pthread_mutex_unlock(lock)
#endif

/***********************************/
/* Thread sync definition          */
/***********************************/
#ifdef WIN32
typedef HANDLE EVENT_t;
typedef HANDLE PHANDLE_t;
typedef LPTHREAD_START_ROUTINE THREAD_FUNC;
#elif defined LINUX
typedef pthread_cond_t EVENT_t;
typedef pthread_t PHANDLE_t;
typedef void * (* THREAD_FUNC)(void *);
#endif
/***********************************/
/* Return value definition         */
/***********************************/
#define ESuccess 0
#define EFailed -1

/***********************************/
/* Date type definition            */
/***********************************/
typedef unsigned long long u64;
#if 0
typedef unsigned int u32;
typedef unsigned short u16;
typedef unsigned char u8;
typedef char b8;

typedef signed long long s64;
typedef signed int s32;
typedef signed short s16;
typedef signed char s8;
#endif

#ifdef __ANDROID__
#ifdef NDK
#include <android/log.h>
#else
#include <utils/Log.h>
#endif

#undef LOG_TAG
#define LOG_TAG "ishare"

#ifdef _ISHARE_LOG_V
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG , __VA_ARGS__)
#else
#define LOGV(...)
#endif

#ifdef _ISHARE_LOG_D
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)
#else
#define LOGD(...)
#endif

#ifdef _ISHARE_LOG_I
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  ,  LOG_TAG , __VA_ARGS__)
#else
#define LOGI(...)
#endif

#ifdef _ISHARE_LOG_W
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  ,  LOG_TAG , __VA_ARGS__)
#else
#define LOGW(...)
#endif

#ifdef _ISHARE_LOG_E
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG , __VA_ARGS__)
#else
#define LOGE(...)
#endif

#elif defined WIN32

#define LOGI(...)
#define LOGV(...)
#define LOGD(...)
#define LOGW(...)
#define LOGE(...)
#endif

#ifndef __func__
#define __func__ __FUNCTION__
#endif

#endif //__ISHARE_COMMON_H_
