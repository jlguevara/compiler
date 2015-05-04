FILES=Mini.java X86.java 
CLASSPATH=-cp ./antlr-3.5.2-complete.jar:./javax.json-1.0.4.jar:.


Mini.class : antlr.generated ${FILES}
	javac ${CLASSPATH} *.java

antlr.generated: antlr.generated.mini antlr.generated.json antlr.generated.graph
	touch antlr.generated

antlr.generated.mini : Mini.g
	java ${CLASSPATH} org.antlr.Tool Mini.g
	touch antlr.generated.mini

antlr.generated.json : ToJSON.g
	java ${CLASSPATH} org.antlr.Tool ToJSON.g
	touch antlr.generated.json

antlr.generated.graph: antlr.generated.types ControlFlowGraph.g
	java ${CLASSPATH} org.antlr.Tool ControlFlowGraph.g
	touch antlr.generated.graph

antlr.generated.types: TypeCheck.g
	java ${CLASSPATH} org.antlr.Tool TypeCheck.g
	touch antlr.generated.types

test: ${FILES}
	java ${CLASSPATH} Mini tests/test.mini 

clean:
	\rm *generated* MiniParser.java MiniLexer.java ToJSON.java TypeCheck.java Mini.tokens ToJSON.tokens ControlFlowGraph.java *.class
