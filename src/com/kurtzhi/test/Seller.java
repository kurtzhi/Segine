package com.kurtzhi.test;

import com.kurtzhi.Segine.Event;
import com.kurtzhi.Segine.ManagedObject;

public class Seller extends ManagedObject {
    private Notification notification;
    private int capacity;
    private int available;
    private String id;

    public Seller(String id, Notification notification) {
        this.id = id;
        this.capacity = 3;
        this.available = 0;
        this.notification = notification;
    }

    public int capacityOfGoodsShelf() {
        return this.capacity;
    }

    public int goodsAvailable() {
        return this.available;
    }

    public void replenishGoods(int i) {
        this.available += i;
        System.out.println("Store#" + this.id + " replenishing " + i
                + " goods, goodsshelf is full.");
    }

    public void selling(int i) {
        this.available -= i;
        System.out.println("Store#" + this.id + " selling " + i + " goods, "
                + this.available + " goods left.");
    }

    public void requestOfProducing() {
        this.notification.trig(new Event[] { Notification.Replenish });
    }

    public void waitingForSale() {
        this.notification.trig(new Event[] { Notification.Selling });
    }

    @Override
    public void init() {
        System.out.println("Capacity of goodsshelf of store#" + this.id
                + " is " + this.capacity + ", " + this.available
                + " goods available.");
    }

    @Override
    public void dispose() {
    }
}
