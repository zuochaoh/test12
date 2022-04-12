package com.ruoyi.quartz.ctp;

import com.ruoyi.common.enums.PriceTypeEnum;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ctp.Text;
import com.ruoyi.quartz.ctp.trader.*;
import com.ruoyi.system.domain.KLineData;
import com.ruoyi.system.service.IKLineDataService;
import ctp.thosttraderapi.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Description
 * @Author gongtan
 * @Date 2019/12/11 14:39
 * @Version 1.0
 **/
public class tdSpi_CTP extends CThostFtdcTraderSpi {

    private final static Logger logger = LoggerFactory.getLogger(tdSpi_CTP.class);
    private String logInfo;

    private CtpGateway gateway; // gateway对象
    private String gatewayName; // gateway对象名称

    private int reqID; // 操作请求编号
    private int orderRef; // 订单编号

    private boolean connectionStatus; // 连接状态
    private boolean loginStatus; // 登录状态
    private boolean authStatus; // 验证状态
    private boolean loginFailed; // 登录失败（账号密码错误）

    private String userID; // 账号
    private String password; // 密码
    private String brokerID; // 经纪商代码
    private String address; // 服务器地址
    private String authCode;
    private String userProductInfo;

    private String tradingDay;//交易日
    private String investorName;//投资者

    //撤单所需变量
    private int frontID; // 前置机编号
    private int sessionID; // 会话编号

    //持仓缓存   vt系统唯一编码为key
    private Map<String, VtPositionData> posDict;
    // 保存合约代码和交易所的印射关系
    private Map<String, String> symbolExchangeDict;
    // 保存合约代码和合约大小的印射关系
    private Map<String, Integer> symbolSizeDict;

    private boolean requireAuthentication;

    //交易API实例
    private CThostFtdcTraderApi tdApi;

    /**
     * @param gateway
     * @return
     * @Description 创建单例时 初始化
     * @author gt_vv
     * @date 2019/12/11
     */
    public tdSpi_CTP(CtpGateway gateway) {
        this.gateway = gateway; // gateway对象
        this.gatewayName = gateway.getGatewayName(); // gateway对象名称

        this.reqID = 0; // 操作请求编号
        this.orderRef = 0; // 订单编号

        this.connectionStatus = false; // 连接状态
        this.loginStatus = false; // 登录状态
        this.authStatus = false; // 验证状态
        this.loginFailed = false; // 登录失败（账号密码错误）

        this.userID = ""; // 账号
        this.password = ""; // 密码
        this.brokerID = ""; // 经纪商代码
        this.address = ""; // 服务器地址

        this.frontID = 0; // 前置机编号
        this.sessionID = 0; // 会话编号

        this.posDict = new ConcurrentHashMap<String, VtPositionData>();
        this.symbolExchangeDict = new ConcurrentHashMap<String, String>(); // 保存合约代码和交易所的印射关系
        this.symbolSizeDict = new ConcurrentHashMap<String, Integer>(); // 保存合约代码和合约大小的印射关系

        this.requireAuthentication = false;
    }


    // 初始化连接
    public void connect(String userID, String password, String brokerID, String address, String authCode,
                        String userProductInfo) {
        this.userID = userID; // 账号
        this.password = password; // 密码
        this.brokerID = brokerID; // 经纪商代码
        this.address = address; // 服务器地址
        this.authCode = authCode; // 验证码
        this.userProductInfo = userProductInfo; // 产品信息

        // 如果尚未建立服务器连接，则进行连接
        if (!this.connectionStatus) {
            // 创建C++环境中的API对象，这里传入的参数是需要用来保存.con文件的文件夹路径
            File path = new File("temp/");
            if (!path.exists()) {
                path.mkdirs();
            }
            //创建  tdApi（单例）
            this.tdApi = CThostFtdcTraderApi.CreateFtdcTraderApi(path.getPath() + System.getProperty("file.separator"));
            //注册一事件处理的实例    参数：实现了 CThostFtdcTraderSpi 接口的实例指针
            this.tdApi.RegisterSpi(this);

            // 设置数据同步模式为推送从今日开始所有数据
            this.tdApi.SubscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESTART);
            this.tdApi.SubscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESTART);

            // 注册服务器地址
            this.tdApi.RegisterFront(this.address);

            // 初始化连接，成功会调用onFrontConnected
            this.tdApi.Init();
            this.tdApi.Join();
        }
        // 若已经连接但尚未登录，则进行登录
        else {
            //是否需要验证和验证状态
            if (this.requireAuthentication && !this.authStatus) {
                this.authenticate();
            } else if (!this.loginStatus) {
                this.login();
            }
        }
    }

    // 连接服务器
    public void login() {
        // 如果之前有过登录失败，则不再进行尝试
        if (this.loginFailed) {
            return;
        }
        // 如果填入了用户名密码等，则登录
        if ((this.userID != null && !"".equals(this.userID.trim()))
                && (this.password != null && !"".equals(this.password.trim()))
                && (this.brokerID != null && !"".equals(this.brokerID.trim()))) {
            CThostFtdcReqUserLoginField req = new CThostFtdcReqUserLoginField();
            req.setUserID(this.userID);
            req.setPassword(this.password);
            req.setBrokerID(this.brokerID);
            this.reqID += 1;
            //ReqUserLogin调用后ctp执行前置机连接
            this.tdApi.ReqUserLogin(req, this.reqID);
        }
    }


    /**
     * @param
     * @return void
     * @Description 申请验证
     * @author gt_vv
     * @date 2019/12/11
     */
    private void authenticate() {
        if ((this.userID != null && !"".equals(this.userID.trim()))
                && (this.brokerID != null && !"".equals(this.brokerID.trim()))
                && (this.authCode != null && !"".equals(this.authCode.trim()))
                && (this.userProductInfo != null && !"".equals(this.userProductInfo.trim()))) {
            CThostFtdcReqAuthenticateField req = new CThostFtdcReqAuthenticateField();
            req.setUserID(this.userID);
            req.setBrokerID(this.brokerID);
            req.setAuthCode(this.authCode);
            req.setUserProductInfo(this.userProductInfo);
            this.reqID += 1;
            logger.warn("交易接口申请验证");
            this.tdApi.ReqAuthenticate(req, this.reqID);
            logger.warn("交易接口发送登录请求成功");
        }
    }

    /**
     * @param
     * @return void
     * @Description 前置机联机回报
     * @author gt_vv
     * @date 2019/12/11
     */
    @Override
    public void OnFrontConnected() {
        try {
            logger.warn("{}交易接口前置机已连接", logInfo);
            // 修改前置机连接状态
            connectionStatus = true;
            //调用认证
            this.authenticate();
        } catch (Throwable t) {
            logger.error("{}OnFrontConnected Exception", logInfo, t);
        }
    }


    /**
     * @param pRspAuthenticateField
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     * @Description 验证客户端回报
     * @author gt_vv
     * @date 2019/12/11
     */
    @Override
    public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            if (pRspInfo != null) {
                if (pRspInfo.getErrorID() == 0) {
                    logger.warn(logInfo + "交易接口客户端验证成功");
                    CThostFtdcReqUserLoginField reqUserLoginField = new CThostFtdcReqUserLoginField();
                    reqUserLoginField.setBrokerID(this.brokerID);
                    reqUserLoginField.setUserID(this.userID);
                    reqUserLoginField.setPassword(this.password);
                    tdApi.ReqUserLogin(reqUserLoginField, reqID);
                } else {
                    logger.error("{}交易接口客户端验证失败 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                    loginFailed = true;
                }
            } else {
                loginFailed = true;
                logger.error("{}处理交易接口客户端验证回报错误,回报信息为空", logInfo);
            }
        } catch (Throwable t) {
            loginFailed = true;
            logger.error("{}处理交易接口客户端验证回报异常", logInfo, t);
        }
    }

    /**
     * @param pRspUserLogin
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     * @Description 登录回报
     * @author gt_vv
     * @date 2019/12/11
     */
    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            if (pRspInfo.getErrorID() == 0) {
                logger.warn("{}交易接口登录成功 TradingDay:{},SessionID:{},BrokerID:{},UserId:{}", logInfo, pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(),
                        pRspUserLogin.getBrokerID(), pRspUserLogin.getUserID());
                sessionID = pRspUserLogin.getSessionID();
                frontID = pRspUserLogin.getFrontID();
                // 修改登录状态为true
                loginStatus = true;
                tradingDay = pRspUserLogin.getTradingDay();
                logger.warn("{}交易接口获取到的交易日为{}", logInfo, tradingDay);

                // 确认结算单
                CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new CThostFtdcSettlementInfoConfirmField();
                settlementInfoConfirmField.setBrokerID(brokerID);
                settlementInfoConfirmField.setInvestorID(userID);
                reqID++;
                //会执行结算回报
                tdApi.ReqSettlementInfoConfirm(settlementInfoConfirmField, reqID);
                this.queryAccount();
                //this.OnRspQryInstrument(null,null,0,true);
                //CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast
            } else {
                logger.error("{}交易接口登录回报错误 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                loginFailed = true;
            }
        } catch (Throwable t) {
            logger.error("{}交易接口处理登录回报异常", logInfo, t);
            loginFailed = true;
        }
    }


    /**
     * @param pSettlementInfoConfirm
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     * @Description API投资者结算（结算在OnRspUserLogin中被调用） 被调用后  spi结算回报会被执行
     * @author gt_vv
     * @date 2019/12/12
     */
    @Override
    public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, CThostFtdcRspInfoField pRspInfo, int nRequestID,
                                           boolean bIsLast) {
        try {
            if (pRspInfo.getErrorID() == 0) {
                logger.warn("{}交易接口结算信息确认完成", logInfo);
            } else {
                logger.error("{}交易接口结算信息确认出错 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                //ctpGatewayImpl.disconnect();
                return;
            }

            // 防止被限流
            Thread.sleep(1000);

            logger.warn("{}交易接口开始查询投资者信息", logInfo);
            CThostFtdcQryInvestorField pQryInvestor = new CThostFtdcQryInvestorField();
            pQryInvestor.setBrokerID(brokerID);
            pQryInvestor.setInvestorID(userID);
            reqID++;
            //查询合约
            //int i = tdApi.ReqQryInvestor( pQryInvestor, reqID);
            //logger.warn("{}API投资者结算API状态返回：" + i,logInfo);
        } catch (Throwable t) {
            logger.error("{}处理结算单确认回报错误", logInfo, t);
            //ctpGatewayImpl.disconnect();
        }
    }

    /**
     * @Description  基础合约查询
     * @author gt_vv
     * @date 2020/1/3
     * @param
     * @return void
     */
    public void qryContract(){
        CThostFtdcQryInvestorField pQryInvestor = new CThostFtdcQryInvestorField();
        pQryInvestor.setBrokerID(brokerID);
        pQryInvestor.setInvestorID(userID);
        reqID++;
        //查询合约
        tdApi.ReqQryInvestor( pQryInvestor, reqID);
    }


    /**
     * @param
     * @return void
     * @Description 此方法为向外暴露接口所调用的   作用查询账户信息 此方法调用后会执行OnRspQryTradingAccount()查询得到回报信息
     * @author gt_vv
     * @date 2019/12/12
     */
    public void queryAccount() {
        if (tdApi == null) {
            logger.warn("{}交易接口尚未初始化,无法查询账户", logInfo);
            return;
        }
        if (!loginStatus) {
            logger.warn("{}交易接口尚未登录,无法查询账户", logInfo);
            return;
        }
        try {
            reqID++;
            CThostFtdcQryTradingAccountField cThostFtdcQryTradingAccountField = new CThostFtdcQryTradingAccountField();
            tdApi.ReqQryTradingAccount(cThostFtdcQryTradingAccountField, reqID);
        } catch (Throwable t) {
            logger.error("{}交易接口查询账户异常", logInfo, t);
        }
    }


    public void reqUserPasswordUpdate(String oldPassword,String newPassword){
        if (tdApi == null) {
            logger.warn("{}交易接口尚未初始化,无法查询账户", logInfo);
            return;
        }
//        if (!loginStatus) {
//            logger.warn("{}交易接口尚未登录,无法查询账户", logInfo);
//            return;
//        }
        try {
            reqID++;
            CThostFtdcUserPasswordUpdateField cThostFtdcUserPasswordUpdateField = new CThostFtdcUserPasswordUpdateField();
            cThostFtdcUserPasswordUpdateField.setBrokerID(brokerID);
            cThostFtdcUserPasswordUpdateField.setUserID(userID);
            cThostFtdcUserPasswordUpdateField.setOldPassword(oldPassword);
            cThostFtdcUserPasswordUpdateField.setNewPassword(newPassword);
            tdApi.ReqUserPasswordUpdate(cThostFtdcUserPasswordUpdateField,reqID);
        }catch (Throwable t){
            logger.error("{}交易接口修改密码异常", logInfo, t);
        }
    }
    public void ReqTradingAccountPasswordUpdate(String oldPassword,String newPassword){
        if (tdApi == null) {
            logger.warn("{}交易接口尚未初始化,无法查询账户", logInfo);
            return;
        }
        if (!loginStatus) {
            logger.warn("{}交易接口尚未登录,无法查询账户", logInfo);
            return;
        }
        try {
            reqID++;
            CThostFtdcTradingAccountPasswordUpdateField cThostFtdcUserPasswordUpdateField = new CThostFtdcTradingAccountPasswordUpdateField();
            cThostFtdcUserPasswordUpdateField.setBrokerID(brokerID);
            cThostFtdcUserPasswordUpdateField.setAccountID(userID);
            cThostFtdcUserPasswordUpdateField.setOldPassword(oldPassword);
            cThostFtdcUserPasswordUpdateField.setNewPassword(newPassword);
            tdApi.ReqTradingAccountPasswordUpdate(cThostFtdcUserPasswordUpdateField,reqID);
        }catch (Throwable t){
            logger.error("{}交易接口修改密码异常", logInfo, t);
        }
    }

    /**
     * @param pTradingAccount
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     * @Description 账户查询回报
     * @author gt_vv
     * @date 2019/12/11
     */
    @Autowired
    private IKLineDataService kLineDataService;

    @Override
    public void OnRspQryTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

        try {
            String accountCode = pTradingAccount.getAccountID();
            String currency = pTradingAccount.getCurrencyID();
            VtAccountData accountData = new VtAccountData();
            accountData.setAccountID(accountCode);
            accountData.setAvailable(pTradingAccount.getAvailable());
            accountData.setCloseProfit(pTradingAccount.getCloseProfit());
            accountData.setCommission(pTradingAccount.getCommission());
            accountData.setMargin(pTradingAccount.getCurrMargin());
            accountData.setPositionProfit(pTradingAccount.getPositionProfit());
            accountData.setPreBalance(pTradingAccount.getPreBalance());
            accountData.setBalance(pTradingAccount.getBalance());
            //System.out.println(currency);
            System.out.println("账户资金信息："+accountData);

            KLineData kLineData = new KLineData();
            kLineData.setData1(pTradingAccount.getBalance());
            kLineData.setData2(pTradingAccount.getAvailable());
            kLineData.setData3(pTradingAccount.getCommission());
            kLineData.setData4(pTradingAccount.getCloseProfit());
            kLineData.setCreateTime(DateUtils.getNowDate());
           // kLineDataService.insertKLineData(kLineData);
            //推送事件引擎
            //this.gateway.onAccount(accountData);
        } catch (Throwable t) {
            logger.error("{}处理查询账户回报异常", logInfo, t);
            //ctpGatewayImpl.disconnect();
        }
    }


    /**
     * @param pInvestor
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     * @Description 查询指令后，交易托管系统返回 响应时，该方法会被调用
     * @author gt_vv
     * @date 2019/12/13
     */
    @Override
    public void OnRspQryInvestor(CThostFtdcInvestorField pInvestor, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

        try {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
                logger.error("{}查询投资者信息失败 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                return;
                //ctpGatewayImpl.disconnect();
            } else {
                if (pInvestor != null) {
                    investorName = pInvestor.getInvestorName();
                    logger.warn("{}交易接口获取到的投资者名为:{}", logInfo, investorName);
                } else {
                    logger.error("{}交易接口未能获取到投资者名", logInfo);
                }
            }
            if (bIsLast) {
                if (StringUtils.isBlank(investorName)) {
                    logger.warn("{}" +
                            ",准备断开", logInfo);
                    return;
                    //ctpGatewayImpl.disconnect();
                }
                //investorNameQueried = true;
                reqID++;
                // 防止被限流
                Thread.sleep(1000);
                // 查询所有合约
                logger.warn("{}交易接口开始查询合约信息", logInfo);
                CThostFtdcQryInstrumentField cThostFtdcQryInstrumentField = new CThostFtdcQryInstrumentField();
                //请求查询合约。
                tdApi.ReqQryInstrument(cThostFtdcQryInstrumentField, reqID);
            }
        } catch (Throwable t) {
            logger.error("{}处理查询投资者回报异常", logInfo, t);
            //ctpGatewayImpl.disconnect();
        }
    }

    /**
     * @Description   查询基础合约回报
     * @author gt_vv
     * @date 2019/12/13
     * @param pInstrument
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     */
    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (pInstrument == null) {
            logger.warn("暂无合约信息");
            return;
        }
        VtContractData vtContractData = new VtContractData();
        //交易所代码
        vtContractData.setSymbol(pInstrument.getInstrumentID());
        vtContractData.setExchange(pInstrument.getExchangeID());
        //vt系统唯一标识
        vtContractData.setVtSymbol(pInstrument.getInstrumentID() + "." + pInstrument.getExchangeID());
        //合约名称
        vtContractData.setName(pInstrument.getInstrumentName());
        //到期日
        vtContractData.setExpiryDate(pInstrument.getExpireDate());
        //合约类型
        vtContractData.setProductClass(CtpGlobal.productClassMapReverse.getOrDefault(pInstrument.getProductClass(), VtConstant.PRODUCT_UNKNOWN));
        //合约大小
        vtContractData.setSize(pInstrument.getVolumeMultiple());
        //priceTick合约最小价格TICK
        vtContractData.setPriceTick(pInstrument.getPriceTick());

        // ETF期权的标的命名方式需要调整（ETF代码 + 到期月份）
        if (VtConstant.EXCHANGE_SSE.equals(vtContractData.getExchange()) || VtConstant.EXCHANGE_SZSE.equals(vtContractData.getExchange())) {
            vtContractData.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID() + "-" + pInstrument.getExpireDate().substring(2, pInstrument.getExpireDate().length() - 2));
        }
        // 商品期权无需调整
        else {
            vtContractData.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID());
        }

        // 期权类型
        if (VtConstant.PRODUCT_OPTION.equals(vtContractData.getProductClass())) {
            if (pInstrument.getOptionsType() == '1') {
                vtContractData.setOptionType(VtConstant.OPTION_CALL);
            } else if (pInstrument.getOptionsType() == '2') {
                vtContractData.setOptionType(VtConstant.OPTION_PUT);
            }
        }
        // 缓存代码和交易所的印射关系
        this.symbolExchangeDict.put(vtContractData.getSymbol(), vtContractData.getExchange());
        this.symbolSizeDict.put(vtContractData.getSymbol(), vtContractData.getSize());

        // 缓存合约代码和交易所映射  ---- 全局映射关系
        CtpGlobal.symbolExchangeDict.put(vtContractData.getSymbol(), vtContractData.getExchange());
        // 推送
        this.gateway.onContract(vtContractData);
        System.out.println(vtContractData);
        if (bIsLast) {
            //为true 合约查询成功
            logger.warn(Text.CONTRACT_DATA_RECEIVED);
        }
    }

    /**
     * @Description  查询持仓
     * @author gt_vv
     * @date 2020/1/3
     * @param
     * @return void
     */
    public void queryPosition() {
        if (tdApi == null) {
            logger.warn("{}交易接口尚未初始化,无法查询持仓", logInfo);
            return;
        }
        if (!loginStatus) {
            logger.warn("{}交易接口尚未登录,无法查询持仓", logInfo);
            return;
        }
        try {
            CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField = new CThostFtdcQryInvestorPositionField();
            cThostFtdcQryInvestorPositionField.setBrokerID(brokerID);
            cThostFtdcQryInvestorPositionField.setInvestorID(userID);
            reqID++;
            tdApi.ReqQryInvestorPosition(cThostFtdcQryInvestorPositionField, reqID);
        } catch (Throwable t) {
            logger.error("{}交易接口查询持仓异常", logInfo, t);
        }

    }

    /**
     * @param pInvestorPosition
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     * @Description // 持仓查询回报
     * @author gt_vv
     * @date 2019/12/13
     */
    @Override
    public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

        if (pInvestorPosition == null || StringUtils.isEmpty(pInvestorPosition.getInstrumentID())) {
            return;
        }
        //缓存在是否 包含vt唯一   不包含则重新创建
       String vtId =  pInvestorPosition.getInstrumentID() + "." + pInvestorPosition.getExchangeID();
        VtPositionData vtPositionData;
        if(posDict.containsKey(vtId)){
            vtPositionData = posDict.get(vtId);
        }else{
            //缓存中没有则创建对象
            vtPositionData = new VtPositionData();
            posDict.put(vtId,vtPositionData);
            vtPositionData.setVtSymbol(vtId);
            vtPositionData.setSymbol(pInvestorPosition.getInstrumentID());
        }


        // 获取持仓
        //持仓量
        vtPositionData.setPosition(pInvestorPosition.getPosition());
        //方向
        String directionLong = VtConstant.DIRECTION_LONG;
        //String direction = Character.toString(pInvestorPosition.getPosiDirection()) == "1" ?  VtConstant.DIRECTION_LONG : VtConstant.DIRECTION_NONE;
        vtPositionData.setDirection(Character.toString(pInvestorPosition.getPosiDirection()));
        //vt 唯一标识
        vtPositionData.setVtPositionName(pInvestorPosition.getInstrumentID() + "." + pInvestorPosition.getExchangeID() + "." + Character.toString(pInvestorPosition.getPosiDirection()));


        if (vtPositionData.getDirection() == VtConstant.DIRECTION_LONG) {
            vtPositionData.setFrozen(pInvestorPosition.getShortFrozen());
        } else {
            vtPositionData.setFrozen(pInvestorPosition.getLongFrozen());
        }
        //TODO 持仓功能未完成
        // 针对上期所、上期能源持仓的今昨分条返回（有昨仓、无今仓）,读取昨仓数据
        if(VtConstant.EXCHANGE_INE == vtPositionData.getExchange() || VtConstant.EXCHANGE_SHFE == vtPositionData.getExchange()){
            if (pInvestorPosition.getYdPosition() > 0 && pInvestorPosition.getTodayPosition() == 0) {

                vtPositionData.setYdPosition(vtPositionData.getYdPosition() + pInvestorPosition.getPosition());

            } else {
                vtPositionData.setPosition(vtPositionData.getPosition() + pInvestorPosition.getPosition());
                //vtPositionData.getDirection()   2 == 多       3 == 空
                if (vtPositionData.getDirection() == "2") {
                    vtPositionData.setFrozen(vtPositionData.getFrozen() + pInvestorPosition.getShortFrozen());
                } else {
                    vtPositionData.setFrozen(vtPositionData.getFrozen() + pInvestorPosition.getLongFrozen());
                }
            }
        }
        //ePosition 持仓推送事件引擎
        this.gateway.onPosition(vtPositionData);
        System.out.println(vtPositionData);
        if(bIsLast == true){
            logger.warn("持仓信息查询完成");
        }
    }


    /**
     * @Description  发单
     * @author gt_vv
     * @date 2019/12/18
     * @param orderReq
     * @return java.lang.String
     */
    public String submitOrder(VtOrderReq orderReq) {
        if(tdApi == null){
            logger.warn("{}交易接口尚未初始化,无法发单", logInfo);
        }
        if (!loginStatus) {
            logger.warn("{}交易接口尚未登录,无法发单", logInfo);
            return null;
        }
        //封装发单所需参数  CThostFtdcInputOrderField
        try{
            orderRef++;
            CThostFtdcInputOrderField cThostFtdcInputOrderField = new CThostFtdcInputOrderField();
            //经济公司代码
            cThostFtdcInputOrderField.setBrokerID(brokerID);
            //合约代码   各交易所不一致
            cThostFtdcInputOrderField.setInstrumentID(orderReq.getSymbol());
            //价格
            cThostFtdcInputOrderField.setLimitPrice(orderReq.getPrice());
            //报单数量
            cThostFtdcInputOrderField.setVolumeTotalOriginal(orderReq.getVolume());
            if(orderReq.getDirection().equals("1")){
                //买卖方向  多
                cThostFtdcInputOrderField.setDirection(thosttradeapiConstants.THOST_FTDC_D_Buy);
            }else{
                //买卖方向  空
                cThostFtdcInputOrderField.setDirection(thosttradeapiConstants.THOST_FTDC_D_Sell);
            }
            /**
             * 组合开平标志  THOST_FTDC_OF_Open是开仓
             * THOST_FTDC_OF_Close是平仓/平昨，
             * THOST_FTDC_OF_CloseToday是平今。
             * 除了上期所/能源中心外，不区分平今平昨，平仓统一使用THOST_FTDC_OF_Close。
             */
            cThostFtdcInputOrderField.setCombOffsetFlag(String.valueOf(thosttradeapiConstants.THOST_FTDC_OF_Open));
            //报单请求编号  此编号为递增
            System.out.println(orderRef);
            cThostFtdcInputOrderField.setOrderRef(Integer.toString(orderRef));
            //用户ID
            cThostFtdcInputOrderField.setInvestorID(userID);
            cThostFtdcInputOrderField.setUserID(userID);
            //交易所
            cThostFtdcInputOrderField.setExchangeID(orderReq.getExchange());
            //组合投机套保标志
            cThostFtdcInputOrderField.setCombHedgeFlag(String.valueOf(thosttradeapiConstants.THOST_FTDC_HF_Speculation));
            /**
             * 触发条件  -- 立即有效
             * THOST_FTDC_CC_Touch和THOST_FTDC_CC_TouchProfit是止损止盈单，需要交易所支持才能填。
             * THOST_FTDC_CC_ParkedOrder是预埋单。预埋单是指预埋在CTP服务端，需要非交易时间报入，开市后自动报往交易所。
             */
            cThostFtdcInputOrderField.setContingentCondition(thosttradeapiConstants.THOST_FTDC_CC_Immediately);
            //强平原因   必填  THOST_FTDC_FCC_NotForceClose
            cThostFtdcInputOrderField.setForceCloseReason(thosttradeapiConstants.THOST_FTDC_FCC_NotForceClose);
            //自动挂起标志
            cThostFtdcInputOrderField.setIsAutoSuspend(0);
            //有效期类型
            /**TimeCondition是枚举类型，
             * 目前只有THOST_FTDC_TC_GFD和THOST_FTDC_TC_IOC这两种类型有用。
             * GFD是指当日有效，报单会挂在交易所直到成交或收盘自动撤销。
             * IOC是立即完成否则撤销，和VolumeCondition、MinVolume 字段配合用于设置FAK或FOK
             */
            cThostFtdcInputOrderField.setTimeCondition(thosttradeapiConstants.THOST_FTDC_TC_GFD);
            //成交量类型
            cThostFtdcInputOrderField.setVolumeCondition(thosttradeapiConstants.THOST_FTDC_VC_AV);
            //最小成交量
            cThostFtdcInputOrderField.setMinVolume(1);
            //
            cThostFtdcInputOrderField.setOrderPriceType(thosttradeapiConstants.THOST_FTDC_OPT_LimitPrice);

            /**      报单：priceType 的传参需求
             *   字段          \       普通            \     FAK                                  \                FOK
             * ----------------\-----------------------\------------------------------------------\------------------------------
             * TimeCondition   \    THOST_FTDC_TC_GFD  \   THOST_FTDC_TC_IOC                      \          THOST_FTDC_TC_IOC
             * ----------------\-----------------------\------------------------------------------\----------------------------
             * VolumeCondition \   THOST_FTDC_VC_AV    \    THOST_FTDC_VC_AV/THOST_FTDC_VC_MV     \          THOST_FTDC_VC_CV
             *-----------------\-----------------------\------------------------------------------\--------------------------
             * MinVolume       \   不需要填            \    如果VolumeCondition为THOST_FTDC_VC_AV \         不需要填
             *                 \                       \   则不需要填。如果为THOST_FTDC_VC_MV，   \
             *                 \                       \  则设为要求的最小成交的手数              \
             */
            //判断 FAK  FOK   普通单
            if(PriceTypeEnum.FAK .toString().equals(orderReq.getPriceType())){
                cThostFtdcInputOrderField.setOrderPriceType(thosttradeapiConstants.THOST_FTDC_OPT_LimitPrice);
                cThostFtdcInputOrderField.setTimeCondition(thosttradeapiConstants.THOST_FTDC_TC_IOC);
                cThostFtdcInputOrderField.setVolumeCondition(thosttradeapiConstants.THOST_FTDC_VC_AV);
            }else if (PriceTypeEnum.FOK.toString().equals(orderReq.getPriceType())) {
                cThostFtdcInputOrderField.setOrderPriceType(thosttradeapiConstants.THOST_FTDC_OPT_LimitPrice);
                cThostFtdcInputOrderField.setTimeCondition(thosttradeapiConstants.THOST_FTDC_TC_IOC);
                cThostFtdcInputOrderField.setVolumeCondition(thosttradeapiConstants.THOST_FTDC_VC_CV);
            }

            reqID++;
            tdApi.ReqOrderInsert(cThostFtdcInputOrderField, reqID);
            return String.valueOf(orderRef);
        }catch (Exception e){
            logger.error("{}交易接口发单错误", logInfo, e);
            return null;
        }
    }


    // 发单错误（柜台）
    /**
     * @Description  发单 回报
     * @author gt_vv
     * @date 2019/12/20
     * @param pInputOrder
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     * @return void
     */
    @Override
    public void OnRspOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            if(pInputOrder != null){
                VtOrderData vtOrderData = new VtOrderData();
                vtOrderData.setFrontID(frontID);
                vtOrderData.setSessionID(sessionID);
                vtOrderData.setSymbol(pInputOrder.getInstrumentID());
                vtOrderData.setVtSymbol(pInputOrder.getInstrumentID()+"."+pInputOrder.getExchangeID());
                vtOrderData.setExchange(pInputOrder.getExchangeID());
                vtOrderData.setOrderID(pInputOrder.getOrderRef());
                vtOrderData.setOffset(pInputOrder.getCombOffsetFlag());
                orderRef = Integer.parseInt(pInputOrder.getOrderRef());
                vtOrderData.setVtOrderID(gatewayName  + "." + orderRef);
                vtOrderData.setDirection(String.valueOf(pInputOrder.getDirection()));
                vtOrderData.setPrice(pInputOrder.getLimitPrice());
                vtOrderData.setTotalVolume(pInputOrder.getVolumeTotalOriginal());
                System.out.println("柜台返回：" + vtOrderData);
            }
        } catch (Throwable t) {
            logger.error("{}处理交易接口发单错误回报(柜台)异常", logInfo, t);
        }
    }

    /**
     * @Description  委托回报
     * @author gt_vv
     * @date 2019/12/24
     * @param pOrder
     * @return void
     */
    @Override
    public void OnRtnOrder(CThostFtdcOrderField pOrder) {
        //更新最新的报单编号
        VtOrderData vtOrderData = new VtOrderData();
        vtOrderData.setSymbol(pOrder.getInstrumentID());
        vtOrderData.setOrderID(pOrder.getOrderRef());
        vtOrderData.setExchange(pOrder.getExchangeID());
        vtOrderData.setVtOrderID(gatewayName + "."+pOrder.getOrderRef());
        vtOrderData.setVtSymbol(pOrder.getInstrumentID()+"."+pOrder.getExchangeID());
        vtOrderData.setDirection(String.valueOf(pOrder.getDirection()));
        vtOrderData.setFrontID(pOrder.getFrontID());
        vtOrderData.setSessionID(pOrder.getSessionID());
        System.out.println("THOST_FTDC_OSS_Accepted" + thosttradeapiConstants.THOST_FTDC_OSS_Accepted);
        System.out.println("THOST_FTDC_OST_AllTraded" + thosttradeapiConstants.THOST_FTDC_OST_AllTraded);
        System.out.println("THOST_FTDC_OST_Canceled" + thosttradeapiConstants.THOST_FTDC_OST_Canceled);
        char orderSubmitStatus = pOrder.getOrderSubmitStatus();
        System.out.println("pOrder.getOrderSubmitStatus()==="+orderSubmitStatus);
        vtOrderData.setStatus(String.valueOf(pOrder.getOrderStatus()));
        vtOrderData.setTradedVolume(pOrder.getVolumeTraded());
        vtOrderData.setTotalVolume(pOrder.getVolumeTotal());
        vtOrderData.setOrderTime(pOrder.getActiveTime());
        vtOrderData.setCancelTime(pOrder.getCancelTime());
        vtOrderData.setPrice(pOrder.getLimitPrice());
        System.out.println(vtOrderData);

        logger.warn("{委托} 回报：" + pOrder.getStatusMsg());
        //CTP的报单号一致性维护需要基于frontID, sessionID, orderID三个字段
        // 无法获取账户信息,使用userId作为账户ID
        String accountCode = userID;
        //DirectionEnum direction = CtpConstant.directionMapReverse.getOrDefault(pOrder.getDirection(), DirectionEnum.UNKNOWN_DIRECTION);
    }

    /**
     * @Description 成交回报    如有成交则会单独回调OnRtnTrade函数
     * @author gt_vv
     * @date 2019/12/24
     * @param pTrade
     * @return void
     */
    @Override
    public void OnRtnTrade(CThostFtdcTradeField pTrade) {
        logger.warn("{成交} 回报：成交数量为" + pTrade.getVolume());
        VtTradeData vtTradeData = new VtTradeData();
        vtTradeData.setSymbol(pTrade.getInstrumentID());
        vtTradeData.setExchange(pTrade.getExchangeID());
        vtTradeData.setOrderID(pTrade.getOrderRef());
        vtTradeData.setVtSymbol(pTrade.getInstrumentID()+"."+pTrade.getExchangeID());
        this.gateway.onTrade(vtTradeData);
        System.out.println("成交");
    }

    // 发单错误回报（交易所）
    @Override
    public void OnErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
        VtOrderData vtOrderData = new VtOrderData();
        if(pInputOrder != null){
            vtOrderData.setVtSymbol(pInputOrder.getInstrumentID()+ pInputOrder.getExchangeID());
            vtOrderData.setSymbol(pInputOrder.getInstrumentID());
            vtOrderData.setFrontID(frontID);
            vtOrderData.setSessionID(sessionID);
            vtOrderData.setVtOrderID(gatewayName  + orderRef);
        }
        logger.error("{}交易接口发单错误回报（交易所） 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
    }

    /**
     * @Description 撤单   //  vtOrderData    FrontID + SessionID + OrderRef
     * @author gt_vv
     * @date 2020/1/2
     * @param cancelOrderReq
     * @return void
     */
    public boolean cancelOrder(VtCancelOrderReq cancelOrderReq){
        if (tdApi == null) {
            logger.warn("{}交易接口尚未初始化,无法撤单", logInfo);
            return false;
        }

        if (!loginStatus) {
            logger.warn("{}交易接口尚未登录,无法撤单", logInfo);
            return false;
        }
        if (StringUtils.isBlank(cancelOrderReq.getOrderID())) {
            logger.error("{}参数为空,无法撤单", logInfo);
            return false;
        }
        try{
            CThostFtdcInputOrderActionField cThostFtdcInputOrderActionField = new CThostFtdcInputOrderActionField();
            cThostFtdcInputOrderActionField.setBrokerID(brokerID);
            cThostFtdcInputOrderActionField.setUserID(userID);
            cThostFtdcInputOrderActionField.setInvestorID(userID);
            cThostFtdcInputOrderActionField.setInstrumentID(cancelOrderReq.getSymbol());
            cThostFtdcInputOrderActionField.setSessionID(Integer.parseInt(cancelOrderReq.getSessionID().trim()));
            cThostFtdcInputOrderActionField.setFrontID(Integer.parseInt(cancelOrderReq.getFrontID().trim()));
            cThostFtdcInputOrderActionField.setOrderRef(cancelOrderReq.getOrderID());
            cThostFtdcInputOrderActionField.setExchangeID(cancelOrderReq.getExchange());
            // 该字段是枚举值，有删除和修改两种，目前只支持删除，即撤单。
            cThostFtdcInputOrderActionField.setActionFlag(thosttradeapiConstants.THOST_FTDC_AF_Delete);
            reqID++;
            tdApi.ReqOrderAction(cThostFtdcInputOrderActionField,reqID);
        }catch (Throwable t){
            logger.error("{}撤单异常", logInfo, t);
            return false;
        }
        return true;
    }


    // 撤单错误回报（柜台）
    @Override
    public void OnRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        if (pRspInfo != null) {
            logger.error("{}交易接口撤单错误回报(柜台) 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
            VtErrorData vtErrorData = new VtErrorData();
            vtErrorData.setErrorID(String.valueOf(pRspInfo.getErrorID()));
            vtErrorData.setErrorMsg(pRspInfo.getErrorMsg());

        } else {
            logger.error("{}处理交易接口撤单错误回报(柜台)错误,无有效信息", logInfo);

        }
    }
}
