import java.util.*;

public class BasicBlock {
   private static final String argRegisters[] = 
   {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"}; 
   private String label;
   private List<BasicBlock> incoming = new LinkedList<BasicBlock>();
   private List<BasicBlock> outgoing = new LinkedList<BasicBlock>();
   private List<Instruction> instructions = new LinkedList<Instruction>();
   private BasicBlock loopHeader;

   /* used for entry blocks */
   private int maxArgCount; 
   private boolean isEntryBlock;

   /* used for register allocation */
   private Set<String> genSet, killSet;
   private Set<String> liveout = new HashSet<String>();

   public BasicBlock(String label) {
      this.label = label;
   }

   public void transform() {
      List<Instruction> asm = new LinkedList<Instruction>();

      while (!instructions.isEmpty()) {
         Instruction ins = instructions.remove(0);
         String opcode = ins.getOpcode();
         List<String> operands = ins.getOperands();
         String source, target, offset, left, right;

         if (opcode.equals("loadinargument")) {
            source = operands.get(1);
            target = operands.get(2);

            String srcAddress = getAddressOfArg(source);
            asm.add(new Instruction("movq", srcAddress, target)); 
         } 

         else if (opcode.equals("loadret")) {
            target = operands.get(0);
            source = "%rax";
            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("storeoutargument")) {
            source = operands.get(0);
            target = getAddressToStoreArg(operands.get(1));
            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("call")) {
            target = operands.get(0);
            asm.add(new Instruction("call", target));
         }

         else if (opcode.equals("new")) {
            target = operands.get(2);
            handleMalloc(asm, operands.get(1), target);
         }

         else if (opcode.equals("del")) {
            source = operands.get(0);
            asm.add(new Instruction("movq", source, "%rdi"));
            asm.add(new Instruction("call", "free"));
         }

         else if (opcode.equals("mov")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("moveq")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("cmove", source, target));
         }

         else if (opcode.equals("movge")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("cmovge", source, target));
         }

         else if (opcode.equals("movgt")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("cmovg", source, target));
         }

         else if (opcode.equals("movle")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("cmovle", source, target));
         }

         else if (opcode.equals("movlt")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("cmovl", source, target));
         }

         else if (opcode.equals("movne")) {
            source = operands.get(0);
            target = operands.get(1);

            asm.add(new Instruction("cmovne", source, target));
         }

         else if (opcode.equals("storeai")) {
            source = operands.get(0);
            String base = operands.get(1);
            offset = "$" + operands.get(2);
            target = offset + "(" + base + ")";

            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("loadi")) {
            source = "$" + operands.get(0);
            target = operands.get(1);
            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("loadai")) {
            String base = operands.get(0);
            offset = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", "$" + offset + "(" + base + ")",
                     target));
         }

         else if (opcode.equals("loadglobal")) {
            source = operands.get(0);
            target = operands.get(1);
            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("storeglobal")) {
            source = operands.get(0);
            target = "$" + operands.get(1);
            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("comp")) {
            left = operands.get(0);
            right = operands.get(1);

            /* in reverse order because intel is weird */
            asm.add(new Instruction("cmpq", right, left));
         }

         else if (opcode.equals("cbreq")) {
            target = operands.get(1);
            asm.add(new Instruction("je", target));
         }

         else if (opcode.equals("cbrge")) {
            target = operands.get(1);
            asm.add(new Instruction("jge", target));
         }

         else if (opcode.equals("cbrgt")) {
            target = operands.get(1);
            asm.add(new Instruction("jg", target));
         }

         else if (opcode.equals("cbrle")) {
            target = operands.get(1);
            asm.add(new Instruction("jle", target));
         }

         else if (opcode.equals("cbrlt")) {
            target = operands.get(1);
            asm.add(new Instruction("jl", target));
         }

         else if (opcode.equals("cbrne")) {
            target = operands.get(1);
            asm.add(new Instruction("jne", target)); 
         }

         else if (opcode.equals("jumpi")) {
            target = operands.get(0);
            asm.add(new Instruction("jmp", target));
         }

         else if (opcode.equals("add")) {
            left = operands.get(0);
            right = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, target));
            asm.add(new Instruction("addq", right, target));
         }

         else if (opcode.equals("sub")) {
            left = operands.get(0);
            right = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, target));
            asm.add(new Instruction("subq", right, target));
         }

         else if (opcode.equals("mult")) {
            left = operands.get(0);
            right = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, target));
            asm.add(new Instruction("imulq", right, target));
         }

         else if (opcode.equals("div")) {
            left = operands.get(0);
            right = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, "%rax"));
            asm.add(new Instruction("movq", left, "%rdx"));
            asm.add(new Instruction("sarq", "$63", "%rdx"));
            asm.add(new Instruction("idivq", right));
            asm.add(new Instruction("movq", "%rax", target));
         }

         else if (opcode.equals("ret")) {
            asm.add(new Instruction("ret"));
         }

         else if (opcode.equals("storeret")) {
            source = operands.get(0);
            target = "%rax"; 

            asm.add(new Instruction("movq", source, target));
         }

         else if (opcode.equals("print")) {
            source = operands.get(0);
            asm.add(new Instruction("movq", "$.LC0", "%rdi"));
            asm.add(new Instruction("movq", source, "%rsi"));
            asm.add(new Instruction("movq", "$0", "%rax"));
            asm.add(new Instruction("call", "printf"));
         }

         else if (opcode.equals("println")) {
            source = operands.get(0);
            asm.add(new Instruction("movq", "$.LC1", "%rdi"));
            asm.add(new Instruction("movq", source, "%rsi"));
            asm.add(new Instruction("movq", "$0", "%rax"));
            asm.add(new Instruction("call", "printf"));
         }

         else if (opcode.equals("read")) {
            target = source = operands.get(0);
            asm.add(new Instruction("movq", "$.LC0", "%rdi"));
            asm.add(new Instruction("movq", "$readtmp", "%rsi"));
            asm.add(new Instruction("movq", "$0", "%rax"));
            asm.add(new Instruction("call", "scanf"));
            asm.add(new Instruction("movq", "readtmp", target));
         }

         else if (opcode.equals("and")) {
            left = operands.get(0);
            right = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, target));
            asm.add(new Instruction("andq", right, target));
         }

         else if (opcode.equals("or")) {
            left = operands.get(0);
            right = operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, target));
            asm.add(new Instruction("orq", right, target));
         }

         else if (opcode.equals("xori")) {
            left = operands.get(0);
            right = "$" + operands.get(1);
            target = operands.get(2);

            asm.add(new Instruction("movq", left, target));
            asm.add(new Instruction("xorq", right, target));
         }

         else {
            System.out.println("unknown op: " + opcode);
         }
      }
      instructions = asm;
   }

   public void computeGenAndKillSet()   {
      genSet = new HashSet<String>();
      killSet = new HashSet<String>();

      String target = null, offset, left, right, opcode;
      List<String> operands, sources;

      for (Instruction ins : instructions) {
         opcode = ins.getOpcode();
         operands = ins.getOperands();
         sources = new LinkedList<String>();

         // source and/or target may be offsets
         // and global variables
         // and constants (for printf)
         if (opcode.equals("movq")) {
            sources.add(operands.get(0));
            target = operands.get(1);
         } 

         else if (opcode.equals("cmove")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("cmovge")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("cmovg")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("cmovle")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("cmovl")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("cmovne")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("cmpq")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
         }

         else if (opcode.equals("addq")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("subq")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("imulq")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("idiv")) {
            sources.add(operands.get(0));
            sources.add("%rdx");
            sources.add("%rax");
            target = "%rax"; 
         }

         else if (opcode.equals("sarq")) {
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("andq")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("orq")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         else if (opcode.equals("xori")) {
            sources.add(operands.get(0));
            sources.add(operands.get(1));
            target = operands.get(1);
         }

         for (String source : sources) {
            if (!killSet.contains(source))
               genSet.add(source);
         }
         killSet.add(target);
      }
   }

   /* Get liveout of this block.  Returns true if liveout set changed */
   public boolean liveOut() {
      Set<String> tmp, result = new HashSet<String>();

      for (BasicBlock child : outgoing) {
         tmp = new HashSet<String>(child.getLiveOut());
         tmp.removeAll(child.getKillSet());  /* liveout(m) - killset(m) */
         tmp.addAll(child.getGenSet());      /* gen(m) union tmp(m) */
         result.addAll(tmp);
      }
      if (!liveout.containsAll(result)) {
         liveout = result;
         return true;
      }
      return false;
   }

   public Set<String> getGenSet() {
      return genSet;
   }

   public Set<String> getKillSet() {
      return killSet;
   }

   public Set<String> getLiveOut() {
      return liveout;
   }

   // used to store out arguments
   private String getAddressToStoreArg(String argIndex) {
      int index = Integer.parseInt(argIndex);

      if (index < argRegisters.length)
         return argRegisters[index];

      // subtract 6 to get index on the stack
      index -= 6;
      if (index == 0)
         return "%rsp";
      else
         return index + "(%rsp)";
   }

   // used to load in arguments
   private String getAddressOfArg(String param) {
      int index = Integer.parseInt(param);

      if (index < argRegisters.length)
         return argRegisters[index];

      // subtrack 4 to get the correct index on the stack for arg[6] and above
      index -= 4;
      int offset = index * 8;
      return offset + "(%rbp)";
   }

   /* Create instructions for the *new* operator */
   private void handleMalloc(List<Instruction> asm, String elements, 
         String target) {
      int size = 8 * elements.split(",").length;
      asm.add(new Instruction("movq", "" + size, "%rdi"));
      asm.add(new Instruction("call", "malloc"));
      asm.add(new Instruction("movq", "%rax", target));
   }

   public String getLabel() {
      return this.label;
   }

   public int getMaxArgCount() {
      return maxArgCount;
   }

   public void setMaxArgCount(int count) {
      maxArgCount = count;
   }

   public boolean isEntryBlock() {
      return isEntryBlock;
   }

   public void setEntryBlock(boolean flag) {
      isEntryBlock = flag;
   }

   public BasicBlock getLoopHeader() {
      return loopHeader;
   }

   public void setLoopHeader(BasicBlock val) {
      loopHeader = val;
   }

   public void addIncoming(BasicBlock parent) {
      this.incoming.add(parent);
   }

   public void addOutgoing(BasicBlock child) {
      this.outgoing.add(child);
   }

   public void addInstruction(Instruction instruction) {
      this.instructions.add(instruction);
   }

   public List<Instruction> getInstructions() {
      return instructions;
   }

   public List<BasicBlock> getOutgoing() {
      return outgoing;
   }

   public List<BasicBlock> getIncoming() {
      return incoming;
   }

   public String toString() {
      return label;
   }
}
