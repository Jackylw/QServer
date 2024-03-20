/**
 * @author Jacky Feng
 * @date 2024/3/17 16:46
 */
package top.fexample.qchat.common;

public interface MessageType {
    String CONNECT_SERVER_TIMEOUT = "CONNECT_SERVER_TIMEOUT";
    String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    String LOGIN_FAIL = "LOGIN_FAIL";
    String COMMON_MESSAGE = "COMMON_MESSAGE";
    String GET_ONLINE_FRIEND = "GET_ONLINE_FRIEND";
    String RECEIVE_ONLINE_FRIEND = "RECEIVE_ONLINE_FRIEND";
    String CLIENT_EXIT = "CLIENT_EXIT";
}