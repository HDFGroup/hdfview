package test.object.misc;

import hdf.hdf5lib.H5;
import hdf.object.h5.FaplFactory;
import hdf.object.h5.FaplFactoryROS3;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author jake.smith
 */
public class FaplFactoryTest {

    @Before
    public void unregisterAllFaplTypes() {
        for (String key : FaplFactory.getTypes()) {
            FaplFactory.unregisterFaplType(key);
        }
    }

    @After
    public void verifyFaplsClosed() {
        assert(H5.getOpenIDCount() == 0);
    }

    @Test
    public void testCreateDefaultFapl() throws Exception {
        assertEquals(0, H5.getOpenIDCount());

        final long fapl_id = FaplFactory.createDefaultFapl();
        assertEquals(1, H5.getOpenIDCount());
        // TODO: check through H5 that it is of File Access Property List type?

        FaplFactory.closeFapl(fapl_id);
        assertEquals(0, H5.getOpenIDCount());
    }

    @Test
    public void testGetTypesWithNoneRegistered() {
        assertEquals(0, FaplFactory.getTypes().size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetFactoryUnregistered() {
        FaplFactory.getFactory("explosive");
    }

    @Test
    public void testGetFactory() throws Exception {
        /* demonstrate dynamic loading
         * Note, this may make the test brittle
         */
        @SuppressWarnings("rawtypes")
        Class factoryClass = Class.forName("hdf.object.h5.FaplFactoryROS3");
        FaplFactory ff = (FaplFactory) factoryClass.newInstance();
        FaplFactory.registerFaplType("ros3", ff);

        assertTrue(FaplFactory.getFactory("ros3") instanceof FaplFactoryROS3);
    }

    @Test
    public void testGetTypesWithOneRegistered() {
        FaplFactory.registerFaplType("ros3", new FaplFactoryROS3());

        Set<String> keys = FaplFactory.getTypes();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("ros3"));
        assertFalse(keys.contains("explosive"));
    }

    @Test
    public void testRegisterFaplTypeOverwrites() {
        FaplFactory.registerFaplType("kind", new FaplFactoryROS3());
        assertTrue("did not set to ROS3",
                FaplFactory.getFactory("kind") instanceof FaplFactoryROS3);

        FaplFactory.registerFaplType("kind", new FaplFactoryMystery());
        assertTrue("did not reset to Mystery fapl",
                FaplFactory.getFactory("kind") instanceof FaplFactoryMystery);

        assertEquals(1, FaplFactory.getTypes().size());
    }

    @Test
    public void testUnregisterFaplType() {
        FaplFactory.registerFaplType("ros3", new FaplFactoryROS3());
        assertEquals(1, FaplFactory.getTypes().size());
        FaplFactory.unregisterFaplType("ros3");
        assertEquals(0, FaplFactory.getTypes().size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRegisterNullFaplFactory() {
        FaplFactory.registerFaplType("bad", null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnregisterUnregisteredFaplType() {
        FaplFactory.unregisterFaplType("bad");
    }

    /**
     * mock-up class for testing purposes
     */
    private class FaplFactoryMystery extends FaplFactory {
        @Override
        public long createFapl(Map<String, String> template) {
            return -1;
        }
    }
}
