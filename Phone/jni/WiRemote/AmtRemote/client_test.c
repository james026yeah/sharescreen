#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "amt_remote.h"

void log_cb(int tag, const char *log)
{
	printf("%s", log);
}

int main(void)
{
	struct amt_handle *handle;
	struct amt_client_callback cb;
	memset(&cb, 0, sizeof(struct amt_client_callback));
	cb.log_cb = log_cb;
	handle = init_client_sock(&cb);
	control_client_log(handle, CB_LOGA);
	connect_client2server(handle, "127.0.0.1", SERVER_PORT);
	while(1)
	{
		int x, y, z;
		usleep(900000);
		x = 111;
		y = 222;
		z = 333;
		data_client_send_test_udp(handle, "123456789");
		mouse_client_send_data(handle, x, y, z, 1);
		touch_client_send_data(handle, 1, &x, &y, &z);
		key_client_send_data(handle, y, 0);
	}
	return 0;
}

