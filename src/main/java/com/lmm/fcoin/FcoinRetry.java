package com.lmm.fcoin;

import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

public class FcoinRetry {
    public static RetryTemplate getRetryTemplate(){
        
        final RetryTemplate retryTemplate = new RetryTemplate();
        
        final SimpleRetryPolicy policy = new SimpleRetryPolicy(3, Collections.<Class<? extends Throwable>, Boolean>
                singletonMap(Exception.class, true));
        
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        
        //设置重试间隔时间
        fixedBackOffPolicy.setBackOffPeriod(500);
        
        retryTemplate.setRetryPolicy(policy);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        
        return retryTemplate;
    }
}
