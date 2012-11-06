/*
 * =====================================================================================
 *
 *       Filename:  utils.h
 *
 *    Description:
 *
 *        Version:  1.0
 *        Created:  05/18/2012 10:03:02 AM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  cyher (), cyher.net A~T gmail.com
 *        Company:  cyher.net
 *
 * =====================================================================================
 */
#ifndef __UTILS_H__
#define __UTILS_H__

#include <sys/time.h>

#define LOG_DEBUG 1

#if defined(LOG_DEBUG) && LOG_DEBUG == 1

#define LOG_TIME_DEFINE(n) \
    struct timeval time_start_##n, time_stop_##n; unsigned long log_time_##n = 0;

#define LOG_TIME_START(n) \
    gettimeofday(&time_start_##n, NULL);

#define LOG_TIME_END(n) \
    gettimeofday(&time_stop_##n, NULL); log_time_##n = measure_time(&time_start_##n, &time_stop_##n);

#define LOG_TIME(n) \
    log_time_##n

#define LOG_D(...) LOGD(__VA_ARGS__)
#else
#define LOG_TIME_DEFINE(n)
#define LOG_TIME_START(n)
#define LOG_TIME_END(n)
#define LOG_TIME(n)
#define LOG_D(...)
#endif
uint64_t timestamp(void);
unsigned long measure_time(struct timeval *start, struct timeval *stop);
int already_running(void);

#endif
