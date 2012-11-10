#include <unistd.h>
#include <pthread.h>
#include "protocol.h"
#include "server.h"
#include "hal.h"
#include "server.h"
#include "comm.h"
#include "display.h"
#include "jpeg_enc.h"


#undef LOG_TAG
#define LOG_TAG "ISHARE_HAL"

enum {
	event_video = 0,
	event_audio,
	event_jpeg,
	event_count,
};

struct event {
	pthread_mutex_t lock;
	pthread_cond_t ready;
	int status;
#define EV_STOP 0x00
#define EV_START 0x03
#define EV_PAUSE 0x02
};

struct event events[event_count];
struct callback_set g_cb = {NULL, NULL, NULL, NULL};

int sw_encode_flag = 1;
extern int g_clear_screen;

static int wait_event(int event, int status)
{
	int ret = 0;

	ret = pthread_mutex_lock(&events[event].lock);
	while (events[event].status == status) {
		pthread_cond_wait(&events[event].ready,
					&events[event].lock);
	}
	ret = pthread_mutex_unlock(&events[event].lock);

	return ret;
}

static int set_event(int event, int status)
{
	int ret;

	ret = pthread_mutex_lock(&events[event].lock);
	events[event].status = status;
	ret = pthread_mutex_unlock(&events[event].lock);

	return ret;
}

static void init_event(void)
{
	pthread_mutex_init(&events[event_video].lock, NULL);
	pthread_mutex_init(&events[event_audio].lock, NULL);
	pthread_mutex_init(&events[event_jpeg].lock, NULL);

	pthread_cond_init(&events[event_video].ready, NULL);
	pthread_cond_init(&events[event_audio].ready, NULL);
	pthread_cond_init(&events[event_jpeg].ready, NULL);

	events[event_video].status = EV_STOP;
	events[event_audio].status = EV_STOP;
	events[event_jpeg].status = EV_STOP;

	return;
}

static void loseconncet(int id)
{
	LOGE("SIGPIPE: id = %d\n", id);
	return;
}

/*static void save_first_jpeg(char *buf, int size)
{
	static int first = 1;
	if(first)
	{
		FILE *fdump= fopen("/sdcard/haibo.jpeg", "wb+");
		if(fdump)
		{
			fwrite(buf, size, 1, fdump);
			fclose(fdump);
		}
		first = 0;
	}
}*/

/*static void save_audio(char *buf, int size)
{
	char filename[]="/sdcard/haibo.pcm";
	FILE *fdump= fopen(filename, "ab+");
	if(fdump!=NULL)
	{
		fwrite(buf, size, 1, fdump);
		fclose(fdump);
	}
}

ssize_t readx(int fd, void *buff, size_t len)
{
	size_t nleft;
	ssize_t nread;
	char *p;

	p = (char *)buff;
	nleft = len;

	while (nleft > 0) {
		if ((nread = read(fd, p, nleft)) < 0) {
			if (errno == EINTR) {
				LOGD("++++++++++++++++++++\n");
				nread = 0;
			}
			else
				return -1;
		} else if (nread == 0)
			break;

		nleft -= nread;
		p += nread;
	}
	return (len - nleft);
}

int recv_audio(int fd, void **buff)
{
	int ret;
	char *packet;
	struct mm_data *md;
	struct audio_info *ai;
	int audio_head_size = sizeof(struct mm_data) + sizeof(struct audio_info);
	char buf[audio_head_size];

	*buff = NULL;
	md = (struct mm_data *)buf;
	md->type = MM_AUDIO;
	md->lenth = sizeof(struct audio_info);

	ai = (struct audio_info *)(buf + sizeof(struct mm_data));
	ret = readx(fd, ai, sizeof(struct audio_info));
	if(ret <= 0)
		return -1;
	packet = (char *)malloc(audio_head_size + ai->audio_length);
	if(!packet)
		return -1;
	memcpy(packet, buf, audio_head_size);
	ret = readx(fd, packet + audio_head_size, ai->audio_length);
	if(ret < 0)
	{
		free(packet);
		return -1;
	}

	*buff = packet;
	return audio_head_size + ai->audio_length;
}*/

int send_video(void *buff, size_t len,
				uint64_t timestamp, int width, int height, struct jpeg_ext *ext)
{
	char *packet;
	int packet_len = 0;
	struct mm_data *md;
	struct jpeg_frame *jf;
	int image_head_size = sizeof(struct mm_data) + sizeof(struct jpeg_frame);

	LOGD("IN [%s:%d]==>\n", __func__, __LINE__);
	packet_len = image_head_size + len;
	if(ext)
	{
		packet_len += sizeof(struct jpeg_ext);
	}
	packet = malloc(packet_len);
	if(!packet)
		return -1;

	md = (void *)packet;
	md->type = MM_JPEG;
	md->lenth = sizeof(struct jpeg_frame);

	jf = (void *)(packet + sizeof(struct mm_data));
	jf->jpeg_type = JPEG_TYPE_RGB565;
	jf->timestamp = timestamp;
	jf->frame_length = len;
	jf->width = width;
	jf->height = height;
	jf->encode_time = 0;
#if 0
	jf->rotation = get_server_rotation();
#else
	jf->rotation = g_clear_screen ? 0xFF : 0;
#endif
	LOGD("HERE: g_clear_screen = %d, jf->rotation = %d\n",
			g_clear_screen, jf->rotation);
	if(ext)
	{
		memcpy(packet + image_head_size, ext, sizeof(struct jpeg_ext));
		jf->frame_length += sizeof(struct jpeg_ext);
		image_head_size += sizeof(struct jpeg_ext);
	}
	memcpy(packet + image_head_size, buff, len);
	async_send_image(packet, packet_len);
	LOGD("OUT [%s:%d]==>\n", __func__, __LINE__);
	return 0;
}

static void video_process(char *machine)
{
	int ret = 0;
	struct display_info *di;

	if (events[event_jpeg].status == EV_STOP)
		set_event(event_jpeg, EV_PAUSE);

	di = (struct display_info *)malloc(
			sizeof(struct display_info));

	ret = display_info_init(di);
	if (ret < 0)
	{
		goto fail;
	}

	if(strcmp(machine, "qualcomm") == 0)
	{
		di->machine = QUALCOMM_PLAT;
	}
	else if(strcmp(machine, "samsung") == 0)
	{
		di->machine = SAMSUNG_PLAT;
	}
	else if(strcmp(machine, "software") == 0)
	{
		di->machine = LIBJPEG_TURBO;
		sw_encode_flag = 1;
	}
	else
	{
		di->machine = UNKNOWN_PLAT;
	}

	//DEBUG:
	//NOTE: Hi boy, these codes could be used to debug hardware encode
	//Just open this switcher to enable hardware encodec
#if 0
	di->machine = QUALCOMM_PLAT;
	sw_encode_flag = 0;
	if (di->machine == LIBJPEG_TURBO)
#else
	if (1)
#endif
	{
		LOGD("-------------------------------------");
		LOGD("\t\t\tsw_jpeg_init\n");
		LOGD("-------------------------------------");
		ret = sw_jpeg_init(di);
	}
	else
	{
		ret = hw_jpeg_init(di);
		if(ret < 0){
			LOGE("hw_jpeg_init failed in %s func at %d line!\n", __func__, __LINE__);
			goto fail;
		}
	}

	LOGD("START SEND IMAGE, ret = %d, events[event_jpeg].status = %d\n",
			ret, events[event_jpeg].status);
	while (ret >= 0 && events[event_jpeg].status != EV_STOP) {
		wait_event(event_jpeg, EV_PAUSE);
		LOGD("events[event_jpeg].status = %d\n", events[event_jpeg].status);
//		ret = display_detect_filp(di);
		ret = 1;
		if (ret && (events[event_jpeg].status == EV_START)) {
			struct jpeg_ext ext;
			if (display_get_frame(di) < 0)
				LOGE("display get frame error\n");

			if(di->slice_flag)
			{
				int index, offset;
				ext.fragment_num = di->slice_num;
				for(index = 0, offset = 0; index < di->slice_num; index++)
				{
					ext.fragment[index].offset = offset;
					ext.fragment[index].size = di->slice_size[index];
					offset += di->slice_size[index];
				}
			}
			else
			{
				ext.fragment_num = 1;
				ext.fragment[0].offset = 0;
				ext.fragment[0].size = di->jpeg_size;
			}

			ret = send_video(di->jpeg_base,
						di->jpeg_size,
						di->timestamp,
						di->vi.xres,
						di->vi.yres, &ext);

			g_clear_screen = 0;
			display_done();
		}
	}

	if (di->machine == LIBJPEG_TURBO)
		sw_jpeg_deinit();
	else
		hw_jpeg_deinit();
	display_info_deinit(di);
fail:
	free(di);
	set_event(event_jpeg, EV_STOP);

	LOGI("STOP SEND IMAGE !!\n");

	return;
}

/*static void audio_process(int ser_fd)
{
	int ret = 0;
	void *p;

	if (events[event_audio].status == EV_STOP)
		set_event(event_audio, EV_PAUSE);

	while(ret >= 0 && events[event_audio].status != EV_STOP) {
		wait_event(event_audio, EV_PAUSE);
		ret = recv_audio(ser_fd, &p);
		if(ret > 0)
		{
			//ret = send_audio(cli_fd, p, ret);
			free(p);
		}
	}
	set_event(event_audio, EV_STOP);

	LOGI("STOP SEND AUDIO!!\n");
	return;
}*/

int handle_control_msg(char *buf)
{
	struct msg_head *msg_head;
	struct msg_control *msg_control;
	struct msg_multouch_st *mmt;
	struct msg_key_st *mk;
	struct msg_mouse_st *mm;

	msg_head = (void *)buf;

	LOGD("%s: %d\n", __func__, msg_head->type);
	switch (msg_head->type) {
	case MSG_CONTROL_HAL:
		msg_control = (void *)(buf + sizeof(struct msg_head));
		g_cb.control_func(msg_control);
		break;
	case MSG_EVENT_TOUCH:
		mmt = (void *)(buf + sizeof(struct msg_head));
		g_cb.touch_func(mmt);
		break;
	case MSG_EVENT_KEY:
		mk = (void *)(buf + sizeof(struct msg_head));
		g_cb.key_func(mk);
		break;
	case MSG_EVENT_MOUSE:
		mm = (void *)(buf + sizeof(struct msg_head));
		g_cb.mouse_func(mm);
		break;
	default:
		return -1;
	}

	return 0;
}

static int send_event(int event, int status)
{
	int ret = 0;

	ret = set_event(event, status);
	ret = pthread_cond_signal(&events[event].ready);

	return ret;
}

static int control_cb(struct msg_control *mc)
{
	switch (mc->type) {
		case MM_VIDEO:
		LOGD("mc->status = %d\n", mc->status);
		if (mc->status == GRAB_START) {
			LOGD("send event for jpeg!\n");
			send_event(event_jpeg, EV_START);
			send_event(event_audio, EV_START);
		} else if (mc->status == GRAB_PAUSE) {
			set_event(event_jpeg, EV_PAUSE);
			set_event(event_audio, EV_PAUSE);
		} else {
			send_event(event_jpeg, EV_STOP);
			send_event(event_audio, EV_STOP);
		}

		break;
	}

	return 0;
}

static void init_comm_callback(struct callback_set *cb)
{
	cb->control_func = control_cb;
	return;
}

static void *video_thread_func(void *arg)
{
	video_process(arg);
	return ((void *)0);
}

/*static void *audio_thread_func(void *arg)
{
	int ac_fd = 0;

	while (ac_fd <= 0) {
		ac_fd = create_local_socket_client(A_CLIENT_SOCKET_NAME,
			ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
		if (-1 == ac_fd)
			LOGE("audio client:%s\n", strerror(errno));
		if(ac_fd > 0)
			break;
		sleep(1);
	};

	audio_process(ac_fd);
	return ((void *)0);
}*/

static int create_detached_thread(pthread_t *tid, void *(*fn)(void *), void *arg)
{
	int ret;
	pthread_attr_t attr;

	ret = pthread_attr_init(&attr);
	if (ret != 0)
		return ret;

	ret = pthread_attr_setdetachstate(&attr,
				PTHREAD_CREATE_DETACHED);
	if (ret == 0)
		ret = pthread_create(tid, &attr, fn, arg);

	pthread_attr_destroy(&attr);
	return ret;
}

int init_hal_socket(struct ishare_service *service)
{
	int ret;
	pthread_t video_thread;

	init_comm_callback(&g_cb);
	signal(SIGPIPE, loseconncet);
	init_event();

	ret = create_detached_thread(
			&video_thread, video_thread_func, "");
	if (ret != 0)
		LOGE("create video thread start error");

/*	ret = create_detached_thread(
			&audio_thread, audio_thread_func, NULL);
	if (ret != 0)
		LOGE("create audio thread start error");*/

	return 0;
}

void deinit_hal_socket(struct ishare_service *service)
{
	//deinit_input();
	return;
}

int hal_send_control(char type, char status)
{
	char buf[sizeof(struct msg_head) + sizeof(struct msg_control)];
	struct msg_head *head = (struct msg_head*)buf;
	struct msg_control *control = (struct msg_control*)(buf + sizeof(struct msg_head));
	head->type = MSG_CONTROL_HAL;
	head->lenth = sizeof(struct msg_control);
	LOGD("type = 0x%x\n", type);
	switch(type & SRV_TYPE_IMAGE_AUDIO)
	{
		case SRV_TYPE_AUDIO:
			control->type = MM_AUDIO;
			break;
		case SRV_TYPE_IMAGE:
			control->type = MM_JPEG;
			break;
		case SRV_TYPE_IMAGE_AUDIO:
			control->type = MM_VIDEO;
			break;
		default:
			break;
	}
	control->status = status;
	handle_control_msg(buf);
	return 1;
}
