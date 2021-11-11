package hdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** a class to track the current versions of java, hdf4, hdf5 and hdfview */
public class HDFVersions {
    private static Properties props;

    static {
        InputStream inst = null;
        props = new Properties();
        try {
            inst = HDFVersions.class.getResourceAsStream("/versions.properties");
            props.load(inst);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** @return the property to track the current versions of java */
    public static String getPropertyVersionJava(){
        return props.getProperty("JAVA_VERSION");
    }

    /** @return the property to track the current versions of hdf4 */
    public static String getPropertyVersionHDF4(){
        return props.getProperty("HDF4_VERSION");
    }

    /** @return the property to track the current versions of hdf5 */
    public static String getPropertyVersionHDF5(){
        return props.getProperty("HDF5_VERSION");
    }

    /** @return the property to track the current versions of hdfview */
    public static String getPropertyVersionView(){
        return props.getProperty("HDFVIEW_VERSION");
    }
}

