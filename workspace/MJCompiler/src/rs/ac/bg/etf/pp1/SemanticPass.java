package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticPass extends VisitorAdaptor {
    Logger log = Logger.getLogger(RuleVisitor.class);

    private boolean errorDetected = false;
    private Struct currentType = null;

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

    // PROGRAM
    public void visit(ProgName progName) {
        progName.obj = SymbolTable.insert(Obj.Prog, progName.getProgName(), SymbolTable.noType);
        SymbolTable.openScope();
    }

    public void visit(Program program) {
        SymbolTable.chainLocalSymbols(program.getProgName().obj);
        SymbolTable.closeScope();
    }

    // TYPE
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
    public void visit(NumberConst constant){
        if (currentType == SymbolTable.intType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
            } else {
                report_error("Simbol " + constant.getConstName() + " je vec deklarisan", constant);
            }
        } else {
            report_error("Tip konstante " + constant.getConstName() + " nije ekvivalentan tipu proslednjenog podatka", constant);
        }
    }

    public void visit(CharConst constant){
        if (currentType == SymbolTable.charType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
            } else {
                report_error("Simbol " + constant.getConstName() + " je vec deklarisan", constant);
            }
        } else {
            report_error("Tip konstante " + constant.getConstName() + " nije ekvivalentan tipu proslednjenog podatka", constant);
        }
    }

    public void visit(BoolConst constant){
        if (currentType == SymbolTable.boolType) {
            Obj obj = SymbolTable.currentScope.findSymbol(constant.getConstName());
            if (obj == null) {
                SymbolTable.insert(Obj.Con, constant.getConstName(), currentType);
            } else {
                report_error("Simbol " + constant.getConstName() + " je vec deklarisan", constant);
            }
        } else {
            report_error("Tip konstante " + constant.getConstName() + " nije ekvivalentan tipu proslednjenog podatka", constant);
        }
    }
}
