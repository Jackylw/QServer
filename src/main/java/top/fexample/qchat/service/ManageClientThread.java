/**
 * @author Jacky Feng
 * @date 2024/3/18 15:10
 * @description 用于管理客户端的线程
 */
package top.fexample.qchat.service;

import top.fexample.qchat.common.Message;
import top.fexample.qchat.common.MessageType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ManageClientThread {
    public static ConcurrentHashMap<String, ServerConnectClientThread> clientMap = new ConcurrentHashMap<>();

    public static void addClient(String userId, ServerConnectClientThread clientThread) {
        clientMap.put(userId, clientThread);
        ManageClientThread.updateFriendList();
        System.out.println(userId + ":用户上线,通知在线用户更新自己的好友列表");
    }

    public static void removeClient(String userId) {
        clientMap.remove(userId);
        ManageClientThread.updateFriendList();
        System.out.println(userId + ":用户下线,通知在线用户更新自己的好友列表");
    }

    public static ServerConnectClientThread getClient(String userId) {
        return clientMap.get(userId);
    }

    // 返回在线用户列表
    public static String getOnlineUserList() {
        // 集合遍历
        Iterator<String> it = clientMap.keySet().iterator();
        StringBuilder onlineUserList = new StringBuilder();
        while (it.hasNext()) {
            onlineUserList.append(it.next()).append(" ");
        }
        return String.valueOf(onlineUserList);
    }

    public static void updateFriendList() {
        // 获取在线用户列表,String用于发送在线用户列表
        String onlineUserListString = ManageClientThread.getOnlineUserList();

        for (String userId : clientMap.keySet()) {
            Message messageOnline = new Message();
            messageOnline.setMsgType(MessageType.RECEIVE_ONLINE_FRIEND);
            messageOnline.setContent(onlineUserListString);

            // 获取用户socket
            ServerConnectClientThread clientThread = ManageClientThread.getClient(userId);
            Socket onlineSocket = clientThread.getSocket();

            ObjectOutputStream oos;
            try {
                oos = new ObjectOutputStream(onlineSocket.getOutputStream());
                oos.writeObject(messageOnline);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
