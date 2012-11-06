common = log.o communicator.o server.o client.o protocol.o multicast.o
server = server_test.o
client = client_test.o

CC = gcc
CFLAGS = -Wall -O2
LINK_LIB = -lpthread

ifeq ($(ARCH), win32)
win32_lib = ./pthread-win32
CC = i586-mingw32msvc-gcc
CFLAGS += -I$(win32_lib) -L$(win32_lib)
LINK_LIB = -lpthreadGC2 -lwsock32
POSTFIX = .exe
server += laser_point.o
endif

all: $(common) $(server) $(client)
	$(CC) -o server_test$(POSTFIX) $(CFLAGS) $(common) $(server) $(LINK_LIB)
	$(CC) -o client_test$(POSTFIX) $(CFLAGS) $(common) $(client) $(LINK_LIB)

.PHONY: clean
clean:
	rm -f server_test server_test.exe client_test client_test.exe *.o

