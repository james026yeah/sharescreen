#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <android/log.h>
#include "wi_remote.h"

#undef LOG_TAG
#define LOG_TAG "TOUCH_HANDLE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)

static int finger_count;
static int move_flag = 0;

static int update_state(int action)
{
	if(action == 0)
		finger_count = 0;
	action &= 0xff;
	if(action >= 5)
		action -= 5;

	switch(action)
	{
		case 0:
			finger_count++;
			break;
		case 1:
			finger_count--;
			break;
		case 2:
		default:
			break;
	}
	return action;
}

void touch_handle(int x1, int y1, int x2, int y2, int action)
{
	int tempx[2], tempy[2],temp_action, press[2];
	tempx[0] = x1;
	tempx[1] = x2;
	tempy[0] = y1;
	tempy[1] = y2;

	temp_action = action;
	LOGD("temp_action = %d at %d line\n", temp_action , __LINE__);
	action = update_state(action);
	LOGD("action = %d at %d line\n", action, __LINE__);

	switch(action)
	{
		case 0:
			if(finger_count == 1)
			{
				press[0] = 1;
				press[1] = 0;
				send_multi_touch_event(tempx, tempy, press);
			}
			else if(finger_count == 1) {
				press[0] = 1;
				press[1] = 1;
				send_multi_touch_event(tempx, tempy, press);
			}

			break;
		case 1:
			if(finger_count == 1 && temp_action == 262)   // the second finger up
			{
				press[0] = 1;
				press[1] = 0;
				send_multi_touch_event(tempx, tempy, press);
			}
			else if(finger_count == 1 && temp_action == 6)   // the first finger up
			{
				press[0] = 0;
				press[1] = 1;
				send_multi_touch_event(tempx, tempy, press);
			}
			else {
				press[0] = 0;
				press[1] = 0;
				send_multi_touch_event(tempx, tempy, press);
			}
			break;

		case 2:
			press[0] = 1;
			press[1] = 1;
			send_multi_touch_event(tempx, tempy, press);
			break;

		default:
			break;
	}
}

