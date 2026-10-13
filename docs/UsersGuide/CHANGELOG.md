HDFView version 3.5.0

# 🔺 HDFView Changelog
All notable changes to this project will be documented in this file. This document describes the differences between this release and the previous HDFView release, platforms tested, and known problems in this release.

# 🔗 Quick Links
* [HDFView releases](https://github.com/HDFGroup/hdfview/releases)
* [HDFView source](https://github.com/HDFGroup/hdfview)
* [Getting help, questions, or comments](https://github.com/HDFGroup/hdfview/issues)

## 📖 Contents
* [Executive Summary](#-executive-summary-hdfview-version-350)
* [New Features & Improvements](#-new-features--improvements)
* [Bug Fixes](#-bug-fixes)
* [Platforms Tested](#%EF%B8%8F-platforms-tested)
* [Known Problems](#-known-problems)

# 🔆 Executive Summary: HDFView Version 3.5.0

## Enhanced Features:

* **Nested and variable-length compound types**: Variable-length sequences of compounds, and compounds with variable-length or nested compound members, can now be opened and displayed.
* **Bundled filter plugins**: The HDF5 filter plugins shipped with HDFView are now found automatically on every platform.

# 🚀 New Features & Improvements

## Major Enhancements

* **[GH #472](https://github.com/HDFGroup/hdfview/issues/472)** - Variable-length sequences of compound types can now be read. Each sequence is displayed in a single cell, e.g. `[{1, 2}, {3, 4}]`.

* **[GH #477](https://github.com/HDFGroup/hdfview/issues/477)** - The bundled filter plugin directory is now on the HDF5 plugin search path by default, so datasets written with those filters open without editing the plugin path in User Options. Windows and macOS installers now include the plugin files as well.

* HDFView is now built against HDF 4.4.0 and HDF5 2.2.0, up from HDF 4.3.1 and HDF5 2.0.0.

# 🪲 Bug Fixes

## Major Bug Fixes

* **[GH #165](https://github.com/HDFGroup/hdfview/issues/165), [GH #470](https://github.com/HDFGroup/hdfview/issues/470), [GH #491](https://github.com/HDFGroup/hdfview/issues/491)** - Compound datasets and attributes with variable-length or nested compound members could show error values, drop columns, fail to open, or crash HDFView. They now display correctly, with nested values shown in a single cell.

* **[GH #475](https://github.com/HDFGroup/hdfview/issues/475)** - Files were always opened read-only in SWMR mode, regardless of the SWMR setting in User Options.

* **Linux RPM now installs on RHEL 9**: The RPM required `webkit2gtk4.1`, which RHEL 9 does not provide. HDFView no longer depends on WebKit.

* **Windows: other HDF4/HDF5 installations no longer prevent HDFView from starting**: HDFView ships its own copies of the HDF4 and HDF5 libraries, but Windows could load a different installation in their place if one was on `PATH`. Windows DLLs carry no version in their name, so any `hdf5.dll` on `PATH` would satisfy the reference regardless of which version it was, and HDFView usually failed to start, reporting "failed to launch JVM".

  The bundled DLLs are now installed next to `HDFView.exe`, whose directory Windows searches ahead of both the system folder and `PATH`, so the bundled copies are used regardless of what else is installed on the machine.

  Consequently, `PATH` can no longer be used to make an installed HDFView load a *different* HDF4/HDF5 build. This applies to the packaged application only, as running HDFView from a source build is unchanged, and still uses the libraries named in `build.properties`.

## Minor Bug Fixes

* **[GH #465](https://github.com/HDFGroup/hdfview/issues/465)** - The "Apply Bitmask" and "Show Value of Selected Bits" options had no effect on displayed data.

# ☑️ Platforms Tested

HDFView is built and tested with **HDF 4.4.0** and **HDF5 2.2.0** on the following platforms:

* Linux (Ubuntu 24, Fedora)
* Windows
* macOS (arm64, x86_64)

Current test results and detailed platform information are available in the [GitHub repository](https://github.com/HDFGroup/hdfview).

# ⛔ Known Problems

* **Large Dataset Handling**: HDFView currently cannot nicely handle large datasets when using the default display mode, as the data is loaded in its entirety. To view large datasets, it is recommended to right click on a data object and use the "Open As" menu item, where a subset of data to view can be selected.

* **Object/Region References in Compound Types**: Object/region references can't be opened by a double-click or by right-clicking and choosing "Show As Table/Image" when inside a compound datatype.

* **Export Dataset in Read-Only Mode**: If a file is opened in read-only mode, right-clicking on a dataset in the tree view and choosing any of the options under the "Export Dataset" menu item will fail with a message of 'Unable to export dataset: Unable to open file'. The current workaround is to re-open the file in read/write mode.

* **Recent Files Button on Mac**: The 'Recent Files' button does not work on Mac due to a cross-platform issue with SWT.

* **PaletteView Selection**: Selecting and changing individual points in PaletteView for an image palette is broken.

* **Source Rebuild Requirements**: Logging and optional HDF4 requires rebuilds from source.

* **Mac File Display**: Automatically opening HDFView and displaying a file selected still does not display the file on a mac.

Please report any new problems found to the [HDFView issue tracker](https://github.com/HDFGroup/hdfview/issues).
