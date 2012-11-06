/*
 * =====================================================================================
 *
 *       Filename:  comm.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  04/19/2012 02:30:52 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  cyher (), cyher.net A~T gmail.com
 *        Company:  cyher.net
 *
 * =====================================================================================
 */
#ifndef __COMM_H__
#define __COMM_H__
#if 1 //NDK_TEMP
#include <sys/socket.h>
#else
#include <cutils/sockets.h>
#endif

#define V_SOCKET_NAME "ishare:video"
#define A_SOCKET_NAME "ishare:audio"
#define C_SOCKET_NAME "ishare:control"
#define A_CLIENT_SOCKET_NAME "ishare:audio_client"

#define PACKET_MAX 1024

#pragma pack(1)

struct mm_data {
	char type;
#define MM_JPEG 0x01
#define MM_AUDIO 0x02
#define MM_VIDEO (MM_JPEG | MM_AUDIO)
	int lenth;
};

struct jpeg_frame {
	char jpeg_type;
#define JPEG_TYPE_RGB565 0x01
#define JPEG_TYPE_RGB8888 0x02
	int width;
	int height;
	int frame_length;
	int rotation;
	uint64_t timestamp;
	unsigned long encode_time;
};

struct jpeg_fragment {
	int offset;
	int size;
};

struct jpeg_ext {
	int fragment_num;
	struct jpeg_fragment fragment[6];
};

struct audio_info {
	uint64_t timestamp;
	unsigned short channel;
	unsigned short samplerate;
	unsigned short samplebit;
	unsigned int audio_length;
};

#define MAX_MULTOUTH_NUM 10

struct msg_head {
	char type;
#define MSG_CONTROL_HAL	0x01
#define MSG_EVENT_TOUCH	0x02
#define MSG_EVENT_KEY	0x03
#define MSG_EVENT_MOUSE	0x04
	int lenth;
};

struct msg_control {
	char type;
#define MM_JPEG 0x01
#define MM_AUDIO 0x02
#define MM_VIDEO (MM_JPEG | MM_AUDIO)
#define MM_INPUT 0x10
	char status;
#define GRAB_STOP 0x00
#define GRAB_PAUSE 0x01
#define GRAB_START 0x03
#define PC2M 0x1c
#define M2M 0x18
};

struct coord_st {
	int x;
	int y;
};

struct touch_st{
	char type;
	int x;
	int y;
};
struct msg_multouch_st {
#define CLIENT_EVENT_DOWN 0x01
#define CLIENT_EVENT_UP 0x00
	unsigned char num; //coordinate num
	struct touch_st pxy[MAX_MULTOUTH_NUM];
};

struct msg_key_st {
	int type; //0x01 down, 0x00 up
	int code;
};

struct msg_mouse_st {
	char left_btn;
	char mid_btn;
	char right_btn;
	int x;
	int y;
};

#pragma pack()

struct callback_set {
	int (*control_func)(struct msg_control *msg_control);
	int (*touch_func)(struct msg_multouch_st *msg_touch);
	int (*key_func)(struct msg_key_st *msg_touch);
	int (*mouse_func)(struct msg_mouse_st *msg_touch);
};

int send_video(void *buff, size_t len, uint64_t timestamp, int width, int height, struct jpeg_ext *ext);
int recv_video(void *buff, size_t len);
#endif
