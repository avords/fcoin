package com.lmm.fcoin;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FcoinJob implements StatefulJob {
    
    private static final Logger logger = LoggerFactory.getLogger(FcoinJob.class);
    
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        FcoinUtils fcoinUtils = new FcoinUtils();
        try {
            //fcoinUtils.ftusdt1("ftusdt", "ft", "usdt",0);
            //fcoinUtils.ftusdt2("ftusdt", "ft", "usdt",0.05);
            //fcoinUtils.ftusdt1("btcusdt","btc","usdt",0);
            fcoinUtils.ftusdt1("ftusdt","ft","usdt",0);
        }catch (Exception e){
            logger.info("==========FcoinJob发生异常============");
            throw new JobExecutionException("ftustd 方法体执行异常");
        }
    }
}
