#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <sys/time.h>
#include <signal.h>
#include <time.h>
#include <android/log.h>
#include "wi_remote.h"

#undef LOG_TAG
#define LOG_TAG "TP2ALL"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)

#define LEFT_CLICK_TIME	200
#define LEFT_PRESS_TIME	300
#define MAX_RANGE	20

#define MOUSE_BUTTON_LEFT	1
static int finger_count;
static uint64_t last_time;
static int last_x, last_y;
static int left_button = 0;
static int left_x, left_y;
static int left_out_range;
static int finger2_status = 0;
struct itimerval value;
int g_action = 0;

static void init_timer_trigger(void);
static void deinit_timer_trigger(void);

static uint64_t timestamp()
{
	struct timeval time;
	uint64_t timestamp;
	gettimeofday(&time, NULL);
	timestamp = (uint64_t)time.tv_sec * 1000 + time.tv_usec / 1000;
	return timestamp;
}

static int timediff()
{
	uint64_t time = timestamp();
	return time - last_time;
}

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

void timer_trigger(int signum)
{
	LOGD("%s at %d line\n", __func__, __LINE__);
	if(g_action == 0)
	{
		LOGD("long press left!\n");
		left_button = 1;
		send_mouse_button(MOUSE_BUTTON_LEFT, 1);
		send_mouse_move(1, 1);
	}
	deinit_timer_trigger();
}

static void init_timer_trigger(void)
{
	signal(SIGALRM, timer_trigger);

	value.it_value.tv_sec = 0;
	value.it_value.tv_usec = 200000;
	value.it_interval.tv_sec = 0;
	value.it_interval.tv_usec = 0;

	setitimer(ITIMER_REAL, &value, NULL);
}

static void deinit_timer_trigger(void)
{
	value.it_value.tv_sec = 0;
	value.it_value.tv_usec = 0;
	value.it_interval.tv_sec = 0;
	value.it_interval.tv_usec = 0;
}


void tp2all(int x1, int y1, int x2, int y2, int action)
{
	g_action = action;
	action = update_state(action);

	switch(action)
	{
		case 0:
			init_timer_trigger();
			if(finger_count == 1)
			{
				last_time = timestamp();
				left_button = 0;
				left_x = x1;
				left_y = y1;
				left_out_range = 0;
			}
			break;

		case 1:
			if(finger_count == 0)
			{
				if(timediff() < LEFT_CLICK_TIME)
				{
					send_mouse_button(MOUSE_BUTTON_LEFT, 1);
					send_mouse_button(MOUSE_BUTTON_LEFT, 0);
					LOGD("left click\n");
				}
				else if(left_button)
					send_mouse_button(MOUSE_BUTTON_LEFT, 0);
			}
			if(finger2_status && (finger_count != 2))
			{
				finger2_status = 0;
				send_touch_event(x1, y1, 0);
			}
			break;

		case 2:
			if(finger_count == 1 || left_button)
			{
				int rel_x, rel_y;
				rel_x = x1 - last_x;
				rel_y = y1 - last_y;
				if((!left_button) && (!left_out_range))
				{
					if(timediff() < LEFT_PRESS_TIME)
					{
						int xdiff = (x1 < left_x) ? left_x - x1: x1 - left_x;
						int ydiff = (y1 < left_y) ? left_y - y1: y1 - left_y;
						if((xdiff > MAX_RANGE) || (ydiff > MAX_RANGE))
							left_out_range = 1;
					}
					else if(!left_out_range)
					{
						left_button = 1;
						send_mouse_button(MOUSE_BUTTON_LEFT, 1);
					}
				}
				if(left_button)
					LOGD("mouse move with left button\n");
				else
					LOGD("mouse move\n");
				send_mouse_move(rel_x, rel_y);
			}
			else if(finger_count == 2)
			{
				finger2_status = 1;
				send_touch_event((x1+x2)/2, (y1+y2)/2, 1);
			}
			break;

		default:
			break;
	}
	if(finger_count)
	{
		last_x = x1;
		last_y = y1;
	}
}

