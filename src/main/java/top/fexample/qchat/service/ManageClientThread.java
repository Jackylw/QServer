/**
 * @author Jacky Feng
 * @date 2024/3/18 15:10
 * @description 用于管理客户端的线程,以及线程变化时的操作
 */
package top.fexample.qchat.service;

import top.fexample.qchat.common.Message;
import top.fexample.qchat.common.MessageType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ManageClientThread {
    public static ConcurrentHashMap<String, ServerConnectClientThread> clientMap = new ConcurrentHashMap<>();

    public static void addClient(String userId, ServerConnectClientThread clientThread) {
        clientMap.put(userId, clientThread);
        ManageClientThread.updateFriendList();
        System.out.println(userId + ":用户上线,通知在线用户更新自己的好友列表");
        System.out.println("检查是否有未接收消息");
        putChatRecord(userId);
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

    // 更新好友列表
    public static void putChatRecord(String receiver) {
        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM chat_history WHERE receiver = ?");
            pstmt.setString(1, receiver);
            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                Message message = new Message();
                message.setMsgType(MessageType.COMMON_MESSAGE);
                message.setSender(resultSet.getString("sender"));
                message.setReceiver(resultSet.getString("receiver"));
                message.setContent(resultSet.getString("content"));
                message.setTime(String.valueOf(resultSet.getTimestamp("time")));

                // 发送消息
                ServerConnectClientThread receiverThread = ManageClientThread.getClient(message.getReceiver());
                ObjectOutputStream oos1 = new ObjectOutputStream(receiverThread.getSocket().getOutputStream());
                oos1.writeObject(message);
                System.out.println("发送给" + message.getReceiver() + "的消息已发送");

                // 删除已发送的消息
                PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM chat_history WHERE receiver = ?");
                deleteStmt.setString(1, receiver);
                deleteStmt.executeUpdate();
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
