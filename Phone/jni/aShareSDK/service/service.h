#ifndef _SERVICE_H
#define _SERVICE_H
#include "protocol.h"

#define RESPONSE_NO_ERROR		0
#define RESPONSE_ERROR_KEY		-1
#define RESPONSE_ERROR_MODE		-2
#define RESPONSE_ERROR_UNKNOWN	-3

enum SERVICE_MODE
{
	SERVICE_SERVER_MODE = 0,
	SERVICE_CLIENT_MODE
};

enum SERVICE_STATUS
{
	STATUS_START = 1,
	STATUS_STOP,
	STATUS_CONNECTED,
	STATUS_DISCONNECTED
};

enum UPNP_STATUS
{
	SERVICE_IDLE = 1,
	SERVICE_CONNECTED
};

#define MAX_NAME_SIZE 32
#define MAX_KEY_SIZE 32

struct ctl_client //array
{
	int id;
	char name[MAX_NAME_SIZE];
};

struct service_control {
	int (*init_service)(void);
	int (*deinit_service)(void);
	int (*start_service)(void);
	int (*stop_service)(void);
	int (*get_service_status)(void);
	int (*set_media_type)(int type);
	int (*get_media_type)(void);
//server mode
	int (*set_rotation)(int rotation);
	int (*set_key)(char *key);
	int (*get_client_list)(struct ctl_client *clients);
	int (*get_client_status)(char client_id);
	int (*disconnect_client)(char *name);
//client mode
	int (*connect_server)(char *ip, char *name, char *key);
	int (*disconnect_server)(void);
	int (*start_media)(void);
	int (*stop_media)(void);
	int (*set_client_info)(int width, int height, int dpi);
	int (*key_input)(MsgKey_st *input);
	int (*touch_input)(MsgMulTouch_st *touch);
	int (*mouse_input)(MsgMouse_st *mouse);
};

struct server_protocol {
	int (*init_protocol)(void);
	int (*deinit_protocol)(void);
	int (*start_protocol)(void);
	int (*stop_protocol)(void);
	int (*control_client)(int client_id, char action, int sock);
	int (*send_raw_data)(int sock, void *buf, int size);
	int (*send_image_data)(int sock, void *buf, int size, int client_id);
	int (*send_audio_data)(int sock, void *buf, int size, int client_id);
};

struct client_protocol {
	int (*init_protocol)(void);
	int (*deinit_protocol)(void);
	int (*set_client_info)(int width, int height, int dpi);
	int (*start_protocol)(char *ip, char *name, char *key);
	int (*stop_protocol)(void);
	int (*setup_media)(char type);
	int (*start_media)(void);
	int (*stop_media)(void);
	int (*key_input)(MsgKey_st *input);
	int (*touch_input)(MsgMulTouch_st *touch);
	int (*mouse_input)(MsgMouse_st *mouse);
};

struct service_server {
	int (*add_client)(char *name, char *key, int sock);
	int (*del_client)(char client_id);
	int (*start_client_transfer)(char client_id);
	int (*stop_client_transfer)(char client_id);
	int (*set_server_key)(char *key);
	int (*set_client_media_type)(char client_id, char type);
	int (*set_client_info)(char client_id, int width, int height, int dpi);
	int (*async_send_control)(char client_id, void *buf, int size);

	void (*client_key_event)(char client_id, MsgKey_st *input);
	void (*client_mouse_event)(char client_id, MsgMouse_st *input);
	void (*client_touch_event)(char client_id, MsgMulTouch_st *input);
};

struct service_client {
	int (*connect_status)(int reason, int status);
	int (*copy_audio)(const void *data, int size);
	int (*copy_image)(const void *data, int size);
};

struct service_manager {
	int current_mode;
	int type;
	int server_enable;
	int status[2];
	struct service_control control[2];
	char name[32];
	int width;
	int height;
	int dpi;

	char uuid[32];
	char xml_path[64];
	char current_ip[32];
	int upnp_started;
	int upnp_status;
	char machine[32];
	int need_key;
};

int service_init(char *uuid, char *upnp_path, char *machine, int version);
int service_deinit(void);
int set_service_name(char *name);
int set_ip(char *ip);
char *get_uuid(void);
int start_service(void);
int stop_service(void);
int enable_server_mode(void);
int disable_server_mode(void);
int get_service_status(void);
int set_media_type(char *type);
int set_key(char *key);
int get_client_list(struct ctl_client *clients);
int get_client_status(char client_id);
int client_connect_server(char *ip, char *key);
int set_client_info(int width, int height, int dpi);
int set_rotation(int rotation);
char *status2string(int status);
void status_changed(char *status);
#endif

