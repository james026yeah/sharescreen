#include <stdlib.h>
#include "service.h"
#include "server.h"
#include "client.h"
#include "protocol_ext.h"
//#include "../native_surface/native_surface.h"
//#include "surface.h"

#undef LOG_TAG
#define LOG_TAG "ishare_service"

static struct service_manager g_manager;
int client_connect_server(char *ip, char *key);

extern int stop_client_dbg(void);
static void exit_service(void)
{
	int i;
	LOGI("exit function: %s\n", __func__);
	for(i = 0; i < 1 /*ONLY SERVER MODE*/; i++)
	{
		LOGD("i = %d", i);
		//HERE SHOULD BE THOUGHT MORE DEEPLY
		if (g_manager.control[i].stop_service)
			g_manager.control[i].stop_service();
		else
			LOGE("ERROR: stop_service is NULL!!!!!!!!\n");
		if (g_manager.control[i].deinit_service)
			g_manager.control[i].deinit_service();
		else
			LOGE("ERROR: deinit_service is NULL!!!!!!!!!\n");
	}
	//deinit_native_surface();
}
int start_server(void)
{
	int ret = 0;
	struct server_protocol protocol_s;
	struct service_server server;

	LOGD("IN %s\n", __func__);
	set_server_control(&g_manager.control[SERVICE_SERVER_MODE]);
	get_server_protocol(&protocol_s);
	set_server_protocol(&protocol_s);
	get_server_service(&server);
	set_server_service(&server);

	g_manager.type = 0;
	g_manager.server_enable = 1;
	g_manager.status[SERVICE_SERVER_MODE] = STATUS_STOP;
	g_manager.control[SERVICE_SERVER_MODE].init_service();
	g_manager.need_key = 0;

	ret = g_manager.control[SERVICE_SERVER_MODE].start_service();
	return ret;
}

int stop_server(void)
{
	exit_service();
	return 0;
}

extern int init_listen_thread(char *srv_addr, char *name, char *key);
int start_client_external(void)
{
	//return init_listen_thread("10.11.18.141", "BF", "NOTHING");
	return init_listen_thread("192.168.1.1", "BF", "NOTHING");
}

int start_client_1(char *ip)
{
/* NOTE JC_0024_1
 * Jerry:
 * HoHa, why do I split start_client into 1 and 2?
 * En, maybe I'm tired execrably, HTC human-outsourcing?
 * Now, I'm writing rather than coding, am I?
 * Oct29, 2012,
 * My God, tommorrow perhaps is the last day for THE EARTH.
 * Breath deeply and happlily.
 * My love daugther and honey!
 * BTW, one more cholocate on codeing next
 * If you see this comments, that's say I have gone from DR building....
 */
	int ret = 0;
	struct client_protocol protocol_c;
	struct service_client client;

	set_client_control(&g_manager.control[SERVICE_CLIENT_MODE]);
	get_client_protocol(&protocol_c);
	set_client_protocol(&protocol_c);
	get_client_service(&client);
	set_client_service(&client);

	g_manager.control[SERVICE_CLIENT_MODE].init_service();
	g_manager.current_mode = SERVICE_CLIENT_MODE;
	g_manager.status[SERVICE_CLIENT_MODE] = STATUS_STOP;

	//client_connect_server(ip, "KEY");
	return ret;
}

int client_connect_server(char *ip, char *key)
{
	int ret = 0;

	LOGD("%s: ip = %s\n", __func__, ip);
	//start_service();
	g_manager.control[SERVICE_CLIENT_MODE].set_client_info(g_manager.width, g_manager.height, g_manager.dpi);
	g_manager.type = SRV_TYPE_IMAGE;
	g_manager.control[SERVICE_CLIENT_MODE].set_media_type(g_manager.type);
	ret = g_manager.control[SERVICE_CLIENT_MODE].connect_server(ip, g_manager.name, key);
	if(ret < 0)
	{
		LOGE("%s: connect server failure!\n", __func__);
		return -1;
	}
	ret = g_manager.control[SERVICE_CLIENT_MODE].start_media();
	if(ret < 0)
	{
		//disconnect_peer(ip);
		LOGE("%s: start media failure!\n", __func__);
		return -1;
	}

	return ret;
}
int stop_client(void)
{
	stop_client_dbg();
	return 0;
}

int set_media_type(char *type)
{
	int mediatype;
	LOGD("%s: %s\n", __func__, type);
	if(strcmp(type, "audio") == 0)
		mediatype = SRV_TYPE_AUDIO;
	else if(strcmp(type, "media") == 0)
		mediatype = SRV_TYPE_IMAGE;
	else
		mediatype = SRV_TYPE_IMAGE_AUDIO;
	g_manager.type &= ~SRV_TYPE_IMAGE_AUDIO;
	g_manager.type |= mediatype;
	if(g_manager.current_mode == SERVICE_SERVER_MODE)
		g_manager.control[SERVICE_SERVER_MODE].set_media_type(g_manager.type);
	return RESPONSE_NO_ERROR;
}

int set_key(char *key)
{
	if(strcmp(key, "") == 0)
		g_manager.need_key = 0;
	else
		g_manager.need_key = 1;
	return g_manager.control[SERVICE_SERVER_MODE].set_key(key);
}

int get_client_list(struct ctl_client *clients)
{
	if(g_manager.current_mode != SERVICE_SERVER_MODE)
		return RESPONSE_ERROR_MODE;
	return g_manager.control[SERVICE_SERVER_MODE].get_client_list(clients);
}

int set_rotation(int rotation)
{
	if(g_manager.current_mode != SERVICE_SERVER_MODE)
		return RESPONSE_ERROR_MODE;
	return g_manager.control[SERVICE_SERVER_MODE].set_rotation(rotation);
}

void status_changed(char *status)
{
	int ret;
	char command[128];
	if(g_manager.current_mode == SERVICE_SERVER_MODE)
	{
		ret = g_manager.control[SERVICE_SERVER_MODE].get_client_list(NULL);
		if(!ret)
		{
			sprintf(command, "status:disconnect:%s", status);
		}
		else
		{
			sprintf(command, "status:connect:%s", status);
		}
	}
	else if(g_manager.current_mode == SERVICE_CLIENT_MODE)
	{
		if(strcmp(status, "disconnect") == 0)
		{
			sprintf(command, "status:disconnect:myself");
		}
	}
}

char *get_machine(void)
{
	return g_manager.machine;
}

