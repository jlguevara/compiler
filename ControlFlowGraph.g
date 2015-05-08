tree grammar ControlFlowGraph;

options
{
   tokenVocab=Mini;
   ASTLabelType=CommonTree;
}

/*
   Tree Parser -- Type checks the AST 
*/
@header
{
    import java.util.HashMap;
    import java.util.LinkedList;
    import java.util.Arrays;
}

@members
{
    private int labelIndex = 1;     /* next label number to use */
    private int nextRegister = 0;   /* next register number to use */
    private boolean storeGlobal;    /* true to store a global variable */
    private String branchOp;        /* what branch operation to use */
    private String chainOp; /* what operation to use during chained && or || */ 
    private boolean chainedExpression; /* are we in a chained expression? */
    private String lvalueMember; 

    private int maxArgCount; /* max number of arguments for invocation call */

    private HashMap<String, String> registerMap; /* id -> register */

    private List<BasicBlock> funBlocks = new LinkedList<BasicBlock>();

    BasicBlock currentExitBlock; 

    private HashMap<String, Type> globalTable = new HashMap<String, Type>();   
    private HashMap<String, StructType> structTable = 
        new HashMap<String, StructType>();
    //private FunType currentFun;

    // Generate the next label to use
    private String getNextLabel() {
        return "L" + labelIndex++;
    }

    // update maxArgument
    private void updateArgCount(int count) {
        if (count > maxArgCount)
            maxArgCount = count;
    }

    // return the list of graphs
    public List<BasicBlock> getFunBlocks() {
        return funBlocks;
    }
   
   public HashMap<String, Type> getGlobals() {
      return globalTable;
   }
    
    private BasicBlock createExitBlock() {
        BasicBlock exitBlock = new BasicBlock(getNextLabel());
        exitBlock.addInstruction(new Instruction("ret"));
        return exitBlock;
    }
}


translate
   :  ^(PROGRAM t=types d=declarations[globalTable] f=functions[globalTable])
   ;

types
   :  ^(TYPES (t=type_decl)*)
   |  
   ;

type_decl
    @init{ StructType st = new StructType(); }
   :  ^(ast=STRUCT id=ID { structTable.put($id.text, st); }   
        n=nested_decl[st.members])
   ;

nested_decl[HashMap<String, Type> scope]
   :  (f=field_decl[scope])+
   ;

field_decl[HashMap<String, Type> scope]
   :  ^(DECL ^(TYPE t=type) id=ID)
      {
        scope.put($id.text, $t.t);
      }
   ;

type
   returns [Type t = null]
   :  INT { t = IntType.getInstance(); }
   |  BOOL { t = BoolType.getInstance(); }
   |  ^(STRUCT id=ID)
        {
        $t = structTable.get($id.text);
        }
   ;


declarations[HashMap<String, Type> scope]
   :  ^(DECLS (d=decl_list[scope])*)
   |  
   ;

decl_list[HashMap<String, Type> scope]
   :  ^(DECLLIST ^(TYPE t=type)
         (id=ID
            {
                scope.put($id.text, $t.t);
                /* only fill register map if we are inside a function */
                if (registerMap != null) {
                   registerMap.put($id.text, "r" + nextRegister);
                    nextRegister++; 
                }
            }
         )+
      )
   ;

functions[HashMap<String, Type> globalScope]
   :  ^(FUNCS (f=function[globalScope])*)
   |  
   ;

function[HashMap<String, Type> globalScope]
    @init{ 
        BasicBlock entryBlock;
        HashMap<String, Type> localScope = new HashMap<String, Type>();
        registerMap = new HashMap<String, String>();
        nextRegister = 0;
        FunType fun = new FunType();
        maxArgCount = 0;
    }
   :  ^(ast=FUN id=ID 
        {   entryBlock = new BasicBlock($id.text); 
            entryBlock.setEntryBlock(true);
            funBlocks.add(entryBlock);

            currentExitBlock = createExitBlock(); 

            globalScope.put($id.text, fun); 
        } 
        p=parameters[localScope, entryBlock] r=return_type 
        d=declarations[localScope] 
        s=statement_list[localScope, entryBlock]) 
    {
        if ($s.block != currentExitBlock) {
            $s.block.addOutgoing(currentExitBlock);
            currentExitBlock.addIncoming($s.block);
        }
        entryBlock.setMaxArgCount(maxArgCount);
    }
   ;

parameters[HashMap<String, Type> localScope, BasicBlock currentBlock]
   :  ^(PARAMS (p=param_decl[localScope, currentBlock])*)
   ;

param_decl[HashMap<String, Type> localScope, BasicBlock currentBlock]
   :  ^(DECL ^(TYPE t=type) id=ID)
      {
        localScope.put($id.text, $t.t);

        String register = "r" + nextRegister;
        registerMap.put($id.text, register);
        String argIndex = "" + nextRegister;

        Instruction op = 
            new Instruction("loadinargument", $id.text, argIndex, register);

        currentBlock.addInstruction(op);
        nextRegister++;
      }
   ;

return_type
   :  ^(RETTYPE rtype) 
   ;

rtype
   :  t=type 
   |  VOID 
   ;

statement[HashMap<String, Type> scope, BasicBlock currentBlock]
    returns [BasicBlock nextBlock = currentBlock]
   :  (s=block[scope, currentBlock] { $nextBlock = $s.block; }
      |  s=assignment[scope, currentBlock]
      |  s=print[scope, currentBlock]
      |  s=read[scope, currentBlock]
      |  s=conditional[scope, currentBlock] { $nextBlock = $s.block; }
      |  s=loop[scope, currentBlock] { $nextBlock = $s.block; }
      |  s=delete[scope, currentBlock]
      |  s=return_stmt[scope, currentBlock] { $nextBlock = $s.block; }
      |  invocation_stmt[scope, currentBlock]
      )
   ;

block[HashMap<String, Type> scope, BasicBlock currentBlock]
    returns [BasicBlock block]
   :  ^(BLOCK s=statement_list[scope, currentBlock])
    {
        $block = $s.block;
    }
   ;

statement_list[HashMap<String, Type> scope, BasicBlock currentBlock]
    returns [BasicBlock block = currentBlock]
   :  ^(STMTS (s=statement[scope, block] 
        { $block = $s.nextBlock; })*)
   ;

assignment[HashMap<String, Type> scope, BasicBlock currentBlock]
   :  ^(ast=ASSIGN e=expression[scope, currentBlock]
        l=lvalue[scope, currentBlock])
    {
        Instruction op;

        if (storeGlobal) {

            op = new Instruction("storeglobal", $e.register, $l.text);
            storeGlobal = false;
        }
        else {
            if (lvalueMember != null) {
                op = new Instruction("storeai", $e.register, $l.register,
                        lvalueMember);
                lvalueMember = null;
            }
            else {
                op = new Instruction("mov", $e.register, $l.register);
            }
        }
        currentBlock.addInstruction(op);
    }
   ;

read[HashMap<String, Type> scope, BasicBlock currentBlock]
   :  ^(ast=READ l=lvalue[scope, currentBlock])
        {
            Instruction op = new Instruction("read", $l.register);
            currentBlock.addInstruction(op);
        }
   ;

lvalue[HashMap<String, Type> localScope, BasicBlock currentBlock]
    returns [String register]
   :  id=ID
    {
            Type varType = localScope.get($id.text);
            if (varType != null) {
                $register = registerMap.get($id.text);
            }
            else {
                storeGlobal = true;
            }
    }
   |  ^(ast=DOT l=lvalue_load[localScope, currentBlock] id=ID)
    {
        $register = $l.register;
        lvalueMember = $id.text;
    }
   ;

lvalue_load[HashMap<String, Type> localScope, BasicBlock currentBlock]
    returns [String register]
   :  id=ID
    {
            Type varType = localScope.get($id.text);
            if (varType != null) {
                $register = registerMap.get($id.text);
            }
            else {
                $register = "r" + nextRegister;
                nextRegister++;

                Instruction ins = new Instruction("loadglobal", $id.text, 
                                    $register);
                currentBlock.addInstruction(ins);
            }
    }
   |  ^(ast=DOT l=lvalue_load[localScope, currentBlock] id=ID)
    {
        Instruction inst;
        $register = "r" + nextRegister;
        nextRegister++;
        inst = new Instruction("loadai", $l.register, $id.text, $register);

        currentBlock.addInstruction(inst);
    }
   ;

print[HashMap<String, Type> scope, BasicBlock currentBlock]
    @init { Instruction op = null; }
   :  ^(ast=PRINT e=expression[scope, currentBlock] 
        (ENDL { op = new Instruction("println"); })?)
        {
            if (op == null) 
                op = new Instruction("print");
            op.addOperand($e.register);
            currentBlock.addInstruction(op);
        }
   ;

conditional[HashMap<String, Type> scope, BasicBlock currentBlock]
    returns [BasicBlock block]
    @init   {   BasicBlock trueBlock = new BasicBlock(getNextLabel());
                BasicBlock falseBlock = new BasicBlock(getNextLabel());
                BasicBlock nextBlock = new BasicBlock(getNextLabel());
                String testOp = null;
            }
   :  ^(ast=IF g=expression[scope, currentBlock] 
        {   
            chainedExpression = false; /* end chain for this block */
            testOp = branchOp;          /* save the branch op */
        }
        t=block[scope, trueBlock] (e=block[scope, falseBlock])?)
    {
        Instruction op = new Instruction(testOp, $g.register);
System.out.println("CURRENT: " + currentBlock);

        // handle true edges
        currentBlock.addOutgoing(trueBlock);
        trueBlock.addIncoming(currentBlock);
        $t.block.addOutgoing(nextBlock);
        nextBlock.addIncoming($t.block);
        // add jump instruction to skip over else clause
        Instruction jumpOp = new Instruction("jumpi", nextBlock.getLabel());
        $t.block.addInstruction(jumpOp);
        
        // handle false edges
        if ($e.block != null) {
            op.addOperand(falseBlock.getLabel());

            currentBlock.addOutgoing(falseBlock);
            falseBlock.addIncoming(currentBlock);
            $e.block.addOutgoing(nextBlock);
            nextBlock.addIncoming($e.block);
        }
        else {
            op.addOperand(nextBlock.getLabel());
        }

        // label for true block
        op.addOperand(trueBlock.getLabel());
        currentBlock.addInstruction(op);
        $block = nextBlock;
    }
   ;

loop[HashMap<String, Type> scope, BasicBlock currentBlock]
    returns [BasicBlock block]
    @init   {   BasicBlock eBlock = new BasicBlock(getNextLabel());
                BasicBlock bodyBlock = new BasicBlock(getNextLabel());
                BasicBlock nextBlock = new BasicBlock(getNextLabel());
                String testOp;
            }
   :  ^(ast=WHILE e=expression[scope, eBlock] 
        {   
            chainedExpression = false; /* done with the chain of && and || */
            testOp = branchOp;          /* save the branch op */
        }  
        b=block[scope, bodyBlock])
    {
        // edge to expression
        currentBlock.addOutgoing(eBlock);
        eBlock.addIncoming(currentBlock);

         /*
        Instruction op = new Instruction("jumpi", eBlock.getLabel());
        currentBlock.addInstruction(op);
         */

        // edge from expression to body block
        eBlock.addOutgoing(bodyBlock);
        bodyBlock.addIncoming(eBlock);

        Instruction op = new Instruction(testOp, $e.register, 
            nextBlock.getLabel(), bodyBlock.getLabel());
        eBlock.addInstruction(op);
        
        // edge from body block to expression
        $b.block.addOutgoing(eBlock);
        eBlock.addIncoming($b.block);

        op = new Instruction("jumpi", eBlock.getLabel());
        $b.block.addInstruction(op);

        // edge from expression to next block (when expression is false)
        eBlock.addOutgoing(nextBlock);
        nextBlock.addIncoming(eBlock);

        $block = nextBlock;
    }
   ;

delete[HashMap<String, Type> scope, BasicBlock currentBlock]
   :  ^(ast=DELETE e=expression[scope, currentBlock])
    {
        Instruction op = new Instruction("del", $e.register);
        currentBlock.addInstruction(op);
    }
   ;

return_stmt[HashMap<String, Type> scope, BasicBlock currentBlock]
    returns [BasicBlock block]
    @init { Instruction op = null; }
   :  ^(ast=RETURN  (exp=expression[scope, currentBlock]
                { op = new Instruction("storeret", $exp.register);
                  currentBlock.addInstruction(op);
                })?)
    {   
        Instruction jumpOut = new Instruction("jumpi", 
            currentExitBlock.getLabel());
        currentBlock.addInstruction(jumpOut);

        currentBlock.addOutgoing(currentExitBlock);
        currentExitBlock.addIncoming(currentBlock);
        $block = currentExitBlock;
    }
   ;

invocation_stmt[HashMap<String, Type> localScope, BasicBlock currentBlock]
    @init { int argIndex = 0; 
            Instruction op;
            List<Instruction> lst = new LinkedList<Instruction>();
        }
   :  ^(INVOKE id=ID ^(ARGS (e=expression[localScope, currentBlock]
        {
            op = 
                new Instruction("storeoutargument", $e.register, "" + argIndex);

            argIndex++;
            lst.add(op);
        })*))
        {
            for (Instruction inst : lst) {
                currentBlock.addInstruction(inst);
            }

            op = new Instruction("call", $id.text, "" + argIndex);
            currentBlock.addInstruction(op);
            updateArgCount(argIndex);
        }
   ;


expression[HashMap<String, Type> localScope, BasicBlock currentBlock]
    returns [String register]
    @init {
    }
   :  ^((ast=AND | ast=OR) { chainedExpression = true; }
         lft=expression[localScope, currentBlock] 
        rht=expression[localScope, currentBlock])
      {
        String opcode = "", operation = $ast.text;
        $register = "r" + nextRegister;
        nextRegister++;

        if (operation.equals("&&")) {
            opcode = "and";
            branchOp = "brz";
        }
        else if (operation.equals("||")) {
            opcode = "or";
            branchOp = "brnz";
        }

        Instruction instruction =
            new Instruction(opcode, $lft.register, $rht.register, $register);
        currentBlock.addInstruction(instruction);
    }
   |  ^((ast=EQ | ast=LT | ast=GT | ast=NE | ast=LE | ast=GE)
        lft=expression[localScope, currentBlock] 
        rht=expression[localScope, currentBlock]) 
        {
            String operation = $ast.text, chainOp = null;

            if (operation.equals("==")) {
System.out.println("EQUAL");
                branchOp = "cbrne";
                chainOp = "moveqi";
            }
            else if (operation.equals("<")) {
                branchOp = "cbrge";
                chainOp = "movlt";
            }
            else if (operation.equals(">")) {
                branchOp = "cbrle";
                chainOp = "movgt";
            }
            else if (operation.equals("!=")) {
                branchOp = "cbreq";
                chainOp = "movne";
            }
            else if (operation.equals("<=")) {
System.out.println("LESS THAN OR EQUAL");
                branchOp = "cbrgt";
                chainOp = "movle";
            }
            else if (operation.equals(">=")) {
                branchOp = "cbrlt";
                chainOp = "movge";
            }
            
            Instruction instruction; 
            String controlRegister = null;
            if (chainedExpression) {
                controlRegister = "r" + nextRegister;
                nextRegister++;
                instruction = new Instruction("loadi", "0", controlRegister);
                currentBlock.addInstruction(instruction);
            }

            $register = "ccr";
            instruction = new Instruction("comp", $lft.register, 
                $rht.register, $register);

            currentBlock.addInstruction(instruction);

            if (chainedExpression) {
                instruction = 
                    new Instruction(chainOp, "ccr", "1", controlRegister);

                currentBlock.addInstruction(instruction);
                $register = controlRegister;
            }
        }
   |  ^((ast=PLUS | ast=MINUS | ast=TIMES | ast=DIVIDE)
        lft=expression[localScope, currentBlock] 
        rht=expression[localScope, currentBlock]) 
        {
            $register = "r" + nextRegister;
            nextRegister++;
            String operation = $ast.text, opcode = "n/a";

            if (operation.equals("+")) {
                opcode = "add";
            }
            else if (operation.equals("-")) {
                opcode = "sub";
            }
            else if (operation.equals("*")) {
                opcode = "mult";
            }
            else if (operation.equals("/")) {
                opcode = "div";
            }

            Instruction instruction = new Instruction(opcode, $lft.register, 
                $rht.register, $register);

            currentBlock.addInstruction(instruction);

        }
   |  ^(ast=NOT exp=expression[localScope, currentBlock])
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("xori", $exp.register, "1", 
                                $register);

            currentBlock.addInstruction(op);
        }
   |  ^(ast=NEG exp=expression[localScope, currentBlock])
        {
            $register = "r" + nextRegister;
            nextRegister++;

            // load -1
            Instruction op = new Instruction("loadi", "-1", $register);
            currentBlock.addInstruction(op);
            
            // multiply expresion by -1
            op = new Instruction("mult", $register, $exp.register);
             
            $register = "r" + nextRegister;
            nextRegister++;
            op.addOperand($register);
            
            currentBlock.addInstruction(op);
        }
   |  ^(ast=DOT    left=expression[localScope, currentBlock]  id=ID)
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("loadai", $left.register, 
                                $id.text, $register);
            
            currentBlock.addInstruction(op);
        }
   |  e=invocation_exp[localScope, currentBlock] 
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("loadret", $register);
            currentBlock.addInstruction(op);
        }
   |  id=ID
        {
            $register = "r" + nextRegister;
            nextRegister++;
            Instruction op;

            Type varType = localScope.get($id.text);
            if (varType != null) {
                String varRegister = registerMap.get($id.text);
                op = new Instruction("mov", varRegister, $register);
            }
            else {
                op = new Instruction("loadglobal", $id.text, $register);
            }
                
            currentBlock.addInstruction(op);
        }
   |  i=INTEGER
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("loadi", $i.text, $register);
            currentBlock.addInstruction(op);
        }
   |  ast=TRUE
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("loadi", "1", $register);
            currentBlock.addInstruction(op);
        }
   |  ast=FALSE
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("loadi", "0", $register);
            currentBlock.addInstruction(op);
        }
   |  ^(ast=NEW id=ID)
        {
            $register = "r" + nextRegister;
            nextRegister++;

            List<String> members = structTable.get($id.text).memberList();
            Instruction op = new Instruction("new", $id.text, 
                            Arrays.toString(members.toArray()), $register);
            currentBlock.addInstruction(op);
        }
   |  ast=NULL
        {
            $register = "r" + nextRegister;
            nextRegister++;

            Instruction op = new Instruction("loadi", "0", $register);
            currentBlock.addInstruction(op);
        }
   ;

invocation_exp[HashMap<String, Type> localScope, BasicBlock currentBlock]
    @init { int argIndex = 0; 
            Instruction op;
            List<Instruction> lst = new LinkedList<Instruction>();
        }
   :  ^(INVOKE id=ID ^(ARGS (e=expression[localScope, currentBlock]
        {
            op = new Instruction("storeoutargument", $e.register, 
                "" + argIndex);
            lst.add(op);
            argIndex++;
        })*))
        {
            for (Instruction inst  : lst) {
                currentBlock.addInstruction(inst);
            }
            op = new Instruction("call", $id.text, "" + argIndex);
            currentBlock.addInstruction(op);
            updateArgCount(argIndex);
        }
   ;
