#include "common.h"
#include "protocol.h"
#include "protocol_server.h"
#include "communicator.h"


struct ptl_server g_server;
extern void _callback_notify_status(int connected);

static int get_client_id(int sock)
{
	int i;

	for(i = 0; i < SOCK_BACKLOG && g_server.ci[i].sock != sock; i++);
	if (i < SOCK_BACKLOG)
		return g_server.ci[i].id;
	else
		return EFailed;
}

static int set_client_id(int sock, int id)
{
	int i;

	for(i = 0; i < SOCK_BACKLOG && g_server.ci[i].sock != sock; i++);
	if (i < SOCK_BACKLOG)
		g_server.ci[i].id = id;
	else
		return EFailed;

	return ESuccess;
}

static int add_client_sock(int sock)
{
	int i ;

	for(i = 0; i < SOCK_BACKLOG && g_server.ci[i].sock; i++);
	if (i < SOCK_BACKLOG)
		g_server.ci[i].sock = sock;
	else
		return EFailed;

	return ESuccess;
}

static int del_client_sock(int sock)
{
	int i;

	for(i = 0; i < SOCK_BACKLOG && g_server.ci[i].sock != sock; i++);
	if (i < SOCK_BACKLOG)
		g_server.ci[i].sock = g_server.ci[i].id = 0;
	else
		return EFailed;

	return ESuccess;
}

static void send_clientinfo_res(char client_id, int fd, int key_len, int result)
{
	ClientInfoRes_st res;

	memset(&res, 0, sizeof(res));

	/*fill packet header*/
	res.hdr.type = MSG_CLIENT_INFO;
	res.hdr.len = sizeof(ClientInfoRes_st) - sizeof(PktHdr_st);
	res.hdr.client_id = client_id;

	res.keyVery = result < 0 ? KEY_ILLEGAL : KEY_APPROVED;

	if (res.keyVery < 0)
	{
		res.reason = key_len > 0 ? KEY_ERR : KEY_EMPTY;
	}
	else
	{
		res.reason = KEY_NO_ERR;
	}

	LOGI("send client key verify result = %d, reason = %d\n", res.keyVery, res.reason);
	__send_socket(fd, (char *)&res, sizeof(res), 0);
}

static int send_release_ind(char client_id, int fd)
{
	int ret;
	ServerCmdInd_st ind;

	memset(&ind, 0, sizeof(ServerCmdInd_st));

	ind.hdr.type = MSG_SERVER_CMD;
	ind.hdr.sub_type =CMD_TYPE_RELEASE;
	ind.hdr.len = sizeof(ServerCmdInd_st) - sizeof(PktHdr_st);
	ind.hdr.client_id = client_id;
	ind.reason = 1;

	LOGI("%s: release client %d\n", __func__, client_id);
	ret = __send_socket(fd, &ind, sizeof(ind), 0);

	if (ret < (int)sizeof(ind))
	{
		LOGE("%s: send release msg falure! send byte = %d\n", __func__, ret);
		return EFailed;
	}
	else
	{
		return ESuccess;
	}
}

static int send_release_res(char client_id, int fd)
{
	int ret;
	ClientSrvRes_st res;

	memset(&res, 0, sizeof(ClientSrvRes_st));

	res.hdr.type = MSG_CLIENT_SIG;
	res.hdr.sub_type = SIG_TYPE_RELEASE;
	res.hdr.len = sizeof(ClientSrvRes_st) - sizeof(PktHdr_st);
	res.hdr.client_id = client_id;
	res.result = 1;

	ret = __send_socket(fd, &res, sizeof(res), 0);

	if (ret < (int)sizeof(res))
	{
		LOGE("%s: send release msg falure! send byte = %d\n", __func__, ret);
		return EFailed;
	}
	else
	{
		return ESuccess;
	}
}

static void process_matchtime_req(char client_id, int sock)
{
	TimeRes_st time_sync;

	/*fill packet header*/
	memset(&time_sync, 0, sizeof(time_sync));
	time_sync.hdr.type = MSG_TIME_SYNC;
	time_sync.hdr.sub_type = 0;
	time_sync.hdr.len = (unsigned short)(sizeof(TimeRes_st) - sizeof(PktHdr_st));
	time_sync.hdr.client_id = client_id;
	time_sync.server_time = __get_system_mstime();

	LOGD("send client curr time %lld\n", time_sync.server_time);
	__send_socket(sock, &time_sync, sizeof(time_sync), 0);
}

static void process_input_req(PktHdr_st *hdr, const char *buf, int sock)
{
	int msg_subtype = hdr->sub_type;
	char client_id = hdr->client_id;

	if (msg_subtype == RMT_INPUT_KEY)
	{
		g_server.service_cb.client_key_event(client_id, (MsgKey_st *)buf);
	}
	else if (msg_subtype == RMT_INPUT_MOUSE)
	{
		g_server.service_cb.client_mouse_event(client_id, (MsgMouse_st *)buf);
	}
	else if (msg_subtype == RMT_INPUT_MULTOUCH)
	{
		MsgMulTouch_st touch;
		touch.num = *buf++;
		memcpy(touch.pXY, buf, sizeof(touch_st) * touch.num);
		g_server.service_cb.client_touch_event(client_id, &touch);
	}
	else
	{
		LOGE("Remote input event UNSUPPOTTED: %d\n", msg_subtype);
	}

	sock = sock;
}

static void process_setup_req(char client_id, const char *buf, int sock)
{
	SetupRes_st res;
	int ret;

	res.hdr.type = MSG_SETUP;
	res.hdr.len = sizeof(SetupRes_st) - sizeof(PktHdr_st);
	res.hdr.client_id = client_id;

	ret = g_server.service_cb.set_client_media_type(client_id, *buf);
	if (ret) {
		res.setup_ret = SETUP_SUCCESS;
	}
	else {
		res.setup_ret = SETUP_FAILED;
		res.err_code = 0x00;
	}

	__send_socket(sock, &res, sizeof(res), 0);
}

static void process_mmcontrol_req(PktHdr_st *hdr, const char *buf, int sock)
{
	CtlRes_st res;
	int ret;
	int client_id = hdr->client_id;

	res.hdr.type = MSG_CONTROL;
	res.hdr.sub_type = hdr->sub_type;
	res.hdr.len = sizeof(CtlRes_st) - sizeof(PktHdr_st);
	res.hdr.client_id = hdr->client_id;

	if (hdr->sub_type == CTL_START)
	{
		LOGI("startMediaTransfer client id = %d\n", client_id);
		ret = g_server.service_cb.start_client_transfer(client_id);
	}
	else
	{
		LOGI("stopMediaTransfer client id = %d\n", client_id);
		ret = g_server.service_cb.stop_client_transfer(client_id);
	}

	res.ctl_ret = (ret == 1) ? RES_SUCCESS : RES_FAILED;
	res.err_code = 0;
	__send_socket(sock, &res, sizeof(res), 0);

	buf = buf;
	return;
}

static void process_cmd_cnf(PktHdr_st *hdr, const char *buf, int sock)
{
	if (hdr->sub_type == CMD_TYPE_RELEASE)
	{
		__send_event(&g_server.sync_event, &g_server.sync_mutex);
	}

	buf = buf;
	sock = sock;
}

static void process_sig_req(PktHdr_st *hdr, const char *buf, int sock)
{
	if (hdr->sub_type == SIG_TYPE_RELEASE)
	{
		send_release_res(hdr->client_id, sock);
#if 0 //close socket by client
		__shutdown_socket(sock, SOCK_SHUT_RDWR);
#endif
	}

	buf = buf;
}

static int process_clientinfo_req(const char *buf, int sock)
{
	ClientInfo_st info;
	int size;
	int offset;
	int ret;

	memset(&info, 0, sizeof(info));

	offset = 0;
	size = sizeof(info.key_len);
	memcpy(&info.key_len, buf + offset, size);

	offset += size;
	size = info.key_len;
	memcpy(info.key, buf + offset, size);

	if(info.key_len > MAX_KEY_CODE_SIZE) {
		LOGE("Error key length %d more than %d. \n",
			info.key_len, MAX_KEY_CODE_SIZE);
		return EFailed;
	}

	offset += size;
	size = sizeof(info.name_len);
	memcpy(&info.name_len, buf + offset, size);

	offset += size;
	size = info.name_len;
	memcpy(info.name, buf + offset, size);

	if(info.name_len > MAX_CLIENT_NAME_SIZE) {
		LOGE("Error name length %d more than %d. \n",
			info.key_len, MAX_CLIENT_NAME_SIZE);
		return EFailed;
	}

	offset += size;
	size = sizeof(info.width) + sizeof(info.height) + sizeof(info.dpi);
	memcpy(&info.width, buf + offset, size);

	LOGI("name is \"%s\", key is \"%s\"\n", info.name, info.key);
	ret = g_server.service_cb.add_client((char *)&info.name, (char *)&info.key, sock);

	if (ret < 0)
	{
		LOGE("add client failure!\n");
	}
	else
	{
		LOGI("add client id = %d\n", ret);
		set_client_id(sock, ret);
	}

	send_clientinfo_res(ret, sock, info.key_len, ret);
	return ESuccess;
}

static void process_msg(PktHdr_st *hdr, const char *buf, int fd)
{
	int msg_type, msg_subtype;
	char client_id;
	int loop;

	msg_type = hdr->type;
	msg_subtype = hdr->sub_type;
	client_id = hdr->client_id;

	LOGI("--------------------------------\n");
	LOGI("%s: msg_type = %d, msg_subtype = %d, fd = %d, id = %d\n",
			__func__, msg_type, msg_subtype, fd, get_client_id(fd));

	for (loop = 0; loop < (int)sizeof(PktHdr_st); loop++)
		LOGD("Pkt_header: [%d]=0x%0X - %d\n", loop, *((char *)hdr + loop), *((char *)hdr + loop));

	for (loop = 0; loop < *(int*)((char *)hdr + 2); loop++)
		LOGD("Pkt_content: [%d]=0X%0X - %d - '%c'\n", loop, *(buf+loop), *(buf+loop), *(buf+loop));

	switch(msg_type)
	{
		case MSG_CLIENT_INFO:
			process_clientinfo_req(buf, fd);
			break;
		case MSG_TIME_SYNC:
			process_matchtime_req(client_id, fd);
			break;
		case MSG_RMT_INPUT:
			process_input_req(hdr, buf, fd);
			break;
		case MSG_SETUP:
			process_setup_req(client_id, buf, fd);
			break;
		case MSG_CONTROL:
			process_mmcontrol_req(hdr, buf, fd);
			break;
		case MSG_SERVER_CMD:
			process_cmd_cnf(hdr, buf, fd);
			break;
		case MSG_CLIENT_SIG:
			process_sig_req(hdr, buf, fd);
			break;
		default:
			LOGE("WARNING: unsupport message: type=%d,subtype=%d\n",
				msg_type, msg_subtype);
			break;
	}
}

static int send_invite_msg(char *msg)
{
	int ret;

	LOGD("%s: msg = '%s'\n", __func__, msg);

	ret = __send_socket(g_server.client_sock, msg, strlen(msg), 0);

	LOGD("__send_socket %d bytes \n", ret);
	return ret;
}

int invite_client(char *ip)
{
	LOGD("%s: ip = %s\n", __func__, ip);

	if (!ip)
		return -1;

    if (__connect_server(ip, &g_server.client_sock, SOCK_PRE_LISTEN_PORT) != ESuccess)
		goto CONNECT_ERROR;

	if (send_invite_msg("hi, date tonight!") != (sizeof("hi, date tonight")))
		goto CONNECT_ERROR;

	//__msleep(1000);
	//__close_socket(g_server.client_sock);

	LOGD("invite msg sent over!\n");
	return ESuccess;

CONNECT_ERROR:
	LOGD("%s: CONNECT_ERROR\n", __func__);
	__close_socket(g_server.client_sock);
	return EFailed;
}

#ifdef WIN32
static DWORD WINAPI listen_thread_loop(LPVOID arg)
#elif defined LINUX
static void *listen_thread_loop(void *arg)
#endif
{
	fd_set rdfds;
	int sock, max_sock, len;
	struct sockaddr_in client_addr;
	int i, ret;
	PktHdr_st pkt_hdr ;
	int read_len;
	char read_buf[MAX_RECV_SIZE];
	struct timeval timeout = {0, 0};

	max_sock = g_server.server_sock;

	while(g_server.listen_running)
	{
		FD_ZERO(&rdfds);
		FD_SET(g_server.server_sock, &rdfds);
		LOGD("\nserver sock = %d\n", g_server.server_sock);

		for (i = 0; i < SOCK_BACKLOG; i++)
		{
			if (g_server.ci[i].sock == 0)
				continue;
			LOGI("client sock = %d, id = %d\n", g_server.ci[i].sock, g_server.ci[i].id);
			FD_SET(g_server.ci[i].sock, &rdfds);
		}

		LOGD("listen select max_sock = %d - - - - - - - - - - - - \n", max_sock);
		//WARNING: timeout.tv_sec should be set "1" for release version
#ifdef _ISHARE_LOG_D
		timeout.tv_sec = 2;
#else
		timeout.tv_sec = 1;
#endif
		ret = select(max_sock + 1, &rdfds, (fd_set *)0, (fd_set *)0, &timeout);

		LOGD("select ret = %d\n", ret);
		if (ret < 0)
		{
			LOGE("Error select errno = %s\n", strerror(errno));
			return NULL;
		}

		/*check if exist new incoming client*/
		if (FD_ISSET(g_server.server_sock, &rdfds))
		{
			LOGD("process server sock ... \n");
			len = sizeof(struct sockaddr_in);
			sock = accept(g_server.server_sock, (struct sockaddr *)&client_addr, &len);

			if(sock > 0)
			{
				LOGD("New incoming client: %d\n", sock);
				add_client_sock(sock);
				__set_sock_noblock(sock);
				max_sock = (max_sock < sock) ? sock : max_sock;
				_callback_notify_status(1);
			}
			continue;
		}

		/*recv each client socket packet*/
		for (i = 0; i < SOCK_BACKLOG; i++)
		{
			int fd = g_server.ci[i].sock;

			if (FD_ISSET(fd, &rdfds))
			{
				LOGD("process client sock = %d\n", fd);
				read_len = __recv_socket(fd, (char *) &pkt_hdr, sizeof(PktHdr_st), 0);

				if (read_len < (int)sizeof(PktHdr_st))
				{
					LOGE("recv error: read pkt header length is less than expected\n");
					goto RECV_ERROR;
				}
				else
				{
					if (pkt_hdr.len)
					{
						read_len = __recv_socket(fd, read_buf, pkt_hdr.len, 0);
						if (read_len < pkt_hdr.len)
						{
							LOGE("recv error: read pkt content length is less than expected\n");
							goto RECV_ERROR;
						}
					}
					process_msg(&pkt_hdr, read_buf, fd);
					break;
				}

				RECV_ERROR:
				LOGE("remove client: sock =  %d, id = %d\n", fd, get_client_id(fd));
				g_server.service_cb.del_client(get_client_id(fd));
				del_client_sock(fd);
				FD_CLR(fd, &rdfds);
				__close_socket(fd);
				_callback_notify_status(0);
			}
		}
	}

	LOGD("%s: LISTEN_THREAD_ABORT\n", __func__);

	/** remove all of clients */
	for (i = 0; i < SOCK_BACKLOG; i++)
	{
		int fd = g_server.ci[i].sock;

		if (fd <= 0) continue;
		LOGE("remove client: sock = %d, id = %d\n", fd, get_client_id(fd));
		//g_server.service_cb.del_client(get_client_id(fd));
		del_client_sock(fd);
		__close_socket(fd);
	}

	/** shudown server */
	__close_socket(g_server.server_sock);

	arg = NULL; //dummy for compile
	return NULL;
}

/**************************************************************
 *I/F SrvManager
 **************************************************************/

int ptl_server_init(void)
{
	LOGI("%s\n", __func__);
	if (g_server.state != SER_STATE_NULL)
	{
		LOGE("%s: error state = %d\n", __func__, g_server.state);
		return EFailed;
	}

	g_server.state = SER_STATE_IDLE;
	g_server.server_sock = 0;
	memset(g_server.ci, 0, sizeof(g_server.ci));

	__create_event(&g_server.sync_event, &g_server.sync_mutex);

	g_server.send_buf = (char *)malloc(MAX_IMAGE_SIZE);
	if (!g_server.send_buf)
	{
		LOGE("%s: create send buffer failure!\n", __func__);
		return EFailed;
	}

	return ESuccess;
}

int ptl_server_deinit(void)
{
	LOGD("fadsfsdfsadfasdfasdfasdfasdf");
	LOGD("%s start\n", __func__);
	LOGD("%s\n", __func__);
	if (g_server.state != SER_STATE_IDLE)
	{
		LOGE("%s: error state = %d\n", __func__, g_server.state);
		return EFailed;
	}

	g_server.state = SER_STATE_NULL;
	g_server.listen_running = 0;

	if (g_server.send_buf)
{
LOGD("free start");
		free(g_server.send_buf);
LOGD("free end");
}

	LOGI("%s finish\n", __func__);
	return ESuccess;
}

int ptl_server_start(void)
{
	int ret;

	LOGI("%s\n", __func__);
	if (g_server.state != SER_STATE_IDLE)
	{
		LOGE("%s: error state = %d\n", __func__, g_server.state);
		return EFailed;
	}

	g_server.state = SER_STATE_STARTING;
	g_server.listen_running = 1;

	if (EFailed == __open_server_socket(&g_server.server_sock, SOCK_LISTEN_PORT))
		return EFailed;

	ret = __create_thread(&g_server.tid, listen_thread_loop, NULL);
	if (ret < 0) {
		return EFailed;
	}

	g_server.state = SER_STATE_START;
	return ESuccess;
}

int ptl_server_stop(void)
{
	LOGD("%s: start\n", __func__);

	if (g_server.state != SER_STATE_START)
	{
		LOGE("%s: error state = %d\n", __func__, g_server.state);
		return EFailed;
	}

	g_server.state = SER_STATE_STOPING;
	g_server.listen_running = 0;

	__wait_thread(g_server.tid);
	g_server.state = SER_STATE_IDLE;
	LOGD("%s: finish\n", __func__);
	return ESuccess;
}

int ptl_sendControlData(int sock, void *buf, int size)
{
	return __send_socket(sock, buf, size, 0);
}

int ptl_sendImageData(int sock, void *buf, int size, int client_id)
{
	PktHdr_st header;
	int hdr_len = sizeof(PktHdr_st);
	int ret;
	int offset = 5; //use offset filter dummy bytes of imageInfo
	char *p = (char *)buf;

	header.type = MSG_IMAGE_DATA;
	header.sub_type = 0;
	header.len = size - offset;
	header.client_id = client_id;

	memcpy(g_server.send_buf, &header, hdr_len);
	memcpy(g_server.send_buf + hdr_len, p + offset, size - offset);

	LOGV("%s: size = %d, client id = %d\n", __func__, size, client_id);
	ret = __send_socket(sock, g_server.send_buf, size - offset + hdr_len, 0);
	return ((ret - hdr_len) < 0) ? 0 : (ret - hdr_len) + offset;
}

int ptl_sendAudioData(int sock, void *buf, int size, int client_id)
{
	PktHdr_st header;
	int hdr_len = sizeof(PktHdr_st);
	int ret;

	header.type = MSG_AUDIO_DATA;
	header.sub_type = AUDIO_TYPE_PCM;
	header.len = size;
	header.client_id = client_id;

	memcpy(g_server.send_buf, &header, hdr_len);
	memcpy(g_server.send_buf + hdr_len, buf, size);

	LOGV("%s: size = %d, client id = %d\n", __func__, size, client_id);
	ret = __send_socket(sock, g_server.send_buf, size + hdr_len, 0);
	return ((ret - hdr_len) < 0) ? 0 : (ret - hdr_len);
}

int ptl_control_client(int client_id, char action, int sock)
{
	switch (action)
	{
	case CTL_CLIENT_REMOVE:
		LOGI("%s: remove client id = %d\n", __func__, client_id);
		if (ESuccess == send_release_ind(client_id, sock))
			__wait_event(&g_server.sync_event, &g_server.sync_mutex, COMMON_PKT_RES_TIMEOUT);
		break;
	default:
		LOGE("%s: VOID action! client id = %d, action = %d\n", __func__, client_id, action);
		break;
	}
	return 1;
}

void get_server_protocol(struct server_protocol *protocol)
{
	protocol->init_protocol		=	ptl_server_init;
	protocol->deinit_protocol	=	ptl_server_deinit;
	protocol->start_protocol	=	ptl_server_start;
	protocol->stop_protocol		=	ptl_server_stop;
	protocol->control_client	=	ptl_control_client;
	protocol->send_raw_data		=	ptl_sendControlData;
	protocol->send_image_data	=	ptl_sendImageData;
	protocol->send_audio_data	=	ptl_sendAudioData;
}

void set_server_service(struct service_server *server)
{
	memcpy(&g_server.service_cb, server, sizeof(struct service_server));
}
