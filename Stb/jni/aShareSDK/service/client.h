#ifndef _CLIENT_H
#define _CLIENT_H
#include "service.h"
void set_client_control(struct service_control *control);
void set_client_protocol(struct client_protocol *protocol);
void get_client_service(struct service_client *client);
#endif

