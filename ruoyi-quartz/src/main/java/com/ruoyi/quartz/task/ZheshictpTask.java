package com.ruoyi.quartz.task;


import com.ruoyi.quartz.ctp.CtpGateway;
import org.springframework.stereotype.Component;

@Component("zheshictpTask")
public class ZheshictpTask {


    /**
     * @Description  测试main方法
     * @author gt_vv
     * @date 2019/12/11
     * @param args
     * @return void
     */
    public  void zheshictpTaskonw() {
        //EventEngine eventEngine = new EventEngine();
        //eventEngine.start(true);
        try {
            CtpGateway ctpGateway = new CtpGateway(null,"CTP");
            ctpGateway.connect();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

}
