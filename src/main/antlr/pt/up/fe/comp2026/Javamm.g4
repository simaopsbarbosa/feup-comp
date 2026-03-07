grammar Javamm;

@header {
    package pt.up.fe.comp2026;
}

CLASS : 'class' ;
INT : 'int' ;
VOID:'void';
ARRAY:'['']';
THIS:'this';
NEW: 'new';
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
FOR:'for';
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
    : IMPORT path+= ID('.' ID)+ ';'
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
    : typeNode=type name=ID
    ;


type
    : baseType (dims+=ARRAY)*
    ;

baseType
    : name=INT
    | name=BOOLEAN
    | name=VOID
    | name=ID
    ;

methodDecl locals[boolean isStatic=false]
    : visibility=(PUBLIC|PRIVATE|PROTECTED)? (STATIC {$isStatic=true;})?
      returnType=type name=ID
      '(' (param(','param)*)? ')'
      '{' varDecl* stmt*'}'
    ;

stmt
    : '{' stmt* '}' #Block
    | IF expr stmt (ELSE IF expr stmt)* (ELSE  stmt)? #ElseifBlockStmt
    | WHILE '('expr')' stmt #WhileStmt
    | expr ';'#Expression
    | var=ID '=' expr ';' #AssignStmt
    | var=ID ('['expr']')+'=' expr';'#ArrayAssignment
    | RETURN (expr)? ';' #ReturnStmt
    ;

arrayInitializer
    : '{' (arrayElement (',' arrayElement)*)? '}'
    ;

arrayElement
    : expr
    | arrayInitializer
    ;


expr
    : expr op=('*'|'/') expr #BinaryExpr
    | expr op=('+'|'-') expr #BinaryExpr
    | expr op=('<'|'>') expr #LogicalExpr
    | op='!' expr #LogicalExpr
    | expr op=('&&'|'||') expr #LogicalExpr
    | expr '.' LENGTH #Length
    | expr '.' ID '(' ( expr ( ',' expr )* )? ')' #FunctionCall
    | expr ('[' expr ']')+ #ArrayAccess
    | NEW baseType ('[' sizes+=expr ']')+ #NewArray
    | NEW baseType (dims+=ARRAY)+ arrayInitializer  #ArrayInit
    | NEW ID '(' ')' #ClassInitialization
    |'(' expr ')' #Parenteses
    | value=INTEGER #IntegerLiteral
    | bool= (TRUE|FALSE) # BoolLiteral
    | name=ID #VarRefExpr
    | THIS #This
    ;


