#ifndef _ISHARE_PROTOCOL_H_
#define _ISHARE_PROTOCOL_H_

#include "common.h"

#ifdef LINUX
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <errno.h>
#include <unistd.h>
#include "list.h"
#include <pthread.h>
#include <fcntl.h>
#endif

/***************************************************************************/
/* list deque module start                                                 */
/***************************************************************************/
#define ERR_NULL 0
#define ERR_NO_MEMORY -1
#define ERR_QUEUE_EMTPY -2
#define ERR_HEADER_NULL -3

struct item
{
	void * data;
	int size;
	struct item *prev;
	struct item *next;
};

extern int init_deque(struct item ** hdr);
extern int deque_is_empty(struct item * hdr);
extern int deque_number(struct item *hdr);
extern int push_deque(struct item ** hdr, void * data, int size);
extern int pop_deque(struct item ** hdr, void ** data, int *size);
/***************************************************************************/
/* Protocol data definition                                               */
/***************************************************************************/

#define ISHARE_PROTOCOL_VERSION "0.4.1"

#define	ISHARE_SERVER_MODE	1
#define	ISHARE_CLIENT_MODE	2

#define MAX_IMAGE_SIZE 1024*300
#define MAX_AUDIO_SIZE 1024*100

/*client*/
#define MAX_SEND_SIZE 1024
/*server*/
#define MAX_RECV_SIZE 1024

#define MAX_SEND_MTU 1024
#define SOCK_LISTEN_PORT 11820
#define SOCK_PRE_LISTEN_PORT (SOCK_LISTEN_PORT + 1)
#define SOCK_COMM_PORT (SOCK_LISTEN_PORT + 2)
#define SOCK_BACKLOG 5
#define SOCK_NUM_MAX 256
#define MAX_IPV4_SIZE 15
#define MAX_COMMON_PKT_SIZE 128
#define MAX_KEY_CODE_SIZE 32
#define MAX_CLIENT_NAME_SIZE 32
#define MAX_SERVER_NAME_SIZE MAX_CLIENT_NAME_SIZE
#define MAX_MULTOUTH_NUM 10

#define MATCH_TIME_LIMIT_MS 5 * DEBUG_DELAY
#define MATCH_TIME_RES_TIMEOUT 1000
#define COMMON_PKT_RES_TIMEOUT 1000
#define THREAD_INIT_TIMEOUT 5000

enum ECtlClient
{
	CTL_CLIENT_REMOVE = 0x01,
	CTL_CLINET_MAX
};

enum EThreadHandle
{
	THREAD_RECV = 0x00,
	THREAD_SEND = 0x01,
	THREAD_COUNT
};

/*Server-client packet type definition*/
enum EPacketType
{
	MSG_CLIENT_INFO = 1,
	MSG_TIME_SYNC = 2,
	MSG_RMT_INPUT = 3,
	MSG_SETUP = 4,
	MSG_CONTROL = 5,
	MSG_IMAGE_INFO = 6, //obsolete
	MSG_IMAGE_DATA = 7,
	MSG_AUDIO_DATA = 8,
	MSG_CLIENT_REPORT = 9, //reserved
	MSG_QWERTY_INFO = 10, //obsolete
	MSG_SERVER_CMD = 11,
	MSG_CLIENT_SIG = 12,
	MSG_REMOTE_COMM = 0x20, //only for comm thread
	MSG_COMM_MAX
};

enum EServerCmdType
{
	CMD_TYPE_RELEASE = 1,
};

enum EClientSigType
{
	SIG_TYPE_RELEASE = 1,
};

enum EImageType
{
	IMAGE_TYPE_JPEG = 0x01,
	IMAGE_TYPE_H264
};

enum EAudioType
{
	AUDIO_TYPE_PCM = 0x01,
	AUDIO_TYPE_MP3
};

enum ERmtInputType
{
	RMT_INPUT_KEY = 1,
	RMT_INPUT_MOUSE,
	RMT_INPUT_MULTOUCH
};


enum ESrvType
{
	SRV_TYPE_NULL = 0x00,
	SRV_TYPE_IMAGE = 0x01,
	SRV_TYPE_AUDIO = 0x02,
	SRV_TYPE_IMAGE_AUDIO = SRV_TYPE_IMAGE | SRV_TYPE_AUDIO,
	SRV_TYPE_KEY = 0x04,
	SRV_TYPE_MOUSE = 0x08,
	SRV_TYPE_TOUCH = 0x10,
	SRV_TYPE_QWERTY = 0x20,
	SRV_TYPE_INPUT = SRV_TYPE_KEY | SRV_TYPE_MOUSE | SRV_TYPE_TOUCH | SRV_TYPE_QWERTY,
	SRV_TYPE_MAX = SRV_TYPE_IMAGE_AUDIO | SRV_TYPE_INPUT
};

enum ESrvCtl
{
	CTL_START = 0x01,
	CTL_STOP
};

enum ERESULT
{
	RES_FAILED = 0x00,
	RES_SUCCESS
};

/*InputType conclude key, button and touch*/
enum EInputType
{
	RMT_INPUT_UP = 0x00,
	RMT_INPUT_DOWN = 0x01,
	RMT_INPUT_NULL = 0xFF
};

#pragma pack(1)

/* Server-client packet type definition*/
/*Packet header definition*/
typedef struct{
	char type;
	char sub_type;
	int len;
	char client_id;
}PktHdr_st;

typedef struct{
	PktHdr_st hdr;
	u64 server_time;
}TimeRes_st;

typedef struct{
	PktHdr_st hdr;
//NOTE: use client_id as timestamp
}TimeReq_st;

typedef struct{
	PktHdr_st hdr;
	char srv_type;
}SetupReq_st;

#define SETUP_FAILED 0x00
#define SETUP_SUCCESS 0x01
typedef struct{
	PktHdr_st hdr;
	char setup_ret;
	char err_code;
}SetupRes_st;

typedef struct{
	PktHdr_st hdr;
}CtlReq_st;

typedef struct{
	PktHdr_st hdr;
	char ctl_ret;
	char err_code;
}CtlRes_st;

typedef struct{
	int x;
	int y;
}coord_st;

typedef struct{
	char type;
	int x;
	int y;
}touch_st;

typedef struct{
	char num;
	touch_st pXY[MAX_MULTOUTH_NUM];
}MsgMulTouch_st;

typedef struct{
	int type;
	int code;
}MsgKey_st;

typedef struct{
	char left_btn;
	char mid_btn;
	char right_btn;
	coord_st pXY;
}MsgMouse_st;

typedef struct{
	char key_len;
	char key[MAX_KEY_CODE_SIZE + 1];
	char name_len;
	char name[MAX_CLIENT_NAME_SIZE + 1];
	int width;
	int height;
	int dpi;
//TODO add protocol version
//  char version[8];
}ClientInfo_st;

typedef struct{
	PktHdr_st hdr;
	ClientInfo_st info;
}ClientInfoReq_st;

typedef struct{
	PktHdr_st hdr;
	char keyVery;
#define KEY_ILLEGAL 0x00
#define KEY_APPROVED 0x01
	char reason;
#define KEY_NO_ERR 0
#define KEY_EMPTY 1
#define KEY_ERR 2
}ClientInfoRes_st;

typedef struct{
	PktHdr_st hdr;
	u64 srv_ref_time;
}MatchTimeReq_st;

#define JPEG_TYPE_RGB565 0x01
#define JPEG_TYPE_RGchar888 0x02
typedef struct{
//NOTE: change member should modify "calling convert_to_client_mstime...."
	char jpeg_type;
	int width;
	int height;
	int frame_length;
//  int frame_offset;
	int rotation;
	u64 timestamp;
	int encode_time;
}ImageInfo_st;

typedef struct{
	u64 time_stamp;
	char channel_num;
	unsigned short sample_rate;
	char bit_depth;
}AudioInfo_st;

typedef struct{
	PktHdr_st hdr;
	char reason;
}ServerCmdInd_st;

typedef struct{
	PktHdr_st hdr;
	char result;
}ServerCmdCnf_st;

typedef struct{
	PktHdr_st hdr;
	char reason;
}ClientSrvReq_st;

typedef struct{
	PktHdr_st hdr;
	char result;
}ClientSrvRes_st;

/*comm thread packet definition*/
typedef struct{
	int cmd_type;
#define REMOTE_SHARE_IMAGE 1
	int cmd_val;
	char name_len;
	char name[MAX_CLIENT_NAME_SIZE + 1];
}RemoteMsg_st;

typedef struct{
	PktHdr_st hdr;
	RemoteMsg_st msg;
}RemoteMsgReq_st;

#pragma pack()

#endif
