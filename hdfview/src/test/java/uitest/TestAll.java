package uitest;

import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@Tag("ui")
@Tag("integration")
@SelectClasses({TestHDFViewMenu.class, TestHDFViewLibBounds.class, TestHDFViewLinks.class,
                TestHDFViewCutCopyPaste.class, TestHDFViewDatasetFrameSelection.class,
                TestHDFViewAttributes.class, TestHDFViewImageConversion.class, TestTreeViewFiles.class,
                TestTreeViewFilters.class, TestHDFViewIntConversions.class, TestTreeViewNewMenu.class,
                TestTreeViewExport.class, TestHDFViewTAttr2.class, TestTreeViewNewVLDatatypes.class,
                TestHDFViewRefs.class, TestHDFViewFloat16.class})

public class TestAll {}
