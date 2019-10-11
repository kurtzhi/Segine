package com.kurtzhi.test;

import com.kurtzhi.Segine.*;

public class Test {
    public static void main(String[] args) {
        StateMachine pcs = Segine.createStateMachine("producerConsumerSeller");

        pcs.createIncomingAssociation("n", Notification.class);

        pcs.createOutgoingAssociation("s", Seller.class);
        pcs.createOutgoingAssociation("p", Producer.class);
        pcs.createOutgoingAssociation("c", Consumer.class);
        
        State top = pcs.getTopState();
        State init = pcs.createState("init", StateType.INITIAL);
        State prepare = pcs.createState("prepare", StateType.COMMON);
        State producing = pcs.createState("producing", StateType.COMMON);
        State consuming = pcs.createState("consuming", StateType.COMMON);
        
/*
        consuming.addExitAction(pcs.createAction("c.preparing"));
        producing.addEntryAction(pcs.createAction("p.producing"));
*/
        /*

        */
        
        //consuming.addExitAction(pcs.createAction("c.preparing,"));
        //consuming.addEntryAction(pcs.createAction("c.buying, c.preparing, s.waitingForSale"));
        //producing.addEntryAction(pcs.createAction("p.producing, s.waitingForSale"));
        //producing.addExitAction(pcs.createAction(""));

consuming.addEntryAction(pcs.createAction("c.buying, s.waitingForSale"));
consuming.addExitAction(pcs.createAction("c.preparing,"));
producing.addEntryAction(pcs.createAction("p.producing, s.waitingForSale"));
top.addSubState(init);

pcs.createTransition(init, prepare, null, null);

pcs.createTransition(prepare, producing, pcs.createGuard("re", null), null);

pcs.createTransition(prepare, consuming, pcs.createGuard("se", null), null);

pcs.createTransition(producing, consuming, pcs.createGuard("se", null), null);

pcs.createTransition(consuming, consuming,
        pcs.createGuard("se", "c.goodsToBuy <= s.goodsAvailable"), null);
pcs.createTransition(consuming, consuming,
        pcs.createGuard("se, de", "c.goodsToBuy > 0 && c.goodsToBuy <= s.goodsAvailable"), null);

pcs.createTransition(consuming, producing,
        pcs.createGuard("se", "c.goodsToBuy > s.goodsAvailable"), null);
/*
top.addSubState(init);

pcs.createTransition(init, prepare, null, null);

pcs.createTransition(prepare, producing, pcs.createGuard("re", null),
        pcs.createAction("p.producing, s.waitingForSale"));

pcs.createTransition(prepare, consuming, pcs.createGuard("se", null),
        pcs.createAction("c.buying, c.preparing, s.waitingForSale"));

pcs.createTransition(producing, consuming, pcs.createGuard("se", null),
        pcs.createAction("c.buying, c.preparing, s.waitingForSale"));

pcs.createTransition(consuming, consuming,
        pcs.createGuard("se", "c.goodsToBuy <= s.goodsAvailable"),
        pcs.createAction("c.buying, c.preparing, s.waitingForSale"));

pcs.createTransition(consuming, producing,
        pcs.createGuard("se", "c.goodsToBuy > s.goodsAvailable"),
        pcs.createAction("p.producing, s.waitingForSale"));
        */
        /*
*/
/*
        top.addSubState(init);
        pc.createTransition(init, prepare, null, null);
        // pc.createTransition(prepare, producing, pc.createGuard("re", null),
        // pc.createAction("p.producing,s.waitingForSale")); //1
        pc.createTransition(prepare, producing, pc.createGuard("re", null),
                pc.createAction("s.waitingForSale")); // 2
        // pc.createTransition(prepare, consuming, pc.createGuard("se", null),
        // pc.createAction("c.buying,c.preparing,s.waitingForSale")); //1
        pc.createTransition(prepare, consuming, pc.createGuard("se", null),
                pc.createAction("c.buying,s.waitingForSale")); // 2
        // pc.createTransition(producing, consuming, pc.createGuard("re", null),
        // pc.createAction("p.producing,s.waitingForSale")); //1
        pc.createTransition(producing, consuming, pc.createGuard("re", null),
                pc.createAction("s.waitingForSale")); // 2
        // pc.createTransition(consuming, consuming, pc.createGuard("se",
        // "c.goodsToBuy<=s.goodsAvailable"),
        // pc.createAction("c.buying,c.preparing,s.waitingForSale")); //1
        pc.createTransition(consuming, consuming,
                pc.createGuard("se", "c.goodsToBuy<=s.goodsAvailable"),
                pc.createAction("c.buying,s.waitingForSale")); // 2
        pc.createTransition(consuming, producing,
                pc.createGuard("se", "c.goodsToBuy>s.goodsAvailable"),
                pc.createAction("s.requestOfProducing"));
        
        
        top.addSubState(init);
        pc.createTransition(init, prepare, null, null);
        pc.createTransition(prepare, producing, pc.createGuard("re", null),
                pc.createAction("s.waitingForSale"));
        pc.createTransition(prepare, consuming, pc.createGuard("se", null),
                pc.createAction("c.buying,s.waitingForSale"));
        pc.createTransition(producing, consuming, pc.createGuard("re", null),
                pc.createAction("s.waitingForSale"));
        pc.createTransition(consuming, consuming,
                pc.createGuard("se", "c.goodsToBuy<=s.goodsAvailable"),
                pc.createAction("c.buying,s.waitingForSale"));
        pc.createTransition(consuming, producing,
                pc.createGuard("se", "c.goodsToBuy>s.goodsAvailable"),
                pc.createAction("s.requestOfProducing"));
        */
        /*
         * c.goodsToBuy <= s.goodsAvailable
         */
        pcs.register();
/*
*/
StateMachineRuntime rt1 = pcs.createStateMachineRuntime();

Notification notification = new Notification();
Seller seller = new Seller("s1", notification);
rt1.linkAssociatedInstance(notification);
rt1.linkAssociatedInstance(seller);
rt1.linkAssociatedInstance(new Producer("p1", seller));
rt1.linkAssociatedInstance(new Consumer("c1", seller));
notification.trig(new Event[] { Notification.Selling });

StateMachine regSm = Segine.lookupStateMachine("producerConsumerSeller");

StateMachineRuntime rt2 = regSm.createStateMachineRuntime();


Notification notification2 = new Notification();
Seller seller2 = new Seller("s2", notification2);
rt2.linkAssociatedInstance(notification2);
rt2.linkAssociatedInstance(seller2);
rt2.linkAssociatedInstance(new Producer("p2", seller2));
rt2.linkAssociatedInstance(new Consumer("c2", seller2));
notification2.trig(new Event[] { Notification.Replenish });

/*
new Thread(new Runnable() {
	
	public void run() {
	}
}).start();
*/
         
    }
}
