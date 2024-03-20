module top.fexample.qserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens top.fexample.qchat to javafx.fxml;
    exports top.fexample.qchat;
}