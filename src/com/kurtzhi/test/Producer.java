package com.kurtzhi.test;

import com.kurtzhi.Segine.ManagedObject;

public class Producer extends ManagedObject {
    private String id;
    private Seller seller;

    public Producer(String id, Seller seller) {
        this.id = id;
        this.seller = seller;
    }

    public void producing() {
        int numToProduce = this.seller.capacityOfGoodsShelf()
                - this.seller.goodsAvailable();
        System.out.println("Producer#" + this.id + " producing " + numToProduce
                + " goods.");
        this.seller.replenishGoods(numToProduce);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        this.producing();
    }

    @Override
    public void dispose() {
    }
}
