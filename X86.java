import java.util.*;

public class X86 {
    private List<BasicBlock> funs;
    private String filename;
    private StringBuilder out;
    private HashMap<String, Type> globals;

    public X86(String filename, List<BasicBlock> funs, 
          HashMap<String, Type> globals) {
        this.filename = filename;
        this.funs = funs;
        this.globals = globals;
        out = new StringBuilder();
    }

    public String go() {
        out.append("\t.file \"" + filename + "\"\n");

        for (String key: globals.keySet()) {
           if (!(globals.get(key) instanceof FunType))
              out.append("\t.comm " + key + " 8, 8\n");
        }
        
        // global variable used to read in values
        out.append("\t.comm readtmp 8, 8\n");

        out.append("\t.section\t.rodata\n");
        out.append(".LC0:\n");
        out.append("\t.string \"%ld\"\n");
        out.append(".LC1:\n");
        out.append("\t.string \"%ld\\n\"\n");
      
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

        out.append("\tpushq %rbp\n");
        out.append("\tmovq %rsp, %rbp\n");
        if (argSize > 0) {
            argSize *= 8;
            out.append("\tsubq $" + argSize + ", %rsp\n");
        }
    }

    private void addFunctionEpilogue() {
        out.append("\tmovq %rbp, %rsp\n");
        out.append("\tpopq %rbp\n");
        out.append("\tretq\n");
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

   /* add decendants of basic block */
   private void addChildren(BasicBlock block, Stack<BasicBlock> stack,
           HashMap<String, BasicBlock> map) {
       List<BasicBlock> lst = block.getOutgoing();
       BasicBlock child;
       boolean seenAllParents;

       // have to traverse the list backwards to get the order right
       for (int i = lst.size() - 1; i >= 0; i--) { 
           child = lst.get(i);

           // skip if child has already been visited
           if (map.get(child.getLabel()) != null)
               continue;
           // enforce topological ordering
           seenAllParents = true;
           for (BasicBlock parent : child.getIncoming()) {
               if (map.get(parent.getLabel()) == null) {
                  // check if we are in a loop
                  if (parent.getLoopHeader() == child)
                     continue;
                  seenAllParents = false;
                  break;
               }
           }
           if (!seenAllParents)
              continue;

            stack.push(child);
       }
   }




}

