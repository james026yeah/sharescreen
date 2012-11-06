#ifndef __ISHARE_PROTOCOL_EXT_H
#define __ISHARE_PROTOCOL_EXT_H

#include "common.h"
#include "protocol.h"
#include "service.h"

extern void get_server_protocol(struct server_protocol *protocol);
extern void set_server_service(struct service_server *server);
extern void get_client_protocol(struct client_protocol *protocol);
extern void set_client_service(struct service_client *client);

#ifdef WIN32
extern int ptl_init(void);
extern int ptl_deinit(void);
extern int ptl_set_client_info(int width, int height, int dpi);
extern int ptl_connect(char *srv_addr, char *name, char *key);
extern int ptl_disconnect(void);
extern int ptl_set_media_type(char type);
extern int ptl_start_media(void);
extern int ptl_stop_media(void);
extern int ptl_send_key(MsgKey_st *key);
extern int ptl_send_mouse(MsgMouse_st *mouse);
extern int ptl_send_touch(MsgMulTouch_st *touch);
extern int ptl_get_protocol_status(void);
extern void ptl_reg_cb_copyimage(int (*copy_image)(const void *, int));
extern void ptl_reg_cb_copyAudio(int (*copy_audio)(const void *, int));
#endif

#endif
