package com.lmm.fcoin;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
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
    
    public static RetryTemplate getTradeRetryTemplate(){
        final RetryTemplate retryTemplate = new RetryTemplate();

        final SimpleRetryPolicy policy = new SimpleRetryPolicy(10, Collections.<Class<? extends Throwable>, Boolean>
                singletonMap(Exception.class, true));

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();

        //设置重试间隔时间
        fixedBackOffPolicy.setBackOffPeriod(500);

        retryTemplate.setRetryPolicy(policy);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        return retryTemplate;
    }

    public static RetryTemplate getExponentialRetryTemplate(){

        RetryTemplate retryTemplateOnce = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(50);
        backOffPolicy.setMaxInterval(1000);
        retryTemplateOnce.setBackOffPolicy(backOffPolicy);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplateOnce.setRetryPolicy(retryPolicy);
        
        return retryTemplateOnce;
    }

    public static void main(String[] args) {
        RetryTemplate retryTemplate = getExponentialRetryTemplate();
        retryTemplate.execute((RetryCallback) retryContext -> {
            System.out.println("我重试了");
            throw new Exception("ee");
        });
    }
    
    
}
