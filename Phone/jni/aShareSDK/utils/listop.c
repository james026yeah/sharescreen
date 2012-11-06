#include "listop.h"

void init_service_list(struct ishare_service *service)
{
	int i;
	for(i = 0; i < MSG_MAX; i++)
	{
		INIT_LIST_HEAD(&service->list_msg[i]);
	}
	INIT_LIST_HEAD(&service->clients);
}

struct ishare_client *find_client_from_list(struct ishare_service *service, char id, char *name)
{
	struct ishare_client *pos;
	list_for_each_entry(pos, &service->clients, list)
	{
		if(((signed char)id != -1) && (pos->id != id))
			continue;
		if(name)
			if(strcmp(name, pos->name) != 0)
				continue;
		return pos;
	 }
	return NULL;
}

void add_client_list(struct ishare_service *service, struct ishare_client *client)
{
	int i;
	list_add_tail(&client->list, &service->clients);
	for(i = 0; i < MSG_MAX; i++)
	{
		INIT_LIST_HEAD(&client->msg_send[i]);
		INIT_LIST_HEAD(&client->msg_peer[i]);
	}
}

void del_client_list(struct ishare_service *service, struct ishare_client *client, struct list_head *del)
{
	int i, found;
	struct send_msg *msg, *msg_next;
	INIT_LIST_HEAD(del);

	for(i = 0; i < MSG_MAX; i++)
	{
		switch(i)
		{
			case MSG_CTRL:
				list_for_each_entry_safe(msg, msg_next, &client->msg_peer[i], peer)
				{
					del_msg_list_client(service, client, msg);
					list_add_tail(&msg->sendlist, del);
				}
				break;
			case MSG_AUDIO:
			case MSG_IMAGE:
				if(list_empty(&client->msg_send[i]))
					break;
				found = 0;
				list_for_each_entry_safe(msg, msg_next, &service->list_msg[i], sendlist)
				{
					if(!found)
					{
						struct ishare_client *pos;
						list_for_each_entry(pos, &msg->clients, msg_send[i])
						{
							if(client->id == pos->id)
							{
								found = 1;
								break;
							}
						}
					}
					if(found)
					{
						del_msg_list_client(service, client, msg);
						if(list_empty(&msg->sendlist))
							list_add_tail(&msg->sendlist, del);
					}
				}
				break;
			default:
				break;
		}
	}
	list_del(&client->list);
}

static int match_msg(int msg_type, int client_type)
{
	client_type &= SRV_TYPE_IMAGE_AUDIO;
	if(msg_type == MSG_AUDIO)
	{
		if((client_type == SRV_TYPE_AUDIO) || (client_type == SRV_TYPE_IMAGE_AUDIO))
			return 1;
	}
	else if(msg_type == MSG_IMAGE)
	{
		if((client_type == SRV_TYPE_IMAGE) || (client_type == SRV_TYPE_IMAGE_AUDIO))
			return 1;
	}
	return 0;
}

int add_msg_list(struct ishare_service *service, struct ishare_client *client, struct send_msg *msg)
{
	int ret = 0;
	struct ishare_client *pos;
	switch(msg->type)
	{
		case MSG_CTRL:
			ret = 1;
			if(list_empty(&client->msg_peer[msg->type]))
				list_add_tail(&client->msg_send[msg->type], &msg->clients);
			list_add_tail(&msg->peer, &client->msg_peer[msg->type]);
			break;
		case MSG_AUDIO:
		case MSG_IMAGE:
			list_for_each_entry(pos, &service->clients, list)
			{
				if(!match_msg(msg->type, pos->mediatype))
					continue;
				ret = 1;
				if(list_empty(&pos->msg_send[msg->type]))
					list_add_tail(&pos->msg_send[msg->type], &msg->clients);
			}
			break;
		default:
			break;
	}
	if(ret)
		list_add_tail(&msg->sendlist, &service->list_msg[msg->type]);

	return ret;
}

void del_msg_list_client(struct ishare_service *service, struct ishare_client *client, struct send_msg *msg)
{
	struct send_msg *next;
	int last;
	list_del_init(&client->msg_send[msg->type]);
	switch(msg->type)
	{
		case MSG_CTRL:
			last = list_is_last(&msg->peer, &client->msg_peer[msg->type]);
			if(!last)
			{
				next = list_entry(msg->peer.next, typeof(*msg), peer);
				list_add_tail(&client->msg_send[msg->type], &next->clients);
			}
			list_del(&msg->peer);
			list_del_init(&msg->sendlist);
			break;
		case MSG_AUDIO:
		case MSG_IMAGE:
			last = list_is_last(&msg->sendlist, &service->list_msg[msg->type]);
			if(!last)
			{
				next = list_entry(msg->sendlist.next, typeof(*msg), sendlist);
				list_add_tail(&client->msg_send[msg->type], &next->clients);
			}
			if(list_empty(&msg->clients) && list_is_first(&msg->sendlist, &service->list_msg[msg->type]))
				list_del_init(&msg->sendlist);
			break;
		default:
			break;
	}
}

void del_msg_list(struct ishare_service *service, struct send_msg *msg)
{
	struct ishare_client *client, *client_next;
	list_for_each_entry_safe(client, client_next, &msg->clients, msg_send[msg->type])
	{
		del_msg_list_client(service, client, msg);
	}
}

struct send_msg *get_first_msg(struct ishare_service *service, int type)
{
	struct send_msg *msg;
	msg = list_entry(service->list_msg[type].next, typeof(*msg), sendlist);
	return msg;
}

struct ishare_client *get_first_client(struct ishare_service *service)
{
	struct ishare_client *client;
	client = list_entry(service->clients.next, typeof(*client), list);
	return client;
}

