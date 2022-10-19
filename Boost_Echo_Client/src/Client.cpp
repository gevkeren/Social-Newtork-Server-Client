//
// Created by spl211 on 08/01/2022.
//

#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include <thread>
#include <boost/thread.hpp>
#include "../include/EncDec.h"

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    std::cout << "running" << std:: endl;
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    EncDec encDec(connectionHandler);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    else{
//        std::cout << "connected" << std::endl;
    }
    std::thread threadWrite(&EncDec::readFromKeyboard, &encDec);
    std::thread threadRead(&EncDec::readFromServer, &encDec);
    threadWrite.join();
    threadRead.join();
    if (encDec.shouldTerminate){
        encDec.handler.close();
    }

}
