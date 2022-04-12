package com.ruoyi.quartz.ctp.event;

/**
 * @Description 事件类型描述
 * @Author gongtan
 * @Date 2019/11/20 15:16
 * @Version 1.0
 **/
public class EventType {
    // 计时器事件
    public static final String EVENT_TIMER = "eTimer";
    public static final String EVENT_LOG = "eLog"; // 日志事件，全局通用


    // Gateway接口 事件类型定义：
    public static final String EVENT_TICK = "eTick."; // TICK行情事件，可后接具体的vtSymbol
    public static final String EVENT_TRADE = "eTrade."; // 成交回报事件
    public static final String EVENT_ORDER = "eOrder."; // 报单回报事件
    public static final String EVENT_POSITION = "ePosition."; // 持仓回报事件
    public static final String EVENT_ACCOUNT = "eAccount."; // 账户回报事件
    public static final String EVENT_CONTRACT = "eContract."; // 合约基础信息回报事件
    public static final String EVENT_ERROR = "eError."; // 错误回报事件
    public static final String EVENT_HISTORY = "eHistory."; // K线数据查询回报事件
}
