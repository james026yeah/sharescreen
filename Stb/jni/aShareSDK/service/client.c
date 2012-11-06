#include "client.h"
#include "service.h"
#include "buffer_client.h"
#include "protocol_client.h"
#include "native_surface.h"
#include "surface.h"

struct client_protocol g_protocol;

static int init_client(void)
{
	init_buffer_client();
	//g_protocol.init_protocol();
	/* MAN Jerry: post to protocol_client.c */
	return 1;
}

static int deinit_client(void)
{
	g_protocol.deinit_protocol();
	return 1;
}

static int stop_client(void)
{
	g_protocol.stop_protocol();
	return 1;
}

//Temp abort procedure
int stop_client_dbg(void)
{
	g_protocol.stop_protocol();
	return 1;
}

static int __get_client_status(void)
{
	return 1;
}

static int set_client_media_type(int type)
{
	g_protocol.setup_media(type);
	return 1;
}

static int get_client_media_type(void)
{
	return 1;
}

static int __client_connect_server(char *ip, char *name, char *key)
{
	return g_protocol.start_protocol(ip, name, key);
}

static int __client_disconnect_server(void)
{
	g_protocol.stop_protocol();
	return 1;
}

static int __start_media(void)
{
	g_protocol.start_media();
	return 1;
}

static int __stop_media(void)
{
	g_protocol.stop_media();
	return 1;
}

static int __client_set_client_info(int width, int height, int dpi)
{
	g_protocol.set_client_info(width, height, dpi);
	return 1;
}

static int copy_image(const void *data, int size)
{
	set_image_buffer(data, size);
	return 1;
}

int start_client_dummy(void)
{
	return 0;
}

void set_client_control(struct service_control *control)
{
	control->init_service = init_client;
	control->deinit_service = deinit_client;
	control->start_service = start_client_dummy;
	control->stop_service = stop_client;
	control->get_service_status = __get_client_status;
	control->set_media_type = set_client_media_type;
	control->get_media_type = get_client_media_type;
	control->connect_server = __client_connect_server;
	control->disconnect_server = __client_disconnect_server;
	control->start_media = __start_media;
	control->stop_media = __stop_media;
	control->set_client_info = __client_set_client_info;
}

void set_client_protocol(struct client_protocol *protocol)
{
	memcpy(&g_protocol, protocol, sizeof(struct client_protocol));
}

void get_client_service(struct service_client *client)
{
	client->copy_image = copy_image;
}
