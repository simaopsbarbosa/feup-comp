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
    | IF '(' expr ')' stmt (ELSE stmt)? #ElseifBlockStmt
    | WHILE '(' expr ')' stmt #WhileStmt
    | FOR '(' (assignment(',' assignment)*)?';' (expr(','expr)*)? ';'(assignment(','assignment)*)?')' body=stmt #ForStmt
    | expr ';' #Expression
    | var=ID '=' expr ';' #AssignStmt
    | var=ID ('[' expr ']')+ '=' expr ';' #ArrayAssignment
    | RETURN (expr)? ';' #ReturnStmt
    ;

assignment
    : ID '=' expr
    ;

arrayInitializer
    : '{' (arrayElement (',' arrayElement)*)? '}'
    ;

arrayElement
    : expr
    | arrayInitializer
    ;


expr
    :'(' expr ')'                                               #Parenteses      //16
    | expr op=('++'|'--')                                       #RightUnaryExpr  //15
    | op=('!'|'+'|'-'|'++'|'--') expr                           #LeftUnaryExpr   //14
    | expr op=('*'|'/'|'%') expr                                #BinaryExpr      //12
    | expr op=('+'|'-') expr                                    #BinaryExpr      //11
    | expr op=('<'|'>'|'<='|'>=') expr                          #LogicalExpr     //9
    | expr op=('=='|'!=') expr                                  #EqualityExpr    //8
    | expr op='&&' expr                                         #LogicalExpr     // 4
    | expr op='||' expr                                         #LogicalExpr     // 3
    | expr '.' LENGTH                                           #Length
    | ID '(' ( expr ( ',' expr )* )? ')'                        #ImplicitClassAttribute
    | expr '.' ID ('(' ( expr ( ',' expr )* )? ')')?            #ExplicitClassAttribute
    | expr ('[' expr ']')+                                      #ArrayAccess
    | NEW baseType ('[' sizes+=expr ']')+                       #NewArray
    | NEW baseType (dims+=ARRAY)+ arrayInitializer              #ArrayInit
    | NEW ID '(' ( expr ( ',' expr )* )? ')'                    #ClassInitialization
    | value=INTEGER                                             #IntegerLiteral
    | bool= (TRUE|FALSE)                                        #BoolLiteral
    | name=ID                                                   #VarRefExpr
    | THIS                                                      #This
    ;

