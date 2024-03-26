/**
 * @author Jacky Feng
 * @date 2024/3/18 14:37
 * @description 等待客户端连接，并且处理客户端消息
 */
package top.fexample.qchat.service;

import top.fexample.qchat.common.Message;
import top.fexample.qchat.common.MessageType;
import top.fexample.qchat.common.User;
import top.fexample.qchat.common.UserType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashMap;

public class Server {
    private ServerSocket serverSocket = null;

    public Server() {
        System.out.println("Server start");
        try {
            serverSocket = new ServerSocket(9999);

            // 循环等待客户端连接
            while (true) {
                Socket socket = serverSocket.accept();

                // 读取客户端传来的User对象
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                User user = (User) ois.readObject();

                // 发送消息给客户端
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                Message message = new Message();

                // 根据客户端选择的请求类型处理:LOGIN/REGISTER/FIND_PASSWORD
                switch (user.getRequestType()) {
                    case UserType.LOGIN:
                        // 验证用户
                        if (checkUser(user.getUserId(), user.getUserPassword())) {
                            message.setMsgType(MessageType.LOGIN_SUCCESS);
                            oos.writeObject(message);

                            System.out.println("用户" + user.getUserId() + "密码" + user.getUserPassword() + "登录成功,建立相关线程");

                            // 创建线程和客户端通信,并添加到管理线程池中
                            ServerConnectClientThread clientThread = new ServerConnectClientThread(socket, user.getUserId());
                            clientThread.start();
                            ManageClientThread.addClient(user.getUserId(), clientThread);
                        } else {

                            System.out.println("用户" + user.getUserId() + "密码" + user.getUserPassword() + "登录失败");

                            message.setMsgType(MessageType.LOGIN_FAIL);
                            oos.writeObject(message);
                            socket.close();
                        }
                        break;
                    case UserType.REGISTER:
                        // 注册用户
                        String result = register(user.getUserId(), user.getUserPassword(), user.getSecurityQuestion(), user.getSecurityAnswer());
                        message.setMsgType(result);
                        oos.writeObject(message);

                        System.out.println("用户" + user.getUserId() + "密码" + user.getUserPassword() + "注册:" + result);

                        break;
                    case UserType.FIND_PASSWORD:
                        // todo 找回密码
                        break;
                    default:
                        throw new IllegalArgumentException("未知的请求类型");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭服务
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 验证用户
    private boolean checkUser(String userId, String userPassword) {
        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM user WHERE user_id = ? AND user_password = ?");
            pstmt.setString(1, userId);
            pstmt.setString(2, encryptPassword(userPassword));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // 注册用户
    public String register(String userId, String password, String securityQuestion, String securityAnswer) {
        try (Connection conn = DatabaseService.getConnection()) {
            // 开始数据库事务,关闭自动提交
            conn.setAutoCommit(false);

            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM user WHERE user_id = ? FOR UPDATE");
            checkStmt.setString(1, userId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                // 用户ID已存在，回滚事务并返回信息
                conn.rollback();
                return MessageType.REGISTER_EXIST;
            }

            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO user (user_id, user_password, user_question, user_answer,user_reg_date) VALUES (?, ?, ?, ?, ?)");
            pstmt.setString(1, userId);
            pstmt.setString(2, encryptPassword(password));
            pstmt.setString(3, securityQuestion);
            pstmt.setString(4, securityAnswer);
            pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            pstmt.executeUpdate();

            // 提交事务
            conn.commit();
            return MessageType.REGISTER_SUCCESS;
        } catch (SQLException e) {

            System.err.println("注册时发生了错误：" + e.getMessage());

            return MessageType.REGISTER_ERROR;
        }
    }

    // 加密密码的方法,默认MD5加密
    private String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(password.getBytes());
            BigInteger bigInteger = new BigInteger(1, bytes);
            StringBuilder result = new StringBuilder(bigInteger.toString(16));

            // 如果不足32位,前面补0
            while (result.length() < 32) {
                result.insert(0, "0");
            }

            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
