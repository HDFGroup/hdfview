/**
 *
 */
package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.object.Datatype;
import hdf.object.h5.H5Datatype;

/**
 * @author rsinha
 *
 */
public class DatatypeTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatatypeTest.class);

    private Datatype[] baseTypes = null;
    private int[] classes = { Datatype.CLASS_BITFIELD, Datatype.CLASS_CHAR, Datatype.CLASS_COMPOUND,
            Datatype.CLASS_ENUM, Datatype.CLASS_FLOAT, Datatype.CLASS_INTEGER, Datatype.CLASS_NO_CLASS,
            Datatype.CLASS_OPAQUE, Datatype.CLASS_REFERENCE, Datatype.CLASS_STRING, Datatype.CLASS_VLEN };
    private int[] signs = { Datatype.SIGN_2, Datatype.SIGN_NONE, Datatype.NSGN };
    private int[] orders = { Datatype.ORDER_BE, Datatype.ORDER_LE, Datatype.ORDER_NONE, Datatype.ORDER_VAX };
    private int n_classes = 11;
    private int n_signs = 3;
    private int n_orders = 4;
    private int[] sizes = { 1, 2, 4, 8 };
    private String[] descriptions = { "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield",
            "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield",
            "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield",
            "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield",
            "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield",
            "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield",
            "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield",
            "8-bit bitfield", "16-bit bitfield", "32-bit bitfield", "64-bit bitfield", "8-bit bitfield", "16-bit bitfield",
            "32-bit bitfield", "64-bit bitfield", "8-bit integer", "8-bit unsigned integer", "8-bit integer",
            "8-bit integer", "8-bit unsigned integer", "8-bit integer", "8-bit integer", "8-bit unsigned integer",
            "8-bit integer", "8-bit integer", "8-bit unsigned integer", "8-bit integer", "Compound", "Compound",
            "Compound", "Compound", "Compound", "Compound", "Compound", "Compound", "Compound", "Compound",
            "Compound", "Compound", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit enum (0=1, 1=2)", "16-bit enum (0=1, 1=2)", "32-bit enum (0=1, 1=2)",
            "64-bit enum (0=1, 1=2)", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit floating-point", "16-bit floating-point", "32-bit floating-point",
            "64-bit floating-point", "8-bit integer", "16-bit integer", "32-bit integer", "64-bit integer",
            "8-bit unsigned integer", "16-bit unsigned integer", "32-bit unsigned integer", "64-bit unsigned integer",
            "8-bit integer", "16-bit integer", "32-bit integer", "64-bit integer", "8-bit integer", "16-bit integer",
            "32-bit integer", "64-bit integer", "8-bit unsigned integer", "16-bit unsigned integer",
            "32-bit unsigned integer", "64-bit unsigned integer", "8-bit integer", "16-bit integer", "32-bit integer",
            "64-bit integer", "8-bit integer", "16-bit integer", "32-bit integer", "64-bit integer",
            "8-bit unsigned integer", "16-bit unsigned integer", "32-bit unsigned integer", "64-bit unsigned integer",
            "8-bit integer", "16-bit integer", "32-bit integer", "64-bit integer", "8-bit integer", "16-bit integer",
            "32-bit integer", "64-bit integer", "8-bit unsigned integer", "16-bit unsigned integer",
            "32-bit unsigned integer", "64-bit unsigned integer", "8-bit integer", "16-bit integer", "32-bit integer",
            "64-bit integer", "Unknown", "Unknown", "Unknown", "Unknown", "Unknown", "Unknown", "Unknown", "Unknown",
            "Unknown", "Unknown", "Unknown", "Unknown", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque",
            "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque",
            "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque",
            "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque",
            "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque",
            "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque",
            "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque",
            "8-byte Opaque", "1-byte Opaque", "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "1-byte Opaque",
            "2-byte Opaque", "4-byte Opaque", "8-byte Opaque", "Object reference", "Object reference",
            "Object reference", "Object reference", "Object reference", "Object reference", "Object reference",
            "Object reference", "Object reference", "Object reference", "Object reference", "Object reference",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "String, length = 1, string padding = H5T_STR_NULLTERM",
            "String, length = 2, string padding = H5T_STR_NULLTERM",
            "Variable-length", "Variable-length", "Variable-length", "Variable-length", "Variable-length",
            "Variable-length", "Variable-length", "Variable-length", "Variable-length", "Variable-length",
            "Variable-length", "Variable-length" };

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("DatatypeTest BeforeClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @AfterClass
    public static void checkIDs() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("DatatypeTest AfterClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Before
    public void createArrays() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                log.debug("Before: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        baseTypes = new H5Datatype[n_orders * n_signs * (n_classes + 16)]; // INT, ENUM, BITFIELD, OPAQUE have 4 sizes
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        baseTypes[counter] = new H5Datatype(classes[i], sizes[l], orders[j], signs[k]);
                        assertNotNull(baseTypes[counter]);
                        log.trace("counter={}: sizes={} for ({},{},{}) type of {}", counter, sizes[l], classes[i],
                                orders[j], signs[k], baseTypes[counter].getDescription());
                        counter++;
                    }
                }
            }
        }
    }

    @After
    public void finish() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                log.debug("After: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Test method for {@link hdf.object.Datatype#getDatatypeClass()}.
     * <p>
     * We test for every combination of class, size and possible signs.
     */
    @Test
    public void testGetDatatypeClass() {
        log.debug("testGetDatatypeClass");
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        assertEquals("Class for size " + l + " [" + i + "," + j + "," + k + "]", classes[i],
                                baseTypes[counter++].getDatatypeClass());
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link hdf.object.Datatype#getDatatypeSize()}.
     * <p>
     * We test for every combination of class, size and possible signs.
     */
    @Test
    public void testGetDatatypeSize() {
        log.debug("testGetDatatypeSize");
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        assertEquals("Size for size " + l + " [" + i + "," + j + "," + k + "]", sizes[l],
                                baseTypes[counter++].getDatatypeSize());
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link hdf.object.Datatype#getDatatypeOrder()}.
     * <p>
     * We test for every combination of class, size and possible signs.
     */
    @Test
    public void testGetDatatypeOrder() {
        log.debug("testGetDatatypeOrder");
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        assertEquals("Order for size " + l + " [" + i + "," + j + "," + k + "]", orders[j],
                                baseTypes[counter++].getDatatypeOrder());
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link hdf.object.Datatype#getDatatypeSign()}.
     * <p>
     * We test for every combination of class, size and possible signs.
     */
    @Test
    public void testGetDatatypeSign() {
        log.debug("testGetDatatypeSign");
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        assertEquals("Sign for size " + l + " [" + i + "," + j + "," + k + "]", signs[k],
                                baseTypes[counter++].getDatatypeSign());
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link hdf.object.Datatype#setEnumMembers(java.lang.String)}.
     * <p>
     * create a new enum data type set it to two different values and check it.
     */
    @Test
    public void testSetEnumMembers() {
        log.debug("testSetEnumMembers");
        Datatype ed = new H5Datatype(Datatype.CLASS_ENUM, 2, Datatype.ORDER_NONE, Datatype.NSGN);
        ed.setEnumMembers("high=40, low=20");
        assertEquals("high=40, low=20", ed.getEnumMembersAsString());
    }

    /**
     * Test method for {@link hdf.object.Datatype#getEnumMembers()}.
     * <p>
     * look at {@link hdf.object.Datatype#setEnumMembers(java.lang.String)}.
     */
    @Test
    public void testGetEnumMembers() {
        log.debug("testGetEnumMembers");
        testSetEnumMembers();
    }

    /**
     * Test method for {@link hdf.object.Datatype#getDescription()} . RISHI SINHA - THE METHOD CALLED IS
     * ONE FOR H5 WHICH OVERRIDES THE BASE CALL.
     * <p>
     * We test for every combination of class, size and possible signs.
     */
    @Test
    public void testGetDatatypeDescription() {
        log.debug("testGetDatatypeDescription");
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        assertEquals(counter + ": Description for size " + l + " [" + i + "," + j + "," + k + "]",
                                descriptions[counter], baseTypes[counter].getDescription());
                        counter++;
                    }
                }
            }
        }
    }

    /**
     * ABSTRACT METHOD Test method for {@link hdf.object.Datatype#isUnsigned()}.
     * <p>
     * We test for every combination of class, size and possible signs.
     */
    @Test
    public void testIsUnsigned() {
        log.debug("testIsUnsigned");
        int counter = 0;
        for (int i = 0; i < n_classes; i++) {
            for (int j = 0; j < n_orders; j++) {
                for (int k = 0; k < n_signs; k++) {
                    int n_sizes;
                    switch (classes[i]) {
                        case Datatype.CLASS_INTEGER:
                        case Datatype.CLASS_ENUM:
                        case Datatype.CLASS_BITFIELD:
                        case Datatype.CLASS_OPAQUE:
                        case Datatype.CLASS_FLOAT:
                            n_sizes = 4;
                            break;
                        case Datatype.CLASS_STRING:
                            n_sizes = 2;
                            break;
                        default:
                            n_sizes = 1;
                            break;
                    }
                    for (int l = 0; l < n_sizes; l++) {
                        boolean isSigned = baseTypes[counter++].isUnsigned();
                        if (isSigned && (signs[k] != Datatype.SIGN_NONE)) {
                            fail("isUnsigned Failed for size " + l + " [" + i + "," + j + "," + k + "].");
                        }
                    }
                }
            }
        }
    }
}
