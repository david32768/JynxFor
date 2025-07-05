# Jynx
[Jynx(bird)](https://en.wikipedia.org/wiki/Wryneck)

## JynxFor

```
 {JynxFree} jynx {options}   [.jx_file]? 
   (produces a class file from a .jx file)


Options are:

*	--SYSIN use SYSIN as input file (it can be abbreviated to '-'). (omit .jx_file)
*	--USE_STACK_MAP use user stack map instead of ASM generated
*	--WARN_UNNECESSARY_LABEL warn if label unreferenced or alias
*	--WARN_STYLE warn if names non-standard
*	--GENERATE_LINE_NUMBERS generate line numbers
*	--SYMBOLIC_LOCAL local variables are symbolic not absolute integers
*	--BASIC_VERIFIER use ASM BasicVerifier instead of ASM SimpleVerifier
*	--ALLOW_CLASS_FORNAME let simple verifier use Class.forName() for non-java classes
*	--CHECK_REFERENCES check that called methods or used fields exist (on class path)
*	--VALIDATE_ONLY do not output class file
*	--TRACE print (ASMifier) trace
*	--DEBUG exit with stack trace if error
*	--VERBOSE print all log messages
*	--TREAT_WARNINGS_AS_ERRORS treat warnings as errors
```

This is a rewritten version of [Jasmin](https://github.com/davidar/jasmin)
 using [ASM](https://asm.ow2.io) version 9.8 as a back end.
 
It requires Java V24
 and supports all features up to V24 except user attributes.

More checking is done before using ASM. For example
 stack and local variables types are checked assuming
 all objects are Object.

ASM is used to generate stack maps where required.

However the stack status must be provided on a label if all the following are true:

*	the label is after an unconditional branch
*	label is not previously branched to
*	label is not an exception handler
*	stack is not empty
*	a stack map is not provided 

The opportunity has beeen taken to change the syntax of some statements.

It supports "macros" as a service.

## WARNING

*	There will be invalid classes which will not produce errors.
*	There will be "valid" classes which will fail.

## Jasmin 1.0

Reference: **Java Virtual Machine** by Jon Meyer and Troy Downing; O'Reilly 1997

Changes are:

*	unicode escape sequences are actioned before parsing line
*	.end_method instead of .end method
*	float constants must be suffixed by 'F' and long constants by 'L'
*	hexadecimal constants are supported

*	lookupswitch - new format
```
lookupswitch default DefaultLabel .array
	1 -> Label1
	10 -> Label2
.end_array
```
*	tableswitch - new format
```
tableswitch default DefaultLabel .array
	0 -> ZeroLabel
	1 -> OneLabel
.end_array
```		
*	.implements - must use array if more than one interface
```
;.implements Interface1
;.implements Interface2
.implements .array
	Interface1
	Interface2
.end_array
```
*	.throws - must use array if more than one throw
```
;.throws Exception1
;.throws Exception2
.throws .array
	Exception1
	Exception2
.end_array
```
*	invokeinterface; omit number as will be calculated and precede method_name with '@'
```
; invokeinterface java/util/Enumeration/hasMoreElements()Z 1
invokeinterface @java/util/Enumeration.hasMoreElements()Z
```
*	if .limit is omitted it will be calculated rather than defaulting to 1
*	class names etc. must be valid Java names
*	labels are constrained to be a Java Id or if generated start with an @
*	.interface must be used to declare an interface rather than .class interface
```
; .class interface abstract anInterface
.interface anInterface
```
*	labels in .catch must not be previously defined
*	if .var labels are omitted then from start_method to end_method is assumed
*	default version is V17(61.0)
  
## Jasmin 2.4

Changes are

*	offsets instead of labels are NOT supported
*	user attributes are NOT supported
*	.bytecode -> .version
```
; .bytecoode 61.0 ; Jasmin 2.4
.version V17 ; Jynx
```
*	options (without -- prefix) can be on .version directive
```
.version V17 GENERATE_LINE_NUMBERS
```
*	.deprecated removed; use "deprecated" pseudo-access_flag
```
; .class public aClass
; .deprecated ; Jasmin 2.4
.class public deprecated aClass ; Jynx
; etc.
```
*	.enum instead of .class enum
```
; .class enum anEnum ; Jasmin 2.4
.enum anEnum [ Jynx
```
*	.define_annotation instead of .class annotation
```
; .class annotation interface abstract anAnnotationClass ; Jasmin 2.4
.define_annotation anAnnotationClass ; Jynx
```
*	.inner class -> .inner_class, .inner interface -> .inner_interface etc.
```
; Jasmin 2.4
; .inner class <access-spec>? <name>? [inner <classname>] [outer <name>]?
;	name after access-spec is inner_name (which can be absent)
;	classname is inner_class

; Jynx
; .inner_class <access-spec>? <inner_class> [outer <outer_class>]? [innername <inner_name>]?
;	i.e. change 
; .inner class x inner y$z outer w ; Jasmin 2.4
.inner_class y$z outer w innername x ; Jynx
```
*	.enclosing method -> .enclosing_method or .outer_class as appropriate
*	invokedynamic boot method and parameters must be specified
```
; (a boot method parameter may be dynamic) 
; invokedynamic { name desc  boot_method_and_parameters }
; see examples/Java11/Hi.jx
```
*	An interface method name should be preceded with a '@' in invoke ops and handles
```
; invokestatic anInterfaceMethod ; Jasmin 2.4
invokestatic @anInterfaceMethod ; Jynx
```
*	if signature of a field is present must use .signature directive (NOT appear in .field directive) 
*	.package
```
; .class interface abstract aPackage/package-info ; Jasmin 2.4
.package aPackage ; Jynx
```

### ANNOTATIONS

*	.end_annotation instead of .end annotation
*	parameter annotations start at zero instead of 1
*	default maxparms annotation is numparms(ASM)
*	[ annotation values must use .array e.g
```
; intArrayValue [I = 0 1 ; Jasmin 2.4
intArrayValue [I = .array ; Jynx
	0
	1
.end_array
```
*	annotation of array of annotations must end line with .annotation_array not .annotation
*	.end_annotation_array NOT .end_annotation after .annotation_array at end of line


## Additions

*	switch
```
; use lookupswitch or tableswitch whichever has the smallest size.
switch default DefaultLabel .array
	1 -> Label1
	10 -> Label2
.end_array
```
*	.parameter
```
.parameter 0 final p0
```
*	.nesthost
```
.nesthost x/y
```
*	.nestmember
```
.nestmember x/y
; must use .array if more than one nest member
```
*	.permittedSubclass
```
.permittedSubclass x/y
; must use .array if more than one permitted subclass
```
*	add support for method-handle to ldc
```
; examples
; handle for smallest POSITIVE float
ldc GS:java/lang/Float.MIN_VALUE()F

ldc ST:java/lang/Integer.getInteger(Ljava/lang/String;)java/lang/Integer

```
*	dynamic ldc ; see examples/Java11/Hi.jx
```
; (a boot method parameter may be dynamic) 
; ldc { name desc boot_method_and_parameters } 
```
*	type_annotations
*	.macrolib <macro-library-name>
*	.hints ; used to help verification if class(es) not available
*	.print ; used in code to print debugging information
*	.record ; see examples/Java17/Point.jx
*	.component
```
.component x I
```
*	.define_module ; see examples/Java11/module-info.jx
