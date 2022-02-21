
/****************************************************************************
 * NCSA HDF                                                                 *
 * National Comptational Science Alliance                                   *
 * University of Illinois at Urbana-Champaign                               *
 * 605 E. Springfield, Champaign IL 61820                                   *
 *                                                                          *
 * For conditions of distribution and use, see the accompanying             *
 * hdf-java/COPYING file.                                                   *
 *                                                                          *
 ****************************************************************************/

package misc;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;

import hdf.hdf5lib.HDF5Constants;

import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5CompoundDS;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;
import hdf.object.h5.H5ScalarAttr;
import hdf.object.h5.H5ScalarDS;

/**
 * Test object at the hdf.object package.
 *
 * @version 1.3.0 10/26/2001
 * @author Peter X. Cao
 *
 */
public class TestH5File
{
    /**
     * Test tree structure of the HDF5 file.
     *
     * Tested with a large file (over 700MB, over 800 datasets) at
     * \\Eirene\sdt\mcgrath\EOS-Data\MODIS\L3\MOD08_E3.A2000337.002.2001037044240.h5
     * it takes about 5 seconds to retrieve the tree structure through the network.
     * Accessing local file can be a lot of faster.
     */
    private static void testTree(String fileName) {
        H5File h5file = new H5File(fileName, HDF5Constants.H5F_ACC_RDONLY);

        long t0 = System.currentTimeMillis();

        try {
            h5file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        long t = System.currentTimeMillis()-t0;
        System.out.println("Time of retrieving the tree is "+t);

        HObject root = h5file.getRootObject();
        if (root != null)
            printNode(root, "    ");

        try {
            h5file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private static void printNode(HObject node, String indent) {
        System.out.println(indent+node);

        int n = ((Group) node).breadthFirstMemberList().size();
        for (int i=0; i<n; i++)
            printNode(((Group) node).getMember(i), indent+"    ");
    }

    /**
     * Test H5CompoundDS.
     */
    @SuppressWarnings("rawtypes")
    private static void testH5CompoundDS(String fileName) {
        H5File h5file = new H5File(fileName, HDF5Constants.H5F_ACC_RDONLY);

        try {
            h5file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h5file.getRootObject();
        H5CompoundDS h5DS = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H5CompoundDS) {
                    h5DS = (H5CompoundDS) node;
                    System.out.println(h5DS);

                    // test H5CompoundDS attributes
                    H5ScalarAttr attr = null;
                    List info = null;
                    try {
                        info = h5DS.getMetadata();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    int n = 0;
                    if (info != null) {
                        n = info.size();
                        for (int i=0; i<n; i++) {
                            attr = (H5ScalarAttr)info.get(i);
                            System.out.println(attr);
                        }
                    }

                    // compound members
                    if (!h5DS.isInited())
                        h5DS.init();

                    n = h5DS.getMemberCount();
                    String[] names = h5DS.getMemberNames();
                    for (int i=0; i<n; i++)
                        System.out.println(names[i]);

                    // compound data
                    List list = null;

                    try {
                        list = (List)h5DS.read();
                    }
                    catch (Exception ex)  {
                        System.out.println(ex);
                    }

                    if (list != null) {
                        n = list.size();
                        Object mdata = null;
                        for (int i=0; i<n; i++) {
                            mdata = list.get(i);
                            if (mdata.getClass().isArray()) {
                                StringBuilder sb = new StringBuilder();
                                // print out the first 1000 data points
                                int mn = Math.min(Array.getLength(mdata), 1000);
                                for (int j=0; j<mn; j++) {
                                    sb.append(Array.get(mdata, j));
                                    sb.append(" ");
                                }
                                System.out.println(sb.toString());
                            }
                        } //  (int i=0; i<n; i++)
                    } //  (list != null)
                } //if (obj instanceof H5CompoundDS
            } //while (nodes.hasMoreElements())
        } //if (root != null)

        try {
            h5file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * Test H5ScalarDS.
     */
    @SuppressWarnings("rawtypes")
    private static void testH5ScalarDS(String fileName) {
        H5File h5file = new H5File(fileName, HDF5Constants.H5F_ACC_RDONLY);

        try {
            h5file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h5file.getRootObject();
        H5ScalarDS h5DS = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H5ScalarDS) {
                    h5DS = (H5ScalarDS) node;
                    System.out.println(h5DS);

                    // test H5CompoundDS attributes
                    H5ScalarAttr attr = null;
                    List info = null;

                    try {
                        info = h5DS.getMetadata();
                    }
                    catch (Exception ex)  {
                        System.out.println(ex);
                    }

                    int n = 0;
                    if (info != null) {
                        n = info.size();
                        for (int i=0; i<n; i++) {
                            attr = (H5ScalarAttr)info.get(i);
                            System.out.println(attr);
                        }
                    }

                    // data
                    Object data = null;
                    try {
                        data = h5DS.read();
                    }
                    catch (Exception ex) {}

                    if ((data != null) && data.getClass().isArray()) {
                        // print out the first 1000 data points
                        n = Math.min(Array.getLength(data), 1000);
                        StringBuilder sb = new StringBuilder();
                        for (int j=0; j<n; j++) {
                            sb.append(Array.get(data, j));
                            sb.append(" ");
                        }
                        System.out.println(sb.toString());
                    }
                } //if (obj instanceof H5CompoundDS
            } //while (nodes.hasMoreElements())
        } //if (root != null)

        try {
            h5file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * Test H5Group.
     */
    @SuppressWarnings("rawtypes")
    private static void testH5Group(String fileName) {
        H5File h5file = new H5File(fileName, HDF5Constants.H5F_ACC_RDONLY);

        try {
            h5file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h5file.getRootObject();
        H5Group g = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H5Group) {
                    g = (H5Group) node;
                    System.out.println(g);

                    // test H5CompoundDS attributes
                    H5ScalarAttr attr = null;
                    List info = null;
                    try {
                        g.getMetadata();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    if (info == null)
                        continue;

                    int n = info.size();
                    for (int i=0; i<n; i++) {
                        attr = (H5ScalarAttr)info.get(i);
                        System.out.println(attr);
                    }
                } //if (obj instanceof H5Group
            } //while (nodes.hasMoreElements())
        } //if (root != null)

        try {
            h5file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void main(String[] argv) {
        int argc = argv.length;

        if (argc <=0)
            System.exit(1);

        System.out.println("Tree: for: "+argv[0]);
        TestH5File.testTree(argv[0]);
    }

}
