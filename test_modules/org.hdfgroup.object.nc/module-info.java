/** the hdf object netcdf module */
module org.hdfgroup.object.nc {
    exports hdf.object.nc2;

    requires netcdf;
    requires org.hdfgroup.object;
    requires org.slf4j;
}