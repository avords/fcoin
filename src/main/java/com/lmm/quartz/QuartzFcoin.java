package com.lmm.quartz;

import com.lmm.fcoin.FcoinJob;

public class QuartzFcoin {

    public static void main(String[] args) {
        QuartzManager.addJob("fcoinJob", FcoinJob.class, "*/30 * * * * ?");//不支持任务并发，实现StatefulJob接口
        QuartzManager.startJobs();
    }
}
