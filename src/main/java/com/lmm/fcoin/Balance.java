package com.lmm.fcoin;

/**
 * Created by avords on 2018/6/12.
 */
public class Balance {
    
    private double available;
    
    private double frozen;
    
    private double balance;

    public double getAvailable() {
        return available;
    }

    public void setAvailable(double available) {
        this.available = available;
    }

    public double getFrozen() {
        return frozen;
    }

    public void setFrozen(double frozen) {
        this.frozen = frozen;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
