package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.visitors.SymbolTableVisitor;

public class SymbolTable extends Tab {

    public static final Struct boolType = new Struct(Struct.Bool);
    public static final Struct setType = new Struct(Struct.Enum);

    public static Obj addObj, addAllObj;

    public static void init() {
        Tab.init();
        currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
        currentScope.addToLocals(new Obj(Obj.Type, "set", setType));

        currentScope.addToLocals(addObj = new Obj(Obj.Meth, "add", noType, 0, 2));
        {
            openScope();
            currentScope.addToLocals(new Obj(Obj.Var, "a", setType, 0, 1));
            currentScope.addToLocals(new Obj(Obj.Var, "b", intType, 0, 1));
            addObj.setLocals(currentScope.getLocals());
            closeScope();
        }

        currentScope.addToLocals(addAllObj = new Obj(Obj.Meth, "addAll", noType, 0, 2));
        {
            openScope();
            currentScope.addToLocals(new Obj(Obj.Var, "a", setType, 0, 1));
            currentScope.addToLocals(new Obj(Obj.Var, "b", new Struct(Struct.Array, intType), 0, 1));
            addAllObj.setLocals(currentScope.getLocals());
            closeScope();
        }
    }

    public static void dump(SymbolTableVisitor stv) {
		System.out.println("=====================SYMBOL TABLE DUMP=========================");
		if (stv == null)
			stv = new SymbolTableVisitorExt();
		for (Scope s = currentScope; s != null; s = s.getOuter()) {
			s.accept(stv);
		}
		System.out.println(stv.getOutput());
	}

	/** Stampa sadrzaj tabele simbola. */
	public static void dump() {
		dump(null);
	}
}