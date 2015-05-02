import java.util.*;

public class FunType extends Type {
    public List<Type> params = new LinkedList<Type>();
    public boolean returns = false;
    public Type returnType;

    public int numOfParams() {
        return params.size();
    }
}
