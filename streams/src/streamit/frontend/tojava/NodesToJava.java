/*
 * NodesToJava.java: traverse a front-end tree and produce Java objects
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: NodesToJava.java,v 1.24 2002-09-20 15:09:58 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import java.util.Iterator;
import java.util.List;

/**
 * NodesToJava is a front-end visitor that produces Java code from
 * expression trees.  Every method actually returns a String.
 */
public class NodesToJava implements FEVisitor
{
    private StreamType st;
    private String streamName;
    // A string consisting of an even number of spaces.
    private String indent;
    
    public NodesToJava(StreamType st)
    {
        this.st = st;
        this.indent = "";
    }

    // Add two spaces to the indent.
    private void addIndent() 
    {
        indent += "  ";
    }
    
    // Remove two spaces from the indent.
    private void unIndent()
    {
        indent = indent.substring(2);
    }

    // Convert a Type to a String.  If visitors weren't so generally
    // useless for other operations involving Types, we'd use one here.
    public static String convertType(Type type)
    {
        // This is So Wrong in the greater scheme of things.
        if (type instanceof TypeArray)
        {
            TypeArray array = (TypeArray)type;
            String base = convertType(array.getBase());
            return base + "[]";
        }
        else if (type instanceof TypeStruct)
        {
            return ((TypeStruct)type).getName();
        }
        else if (type instanceof TypePrimitive)
        {
            switch (((TypePrimitive)type).getType())
            {
            case TypePrimitive.TYPE_BIT: return "int";
            case TypePrimitive.TYPE_INT: return "int";
            case TypePrimitive.TYPE_FLOAT: return "float";
            case TypePrimitive.TYPE_DOUBLE: return "double";
            case TypePrimitive.TYPE_COMPLEX: return "Complex";
            case TypePrimitive.TYPE_VOID: return "void";
            }
        }
        return null;
    }

    // Do the same conversion, but including array dimensions.
    public String convertTypeFull(Type type)
    {
        if (type instanceof TypeArray)
        {
            TypeArray array = (TypeArray)type;
            return convertTypeFull(array.getBase()) + "[" +
                (String)array.getLength().accept(this) + "]";
        }
        return convertType(type);
    }

    // Get a constructor for some type.
    public String makeConstructor(Type type)
    {
        if (type instanceof TypeArray)
            return "new " + convertTypeFull(type);
        else
            return "new " + convertTypeFull(type) + "()";
    }

    // Get a Java Class object corresponding to a type.
    public String typeToClass(Type t)
    {
        if (t instanceof TypePrimitive)
        {
            switch (((TypePrimitive)t).getType())
            {
            case TypePrimitive.TYPE_BIT:
                return "Integer.TYPE";
            case TypePrimitive.TYPE_INT:
                return "Integer.TYPE";
            case TypePrimitive.TYPE_FLOAT:
                return "Float.TYPE";
            case TypePrimitive.TYPE_DOUBLE:
                return "Double.TYPE";
            case TypePrimitive.TYPE_COMPLEX:
                return "Complex.class";
            }
        }
        else if (t instanceof TypeStruct)
            return ((TypeStruct)t).getName() + ".class";
        else if (t instanceof TypeArray)
            return "(" + makeConstructor(t) + ").getClass()";
        // Errp.
        return null;
    }

    // Helpers to get function names for stream types.
    public static String pushFunction(StreamType st)
    {
        return annotatedFunction("output.push", st.getOut());
    }
    
    public static String popFunction(StreamType st)
    {
        return annotatedFunction("input.pop", st.getIn());
    }
    
    public static String peekFunction(StreamType st)
    {
        return annotatedFunction("input.peek", st.getIn());
    }
    
    private static String annotatedFunction(String name, Type type)
    {
        String prefix = "", suffix = "";
        // Check for known suffixes:
        if (type instanceof TypePrimitive)
        {
            switch (((TypePrimitive)type).getType())
            {
            case TypePrimitive.TYPE_BIT:
                suffix = "Int";
                break;
            case TypePrimitive.TYPE_INT:
                suffix = "Int";
                break;
            case TypePrimitive.TYPE_FLOAT:
                suffix = "Float";
                break;
            case TypePrimitive.TYPE_DOUBLE:
                suffix = "Double";
                break;
            case TypePrimitive.TYPE_COMPLEX:
                if (name.startsWith("input"))
                    prefix  = "(Complex)";
                break;
            }
        }
        else if (name.startsWith("input"))
        {
            prefix = "(" + convertType(type) + ")";
        }
        return prefix + name + suffix;
    }

    // Return a representation of a list of Parameter objects.
    public String doParams(List params)
    {
        String result = "(";
        boolean first = true;
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            if (!first) result += ", ";
            result += convertType(param.getType());
            result += " ";
            result += param.getName();
            first = false;
        }
        result += ")";
        return result;
    }

    // Return a representation of lhs = rhs, with no trailing semicolon.
    public String doAssignment(Expression lhs, Expression rhs,
                               SymbolTable symtab)
    {
        // If the left-hand-side is a complex variable, we need to
        // properly decompose the right-hand side.
        // We can use a null stream type here since the left-hand
        // side shouldn't contain pushes, pops, or peeks.
        GetExprType eType = new GetExprType(symtab, st);
        Type lhsType = (Type)lhs.accept(eType);
        if (lhsType.isComplex())
        {
            Expression real = new ExprField(lhs.getContext(), lhs, "real");
            Expression imag = new ExprField(lhs.getContext(), lhs, "imag");
            // If the right hand side is complex too (at this point
            // just test the run-time type of the expression), then we
            // should do field copies; otherwise we only have a real part.
            if (rhs instanceof ExprComplex)
            {
                ExprComplex cplx = (ExprComplex)rhs;
                return real.accept(this) + " = " +
                    cplx.getReal().accept(this) + ";\n" +
                    imag.accept(this) + " = " +
                    cplx.getImag().accept(this);
            }
            else
                return real.accept(this) + " = " +
                    rhs.accept(this) + ";\n" +
                    imag.accept(this) + " = 0.0";
        }
        else
        {
            // Might want to special-case structures and arrays;
            // ignore for now.
            return lhs.accept(this) + " = " + rhs.accept(this);
        }
    }

    public Object visitExprArray(ExprArray exp)
    {
        String result = "";
        result += (String)exp.getBase().accept(this);
        result += "[";
        result += (String)exp.getOffset().accept(this);
        result += "]";
        return result;
    }
    
    public Object visitExprBinary(ExprBinary exp)
    {
        String result;
        String op = null;
        result = "(";
        result += (String)exp.getLeft().accept(this);
        switch (exp.getOp())
        {
        case ExprBinary.BINOP_ADD: op = "+"; break;
        case ExprBinary.BINOP_SUB: op = "-"; break;
        case ExprBinary.BINOP_MUL: op = "*"; break;
        case ExprBinary.BINOP_DIV: op = "/"; break;
        case ExprBinary.BINOP_MOD: op = "%"; break;
        case ExprBinary.BINOP_AND: op = "&&"; break;
        case ExprBinary.BINOP_OR:  op = "||"; break;
        case ExprBinary.BINOP_EQ:  op = "=="; break;
        case ExprBinary.BINOP_NEQ: op = "!="; break;
        case ExprBinary.BINOP_LT:  op = "<"; break;
        case ExprBinary.BINOP_LE:  op = "<="; break;
        case ExprBinary.BINOP_GT:  op = ">"; break;
        case ExprBinary.BINOP_GE:  op = ">="; break;
        case ExprBinary.BINOP_BAND:op = "&"; break;
        case ExprBinary.BINOP_BOR: op = "|"; break;
        case ExprBinary.BINOP_BXOR:op = "^"; break;
        }
        result += " " + op + " ";
        result += (String)exp.getRight().accept(this);
        result += ")";
        return result;
    }

    public Object visitExprComplex(ExprComplex exp)
    {
        // This should cause an assertion failure, actually.
        String r = "";
        String i = "";
        if (exp.getReal() != null) r = (String)exp.getReal().accept(this);
        if (exp.getImag() != null) i = (String)exp.getImag().accept(this);
        return "/* (" + r + ")+i(" + i + ") */";
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        return "'" + exp.getVal() + "'";
    }

    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        return Double.toString(exp.getVal());
    }

    public Object visitExprConstInt(ExprConstInt exp)
    {
        return Integer.toString(exp.getVal());
    }
    
    public Object visitExprConstStr(ExprConstStr exp)
    {
        return "\"" + exp.getVal() + "\"";
    }

    public Object visitExprField(ExprField exp)
    {
        String result = "";
        result += (String)exp.getLeft().accept(this);
        result += ".";
        result += (String)exp.getName();
        return result;
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
	String result;
	// look for print and println statements; assume everything
	// else is a math function
	if (exp.getName().equals("print")) {
	    result = "System.out.print(";
	} else if (exp.getName().equals("println")) {
	    result = "System.out.println(";
        } else if (exp.getName().equals("super")) {
            result = "super(";
	} else {
	    // Math.sqrt will return a double, but we're only supporting
	    // float's now, so add a cast to float.  Not sure if this is
	    // the right thing to do for all math functions in all cases?
	    result = "(float)Math." + exp.getName() + "(";
	}
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            first = false;
            result += (String)param.accept(this);
        }
        result += ")";
        return result;
    }

    public Object visitExprPeek(ExprPeek exp)
    {
        String result = (String)exp.getExpr().accept(this);
        return peekFunction(st) + "(" + result + ")";
    }
    
    public Object visitExprPop(ExprPop exp)
    {
        return popFunction(st) + "()";
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        String a = (String)exp.getA().accept(this);
        String b = (String)exp.getB().accept(this);
        String c = (String)exp.getC().accept(this);
        switch (exp.getOp())
        {
        case ExprTernary.TEROP_COND:
            return "(" + a + " ? " + b + " : " + c + ")";
        }
        
        return null;
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        String child = (String)exp.getExpr().accept(this);
        switch(exp.getOp())
        {
        case ExprUnary.UNOP_NOT: return "!" + child;
        case ExprUnary.UNOP_NEG: return "-" + child;
        case ExprUnary.UNOP_PREINC: return "++" + child;
        case ExprUnary.UNOP_POSTINC: return child + "++";
        case ExprUnary.UNOP_PREDEC: return "--" + child;
        case ExprUnary.UNOP_POSTDEC: return child + "--";
        }

        return null;
    }

    public Object visitExprVar(ExprVar exp)
    {
        return exp.getName();
    }

    public Object visitFunction(Function func)
    {
        String result = indent + "public ";
        if (!func.getName().equals(streamName))
            result += convertType(func.getReturnType()) + " ";
        result += func.getName();
        result += doParams(func.getParams()) + " ";
        result += (String)func.getBody().accept(this);
        result += "\n";
        return result;
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        // Nothing special here; we get to ignore the I/O rates.
        return visitFunction(func);
    }

    public Object visitProgram(Program prog)
    {
        // Nothing special here either.  Just accumulate all of the
        // structures and streams.
        String result = "";
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            result += indent + "class " + struct.getName() +
                " extends Structure {\n";
            addIndent();
            for (int i = 0; i < struct.getNumFields(); i++)
            {
                String name = struct.getField(i);
                Type type = struct.getType(name);
                result += indent + convertType(type) + " " + name + ";\n";
            }
            unIndent();
            result += indent + "}\n";
        }
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
            result += (String)((StreamSpec)iter.next()).accept(this);
        return result;
    }

    public Object visitSCAnon(SCAnon creator)
    {
        return creator.getSpec().accept(this);
    }
    
    public Object visitSCSimple(SCSimple creator)
    {
        String result = "new " + creator.getName() + "(";
        boolean first = true;
        for (Iterator iter = creator.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            result += (String)param.accept(this);
            first = false;
        }
        result += ")";
        return result;
    }

    public Object visitSJDuplicate(SJDuplicate sj)
    {
        return "DUPLICATE()";
    }

    public Object visitSJRoundRobin(SJRoundRobin sj)
    {
        return "ROUND_ROBIN(" + (String)sj.getWeight().accept(this) + ")";
    }

    public Object visitSJWeightedRR(SJWeightedRR sj)
    {
        String result = "WEIGHTED_ROUND_ROBIN(";
        boolean first = true;
        for (Iterator iter = sj.getWeights().iterator(); iter.hasNext(); )
        {
            Expression weight = (Expression)iter.next();
            if (!first) result += ", ";
            result += (String)weight.accept(this);
            first = false;
        }
        result += ")";
        return result;
    }

    public Object visitStmtAdd(StmtAdd stmt)
    {
        return "add(" + (String)stmt.getCreator().accept(this) + ")";
    }
    
    public Object visitStmtAssign(StmtAssign stmt)
    {
        // Assume both sides are the right type.
        return (String)stmt.getLHS().accept(this) + " = " +
            (String)stmt.getRHS().accept(this);
    }

    public Object visitStmtBlock(StmtBlock stmt)
    {
        String result = "{\n";
        addIndent();
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
            result += indent + (String)((Statement)(iter.next())).accept(this) + ";\n";
        unIndent();
        result += indent + "}";
        return result;
    }

    public Object visitStmtBody(StmtBody stmt)
    {
        return "body(" + (String)stmt.getCreator().accept(this) + ")";
    }
    
    public Object visitStmtBreak(StmtBreak stmt)
    {
        return "break";
    }
    
    public Object visitStmtContinue(StmtContinue stmt)
    {
        return "continue";
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        String result = "do ";
        result += (String)stmt.getBody().accept(this);
        result += "while (" + (String)stmt.getCond().accept(this) + ")";
        return result;
    }

    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
        // Errk: this doesn't become nice Java code.
        return "/* enqueue(" + (String)stmt.getValue().accept(this) +
            ") */";
    }
    
    public Object visitStmtExpr(StmtExpr stmt)
    {
        return (String)stmt.getExpression().accept(this);
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        String result = "for (";
        if (stmt.getInit() != null)
            result += (String)stmt.getInit().accept(this);
        result += "; ";
        if (stmt.getCond() != null)
            result += (String)stmt.getCond().accept(this);
        result += "; ";
        if (stmt.getIncr() != null)
            result += (String)stmt.getIncr().accept(this);
        result += ") ";
        result += (String)stmt.getBody().accept(this);
        return result;
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // must have an if part...
        String result = "if (" + (String)stmt.getCond().accept(this) + ") ";
        result += (String)stmt.getCons().accept(this);
        if (stmt.getAlt() != null)
            result += " else " + (String)stmt.getAlt().accept(this);
        return result;
    }

    public Object visitStmtJoin(StmtJoin stmt)
    {
        return "setJoiner(" + (String)stmt.getJoiner().accept(this) + ")";
    }
    
    public Object visitStmtLoop(StmtLoop stmt)
    {
        return "loop(" + (String)stmt.getCreator().accept(this) + ")";
    }

    public Object visitStmtPush(StmtPush stmt)
    {
        return pushFunction(st) + "(" +
            (String)stmt.getValue().accept(this) + ")";
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        if (stmt.getValue() == null) return "return";
        return "return " + (String)stmt.getValue().accept(this);
    }

    public Object visitStmtSplit(StmtSplit stmt)
    {
        return "setSplitter(" + (String)stmt.getSplitter().accept(this) + ")";
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        String result = convertType(stmt.getType()) + " " + stmt.getName();
        if (stmt.getInit() != null)
            result += " = " + (String)stmt.getInit().accept(this);
        else
        {
            // If the type of the statement isn't a primitive type
            // (or is complex), emit a constructor.
            Type type = stmt.getType();
            if (type.isComplex() || !(type instanceof TypePrimitive))
                result += " = " + makeConstructor(type);
        }
        return result;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        return "while (" + (String)stmt.getCond().accept(this) +
            ") " + (String)stmt.getBody().accept(this);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        String result = "";
        // Anonymous classes look different from non-anonymous ones.
        // This appears in two places: (a) as a top-level (named)
        // stream; (b) in an anonymous stream creator (SCAnon).
        if (spec.getName() != null)
        {
            // Non-anonymous stream:
            result += indent + "public class " + spec.getName() + " extends ";
            switch (spec.getType())
            {
            case StreamSpec.STREAM_FILTER: result += "Filter";
                break;
            case StreamSpec.STREAM_PIPELINE: result += "Pipeline";
                break;
            case StreamSpec.STREAM_SPLITJOIN: result += "SplitJoin";
                break;
            case StreamSpec.STREAM_FEEDBACKLOOP: result += "FeedbackLoop";
                break;
            }
            result += "\n" + indent + "{\n";
            addIndent();
        }
        else
        {
            // Anonymous stream:
            switch (spec.getType())
            {
            case StreamSpec.STREAM_FILTER: result += "Filter";
                break;
            case StreamSpec.STREAM_PIPELINE: result += "Pipeline";
                break;
            case StreamSpec.STREAM_SPLITJOIN: result += "SplitJoin";
                break;
            case StreamSpec.STREAM_FEEDBACKLOOP: result += "FeedbackLoop";
                break;
            }
            result += "() {\n" + indent;
            addIndent();
        }
        
        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        StreamType oldST = st;
        st = spec.getStreamType();
        String oldStreamName = streamName;
        streamName = spec.getName();

        // Output field definitions:
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            Statement varDecl = (Statement)iter.next();
            result += indent + (String)varDecl.accept(this) + ";\n";
        }
        
        // Output method definitions:
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
            result += (String)(((Function)iter.next()).accept(this));

        streamName = oldStreamName;
        st = oldST;
        unIndent();
        result += "}\n";
        return result;
    }
    
    public Object visitStreamType(StreamType type)
    {
        // Nothing to do here.
        return "";
    }
    
    public Object visitOther(FENode node)
    {
        return "";
    }
}
