/**
 * @author Jacky Feng
 * @date 2024/3/18 14:37
 * @description 等待客户端连接，并且处理客户端消息
 */
package top.fexample.qchat.service;

import top.fexample.qchat.common.Message;
import top.fexample.qchat.common.MessageType;
import top.fexample.qchat.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {
    private ServerSocket serverSocket = null;

    // 使用一个集合模拟数据库
    private static HashMap<String, User> users = new HashMap<>();

    static {
        users.put("123", new User("123", "123"));
        users.put("456", new User("456", "456"));
        users.put("789", new User("789", "789"));
    }

    public Server() {
        System.out.println("Server start");
        try {
            serverSocket = new ServerSocket(9999);

            // 循环等待客户端连接
            while (true) {
                Socket socket = serverSocket.accept();

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                User user = (User) ois.readObject();

                // 发
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                Message message = new Message();

                // todo 验证用户,应使用数据库,这里先简单模拟
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
        return users.containsKey(userId) && users.get(userId).getUserPassword().equals(userPassword);
    }
}
