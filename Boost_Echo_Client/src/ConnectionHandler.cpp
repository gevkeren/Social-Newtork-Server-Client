#include <ConnectionHandler.h>
#include <ctime>
#include <iostream>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <deque>

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
using namespace std;

ConnectionHandler::ConnectionHandler(string host, short port):
        host_(host),
        port_(port),
        io_service_(),
        socket_(io_service_)
{
}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to "
              << host_ << ":" << port_ << std::endl;
    try {
        tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
        socket_.connect(endpoint, error);
        if (error)
            throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
            tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);
        }
        if(error)
            throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
        if(error)
            throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getLine(std::string& line) {
    return getFrameAscii(line, ';');
}

bool ConnectionHandler::sendLine(std::string& line) {
    return sendFrameAscii(line, '\n');
}
//bool ConnectionHandler::send(std::string toSend) {
//    string str = "";
//    for (int i = 0; i < toSend.size(); i++){
//        string s = toSend.front();
//        toSend.pop_front();
//        str = str + s;
//    }
//}

bool ConnectionHandler::getFrameAscii(std::string& frame, char delimiter) {
    char ch;
    // Stop when we encounter the null character.
    // Notice that the null character is not appended to the frame string.
    try {
        do{
            getBytes(&ch, 1);
            frame.append(1, ch);
        }while (delimiter != ch);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendFrameAscii(const std::string& frame, char delimiter) {
    bool result=sendBytes(frame.c_str(),frame.length());
    if(!result) return false;
    return sendBytes(&delimiter,1);
}

// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}

void ConnectionHandler:: shortToBytes(short num, char* bytesArr)
{
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}
short ConnectionHandler:: bytesToShort(char* bytesArr)
{
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}
char* ConnectionHandler::appendCharToCharArr(char* old, char a){
    size_t len = strlen(old);
    char* newCharArr = new char[len+1];
    strcpy(newCharArr, old);
    newCharArr[len] = a;
    char* tmp = old;//not to lose the pointer
    old = newCharArr;//new assignment
    delete[] tmp;//deleting old assignment
    return old;
}
char* ConnectionHandler::clearCharArray(char* toClear){
//    size_t len = strlen(toClear);
    char* newCharArr = new char[0];
    strcpy(newCharArr, toClear);
    char* tmp = toClear;//not to lose the pointer
    toClear = newCharArr;//new assignment
    delete[] tmp;//deleting old assignment
    return toClear;
}

std::string ConnectionHandler:: decoder(string& decodeResult){
    //ACK, ERROR or NOTIFICATION
    //10 1; //2
    string temp = decodeResult;
    int findSpace = temp.find(" ");
    string opString = temp.substr(0, findSpace);
    temp = temp.substr(findSpace + 1);
//    std::cout << decodeResult << std:: endl;
    int opCode = stoi(opString);
    if (opCode == 9){
        decodeResult = "NOTIFICATION ";
    }
    else{
        if (opCode == 10){
            decodeResult = "ACK ";
        }
        else{//opCode == 11
            decodeResult = "ERROR ";
        }
    }
    if (opCode == 10 || opCode == 11){//ACK or ERROR
        string rest = temp.substr(0, temp.length() - 1);
        decodeResult = decodeResult + rest;
    }
    else {//NOTIFICATION
        string typeStr = temp.substr(0, 1);
        if (stoi(typeStr) == 0){
            decodeResult = decodeResult + "PM ";
        }
        else{
            decodeResult = decodeResult + "POST ";
        }
        decodeResult = decodeResult + temp.substr(0, temp.length() - 1);
    }
//    std::cout<<decodeResult<<std::endl;
    return decodeResult;
}
std::string ConnectionHandler:: encoder(string& frame){
    std::string toSend;
    string temp = frame;
    short opCode;
    size_t firstPos = frame.find(" ");
    string firstStr = frame.substr(0,firstPos);
    if (firstStr == "REGISTER"){
        opCode = 1;
    }
    if (firstStr == "LOGIN"){
        opCode = 2;
    }
    if (firstStr == "LOGOUT"){
        opCode = 3;
    }
    if (firstStr == "FOLLOW"){
        opCode = 4;
    }
    if (firstStr == "POST"){
        opCode = 5;
    }
    if (firstStr == "PM"){
        opCode = 6;
    }
    if (firstStr == "LOGSTAT"){
        opCode = 7;
    }
    if (firstStr == "STAT"){
        opCode = 8;
    }
    if (firstStr == "NOTIFICATION"){
        opCode = 9;
    }
    if (firstStr == "ACK"){
        opCode = 10;
    }
    if (firstStr == "ERROR"){
        opCode = 11;
    }
    if (firstStr == "BLOCK"){
        opCode = 12;
    }
    toSend = toSend + to_string(opCode);
    //all the commands which doesn't need more information other than the opCode:
    //LOGOUT, LOGSTAT
    if (opCode == 3 || opCode == 7){
        toSend = toSend + ";";
        return toSend;
    }
    //REGISTER or LOGIN (Both needs username and password, avoiding code duplications
    //opcode\0username\0password(\0date);
    if (opCode == 1 || opCode == 2){
        toSend = toSend + " ";
        temp.erase(0, firstPos + 1);
        size_t namePos = temp.find(" ");
        string userName = temp.substr(0, namePos);
        toSend = toSend + userName + '\0';

        temp.erase(0, namePos + 1);
        size_t passPos = temp.find(" ");
        string password = temp.substr(0, passPos);
        toSend = toSend + password + '\0';

        if (opCode == 1) {//Register
            sendBytes("\0", 1);
            temp.erase(0, passPos + 1);
            size_t datePos = temp.find('\0');//"REGISTER gev qwe123 26-7-95\0"
            string date = temp.substr(0, datePos);
            toSend = toSend + date + '\0';
            toSend = toSend + ";";
            return toSend;
        }
        else{//login - adding captcha
            sendBytes("\0", 1);
            temp.erase(0, passPos + 1);
            size_t captchaPos = temp.find('\0');
            string captcha = temp.substr(0, captchaPos);
            toSend = toSend + captcha;
            toSend = toSend + ";";
            return toSend;
        }
    }
    //FOLLOW/UNFOLLOW
    if (opCode == 4){
        temp.erase(0, firstPos + 1);
        short op;
        if (temp.substr(0,1) == "0"){//follow
            op = 0;
        }
        else {
            op = 1; // temp.substr(0, 1) == "1" - unfollow
        }
        toSend = toSend + to_string(op);
        temp.erase(0, 2);
        size_t userNamePos = temp.find('\0');
        string userName = temp.substr(0, userNamePos);
        toSend = toSend + userName + '\0';
        toSend = toSend + ";";
        return toSend;
    }
    //POST or STAT or BLOCK
    //the string is either the post content or the usernames list (for stat ot block), server's problem
    if (opCode == 5 || opCode == 8 || opCode == 12){
        temp.erase(0, firstPos + 1);
        size_t contentPos = temp.find('\0');
        string content = temp.substr(0, contentPos);
        toSend = toSend + content + '\0';
        toSend = toSend + ";";
        return toSend;
    }
    //PM
    if (opCode == 6){
        temp.erase(0, firstPos + 1);
        size_t userNamePos = temp.find('\0');
        string userName = temp.substr(0, userNamePos);
        toSend = toSend + userName + '\0';

        temp.erase(0, userNamePos + 1);
        size_t contentPos = temp.find(" ");
        string content = temp.substr(0, contentPos);
        toSend = toSend + content + '\0';

        time_t now = time(0);
        tm *ltm = localtime(&now);
        int day = ltm->tm_mday;
        string sDay;
        if (day < 10){
            sDay = "0" + std::to_string(day);
        }
        else{
            sDay = std::to_string(day);
        }
        int month = 1 + (ltm->tm_mon);
        string sMonth;
        if (month < 10){
            sMonth = "0" + std::to_string(month);
        }
        else{
            sMonth = std::to_string(month);
        }
        int year = 1900 + (ltm->tm_year);
        string sYear = std::to_string(year);
        int hour = ltm->tm_hour;
        string sHour;
        if (hour < 10){
            sHour = "0" + std::to_string(hour);
        }
        else{
            sHour = std::to_string(hour);
        }
        int minutes = ltm->tm_min;
        string sMinutes;
        if (minutes < 10){
            sMinutes = "0" + std::to_string(minutes);
        }
        else{
            sMinutes = std::to_string(minutes);
        }
        string dateAndTime = sDay + "-" + sMonth + "-" + sYear + " " + sHour + ":" + sMinutes;
        toSend = toSend + dateAndTime + '\0';
        toSend = toSend + ";";
        return toSend;
    }
    return nullptr;
}
