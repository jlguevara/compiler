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
