package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

public class RuleVisitor extends VisitorAdaptor {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

    int printCallCount = 0;

    Logger log = Logger.getLogger(RuleVisitor.class);

    // public void visit(PrintExpr PrintExpr) {
    //     printCallCount++;
    //     log.info("Prepoznata naredba print!");
    // }
}
