import java.util.*;

public class Instruction {
    private String opcode;
    private List<String> operands;

    public Instruction(String opcode) 
    {
        this.opcode = opcode; 
        operands = new LinkedList<String>();
    }

    public Instruction(String opcode, String operand1) {
        this(opcode);
        operands.add(operand1);
    }

    public Instruction(String opcode, String operand1, String operand2) {
        this(opcode, operand1);
        operands.add(operand2);
    }

    public Instruction(String opcode, String operand1, String operand2,
            String operand3) {
        this(opcode, operand1, operand2);
        operands.add(operand3);
    }

    public void setOpcode(String val) {
        opcode = val;
    }

    public String getOpcode()
    {
        return opcode;
    }

    public void addOperand(String operand) 
    {
        operands.add(operand);
    }

    public List<String> getOperands() 
    {
        return operands;
    }
   
    public List<String> getSources() {
       List<String> sources = new LinkedList<String>();

       // source and/or target may be offsets
       // and global variables
       // and constants (for printf)
       if (opcode.equals("movq")) {
          sources.add(operands.get(0));
       } 
       else if (opcode.equals("cmove")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("cmovge")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("cmovg")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("cmovle")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("cmovl")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("cmovne")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("cmpq")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("addq")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("subq")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("imulq")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("idiv")) {
          sources.add(operands.get(0));
          sources.add("%rdx");
          sources.add("%rax");
       }
       else if (opcode.equals("sarq")) {
          sources.add(operands.get(1));
       }
       else if (opcode.equals("andq")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("orq")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }
       else if (opcode.equals("xori")) {
          sources.add(operands.get(0));
          sources.add(operands.get(1));
       }

       return sources;
    }
   
    public String getTarget() {
      String target = null; 

         // source and/or target may be offsets
         // and global variables
         // and constants (for printf)
         if (opcode.equals("movq")) {
            target = operands.get(1);
         } 
         else if (opcode.equals("cmove")) {
            target = operands.get(1);
         }
         else if (opcode.equals("cmovge")) {
            target = operands.get(1);
         }
         else if (opcode.equals("cmovg")) {
            target = operands.get(1);
         }
         else if (opcode.equals("cmovle")) {
            target = operands.get(1);
         }
         else if (opcode.equals("cmovl")) {
            target = operands.get(1);
         }
         else if (opcode.equals("cmovne")) {
            target = operands.get(1);
         }
         else if (opcode.equals("cmpq")) {
            /* do nothing */
         }
         else if (opcode.equals("addq")) {
            target = operands.get(1);
         }
         else if (opcode.equals("subq")) {
            target = operands.get(1);
         }
         else if (opcode.equals("imulq")) {
            target = operands.get(1);
         }
         else if (opcode.equals("idiv")) {
            target = "%rax"; 
         }
         else if (opcode.equals("sarq")) {
            target = operands.get(1);
         }
         else if (opcode.equals("andq")) {
            target = operands.get(1);
         }
         else if (opcode.equals("orq")) {
            target = operands.get(1);
         }
         else if (opcode.equals("xori")) {
            target = operands.get(1);
         }
      return target;
   }
    public String toString() 
    {
        String result = opcode;

        for (int i = 0; i < operands.size(); i++) {
            result += " " + operands.get(i);
            if (i != operands.size() - 1)
                result += ",";
        }
        return result;
    }
    
}
