package com.lmm.fcoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

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
        logger.info("程序结束");
    }
    
    public void buy() throws Exception {
        logger.info("=====================execute buy================");
        Thread.sleep(10000);
        logger.info("我休眠了，哈哈哈");
        throw new Exception("ee");
    }
}
