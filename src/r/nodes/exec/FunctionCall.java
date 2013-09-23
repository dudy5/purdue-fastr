package r.nodes.exec;

import r.*;
import r.builtins.*;
import r.data.*;
import r.errors.*;
import r.nodes.ast.*;
import r.nodes.tools.*;
import r.runtime.*;

// TODO: fix (extend?) the propagation of scalar values and values with guards, currently it is very restricted
public abstract class FunctionCall extends AbstractCall {

    public final static boolean PROMISES = true;

    @Child RNode callableExpr;
    protected final int[] dotsArgs; // FIXME: move to FunctionCall?

    private FunctionCall(ASTNode ast, RNode callableExpr, RSymbol[] argNames, RNode[] argExprs, int[] dotsArgs) {
        super(ast, argNames, argExprs);
        this.callableExpr = adoptChild(callableExpr);
        this.dotsArgs = dotsArgs;
    }

    @Override
    protected <N extends RNode> N replaceChild(RNode oldNode, N newNode) {
        assert oldNode != null;
        if (callableExpr == oldNode) {
            callableExpr = newNode;
            return adoptInternal(newNode);
        }
        return super.replaceChild(oldNode, newNode);
    }

    public static FunctionCall getFunctionCall(ASTNode ast, RNode callableExpr, RSymbol[] argNames, RNode[] argExprs) {
        // place optimized nodes here, e.g. for positional-only argument passing
        // (but beware, at some point we had them and their were helping, but later there was no longer any performance improvement)

        int[] dotsArgs = findDotsArgs(argExprs);
        if (dotsArgs != null) {
            return new GenericDotsCall(ast, callableExpr, argNames, argExprs, dotsArgs);
        } else {
            return new UninitializedCall(ast, callableExpr, argNames, argExprs);
        }
    }

    // returns indexes of ... arguments (or null if none present)
    public static int[] findDotsArgs(RNode[] argExprs) {

        int[] res = new int[argExprs.length];
        int j = 0;
        for (int i = 0; i < argExprs.length; i++) {
            RNode expr = argExprs[i];
            if (expr == null) {
                continue;
            }
            ASTNode ast = expr.getAST();
            if (ast instanceof SimpleAccessVariable && ((SimpleAccessVariable) ast).getSymbol() == RSymbol.THREE_DOTS_SYMBOL) {
                res[j++] = i;
            }
        }
        if (j == 0) {
            return null;
        } else {
            int[] toret = new int[j];
            System.arraycopy(res, 0, toret, 0, j);
            return toret;
        }
    }

    public static CallFactory FACTORY = new CallFactory("<empty>") { // only used with static lookup of builtins

        @Override public FunctionCall create(ASTNode call, RSymbol[] names, RNode[] exprs) {
            r.nodes.ast.FunctionCall fcall = (r.nodes.ast.FunctionCall) call;
            RSymbol fname = fcall.getName();
            RNode fexp = r.nodes.exec.MatchCallable.getUninitialized(call, fname);
            return getFunctionCall(fcall, fexp, names, exprs);
        }
    };

    public static AbstractCall createBuiltinCall(ASTNode call, RSymbol[] argNames, RNode[] argExprs) {

        int[] dotsArgs = findDotsArgs(argExprs);
        if (dotsArgs != null) { return null; // can't use a fixed builtin node when calling using ...
        }

        r.nodes.ast.FunctionCall fcall = (r.nodes.ast.FunctionCall) call;
        RSymbol fname = fcall.getName();

        RBuiltIn builtin = Primitives.getBuiltIn(fname, null);
        if (builtin != null) {
            // probably calling a builtin, but maybe not
            RNode builtinNode;
            try {
                builtinNode = builtin.callFactory().create(call, argNames, argExprs);
            } catch (RError e) {
                // not a builtin
                // TODO: what if the attempt to create a builtin has produced warnings???
                return null;
            }
            assert Utils.check(builtinNode != null);
            return new SimpleBuiltinCall(fcall, fname, argNames, argExprs, builtinNode);
        }
        return null;
    }

    public static final class UninitializedCall extends FunctionCall {

        public UninitializedCall(ASTNode ast, RNode callableExpr, RSymbol[] argNames, RNode[] argExprs) {
            super(ast, callableExpr, argNames, argExprs, null);
        }

        @Override public Object execute(Frame callerFrame) {
            Object callable = callableExpr.execute(callerFrame);
            try {
                throw new SpecializationException(null);
            } catch (SpecializationException e) {
                RNode n;
                RNode theCallableExpr = callableExpr;
                if (callable instanceof RBuiltIn) {
                    RBuiltIn builtIn = (RBuiltIn) callable;
                    RNode builtInNode = builtIn.callFactory().create(ast, argNames, argExprs);
                    n = new StableBuiltinCall(ast, callableExpr, argNames, argExprs, builtIn, builtInNode);
                } else {
                    n = new GenericCall(ast, callableExpr, argNames, argExprs);
                }
                return replace(theCallableExpr, callable, n, callerFrame);
            }
        }
    }

    // calling a non-overridden builtin via its standard name
    public static final class SimpleBuiltinCall extends AbstractCall {

        final RSymbol builtinName;
        @Child RNode builtinNode;

        SimpleBuiltinCall(ASTNode ast, RSymbol builtinName, RSymbol[] argNames, RNode[] argExprs, RNode builtInNode) {
            super(ast, argNames, argExprs, false);
            // NOTE: argExprs are not children - the real parent of the exprs is the builtin
            // NOTE: as long as this array is _shared_ with the builtin, it all works with node rewriting
            // FIXME: this feels indeed quite fragile

            // FIXME: also, this does not work for builtins which take their argument nodes out of the arguments array (!)
            // FIXME: probably we should keep a copy of the tree (or create more "root" nodes, but that may have performance overhead)

            this.builtinName = builtinName;
            this.builtinNode = adoptChild(builtInNode);
        }

        @Override public Object execute(Frame callerFrame) {
            try {
                if (builtinName.getValue() != null || builtinName.getVersion() != 0) { throw new SpecializationException(null); }
                return builtinNode.execute(callerFrame);
            } catch (SpecializationException e) {
                RNode callableExpr = r.nodes.exec.MatchCallable.getUninitialized(ast, builtinName);
                return replace(getFunctionCall(ast, callableExpr, argNames, argExprs)).execute(callerFrame);
            }
        }

        @Override
        protected <N extends RNode> N replaceChild(RNode oldNode, N newNode) {
            assert oldNode != null;
            if (builtinNode == oldNode) {
                builtinNode = newNode;
                return adoptInternal(newNode);
            }
            return super.replaceChild(oldNode, newNode);
        }

    }

    public static final class StableBuiltinCall extends BaseR {

        final RBuiltIn builtIn; // null when last callable wasn't a builtin
        @Child RNode builtInNode;
        @Child RNode callableExpr;

        final RNode[] rememberedArgExprs; // NOTE: not children - the real parent of the exprs is the builtin
        final RSymbol[] rememberedArgNames;

        StableBuiltinCall(ASTNode ast, RNode callableExpr, RSymbol[] argNames, RNode[] argExprs, RBuiltIn builtIn, RNode builtInNode) {
            super(ast);
            this.callableExpr = adoptChild(callableExpr);
            this.rememberedArgNames = argNames;
            this.rememberedArgExprs = argExprs; // NOTE: not children
            this.builtIn = builtIn;
            this.builtInNode = adoptChild(builtInNode);
        }

        @Override public Object execute(Frame callerFrame) {
            Object callable = callableExpr.execute(callerFrame);
            try {
                if (callable != builtIn) { throw new SpecializationException(null); }
                return builtInNode.execute(callerFrame);
            } catch (SpecializationException e) {
                RNode theCallableExpr = callableExpr;
                GenericCall n = new GenericCall(ast, callableExpr, rememberedArgNames, rememberedArgExprs);
                return replace(theCallableExpr, callable, n, callerFrame);
            }
        }

        @Override
        protected <N extends RNode> N replaceChild(RNode oldNode, N newNode) {
            assert oldNode != null;
            if (builtInNode == oldNode) {
                builtInNode = newNode;
                return adoptInternal(newNode);
            }
            if (callableExpr == oldNode) {
                callableExpr = newNode;
                return adoptInternal(newNode);
            }
            if (rememberedArgExprs != null) {
                for(int i = 0; i < rememberedArgExprs.length; i++) {
                    if (rememberedArgExprs[i] == oldNode) {
                        rememberedArgExprs[i] = newNode;
                        return adoptInternal(newNode);
                    }
                }
            }
            return super.replaceChild(oldNode, newNode);
        }
    }

    public static final class GenericCall extends FunctionCall {

        Object lastCallable; // RCallable, but using Object to avoid cast

        // for functions
        RClosure lastClosure; // null when last callable wasn't a function (closure)
        RFunction closureFunction;
        int[] functionArgPositions;
        Frame closureEnclosingFrame;
        final DotsInfo functionDotsInfo = new DotsInfo();

        // for builtins
        RBuiltIn lastBuiltIn; // null when last callable wasn't a builtin
        RSymbol builtInName;
        @Child RNode builtInNode;

        GenericCall(ASTNode ast, RNode callableExpr, RSymbol[] argNames, RNode[] argExprs) {
            super(ast, callableExpr, argNames, argExprs, null);
        }

        @Override public Object execute(Frame callerFrame) {
            Object callable = callableExpr.execute(callerFrame);
            if (callable == lastClosure) {
                Frame newFrame = closureFunction.createFrame(closureEnclosingFrame);
                placeArgs(callerFrame, newFrame, functionArgPositions, functionDotsInfo, closureFunction.dotsIndex(), closureFunction.nparams());
                return closureFunction.call(newFrame);
            }
            if (callable == lastBuiltIn) { return builtInNode.execute(callerFrame); }
            if (callable instanceof RClosure) {
                RClosure closure = (RClosure) callable;
                RFunction function = closure.function();
                if (function != closureFunction) {
                    closureFunction = function;
                    functionArgPositions = computePositions(closureFunction, functionDotsInfo);
                }
                closureEnclosingFrame = closure.enclosingFrame();
                lastClosure = closure;
                lastBuiltIn = null;
                Frame newFrame = closureFunction.createFrame(closureEnclosingFrame);
                placeArgs(callerFrame, newFrame, functionArgPositions, functionDotsInfo, closureFunction.dotsIndex(), closureFunction.nparams());
                return closureFunction.call(newFrame);
            } else {
                // callable instanceof RBuiltin
                RBuiltIn builtIn = (RBuiltIn) callable;
                RSymbol name = builtIn.name();
                if (name != builtInName) {
                    builtInName = name;
                    if (builtInNode == null) {
                        builtInNode = adoptChild(builtIn.callFactory().create(ast, argNames, argExprs));
                    } else {
                        builtInNode.replace(builtIn.callFactory().create(ast, argNames, argExprs));
                    }
                }
                lastBuiltIn = builtIn;
                lastClosure = null;
                return builtInNode.execute(callerFrame);
            }
        }

        @Override
        protected <N extends RNode> N replaceChild(RNode oldNode, N newNode) {
            assert oldNode != null;
            if (builtInNode == oldNode) {
                builtInNode = newNode;
                return adoptInternal(newNode);
            }
            return super.replaceChild(oldNode, newNode);
        }

        // FIXME: essentially copy paste of execute
        // TODO: it would be far more important to have these in simple and stable builtin call than here
        @Override public int executeScalarLogical(Frame callerFrame) throws SpecializationException {
            Object callable = callableExpr.execute(callerFrame);
            if (callable == lastClosure) {
                Frame newFrame = closureFunction.createFrame(closureEnclosingFrame);
                placeArgs(callerFrame, newFrame, functionArgPositions, functionDotsInfo, closureFunction.dotsIndex(), closureFunction.nparams());
                return RValueConversion.expectScalarLogical((RAny) closureFunction.call(newFrame));
            }
            if (callable == lastBuiltIn) { return builtInNode.executeScalarLogical(callerFrame); }
            if (callable instanceof RClosure) {
                RClosure closure = (RClosure) callable;
                RFunction function = closure.function();
                if (function != closureFunction) {
                    closureFunction = function;
                    functionArgPositions = computePositions(closureFunction, functionDotsInfo);
                }
                closureEnclosingFrame = closure.enclosingFrame();
                lastClosure = closure;
                lastBuiltIn = null;
                Frame newFrame = closureFunction.createFrame(closureEnclosingFrame);
                placeArgs(callerFrame, newFrame, functionArgPositions, functionDotsInfo, closureFunction.dotsIndex(), closureFunction.nparams());
                return RValueConversion.expectScalarLogical((RAny) closureFunction.call(newFrame));
            } else {
                // callable instanceof RBuiltin
                RBuiltIn builtIn = (RBuiltIn) callable;
                RSymbol name = builtIn.name();
                if (name != builtInName) {
                    builtInName = name;
                    if (builtInNode == null) {
                        builtInNode = adoptChild(builtIn.callFactory().create(ast, argNames, argExprs));
                    } else {
                        builtInNode.replace(builtIn.callFactory().create(ast, argNames, argExprs));
                    }
                }
                lastBuiltIn = builtIn;
                lastClosure = null;
                return builtInNode.executeScalarLogical(callerFrame);
            }
        }

        // FIXME: essentially copy paste of execute
        // TODO: it would be far more important to have these in simple and stable builtin call than here
        @Override public int executeScalarNonNALogical(Frame callerFrame) throws SpecializationException {
            Object callable = callableExpr.execute(callerFrame);
            if (callable == lastClosure) {
                Frame newFrame = closureFunction.createFrame(closureEnclosingFrame);
                placeArgs(callerFrame, newFrame, functionArgPositions, functionDotsInfo, closureFunction.dotsIndex(), closureFunction.nparams());
                return RValueConversion.expectScalarNonNALogical((RAny) closureFunction.call(newFrame));
            }
            if (callable == lastBuiltIn) { return builtInNode.executeScalarNonNALogical(callerFrame); }
            if (callable instanceof RClosure) {
                RClosure closure = (RClosure) callable;
                RFunction function = closure.function();
                if (function != closureFunction) {
                    closureFunction = function;
                    functionArgPositions = computePositions(closureFunction, functionDotsInfo);
                }
                closureEnclosingFrame = closure.enclosingFrame();
                lastClosure = closure;
                lastBuiltIn = null;
                Frame newFrame = closureFunction.createFrame(closureEnclosingFrame);
                placeArgs(callerFrame, newFrame, functionArgPositions, functionDotsInfo, closureFunction.dotsIndex(), closureFunction.nparams());
                return RValueConversion.expectScalarNonNALogical((RAny) closureFunction.call(newFrame));
            } else {
                // callable instanceof RBuiltin
                RBuiltIn builtIn = (RBuiltIn) callable;
                RSymbol name = builtIn.name();
                if (name != builtInName) {
                    builtInName = name;
                    if (builtInNode == null) {
                        builtInNode = adoptChild(builtIn.callFactory().create(ast, argNames, argExprs));
                    } else {
                        builtInNode.replace(builtIn.callFactory().create(ast, argNames, argExprs));
                    }
                }
                lastBuiltIn = builtIn;
                lastClosure = null;
                return builtInNode.executeScalarNonNALogical(callerFrame);
            }
        }

    }

    // function call that passes "..."
    public static final class GenericDotsCall extends FunctionCall {

        GenericDotsCall(ASTNode ast, RNode callableExpr, RSymbol[] argNames, RNode[] argExprs, int[] dotsArgs) {
            super(ast, callableExpr, argNames, argExprs, dotsArgs);
            assert Utils.check(dotsArgs != null);
        }

        @Override public Object execute(Frame callerFrame) {
            Object callable = callableExpr.execute(callerFrame);

            if (callable instanceof RClosure) {
                RClosure closure = (RClosure) callable;
                RFunction function = closure.function();
                Frame newFrame = function.createFrame(closure.enclosingFrame());
                placeDotsArgs(callerFrame, newFrame, function.paramNames());
                return function.call(newFrame);

            } else {
                // FIXME: these calls to builtin seem pretty expensive

                assert Utils.check(callable instanceof RBuiltIn);
                RBuiltIn builtIn = Utils.cast(callable);

                int nextDots = dotsArgs[0];
                RDots dotsArg = (RDots) argExprs[nextDots].execute(callerFrame);
                RSymbol[] dotsArgNames = dotsArg.names();
                Object[] dotsArgValues = dotsArg.values();
                int dotsArgLen = dotsArgNames.length;
                int ndots = dotsArgs.length;
                int nArgs = argExprs.length + ndots * (dotsArgLen - 1);

                RSymbol[] actualArgNames = new RSymbol[nArgs];
                RNode[] actualArgExprs = new RNode[nArgs];
                int dotsIndex = 0;

                for (int i = 0, j = 0; j < nArgs; i++) {
                    if (i == nextDots) {
                        for (int k = 0; k < dotsArgLen; k++, j++) {
                            actualArgNames[j] = dotsArgNames[k];
                            Object value = dotsArgValues[k];

                            if (FunctionCall.PROMISES && value instanceof RPromise) {
                                final RPromise promise = (RPromise) value;
                                actualArgExprs[j] = new BaseR(promise.expression().getAST()) {

                                    @Override public Object execute(Frame frame) {
                                        return promise.forceOrGet();
                                    }

                                };
                            } else {
                                // NOTE: in GNU-R, dots arguments are re-promised on a call, so this would be unreachable
                                assert Utils.check(value instanceof RAny);
                                RAny rvalue = Utils.cast(value);

                                ASTNode dummyAST = new r.nodes.ast.Constant(rvalue);
                                actualArgExprs[j] = new r.nodes.exec.Constant(dummyAST, rvalue);
                            }

                        }
                        dotsIndex++;
                        if (dotsIndex < ndots) {
                            nextDots = dotsIndex;
                        }
                    } else {
                        actualArgNames[j] = argNames[i];
                        actualArgExprs[j] = argExprs[i];
                        j++;
                    }
                }
                return builtIn.callFactory().invokeDynamic(callerFrame, actualArgNames, actualArgExprs, ast);
            }
        }
    }

 // used when the call is passing "..." (note, ... can be passed more than once)
    protected final void placeDotsArgs(Frame callerFrame, Frame newFrame, RSymbol[] paramNames) {

        int nextDots = dotsArgs[0];
        RDots dotsArg = (RDots) argExprs[nextDots].execute(callerFrame);
        RSymbol[] dotsArgNames = dotsArg.names();
        Object[] dotsArgValues = dotsArg.values();
        int dotsArgLen = dotsArgNames.length;
        int ndots = dotsArgs.length;
        int nArgs =  argExprs.length + ndots * (dotsArgLen - 1);
        int nParams = paramNames.length;

        RSymbol[] actualArgNames = new RSymbol[nArgs];
        Object[] actualArgValues = new Object[nArgs];
        int dotsIndex = 0;

        // copy all arguments from dots into a single arguments array, which makes the matching easier
        // FIXME: could do without this, but the iteration over arguments would be more difficult, and perhaps
        // the whole call won't then be faster, anyway
        for (int i = 0, j = 0; j < nArgs; i++) {
            if (i == nextDots) {
                for (int k = 0; k < dotsArgLen; k++, j++) {
                    actualArgNames[j] = dotsArgNames[k];
                    actualArgValues[j] = dotsArgValues[k]; // FIXME: GNU-R would create a recursive promise here
                }
                dotsIndex++;
                if (dotsIndex < ndots) {
                    nextDots = dotsIndex;
                }
            } else {
                actualArgNames[j] = argNames[i];
                actualArgValues[j] = promiseForArgument(callerFrame, i);
                j++;
            }
        }

        boolean[] usedArgs = new boolean[nArgs];

        for (int i = 0; i < nArgs; i++) { // matching by name
            RSymbol argName = actualArgNames[i];
            if (argName == null) {
                continue;
            }
            for (int j = 0; j < nParams; j++) {
                if (argName == paramNames[j]) {
                    if (newFrame.get(j) != null) {
                        throw RError.getFormalMatchedMultiple(ast, argName.name());
                    }
                    newFrame.set(j, actualArgValues[i]);
                    usedArgs[i] = true;
                }
            }
        }

        // do we need to do partial matching at all?
        boolean hasUnmatchedNamedArgs = false;
        for (int i = 0; i < nArgs; i++) {
            if (actualArgNames[i] != null && !usedArgs[i]) {
                hasUnmatchedNamedArgs = true;
                break;
            }
        }

        if (hasUnmatchedNamedArgs) { // partial matching
            boolean[] argMatchedViaPartialMatching = new boolean[nArgs];
            for (int j = 0; j < nParams; j++) {
                if (newFrame.get(j) != null) {
                    continue;
                }
                RSymbol paramName = paramNames[j];
                if (paramName == RSymbol.THREE_DOTS_SYMBOL) {
                    // only exact matches after ...
                    // NOTE: GNU-R continues in the search, but I don't see why - exact matching would have established such matches already
                    break;
                }

                for (int i = 0; i < nArgs; i++) {
                    RSymbol argName = actualArgNames[i];
                    if (argName == null) {
                        continue;
                    }
                    if (argMatchedViaPartialMatching[i]) {
                        if (paramName.startsWith(argName)) {
                            throw RError.getArgumentMatchesMultiple(ast, i + 1);
                        }
                    } else if (!usedArgs[i] && paramName.startsWith(argName)) {
                        if (newFrame.get(j) != null) {
                            throw RError.getFormalMatchedMultiple(ast, paramName.name());
                        }
                        newFrame.set(j, actualArgValues[i]);
                        usedArgs[i] = true;
                        argMatchedViaPartialMatching[i] = true;
                    }
                }
            }
        }

        int i = 0; // positional matching
        int j = 0;
        boolean hasUnusedArgsWithNames = false;

        outer: for(;;) {
            for(;;) {
                if (i == nArgs) {
                    if (hasUnusedArgsWithNames) {
                        reportUnusedArgsError(usedArgs, actualArgValues, actualArgNames);
                    }
                    break outer;
                }
                if (!usedArgs[i]) {
                    break;
                }
                i++;
            }
            // i now points to unused argument

            for(;;) {
                if (j == nParams) {
                    reportUnusedArgsError(usedArgs, actualArgValues, actualArgNames);
                }
                if (newFrame.get(j) == null) {
                    break;
                }
                j++;
            }
            // j now points to unmatched parameter

            RSymbol paramName = paramNames[j];
            if (paramName == RSymbol.THREE_DOTS_SYMBOL) { // handle three dots in parameters

                int nToDots = 0;
                for(int ii = i; ii < nArgs; ii++) {
                    if (!usedArgs[ii]) {
                        nToDots++;
                    }
                }

                RSymbol[] dnames = new RSymbol[nToDots];
                Object[] dvalues = new Object[nToDots];

                for (int di = 0; i < nArgs; i++) {
                    if (!usedArgs[i]) {
                        dnames[di] = actualArgNames[i];
                        dvalues[di] = actualArgValues[i];
                        di++;
                        usedArgs[i] = true;
                    }
                }

                newFrame.set(j, new RDots(dnames, dvalues));
                continue;
            }
            // j now points to unmatched parameter, which is not the three dots

            if (actualArgNames[i] == null) {
                newFrame.set(j, actualArgValues[i]);
                usedArgs[i] = true;
                i++;
                j++;
            } else {
                i++;
                hasUnusedArgsWithNames = true;
            }
        }
    }

    private int reportUnusedArgsError(boolean[] usedArgs, Object[] actualArgValues, RSymbol[] actualArgNames) {
        StringBuilder str = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < usedArgs.length; i++) {
            if (!usedArgs[i]) {
                if (!first) {
                    str.append(", ");
                } else {
                    first = false;
                }
                RSymbol argName = actualArgNames[i];
                if (argName != null) {
                    str.append(argName);
                    str.append(" = ");
                }
                Object argValue = actualArgValues[i];
                if (argValue != null) {
                    RNode argExpr;
                    if (argValue instanceof RPromise) {
                        argExpr = ((RPromise) argValue).expression();
                        str.append(PrettyPrinter.prettyPrint(argExpr.getAST()));
                    } else if (argValue instanceof RAny) {
                        str.append( ((RAny) argValue).pretty());
                    }
                }
            }
        }
        throw RError.getUnusedArgument(ast, str.toString());
    }

    public static class DotsInfo {
        RSymbol[] names; // names of arguments to be passed into ... parameter
    }

}