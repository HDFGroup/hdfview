/** the hdf object module */
module org.hdfgroup.object {
    exports hdf.object;
    exports hdf.object.fits;
    exports hdf.object.h4;
    exports hdf.object.h5;
    exports hdf.object.nc2;

    requires netcdf;
    requires fits;
    requires jarhdf;
    requires jarhdf5;
    requires org.slf4j;
}
