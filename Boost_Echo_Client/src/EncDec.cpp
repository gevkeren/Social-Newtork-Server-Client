//
// Created by gevk@wincs.cs.bgu.ac.il on 07/01/2022.
//

#include "EncDec.h"
#include <iostream>
#include <string>
#include <stdio.h>
#include <stdlib.h>
using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
EncDec::EncDec(ConnectionHandler &connectionHandler):
        shouldTerminate(false),
        handler(connectionHandler)
{
}
void EncDec::readFromKeyboard(){
    while (!this->shouldTerminate) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
        std::string line(buf);
        int len=line.length();
//        std::string toSend = handler.encoder(line);
        if (!handler.sendLine(line)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
//        std::cout << "Sent " << len+1 << " bytes to server" << std::endl;
//        std::cout << toSend << std::endl;
    }
}
void EncDec::readFromServer(){
    while (!this->shouldTerminate) {
        std::string answer;
        if (!handler.getLine(answer)) {
            this->shouldTerminate = true;
        }
        if (!answer.empty()){
//            std::cout << answer << std:: endl;
            answer = handler.decoder(answer);
            std:: cout << answer << std::endl;
            if (answer == "ack 3"){
                this->shouldTerminate = true;
                this->handler.close();
            }
        }
    }
}
