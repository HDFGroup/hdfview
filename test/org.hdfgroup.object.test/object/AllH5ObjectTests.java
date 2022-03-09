/**
 * 
 */
package object;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
// hdf.object.h5 package
    H5CompoundDSTest.class, 
    H5BugFixTest.class, 
    H5ScalarDSTest.class, 
    H5GroupTest.class, 
    H5DatatypeTest.class, 
    H5FileTest.class,

// hdf.object package
    CompoundDSTest.class, 
    DatasetTest.class, 
    ScalarDSTest.class, 
    AttributeTest.class, 
    DatatypeTest.class, 
    FileFormatTest.class, 
    GroupTest.class, 
    HObjectTest.class
})

public class AllH5ObjectTests {
}
