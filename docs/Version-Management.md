# HDFView Version Management

Master always carries the placeholder version `99.99.99`. Each release is cut on its own branch, `release/hdfview-X.Y.Z`, and every placeholder below is replaced with the release version on that branch. All of these must agree at release time.

## 1. VERSION File

The `VERSION` file at the project root is read by the build and release workflows. Maven does not read it. Update both version lines:

```properties
HDFVIEW_VERSION=3.5.0
VERSION=HDFView-3.5.0
```

`HDFVIEW_VERSION` names the build artifacts and the release tag (`v3.5.0`). `VERSION` is read separately by `release.yml` and names the release, including pre-releases (`HDFView-3.5.0-pre1`). Leave `HDFVIEW_TAG_PREFIX` and the header comments unchanged.

## 2. Maven POM Files

Update the module version in each POM:

- **pom.xml** (root): project `<version>`
- **repository/pom.xml**: parent `<version>`
- **object/pom.xml**: parent `<version>`
- **hdfview/pom.xml**: parent `<version>` and the `object` dependency `<version>`

The project version is the version the application reports: it is passed to jpackage as `--app-version` and shown in the HDFView title bar. It must match `HDFVIEW_VERSION` in `VERSION`, which names the artifacts that contain it. The `check-version-consistency` action fails the CI pipeline and the release workflow when they differ.

## 3. CHANGELOG

The release notes live in `docs/UsersGuide/CHANGELOG.md`. Update:

- the version on the first line and in the "Executive Summary" heading
- the "Executive Summary" link in the table of contents, whose anchor contains the version without dots (`#-executive-summary-hdfview-version-350` for 3.5.0)
- the HDF4 and HDF5 versions under "Platforms Tested", which should match the `hdf.version` and `hdf5.version` properties in the root `pom.xml` that CI builds against

## Development Versions

Master uses `99.99.99`. Do not merge a release branch's version changes back into master.
