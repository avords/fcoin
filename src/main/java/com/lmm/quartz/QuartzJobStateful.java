package com.lmm.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * Created by Administrator on 2016/11/20.
 */
public class QuartzJobStateful implements StatefulJob{
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("你是一只小鸭子，咿呀咿呀哟！");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("休眠结束");
    }
}
