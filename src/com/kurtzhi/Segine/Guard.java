package com.kurtzhi.Segine;

/*
 * Guards give you the possibility to decide which transition is executed 
 * depending on a boolean criteria. When an event is fired onto the state 
 * machine, it takes all transitions defined in the current state for the fired 
 * event and executes the first transition with a guard returning true. 
 */

class Guard {
    protected StateMachine sm;
    protected String evtId;
    protected LogicExpression lgcExpr;

    public void setLogicExpression(LogicExpression lgcExpr) {
        if (!sm.isInitialized() && lgcExpr.sm == sm) {
            this.lgcExpr = lgcExpr;
        }
    }

    protected Guard(String expr, LogicExpression lgcExpr) {
        this.evtId = Event.expressionToIdentifer(expr);
        this.lgcExpr = lgcExpr;
    }
}
