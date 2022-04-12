package com.ruoyi.quartz.ctp.event;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description  事件实体对象
 * @Author gongtan
 * @Date 2019/11/20 15:15
 * @Version 1.0
 **/
public class Event {

    //事件类型    ---  具体对应 EventType 中的类型
    private String eventType;

    // 字典用于保存具体的事件数据   key-事件类型    value-事件数据
    private Map<String, Object> eventDict;


    //提供空参  全参数的 构造函数
    public Event(String eventType, Map<String, Object> eventDict) {
        this.eventType = eventType;
        this.eventDict = eventDict;
    }

    //用于调用生成 事件
    public Event(String eventType) {
        // 事件类型
        this.eventType = eventType;
        // 字典用于保存具体的事件数据
        this.eventDict = new HashMap<String, Object>();
    }


    //get获取    set设置  调用方法
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getEventDict() {
        return eventDict;
    }

    public void setEventDict(Map<String, Object> eventDict) {
        this.eventDict = eventDict;
    }
}
