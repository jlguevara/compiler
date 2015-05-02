import java.util.*;

public class BasicBlock {
    private static final String argRegisters[] = 
                                {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"}; 
    private String label;
    private List<BasicBlock> incoming = new LinkedList<BasicBlock>();
    private List<BasicBlock> outgoing = new LinkedList<BasicBlock>();
    private List<Instruction> instructions = new LinkedList<Instruction>();

    public BasicBlock(String label) {
        this.label = label;
    }

    public void transform(HashMap<String, String> table) {
        List<Instruction> asm = new LinkedList<Instruction>();

        while (!instructions.isEmpty()) {
            Instruction ins = instructions.remove(0);
            String opcode = ins.getOpcode();
            List<String> operands = ins.getOperands();
            String source, target, offset;

            if (opcode.equals("loadinargument")) {
                source = operands.get(1);
                target = operands.get(2);

                String srcAddress = getAddressOfArg(source);
                asm.add(new Instruction("movq", srcAddress, target)); 
            } 

            else if (opcode.equals("storeoutargument")) {
                source = operands.get(0);
                target = getAddressToStoreArg(operands.get(1));
                asm.add(new Instruction("movq", source, target));
            }

            else if (opcode.equals("call")) {
                target = operands.get(0);
                asm.add(new Instruction("callq", target));
            }

            else if (opcode.equals("new")) {
                target = operands.get(2);
                handleMalloc(asm, operands.get(1), target);
            }

            else if (opcode.equals("del")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("mov")) {
                source = operands.get(0);
                target = operands.get(1);

                asm.add(new Instruction("movq", source, target));
            }

            else if (opcode.equals("storeai")) {
                source = operands.get(0);
                String base = operands.get(1);
                offset = operands.get(2);
                target = offset + "(" + base + ")";

                asm.add(new Instruction("movq", source, target));
            }

            else if (opcode.equals("loadai")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("loadi")) {
                source = "$" + operands.get(0);
                target = operands.get(1);
                asm.add(new Instruction("movq", source, target));
            }

            else if (opcode.equals("loadglobal")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("storeglobal")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("comp")) {
                String x = operands.get(0);
                String y = operands.get(1);
                asm.add(new Instruction("cmpq", x, y));
            }

            else if (opcode.equals("cbrne")) {
                target = operands.get(1);
                asm.add(new Instruction("jne", target)); 
            }

            else if (opcode.equals("cbrgt")) {
                target = operands.get(1);
                asm.add(new Instruction("jg", target));
            }

            else if (opcode.equals("cbrle")) {
                target = operands.get(1);
                asm.add(new Instruction("jle", target));
            }

            else if (opcode.equals("jumpi")) {
                target = operands.get(0);
                asm.add(new Instruction("jmp", target));
            }

            else if (opcode.equals("sub")) {
                String left = operands.get(0);
                String right = operands.get(1);
                target = operands.get(2);

                asm.add(new Instruction("movq", left, target));
                asm.add(new Instruction("subq", right, target));
            }

            else if (opcode.equals("mult")) {
                String left = operands.get(0);
                String right = operands.get(1);
                target = operands.get(2);

                asm.add(new Instruction("movq", left, target));
                asm.add(new Instruction("imulq", right, target));
                
            }

            else if (opcode.equals("div")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("ret")) {
                // do nothing
            }
            
            else if (opcode.equals("storeret")) {
                source = operands.get(0);
                target = "%rax"; 

                asm.add(new Instruction("movq", source, target));
            }

            else if (opcode.equals("print")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("println")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else if (opcode.equals("read")) {
                System.out.println("DIV");
                System.exit(1);
            }

            else {
                System.out.println("unknown op: " + opcode);
                System.exit(1);
            }
        }
        instructions = asm;
    }

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
        asm.add(new Instruction("callq", "malloc"));
        asm.add(new Instruction("movq", "%rax", target));
    }

    public String getLabel() {
        return this.label;
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

    public String toString() {
        return label;
    }
}
