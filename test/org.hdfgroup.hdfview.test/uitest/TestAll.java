package uitest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    TestHDFViewMenu.class,
    TestHDFViewLibBounds.class,
    TestHDFViewLinks.class,
    TestHDFViewCutCopyPaste.class,
    TestHDFViewDatasetFrameSelection.class,
    TestHDFViewAttributes.class,
    TestHDFViewImageConversion.class,
    TestTreeViewFiles.class,
    TestTreeViewFilters.class,
    TestHDFViewIntConversions.class,
    TestTreeViewNewMenu.class,
    TestTreeViewExport.class,
    TestHDFViewTAttr2.class,
    TestTreeViewNewVLDatatypes.class,
    TestHDFViewRefs.class
})

public class TestAll {
}
