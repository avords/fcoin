package com.lmm.fcoin;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;

public class FcoinJob implements StatefulJob {

    private static final RetryTemplate retryTemplate = FcoinRetry.getRetryTemplate();
    
    private static final Logger logger = LoggerFactory.getLogger(FcoinJob.class);
    
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        FcoinUtils fcoinUtils = new FcoinUtils();
        try {
            retryTemplate.execute(retryContext -> {
                fcoinUtils.ftusdt();
                return null;
            });
        }catch (Exception e){
            logger.info("==========fcoinUtils.ftusdt重试后还是异常============");
            throw new JobExecutionException("ftustd 方法体执行异常");
        }
    }
}
