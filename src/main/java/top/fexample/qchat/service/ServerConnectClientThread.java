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
import java.net.Socket;

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

                if (message.getMsgType().equals(MessageType.CLIENT_EXIT)) {

                    System.out.println(message.getSender() + "用户下线,通知在线用户更新自己的好友列表");

                    socket.close();
                    // 从线程池中移除该用户,并关闭连接
                    ManageClientThread.removeClient(message.getSender());

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
}
