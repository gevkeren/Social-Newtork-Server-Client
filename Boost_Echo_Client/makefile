CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_thread -lpthread

all: Client

Client: bin/ConnectionHandler.o bin/Client.o bin/echoClient.o bin/EncDec.o
	@echo 'Building target : Client'
	@echo 'Invoking:C++ Linker'
	g++ -o bin/BGSclient bin/ConnectionHandler.o bin/Client.o bin/EncDec.o $(LDFLAGS)
	@echo 'finished building target:Client'

bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/Client.o: src/Client.cpp
	g++ $(CFLAGS) -o bin/Client.o src/Client.cpp

bin/echoClient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp

bin/EncDec.o: src/EncDec.cpp
	g++ $(CFLAGS) -o bin/EncDec.o src/EncDec.cpp


.PHONY: clean
clean:
	rm -f bin/*