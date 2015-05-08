tree grammar TypeCheck;

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
}

@members
{
    private HashMap<String, Type> globalTable = new HashMap<String, Type>();   
    private HashMap<String, Type> structTable = new HashMap<String, Type>();
    private FunType currentFun;

    // check that a variable exists
    private Type checkVar(CommonTree id, HashMap<String, Type> localScope) {
        Type varType = localScope.get(id.getText());
        if (varType == null) {
            varType = globalTable.get(id.getText());
            if (varType == null) {
                System.out.println("Line " + id.getLine() + ": identifier " +
                    id.getText() + " doesn't exist");
                System.exit(1);
            }
        }
        return varType;
    }
    
    // check if a structure contains value
    private Type checkMember(CommonTree id, StructType struct) {
        Type member = struct.members.get(id.getText());
        if (member == null) {
            System.out.println("Line " + id.getLine() + ": member " +
                id.getText() + " doesn't exist");
            System.exit(1);
        }
        return member;
    }

    // check for valid invocation
    public void checkInvocation(CommonTree id, List<Type> args) {
        Type fun = globalTable.get(id.getText());
        if (fun != null) {
            if (!(fun instanceof FunType)) {
                System.out.println("Line " + id.getLine() + ": " + 
                    id.getText() + " is not a function");
                System.exit(1);
            }  
            checkArgs(id, (FunType)fun, args);
        }
        else {
            System.out.println("Line " + id.getLine() + ": " + 
                id.getText() + " does not exists");
            System.exit(1);
        }
      }

    // Check that function call got the correct types
    private void checkArgs(CommonTree funNode, FunType fun, List<Type> args) {
        Type param, arg;
        String errorMsg = "Line " + funNode.getLine() + ":" +
                " function arguments mismatch";

        if (fun.params.size() != args.size()) {
            System.out.println(errorMsg);
            System.exit(1);
        }
        for (int i = 0; i < fun.params.size(); i++) {
            param = fun.params.get(i);
            if (param.getClass() != args.get(i).getClass()) {

                // check that it's not a struct with a null value
                if (!(param instanceof StructType && 
                    args.get(i) instanceof NullType)) {

                    System.out.println(errorMsg);
                    System.exit(1);
                }
            } 
        }
    }
}


translate
   :  ^(PROGRAM t=types d=declarations[globalTable] f=functions[globalTable])
         {
            Type mainType = globalTable.get("main");
            if (mainType == null) {
                System.out.println("missing main function");
                System.exit(1);
            }
            FunType mainFun = (FunType)mainType;
            if (mainFun.params.size() != 0) {
                System.out.println("main must take no arguments"); 
                System.exit(1);
            }
            if (!(mainFun.returnType instanceof IntType)) {
                System.out.println("main must return an int"); 
                System.exit(1);
            }
         }
   ;

types
   :  ^(TYPES (t=type_decl)*)
   |  
   ;

type_decl
    @init{ StructType st = new StructType(); }
   :  ^(ast=STRUCT id=ID {
        if (structTable.get($id.text) != null) {
            System.out.println("Line " + $id.line + 
                    ": duplicate type definition");
            System.exit(1);
        }
        structTable.put($id.text, st); 
        }   
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

        if ($t == null) {
            System.out.println("Line " + $id.line + 
                    ": unknown definition for " + $id.text);
            System.exit(1);
        }
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
                if (scope.get($id.text) != null) {
                    System.out.println("Line " + $id.line +
                        " duplicate definition of " + $id.text);
                    System.exit(1); 
                }
                scope.put($id.text, $t.t);
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
        HashMap<String, Type> localScope = new HashMap<String, Type>();
        FunType fun = new FunType();
    }
   :  ^(ast=FUN id=ID 
        {   globalScope.put($id.text, fun); 
            currentFun = fun;     
        } 
        p=parameters[localScope, fun] r=return_type[fun] 
        d=declarations[localScope] s=statement_list[localScope]) 
    {
        if (currentFun.returnType != null && !currentFun.returns) {
            System.out.println("Line " + $ast.line + ": " + "function " +
                $id.text + " doesn't seem to return");
            System.exit(1);
        }
    }
   ;

parameters[HashMap<String, Type> localScope, FunType fun]
   :  ^(PARAMS (p=param_decl[localScope, fun])*)
   ;

param_decl[HashMap<String, Type> localScope, FunType fun]
   :  ^(DECL ^(TYPE t=type) id=ID)
      {
        localScope.put($id.text, $t.t);
        fun.params.add($t.t);
      }
   ;

return_type[FunType fun]
   :  ^(RETTYPE rtype[fun]) 
   ;

rtype[FunType fun]
   :  t=type {  if ($t.t instanceof NullType) {
                    System.out.println("invalid return");
                    System.exit(1);
                }
                fun.returnType = $t.t; 
            }
   |  VOID 
   ;

statement[HashMap<String, Type> scope]
   :  (s=block[scope]
      |  s=assignment[scope]
      |  s=print[scope]
      |  s=read[scope]
      |  s=conditional[scope]
      |  s=loop[scope]
      |  s=delete[scope]
      |  s=return_stmt[scope]
      |  s=invocation_stmt[scope]
      )
   ;

block[HashMap<String, Type> scope]
   :  ^(BLOCK s=statement_list[scope])
   ;

statement_list[HashMap<String, Type> scope]
   :  ^(STMTS (s=statement[scope])*)
   ;

assignment[HashMap<String, Type> scope]
   :  ^(ast=ASSIGN e=expression[scope] l=lvalue[scope])
      {
        if ($e.type.getClass() != $l.type.getClass()) {
            if (!($l.type instanceof StructType) || 
                !($e.type instanceof NullType)) { 
                    System.out.println("Line " + $ast.line + ": type mismatch");
                    System.exit(1);
            }
        }
      }
   ;

print[HashMap<String, Type> scope]
   :  ^(ast=PRINT e=expression[scope] (ENDL)?)
   ;

read[HashMap<String, Type> scope]
   :  ^(ast=READ l=lvalue[scope])
   ;

conditional[HashMap<String, Type> scope]
    @init { boolean ifReturns = false, elseReturns = false; }
   :  ^(ast=IF g=expression[scope] t=block[scope] 
        { ifReturns = currentFun.returns;
            currentFun.returns = false; } 
         (e=block[scope])?
        {   
                elseReturns = currentFun.returns;
                if (!ifReturns || !elseReturns) {
                    currentFun.returns = false; 
                }
        })
   ;

loop[HashMap<String, Type> scope]
   :  ^(ast=WHILE e=expression[scope] b=block[scope])
   ;

delete[HashMap<String, Type> scope]
   :  ^(ast=DELETE e=expression[scope])
      {
        if (!($e.type instanceof StructType)) {
            System.out.println("Line " + $ast.line + 
                ": delete requires a struct");
        }
      }
   ;

return_stmt[HashMap<String, Type> scope]
   :  ^(ast=RETURN  (exp=expression[scope])?)
      {   
        // check that void functions don't return anything
        if (currentFun.returnType == null) {
            if ($exp.type != null) {
                System.out.println("Line " + $ast.line + ": " +
                    "void function must not return a value");
                System.exit(1);
            }
        } 

        // check that nonvoid functions return something
        else if (currentFun.returnType != null && $exp.type == null) {
                System.out.println("Line " + $ast.line + ": " +
                    "nonvoid function must return a value");
                System.exit(1);
        }

        // check struct return 
        else if (currentFun.returnType instanceof StructType) {
            if (!($exp.type instanceof StructType || 
                    $exp.type instanceof NullType)) { 

                System.out.println("Line " + $ast.line + ": " +
                    "function must return a struct value");
                System.exit(1);
            }
        } 

        else if (currentFun.returnType.getClass() != $exp.type.getClass()) {
            System.out.println("Line " + $ast.line + ": " +
                "function returns the wrong type");
            System.exit(1);
        }

        currentFun.returns = true;
    }
   ;

invocation_stmt[HashMap<String, Type> scope]
   @init {  List<Type> arguments = new LinkedList<Type>(); }
   :  ^(INVOKE id=ID ^(ARGS (e=expression[scope] { arguments.add($e.type); })*))
      {
        checkInvocation(id, arguments); 
      }
   ;

lvalue[HashMap<String, Type> localScope]
   returns [Type type = null]
    @init { Type varType = null; }
   :  id=ID
      { $type = checkVar(id, localScope); }
   |  ^(ast=DOT l=lvalue[localScope] id=ID)
      {
        if (!($l.type instanceof StructType)) {
            System.out.println("lvalue at line " + $ast.line + ": " +
                $id.text + " is not a structure");
            System.exit(1);
        }
        $type = checkMember(id, (StructType)$l.type);
      }
   ;

expression[HashMap<String, Type> localScope]
   returns [Type type = null]
    @init { Type leftType = null;
            Type rightType = null;
    }
   :  ^((ast=EQ | ast=NE) lft=expression[localScope] rht=expression[localScope])
      {
        leftType = $lft.type;
        rightType = $rht.type;
        String op = $ast.text;

        if (leftType instanceof IntType || rightType instanceof IntType) {
            if (!(rightType instanceof IntType) || 
                    !(leftType instanceof IntType)) {
                System.out.println("Line " + $ast.line + ": " + $ast.text +
                    " invalid operands, both must be ints");
                System.exit(1); 
            }
        }
        else if (leftType instanceof BoolType || rightType instanceof 
                    BoolType) {
            if (!(leftType instanceof BoolType) || 
                    !(rightType instanceof BoolType)) {
                System.out.println("Line " + $ast.line + ": " + $ast.text +
                    " invalid operands, both must be bool");
                System.exit(1); 
            }
        }
        else if (leftType instanceof StructType || 
                rightType instanceof StructType ||
                leftType instanceof NullType || 
                rightType instanceof NullType) { 
            if (!(leftType instanceof StructType || 
                    leftType instanceof NullType) || 
                    !(rightType instanceof StructType ||
                    rightType instanceof NullType)) { 

                System.out.println("Line " + $ast.line + 
                    ": invalid operands, must be structs."); 
                System.exit(1); 
            }
        } 
        else {
            System.out.println("Line " + $ast.line + 
                ": unknow type"); 
            System.exit(1); 
        } 
         $type = BoolType.getInstance();
    }
   |  ^((ast=AND | ast=OR)
         lft=expression[localScope] rht=expression[localScope])
        {
            if (!($lft.type instanceof BoolType) || 
                    !($rht.type instanceof BoolType)) {
                System.out.println("Line " + $ast.line + ": " + $ast.text +
                    " expects boolean operands");
                System.exit(1); 
            }
            $type = BoolType.getInstance();
        }
   |  ^((ast=LT | ast=GT | ast=LE | ast=GE | ast=PLUS | ast=MINUS | 
            ast=TIMES | ast=DIVIDE)
         lft=expression[localScope] rht=expression[localScope])
        {
        // check if operands are int
            if (!($lft.type instanceof IntType) || 
                    !($rht.type instanceof IntType)) {
                System.out.println("Line " + $ast.line + ": " + $ast.text +
                    " expects integer  operands");
                System.exit(1); 
            }

            String operator = $ast.text;
            if (operator.equals("+") || operator.equals("-") || 
                  operator.equals("*") || operator.equals("/")) {
               $type = IntType.getInstance();
            }
            else {
               $type = BoolType.getInstance();
            }
        }
   |  ^(ast=NOT exp=expression[localScope])
      {
        if (!($exp.type instanceof BoolType)) {
            System.out.println("Line " + $ast.line + ": expected boolean exp");
            System.exit(1);
        }
        $type = $exp.type;
      }
   |  ^(ast=NEG exp=expression[localScope])
      {
        if (!($exp.type instanceof IntType)) {
            System.out.println("Line " + $ast.line + ": expected int exp");
            System.exit(1);
        }
        $type = $exp.type;
      }
   |  ^(ast=DOT    left=expression[localScope]  id=ID)
      {
        StructType st;

        if (!($left.type instanceof StructType)) {
            System.out.println("Line " + $ast.line + ": " +
                $left.text + " is not a structure");
            System.exit(1);
        }
        $type = checkMember(id, (StructType)($left.type));
      }
   |  e=invocation_exp[localScope] 
        { $type = $e.type; }
   |  id=ID
      {
        $type = checkVar(id, localScope); 
      }
   |  i=INTEGER
      {
        $type = IntType.getInstance();
      }
   |  ast=TRUE
      {
        $type = BoolType.getInstance();
      }
   |  ast=FALSE
      {
        $type = BoolType.getInstance();
        }
   |  ^(ast=NEW id=ID)
      {
        $type = structTable.get($id.text);
        if ($type == null) {
            System.out.println("Line " + $id.line + ": " + $id.text +
                " was not defined");
            System.exit(1);
        }
      }
   |  ast=NULL
      {
        $type = new NullType(); }
   ;

invocation_exp[HashMap<String, Type> localScope]
   returns [Type type = null]
   @init {  List<Type> arguments = new LinkedList<Type>(); }
   :  ^(INVOKE id=ID ^(ARGS (e=expression[localScope] { arguments.add($e.type); })*))
      {
        FunType fun = null;
        checkInvocation(id, arguments); 
        fun = (FunType)globalTable.get($id.text);
        $type = fun.returnType;
      }
   ;
