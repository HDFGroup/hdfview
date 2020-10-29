package hdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

    public static String getPropertyVersionJava(){
        return props.getProperty("JAVA_VERSION");
    }

    public static String getPropertyVersionHDF4(){
        return props.getProperty("HDF4_VERSION");
    }

    public static String getPropertyVersionHDF5(){
        return props.getProperty("HDF5_VERSION");
    }

    public static String getPropertyVersionView(){
        return props.getProperty("HDFVIEW_VERSION");
    }
}

