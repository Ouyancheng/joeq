/*
 * System.java
 *
 * Created on January 29, 2001, 10:26 AM
 *
 * @author  John Whaley
 * @version 
 */

package ClassLib.sun13_linux.java.lang;
import Bootstrap.PrimordialClassLoader;

public abstract class System {
    
    private static java.util.Properties initProperties(java.util.Properties props) {
        props.setProperty("java.version", "1.3.1_01");
        props.setProperty("java.vendor", "joeq");
        props.setProperty("java.vendor.url", "http://www.joewhaley.com");
        props.setProperty("java.class.version", "47.0");
        
        // TODO: read these properties from environment.
        props.setProperty("java.home", "/usr/java/jdk1.3.1_01/jre");
        props.setProperty("os.name", "Linux");
        props.setProperty("os.arch", "i386");
        props.setProperty("os.version", "2.4.9-31");
        props.setProperty("file.separator", "/");
        props.setProperty("path.separator", ":");
        props.setProperty("line.separator", "\n");
        props.setProperty("user.name", "jwhaley");
        props.setProperty("user.home", "/u/jwhaley");
        props.setProperty("user.dir", "/u/jwhaley/joeq");
        props.setProperty("java.class.path", PrimordialClassLoader.loader.classpathToString());
        return props;
    }

}
