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
}
