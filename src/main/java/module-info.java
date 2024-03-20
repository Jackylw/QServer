module top.fexample.qserver {
    requires javafx.controls;
    requires javafx.fxml;


    opens top.fexample.qchat to javafx.fxml;
    exports top.fexample.qchat;
}