#ifndef _WI_REMOTE_H
#define _WI_REMOTE_H

int init_wiremote(void);
int connect_server(char *ip);
void deinit_wiremote(void);
int send_touch_event(int x, int y, int press);
int send_mouse_move(int x, int y);
int send_mouse_button(int button, int press);
int send_key_event(int code, int press);
int gyro_mouse_cotrol(int enable);
#endif

