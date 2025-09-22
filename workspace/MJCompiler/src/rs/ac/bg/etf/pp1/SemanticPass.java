package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor {
    Logger log = Logger.getLogger(RuleVisitor.class);

    private boolean errorDetected = false;

    private Struct currentType = null;
    private Obj currentMethod = null;
    private boolean returnFound = false;
    private int mainExists = 0;

    private int loopDepth = 0;
    private boolean insideLoop = false;

    Stack<List<Struct>> actParsStack = new Stack<>();
    private List<Struct> currentActPars = new ArrayList<>();

    // LOGS
    public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder("\nSemanticka greska: ");
        msg.append(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}

    // SUCCESSFUL PASS
    public boolean success(){
        return !errorDetected;
    }

    // PROGRAM
    @Override
    public void visit(ProgName progName) {
        progName.obj = SymbolTable.insert(Obj.Prog, progName.getProgName(), SymbolTable.noType);
        SymbolTable.openScope();
    }

    @Override
    public void visit(Program program) {
        SymbolTable.chainLocalSymbols(program.getProgName().obj);
        SymbolTable.closeScope();
        if (mainExists < 1){
            report_error("Ne postoji metoda void main()", program);
        }
    }

    // TYPE
    @Override
    public void visit(Type type){
        Obj typeNode = SymbolTable.find(type.getTypeName());
		if (typeNode == SymbolTable.noObj) {
            report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola", type);
			currentType = SymbolTable.noType;
		} else if (typeNode.getKind() != Obj.Type) {
            report_error("Ime " + type.getTypeName() + " ne predstavlja tip", type);
            currentType = SymbolTable.noType;
		} else {
            currentType = typeNode.getType();
        }
    }

    // CONST
    @Override
    public void visit(NumberConst constant){
        if (currentType == SymbolTable.intType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                obj = SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
                obj.setAdr(constant.getN1());
                report_info("Deklarisana konstanta " + constant.getConstName(), constant);
            } else {
                report_error("Simbol " + constant.getConstName() + " je vec deklarisan", constant);
            }
        } else {
            report_error("Tip konstante " + constant.getConstName() + " nije ekvivalentan tipu proslednjenog podatka", constant);
        }
    }

    @Override
    public void visit(CharConst constant){
        if (currentType == SymbolTable.charType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                obj = SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
                obj.setAdr(constant.getC1());
                report_info("Deklarisana konstanta " + constant.getConstName(), constant);
            } else {
                report_error("Simbol " + constant.getConstName() + " je vec deklarisan", constant);
            }
        } else {
            report_error("Tip konstante " + constant.getConstName() + " nije ekvivalentan tipu proslednjenog podatka", constant);
        }
    }

    @Override
    public void visit(BoolConst constant){
        if (currentType == SymbolTable.boolType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                obj = SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
                obj.setAdr(constant.getB1() ? 1 : 0);
                report_info("Deklarisana konstanta " + constant.getConstName(), constant);
            } else {
                report_error("Simbol " + constant.getConstName() + " je vec deklarisan", constant);
            }
        } else {
            report_error("Tip konstante " + constant.getConstName() + " nije ekvivalentan tipu proslednjenog podatka", constant);
        }
    }

    // VAR
    @Override
    public void visit(VarElem var){
        Obj obj = SymbolTable.currentScope.findSymbol(var.getVarName());
        if (obj == null) {
            SymbolTable.insert(Obj.Var, var.getVarName(), currentType);
            report_info("Deklarisana promenljiva " + var.getVarName(), var);
        } else {
            report_error("Simbol " + var.getVarName() + " je vec deklarisan", var);
        }
    }

    @Override
    public void visit(VarArray var){
        Obj obj = SymbolTable.currentScope.findSymbol(var.getVarName());
        if (obj == null) {
            SymbolTable.insert(Obj.Var, var.getVarName(), new Struct(Struct.Array, currentType));
            report_info("Deklarisana promenljiva " + var.getVarName(), var);
        } else {
            report_error("Simbol " + var.getVarName() + " je vec deklarisan", var);
        }
    }

    //METHOD
    public static boolean checkMain(MethodDecl method){
        return (
            method.getMethodPars() instanceof NoMethodParameters
            && method.getMethodName() instanceof VoidMethodName
            && "main".equals(((VoidMethodName) method.getMethodName()).getMethodName())
        );
    }

    @Override
    public void visit(TypeMethodName methodName) {
        methodName.obj = currentMethod = SymbolTable.insert(Obj.Meth, methodName.getMethodName(), currentType);
        report_info("Deklarisana metoda " + methodName.getMethodName(), methodName);
        SymbolTable.openScope();
    }

    @Override
    public void visit(VoidMethodName methodName) {
        methodName.obj = currentMethod = SymbolTable.insert(Obj.Meth, methodName.getMethodName(), SymbolTable.noType);
        report_info("Deklarisana metoda " + methodName.getMethodName(), methodName);
        SymbolTable.openScope();
    }

    @Override
    public void visit(MethodDecl method) {
        if(checkMain(method)){
            mainExists++;
        }

        if (mainExists > 1) {
            report_error("Metoda main je vec deklarisana", method);
        }

        if (!returnFound && !currentMethod.getType().equals(SymbolTable.noType)) {
            report_error("Metoda " + method.getMethodName().obj.getName() + " nema return iskaz", method);
        }

        SymbolTable.chainLocalSymbols(currentMethod);
        SymbolTable.closeScope();

        currentMethod = null;
    }

    //FORM PARAM
    @Override
    public void visit(FormParamElement formParam) {
        Obj obj = SymbolTable.currentScope.findSymbol(formParam.getParamName());
        if (obj == null) {
            obj = SymbolTable.insert(Obj.Var, formParam.getParamName(), currentType);
            obj.setFpPos(1);
            currentMethod.setLevel(currentMethod.getLevel() + 1);
            report_info("Deklarisan formalni parametar " + formParam.getParamName(), formParam);
        } else {
            report_error("Simbol " + formParam.getParamName() + " je vec deklarisan", formParam);
        }
    }

    @Override
    public void visit(FormParamArray formParam) {
        Obj obj = SymbolTable.currentScope.findSymbol(formParam.getParamName());
        if (obj == null) {
            obj = SymbolTable.insert(Obj.Var, formParam.getParamName(), new Struct(Struct.Array, currentType));
            obj.setFpPos(1);
            currentMethod.setLevel(currentMethod.getLevel() + 1);
            report_info("Deklarisan formalni parametar " + formParam.getParamName(), formParam);
        } else {
            report_error("Simbol " + formParam.getParamName() + " je vec deklarisan", formParam);
        }
    }

    //FORM PARS


    //DESIGNATOR
    @Override
    public void visit(DesignatorElement designator) {
        Obj obj = SymbolTable.find(designator.getDesignName());
        if (obj == SymbolTable.noObj) {
            report_error("Simbol " + designator.getDesignName() + " nije deklarisan", designator);
            designator.obj = SymbolTable.noObj;
        } else if (
            obj.getKind() != Obj.Var &&
            obj.getKind() != Obj.Con &&
            obj.getKind() != Obj.Meth
        ) {
            report_error("Neadekatna promenljiva " + designator.getDesignName(), designator);
            designator.obj = SymbolTable.noObj;
        } else {
            designator.obj = obj;
        }
    }

    @Override
    public void visit(DesignatorName name) {
        Obj obj = SymbolTable.find(name.getDesignName());
        if (obj == SymbolTable.noObj) {
            report_error("Simbol " + name.getDesignName() + " nije deklarisan", name);
            name.obj = SymbolTable.noObj;
        } else if (obj.getKind() != Obj.Var && obj.getType().getKind() != Struct.Array) {
            report_error("Neadekatna promenljiva " + name.getDesignName(), name);
            name.obj = SymbolTable.noObj;
        } else {
            name.obj = obj;
        }

    }

    @Override
    public void visit(DesignatorArray designatorArray) {
        Obj obj = designatorArray.getDesignatorName().obj;
        if (obj == SymbolTable.noObj) {
            designatorArray.obj = SymbolTable.noObj;
        } else if (!designatorArray.getExpr().struct.equals(SymbolTable.intType)) {
            report_error("Na referencirani element niza mora ukazivati int", designatorArray);
        } else {
            designatorArray.obj = new Obj(Obj.Elem, obj.getName() + "[$]", obj.getType().getElemType());
        }
    }

    //FACTOR
    @Override
    public void visit(DesignFactor factorVar) {
        factorVar.struct = factorVar.getDesignator().obj.getType();
    }

    @Override
    public void visit(DesingFuncFactor funcFactor) {
        Obj funcObj = funcFactor.getDesignator().obj;
        if (funcObj.getKind() != Obj.Meth) {
            report_error("Simbol " + funcObj.getName() + " nije funkcija", funcFactor);
            funcFactor.struct = SymbolTable.noType;
        } else {
            funcFactor.struct = funcFactor.getDesignator().obj.getType();

            checkActPars(funcObj, funcFactor);
        }
    }

    private void checkActPars(Obj funcObj, SyntaxNode funcNode) {
        List<Struct> formPars = new ArrayList<>();
        for (Obj local: funcObj.getLocalSymbols()) {
            if (local.getKind() == Obj.Var && local.getLevel() == 1 && local.getFpPos() == 1) {
                formPars.add(local.getType());
            }
        }

        if (formPars.size() != currentActPars.size()) {
            // Provera broja argumenata
            report_error(
                "Neodgovarajuci broj parametara pri pozivu metode " + funcObj.getName(),
                funcNode
            );
        } else {
            // Provera tipa argumenata
            for (int i = 0; i < formPars.size(); i++) {
                Struct actPar = currentActPars.get(i);
                Struct formPar = formPars.get(i);
                if (!actPar.assignableTo(formPar)) {
                    report_error(
                        "Neodgovarajuci tip parametara pri pozivu metode " + funcObj.getName(),
                        funcNode
                    );
                }
            }
        }
    }

    @Override
    public void visit(FactorNumber factorNumber) {
        factorNumber.struct = SymbolTable.intType;
    }

    @Override
    public void visit(FactorChar factorChar) {
        factorChar.struct = SymbolTable.charType;
    }

    @Override
    public void visit(FactorBool factorBool) {
        factorBool.struct = SymbolTable.boolType;
    }

    @Override
    public void visit(FactorNew factorNew) {
        if (!factorNew.getExpr().struct.equals(SymbolTable.intType)) {
            report_error("Velicina niza ili seta mora biti tipa int", factorNew);
            factorNew.struct = SymbolTable.noType;
        } else if (currentType.equals(SymbolTable.setType)) {
            factorNew.struct = SymbolTable.setType;
        } else {
            factorNew.struct = new Struct(Struct.Array, currentType);
        }
    }

    @Override
    public void visit(FactorExpr factorExpr) {
        factorExpr.struct = factorExpr.getExpr().struct;
    }

    //TERM
    @Override
    public void visit(SingleFactor term) {
        term.struct = term.getFactor().struct;
    }

    @Override
    public void visit(Factors term) {
        if (
            !term.getTerm().struct.equals(SymbolTable.intType) ||
            !term.getFactor().struct.equals(SymbolTable.intType)
        ) {
            report_error("Operandi operacije mnozenja moraju biti tipa int", term);
            term.struct = SymbolTable.noType;
        } else {
            term.struct = SymbolTable.intType;
        }
    }

    //TERM LIST
    @Override
    public void visit(SingleTerm term) {
        term.struct = term.getTerm().struct;
    }

    @Override
    public void visit(TerminalList term) {
        if (
            !term.getTerm().struct.equals(SymbolTable.intType) ||
            !term.getTermList().struct.equals(SymbolTable.intType)
        ) {
            report_error("Operandi operacije sabiranja moraju biti tipa int", term);
            term.struct = SymbolTable.noType;
        } else {
            term.struct = SymbolTable.intType;
        }
    }

    @Override
    public void visit(NegativeTerm term) {
        if (!term.getTerm().struct.equals(SymbolTable.intType)) {
            report_error("Izraz mora biti tipa int", term);
            term.struct = SymbolTable.noType;
        } else {
            term.struct = SymbolTable.intType;
        }
    }

    //EXPR
    @Override
    public void visit(TermListExpr expr) {
        expr.struct = expr.getTermList().struct;
    }

    @Override
    public void visit(MapExpr expr) {
        Obj funcObj = expr.getDesignator().obj;
        boolean funcCondition = funcObj.getKind() == Obj.Meth &&
            funcObj.getType().equals(SymbolTable.intType) &&
            funcObj.getLevel() == 1;

        Obj arrObj = expr.getDesignator1().obj;
        boolean arrCondition = arrObj.getKind() == Obj.Var &&
            arrObj.getType().getKind() == Struct.Array &&
            arrObj.getType().getElemType().equals(SymbolTable.intType);

        if (!funcCondition || !arrCondition) {
            report_error("Neispravan poziv funkcije map", expr);
            expr.struct = SymbolTable.noType;
        } else {
            expr.struct = SymbolTable.intType;
        }
    }

    //DESIGNATOR STATEMENT
    @Override
    public void visit(DesignAssign designAssign) {
        Obj designatorObj = designAssign.getDesignator().obj;
        Struct exprStruct = designAssign.getExpr().struct;
        if (!exprStruct.assignableTo(designatorObj.getType())) {
            report_error(
                "Nekompatabilni tipovi pri dodeli vrednosti",
                designAssign
            );
        }
    }

    @Override
    public void visit(DesignFunc designFunc) {
        Obj funcObj = designFunc.getDesignator().obj;
        if (funcObj.getKind() != Obj.Meth) {
            report_error("Simbol " + funcObj.getName() + " nije funkcija", designFunc);
        } else {
            checkActPars(funcObj, designFunc);
        }
    }

    @Override
    public void visit(DesignInc designInc) {
        Obj deisgnatorObj = designInc.getDesignator().obj;
        if (!isDesignatorInt(deisgnatorObj)) {
            report_error(
                "Promenljiva ili element niza mora biti tipa int za operaciju inc",
                designInc
            );
        }
    }

    @Override
    public void visit(DesignDec designDec) {
        Obj deisgnatorObj = designDec.getDesignator().obj;
        if (!isDesignatorInt(deisgnatorObj)) {
            report_error(
                "Promenljiva ili element niza mora biti tipa int za operaciju dec",
                designDec
            );
        }
    }

    private boolean isDesignatorInt(Obj designatorObj){
        return designatorObj.getType().equals(SymbolTable.intType) ||
            (
                designatorObj.getType().getKind() == Struct.Array &&
                designatorObj.getType().getElemType().equals(SymbolTable.intType)
            );
    }

    @Override
    public void visit(DesignUnion designUnion) {
        Obj resultDesignObj = designUnion.getDesignator().obj;
        Obj firstOpDesignObj = designUnion.getDesignator1().obj;
        Obj secondOpDesignObj = designUnion.getDesignator2().obj;

        if (
            !resultDesignObj.getType().equals(SymbolTable.setType) ||
            !firstOpDesignObj.getType().equals(SymbolTable.setType) ||
            !secondOpDesignObj.getType().equals(SymbolTable.setType)
        ) {
            report_error(
                "Svi operandi operacije unija moraju biti tipa set",
                designUnion
            );
        }
    }

    //UNMATCHED STATEMENT
    @Override
    public void visit(UnmatchedIf ifStmt) {
        visitIfStatement(ifStmt.getCondition().struct, ifStmt);
    }

    @Override
    public void visit(UnmatchedIfElse ifStmt) {
        visitIfStatement(ifStmt.getCondition().struct, ifStmt);
    }

    private void visitIfStatement(Struct conditionType, SyntaxNode ifStmt) {
        if (!conditionType.equals(SymbolTable.boolType)) {
            report_error("Uslov if izraza mora biti tipa bool", ifStmt);
        }
    }

    //MATCHED STATEMENT
    @Override
    public void visit(MatchedIf ifStmt) {
        visitIfStatement(ifStmt.getCondition().struct, ifStmt);
    }

    @Override
    public void visit(BreakStmt breakStmt) {
        if (!insideLoop) {
            report_error("Break iskaz nije unutar petlje", breakStmt);
        }
    }

    @Override
    public void visit(ContinueStmt continueStmt) {
        if (!insideLoop) {
            report_error("Continue iskaz nije unutar petlje", continueStmt);
        }
    }

    @Override
    public void visit(ReadStmt readStmt) {
        Obj designatorObj = readStmt.getDesignator().obj;
        if (
            !designatorObj.getType().equals(SymbolTable.intType) &&
            !designatorObj.getType().equals(SymbolTable.charType) &&
            !designatorObj.getType().equals(SymbolTable.boolType)
        ) {
            report_error(
                "Metoda read kao parametar prima samo int, char i bool tipove",
                readStmt
            );
        } else if (
            designatorObj.getKind() != Obj.Var &&
            designatorObj.getKind() != Obj.Elem
        ) {
            report_error(
                "Metoda read kao parametar prima samo promenljive i elemente niza",
                readStmt
            );
        }
    }

    @Override
    public void visit(PrintExpr printStmt) {
        visitPrintStatement(printStmt.getExpr().struct, printStmt);
    }

    @Override
    public void visit(PrintExprNumber printStmt) {
        visitPrintStatement(printStmt.getExpr().struct, printStmt);
    }

    private void visitPrintStatement(Struct type, SyntaxNode printStmt) {
        if (
            !type.equals(SymbolTable.intType) &&
            !type.equals(SymbolTable.charType) &&
            !type.equals(SymbolTable.boolType) &&
            !type.equals(SymbolTable.setType)
        ) {
            report_error(
                "Metoda print moze da se zove samo za tipove int, char, bool ili set",
                printStmt
            );
        }
    }

    @Override
    public void visit(ReturnStmt returnStmt) {
        visitReturnStatement(SymbolTable.noType, returnStmt);
    }

    @Override
    public void visit(ReturnExprStmt returnStmt) {
        visitReturnStatement(returnStmt.getExpr().struct, returnStmt);
    }

    private void visitReturnStatement(Struct returnType, SyntaxNode returnStmt) {
        if(currentMethod == null){
            report_error(
                "Return naredba mora da se nalazi unutar metode",
                returnStmt
            );
            return;
        }

        if(!currentMethod.getType().compatibleWith(returnType)){
            report_error(
                "Tip izraza u return naredbi se ne slaze sa povratnom vrednosti metode " + currentMethod.getName(),
                returnStmt
            );
        }
        returnFound = true;
    }

    @Override
    public void visit(DoWhileLoop doWhileLoop) {
        loopDepth++;
        if (loopDepth == 1) {
            insideLoop = true;
        }
    }

    @Override
    public void visit(DoWhileStmt doWhileStmt) {
        visitDoWhileStatement(null, doWhileStmt);
    }

    @Override
    public void visit(DoWhileConditionStmt doWhileStmt) {
        visitDoWhileStatement(doWhileStmt.getCondition().struct, doWhileStmt);
    }

    @Override
    public void visit(DoWhileConditionStepStmt doWhileStmt) {
        visitDoWhileStatement(doWhileStmt.getCondition().struct, doWhileStmt);
    }

    private void visitDoWhileStatement(Struct conditionType, SyntaxNode doWhileStmt) {
        loopDepth--;
        if (loopDepth == 0) {
            insideLoop = false;
        }

        if (conditionType != null && !conditionType.equals(SymbolTable.boolType)) {
            report_error(
                "Uslov do while izraza mora biti tipa bool",
                doWhileStmt
            );
        }
    }

    //COND FACT
    @Override
    public void visit(SingleCondition cond) {
        cond.struct = cond.getExpr().struct;
    }

    @Override
    public void visit(ConditionList cond) {
        Struct firstOpType = cond.getExpr().struct;
        Struct secondOpType = cond.getExpr1().struct;
        Relop relop = cond.getRelop();

        if (!firstOpType.compatibleWith(secondOpType)) {
            report_error("Tipovi nisu kompatabilni", cond);
            cond.struct = SymbolTable.noType;
        } else if (
            firstOpType.isRefType() &&
            (
                !(relop instanceof RelopEquals) ||
                !(relop instanceof RelopNotEquals)
            )
        ) {
            report_error("Uz nizove mogu da idu samo operatori \"==\" ili \"!=\"", cond);
            cond.struct = SymbolTable.noType;
        } else {
            cond.struct = SymbolTable.boolType;
        }
    }

    //COND TERM
    @Override
    public void visit(SingleConditionFact cond) {
        cond.struct = cond.getCondFact().struct;
    }

    @Override
    public void visit(ConditionFactList cond) {
        cond.struct = cond.getCondFact().struct;
    }

    //CONDITION
    @Override
    public void visit(SingleConditionTerm cond) {
        cond.struct = cond.getCondTerm().struct;
    }

    @Override
    public void visit(ConditionTermList cond) {
        cond.struct = cond.getCondTerm().struct;
    }

    //ACT PARS
    @Override
    public void visit(ActParsBegin actParsBegin) {
        actParsStack.push(new ArrayList<>());
    }

    @Override
    public void visit(SingleActParam actParam) {
        actParsStack.peek().add(actParam.getExpr().struct);
    }

    @Override
    public void visit(ActParamList actParam) {
        actParsStack.peek().add(actParam.getExpr().struct);
    }

    @Override
    public void visit(ActParameters actParams) {
        currentActPars = actParsStack.pop();
    }

    @Override
    public void visit(NoActParameters actParams) {
        currentActPars = actParsStack.pop();
    }
}
