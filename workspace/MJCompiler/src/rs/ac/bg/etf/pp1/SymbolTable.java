package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.visitors.SymbolTableVisitor;

public class SymbolTable extends Tab {

    public static final Struct boolType = new Struct(Struct.Bool);

    public static void init() {
        Tab.init();
        currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
    }

    // TODO: Uncomment when implementing SymbolTableVisitorExt
    // public static void dump(SymbolTableVisitor stv) {
	// 	System.out.println("=====================SYMBOL TABLE DUMP=========================");
	// 	if (stv == null)
	// 		stv = new SymbolTableVisitorExt();
	// 	for (Scope s = currentScope; s != null; s = s.getOuter()) {
	// 		s.accept(stv);
	// 	}
	// 	System.out.println(stv.getOutput());
	// }

	/** Stampa sadrzaj tabele simbola. */
	public static void dump() {
		dump(null);
	}
}