//
// Created by gevk@wincs.cs.bgu.ac.il on 07/01/2022.
//

#ifndef ASSIGNMENT3_ENCDEC_H
#define ASSIGNMENT3_ENCDEC_H
#include "ConnectionHandler.h"
#include <string>
#include <iostream>
#include <boost/asio.hpp>

class EncDec {
private:

public:
    ConnectionHandler &handler;
    bool shouldTerminate;
    EncDec(ConnectionHandler &connectionHandler);

    void readFromKeyboard();
    void readFromServer();
};


#endif //ASSIGNMENT3_ENCDEC_H
