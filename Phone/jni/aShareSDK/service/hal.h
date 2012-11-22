#ifndef _HAL_H
#define _HAL_H
#include "comm.h"
#include "protocol.h"

ssize_t writex(int fd, const void *buff, size_t len);
ssize_t readx(int fd, void *buff, size_t len);
int init_hal_socket(struct ishare_service *service);
void deinit_hal_socket(struct ishare_service *service);
int hal_send_control(char type, char status);
int hal_send_control_input(char type, char status);
int hal_send_event_key(MsgKey_st *input);
int hal_send_event_mouse(MsgMouse_st *input);
int hal_send_event_touch(MsgMulTouch_st *input);
void *grab_thread(void *arg);
#endif

