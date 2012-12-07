CXX=g++
obj=HttpParser.o md5.o AirTunesServer.o DllLibPlist.o dnssd.o utils.o
CXXFLAGS= -Wall -g
LDFLAGS= -ldl -pthread -ldns_sd

all:Airplay
Airplay:$(obj) main.o
	$(CXX) $(CFLAGS) -o $@ main.o $(obj) $(LDFLAGS)

clean:
	-rm -f *.o Airplay

.PHONY: clean all


