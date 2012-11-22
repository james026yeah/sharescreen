#ifndef _LISTOP_H
#define _LISTOP_H
#include "server.h"

void init_service_list(struct ishare_service *service);
struct ishare_client *find_client_from_list(struct ishare_service *service, char id, char *name);
void add_client_list(struct ishare_service *service, struct ishare_client *client);
void del_client_list(struct ishare_service *service, struct ishare_client *client, struct list_head *del);
int add_msg_list(struct ishare_service *service, struct ishare_client *client, struct send_msg *msg);
void del_msg_list_client(struct ishare_service *service, struct ishare_client *client, struct send_msg *msg);
void del_msg_list(struct ishare_service *service, struct send_msg *msg);
struct send_msg *get_first_msg(struct ishare_service *service, int type);
struct ishare_client *get_first_client(struct ishare_service *service);

#endif

