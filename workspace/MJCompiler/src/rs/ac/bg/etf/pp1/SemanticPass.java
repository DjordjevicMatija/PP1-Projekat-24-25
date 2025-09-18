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
		} else {
			if (Obj.Type == typeNode.getKind()) {
				currentType = typeNode.getType();
			} else {
                report_error("Ime " + type.getTypeName() + " ne predstavlja tip", type);
				currentType = SymbolTable.noType;
			}
		}
    }

    // CONST
    @Override
    public void visit(NumberConst constant){
        if (currentType == SymbolTable.intType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
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
                SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
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
                SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
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

        if (!returnFound && method.getMethodName().obj.getType() != SymbolTable.noType) {
            report_error("Metoda " + method.getMethodName().obj.getName() + " nema return iskaz", method);
        }

        SymbolTable.chainLocalSymbols(currentMethod);
        SymbolTable.closeScope();

        currentMethod = null;
    }
}
