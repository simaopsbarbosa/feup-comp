grammar Javamm;

@header {
    package pt.up.fe.comp2026;
}

CLASS : 'class' ;
INT : 'int' ;
VOID:'void';
ARRAY:'['']';
STATIC : 'static' ;
RETURN : 'return' ;
PACKAGE : 'package';
IMPORT: 'import';
PUBLIC : 'public' ;
PRIVATE : 'private' ;
PROTECTED:'protected';
BOOLEAN:'boolean';
IF:'if';
ELSE:'else';
TRUE: 'true';
FALSE:'false';
WHILE:'while';
LENGTH:'length';
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
    :  visibility=(PUBLIC|PRIVATE|PROTECTED)? CLASS name=ID ('extends' ID)? '{' (fieldDecl|methodDecl)* '}'
    ;

fieldDecl
    : visibility=(PUBLIC|PRIVATE|PROTECTED)? varDecl
    ;


varDecl
    :  typeNode =type name=ID ('='expr)? ';'
    ;

param
    : typeNode = type name = ID
    ;

array
    : type (ARRAY);

type
    : name = INT
    | name= BOOLEAN
    | name= VOID
    | name=ID
    ;

methodDecl locals[boolean isStatic=false]
    : visibility=(PUBLIC|PRIVATE|PROTECTED)? (STATIC {$isStatic=true;})?
      returnType = type name=ID
      '(' (param(','param)*)? ')'
      '{' varDecl* stmt*'}'
    ;

stmt
    : var=ID '=' expr ';' #AssignStmt
    | '{' stmt* '}' #Block
    | expr ';'#Expression
    | IF expr stmt (ELSE IF expr stmt)* (ELSE  stmt)? #ElseifBlockStmt
    | WHILE '('expr')' stmt #WhileStmt
    | var=ID '['expr']''=' expr';'# ArrayAssignment
    | RETURN (expr)? ';' #ReturnStmt
    ;

expr
    : '(' expr ')' #Parenteses
    | op='!' expr #LogicalExpr
    | expr op=('*'|'/') expr #BinaryExpr
    | expr op=('+'|'-') expr #BinaryExpr
    | expr op=('<'|'>') expr #LogicalExpr
    | value=INTEGER #IntegerLiteral
    | expr '.' LENGTH #Length
    | expr '.' ID '(' ( expr ( ',' expr )* )? ')' #FunctionCall
    | expr op='&&' expr #LogicalExpr
    | bool= (TRUE|FALSE) # BoolLiteral
    | name=ID #VarRefExpr
    ;


