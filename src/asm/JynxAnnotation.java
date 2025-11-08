package asm;

import java.util.Optional;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.CheckAnnotationAdapter;

import static com.github.david32768.jynxfor.my.Message.*;
import static com.github.david32768.jynxfree.jynx.Global.*;
import static com.github.david32768.jynxfree.jynx.ReservedWord.*;

import static com.github.david32768.jynxfree.jvm.Context.ANNOTATION;

import com.github.david32768.jynxfor.scan.ConstType;
import com.github.david32768.jynxfor.scan.JynxScanner;
import com.github.david32768.jynxfor.scan.Line;
import com.github.david32768.jynxfor.scan.LinesIterator;
import com.github.david32768.jynxfor.scan.Token;
import com.github.david32768.jynxfor.scan.TokenArray;

import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.TypeRef;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.LogAssertionError;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.LogIllegalStateException;
import com.github.david32768.jynxfree.jynx.NameDesc;
import com.github.david32768.jynxfree.jynx.ReservedWord;

public class JynxAnnotation {

    private final ContextDependent sd;
    private final JynxScanner js;
    private final Directive dir;
    
    private JynxAnnotation(ContextDependent sd, JynxScanner js, Directive dir) {
        this.sd = sd;
        this.js = js;
        this.dir = dir;
    }

    public static void setAnnotation(Directive dir, ContextDependent sd, JynxScanner js) {
        JynxAnnotation ja = new JynxAnnotation(sd,js,dir);
        ja.visitAnnotation();
    }
    
    private void visitAnnotation() {
        AnnotationVisitor av;
        try {
            av = getAnnotationVisitor();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            js.skipTokens();
            LOG(ex);
            av = new AnnotationNode("throw_away"); // syntax check and throw away
        }
        av = new CheckAnnotationAdapter(av);
        visitAnnotationValues(av);
    }
    
    private AnnotationVisitor getAnnotationVisitor() {
        int paramStart = 0;
        Line line = js.getLine();
        Context acctype = sd.getContext();
        if (dir == Directive.dir_except_type_annotation) {
            acctype = Context.CATCH;
        }
        return switch (dir) {
            case dir_annotation -> {
                ReservedWord visibility = line.nextToken().expectOneOf(res_visible, res_invisible);
                String classdesc = line.nextToken().asString();
                NameDesc.CLASS_PARM.validate(classdesc);
                line.noMoreTokens();
                yield sd.visitAnnotation(classdesc, visibility == res_visible);
            }
            case dir_default_annotation -> {
                line.noMoreTokens();
                yield sd.visitAnnotationDefault();
            }
            case dir_parameter_annotation -> {
                ReservedWord visibility = line.nextToken().expectOneOf(res_visible, res_invisible);
                int parameter = line.nextToken().asInt();
                parameter -= paramStart;
                String classdesc = line.nextToken().asString();
                NameDesc.CLASS_PARM.validate(classdesc);
                line.noMoreTokens();
                yield sd.visitParameterAnnotation(classdesc, parameter, visibility == res_visible);
            }
            case dir_except_type_annotation, dir_argmethod_type_annotation,
                    dir_argmethodref_type_annotation, dir_argnew_type_annotation,
                    dir_argnewref_type_annotation, dir_cast_type_annotation,
                    dir_extends_type_annotation, dir_field_type_annotation,
                    dir_formal_type_annotation, dir_instanceof_type_annotation,
                    dir_methodref_type_annotation, dir_new_type_annotation,
                    dir_newref_type_annotation, dir_param_bound_type_annotation,
                    dir_param_type_annotation, dir_receiver_type_annotation,
                    dir_resource_type_annotation, dir_return_type_annotation,
                    dir_throws_type_annotation, dir_var_type_annotation -> {
                ReservedWord visibility = line.nextToken().expectOneOf(res_visible, res_invisible);
                TypeRef tr = TypeRef.getInstance(dir,acctype);
                int numind = tr.getNumberIndices();
                int[] indices = new int[numind];
                for (int i = 0; i < numind; ++i) {
                    indices[i] = line.nextToken().asInt();
                }
                int typeref = tr.getTypeRef(indices);
                Optional<String> typepathstr = line.optAfter(res_typepath);
                TypePath typepath = TypePath.fromString(typepathstr.orElse(null));
                String desc = line.nextToken().asString();
                yield sd.visitTypeAnnotation(typeref, typepath, desc, visibility == res_visible);
            }
            default -> // "unknown directive %s for context %s"
                throw new LogAssertionError(M907,dir,Context.ANNOTATION);
        };
    }

    private void visitAnnotationValues(AnnotationVisitor av) {
        try (LinesIterator lines = new LinesIterator(js,Directive.end_annotation)) {
            while(lines.hasNext()) {
                Line line = lines.next();
                String name = line.firstToken().asString();
                String chdesc;
                if (dir == Directive.dir_default_annotation) {
                    chdesc = name;
                } else {
                    chdesc = line.nextToken().asString();
                }
                boolean array = chdesc.startsWith("[");
                if (array) {
                    chdesc = chdesc.substring(1);
                }
                if (chdesc.length() != 1) {
                    throw new LogIllegalArgumentException(M96); // "syntax error in annotation field type"
                }
                char typech = chdesc.charAt((0));
                switch (typech) {
                    case '@' -> {
                        // case '&':
                        String desc = line.nextToken().asString();
                        line.nextToken().mustBe(equals_sign);
                        Token dot = line.nextToken();
                        if (array) {
                            dot.mustBe(dot_annotation_array);
                            line.noMoreTokens();
                            Directive enddir = Directive.end_annotation_array;
                            visitArrayOfAnnotations(av, name,desc,enddir);
                        } else {
                            dot.mustBe(dot_annotation);
                            line.noMoreTokens();
                            visitAnnotationValues(av.visitAnnotation(name, desc));
                        }
                    }
                    case '\n' -> {
                        AnnotationVisitor avnull = av.visitArray(name);
                        avnull.visitEnd();
                    }
                    default -> {
                        String enumdesc = typech == 'e'?line.nextToken().asString():null;
                        assert typech != '@';
                        ConstType cta = ConstType.getInstance(typech,ANNOTATION);
                        line.nextToken().mustBe(equals_sign);
                        if (array) {
                            AnnotationVisitor avarr = av.visitArray(name);
                            try (TokenArray tokens = line.getTokenArray()) {
                                while (true) {
                                    Token token = tokens.firstToken();
                                    if (token.is(right_array)) {
                                        break;
                                    }
                                    Object value = token.getValue(cta);
                                    visitvalue(avarr,name,enumdesc,value);
                                    tokens.noMoreTokens();
                                }
                                avarr.visitEnd();
                            }
                        } else {
                            Token token = line.lastToken();
                            Object value = token.getValue(cta);
                            visitvalue(av,name,enumdesc,value);
                        }
                    }
                }
            }
            av.visitEnd();
        }
    }
    
    private void visitvalue(AnnotationVisitor av,String name,String enumdesc,Object value) {
        if (enumdesc == null) {
            av.visit(name, value);
        } else {
            av.visitEnum(name, enumdesc, value.toString());
        }
    }
    
    private void visitArrayOfAnnotations(AnnotationVisitor av, String name,String desc, Directive enddir) {
        AnnotationVisitor avarr = av.visitArray(name);
        while(true) {
            Line line = js.next();
            Token token = line.firstToken();
            Directive dirx = token.asDirective();
            if (dirx == enddir) {
                break;
            }
            if (dirx != Directive.dir_annotation) {
                throw new LogIllegalStateException(M168,dirx); // "unexpected directive(%s) in annotation"
            }
            line.noMoreTokens();
            AnnotationVisitor avarrav = avarr.visitAnnotation(name, desc);
            visitAnnotationValues(avarrav);
        }
        avarr.visitEnd();
    }

}
