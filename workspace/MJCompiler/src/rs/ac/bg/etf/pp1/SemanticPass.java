package rs.ac.bg.etf.pp1;

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
    private boolean mainExists = false;

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
        if(!mainExists){
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
            mainExists = true;
        }

        if (!returnFound && !currentMethod.getType().equals(SymbolTable.noType)) {
            report_error("Metoda " + method.getMethodName().obj.getName() + " nema return iskaz", method);
        }

        SymbolTable.chainLocalSymbols(currentMethod);
        SymbolTable.closeScope();

        currentMethod = null;
    }

    // FORM PARS
    @Override
    public void visit(FormParamElement formParam) {
        Obj obj = SymbolTable.currentScope.findSymbol(formParam.getParamName());
        if (obj == null) {
            SymbolTable.insert(Obj.Var, formParam.getParamName(), currentType);
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
            SymbolTable.insert(Obj.Var, formParam.getParamName(), new Struct(Struct.Array, currentType));
            currentMethod.setLevel(currentMethod.getLevel() + 1);
            report_info("Deklarisan formalni parametar " + formParam.getParamName(), formParam);
        } else {
            report_error("Simbol " + formParam.getParamName() + " je vec deklarisan", formParam);
        }
    }

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
            report_info("Detektovan simbol " + designator.getDesignName(), designator);
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
            report_info("Detektovan simbol " + name.getDesignName(), name);
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
        funcFactor.struct = funcFactor.getDesignator().obj.getType();
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
        if(factorNew.getExpr().struct.equals(SymbolTable.intType)){
            report_error("Velicina niza nije tipa int", factorNew);
            factorNew.struct = SymbolTable.noType;
        }
        else{
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
}
