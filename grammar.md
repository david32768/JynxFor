# Grammar

## Outline

```
[<version>]?
[<macro-library>]?
[<record>|<define-module>|<package>|<define-annotation>|<enum>|<interface>|<class>]

<version> = .version V[0-9]+ [<option>]*
<macro-library> = .macrolib <name>
```

## Class

```
<class> = .class <access-flags> <class-name>
	[<class-header>]?
	[<field>]*
	[<method>]*
```

## Interface

```
<interface> = .interface <access-flags> <class-name>
	[<class-header>]?
	[<field>]*
	[<method>]*
```

## Enum

```
<enum> = .enum <access-flags> <class-name>
	[<class-header>]?
	[<field>]*
	[<method>]*
```

## Record

```
<record> = [.record <access-flags> <class-name>]
	[<class-header>]?
	[<component>]*
	[<field>]* ; .field for each component must be present
	[<method>]* ; .method for each component must be present

<component> = [<simple-component>|<compound-component>]
<simple-component> = .component <component-name> <desc>

<compond-component> = .component <component-name> <desc>
	[<signature>]?
	[<annotation>|<type-annotation>]*
 	.end_component
```
## Define Annotation

```
<define-annotation> = .define_annotation <access-flags> <class-name>
	[class-header]?
	[<method>]
```

## Package

```
<package> = .package <class-name> ; omit "/package-info" from name
	<annotation>*
```

## Module 

```
<define_module> = .define_module
[<class-header>]* ; for those which are valid for module
.module <access-spec> <module-name> [<version>]?
	; in any order
	<main>?
	<requires>*
	<exports>*
	<open>*
	<uses>*
	<provides>*
	<packages>
.end_module
; end of file

<main> = .main <class-name>

<requires> = .requires <access-spec> <module-name> [module-version]?

<exports> = [<unqualified-export>|<qualified export>]
<unqualified-export> = .exports <access-spec> <package-name>
<qualified-export> = .exports <access-spec> <package-name> to <module-name-array>
<module-name-array> = .array
	[<module-name>]+
.end_array
	
<open>=<unqualified-open>|<qualified open>
<unqualified-open> = open <access-spec> <package-name>
<qualified-open> = .open <access-spec> <package-name> to <module-name-array>
	
<uses> = .uses <service-class-name>

<provides> = .provides <service-class-name> with <class-name-array>
<class-name-array> = .array
	[<class-name>]+
.end_array
	
<packages> = .packages <package-name-array>
<package-name-array> = .array
	[<package-name>]+
.end_array
	
```

## Class Header

```
<class-header> =
	[<super>]?
	[<implements>]?
	[<source>]?
	[<debug>]*
	[<innerClassAttribute>]*
	[<nesthost>]?
	[<nestmember>]
	[<permittedSubclass>]?
	[<signature>]?
	[<annotation>]*
	[<type-annotation>]*
```

*	super
```
<super> = .super <class-name>
```
*	implements
```
; must use array if more than one interface
<implements> = .implements [<single-interface>|<multiple-interface>]
<single-interface> = <interface-name>
<multiple-interface> = .array
	<interface-name>+
.end_array
```

*	source
```
<source> = .source <string>
```
*	debug
```
<debug> = .debug <quoted-string>
```
*	InnerClassAttribute
```
<InnerClassAttribute> = [<inner_class_spec>|<enclosing_method>|<outer_class>]
<inner_class_spec> = <inner_type> <inner_class_flags>? <inner_class> [outer <outer_class>]? [innername <inner_name>]? 
<inner-type> = [.inner_class|.inner_interface|.inner_enum|.inner_record|inner_define_annotation]
<inner_class_flags> = <InnerClassAttribute.inner_class_flags>  ; ASM access
<inner_class> = <InnerClassAttribute.inner_class> ; ASM name
<outer_class> =  <InnerClassAttribute.outer_class> ; ASM outer_name
<inner_name> = <InnerClassAttribute.inner_name> ; ASM inner_name

;  NOT Jasmin 2.4
; .inner class <access-spec>? <name>? [inner <classname>] [outer <name>]?
;	name after access-spec is inner_name (which can be absent)
;	classname is inner_class

<enclosing_method> = .enclosing_method <method_id>
<outer_class> = .outer_class <class_id>

```

*	nesthost
```
<nesthost> = .nesthost <host-class-name>
```

*	nestmember
```
<nestmember> =  .nestmember [<member-class-name>| <member-class_names>]
<member-class_names> = .array
	[<member-class-name>]+
.end_array	
; must use .array if more than one nest member
```

*	permittedSubclass
```
<permittedSubclass> = .permittedSubclass [<subclass-name>|<subclass-names>]
<subclass-names> = .array
	[<subclass-name>]+
.end_array
; must use .array if more than one permitted subclass
```

*	hints ; used to help classfile verification if class(es) not available
```
<hints> = .hints .array
	; in any order
	[<interface>]*
	[<extends>]*
.end_array
<interface> = <interface-name> interface
<extends> =  <class_name> extends <super-class-name>
```

*	signature

```
<signature> = .signature <signature-string>
```

## Field

```
<field> = [<simple-field>|<compound-field>]
<simple-field> = .field <field-name> <desc> [= <field-value>]?

<compound-field> = <simple-field>
	[<signature>]?
	[<annotation>]*
	[<type-annotation>]*
 	.end_field
```

*	signature

```
<signature> = .signature <signature-string>
```

## Method

```
<method> = <method-desc>
	<method-header>
	<code-header>
	<instructions>
.end_method
```
### Method Header

```
<method-header> =
	[<signature>]?
	[<throws>]?
	[<parameter-desc>]*
	[visible-parm-count]?
	[invisible-parm-count]?
	[<annotation>]*
	[<type-annotation>]*
```
*	signature

```
<signature> = .signature <signature-string>
```

*	throws
```
; must use array if more than one throw
<throws> = .throws [<single-throw>|<multiple-throw>]
<single-throw> = <class-name>
<multiple-throw> = .array
	[<class-name>]+
.end_array
```

*	parameter description
```
<parameter-desc> =  .parameter <parameter-number> <access-flags>? <name>?
```

*	visible parameter count

```
<visible-parm-count> = .visible_parameter_count <number>
```

*	invisible parameter count

```
<invisible-parm-count> = .invisible_parameter_count <number>
```

### Code Header

```
<code-header> =
	[<catch-block>]*
	[<stack-limit>]?
	[<locals-limit>]?
```

*	catch block
```
<catch-block> = .catch <exception> from <label> to <label> using <label>
	<type-annotation>?
```

*	stack limit
```
<stack-limit> = .limit stack <number>
```

*	locals limit
```
<locals-limit> = .limit locals <number>
```

### Instructions

* switch
```
<switch> default <default_label> <switch-array>
<switch> = [switch|lookupswitch|tableswitch]
; switch generates the [lookupswitch|tableswitch] instruction which has the smallest code size
<switch-array> = .array
	[<num> -> <label>]+
.end_array
```

*	invokeinterface
```
<invokeinterface> = invokeinterface @<class-name> ; omit number as will be calculated
```

*	invokestatic
```
<invokestatic> = invokestatic [<class_method>|<interface_method>]
<interface_method> = @<class-method>
```

*	ldc method-handle
```
ldc <method-handle>
<method-handle> = [<handle-to-method>|<handle-to-field>]

<handle-to-method> = <method-handle-type>:<method-name-desc>
<method-handle-type> = [VL|ST|SP|NW|IN]

<handle-to-field> = <field-handle-type>:<field-name-desc>
<field-handle-type> = [GF|GS|PF|PS]
<field-name-desc> = <field-name>()<field-desc> ; "()" added to make one token
	
```
*	dynamic ldc ; see examples/Java11/Hi.java
```
; (a boot method parameter may be dynamic) 
; ldc { name desc boot_method_and_parameters } 
```
*	.print ; used in code to print debugging information
```
; .print can be nested
<print> =  .print [on|off|label <label-name>|on [locals|stack|offset|expand]+]
```
### Annotation

*	.end_annotation instead of .end annotation
*	parameter annotations start at zero instead of 1
*	default maxparms annotation is numparms(ASM)
*	annotation values must use .array e.g
```
; intArrayValue [I = 0 1 ; Jasmin 2.4
intArrayValue [I = .array ; Jynx
	0
	1
.end_array
```
*	annotation of array of annotations must end line with .annotation_array not .annotation
*	.end_annotation_array NOT .end_annotation after .annotation_array at end of line

*	type_annotations
