package com.kurtzhi.test;

import java.util.Random;

import com.kurtzhi.Segine.ManagedObject;

public class Consumer extends ManagedObject {
    private Seller seller;
    private String id;
    private int numToBuy;

    public Consumer(String id, Seller seller) {
        this.id = id;
        this.seller = seller;
    }

    public int goodsToBuy() {
        return this.numToBuy;
    }

    public void preparing() {
        this.numToBuy = (Math.abs(new Random().nextInt()) % 3) + 1;
        System.out.println("Consumer#" + this.id + " will buy " + this.numToBuy
                + " goods.");
    }

    public void buying() {
        this.seller.selling(this.numToBuy);
        System.out.println("Consumer#" + this.id + " buying " + this.numToBuy
                + " goods.");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        this.preparing();
    }

    @Override
    public void dispose() {
    }
}
