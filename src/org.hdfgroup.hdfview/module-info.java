/** the hdfview module */
module org.hdfgroup.hdfview {
    exports hdf;
    exports hdf.view;
    exports hdf.view.TreeView;
    exports hdf.view.TableView;
    exports hdf.view.DataView;
    exports hdf.view.PaletteView;
    exports hdf.view.ImageView;
    exports hdf.view.dialog;
    exports hdf.view.HelpView;
    exports hdf.view.MetaDataView;

    requires org.hdfgroup.object;
    requires java.datatransfer;
    requires java.desktop;
    requires java.management;
    requires java.prefs;
    requires java.sql;
    requires java.logging;
    requires java.xml;
    requires org.slf4j;
    requires org.eclipse.core.commands;
    requires org.eclipse.jface;
    requires org.eclipse.nebula.widgets.nattable.core;
}
