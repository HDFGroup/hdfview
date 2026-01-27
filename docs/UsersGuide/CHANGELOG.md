HDFView version 99.99.99

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

# 🔆 Executive Summary: HDFView Version 99.99.99

## Enhanced Features:

# 🚀 New Features & Improvements

## Major Enhancements

# 🪲 Bug Fixes

## Major Bug Fixes

## Minor Bug Fixes

# ☑️ Platforms Tested

HDFView is built and tested with **HDF 4.3.X** and **HDF5 2.Y.Z** on the following platforms:

* Linux (Ubuntu 24, Fedora)
* Windows
* macOS (amd64, intel)

Current test results and detailed platform information are available in the [GitHub repository](https://github.com/HDFGroup/hdfview).

# ⛔ Known Problems

* **PATH pointing to other HDF4/5 installations**: If the environment path points to a directory including HDF4/5 installations, then these installations may be loaded by HDFView instead of the bundled HDF4/5 versions, causing the application to fail to launch with a "failed to launch JVM" error. This can be resolved by either removing those directories from the PATH, or removing the HDF4/5 installations from that directory.

* **Large Dataset Handling**: HDFView currently cannot nicely handle large datasets when using the default display mode, as the data is loaded in its entirety. To view large datasets, it is recommended to right click on a data object and use the "Open As" menu item, where a subset of data to view can be selected.

* **Object/Region References in Compound Types**: Object/region references can't be opened by a double-click or by right-clicking and choosing "Show As Table/Image" when inside a compound datatype.

* **Export Dataset in Read-Only Mode**: If a file is opened in read-only mode, right-clicking on a dataset in the tree view and choosing any of the options under the "Export Dataset" menu item will fail with a message of 'Unable to export dataset: Unable to open file'. The current workaround is to re-open the file in read/write mode.

* **Recent Files Button on Mac**: The 'Recent Files' button does not work on Mac due to a cross-platform issue with SWT.

* **PaletteView Selection**: Selecting and changing individual points in PaletteView for an image palette is broken.

* **Source Rebuild Requirements**: Logging and optional HDF4 requires rebuilds from source.

* **Mac File Display**: Automatically opening HDFView and displaying a file selected still does not display the file on a mac.

Please report any new problems found to the [HDFView issue tracker](https://github.com/HDFGroup/hdfview/issues).
