package com.lmm.fcoin;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Created by avords on 2018/6/16.
 */
public class DateDemo {
    public static void main(String[] args) {
        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println(localDateTime);
        
        LocalDateTime localDateTime1 = 
                LocalDateTime.of(localDateTime.getYear(),localDateTime.getMonth(),localDateTime.getDayOfMonth(),localDateTime.getHour()+1,0);

        System.out.println(Duration.between(localDateTime, localDateTime1).toMinutes());
        if(localDateTime.compareTo(localDateTime1)<0&& Duration.between(localDateTime1, localDateTime).toMinutes()<=10){
            
        }
    }
}
