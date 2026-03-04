grammar Javamm;

@header {
    package pt.up.fe.comp2026;
}

CLASS : 'class' ;
INT : 'int' ;
STATIC : 'static' ;
RETURN : 'return' ;
PACKAGE : 'package';
IMPORT: 'import';
PUBLIC : 'public' ;
BOOLEAN:'boolean';
IF:'if';
ELSE:'else';
TRUE: 'true';
FALSE:'false';
INTEGER : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]*;
WS : [ \t\n\r\f]+ -> skip ;

SINGLE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

program
    : packageDecl  importDecl* classNode=classDecl EOF
    ;

packageDecl
    : PACKAGE path += ID ('.' path += ID)* ';'
    ;
importDecl
    : IMPORT path+= ID('.' ID)* ';'
    ;

classDecl
    : CLASS name=ID '{' methodDecl '}'
    ;

varDecl
    : typeNode = type name=ID ';'
    ;

param
    : typeNode = type name = ID
    ;

type
    : name = INT
    | name= BOOLEAN
    ;

methodDecl locals[boolean isStatic=false]
    : visibility=PUBLIC (STATIC {$isStatic=true;})?
      returnType = type name=ID
      '(' params = param ')'
      '{' varDecl* stmt* '}'
    ;

stmt
    : var=ID '=' expr ';' #AssignStmt
    | '{' stmt* '}' #Block
    | IF expr stmt #IfStmt
    | ELSE  stmt #ElseStmt
    | RETURN (expr)? ';' #ReturnStmt
    ;

expr
    : expr op='*' expr #BinaryExpr
    | '(' expr ')' #Parenteses
    | expr op='+' expr #BinaryExpr
    | value=INTEGER #IntegerLiteral
    | expr op='&&' expr #LogicalExpr
    | expr op='<' expr #LogicalExpr
    | op='!' expr #LogicalExpr
    | bool= (TRUE|FALSE) # BoolValues
    | name=ID #VarRefExpr
    ;


