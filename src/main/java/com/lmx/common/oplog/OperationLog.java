package com.lmx.common.oplog;

import com.google.gson.Gson;
import lombok.Data;

@Data
public class OperationLog {

    //应用ID appId
    private int appId;
    //应用名称 appName 对应 ES的index
    private String appName, skyWalkingTraceId,traceId, tableName, tableId, field;
    //日志级别
    private String loglevel;
    //创建时间
    private String cTime;
    //菜单id
    private String mId;
    //菜单名称
    private String mName;
    //操作人id
    private String opId;
    //操作人名称
    private String opName;
    //操作类型 默认修改
    private String operationType = "Modify";
    // 业务操作名称（对接口的概要描述）
    private String operationName;
    // 操作前值
    private String originalValue;
    // 操作后值
    private String modifyValue;
    //描述
    private String description;
    //登录时间
    private String logTime;
    //类型
    private String type;
    //公钥token
    private String publicKeyToken;
    //用户名称
    private String userName;

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    public String getcTime() {
        return cTime;
    }

    public void setcTime(String cTime) {
        this.cTime = cTime;
    }

    public String getmId() {
        return mId;
    }

    public void setmId(String mId) {
        this.mId = mId;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }

    public String getOpName() {
        return opName;
    }

    public void setOpName(String opName) {
        this.opName = opName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getModifyValue() {
        return modifyValue;
    }

    public void setModifyValue(String modifyValue) {
        this.modifyValue = modifyValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPublicKeyToken() {
        return publicKeyToken;
    }

    public void setPublicKeyToken(String publicKeyToken) {
        this.publicKeyToken = publicKeyToken;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
