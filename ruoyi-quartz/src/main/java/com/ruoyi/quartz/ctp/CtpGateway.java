package com.ruoyi.quartz.ctp;


import com.ruoyi.quartz.ctp.event.EventEngine;
import com.ruoyi.quartz.ctp.trader.*;

/**
 * @Description   ctp的接口 实例化接口
 * @Author gongtan
 * @Date 2019/11/27 16:53
 * @Version 1.0
 **/
public class CtpGateway extends VtGateway {

    // 行情SPI
    private mdSpi_CTP mdSpi;

    // 交易SPI
    private tdSpi_CTP tdSpi;

    // 行情API连接状态，登录完成后为True
    private boolean mdConnected;

    // 行情API连接状态，登录完成后为True
    private boolean tdConnected;

    public void CtpGateway() throws Throwable {
        finalize();
    }

    static{
        //资源目录library.path
        System.out.println(System.getProperty("java.library.path"));
        //加载动态库
        //交易dll
        System.loadLibrary("thosttraderapi_se");
        System.loadLibrary("thosttraderapi_wrap");
        //行情dll
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("thostmduserapi_wrap");
    }

    public CtpGateway(EventEngine eventEngine) {
        this(eventEngine, "CTP");
    }

    /**
     * @Description  重写 CtpGateway  本类中调用
     * @author gt_vv
     * @date 2019/12/10
     * @param eventEngine
     * @param gatewayName
     * @return
     */
    public CtpGateway(EventEngine eventEngine, String gatewayName) {
        super(eventEngine, gatewayName);
//        eventEngine.start();
        //行情Api
        mdSpi = new mdSpi_CTP(this);
        tdSpi = new tdSpi_CTP(this);
        // 行情API连接状态，登录完成后为True
        this.mdConnected = false;
        this.tdConnected = false;
        // 循环查询
        this.setQryEnabled(true);
        this.setGatewayType(VtConstant.GATEWAYTYPE_FUTURES);
    }

  /*  // 连接
    @PostMapping("/login")
    public void connect(String userID,String password,String brokerID,String mdAddress, String authCode,String userProductInfo) {
        // 创建行情和交易接口对象
        this.mdSpi.connect(userID, password, brokerID, mdAddress);
       // this.tdSpi.connect(userID, password, brokerID, tdAddress, authCode, userProductInfo);
        // 初始化并启动查询
        // this.initQuery();
    }*/


    @Override
    public void connect() {
        String userID;
        String password;
        String brokerID;
        String tdAddress;
        String mdAddress;
        String authCode;
        String userProductInfo;

        // 创建行情和交易接口对象
        this.tdSpi.connect("193329","lihaib1225@","9999","tcp://180.168.146.187:10202","0000000000000000","simnow_client_test");
        //tcp://180.168.146.187:10130  7*24交易CTP
        //tcp://218.202.237.33:10102  simnow 实时行情
        this.mdSpi.connect("193329", "lihaib1225@", "9999", "tcp://180.168.146.187:10211");
        // 真实 服务器
        //tcp://180.168.146.187:10131   7*24 模拟环境
        //tcp://180.168.146.187:10110   真实环境

    }
    // 订阅行情
    @Override
    public void subscribe(VtSubscribeReq subscribeReq) {
        this.mdSpi.subscribe(subscribeReq);
    }

    //退定行情
    @Override
    public void unsubscribe(String symbol){
        this.mdSpi.unsubscribe(symbol);
    }

    //报单
    @Override
    public void sendOrder(VtOrderReq orderReq) {
        this.tdSpi.submitOrder(orderReq);
    }

    //撤单
    @Override
    public void cancelOrder(VtCancelOrderReq cancelOrderReq){
        this.tdSpi.cancelOrder(cancelOrderReq);
    }

    //账户信息
    @Override
    public void qryAccount() {
        this.tdSpi.queryAccount();
    }

    //持仓
    @Override
    public void qryPosition() {
        this.tdSpi.queryPosition();
    }

    //查询基础合约
    @Override
    public void qryContract() {
        this.tdSpi.qryContract();
    }
    @Override
    public void updateAccountPassword(String oldPassword,String newPassword){
        this.tdSpi.reqUserPasswordUpdate(oldPassword,newPassword);
    }
    @Override
    public void ReqTradingAccountPasswordUpdate(String oldPassword,String newPassword){
        this.tdSpi.ReqTradingAccountPasswordUpdate(oldPassword,newPassword);
    }

    private void login(){
        this.mdSpi.login();;
    }

    private void loginTd(){
        this.tdSpi.login();;
    }

    /**
     * @Description  测试main方法
     * @author gt_vv
     * @date 2019/12/11
     * @param args
     * @return void
     */
    public static void main(String[] args) {
        //EventEngine eventEngine = new EventEngine();
        //eventEngine.start(true);
        CtpGateway ctpGateway = new CtpGateway(null,"CTP");
        ctpGateway.connect();
    }

}
