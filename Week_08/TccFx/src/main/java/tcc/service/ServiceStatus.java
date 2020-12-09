package tcc.service;

public enum ServiceStatus {
    STARTED,                    //已启动
    TRY_SUCCEEDED,              //try阶段成功
    TRY_FAILED,                 //try阶段失败
    TRY_UNKNOWN,                //try阶段未明
    CONFIRM_SUCCEEDED,          //confirm阶段成功
    CONFIRM_FAILED,             //confirm阶段失败
    CONFIRM_UNKNOWN,            //confirm阶段未明
    CANCEL_SUCCEEDED,           //cancel阶段成功
    CANCEL_FAILED,              //cancel阶段失败
    CANCEL_UNKNOWN,             //cancel阶段未明

    SENDING_FAILED,             //发送失败
    RECEIVE_FAILED;             //接收失败
}
