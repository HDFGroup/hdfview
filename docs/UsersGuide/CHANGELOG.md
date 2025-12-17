HDFView version 4.3.0

# 🔺 HDFView Changelog
All notable changes to this project will be documented in this file. This document describes the differences between this release and the previous HDFView release, platforms tested, and known problems in this release.

# 🔗 Quick Links
* [HDFView releases](https://github.com/HDFGroup/hdfview/releases)
* [HDFView source](https://github.com/HDFGroup/hdfview)
* [Getting help, questions, or comments](https://github.com/HDFGroup/hdfview/issues)

## 📖 Contents
* [Executive Summary](#-executive-summary-hdfview-version-430)
* [New Features & Improvements](#-new-features--improvements)
* [Bug Fixes](#-bug-fixes)
* [Platforms Tested](#%EF%B8%8F-platforms-tested)
* [Known Problems](#-known-problems)

# 🔆 Executive Summary: HDFView Version 4.3.0

## Enhanced Features:

* **Float16 Datatype Support**: Added comprehensive support for operating with float16 datatypes
* **Plugin Path Configuration**: New user option for editing the plugin path and managing plugins

## Updated Foundation:

> [!IMPORTANT]
>
> - Built and tested with **HDF 4.3.1** and **HDF5 2.0.0**
> - Built and tested with **OpenJDK 21**
> - Uses Java modules for improved modularity
> - Utilizes newest `jpackage` for distribution

# 🚀 New Features & Improvements

## Major Enhancements

* **[GH #117](https://github.com/HDFGroup/hdfview/issues/117)** - Add User Option for editing the plugin path and include plugins
* **[GH #138](https://github.com/HDFGroup/hdfview/issues/138)** - Add Support for operating with float16 datatypes

# 🪲 Bug Fixes

## Major Bug Fixes

* **HDFVIEW-284** - Fixed crashes on NETCDF-4 grids

  The problem was that references in variable-length containers were handled like strings. Upon investigation, the problem found was that references in any container were handled like strings.

  The table display code for vlen references also changed to account for the changes which fixed the hdf5 Java API. The fix required that variable-length types in the Java wrappers in the hdf5 library for read and write also be fixed.

  Related work:
  - **HDFView-221** - Add support for true Variable-length types in Java
  - **HDFView-222** - Fixed the read/write support for variable-length in the Java wrappers. This fix involved handling the data object as a list of lists and using the datatype of the list.
  - **HDFView-222** - Fixed the object library to handle the List of Lists concept
  - **HDFView-223** - Updated the DataProviders, DataDisplayConverters and DataValidators to work with variable-length List of Lists

## Minor Bug Fixes

* **[GH #171](https://github.com/HDFGroup/hdfview/issues/171)** - HDFView fails to find input files on command line when using relative paths

# ☑️ Platforms Tested

HDFView is tested on the following platforms:

* Linux (x86_64, aarch64)
* Windows
* macOS

Current test results and detailed platform information are available in the [GitHub repository](https://github.com/HDFGroup/hdfview).

# ⛔ Known Problems

* **Large Dataset Handling**: HDFView currently cannot nicely handle large datasets when using the default display mode, as the data is loaded in its entirety. To view large datasets, it is recommended to right click on a data object and use the "Open As" menu item, where a subset of data to view can be selected.

* **Large Number of Objects**: HDFView also cannot nicely handle large numbers of objects, because of a design issue that requires HDFView to visit all the objects in a file.

* **Object/Region References in Compound Types**: Object/region references can't be opened by a double-click or by right-clicking and choosing "Show As Table/Image" when inside a compound datatype.

* **Export Dataset in Read-Only Mode**: If a file is opened in read-only mode, right-clicking on a dataset in the tree view and choosing any of the options under the "Export Dataset" menu item will fail with a message of 'Unable to export dataset: Unable to open file'. The current workaround is to re-open the file in read/write mode.

* **Recent Files Button on Mac**: The 'Recent Files' button does not work on Mac due to a cross-platform issue with SWT.

* **PaletteView Selection**: Selecting and changing individual points in PaletteView for an image palette is broken.

* **Source Rebuild Requirements**: Logging and optional HDF4 requires rebuilds from source.

* **Mac File Display**: Automatically opening HDFView and displaying a file selected still does not display the file on a mac.

Please report any new problems found to the [HDFView issue tracker](https://github.com/HDFGroup/hdfview/issues).
