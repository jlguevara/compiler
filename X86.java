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

    private void addFunctionEpilogue() {
        out.append("\tmovq %ebp %esp\n");
        out.append("\tpopq %ebp\n");
        out.append("\tretq");
    }

    private void addFunctionCode(BasicBlock fun) {
       Stack<BasicBlock> stack = new Stack<BasicBlock>();
       HashMap<String, BasicBlock> map = new HashMap<String, BasicBlock>();
       BasicBlock nextBlock;

       stack.push(fun);

       while (!stack.empty()) {
           nextBlock = stack.pop();
           nextBlock.transform();
           map.put(nextBlock.getLabel(), nextBlock);
           addBlockString(nextBlock);
           addChildren(nextBlock, stack, map);
       }
   }

   private void addBlockString(BasicBlock block) {
       List<Instruction> instructions = block.getInstructions();

       if (!block.isEntryBlock())
            out.append(block + ":\n");

       for (Instruction op : instructions)
            out.append("\t" + op + "\n");
   }

   private void addChildren(BasicBlock block, Stack<BasicBlock> stack,
           HashMap<String, BasicBlock> map) {
       boolean flag;
       List<BasicBlock> lst = block.getOutgoing();
       BasicBlock child;

       // have to traverse the list backwards to get the order right
       for (int i = lst.size() - 1; i >= 0; i--) { 
           child = lst.get(i);

           // skip if child has already been visited
           if (map.get(child.getLabel()) != null)
               continue;

           // check that there are no dependencies, being careful about
           // loops: expression -> body -> expression
           flag = true;
           for (BasicBlock parent : child.getIncoming()) {
                if (map.get(parent.getLabel()) == null &&
                        !child.getOutgoing().contains(parent)) {
                    flag = false;
                    break;
                }
           }
           if (flag) 
               stack.push(child);
       }
   }
}

