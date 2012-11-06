#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <windows.h>
#include <winuser.h>

#define MAX_X_ANGLE	90
#define MAX_Y_ANGLE	18

void get_screen_size(int *x, int *y)
{
	*x = GetSystemMetrics(SM_CXSCREEN);
	*y = GetSystemMetrics(SM_CYSCREEN);
}

void get_pointer(int *x, int *y)
{
	POINT pos;
	GetCursorPos(&pos);
	*x = pos.x;
	*y = pos.y;
}

void set_pointer(int x, int y)
{
	SetCursorPos(x, y);
}

void move_pointer(int x, int y)
{
	mouse_event(MOUSEEVENTF_MOVE, x, y, 0, 0);
}

void __translate_pointer(float *x_p, float *y_p, int width, int height, float cal)
{
	float x = *x_p;
	float y = *y_p;

	if(x > 180)
		x = -(360 - x);
	if(cal > 180)
		cal = -(360 - cal);
	x -= cal;
	x = width / 2 + (width * x) / MAX_X_ANGLE;
	if(x < 0)
		x = 0;
	else if(x > width)
		x = width;

	y = height / 2 - (height * y) /MAX_Y_ANGLE;
	if(y < 0)
		y = 0;
	if(y > height)
		y = height;
	*x_p = x;
	*y_p = y;
}

void translate_pointer(float *x, float *y, float cal)
{
	int width, height;
	get_screen_size(&width, &height);
	__translate_pointer(x, y, width, height, cal);
}

void update_sensor(float g_sensor_y, float o_sensor_x, float cal)
{
	float x, y;
	x = g_sensor_y;
	y = o_sensor_x;
	translate_pointer(&x, &y, cal);
	set_pointer(x, y);
}

