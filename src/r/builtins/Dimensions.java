package r.builtins;

import com.oracle.truffle.api.frame.*;

import r.builtins.BuiltIn.NamedArgsBuiltIn.*;
import r.data.*;
import r.nodes.*;
import r.nodes.truffle.*;


public class Dimensions {

    private static final String[] paramNames = new String[]{"x"};
    private static final int IX = 0;

    public abstract static class Operation {
        public abstract RInt extract(int[] dimensions);
    }

    public static final class DimensionsCallFactory extends CallFactory {
        final Operation op;

        public DimensionsCallFactory(Operation op) {
            this.op = op;
        }

        @Override
        public RNode create(ASTNode call, RSymbol[] names, RNode[] exprs) {

            AnalyzedArguments a = BuiltIn.NamedArgsBuiltIn.analyzeArguments(names, exprs, paramNames);
            final boolean[] provided = a.providedParams;

            if (!provided[IX]) {
                BuiltIn.missingArg(call, paramNames[IX]);
            }

            return new BuiltIn.BuiltIn1(call, names, exprs) {

                @Override
                public RAny doBuiltIn(Frame frame, RAny x) {
                    if (!(x instanceof RArray)) {
                        return RNull.getNull();
                    }
                    RArray ax = (RArray) x;
                    int[] dim = ax.dimensions();
                    if (dim == null) {
                        return RNull.getNull();
                    }
                    return op.extract(dim);
                }
            };
        }
    }

    // needed because dim is an attribute (for attr, attributes)
    // TODO: add a replacement version
    public static RAny getDim(RAny arg) {
        if (!(arg instanceof RArray)) {
            return RNull.getNull();
        }
        RArray arr = (RArray) arg;
        int[] dim = arr.dimensions();
        if (dim == null) {
            return RNull.getNull();
        }
        return RInt.RIntFactory.getArray(dim);
    }

    public static final CallFactory DIM_FACTORY = new DimensionsCallFactory(
                    new Operation() {
                        @Override
                        public RInt extract(int[] dimensions) {
                            return RInt.RIntFactory.getArray(dimensions);
                        }
                    }
    );

    public static final CallFactory NROW_FACTORY = new DimensionsCallFactory(
                    new Operation() {
                        @Override
                        public RInt extract(int[] dimensions) {
                            if (dimensions.length > 0) {
                                return RInt.RIntFactory.getScalar(dimensions[0]);
                            } else {
                                return RInt.BOXED_NA;
                            }
                        }
                    }
    );

    public static final CallFactory NCOL_FACTORY = new DimensionsCallFactory(
                    new Operation() {
                        @Override
                        public RInt extract(int[] dimensions) {
                            if (dimensions.length > 1) {
                                return RInt.RIntFactory.getScalar(dimensions[1]);
                            } else {
                                return RInt.BOXED_NA;
                            }
                        }
                    }
    );

 }
