/**
 * @author Jacky Feng
 * @date 2024/3/18 15:03
 * @description 和某个客户端保持通讯
 */
package top.fexample.qchat.service;

import top.fexample.qchat.common.Message;
import top.fexample.qchat.common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;

public class ServerConnectClientThread extends Thread {
    private Socket socket;

    // 连接服务端的用户id
    private String userId;

    public ServerConnectClientThread(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("服务器端和客户端保持通信,等待读取数据");

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message) ois.readObject();

                // 若为客户端下线,则关闭连接,退出线程不在执行剩余代码
                // 若放在switch中,会多循环一次产生Socket异常
                if (message.getMsgType().equals(MessageType.CLIENT_EXIT)) {
                    socket.close();
                    ManageClientThread.removeClient(message.getSender());
                    break;
                }

                switch (message.getMsgType()) {
                    case MessageType.REQUEST_FRIEND:
                        // 转发好友请求
                        Message friendMessage = new Message();
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        friendMessage.setMsgType(MessageType.RECEIVE_FRIEND_REQUEST);
                        friendMessage.setContent(getFriendList());
                        oos.writeObject(friendMessage);
                        break;
                    case MessageType.COMMON_MESSAGE:
                        // 转发消息
                        // 用户上线转发，未上线存入数据库
                        if (ManageClientThread.getClient(message.getReceiver()) != null) {
                            ServerConnectClientThread receiverThread = ManageClientThread.getClient(message.getReceiver());
                            ObjectOutputStream oos1 = new ObjectOutputStream(receiverThread.getSocket().getOutputStream());
                            oos1.writeObject(message);
                        } else {
                            saveChatRecord(message);
                        }
                        break;
                    case MessageType.ADD_USER:
                        // 添加好友,Sender为请求方,Receiver为被请求方
                        Message addFriendMessage = new Message();
                        addFriendMessage.setSender(message.getSender());
                        addFriendMessage.setReceiver(message.getReceiver());
                        if (addFriend(message.getSender(), message.getReceiver())) {
                            addFriendMessage.setMsgType(MessageType.ADD_USER_SUCCESS);
                            ServerConnectClientThread senderThread = ManageClientThread.getClient(message.getSender());
                            ObjectOutputStream oos2 = new ObjectOutputStream(senderThread.getSocket().getOutputStream());
                            oos2.writeObject(addFriendMessage);
                            System.out.println(message.getSender() + "添加好友" + message.getReceiver() + "成功");
                        } else {
                            addFriendMessage.setMsgType(MessageType.ADD_USER_FAIL);
                            ServerConnectClientThread senderThread = ManageClientThread.getClient(message.getSender());
                            ObjectOutputStream oos3 = new ObjectOutputStream(senderThread.getSocket().getOutputStream());
                            oos3.writeObject(addFriendMessage);
                            System.out.println(message.getSender() + "添加好友" + message.getReceiver() + "失败");
                        }
                    default:
                        break;
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    // 获取好友列表
    public String getFriendList() {
        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT friend_id FROM friends_list WHERE user_id = ?");
            checkStmt.setString(1, userId);
            ResultSet resultSet = checkStmt.executeQuery();
            StringBuilder friendList = new StringBuilder();
            while (resultSet.next()) {
                friendList.append(resultSet.getString("friend_id")).append(" ");
            }
            return friendList.toString();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 将聊天记录存入数据库
    public void saveChatRecord(Message message) {
        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO chat_history (sender, receiver, content, time) VALUES (?, ?, ?, ?)");
            pstmt.setString(1, message.getSender());
            pstmt.setString(2, message.getReceiver());
            pstmt.setString(3, message.getContent());
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.executeUpdate();
            System.out.println(message.getSender() + "发送给" + message.getReceiver() + "的消息已存入数据库");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 添加好友方法
    public boolean addFriend(String userId, String friendId) {
        try (Connection conn = DatabaseService.getConnection()) {
            // 检查是否已经是好友
            if (checkFriend(userId, friendId)) {
                System.out.println(userId + "已经是好友" + friendId);
                return false;
            }
            // 检查好友是否是已注册的用户
            if (!checkFriendExist(friendId)) {
                System.out.println("好友" + friendId + "不存在");
                return false;
            }
            // 双向添加好友
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO friends_list (user_id, friend_id) VALUES (?, ?)");
            pstmt.setString(1, userId);
            pstmt.setString(2, friendId);
            pstmt.executeUpdate();
            PreparedStatement pstmtReverse = conn.prepareStatement("INSERT INTO friends_list (user_id, friend_id) VALUES (?, ?)");
            pstmtReverse.setString(1, friendId);
            pstmtReverse.setString(2, userId);
            pstmtReverse.executeUpdate();
            System.out.println(userId + "添加了好友" + friendId);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 检查是否已经是好友
    public boolean checkFriend(String userId, String friendId) {
        try (Connection conn = DatabaseService.getConnection()) {
            // 单向检查即可
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM friends_list WHERE user_id = ? AND friend_id = ?");
            checkStmt.setString(1, userId);
            checkStmt.setString(2, friendId);
            ResultSet resultSet = checkStmt.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 检查好友是否存在
    public boolean checkFriendExist(String friendId) {
        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM user WHERE user_id = ?");
            checkStmt.setString(1, friendId);
            ResultSet resultSet = checkStmt.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
