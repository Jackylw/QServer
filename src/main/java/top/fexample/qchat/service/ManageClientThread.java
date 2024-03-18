/**
 * @author Jacky Feng
 * @date 2024/3/18 15:10
 * @description 用于管理客户端的线程
 */
package top.fexample.qchat.service;

import java.util.HashMap;
import java.util.Iterator;

public class ManageClientThread {
    private static HashMap<String, ServerConnectClientThread> clientMap = new HashMap<>();

    public static void addClient(String userId, ServerConnectClientThread clientThread) {
        clientMap.put(userId, clientThread);
    }

    public static void removeClient(String userId) {
        clientMap.remove(userId);
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
}
