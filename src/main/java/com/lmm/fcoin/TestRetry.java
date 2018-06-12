package com.lmm.fcoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import java.util.Random;

public class TestRetry {

    private static final Logger logger = LoggerFactory.getLogger(TestRetry.class);
    
    private static final RetryTemplate retryTemplate = FcoinRetry.getRetryTemplate();
    public static void main(String[] args) {
        TestRetry testRetry = new TestRetry();
        
        try {
            testRetry.retryTemplate.execute((RetryCallback) retryContext -> {
                testRetry.buy();
                return null;
            });
        }catch (Exception e){
            logger.info("==========重试后还是异常============");
        }
    }
    
    public void buy() throws Exception {
        logger.info("=====================execute buy================");
        Random r = new Random();
        int num = r.nextInt(2);
        if(num==1){
            logger.info("random num is {}",num);
            throw new Exception("ee");
        }
        
        logger.info("random num is {}",num);
        
    }
}
