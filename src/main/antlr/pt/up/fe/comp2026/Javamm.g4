grammar Javamm;

@header {
    package pt.up.fe.comp2026;
}

CLASS : 'class' ;
INT : 'int' ;
VOID:'void';
EQUALS:'=';
LSB:'[';
RSB:']';
LCB:'{';
RCB:'}';
LP:'(';
RP:')';
COMMA:',';
DOT:'.';
SEMICOLON:';';
COLON:':';
EXTENDS:'extends';
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
DO:'do';
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
    : PACKAGE path += ID (DOT path += ID)* SEMICOLON
    ;
importDecl
    : IMPORT path+= ID(DOT path+=ID)+ SEMICOLON
    ;

classDecl
    :  visibility=(PUBLIC|PRIVATE|PROTECTED)? CLASS name=ID (EXTENDS supername=ID)? LCB (fieldDecl|methodDecl)* RCB
    ;

fieldDecl
    : visibility=(PUBLIC|PRIVATE|PROTECTED)? varDecl
    ;


varDecl
    :  typeNode =type name=ID (EQUALS expr)? SEMICOLON
    ;

param
    : typeNode=type name=ID
    ;


type
    : baseType (dims+='[]')*
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
      LP (param(COMMA param)*)? RP
      LCB varDecl* stmt*RCB
    ;

stmt
    : LCB stmt* RCB #Block
    | IF LP expr RP stmt (ELSE stmt)? #ElseifBlockStmt
    | WHILE LP expr RP stmt #WhileStmt
    | FOR LP (assignment(COMMA assignment)*)?SEMICOLON (expr(COMMA expr)*)? SEMICOLON(assignment(COMMA assignment)*)? RP body=stmt #ForStmt
    | DO (expr|stmt) WHILE LP expr RP SEMICOLON #DoWhileStmt
    | expr SEMICOLON #Expression
    | var=ID EQUALS expr SEMICOLON #AssignStmt
    | var=ID (LSB expr RSB)+ EQUALS expr SEMICOLON #ArrayAssignment
    | RETURN (expr)? SEMICOLON #ReturnStmt
    ;

assignment
    : var=ID EQUALS expr
    ;

arrayInitializer
    : LCB (arrayElement (COMMA arrayElement)*)? RCB
    ;

arrayElement
    : expr
    | arrayInitializer
    ;


expr
    :LP expr RP                                                 #Parenteses      //16
    | expr DOT LENGTH                                           #Length
    | expr DOT name=ID                                          #FieldAcess
    | expr DOT methodname=ID LP ( expr ( COMMA expr )* )? RP               #ExplicitMethodCall
    | expr (LSB expr RSB)+                                      #ArrayAccess
    | expr op=('++'|'--')                                       #RightUnaryExpr  //15
    | op='!' rest=expr                                          #LogicalNot  //14
    | op=('+'|'-'|'++'|'--') expr                               #LeftUnaryExpr   //14
    | expr op=('*'|'/'|'%') expr                                #BinaryExpr      //12
    | expr op=('+'|'-') expr                                    #BinaryExpr      //11
    | expr op=('<'|'>'|'<='|'>=') expr                          #LogicalExpr     //9
    | expr op=('=='|'!=') expr                                  #EqualityExpr    //8
    | expr op='&&' expr                                         #LogicalExpr     // 4
    | expr op='||' expr                                         #LogicalExpr     // 3
    | NEW baseType (LSB size+=expr RSB)+                       #NewArray
    | NEW baseType (dims+='[]')+ arrayInitializer               #ArrayInit
    | NEW name=ID LP ( expr ( COMMA expr )* )? RP                    #ClassInitialization
    | methodname=ID LP (expr (COMMA expr)*)? RP                            #ImplicitMethodCall
    | value=INTEGER                                             #IntegerLiteral
    | bool= (TRUE|FALSE)                                        #BoolLiteral
    | THIS                                                      #This
    | name=ID                                                   #VarRefExpr
    ;

