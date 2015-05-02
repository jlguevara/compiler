import java.util.*;

public class StructType extends Type {
    public HashMap<String, Type> members = new LinkedHashMap<String, Type>();

    public List<String> memberList() {
        return new ArrayList<String>(members.keySet());
    }
}
