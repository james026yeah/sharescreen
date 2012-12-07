#include <string.h>
#include <jni.h>
#include "common.h"
#include "protocol.h"
#include "protocol_client.h"
#include "communicator.h"
//#include "native_surface.h"
//#include "surface.h"

static struct ptl_client g_client;
extern int g_surface_ready;
//extern int g_clear_screen_client;

int ptl_disconnect_internal(void);
int ptl_connect_internal(char *srv_addr, char *name, char *key);
//extern void init_surface(JNIEnv *env, jobject jsurface);
int ptl_init(void);
int ptl_start_media(void);
extern void _callback_notify_status_client(int connected);

#if DEBUG_TIME_POINT
static u64 get_server_mstime(void)
{
	return __get_system_mstime() + (g_client.mt_zero - g_client.mt_stop);
}
#endif

static u64 convert_to_client_mstime(u64 server_mstime)
{
	return server_mstime + ( g_client.mt_stop - g_client.mt_zero );
}

static int disconnect_server(void)
{
	LOGD("%s\n", __func__);
	__shutdown_socket(g_client.client_sockfd, SOCK_SHUT_RDWR) ;
	return ESuccess;
}

static int async_send_msg(void *msg, int len)
{
	int err;
	LOGD("%s: msg = '%s'\n", __func__, (char *)msg);

    if (!msg || len <= 0 || len > MAX_SEND_SIZE)
    {
		LOGE("msg is invalid\n");
		return EFailed;
    }

	MUTEX_LOCK(&g_client.deque_mutex);
	err = push_deque(&g_client.send_msg, msg, len);
	if (ERR_NULL != err)
	{
		LOGE("push_deque error, ecode = %d\n", err);
		return EFailed;
	}
	MUTEX_UNLOCK(&g_client.deque_mutex);
	__set_event(&g_client.send_event, &g_client.send_mutex, &g_client.send_cond);

    return ESuccess;
}

static int send_client_info_msg(void)
{
	char *send_pkt, *p;
	PktHdr_st hdr;
	int klen = g_client.ci.key_len;
	int mlen = g_client.ci.name_len;
	int pkt_len;
	int size;
	int ret;

	pkt_len = sizeof(PktHdr_st) + sizeof(ClientInfo_st) -
			sizeof(char) * (MAX_KEY_CODE_SIZE + 1 - klen) -
			sizeof(char) * (MAX_CLIENT_NAME_SIZE + 1 - mlen);
	LOGD("%s: pkt_len = %d, klen = %d, mlen = %d\n", __func__, pkt_len, klen, mlen);
	send_pkt = (char *)malloc(pkt_len);
	if (!send_pkt)
		return EFailed;

	hdr.type = MSG_CLIENT_INFO;
	hdr.sub_type = 0;
	hdr.len = pkt_len - sizeof(PktHdr_st);
	hdr.client_id = g_client.client_id;

	p = send_pkt;
	size = sizeof(PktHdr_st);
	memcpy(p, &hdr, size);
	p += size;

	size = sizeof(g_client.ci.key_len);
	memcpy(p, &g_client.ci.key_len, size);
	p += size;

	size = g_client.ci.key_len;
	memcpy(p, g_client.ci.key, size);
	p += size;

	size = sizeof(g_client.ci.name_len);
	memcpy(p, &g_client.ci.name_len, size);
	p += size;

	size = g_client.ci.name_len;
	memcpy(p, g_client.ci.name, size);
	p += size;

	size = sizeof(g_client.ci.width) + sizeof(g_client.ci.height) +
		sizeof(g_client.ci.dpi);
	memcpy(p, &g_client.ci.width, size);

	ret = async_send_msg(send_pkt, pkt_len);
	if (ret == EFailed)
		free(send_pkt);

	return ret;
}

static int send_match_time_msg(void)
{
	int ret;
	TimeReq_st *send_pkt;

	send_pkt = (TimeReq_st*)malloc(sizeof(TimeReq_st));
	if (!send_pkt) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	send_pkt->hdr.type = MSG_TIME_SYNC;
	send_pkt->hdr.sub_type = 0;
	send_pkt->hdr.len = 0;
	g_client.timestamp++;
	send_pkt->hdr.client_id = g_client.timestamp;

	ret = async_send_msg(send_pkt, sizeof(* send_pkt));
	if (ret == EFailed)
		free(send_pkt);

	return ret;
}

static int send_release_cnf(void)
{
	int ret;
	ServerCmdCnf_st *send_pkt;

	send_pkt = (ServerCmdCnf_st *)malloc(sizeof(ServerCmdCnf_st));
	if (!send_pkt)
	{
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	send_pkt->hdr.type = MSG_SERVER_CMD;
	send_pkt->hdr.sub_type = CMD_TYPE_RELEASE;
	send_pkt->hdr.len = sizeof(ServerCmdCnf_st) - sizeof(PktHdr_st);
	send_pkt->hdr.client_id = g_client.client_id;
	send_pkt->result = 1;

	ret = async_send_msg(send_pkt, sizeof(* send_pkt));
	if (ret == EFailed)
		free(send_pkt);

	return ret;
}

static int send_release_req(void)
{
	int ret;
	ClientSrvReq_st *send_pkt;

	send_pkt = (ClientSrvReq_st *)malloc(sizeof(ClientSrvReq_st));
	if (!send_pkt)
	{
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	send_pkt->hdr.type = MSG_CLIENT_SIG;
	send_pkt->hdr.sub_type = SIG_TYPE_RELEASE;
	send_pkt->hdr.len = sizeof(ClientSrvReq_st) - sizeof(PktHdr_st);
	send_pkt->hdr.client_id = g_client.client_id;
	send_pkt->reason = 1;

	ret = async_send_msg(send_pkt, sizeof(* send_pkt));
	if (ret == EFailed)
		free(send_pkt);

	return ret;
}

static int process_image_data(PktHdr_st *hdr)
{
	int read_len;
	int info_len;
	u64 server_time, local_time;
	int time_offset;
#if DEBUG_TIME_POINT
	u64 start, stop;
#endif

	info_len = sizeof(ImageInfo_st);

	if (hdr->len > MAX_IMAGE_SIZE) {
		LOGE("ERR: image packet length %d larger than %d!\n", hdr->len, MAX_IMAGE_SIZE);
		return EFailed;
	}

#if DEBUG_TIME_POINT
	LOGD("-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'");
	start = __get_system_mstime();
	LOGD("wait frame..... start time = %lld\n", start);
#endif
	read_len = __recv_socket(g_client.client_sockfd, g_client.image_data, hdr->len, 0);
#if DEBUG_TIME_POINT
	stop = __get_system_mstime();
	LOGD("recv frame..... stop time = %lld, delay = %lld ms\n",stop, stop - start);
#endif

	if (read_len < hdr->len)
	{
		LOGE("ERR: expected image size = %d, but actual recv size = %d\n", hdr->len, read_len);
		disconnect_server();
		return EFailed;
	}

	if (hdr->len - info_len != ((ImageInfo_st *)g_client.image_data)->frame_length)
	{
		LOGE("ERROR! hdr->len = %d, but frame_length = %d\n", hdr->len,
				((ImageInfo_st *)g_client.image_data)->frame_length);
		return EFailed;
	}

#if 0
	((ImageInfo_st *)g_client.image_data)->encode_time =
		((ImageInfo_st *)g_client.image_data)->rotation == 0xFF ? 1 : 0;
	LOGD("Jerry: rotation = %d\n", ((ImageInfo_st *)g_client.image_data)->rotation);
#endif

#if DEBUG_TIME_POINT
	LOGD("-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'-'");
	LOGD("copy image: image_timestamp = %lld, curr time = %lld, transmit delay tiem = %lld\n",
			((ImageInfo_st *)g_client.image_data) -> timestamp,
			__get_system_mstime(),
			__get_system_mstime() -
			convert_to_client_mstime(((ImageInfo_st *)g_client.image_data) -> timestamp));

#endif

	/*convert timestamp to local time*/
	server_time = ((ImageInfo_st *)g_client.image_data) -> timestamp;
	local_time = convert_to_client_mstime(server_time);
	LOGV("server time = %lld -> local time = %lld\n", server_time, local_time);
	time_offset = sizeof(ImageInfo_st) - sizeof(int) - sizeof(u64);
	memcpy((char *)g_client.image_data + time_offset, &local_time, sizeof(u64));

	LOGD("copy image size = %d\n", hdr->len - info_len);
#ifdef WIN32
	g_client.service_cb.copy_image((char *)g_client.image_data, hdr->len);
#elif defined LINUX
	if(!g_client.service_cb.copy_image)
	{
		LOGE("ERROR!\n");
		return EFailed;
	}
	g_client.service_cb.copy_image((char *)g_client.image_data, hdr->len);
#endif
	return ESuccess;
}

static int process_audio_data(PktHdr_st *hdr)
{
	int read_len;
	int audio_len;

	if (hdr->len > MAX_AUDIO_SIZE) {
		LOGE("ERR: audio packet length %d larger than %d!\n", hdr->len, MAX_AUDIO_SIZE);
		return EFailed;
	}

	read_len = __recv_socket(g_client.client_sockfd, g_client.audio_data, hdr->len, 0);

	if (read_len < hdr->len)
	{
		LOGE("ERR: expected audio size = %d, but actual recv size = %d\n", hdr->len, read_len);
		disconnect_server();
		return EFailed;
	}

	audio_len = sizeof(AudioInfo_st);
	g_client.service_cb.copy_audio((char *)g_client.audio_data + audio_len, hdr->len - audio_len);
	return ESuccess;
}

static int process_match_time(PktHdr_st *hdr, char *msg)
{
	u64 tm_delta;
	MatchTimeReq_st mt;

	mt.srv_ref_time = *(u64 *)msg;

	/* check timestamp */
	if (hdr->client_id != g_client.timestamp)
	{
		LOGD("Match time timestamp checking NG!\n");
		LOGD("hdr->client_id = %d, g_client.timestamp = %d\n",
				hdr->client_id, g_client.timestamp);
		return ESuccess;
	}

	g_client.mt_status = 0;
	tm_delta = g_client.mt_stop - g_client.mt_start;
	LOGD("Match time tm_delta = %lld\n", tm_delta);

	if ( tm_delta < MATCH_TIME_LIMIT_MS * 2)
	{
		LOGD("Match time OK\n");
		g_client.mt_status = 1;
		g_client.mt_zero = mt.srv_ref_time;
	}
	else
	{
		LOGD("Match time failed!\n");
	}

	__send_event(&g_client.sync_event, &g_client.sync_mutex);
	hdr = NULL; //dummy for compile
	return ESuccess;
}

static int process_setup_res(PktHdr_st *hdr, char *msg)
{
	SetupRes_st res;

	res.setup_ret = *msg++;
	res.err_code = *msg;

	if (res.setup_ret == RES_SUCCESS) {
		g_client.srv_type = SRV_TYPE_MAX;
	}
	else {
		LOGE("ERR: setup failure. error code is %d\n", res.err_code);
	}
	__send_event(&g_client.sync_event, &g_client.sync_mutex);

	hdr = NULL; //dummy for compile
	return ESuccess;
}

static int process_control_res(PktHdr_st *hdr, char *msg)
{
	CtlRes_st res;

	res.ctl_ret = *(char *)msg++;
	res.err_code = *(char *)msg;

	if (res.ctl_ret == RES_SUCCESS) {
		g_client.state = (hdr->sub_type == CTL_START) ? CLT_STATE_START : CLT_STATE_CONNECT;
	}
	else {
		LOGE("ERR: control failure. error code is %d\n", res.err_code);
	}
	__send_event(&g_client.sync_event, &g_client.sync_mutex);

	return ESuccess;
}

static int process_clientinfo_res(PktHdr_st *hdr, char *msg)
{
	ClientInfoRes_st res;
	res.keyVery = *(char *)msg;

	if (res.keyVery == KEY_APPROVED) {
		g_client.client_id = hdr->client_id;
		g_client.key_state = KEY_APPROVED;
	}
	else {
		LOGE("ERR: server refuse client connect\n");
		g_client.key_state = KEY_ILLEGAL;
	}
	__send_event(&g_client.sync_event, &g_client.sync_mutex);
	return ESuccess;
}

static int process_cmd_ind(PktHdr_st *hdr, char *msg)
{
	switch(hdr->sub_type)
	{
	case CMD_TYPE_RELEASE:
		send_release_cnf();
		break;
	default:
		LOGE("ERR: unsupported sub_type %d\n", hdr->sub_type);
		break;
	}

	msg = NULL; //dummy for compile
	return ESuccess;
}

static int process_sig_res(PktHdr_st *hdr, char *msg)
{
	__send_event(&g_client.sync_event, &g_client.sync_mutex);
	disconnect_server();

	hdr = hdr;
	msg = msg;
	return ESuccess;
}

static int match_time(void)
{
	int retry;

	LOGD("%s start match time\n", __func__);

	retry = 0;
	do
	{
		if (EFailed == send_match_time_msg()) {
			LOGE("send match time msg fail\n");
			return EFailed;
		}

		LOGD("wait match_time response\n");
		if(EFailed == __wait_event(&g_client.sync_event,
			&g_client.sync_mutex, MATCH_TIME_RES_TIMEOUT))
		{
			LOGE("%s: wait event error\n", __func__);
		}
		else
		{
			if (1 == g_client.mt_status)
			{
				__reset_event(&g_client.sync_event);
				return ESuccess;
			}
		}

		__reset_event(&g_client.sync_event);
		retry++;
	} while (retry < 5);

    return EFailed;
}

static int register_client(void)
{
	LOGD("%s: regiter client ......\n", __func__);

	if (EFailed == send_client_info_msg()) {
		LOGE("send client info msg fail\n");
		return EFailed;
	}

	if(EFailed == __wait_event(&g_client.sync_event,
		&g_client.sync_mutex, COMMON_PKT_RES_TIMEOUT))
	{
		return EFailed;
	}

	__reset_event(&g_client.sync_event);
	if (g_client.key_state == KEY_APPROVED)
		return ESuccess;
	else
		return EFailed;
}

static int deregister_client(void)
{
	if (EFailed == send_release_req())
	{
		LOGE("%s: fail to send release req!\n", __func__);
		return EFailed;
	}
	else
	{
		__wait_event(&g_client.sync_event,
				&g_client.sync_mutex, COMMON_PKT_RES_TIMEOUT);
		return ESuccess;
	}
}


static void *listen_thread_loop(void *arg)
{
	int sock, len, read_len;
	struct sockaddr_in client_addr;
	//int ret;
	char read_buf[MAX_RECV_SIZE];
	char ip[16] = {0};
	char *pBuf = read_buf;
	int count = 0;

	LOGI("listen thread starting ......\n");
	g_client.lstn_running = 1;

	if (EFailed == __open_server_socket(&g_client.listen_sockfd, SOCK_PRE_LISTEN_PORT))
		return NULL;

	while(g_client.lstn_running)
	{
		/*check if exist new incoming client*/
		len = sizeof(struct sockaddr_in);
		LOGD("start to accept client .... \n");
		sock = accept(g_client.listen_sockfd, (struct sockaddr *)&client_addr, &len);
		LOGD("accept time up, sock = %d\n", sock);

		if (sock == -1)
			goto ABORT_ERROR;

		do
		{
			read_len = recv(sock, pBuf, sizeof(read_buf), 0);
			LOGD("__recv_socket time up : '%s'\n", pBuf);
			inet_ntop(AF_INET,&client_addr.sin_addr,ip,sizeof(ip));
			LOGD("Client ip = %s\n", ip);
			if (read_len == 0)
				goto RECV_ERROR;

			if (strstr(pBuf, "hi, date tonight!"))
			{
				/* Have a meal with server tonight, okey */
				ptl_init();
				_callback_notify_status_client(1);
				while(count++ < 1000)
				{
					__msleep(10);
					LOGD("count = %d\n", count);
					//WARNING: this variable should be sychronized with LOCK
					//I just wannat fast debug, forgive me!
					if (g_surface_ready == 1)
					{
						LOGD("surface ready OK!");
						break;
					}
				}

				//ptl_connect_internal("192.168.1.1", "HOT_GIRL", "KISS_ME");
				ptl_connect_internal(ip, "HOT_GIRL", "KISS_ME");
				ptl_start_media();
				break;
			}

		}
		while(1);

		continue;
RECV_ERROR:
		LOGE("RECV_ERROR, __close_socket\n");
		__close_socket(sock);
	}

ABORT_ERROR:
	LOGE("ABORT_ERROR, __close_socket\n");
	__close_socket(g_client.listen_sockfd);

	arg = arg;
	return NULL;
}

#ifdef WIN32
static DWORD WINAPI send_thread_loop(LPVOID arg)
#elif defined LINUX
static void * send_thread_loop(void *arg)
#endif
{
	int ret;
	int i;
	int interval;

#ifdef _ISHARE_LOG_D
	interval = 2 * 1000;
#else
	interval = 1 * 1000;
#endif

	LOGD("start send_thread > > > \n");
	__set_event(&g_client.init_event, &g_client.init_mutex, &g_client.init_cond);

	while(g_client.send_running)
    {
    	LOGD("wait send event......\n");
    	ret = __check_event(&g_client.send_event, &g_client.send_mutex,
			   &g_client.send_cond,	interval);

		if (ret == EFailed)
		{
			continue;
		}

		while (g_client.send_running)
		{
			char *send_pkt;
			int len;
			int num;

			num = deque_number(g_client.send_msg);
			LOGV("deque data number = %d\n", num);

			MUTEX_LOCK(&g_client.deque_mutex);
			if (deque_is_empty(g_client.send_msg))
			{
				__reset_event(&g_client.send_event);
				MUTEX_UNLOCK(&g_client.deque_mutex);
				break;
			}
			ret = pop_deque(&g_client.send_msg, (void **)&send_pkt, &len);
			if (ERR_NULL != ret) {
				LOGE("ERR: pop_deque error %d\n", ret);
				__reset_event(&g_client.send_event);
				MUTEX_UNLOCK(&g_client.deque_mutex);
				break;
			}

			MUTEX_UNLOCK(&g_client.deque_mutex);

			LOGD("- - - - - - send packet - - - - - - \n");
			for (i = 0; i < (int)sizeof(PktHdr_st); i++)
				LOGD("pkt_header[%d] = 0x%X\n", i, *(send_pkt + i));
			for (i = 0; i < len - (int)sizeof(PktHdr_st); i++)
				LOGD("pkt_content[%d] = 0x%X - %d - '%c'\n", i,
						*(send_pkt + i + sizeof(PktHdr_st)),
						*(send_pkt + i + sizeof(PktHdr_st)),
						*(send_pkt + i + sizeof(PktHdr_st)));

			/* record start time for match time */
			if (*(char *)send_pkt == MSG_TIME_SYNC)
				g_client.mt_start = __get_system_mstime();

			ret = __send_socket(g_client.client_sockfd, send_pkt, len, 0);

			/*abort protocol if release cnf packet*/
			if (*(char *)send_pkt == MSG_SERVER_CMD &&
				   	*((char *)send_pkt + 1) == CMD_TYPE_RELEASE)
			{
				LOGD("%s: abort protocol if release cnf packet\n", __func__);
				disconnect_server();
				LOGD("abort send thread\n");
				pthread_join(g_client.recv_handle, NULL);
				/*abort send thread after disconnect server*/
#if 0
				if (g_client.service_cb.connect_status)
				{
					LOGD("callback connection status start");
					g_client.service_cb.connect_status(0, 0);
					LOGD("callback connection status end");
				}
				else
				{
					LOGD("ERROR: g_client.service_cb.connect_status is NULL!\n");
				}
#endif

				//Notify client is aborting......
				_callback_notify_status_client(0);
				return ESuccess;
			}
			free(send_pkt);

		}
    }

	arg = NULL; //dummy for compile
    return ESuccess;
}

#ifdef WIN32
static DWORD WINAPI recv_thread_loop(LPVOID arg)
#elif defined LINUX
static void * recv_thread_loop(void *arg)
#endif
{
	int i = 0;
	int read_len;
	PktHdr_st pkt_hdr;
	char read_buf[MAX_COMMON_PKT_SIZE];
#if DEBUG_TIME_POINT
u64 start, stop;
#endif

	//temp test
	LOGD("start recv thread > > > \n");
	__set_event(&g_client.init_event, &g_client.init_mutex, &g_client.init_cond);

	while(g_client.recv_running){
		read_len = __recv_socket(g_client.client_sockfd, &pkt_hdr,
			sizeof(PktHdr_st), 0);

		if (read_len < (int)sizeof(PktHdr_st))
		{
			LOGE("ERR: expected size = %d, but actual recv size = %d\n", sizeof(PktHdr_st), read_len);
			goto PROTOCOL_ABORT;
		}

		LOGD("IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII\n");
		LOGD("Client protocol: receive packet type is %d\n", pkt_hdr.type);

		if (pkt_hdr.type == MSG_IMAGE_DATA)
		{
			process_image_data(&pkt_hdr);
		}
		else if (pkt_hdr.type == MSG_AUDIO_DATA)
		{
			process_audio_data(&pkt_hdr);
		}
		else if (pkt_hdr.type == MSG_TIME_SYNC ||
					pkt_hdr.type == MSG_CLIENT_INFO ||
					pkt_hdr.type == MSG_CONTROL ||
					pkt_hdr.type == MSG_SETUP ||
					pkt_hdr.type == MSG_SERVER_CMD ||
					pkt_hdr.type == MSG_CLIENT_SIG)
		{
			if (pkt_hdr.len > (int)sizeof(read_buf))
			{
				LOGE("read_buf size = %d larger than pkt_hdr.len = %d\n",
						sizeof(read_buf), pkt_hdr.len);
				goto PROTOCOL_ABORT;
			}

			read_len = __recv_socket(g_client.client_sockfd, read_buf, pkt_hdr.len, 0);

			if (read_len < pkt_hdr.len)
			{
				LOGE("ERR: expected size = %d, but actual recv size = %d\n", pkt_hdr.len, read_len);
				goto PROTOCOL_ABORT;
			}

			LOGI("packet type = %d, sub_type = %d\n", pkt_hdr.type, pkt_hdr.sub_type);
			LOGD("\t- - - - - - recv packet - - - - - - \n");
			for (i = 0; i < (int)sizeof(PktHdr_st); i++)
				LOGD("\tpkt_hdr[%d] = 0x%X\n", i, *((char *)&pkt_hdr + i));

			for (i = 0; i < pkt_hdr.len; i++)
				LOGD("\tpkt_content[%d] = 0x%X - %d - '%c'\n", i,
						*(read_buf + i), *(read_buf + i), *(read_buf + i));

			switch(pkt_hdr.type) {
			case MSG_TIME_SYNC:
				g_client.mt_stop = __get_system_mstime();
				process_match_time(&pkt_hdr, read_buf);
				break;
			case MSG_CLIENT_INFO:
				process_clientinfo_res(&pkt_hdr, read_buf);
				break;
			case MSG_SETUP:
				process_setup_res(&pkt_hdr, read_buf);
				break;
			case MSG_CONTROL:
				process_control_res(&pkt_hdr, read_buf);
				break;
			case MSG_SERVER_CMD:
				process_cmd_ind(&pkt_hdr, read_buf);
				break;
			case MSG_CLIENT_SIG:
				process_sig_res(&pkt_hdr, read_buf);
				break;
			default:
				LOGE("ERR: Unsupported packet, type = %d\n", pkt_hdr.type);
				break;
			}
		}
		else
		{
			LOGE("ERR: unsupported packet type is %d\n", pkt_hdr.type);
		}

	}

	arg = NULL; //dummy for compile

PROTOCOL_ABORT:
	ptl_disconnect_internal();
	return NULL;
}

int init_listen_thread(char *srv_addr, char *name, char *key)
{
	LOGD("%s: start init!\n", __func__);

    if (ESuccess != __create_thread(&g_client.listen_handle, listen_thread_loop, NULL))
	{
		LOGD("ERROR!!! __LINE__ = %d\n", __LINE__);
		return EFailed;
	}

	srv_addr = srv_addr;
	name = name;
	key = key;
	return 0;
}

static int create_transfer_threads(void)
{
    if (ESuccess != __create_thread(&g_client.send_handle, send_thread_loop, NULL))
		return EFailed;
	if (ESuccess != __check_event(&g_client.init_event, &g_client.init_mutex,
				&g_client.init_cond, THREAD_INIT_TIMEOUT))
		return EFailed;
	__reset_event(&g_client.init_event);

    if (ESuccess != __create_thread(&g_client.recv_handle, recv_thread_loop, NULL))
		return EFailed;
	if (ESuccess != __check_event(&g_client.init_event, &g_client.init_mutex,
				&g_client.init_cond, THREAD_INIT_TIMEOUT))
		return EFailed;
	__reset_event(&g_client.init_event);

    return ESuccess;
}


/************************************************************************/
/*protocol external interface*/

int ptl_init(void)
{
	LOGI("%s\n", __func__);

	char name[] = "no name";
	char key[] = "no key";

	g_client.timestamp = 1;
	g_client.srv_type = SRV_TYPE_NULL;
	g_client.port = SOCK_LISTEN_PORT;
	g_client.key_state = KEY_ILLEGAL;

	//memset(&g_client.service_cb, 0, sizeof(g_client.service_cb));

	init_deque(&g_client.send_msg);

	memset(g_client.ci.name, 0, sizeof(g_client.ci.name));
	memcpy(g_client.ci.name, name, sizeof(name));
	g_client.ci.name_len = strlen(name);

	memset(g_client.ci.key, 0, sizeof(g_client.ci.key));
	memcpy(g_client.ci.key, key, sizeof(key));
	g_client.ci.key_len = strlen(key);

	MUTEX_INIT(&g_client.srv_mutex);
	MUTEX_INIT(&g_client.deque_mutex);

	__create_event(&g_client.sync_event, &g_client.sync_mutex);
	__init_event(&g_client.send_event, &g_client.send_mutex, &g_client.send_cond);
	__init_event(&g_client.init_event, &g_client.init_mutex, &g_client.init_cond);

	g_client.image_data = malloc(MAX_IMAGE_SIZE);
	g_client.audio_data = malloc(MAX_AUDIO_SIZE);

	LOGD("%s finished!\n", __func__);
	return ESuccess;
}

int ptl_deinit(void)
{
	LOGI("%s\n", __func__);

	MUTEX_DEL(&g_client.srv_mutex);
	MUTEX_DEL(&g_client.deque_mutex);

	__destroy_event(&g_client.sync_event, &g_client.sync_mutex);
	__deinit_event(&g_client.send_event, &g_client.send_mutex, &g_client.send_cond);
	__deinit_event(&g_client.init_event, &g_client.init_mutex, &g_client.init_cond);

	if (g_client.image_data) free(g_client.image_data);
	if (g_client.audio_data) free(g_client.audio_data);

	return ESuccess;
}

int ptl_set_client_info(int width, int height, int dpi)
{
	MUTEX_LOCK(&g_client.srv_mutex);
	g_client.ci.width = width;
	g_client.ci.height = height;
	g_client.ci.dpi = dpi;
	MUTEX_UNLOCK(&g_client.srv_mutex);

	return ESuccess;
}

int ptl_connect_internal(char *srv_addr, char *name, char *key)
{
	int len;

	if (!srv_addr || !name || !key)
		return EFailed;

	LOGD("%s\n", __func__);

	/*set key*/
	len = strlen(key);
	if (len > MAX_KEY_CODE_SIZE) {
		LOGE("ERR: key's len %d more than %d\n", len, MAX_KEY_CODE_SIZE);
		goto CONNECT_ERROR;
	}
	memset(&g_client.ci.key, 0, MAX_KEY_CODE_SIZE);
	memcpy(g_client.ci.key, key, len);
	g_client.ci.key_len = len;

	/*set name*/
	len = strlen(name);
	if (len > MAX_CLIENT_NAME_SIZE) {
		LOGE("ERR: name's len %d more than %d\n", len, MAX_CLIENT_NAME_SIZE);
		goto CONNECT_ERROR;
	}
	memset(g_client.ci.name, 0, MAX_CLIENT_NAME_SIZE);
	memcpy(g_client.ci.name, name, len);
	g_client.ci.name_len = len;

	/*set server ip*/
    if (!srv_addr || strlen(srv_addr) > MAX_IPV4_SIZE)
		goto CONNECT_ERROR;

	memset(g_client.server_ip, 0, sizeof(g_client.server_ip));
	strncpy(g_client.server_ip, srv_addr, MAX_IPV4_SIZE);

    if (__connect_server(srv_addr, &g_client.client_sockfd, SOCK_LISTEN_PORT) != ESuccess)
		goto CONNECT_ERROR;

	g_client.send_running = 1;
	g_client.recv_running = 1;
	g_client.lstn_running = 1;

    if (create_transfer_threads() != ESuccess)
		goto CONNECT_ERROR;

    if (match_time() != ESuccess)
		goto CONNECT_ERROR;

	if (register_client() != ESuccess)
		goto CONNECT_ERROR;

	return ESuccess;


CONNECT_ERROR:
	LOGD("%s: CONNECT_ERROR\n", __func__);
	disconnect_server();
	return EFailed;
}

int ptl_disconnect(void)
{
	LOGD("%s\n", __func__);

	deregister_client();
	disconnect_server();

	g_client.send_running = 0;
	g_client.recv_running = 0;

	pthread_join(g_client.recv_handle, NULL);

	LOGD("%s finish\n", __func__);
    return ESuccess;
}

int ptl_disconnect_internal(void)
{
	LOGI("%s\n", __func__);
	__close_socket(g_client.client_sockfd);
	__clear_socket();

	g_client.send_running = 0;
	g_client.recv_running = 0;
    return ESuccess;
}

int ptl_set_media_type(char type)
{
	g_client.srv_type = type;
	return ESuccess;
}

static int ptl_setup_media(char type)
{
	int ret;
	SetupReq_st *send_pkt;
	char prev_type;

	prev_type = g_client.srv_type;
	g_client.srv_type = SRV_TYPE_NULL;

	send_pkt = (SetupReq_st *)malloc(sizeof(SetupReq_st));
	if (!send_pkt) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	send_pkt->hdr.type = MSG_SETUP;
	send_pkt->hdr.sub_type = 0;
	send_pkt->hdr.len = sizeof(SetupReq_st) - sizeof(PktHdr_st);
	send_pkt->hdr.client_id = g_client.client_id;
	send_pkt->srv_type = type;

	ret = async_send_msg(send_pkt, sizeof(*send_pkt));
	if (ret == EFailed) {
		free(send_pkt);
		return EFailed;
	}

	if(EFailed == __wait_event(&g_client.sync_event,
		&g_client.sync_mutex, COMMON_PKT_RES_TIMEOUT))
	{
		return EFailed;
	}

	__reset_event(&g_client.sync_event);
	if (g_client.srv_type == SRV_TYPE_MAX) {
		g_client.srv_type = type;
		return ESuccess;
	}
	else {
		g_client.srv_type = prev_type;
		return EFailed;
	}
}

int ptl_start_media(void)
{
	int ret;
	CtlReq_st *send_pkt;

	LOGD("%s, g_client.srv_type = %d\n", __func__, g_client.srv_type);

	/* WHAT??????????????????????????????????
	 * WHY???????????????????????????????????
	 * SET VALUE DIRECTLY shit!!!!!!!!!!!!!!!
	 */
	g_client.srv_type = SRV_TYPE_MAX;
	if (ptl_setup_media(g_client.srv_type) == EFailed)
		return EFailed;

	send_pkt = (CtlReq_st *)malloc(sizeof(CtlReq_st));
	if (!send_pkt) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	send_pkt->hdr.type = MSG_CONTROL;
	send_pkt->hdr.sub_type = CTL_START;
	send_pkt->hdr.len = sizeof(CtlReq_st) - sizeof(PktHdr_st);
	send_pkt->hdr.client_id = g_client.client_id;

	ret = async_send_msg(send_pkt, sizeof(*send_pkt));
	if (ret == EFailed) {
		free(send_pkt);
		return EFailed;
	}

	if(EFailed == __wait_event(&g_client.sync_event,
		&g_client.sync_mutex, COMMON_PKT_RES_TIMEOUT))
	{
		return EFailed;
	}

	__reset_event(&g_client.sync_event);

	return ESuccess;
}

int ptl_stop_media(void)
{
	int ret;
	CtlReq_st *send_pkt;

	send_pkt = (CtlReq_st *)malloc(sizeof(CtlReq_st));
	if (!send_pkt) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	send_pkt->hdr.type = MSG_CONTROL;
	send_pkt->hdr.sub_type = CTL_STOP;
	send_pkt->hdr.len = sizeof(CtlReq_st) - sizeof(PktHdr_st);
	send_pkt->hdr.client_id = g_client.client_id;

	ret = async_send_msg(send_pkt, sizeof(*send_pkt));
	if (ret == EFailed) {
		free(send_pkt);
		return EFailed;
	}

	if(EFailed == __wait_event(&g_client.sync_event,
		&g_client.sync_mutex, COMMON_PKT_RES_TIMEOUT))
	{
		return EFailed;
	}

	__reset_event(&g_client.sync_event);
	return ESuccess;
}


int ptl_send_key(MsgKey_st *key)
{
	int ret;
    char *buf;
	PktHdr_st *pkt_hdr;

	buf = (char *)malloc(sizeof(PktHdr_st) + sizeof(MsgKey_st));
	if (!buf) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	pkt_hdr = (PktHdr_st *)buf;
	pkt_hdr->type = MSG_RMT_INPUT;
	pkt_hdr->sub_type =RMT_INPUT_KEY;
	pkt_hdr->len = sizeof(MsgKey_st);
	pkt_hdr->client_id = g_client.client_id;

	memcpy(buf + sizeof(PktHdr_st), key, sizeof(MsgKey_st));

	ret = async_send_msg(buf, sizeof(PktHdr_st) + sizeof(MsgKey_st));
	if (ret == EFailed) {
		free(buf);
		return EFailed;
	}
    return ESuccess;
}

int ptl_send_mouse(MsgMouse_st *mouse)
{
	int ret;
	char *buf;
	PktHdr_st *pkt_hdr;

	buf = (char *)malloc(sizeof(PktHdr_st) + sizeof(MsgMouse_st));
	if (!buf) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	pkt_hdr = (PktHdr_st *)buf;
	pkt_hdr->type = MSG_RMT_INPUT;
	pkt_hdr->sub_type =RMT_INPUT_MOUSE;
	pkt_hdr->len = sizeof(MsgMouse_st);
	pkt_hdr->client_id = g_client.client_id;

	memcpy(buf + sizeof(PktHdr_st), mouse, sizeof(MsgMouse_st));

	ret = async_send_msg(buf, sizeof(PktHdr_st) + sizeof(MsgMouse_st));
	if (ret == EFailed) {
		free(buf);
		return EFailed;
	}
	return ESuccess;
}

int ptl_send_touch(MsgMulTouch_st *touch)
{
	int ret;
	char *buf;
	int buf_len;
	PktHdr_st hdr;
	int size, offset;

	buf_len = sizeof(PktHdr_st) + sizeof(touch_st) * touch->num + sizeof(touch->num);
	buf = (char *)malloc(buf_len);
	if (!buf) {
		LOGE("ERR: NO MEMORY\n");
		return EFailed;
	}

	hdr.type = MSG_RMT_INPUT;
	hdr.sub_type =RMT_INPUT_MULTOUCH;
	hdr.len = sizeof(touch_st) * touch->num + sizeof(touch->num);
	hdr.client_id = g_client.client_id;

	size = sizeof(PktHdr_st);
	memcpy(buf, &hdr, size);

	offset = size;
	size = sizeof(touch->num);
	memcpy(buf + offset, &touch->num, size);

	offset += size;
	size = sizeof(touch_st) * touch->num;
	memcpy(buf + offset, touch->pXY, size);

	ret = async_send_msg(buf, buf_len);
	if (ret == EFailed)
	{
		free(buf);
		return EFailed;
	}
	return ESuccess;
}

int ptl_get_protocol_status(void)
{
	return g_client.state;
}

/* external interface service -> protocol*/

void get_client_protocol(struct client_protocol *protocol)
{
	protocol->init_protocol		=	ptl_init;
	protocol->deinit_protocol	=	ptl_deinit;
	protocol->set_client_info	=	ptl_set_client_info;
	protocol->start_protocol	=	init_listen_thread;
	protocol->stop_protocol		=	ptl_disconnect;
	protocol->setup_media		=	ptl_set_media_type;
	protocol->start_media		=	ptl_start_media;
	protocol->stop_media		=	ptl_stop_media;
	protocol->key_input			=	ptl_send_key;
	protocol->touch_input		=	ptl_send_touch;
	protocol->mouse_input		=	ptl_send_mouse;
}

void set_client_service(struct service_client *client)
{
	LOGD("%s\n", __func__);
	memcpy(&g_client.service_cb, client, sizeof(struct service_client));
}

#ifdef WIN32
void ptl_reg_cb_copyimage(int (*copy_image)(const void *, int))
{
	g_client.service_cb.copy_image = copy_image;
}

void ptl_reg_cb_copyAudio(int (*copy_audio)(const void *, int))
{
	g_client.service_cb.copy_audio = copy_audio;
}
#endif
