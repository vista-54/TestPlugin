package com.aaw.beaconsmanager;


import org.altbeacon.beacon.Region;

import java.io.Serializable;

public class ExtBeacon implements Serializable {

    static final long serialVersionUID = 1L;

    private int id;
    private String uuid;
    private String major;
    private String minor;
    //private int actionType;
    private MsgForType msgForEnter;
    private MsgForType msgForExit;
    private String data;
    private Region region;

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

//    public int getActionType() {
//        return actionType;
//    }
//
//    public void setActionType(int actionType) {
//        this.actionType = actionType;
//    }

//    public String getMsg() {
//        return msg;
//    }
//
//    public void setMsg(String msg) {
//        this.msg = msg;
//    }


    public MsgForType getMsgForEnter() {
        return msgForEnter;
    }

    public void setMsgForEnter(MsgForType msgForEnter) {
        this.msgForEnter = msgForEnter;
    }

    public MsgForType getMsgForExit() {
        return msgForExit;
    }

    public void setMsgForExit(MsgForType msgForExit) {
        this.msgForExit = msgForExit;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }


    public MsgForType getMsgForType(ActionType type){
        if(type == ActionType.enter){
            return getMsgForEnter();
        }
        if(type == ActionType.exit){
            return getMsgForExit();
        }
        return null;
    }


    public static class MsgForType implements Serializable{
        private String msg;
        private boolean show;
        private ActionType type;

        public MsgForType(String msg, boolean show, ActionType type){
            this.msg = msg;
            this.show = show;
            this.type = type;
        }

        public String getMsg() {
            return msg;
        }

        public boolean isShow() {
            return show;
        }

        public ActionType getType() {
            return type;
        }
    }


    public static enum ActionType implements Serializable{
        enter,
        exit
    }

}
