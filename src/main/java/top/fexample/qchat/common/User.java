/**
 * @author Jacky Feng
 * @date 2024/3/17 16:34
 * @
 */
package top.fexample.qchat.common;

import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String userId;
    private String userPassword;
    private String requestType;

    public User(String userId, String userPassword) {
        this.userId = userId;
        this.userPassword = userPassword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestStatus) {
        this.requestType = requestStatus;
    }
}
