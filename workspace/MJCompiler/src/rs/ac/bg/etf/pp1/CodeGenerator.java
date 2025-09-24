package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor{
    private int mainPc;

    public int getMainPc(){
        return mainPc;
    }

    //METHOD
    @Override
    public void visit(TypeMethodName methodName) {
        visitMethodName(methodName.obj);
    }

    @Override
    public void visit(VoidMethodName methodName) {
        visitMethodName(methodName.obj);
    }

    private void visitMethodName(Obj methodObj) {
        Code.put(Code.enter);
        Code.put(methodObj.getLevel());
        Code.put(methodObj.getLocalSymbols().size());
    }

    @Override
    public void visit(MethodDecl method) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    //FACTOR
    @Override
    public void visit(DesignFactor factorVar) {
        Code.load(factorVar.getDesignator().obj);
    }

    @Override
    public void visit(DesingFuncFactor funcFactor) {
        //TODO
    }

    @Override
    public void visit(FactorNumber factorNumber) {
        Code.loadConst(factorNumber.getN1());
    }

    @Override
    public void visit(FactorChar factorChar) {
        Code.loadConst(factorChar.getC1());
    }

    @Override
    public void visit(FactorBool factorBool) {
        Code.loadConst(factorBool.getB1() ? 1 : 0);
    }


    @Override
    public void visit(FactorNew factorNew) {
        //TODO
    }

    //TERM
    @Override
    public void visit(Factors term) {
        if (term.getMulop() instanceof MulopMul) {
            Code.put(Code.mul);
        } else if (term.getMulop() instanceof MulopDiv) {
            Code.put(Code.div);
        } else if (term.getMulop() instanceof MulopMod) {
            Code.put(Code.rem);
        }
    }

    //TERM LIST
    @Override
    public void visit(TerminalList term) {
        if (term.getAddop() instanceof AddopPlus) {
            Code.put(Code.add);
        } else if (term.getAddop() instanceof AddopMinus) {
            Code.put(Code.sub);
        }
    }

    @Override
    public void visit(NegativeTerm term) {
        Code.put(Code.neg);
    }

    //MATCHED STATEMENT
    @Override
    public void visit(PrintExpr printStmt) {
        Code.loadConst(0);
        visitPrintStatement(printStmt.getExpr().struct);
    }

    @Override
    public void visit(PrintExprNumber printStmt) {
        Code.loadConst(printStmt.getN2());
        visitPrintStatement(printStmt.getExpr().struct);
    }

    private void visitPrintStatement(Struct type) {
        if (type.equals(SymbolTable.charType)) {
            Code.put(Code.bprint);
        } else if (type.equals(SymbolTable.setType)) {
            //TODO: Implement set print
        } else {
            Code.put(Code.print);
        }
    }

}
