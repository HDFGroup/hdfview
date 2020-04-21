/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see https://support.hdfgroup.org/products/licenses.html               *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object.nc2;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import hdf.object.FileFormat;
import hdf.object.Group;
import ucar.nc2.NetcdfFile;

/**
 * An NC2Group represents NetCDF3 group, inheriting from Group. Every NetCDF3 object
 * has at least one name. An NetCDF3 group is used to store a set of the names
 * together in one place, i.e. a group. The general structure of a group is
 * similar to that of the UNIX file system in that the group may contain
 * references to other groups or data objects just as the UNIX directory may
 * contain subdirectories or files.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public class NC2Group extends Group {
    private static final long serialVersionUID = -1261533010442193447L;

    private static final org.slf4j.Logger   log = org.slf4j.LoggerFactory.getLogger(NC2Group.class);

    /**
     * The corresponding netcdf Group for this group.
     */
    protected ucar.nc2.Group netCDFGroup;

    public ucar.nc2.Group getNetCDFGroup() {
        return netCDFGroup;
    }

    public void setNetCDFGroup(ucar.nc2.Group netCDFGroup) {
        this.netCDFGroup = netCDFGroup;
    }

    /**
     * The list of attributes of this data object. Members of the list are
     * instance of Attribute.
     */
    private List attributeList;

    /**
     * The list of netcdf typedefs, dimensions and attributes of this data object. Members of the list are
     * instance of ucar.nc2.*.
     */
    private List netcdfTypedefList;
    private List netcdfDimensionList;
    private List netcdfAttributeList;

    /** The default object ID for NC2 objects */
    private static final long[] DEFAULT_OID = { 0 };

    /**
     * Constructs an NC2 group with specific name, path, and parent.
     *
     * @param fileFormat
     *            the file which containing the group.
     * @param name
     *            the name of this group.
     * @param path
     *            the full path of this group.
     * @param parent
     *            the parent of this group.
     * @param oid
     *            the unique identifier of this data object.
     */
    public NC2Group(FileFormat fileFormat, String name, String path, Group parent, long[] theID) {
        super(fileFormat, name, path, parent, ((theID == null) ? DEFAULT_OID : theID));
        ucar.nc2.Group parentGroup = null;
        if (parent != null)
            parentGroup = ((NC2Group)parent).getNetCDFGroup();
        netCDFGroup = new ucar.nc2.Group(((NC2File)fileFormat).getNetcdfFile(), parentGroup, name);
        log.trace("NC2Group:{}", name);
    }

    /*
     * (non-Javadoc)
     *
     * @see hdf.object.DataFormat#hasAttribute()
     */
    public boolean hasAttribute() {
        return false;
    }

    // Implementing DataFormat
    @SuppressWarnings("rawtypes")
    public List getMetadata() throws Exception {
        log.trace("getMetadata(): start");

        if (attributeList != null) {
            log.trace("getMetadata(): attributeList exists -finish");
            return attributeList;
        }

        NC2File theFile = (NC2File)getFileFormat();
        NetcdfFile ncFile = theFile.getNetcdfFile();
        if (!isRoot() && (netCDFGroup !=null)) {
            netcdfDimensionList = netCDFGroup.getDimensions();

            netcdfTypedefList = netCDFGroup.getEnumTypedefs();

            netcdfAttributeList = netCDFGroup.getAttributes();
        }
        else {
            netcdfDimensionList = ncFile.getDimensions();

            netcdfAttributeList = ncFile.getGlobalAttributes();
        }
        if (netcdfAttributeList == null) {
            attributeList = null;
        }
        else {
            int n = netcdfAttributeList.size();
            log.trace("Attribute size:{}", n);
            attributeList = new Vector(n);

            ucar.nc2.Attribute netcdfAttr = null;
            for (int i = 0; i < n; i++) {
                netcdfAttr = (ucar.nc2.Attribute) netcdfAttributeList.get(i);
                log.trace("getMetadata(): Attribute[{}]:{}", i, netcdfAttr.toString());
                attributeList.add(NC2File.convertAttribute(this, netcdfAttr));
            }
        }
        log.trace("getMetadata(): finish");
        return attributeList;
    }

    /**
     * Creates a new attribute and attached to this dataset if attribute does
     * not exist. Otherwise, just update the value of the attribute.
     *
     * @param info
     *            the atribute to attach
     *
* @throws Exception
     *            if there is an error
     */
    public void writeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException(
                "Unsupported operation for NetCDF.");
    }

    /**
     * Deletes an attribute from this dataset.
     *
     * @param info
     *            the attribute to delete.
     *
* @throws Exception
     *            if there is an error
     */
    public void removeMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException(
                "Unsupported operation for NetCDF.");
    }

    // implementing DataFormat
    public void updateMetadata(Object info) throws Exception {
        // not supported
        throw new UnsupportedOperationException("Unsupported operation for NetCDF.");
    }

    // Implementing DataFormat
    @Override
    public long open() {
        // not supported
        throw new UnsupportedOperationException(
                "Unsupported operation for NetCDF.");
    }

    /** close group access */
    @Override
    public void close(long gid) {
        // not supported
        throw new UnsupportedOperationException(
                "Unsupported operation for NetCDF.");
    }

    /**
     * Creates a new group.
     *
     * @param file
     *            the file which the group is added to.
     * @param name
     *            the name of the group to create.
     * @param pgroup
     *            the parent group of the new group.
     *
     * @return the new group if successful. Otherwise returns null.
     *
* @throws Exception
     *            if there is an error
     */
    public static NC2Group create(String name, Group pgroup) throws Exception {
        // not supported
        throw new UnsupportedOperationException(
                "Unsupported operation for NetCDF.");
    }

    // overload to get different lists
    @SuppressWarnings("rawtypes")
    public List getMetadata(int... attrPropList) throws Exception {
        log.trace("getMetadata(...): start");
        int hdfType = 0;
        int attrType = 0;
        int dimType = 0;
        int enumType = 0;
        List returnList = null;

        // use 0 to skip or 1 select in attrPropList to get the list
        // hdf attributes first netcdf attributes second, dimensions third, enumTypes fourth
        log.trace("getMetadata(...): attrPropList={}", attrPropList.length);
        if (attrPropList.length > 0) {
            hdfType = attrPropList[0];
        }
        if (attrPropList.length > 1) {
            attrType = attrPropList[1];
        }
        if (attrPropList.length > 2) {
            dimType = attrPropList[2];
        }
        if (attrPropList.length > 3) {
            enumType = attrPropList[3];
        }
        if ((hdfType != 0) && (attributeList != null)) {
            log.trace("getMetadata(): get attributeList");

            returnList = attributeList;
        }
        else if ((attrType != 0) && (netcdfAttributeList != null)) {
            log.trace("getMetadata(): get netcdfAttributeList");

            returnList = netcdfAttributeList;
        }
        else if ((dimType != 0) && (netcdfDimensionList != null)) {
            log.trace("getMetadata(): get netcdfDimensionList");

            returnList = netcdfDimensionList;
        }
        else if ((enumType != 0) && (netcdfTypedefList != null)) {
            log.trace("getMetadata(): get netcdfTypedefList");

            returnList = netcdfTypedefList;
        }

        log.trace("getMetadata(...): finish");
        return returnList;
    }

    public String netcdfAttributeString(int index) {
        ucar.nc2.Attribute netcdfAttr = (ucar.nc2.Attribute) netcdfAttributeList.get(index);
        log.trace("netcdfAttributeString(): netcdfAttribute[{}]:{}", index, netcdfAttr.toString());
        String returnStr = netcdfAttr.toString();
        return returnStr;
    }

    public String netcdfDimensionString(int index) {
        ucar.nc2.Dimension netcdfDim = (ucar.nc2.Dimension) netcdfDimensionList.get(index);
        log.trace("netcdfDimensionString(): netcdfDimension[{}]:{}", index, netcdfDim.toString());
        StringBuilder objDimensionStr = new StringBuilder(netcdfDim.getShortName());
        if (netcdfDim.isShared())
            objDimensionStr.append("[SHARED]");
        if (netcdfDim.isUnlimited())
            objDimensionStr.append(" = UNLIMITED");
          else if (netcdfDim.isVariableLength())
              objDimensionStr.append(" = UNKNOWN");
          else
              objDimensionStr.append(" = " + netcdfDim.getLength());
        return objDimensionStr.toString();
    }

    public String netcdfTypedefString(int index) {
        ucar.nc2.EnumTypedef netcdfType = (ucar.nc2.EnumTypedef) netcdfTypedefList.get(index);
        log.trace("netcdfEnumTypedefString(): netcdfTypedef[{}]:{}", index, netcdfType.toString());
        StringBuilder objEnumTypedefStr = new StringBuilder(netcdfType.getShortName() + " {");
        int count = 0;
        List<Object> keyset = Arrays.asList(netcdfType.getMap().keySet().toArray());
        for (Object key : keyset) {
          String s = netcdfType.getMap().get(key);
          if (0 < count++)
              objEnumTypedefStr.append(", ");
          objEnumTypedefStr.append("'" + s + "' = " + key);
        }
        objEnumTypedefStr.append(" }");
        return objEnumTypedefStr.toString();
    }

}
