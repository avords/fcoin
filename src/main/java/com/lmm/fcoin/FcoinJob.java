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
            fcoinUtils.ftusdt();
        }catch (Exception e){
            try {
                fcoinUtils.ftusdt();
            } catch (Exception e1) {
                try {
                    fcoinUtils.ftusdt();
                } catch (Exception e2) {
                    logger.error("Fcoin job error!", e2);
                }
            }
        }
    }
}
