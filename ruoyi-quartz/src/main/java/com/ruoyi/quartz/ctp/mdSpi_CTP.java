package com.ruoyi.quartz.ctp;

import com.ruoyi.quartz.ctp.trader.VtConstant;
import com.ruoyi.quartz.ctp.trader.VtSubscribeReq;
import com.ruoyi.quartz.ctp.trader.VtTickData;
import ctp.thostmduserapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * @Description ctp  md 行情服务 接口实现类
 * @Author gongtan
 * @Date 2019/12/10 17:22
 * @Version 1.0
 **/
public class mdSpi_CTP extends CThostFtdcMdSpi {

    private final static Logger logger = LoggerFactory.getLogger(mdSpi_CTP.class);

    private String logInfo;

    // gateway对象
    private CtpGateway gateway;

    // gateway对象名称
    private String gatewayName;

    // 操作请求编号
    private int reqID;

    // 连接状态
    private boolean connectionStatus;

    // 登录状态
    public static boolean loginStatus;

    // 已订阅合约代码
    private Set<VtSubscribeReq> subscribedSymbols;

    // 账号
    private String userID;

    // 密码
    private String password;

    // 经纪商代码
    private String brokerID;

    // 服务器地址
    private String address;

    //交易日
    private String tradingDay;

    private CThostFtdcMdApi mdApi;

    /**
     * @Description  建立连接初始化时调用
     * @author gt_vv
     * @date 2019/12/10
     * @param gateway
     * @return
     */
    public mdSpi_CTP(CtpGateway gateway) {

        // gateway对象
        this.gateway = gateway;
        // gateway对象名称
        this.gatewayName = gateway.getGatewayName();
        // 操作请求编号
        this.reqID = 0;
        // 连接状态
        this.connectionStatus = false;
        // 登录状态
        loginStatus = false;

        // 已订阅合约代码
        this.subscribedSymbols = new HashSet<VtSubscribeReq>();
        // 账号
        this.userID = "";
        // 密码
        this.password = "";
        // 经纪商代码
        this.brokerID = "";
        // 服务器地址
        this.address = "";
    }



    /**
     * @Description
     * @author gt_vv
     * @date 2019/12/10
     * @param userID
     * @param password
     * @param brokerID
     * @param address
     * @return void
     */
    public void connect(String userID, String password, String brokerID, String address) {
        // 账号
        this.userID = userID;
        // 密码
        this.password = password;
        // 经纪商代码
        this.brokerID = brokerID;
        // 服务器地址
        this.address = address;

        // 如果尚未建立服务器连接，则进行连接
        if (!this.connectionStatus) {
            // 创建C++环境中的API对象，这里传入的参数是需要用来保存.con文件的文件夹路径
            File path = new File("temp/");
            if (!path.exists()) {
                path.mkdirs();
            }
            this.mdApi = CThostFtdcMdApi.CreateFtdcMdApi(path.getPath()+System.getProperty("file.separator"));
            this.mdApi.RegisterSpi(this);

            // 注册服务器地址
            this.mdApi.RegisterFront(this.address);

            // 初始化连接，成功会调用onFrontConnected
            this.mdApi.Init();
            this.mdApi.Join();
        }
        // 若已经连接但尚未登录，则进行登录
        else {
            if (!this.loginStatus) {
               this.login();
            }
        }
    }


    /**
     * @Description  登录
     * @author gt_vv
     * @date 2019/12/10
     * @param
     * @return void
     */
    public void login() {
        // 如果填入了用户名密码等，则登录
        if ((this.userID != null && !"".equals(this.userID.trim()))
                && (this.password != null && !"".equals(this.password.trim()))
                && (this.brokerID != null && !"".equals(this.brokerID.trim()))) {
            CThostFtdcReqUserLoginField req = new CThostFtdcReqUserLoginField();
            req.setUserID(this.userID);
            req.setPassword(this.password);
            req.setBrokerID(this.brokerID);
            this.reqID += 1;
            int i = this.mdApi.ReqUserLogin(req, this.reqID);
            System.out.println(this.gatewayName+"denglu"+i);
        }
    }

    /**
     * @Description  登录前执行
     * @author gt_vv
     * @date 2019/12/10
     * @param
     * @return void
     */
    @Override
    public void OnFrontConnected() {
        try {
            logger.warn(logInfo + "行情接口前置机已连接");
            // 修改前置机连接状态
            connectionStatus = true;
            login();
        } catch (Throwable t) {
            logger.error("{} OnFrontConnected Exception", logInfo, t);
        }
    }

    /**
     * @Description 前置机断开 会调用此方法
     * @author gt_vv
     * @date 2019/12/10
     * @param nReason
     * @return void
     */
    @Override
    public void OnFrontDisconnected(int nReason) {
        try {
            logger.warn("{}行情接口前置机已断开, 原因:{}", logInfo, nReason);
            //ctpGatewayImpl.disconnect();
        } catch (Throwable t) {
            logger.error("{} OnFrontDisconnected Exception", logInfo, t);
        }
    }

    // 返回接口状态
    public boolean isConnected() {
        return connectionStatus == true && loginStatus;
    }

    // 登录回报
    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            if (pRspInfo.getErrorID() == 0) {
                logger.info("{}OnRspUserLogin TradingDay:{},SessionID:{},BrokerId:{},UserID:{}", logInfo, pRspUserLogin.getTradingDay(),
                        pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(), pRspUserLogin.getUserID());
                // 修改登录状态为true
                loginStatus = true;
                //更新交易日
                tradingDay = pRspUserLogin.getTradingDay();
                logger.warn("{}行情接口获取到的交易日为{}", logInfo, tradingDay);
                ctp.thosttraderapi.CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new ctp.thosttraderapi.CThostFtdcSettlementInfoConfirmField();
                settlementInfoConfirmField.setBrokerID(brokerID);
                settlementInfoConfirmField.setInvestorID(userID);
                this.OnRtnDepthMarketData(new CThostFtdcDepthMarketDataField());
            } else {
                logger.warn("{}行情接口登录回报错误 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
            }

        } catch (Throwable t) {
            logger.error("{} OnRspUserLogin Exception", logInfo, t);
        }

    }

    // 订阅合约回报
    @Override
    public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (pRspInfo != null) {
            if (pRspInfo.getErrorID() == 0) {
                if (pSpecificInstrument != null) {
                    logger.info("{}行情接口订阅合约成功:{}", logInfo, pSpecificInstrument.getInstrumentID());
                } else {
                    logger.error("{}行情接口订阅合约成功,不存在合约信息", logInfo);
                }
            } else {
                logger.error("{}行情接口订阅合约失败,错误ID:{} 错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
            }
        } else {
            logger.info("{}行情接口订阅回报，不存在回报信息", logInfo);
        }
    }

    // 订阅合约
    public void subscribe(VtSubscribeReq subscribeReq) {
        // 这里的设计是，如果尚未登录就调用了订阅方法
        // 则先保存订阅请求，登录完成后会自动订阅
        if (loginStatus) {
            this.mdApi.SubscribeMarketData(new String[] { subscribeReq.getSymbol() }, 1);
        }
        this.subscribedSymbols.add(subscribeReq);
    }


    // 退订行情
    public boolean unsubscribe(String symbol) {
        subscribedSymbols.remove(symbol);
        if (isConnected()) {
            String[] symbolArray = new String[1];
            symbolArray[0] = symbol;
            try {
                mdApi.UnSubscribeMarketData(symbolArray, 1);
            } catch (Throwable t) {
                logger.error("{}行情退订异常,合约代码{}", logInfo, symbol, t);
                return false;
            }
            return true;
        } else {
            logger.warn("{}行情退订无效,行情服务器尚未连接成功,合约代码:{}", logInfo, symbol);
            return false;
        }
    }

    // 心跳警告
    @Override
    public void OnHeartBeatWarning(int nTimeLapse) {
        logger.warn(logInfo + "行情接口心跳警告 nTimeLapse:" + nTimeLapse);
    }

    // 错误回报
    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (pRspInfo != null) {
            logger.error("{}行情接口错误回报!错误ID:{},错误信息:{},请求ID:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg(), nRequestID);
        } else {
            logger.error("{}行情接口错误回报!不存在错误回报信息", logInfo);
        }
    }

    /**
     * @Description 合约行情推送
     * @author gt_vv+
     * @date 2019/12/10
     * @param pDepthMarketData
     * @return void
     */
    @Override
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
        // 过滤尚未获取合约交易所时的行情推送
        if (pDepthMarketData == null) {
            return;
        }
        // 创建对象
        VtTickData tick = new VtTickData();
        tick.setGatewayName(this.gatewayName);
        //tick.setExchange(CtpGlobal.symbolExchangeDict.get(tick.getSymbol()));
        // 行情代码
        tick.setSymbol(pDepthMarketData.getInstrumentID());
        // 交易所代码
        tick.setExchange(pDepthMarketData.getExchangeID());
        //lastPrice 最新价
        tick.setLastPrice(pDepthMarketData.getLastPrice());
        //Volume 当日成交量
        tick.setVolume(pDepthMarketData.getVolume());
        //持仓量
        tick.setOpenInterest((int) pDepthMarketData.getOpenInterest());
        // 最后修改时间 与 最后修改毫秒
        tick.setTime(pDepthMarketData.getUpdateTime() + "." + (pDepthMarketData.getUpdateMillisec() / 100));
        //成交金额
        tick.setTurnover(pDepthMarketData.getTurnover());
        // 上期所和郑商所可以直接使用，大商所需要转换
        tick.setDate(pDepthMarketData.getActionDay());
        // 大商所日期转换
        if (VtConstant.EXCHANGE_DCE.equals(tick.getExchange())) {
            tick.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        }
        // -----------------    常规行情    ---------------------------
        //今日开盘价
        tick.setOpenPrice(pDepthMarketData.getOpenPrice());
        //今日最高价
        tick.setHighPrice(pDepthMarketData.getHighestPrice());
        //今日最低价
        tick.setLowPrice(pDepthMarketData.getLowestPrice());
        //昨日收盘价
        tick.setPreClosePrice(pDepthMarketData.getPreClosePrice());
        //upperLimit涨停板
        tick.setUpperLimit(pDepthMarketData.getUpperLimitPrice());
        //跌停板
        tick.setLowerLimit(pDepthMarketData.getLowerLimitPrice());

        // CTP只有一档行情
        tick.setBidPrice1(pDepthMarketData.getBidPrice1());
        tick.setBidVolume1(pDepthMarketData.getBidVolume1());
        tick.setAskPrice1(pDepthMarketData.getAskPrice1());
        tick.setAskVolume1(pDepthMarketData.getAskVolume1());
        // -----------------    五档行情    ---------------------------
        // 上交所，SSE，股票期权相关
        if (VtConstant.EXCHANGE_SSE.equals(tick.getExchange())) {
            tick.setBidPrice2(pDepthMarketData.getBidPrice2());
            tick.setBidVolume2(pDepthMarketData.getBidVolume2());
            tick.setAskPrice2(pDepthMarketData.getAskPrice2());
            tick.setAskVolume2(pDepthMarketData.getAskVolume2());

            tick.setBidPrice3(pDepthMarketData.getBidPrice3());
            tick.setBidVolume3(pDepthMarketData.getBidVolume3());
            tick.setAskPrice3(pDepthMarketData.getAskPrice3());
            tick.setAskVolume3(pDepthMarketData.getAskVolume3());

            tick.setBidPrice4(pDepthMarketData.getBidPrice4());
            tick.setBidVolume4(pDepthMarketData.getBidVolume4());
            tick.setAskPrice4(pDepthMarketData.getAskPrice4());
            tick.setAskVolume4(pDepthMarketData.getAskVolume4());

            tick.setBidPrice5(pDepthMarketData.getBidPrice5());
            tick.setBidVolume5(pDepthMarketData.getBidVolume5());
            tick.setAskPrice5(pDepthMarketData.getAskPrice5());
            tick.setAskVolume5(pDepthMarketData.getAskVolume5());

            tick.setDate(pDepthMarketData.getTradingDay());
        }
        System.out.println(tick.toString());
        System.out.println("tick数据");
        //this.gateway.onTick(tick);
    }
}
