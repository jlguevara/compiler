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
        out.append(".text\n");
        for (BasicBlock f : funs) {
            addFunctionHeader(f);            
            addFunctionCode(f);
        }
        return out.toString();
    }

    private void addFunctionHeader(BasicBlock fun) {
        out.append(".globl " + fun.getLabel() + "\n");
        out.append("\t.type " + fun.getLabel() + ", @function\n");
        out.append("\tpushl %ebp\n");
    }

    private void addFunctionCode(BasicBlock fun) {
    }
}
