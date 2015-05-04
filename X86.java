import java.util.*;

public class X86 {
    private List<BasicBlock> funs;
    private String filename;
    private StringBuilder out;

    public X86(String filename, List<BasicBlock> funs) {
        this.filename = filename;
        this.funs = funs;
        out = new StringBuilder();
    }

    public String go() {
        out.append("\t.file \"" + filename + "\"\n");
        out.append("\t.text\n");
        for (BasicBlock f : funs) {
            addFunctionPrologue(f);            
            addFunctionCode(f);
            addFunctionEpilogue();
        }
        return out.toString();
    }

    private void addFunctionPrologue(BasicBlock fun) {
        int argSize = fun.getMaxArgCount() - 6;

        out.append(".globl " + fun.getLabel() + "\n");
        out.append("\t.type " + fun.getLabel() + ", @function\n");
        out.append(fun.getLabel() + ":\n");

        out.append("\tpushq %ebp\n");
        if (argSize > 0) {
            argSize *= 8;
            out.append("subq $" + argSize + "%esp\n");
        }
    }

    private void addFunctionCode(BasicBlock fun) {
    }

    private void addFunctionEpilogue() {
        out.append("movq %ebp %esp\n");
        out.append("popq %ebp\n");
        out.append("retq");
    }
}
