package com.kurtzhi.Segine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.kurtzhi.Segine.Logger.TraceLevel;
import com.kurtzhi.Segine.ManagedObjectHelper.MethodType;

class LogicExpression {
    final private static String RetBoolExprPattern = "^[!]?[a-zA-Z][\\w]*\\.[a-zA-Z][\\w]*";
    final private static String CmpIntExprPattern = "^[a-zA-Z][\\w]*\\.[a-zA-Z][\\w]*(([\\>|\\<]\\=?)|([\\!|\\=]\\=))[\\d]+";
    final private static String CmpOperandExprPattern = "^[a-zA-Z][\\w]*\\.[a-zA-Z][\\w]*(([\\>|\\<]\\=?)|([\\!|\\=]\\=))[a-zA-Z][\\w]*\\.[a-zA-Z][\\w]*";
    final private static String LegalOperandsPattern = "[\\w\\.]*";
    final private static String CmpOpcodePattern = "([\\>|\\<]\\=?)|([\\!|\\=]\\=)";
    final private static String ObjFuncDivPattern = "\\.";
    final private static String BoolNeqPattern = "!";
    final private static Map<String, CompareOpcode> operators = new HashMap<String, CompareOpcode>();
    protected StateMachine sm;
    protected Operations operations = new Operations();
    protected String identifier;

    public static boolean Test(StateMachineRuntime runtime,
            LogicExpression logicExpr) {
        if (logicExpr == null) {
            return false;
        }

        boolean result = false;
        int length;
        StateMachine sm = logicExpr.sm;

        if (logicExpr.operations != null) {
            length = logicExpr.operations.opcodes.size();
            result = _computeExpr(sm, runtime, logicExpr.operations.operands.get(0));
            for (int idx = 0; idx < length; idx++) {
                if (logicExpr.operations.opcodes.get(idx) == LogicalOpcode.And) {
                    if (result) {
                        result = _computeExpr(sm, runtime, logicExpr.operations.operands.get(idx + 1));
                    }
                } else {
                    if (!result) {
                        result = _computeExpr(sm, runtime, logicExpr.operations.operands.get(idx + 1));
                    }
                }
            }
        }
        return result;
    }

    public void and(String expr) {
        expr = expr.replace(" ", "");
        Object operand = _logicExpressionToOperand(expr);
        if (operand != null) {
            operations.opcodes.add(LogicalOpcode.And);
            operations.operands.add(operand);
            identifier += "&&" + expr;
        } else {
            Logger.log(TraceLevel.Error, LogicExpression.class.getSimpleName(),
                    "Incorrect syntax of boolean expression \"" + expr + "\"");
        }
    }

    public void or(String expr) {
        expr = expr.replace(" ", "");
        Object operand = _logicExpressionToOperand(expr);
        if (operand != null) {
            operations.opcodes.add(LogicalOpcode.Or);
            operations.operands.add(operand);
            identifier += "||" + expr;
        } else {
            Logger.log(TraceLevel.Error, LogicExpression.class.getSimpleName(),
                    "Incorrect syntax of boolean expression \"" + expr + "\"");
        }
    }

    protected LogicExpression(String expr) {
        if (operators.size() == 0) {
        }

        expr = expr.replace(" ", "");
        String[] exprs = null;
        int cplxExprCnt = 0;
        LogicalOpcode opcode = null;
        if (expr.indexOf("\\|\\|") > 0) {
            exprs = expr.split("||");
            opcode = LogicalOpcode.Or;
            cplxExprCnt ++;
        }
        if (expr.indexOf("&&") > 0) {
            exprs = expr.split("&&");
            opcode = LogicalOpcode.And;
            cplxExprCnt ++;
        }
        if (cplxExprCnt == 2) {
            Logger.log(TraceLevel.Error, LogicExpression.class.getSimpleName(),
                    "\"&&\" and \"||\" cannot use together, expression \"" + expr + "\"");
            return;
        } else if (cplxExprCnt == 0) {
            exprs = new String[]{ expr };
        }
        Object operand = _logicExpressionToOperand(exprs[0]);
        if (operand != null) {
            operations.operands.add(operand);
            identifier = exprs[0];
        } else {
            Logger.log(TraceLevel.Error, LogicExpression.class.getSimpleName(),
                    "Incorrect syntax of boolean expression \"" + exprs[0] + "\"");
        }
        int len = exprs.length;
        for (int idx = 1; idx < len; idx ++) {
            if (opcode == LogicalOpcode.And) {
                this.and(exprs[idx]);
            } else {
                this.or(exprs[idx]);
            }
        }
    }

    private static Object _logicExpressionToOperand(String expr) {
        String[] strArr1, strArr2;
        if (expr.matches(RetBoolExprPattern)) {
            strArr1 = expr.split(ObjFuncDivPattern);
            OperandReturnBool ob = new OperandReturnBool();
            strArr2 = strArr1[0].split(BoolNeqPattern);
            if (strArr2.length == 1) {
                ob.id = strArr1[0];
                ob.isNotSet = false;
            } else {
                ob.id = strArr2[1];
                ob.isNotSet = true;
            }
            ob.name = strArr1[1];
            return ob;
        } else if (expr.matches(CmpIntExprPattern)) {
            strArr1 = expr.split(CmpOpcodePattern);
            strArr2 = strArr1[0].split(ObjFuncDivPattern);
            OperandCompareInt oi = new OperandCompareInt();
            oi.value = Integer.parseInt(strArr1[1]);
            oi.id = strArr2[0];
            oi.name = strArr2[1];
            oi.opcode = operators
                    .get(expr.replaceAll(LegalOperandsPattern, ""));
            return oi;
        } else if (expr.matches(CmpOperandExprPattern)) {
            strArr1 = expr.split(CmpOpcodePattern);
            strArr2 = strArr1[0].split(ObjFuncDivPattern);
            strArr1 = strArr1[1].split(ObjFuncDivPattern);
            OperandCompareOperand oi = new OperandCompareOperand();
            oi.id1 = strArr2[0];
            oi.name1 = strArr2[1];
            oi.id2 = strArr1[0];
            oi.name2 = strArr1[1];
            oi.opcode = operators
                    .get(expr.replaceAll(LegalOperandsPattern, ""));
            return oi;
        }
        return null;
    }

    private static boolean _computeExpr(StateMachine sa,
            StateMachineRuntime saRt, Object operand) {
        if (operand instanceof OperandReturnBool) {
            return OpRetBoolExecutor.execute(sa, saRt, operand);
        } else if (operand instanceof OperandCompareInt) {
            return OpCmpIntExecutor.execute(sa, saRt, operand);
        } else if (operand instanceof OperandCompareOperand) {
            return OpCmpOpExecutor.execute(sa, saRt, operand);
        }

        return false;
    }

    public interface ExprExecutor {
        boolean execute(StateMachine sa, StateMachineRuntime saRt,
                Object operand);
    }

    final private static ExprExecutor OpRetBoolExecutor = new ExprExecutor() {
        public boolean execute(StateMachine sm, StateMachineRuntime saRt,
                Object operand) {
            OperandReturnBool orb = (OperandReturnBool) operand;
            ManagedObjectHelper moh = sm.getManagedObjectHelper(orb.id);
            if (moh == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong managed object identifier \"" + orb.id
                                + "\" specified");
            }

            Method method = moh.getMethod(MethodType.BOOL, orb.name);
            if (method == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong method name \"" + orb.name + "\" specified");
            }

            boolean result = false;
            ManagedObject mo = saRt.getManagedObject(orb.id);
            try {
                result = (Boolean) method.invoke(mo);
            } catch (IllegalAccessException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            } catch (IllegalArgumentException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            } catch (InvocationTargetException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            }

            return orb.isNotSet ? !result : result;
        }
    };

    final private static ExprExecutor OpCmpIntExecutor = new ExprExecutor() {
        public boolean execute(StateMachine sm, StateMachineRuntime saRt,
                Object operand) {
            OperandCompareInt oci = (OperandCompareInt) operand;
            ManagedObjectHelper moh = sm.getManagedObjectHelper(oci.id);
            if (moh == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong managed object identifier \"" + oci.id
                                + "\" specified");
            }
            Method method = moh.getMethod(MethodType.INT, oci.name);
            if (method == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong method name \"" + oci.name + "\" specified");
            }

            ManagedObject mo = saRt.getManagedObject(oci.id);
            try {
                int ret = (Integer) method.invoke(mo);
                switch (oci.opcode) {
                case EQ:
                    return ret == oci.value;

                case NEQ:
                    return ret != oci.value;

                case LT:
                    return ret < oci.value;

                case LE:
                    return ret <= oci.value;

                case GT:
                    return ret > oci.value;

                case GE:
                    return ret >= oci.value;
                }
            } catch (IllegalAccessException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            } catch (IllegalArgumentException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            } catch (InvocationTargetException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            }

            return false;
        }
    };

    final private static ExprExecutor OpCmpOpExecutor = new ExprExecutor() {
        public boolean execute(StateMachine sm, StateMachineRuntime saRt,
                Object operand) {
            OperandCompareOperand oco = (OperandCompareOperand) operand;
            ManagedObjectHelper moh1 = sm.getManagedObjectHelper(oco.id1);
            if (moh1 == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong managed object identifier \"" + oco.id1
                                + "\" specified");
            }
            Method method1 = moh1.getMethod(MethodType.INT, oco.name1);
            if (method1 == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong method name \"" + oco.name1 + "\" specified");
            }
            ManagedObject mo1 = saRt.getManagedObject(oco.id1);

            ManagedObjectHelper moh2 = sm.getManagedObjectHelper(oco.id2);
            if (moh2 == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong managed object identifier \"" + oco.id2
                                + "\" specified");
            }
            Method method2 = moh2.getMethod(MethodType.INT, oco.name2);
            if (method2 == null) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(),
                        "Wrong method name \"" + oco.name2 + "\" specified");
            }
            ManagedObject mo2 = saRt.getManagedObject(oco.id2);
            try {
                int ret1 = (Integer) method1.invoke(mo1);
                int ret2 = (Integer) method2.invoke(mo2);
                switch (oco.opcode) {
                case EQ:
                    return ret1 == ret2;

                case NEQ:
                    return ret1 != ret2;

                case LT:
                    return ret1 < ret2;

                case LE:
                    return ret1 <= ret2;

                case GT:
                    return ret1 > ret2;

                case GE:
                    return ret1 >= ret2;
                }
            } catch (IllegalAccessException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            } catch (IllegalArgumentException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            } catch (InvocationTargetException e) {
                Logger.log(TraceLevel.Error,
                        LogicExpression.class.getSimpleName(), e.getMessage());
            }

            return false;
        }
    };

    private enum LogicalOpcode {
        Or, And
    }

    private enum CompareOpcode {
        EQ, NEQ, LT, LE, GT, GE
    }

    private static class OperandReturnBool {
        public String id;
        public String name;
        public boolean isNotSet;
    }

    private static class OperandCompareInt {
        public String id;
        public String name;
        public CompareOpcode opcode;
        public int value;
    }

    private static class OperandCompareOperand {
        public String id1;
        public String name1;
        public CompareOpcode opcode;
        public String id2;
        public String name2;
    }

    private class Operations {
        ArrayList<Object> operands = new ArrayList<Object>();
        ArrayList<LogicalOpcode> opcodes = new ArrayList<LogicalOpcode>();
    }

    static {
        operators.put("==", CompareOpcode.EQ);
        operators.put("!=", CompareOpcode.NEQ);
        operators.put("<", CompareOpcode.LT);
        operators.put("<=", CompareOpcode.LE);
        operators.put(">", CompareOpcode.GT);
        operators.put(">=", CompareOpcode.GE);
    }
}
