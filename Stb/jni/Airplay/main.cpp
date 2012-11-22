#include "AirPlayServer.h"
#include "dnssd.h"
#include <unistd.h>

int main(int argc, char **argv)
{
	int listenPort = 36667;
	CStdString password = "";
	bool usePassword = false;
	const char *name = "AppleTV";
	unsigned short raop_port = 36667;
	const char hwaddr[] = { 0x48, 0x5d, 0x60, 0x7c, 0xee, 0x22 };

	dnssd_t *dnssd;

	if (CAirPlayServer::StartServer(listenPort, true)) {
		CAirPlayServer::SetCredentials(usePassword, password);
		dnssd = dnssd_init(NULL);
		dnssd_register_airplay(dnssd, name, raop_port, hwaddr, sizeof(hwaddr));
	}
	while(1)
		sleep(1);
	dnssd_unregister_raop(dnssd);
	dnssd_destroy(dnssd);
	return 0;
}
