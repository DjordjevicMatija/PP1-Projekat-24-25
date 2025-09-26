package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor{
    private int mainPc;

    public CodeGenerator() {
        // Generisanje koda za predeklarisane metode chr, ord, add i addAll
        generateChr();
        generateOrd();
        generateAdd();
        generateAddAll();
    }

    private void generateChr() {
        Obj chrObj = SymbolTable.find("chr");
        // Enter
        visitMethodName(chrObj);
        // Body
        Code.put(Code.load_n);
        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void generateOrd() {
        Obj ordObj = SymbolTable.find("ord");
        // Enter
        visitMethodName(ordObj);
        // Body
        Code.put(Code.load_n);
        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void generateAdd() {
        //TODO: Implementiraj posle skokova
        Obj addObj = SymbolTable.find("add");
        // Enter
        visitMethodName(addObj);
        // Body
        // Stavi na stack adr, len i newElem
        Code.put(Code.load_n);
        Code.put(Code.load_n);
        Code.put(Code.arraylength);
        Code.put(Code.load_1);
        Code.put(Code.enter);
        Code.put(3);
        Code.put(4);

        // Prodji kroz sve elemente seta i proveri da li vec postoji taj element
        // Dohvati element i
        Code.put(Code.load_n);
        Code.put(Code.load_3);
        Code.put(Code.aload);
        Code.put(Code.dup);
        // Proveri da li je 0


        Code.put(Code.exit);
        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void generateAddAll() {
        //TODO: Implementiraj posle skokova
    }

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
        methodObj.setAdr(Code.pc);

        Code.put(Code.enter);
        Code.put(methodObj.getLevel());
        Code.put(methodObj.getLocalSymbols().size());
    }

    @Override
    public void visit(MethodDecl method) {
        // Check if this is main method and set mainPc
        if (SemanticPass.checkMain(method)) {
            mainPc = method.getMethodName().obj.getAdr();
        }

        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    //DESIGNATOR
    @Override
    public void visit(DesignatorName name) {
        Code.load(name.obj);
    }

    //FACTOR
    @Override
    public void visit(DesignFactor factorVar) {
        Code.load(factorVar.getDesignator().obj);
    }

    @Override
    public void visit(DesingFuncFactor funcFactor) {
        int offset = funcFactor.getDesignator().obj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);
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
        Struct type = factorNew.getType().struct;
        Code.put(Code.newarray);
        if (type.equals(SymbolTable.setType)) {
            //TODO: Implement set creation - mislim da set moze da se kreira
            // isto kao i niz, samo su posle operacije drugacije
            Code.put(1);
        } else if (!type.equals(SymbolTable.charType)) {
            Code.put(1);
        } else {
            Code.put(0);
        }
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

    //DESIGNATOR STATEMENT
    @Override
    public void visit(DesignAssign designAssign) {
        Code.store(designAssign.getDesignator().obj);
    }

    @Override
    public void visit(DesignFunc designFunc) {
        Obj funcObj = designFunc.getDesignator().obj;
        int offset = funcObj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);

        if (!funcObj.getType().equals(SymbolTable.noType)) {
            Code.put(Code.pop);
        }
    }

    @Override
    public void visit(DesignInc designInc) {
        Obj designatorObj = designInc.getDesignator().obj;
        visitIncDec(designatorObj, true);
    }

    @Override
    public void visit(DesignDec designDec) {
        Obj designatorObj = designDec.getDesignator().obj;
        visitIncDec(designatorObj, false);
    }

    private void visitIncDec(Obj designatorObj, boolean inc) {
        if (designatorObj.getKind() == Obj.Elem) {
            Code.put(Code.dup2);
        }
        Code.load(designatorObj);
        Code.loadConst(1);
        Code.put(inc ? Code.add : Code.sub);
        Code.store(designatorObj);
    }

    //MATCHED STATEMENT
    @Override
    public void visit(ReturnStmt returnStmt) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    @Override
    public void visit(ReturnExprStmt returnStmt) {
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    @Override
    public void visit(ReadStmt readStmt) {
        Obj designatorObj = readStmt.getDesignator().obj;
        if (designatorObj.getType().equals(SymbolTable.charType)) {
            Code.put(Code.bread);
        } else {
            Code.put(Code.read);
        }
        Code.store(designatorObj);
    }

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
        if (type.equals(SymbolTable.setType)) {
            //TODO: Implement set print
        } else if (type.equals(SymbolTable.charType)) {
            Code.put(Code.bprint);
        } else {
            Code.put(Code.print);
        }
    }
}
