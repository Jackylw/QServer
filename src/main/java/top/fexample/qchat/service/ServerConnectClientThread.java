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

                if (message.getMsgType().equals(MessageType.GET_ONLINE_FRIEND)) {

                    System.out.println(message.getSender() + "请求获取在线好友列表");

                    String onlineUserList = ManageClientThread.getOnlineUserList();
                    Message messageOnline = new Message();
                    messageOnline.setMsgType(MessageType.RECEIVE_ONLINE_FRIEND);
                    messageOnline.setContent(onlineUserList);
                    messageOnline.setReceiver(message.getSender());

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(messageOnline);
                } else if (message.getMsgType().equals(MessageType.CLIENT_EXIT)) {

                    System.out.println(message.getSender() + "客户端退出");

                    // 从线程池中移除该用户,并关闭连接,结束循环
                    ManageClientThread.removeClient(message.getSender());
                    socket.close();
                    break;
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
