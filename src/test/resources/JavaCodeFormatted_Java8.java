public class Java {
    public static void main(String[] args) {
        System.out.println("hello");
    }

    public void doStuff() throws Exception {
        Function<String, Integer> example = Integer::parseInt;
        example.andThen(val -> {
            return val + 2;
        } );
        SimpleEnum val = SimpleEnum.A;
        switch (val) {
        case A:
            break;
        case B:
            break;
        case C:
            break;
        default:
            throw new Exception();
        }
    }
}