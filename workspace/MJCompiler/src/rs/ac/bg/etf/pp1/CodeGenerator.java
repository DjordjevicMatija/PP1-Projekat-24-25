package rs.ac.bg.etf.pp1;

import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor{
    private int mainPc;

    public CodeGenerator() {
        // Generisanje koda za predeklarisane metode chr, ord, len, add i addAll
        generateChrAndOrd();
        generateLen();
        generateAdd();
        generateAddAll();
    }

    private void generateChrAndOrd() {
        // Enter
        visitMethodName(SymbolTable.chrObj);
        SymbolTable.ordObj.setAdr(SymbolTable.chrObj.getAdr());
        // Body
        Code.put(Code.load_n);
        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void generateLen() {
        // Enter
        visitMethodName(SymbolTable.lenObj);
        // Body
        Code.put(Code.load_n);
        Code.put(Code.arraylength);
        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void generateAdd() {
        // Enter
        visitMethodName(SymbolTable.addObj);

        // Body
        // Stavi na stack set, newElem i elemCnt
        Code.put(Code.load_n); // set
        Code.put(Code.load_1); // set newElem
        Code.put(Code.load_n); // set newElem set
        Code.loadConst(0); // set newElem set 0
        Code.put(Code.aload); // set newElem elemCnt

        // Enter sub-method
        Code.put(Code.enter);
        Code.put(3); // formParam: set[0], newElem[1], elemCnt[2]
        Code.put(4); // localVar: i[3]

        Code.loadConst(1); // 1
        Code.put(Code.store_3); // {i = 1}

        // Prodji kroz sve elemente seta i proveri da li vec postoji taj element
        int loopStartAdr = Code.pc;

        Code.put(Code.load_3); // i
        Code.put(Code.load_2); // elemCnt

        // if (i > elemCnt) -> exit loop
        Code.putFalseJump(Code.le, 0);
        int addElemAdr = Code.pc - 2;

        Code.put(Code.load_n); // set
        Code.put(Code.load_3); // set i
        Code.put(Code.aload); // set[i]
        Code.put(Code.load_1); // set[i] newElem

        // if (set[i] == newElem) -> return
        Code.putFalseJump(Code.ne, 0);
        int returnAdr = Code.pc - 2;

        // i++
        Code.put(Code.load_3); // i
        Code.loadConst(1); // i 1
        Code.put(Code.add); // i+1
        Code.put(Code.store_3); //

        // Jump to loop
        Code.putJump(loopStartAdr);

        // Dodaj novi element u set
        Code.fixup(addElemAdr);

        Code.put(Code.load_n); // set
        Code.put(Code.dup); // set set
        Code.put(Code.load_3); // set set i
        Code.put(Code.load_1); // set set i newElem
        Code.put(Code.astore); // set
        Code.loadConst(0); // set 0
        Code.put(Code.load_3); // set 0 i
        Code.put(Code.astore);

        // Exit sub-method
        Code.fixup(returnAdr);
        Code.put(Code.exit);

        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void generateAddAll() {
        // Enter
        visitMethodName(SymbolTable.addAllObj);

        // Body
        // Stavi na stack set, arr, elemCnt
        Code.put(Code.load_n); // set
        Code.put(Code.load_1); // set arr
        Code.put(Code.dup); // set arr arr
        Code.put(Code.arraylength); // set arr arrCnt

        // Enter sub-method
        Code.put(Code.enter);
        Code.put(3); // formParam: set[0], arr[1], arrCnt[2]
        Code.put(4); // localVar: i[3]

        // Prodji kroz sve elemente niza i probaj da ih dodas u set
        int loopStartAdr = Code.pc;

        Code.put(Code.load_3); // i
        Code.put(Code.load_2); // arrCnt

        // if (i >= arrCnt) -> return
        Code.putFalseJump(Code.lt, 0);
        int returnAdr = Code.pc - 2;

        Code.put(Code.load_n); // set
        Code.put(Code.load_1); // set arr
        Code.put(Code.load_3); // set arr i
        Code.put(Code.aload); // set arr[i]

        // Pozovi add(set, arr[i])
        int offset = SymbolTable.addObj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);

        // i++
        Code.put(Code.load_3); // i
        Code.loadConst(1); // i 1
        Code.put(Code.add); // i+1
        Code.put(Code.store_3); //

        // Jump to loop
        Code.putJump(loopStartAdr);

        // Exit sub-method
        Code.fixup(returnAdr);
        Code.put(Code.exit);

        // Exit
        Code.put(Code.exit);
        Code.put(Code.return_);
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
        if (type.equals(SymbolTable.setType)) {
            // Povecavamo duzinu seta za 1, i u 1. elementu cuvano trenutni broj elemenata
            Code.loadConst(1);
            Code.put(Code.add);
        }

        Code.put(Code.newarray);
        if (type.equals(SymbolTable.charType)) {
            Code.put(0);
        } else {
            Code.put(1);
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

    //EXPR
    @Override
    public void visit(MapExpr expr) {
        Obj funcObj = expr.getDesignator().obj;
        Obj arrObj = expr.getDesignator1().obj;

        Code.load(arrObj); // arr
        Code.put(Code.dup); // arr arr
        Code.put(Code.arraylength); // arr arrCnt

        // Enter
        Code.put(Code.enter);
        Code.put(2); // formParam: arr[0] arrCnt[1]
        Code.put(4); // localVar: i[2], sum[3]

        // Prodji kroz sve elemente niza, pozovi funkciju funcObj za svaki element i uvecaj sum
        int loopStartAdr = Code.pc;

        Code.put(Code.load_2); // i
        Code.put(Code.load_1); // arrCnt

        // if (i >= arrCnt) -> return
        Code.putFalseJump(Code.lt, 0);
        int returnAddr = Code.pc - 2;

        Code.put(Code.load_n); // arr
        Code.put(Code.load_2); // arr i
        Code.put(Code.aload); // arr[i]

        // Pozovi f(arr[i])
        int offset = funcObj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset); // res

        // sum += res
        Code.put(Code.load_3); // res sum
        Code.put(Code.add); // res+sum
        Code.put(Code.store_3); //

        // i++
        Code.put(Code.load_2); // i
        Code.loadConst(1); // i 1
        Code.put(Code.add); // i+1
        Code.put(Code.store_2); //

        // Jump to loop
        Code.putJump(loopStartAdr);

        // Exit
        Code.fixup(returnAddr);
        Code.put(Code.load_3); // sum
        Code.put(Code.exit);
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

    @Override
    public void visit(DesignUnion designUnion) {
        Obj resultSetObj = designUnion.getDesignator().obj;
        Obj firstOpSetObj = designUnion.getDesignator1().obj;
        Obj secondOpSetObj = designUnion.getDesignator2().obj;

        addAllSet(resultSetObj, firstOpSetObj);
        addAllSet(resultSetObj, secondOpSetObj);
    }

    private void addAllSet(Obj resultSetObj, Obj opSetObj) {
        Code.load(resultSetObj); // resSet
        Code.load(opSetObj); // resSet opSet
        Code.put(Code.dup); // resSet opSet opSet
        Code.loadConst(0); // resSet opSet opSet 0
        Code.put(Code.aload); // resSet opSet opSetCnt

        // Enter
        Code.put(Code.enter);
        Code.put(3); // formParam: resSet[0], opSet[1], opSetCnt[2]
        Code.put(4); // localVar: i[3]

        Code.loadConst(1); // 1
        Code.put(Code.store_3); // {i = 1}

        // Prodji kroz sve elemente opSet-a i probaj da ih dodas u resSet
        int loopStartAdr = Code.pc;

        Code.put(Code.load_3); // i
        Code.put(Code.load_2); // opSetCnt

        // if (i > opSetCnt) -> return
        Code.putFalseJump(Code.le, 0);
        int returnAdr = Code.pc - 2;

        Code.put(Code.load_n); // resSet
        Code.put(Code.load_1); // resSet opSet
        Code.put(Code.load_3); // resSet opSet i
        Code.put(Code.aload); // resSet opSet[i]

        // Pozovi add(resSet, opSet[i])
        int offset = SymbolTable.addObj.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);

        // i++
        Code.put(Code.load_3); // i
        Code.loadConst(1); // i 1
        Code.put(Code.add); // i+1
        Code.put(Code.store_3); //

        // Jump to loop
        Code.putJump(loopStartAdr);

        // Exit
        Code.fixup(returnAdr);
        Code.put(Code.exit);
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
        visitPrintStatement(0, printStmt.getExpr().struct);
    }

    @Override
    public void visit(PrintExprNumber printStmt) {
        visitPrintStatement(printStmt.getN2(), printStmt.getExpr().struct);
    }

    private void visitPrintStatement(int numConst, Struct type) {
        if (type.equals(SymbolTable.setType)) {
            printSet(numConst);
        } else if (type.equals(SymbolTable.charType)) {
            Code.loadConst(numConst);
            Code.put(Code.bprint);
        } else {
            Code.loadConst(numConst);
            Code.put(Code.print);
        }
    }

    private void printSet(int numConst) {
        // Enter
        Code.put(Code.dup); // set set
        Code.loadConst(0); // set set 0
        Code.put(Code.aload); // set elemCnt
        Code.put(Code.enter);
        Code.put(2); // formParam: set[0], elemCnt[1]
        Code.put(3); // localVar: i[2]

        Code.loadConst(1); // 1
        Code.put(Code.store_2); // {i = 1}

        // Prodji kroz sve elemente seta i ispisi ih
        int loopStartAdr = Code.pc;

        Code.put(Code.load_2); // i
        Code.put(Code.load_1); // elemCnt

        // if (i > elemCnt) -> return
        Code.putFalseJump(Code.le, 0);
        int returnAdr = Code.pc - 2;

        Code.put(Code.load_n); // set
        Code.put(Code.load_2); // set i
        Code.put(Code.aload); // set[i]
        Code.loadConst(numConst); // set[i] numConst
        Code.put(Code.print); //
        Code.loadConst(' '); // ' '
        Code.loadConst(0); // ' ' 0
        Code.put(Code.bprint); //

        // i++
        Code.put(Code.load_2); // i
        Code.loadConst(1); // i 1
        Code.put(Code.add); // i+1
        Code.put(Code.store_2); //

        // Jump to loop
        Code.putJump(loopStartAdr);

        // Exit
        Code.fixup(returnAdr);
        Code.put(Code.exit);
    }

    //CONDITIONS AND BRANCHES
    private Stack<Integer> skipCondTerm = new Stack<>();
    private Stack<Integer> skipCondition = new Stack<>();
    private Stack<Integer> skipThenBlock = new Stack<>();
    private Stack<Integer> skipElseBlock = new Stack<>();

    private int returnRelop(Relop relop) {
        if (relop instanceof RelopEquals) {
            return Code.eq;
        } else if (relop instanceof RelopNotEquals) {
            return Code.ne;
        } else if (relop instanceof RelopGreater) {
            return Code.gt;
        } else if (relop instanceof RelopGreaterEqual) {
            return Code.ge;
        } else if (relop instanceof RelopLess) {
            return Code.lt;
        } else {
            // relop instanceof RelopLessEqual
            return Code.le;
        }
    }

    //COND FACT
    @Override
    public void visit(SingleCondition cond) {
        Code.loadConst(0);
        // cond = fales -> skoci na sledeci OR, ili na THEN blok
        Code.putFalseJump(Code.ne, 0);
        skipCondTerm.push(Code.pc - 2);
        // cond = true -> nastavi izvrsavanje
    }

    @Override
    public void visit(ConditionList cond) {
        int relop = returnRelop(cond.getRelop());
        // cond = fales -> skoci na sledeci OR, ili na THEN blok
        Code.putFalseJump(relop, 0);
        skipCondTerm.push(Code.pc - 2);
        // cond = true -> nastavi izvrsavanje
    }

    //COND TERM
    @Override
    public void visit(CondTerm cond) {
        // Ovaj cvor predstavlja kraj jednog OR i pocetak sledeceg

        // cond = true -> skoci na THEN blok
        Code.putJump(0);
        skipCondition.push(Code.pc - 2);

        // cond = false
        // Ovo je pocetak sledeceg OR-a -> popuni skokove za
        // prethodne cond = false
        while (!skipCondTerm.isEmpty()) {
            Code.fixup(skipCondTerm.pop());
        }
    }

    //CONDITION
    /*
     * Ako je CONDITION false -> skace se na skipThenBlock (treba zakrpiti ovu adresu)
     * Ako je CONDITION true -> nastavlja se izvrsavanje koda nakon obilaska CONDITION-a
     */
    @Override
    public void visit(Condition cond) {
        // Ovaj cvor predstavlja pocetak THEN bloka

        // cond = false -> skoci na pocetak ELSE bloka/kraj THEN bloka
        Code.putJump(0);
        skipThenBlock.push(Code.pc - 2);

        // cond = true
        // Ovo je pocetak THEN bloka -> popuni skokove za
        // prethodne cond = true
        while (!skipCondition.isEmpty()) {
            Code.fixup(skipCondition.pop());
        }
    }

    //ELSE
    @Override
    public void visit(Else else_) {
        // Ovaj cvor predstavlja pocetak ELSE bloka/kraj THEN bloka

        // THEN blok se zavrsio -> skoci iza ELSE bloka
        Code.putJump(0);
        skipElseBlock.push(Code.pc - 2);

        // Popuni skokove za cond = false
        Code.fixup(skipThenBlock.pop());
    }

    //UNMATCHED STATEMENT
    @Override
    public void visit(UnmatchedIf unmatchedIf) {
        // Ovaj cvor predstavlja kraj THEN bloka
        Code.fixup(skipThenBlock.pop());
    }

    @Override
    public void visit(UnmatchedIfElse nmatchedIfElse) {
        // Ovaj cvor predstavlja kraj ELSE bloka
        Code.fixup(skipElseBlock.pop());
    }


    //MATCHED STATEMENT - BRANCHES
    @Override
    public void visit(MatchedIf matchedIf) {
        // Ovaj cvor predstavlja kraj ELSE bloka
        Code.fixup(skipElseBlock.pop());
    }

    //LOOPS
    private Stack<Integer> doBegin = new Stack<>();
    private Stack<Stack<Integer>> whileBegin = new Stack<>();
    private Stack<Stack<Integer>> whileEnd = new Stack<>();

    //DO-WHILE
    @Override
    public void visit(DoTerm doTerm) {
        doBegin.push(Code.pc);
        whileBegin.push(new Stack<Integer>());
        whileEnd.push(new Stack<Integer>());
    }

    @Override
    public void visit(WhileTerm whileTerm) {
        // Pocetak WHILE uslova -> popuni sve continue iskaze
        if (!whileBegin.isEmpty()) {
            Stack<Integer> whileBeginStack = whileBegin.pop();
            while (!whileBeginStack.isEmpty()) {
                Code.fixup(whileBeginStack.pop());
            }
        }
    }

    //MATCHED STATEMENT - LOOPS
    @Override
    public void visit(BreakStmt breakStmt) {
        Code.putJump(0);
        whileEnd.peek().push(Code.pc - 2);
    }

    @Override
    public void visit(ContinueStmt continueStmt) {
        Code.putJump(0);
        whileBegin.peek().push(Code.pc - 2);
    }

    @Override
    public void visit(DoWhileStmt DoWhileStmt) {
        visitDoWhile();
    }

    @Override
    public void visit(DoWhileConditionStmt DoWhileStmt) {
        visitDoWhile();
    }

    @Override
    public void visit(DoWhileConditionStepStmt DoWhileStmt) {
        visitDoWhile();
    }

    private void visitDoWhile() {
        // Ovaj cvor predstavlja kraj tela WHILE petlje
        Code.putJump(doBegin.pop());

        // Potrebna je provera da li nije empty, jer postoji WHILE bez uslova
        if (!skipThenBlock.isEmpty()) {
            Code.fixup(skipThenBlock.pop());
        }

        // Kraj WHILE petlje -> popuni sve break iskaze
        if (!whileEnd.isEmpty()) {
            Stack<Integer> whileEndStack = whileEnd.pop();
            while (!whileEndStack.isEmpty()) {
                Code.fixup(whileEndStack.pop());
            }
        }
    }
}
