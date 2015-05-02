import java.util.*;
import java.util.Map.*;

public class Test {
    public static void main(String[] args) {
        HashMap<String, Integer> map = 
            new HashMap<String, Integer>();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        Set<String> keys = map.keySet();

        for (String k: keys) {
            System.out.println(k);
        }
    }
}
