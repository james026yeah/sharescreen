#include "protocol.h"

/* list deque module start */
int init_deque(struct item ** hdr)
{
	if (!hdr)
		return ERR_HEADER_NULL;

	*hdr = NULL;
	return ERR_NULL;
}

int deque_is_empty(struct item * hdr)
{
	return hdr == NULL;
}

int deque_number(struct item *hdr)
{
	int num = 0;
	struct item *p = hdr;

	if (!hdr) return 0;
	do
	{
		p = p->next;
		num++;
	} while (p->next != hdr);

	return num;
}

int push_deque(struct item ** hdr, void * data, int size)
{
	struct item *new_item, *p;

	new_item = (struct item *)malloc(sizeof(struct item));
	if (!new_item)
		return ERR_NO_MEMORY;

	new_item->data = data;
	new_item->size = size;
	new_item->prev = new_item;
	new_item->next = new_item;

	p = *hdr;
	if (!p) {
		p = new_item;
		*hdr = p;
	}
	else {
		new_item->prev = p->prev;
		new_item->next = p;
		p->prev->next = new_item;
		p->prev = new_item;
	}

	return ERR_NULL;
}

int pop_deque(struct item ** hdr, void ** data, int *size)
{
	struct item *p;

	p = *hdr;

	if (!p)
		return ERR_QUEUE_EMTPY;

	*data = p->data;
	*size = p->size;

	if (p == p->next)
	{
		*hdr = NULL;
	}
	else
	{
		p->prev->next = p->next;
		p->next->prev = p->prev;
		*hdr = p->next;
	}

	free(p);
	return ERR_NULL;
}
