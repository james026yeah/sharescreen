#include <string.h>
#include "protocol.h"

void cmd_set_udp_port(struct protocol_handle *handle, struct protocol_event *event, unsigned short port)
{
	event->type = PROTOCOL_CONTROL;
	event->magic = PROTOCOL_MAGIC;
	event->packet.control.cmd = CONTROL_CMD_UDP_PORT;
	event->packet.control.direct = DIRECT_REQUEST;
	event->packet.control.argv.b16[0] = port;
}

void cmd_set_sensor_control(struct protocol_handle *handle, struct protocol_event *event, int sensor, int on)
{
	event->type = PROTOCOL_CONTROL;
	event->magic = PROTOCOL_MAGIC;
	event->packet.control.cmd = CONTROL_CMD_SENSOR;
	event->packet.control.direct = DIRECT_REQUEST;
	event->packet.control.argv.b32[0] = 1;
	event->packet.control.argv.b32[1] = sensor;
	event->packet.control.argv.b32[2] = on;
}

void cmd_set_sensor_delay(struct protocol_handle *handle, struct protocol_event *event, int sensor, int delay)
{
	event->type = PROTOCOL_CONTROL;
	event->magic = PROTOCOL_MAGIC;
	event->packet.control.cmd = CONTROL_CMD_SENSOR;
	event->packet.control.direct = DIRECT_REQUEST;
	event->packet.control.argv.b32[0] = 2;
	event->packet.control.argv.b32[1] = sensor;
	event->packet.control.argv.b32[2] = delay;
}

void data_set_test(struct protocol_handle *handle, struct protocol_event *event, char *test)
{
	event->type = PROTOCOL_TEST;
	event->magic = PROTOCOL_MAGIC;
	strncpy(event->packet.test, test, 30);
}

void data_set_sensor_data(struct protocol_handle *handle, struct protocol_event *event, unsigned int num, struct amt_sensor_data *data)
{
	int i;
	memset(event, 0, sizeof(struct protocol_event));
	event->type = PROTOCOL_SENSOR;
	event->magic = PROTOCOL_MAGIC;
	if(num > MAX_SENSOR_TYPE)
		num = MAX_SENSOR_TYPE;
	for(i = 0; i < num; i++)
	{
		event->packet.sensor[i].sensor_type = data[i].sensor_type;
		event->packet.sensor[i].data[0] = data[i].data[0];
		event->packet.sensor[i].data[1] = data[i].data[1];
		event->packet.sensor[i].data[2] = data[i].data[2];
	}
}

void data_set_mouse_data(struct protocol_handle *handle, struct protocol_event *event, int x, int y, int button, int press)
{
	event->type = PROTOCOL_MOUSE;
	event->magic = PROTOCOL_MAGIC;
	event->packet.mouse.x = x;
	event->packet.mouse.y = y;
	event->packet.mouse.button = button;
	event->packet.mouse.press = press;
}

void data_set_touch_data(struct protocol_handle *handle, struct protocol_event *event, unsigned int num, int *x, int *y, int *press)
{
	int i;
	event->type = PROTOCOL_TOUCH;
	event->magic = PROTOCOL_MAGIC;
	if(num > MAX_MULTI_TOUCH)
		num = MAX_MULTI_TOUCH;
	event->packet.touch.num = num;
	for(i = 0; i < num; i++)
	{
		event->packet.touch.x[i] = x[i];
		event->packet.touch.y[i] = y[i];
		event->packet.touch.press[i] = press[i];
	}
}

void data_set_key_data(struct protocol_handle *handle, struct protocol_event *event, int code, int press)
{
	event->type = PROTOCOL_KEY;
	event->magic = PROTOCOL_MAGIC;
	event->packet.key.keycode = code;
	event->packet.key.keypress = press;
}

static int recv_command(struct protocol_handle *handle, struct control_data *cmd, struct protocol_event *event)
{
	int ret = 0;
	if(cmd->direct == DIRECT_RESPONSE)
	{
		if(handle->cmd_response)
			handle->cmd_response(handle->data, cmd->cmd, cmd->ret);
		return ret;
	}

	switch(cmd->cmd)
	{
		case CONTROL_CMD_UDP_PORT:
			{
				int port = cmd->argv.b16[0];
				if(handle->update_udp_port)
					ret = handle->update_udp_port(handle->data, port);
			}
			break;

		case CONTROL_CMD_SENSOR:
			{
				if(cmd->argv.b32[0] == 1)
				{
					int sensor = cmd->argv.b32[1];
					int on = cmd->argv.b32[2];
					if(handle->sensor_control)
						ret = handle->sensor_control(handle->data, sensor, on);
				}
				else if(cmd->argv.b32[0] == 2)
				{
					int sensor = cmd->argv.b32[1];
					int delay = cmd->argv.b32[2];
					if(handle->sensor_delay)
						ret = handle->sensor_delay(handle->data, sensor, delay);
				}
			}
			break;

		case CONTROL_CMD_LOCAION:
			break;

		case CONTROL_CMD_KEY:
			break;

		case CONTROL_CMD_MOUSE:
			break;

		case CONTROL_CMD_TOUCH:
			break;

		default:
			ret = RETURN_ERROR;
			break;
	}
	cmd->direct = DIRECT_RESPONSE;
	cmd->ret = ret;
	if(cmd->cmd != CONTROL_CMD_UDP_PORT)
	{
		if(handle->cmd_ack)
			handle->cmd_ack(handle->data, event);
	}
	return 0;
}

int recv_packet(struct protocol_handle *handle, struct protocol_event *event)
{
	int ret = 0;
	if(event->magic != PROTOCOL_MAGIC)
	{
		LOGE(handle->log, "%s bad magic number\n", __func__);
		return -1;
	}
	switch(event->type)
	{
		case PROTOCOL_CONTROL:
			ret = recv_command(handle, &event->packet.control, event);
			break;

		case PROTOCOL_TOUCH:
			if(event->packet.touch.num > MAX_MULTI_TOUCH)
				LOGE(handle->log, "%s bad touch number: %d\n", __func__, event->packet.touch.num);
			else if(handle->touch_data)
			{
				unsigned int i;
				int tempx[MAX_MULTI_TOUCH];
				int tempy[MAX_MULTI_TOUCH];
				int temppress[MAX_MULTI_TOUCH];
				for(i = 0; i < event->packet.touch.num; i++)
				{
					tempx[i] = event->packet.touch.x[i];
					tempy[i] = event->packet.touch.y[i];
					temppress[i] = event->packet.touch.press[i];
				}
				ret = handle->touch_data(handle->data, event->packet.touch.num, tempx, tempy, temppress);
			}
			break;

		case PROTOCOL_KEY:
			if(handle->key_data)
				ret = handle->key_data(handle->data, event->packet.key.keycode, event->packet.key.keypress);
			break;

		case PROTOCOL_MOUSE:
				ret = handle->mouse_data(handle->data, event->packet.mouse.x, event->packet.mouse.y,
						event->packet.mouse.button, event->packet.mouse.press);
			break;

		case PROTOCOL_LOCATION:
			break;

		case PROTOCOL_TEST:
			if(handle->update_test)
				ret = handle->update_test(handle->data, event->packet.test);
			break;

		case PROTOCOL_SENSOR:
			{
				unsigned int num = 0;
				struct amt_sensor_data data[MAX_SENSOR_TYPE];
				while(event->packet.sensor[num].sensor_type)
				{
					data[num].sensor_type = event->packet.sensor[num].sensor_type;
					data[num].data[0] = event->packet.sensor[num].data[0];
					data[num].data[1] = event->packet.sensor[num].data[1];
					data[num].data[2] = event->packet.sensor[num].data[2];
					num++;
					if(num >= MAX_SENSOR_TYPE)
						break;
				}
				if(handle->sensor_data)
					ret = handle->sensor_data(handle->data, num, data);
			}
			break;

		default:
			ret = -1;
			break;
	}
	return ret;
}

