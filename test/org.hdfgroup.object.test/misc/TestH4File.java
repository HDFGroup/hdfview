
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

import hdf.hdflib.HDFConstants;
import hdf.hdflib.HDFException;

import hdf.object.Group;
import hdf.object.HObject;

import hdf.object.h4.H4CompoundAttribute;
import hdf.object.h4.H4File;
import hdf.object.h4.H4GRImage;
import hdf.object.h4.H4Group;
import hdf.object.h4.H4SDS;
import hdf.object.h4.H4Vdata;

/**
 * Test object at the hdf.object package.
 *
 * @version 1.3.0 10/26/2001
 * @author Peter X. Cao
 */
public class TestH4File
{
    /**
     * Test tree structure of the HDF4 file.
     *
     * Tested for regular file: c:\winnt\profiles\xcao\desktop\hdf_files\amortest000171999.hdf Tested with a large file
     * (over 700MB, over 800 datasets) at \\Eirene\sdt\mcgrath\EOS-Data\MODIS\L3\MOD08_E3.A2000337.002.2001037044240.hdf
     * it takes about 5 seconds to retrieve the tree structure through the network. Accessing local file can be a lot of
     * faster.
     */
    private static void testTree(String fileName) {
        H4File h4file = new H4File(fileName, HDFConstants.DFACC_WRITE);

        long t0 = System.currentTimeMillis();

        try {
            h4file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
        long t = System.currentTimeMillis() - t0;
        System.out.println("Time of retrieving the tree is " + t);

        HObject root = h4file.getRootObject();
        if (root != null)
            printNode(root, "    ");

        try {
            h4file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private static void printNode(HObject node, String indent) {
        System.out.println(indent + node);

        int n = ((Group) node).breadthFirstMemberList().size();
        for (int i = 0; i < n; i++)
            printNode(((Group) node).getMember(i), indent + "    ");
    }

    /**
     * Test H4Group.
     */
    @SuppressWarnings("rawtypes")
    private static void testH4Group(String fileName) {
        H4File h4file = new H4File(fileName, HDFConstants.DFACC_WRITE);

        try {
            h4file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h4file.getRootObject();
        H4Group g = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H4Group) {
                    g = (H4Group) node;
                    System.out.println(g);

                    // test H4CompoundDS attributes
                    H4CompoundAttribute attr = null;
                    List info = null;
                    try {
                        info = g.getMetadata();
                    }
                    catch (Exception ex) {
                    }

                    if (info == null)
                        continue;

                    int n = info.size();
                    for (int i = 0; i < n; i++) {
                        attr = (H4CompoundAttribute) info.get(i);
                        System.out.println(attr);
                    }
                } // if (obj instanceof H4Group
            } // while (nodes.hasMoreElements())
        } // if (root != null)

        try {
            h4file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * Test H4SDS.
     */
    @SuppressWarnings("rawtypes")
    private static void testH4SDS(String fileName) {
        H4File h4file = new H4File(fileName, HDFConstants.DFACC_READ);

        try {
            h4file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h4file.getRootObject();
        H4SDS sds = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H4SDS) {
                    sds = (H4SDS) node;
                    System.out.println(sds);

                    // test H4CompoundDS attributes
                    H4CompoundAttribute attr = null;
                    List info = null;
                    try {
                        info = sds.getMetadata();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    int n = 0;
                    if (info != null) {
                        n = info.size();
                        for (int i = 0; i < n; i++) {
                            attr = (H4CompoundAttribute) info.get(i);
                            System.out.println(attr);
                        }
                    }

                    // data
                    Object data = null;
                    try {
                        data = sds.read();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    if ((data != null) && data.getClass().isArray()) {
                        // print out the first 1000 data points
                        n = Math.min(Array.getLength(data), 1000);
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < n; j++) {
                            sb.append(Array.get(data, j));
                            sb.append(" ");
                        }
                        System.out.println(sb.toString());
                    }
                } // if (obj instanceof H4Group
            } // while (nodes.hasMoreElements())
        } // if (root != null)

        try {
            h4file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * Test H4Vdata.
     */
    @SuppressWarnings("rawtypes")
    private static void testH4Vdata(String fileName) {
        H4File h4file = new H4File(fileName, HDFConstants.DFACC_READ);

        try {
            h4file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h4file.getRootObject();
        H4Vdata vdata = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H4Vdata) {
                    vdata = (H4Vdata) node;
                    System.out.println(vdata);

                    // test H4CompoundDS attributes
                    H4CompoundAttribute attr = null;
                    List info = null;
                    try {
                        info = vdata.getMetadata();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    int n = 0;
                    if (info != null) {
                        n = info.size();
                        for (int i = 0; i < n; i++) {
                            attr = (H4CompoundAttribute) info.get(i);
                            System.out.println(attr);
                        }
                    }

                    // compound members
                    if (vdata.isInited())
                        vdata.init();

                    n = vdata.getMemberCount();
                    String[] names = vdata.getMemberNames();
                    for (int i = 0; i < n; i++)
                        System.out.println(names[i]);

                    // compound data
                    List list = null;

                    try {
                        list = (List) vdata.read();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    if (list != null) {
                        n = list.size();
                        Object mdata = null;
                        for (int i = 0; i < n; i++) {
                            mdata = list.get(i);
                            if (mdata.getClass().isArray()) {
                                StringBuilder sb = new StringBuilder();
                                // print out the first 1000 data points
                                int mn = Math.min(Array.getLength(mdata), 1000);
                                for (int j = 0; j < mn; j++) {
                                    sb.append(Array.get(mdata, j));
                                    sb.append(" ");
                                }
                                System.out.println(sb.toString());
                            }
                        } // (int i=0; i<n; i++)
                    } // (list != null)
                } // if (obj instanceof H4Vdata
            } // while (nodes.hasMoreElements())
        } // if (root != null)

        try {
            h4file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * Test H4GRImage.
     */
    @SuppressWarnings("rawtypes")
    private static void testH4GRImage(String fileName) {
        H4File h4file = new H4File(fileName, HDFConstants.DFACC_READ);

        try {
            h4file.open();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }

        HObject root = h4file.getRootObject();
        H4GRImage sds = null;
        HObject node = null;
        if (root != null) {
            Iterator<HObject> nodes = ((Group) root).depthFirstMemberList().iterator();
            while (nodes.hasNext()) {
                node = nodes.next();
                if (node instanceof H4GRImage) {
                    sds = (H4GRImage) node;
                    System.out.println(sds);

                    // test H4CompoundDS attributes
                    H4CompoundAttribute attr = null;
                    List info = null;
                    try {
                        info = sds.getMetadata();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    int n = 0;
                    if (info != null) {
                        n = info.size();
                        for (int i = 0; i < n; i++) {
                            attr = (H4CompoundAttribute) info.get(i);
                            System.out.println(attr);
                        }
                    }

                    // data
                    Object data = null;
                    try {
                        data = sds.read();
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    if ((data != null)
                            && data.getClass().isArray()) {
                        // print out the first 1000 data points
                        n = Math.min(Array.getLength(data), 1000);
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < n; j++) {
                            sb.append(Array.get(data, j));
                            sb.append(" ");
                        }
                        System.out.println(sb.toString());
                    }
                } // if (obj instanceof H4Group
            } // while (nodes.hasMoreElements())
        } // if (root != null)

        try {
            h4file.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void main(String[] argv) {
        int argc = argv.length;

        if (argc <= 0) {
            System.exit(1);
        }

        TestH4File.testTree(argv[0]);
    }

}
