#ifndef _SENSOR_GRAB_H
#define _SENSOR_GRAB_H

#define SENSOR_TYPE_ACCELEROMETER       1
#define SENSOR_TYPE_MAGNETIC_FIELD      2
#define SENSOR_TYPE_ORIENTATION         3
#define SENSOR_TYPE_GYROSCOPE           4
#define SENSOR_TYPE_LIGHT               5
#define SENSOR_TYPE_PRESSURE            6
#define SENSOR_TYPE_TEMPERATURE         7   // deprecated
#define SENSOR_TYPE_PROXIMITY           8
#define SENSOR_TYPE_GRAVITY             9
#define SENSOR_TYPE_LINEAR_ACCELERATION 10
#define SENSOR_TYPE_ROTATION_VECTOR     11
#define SENSOR_TYPE_RELATIVE_HUMIDITY   12
#define SENSOR_TYPE_AMBIENT_TEMPERATURE 13

#pragma pack(1)
struct sensor_event
{
	int type;
	float data[3];
};
#pragma pack()

struct sensor_grab_callback
{
	void (*sensor_data_cb)(struct sensor_event *event, int count);
};

int sensor_grab_init(struct sensor_grab_callback *callback);
void sensor_grab_deinit(void);
int sensor_grab_enable(int sensor);
int sensor_grab_disable(int sensor);
int sensor_grab_set_delay(int sensor, int delay);

#endif

