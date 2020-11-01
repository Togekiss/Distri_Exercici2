public class main {
    public static void main(String[] args) {
        LWA lwa = new LWA(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
        lwa.start();
    }
}