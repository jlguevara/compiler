public class BoolType extends Type {
    private static BoolType instance = new BoolType();

    private BoolType() {
    }

    public static BoolType getInstance() {
        return instance;
    }

    public int size() {
       return 8;
    }
}

