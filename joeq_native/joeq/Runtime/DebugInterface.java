package Run_Time;


/**
 * @author  John Whaley
 * @version $Id$
 */
public abstract class DebugInterface {

    public static void debugwrite(String msg) {
	System.err.println(msg);
	return;
    }
    
    public static void debugwriteln(String msg) {
	System.err.println(msg);
	return;
    }
    
    public static void die(int code) {
        new InternalError().printStackTrace();
	System.exit(code);
    }    
}