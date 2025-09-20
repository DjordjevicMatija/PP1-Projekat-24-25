package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class SymbolTableVisitorExt extends DumpSymbolTableVisitor {

	@Override
	public void visitStructNode(Struct structToVisit) {
		switch (structToVisit.getKind()) {
		case Struct.None:
			output.append("notype");
			break;
		case Struct.Int:
			output.append("int");
			break;
		case Struct.Char:
			output.append("char");
			break;
        case Struct.Bool:
            output.append("bool");
            break;
        case Struct.Enum:
            output.append("set");
            break;
		case Struct.Array:

			switch (structToVisit.getElemType().getKind()) {
			case Struct.None:
				output.append("Arr of notype");
				break;
			case Struct.Int:
				output.append("Arr of int");
				break;
			case Struct.Char:
				output.append("Arr of char");
				break;
            case Struct.Bool:
                output.append("Arr of bool");
            break;
			case Struct.Class:
				output.append("Class");
				break;
			}

			break;
		case Struct.Class:
			output.append("Class [");
			for (Obj obj : structToVisit.getMembers()) {
				obj.accept(this);
			}
			output.append("]");
			break;
		}

	}
}
