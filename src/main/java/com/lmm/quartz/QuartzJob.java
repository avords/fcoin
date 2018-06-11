package com.lmm.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by Administrator on 2016/11/20.
 */
public class QuartzJob implements Job{
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("你是一只小鸭子，咿呀咿呀哟！");
    }
}
