package test.uitest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    TestHDFViewMenu.class,
    TestHDFViewLibBounds.class,
    TestHDFViewLinks.class,
    TestHDFViewDatasetFrameSelection.class,
    TestHDFViewImageConversion.class,
    TestTreeViewFiles.class,
    TestTreeViewGroups.class,
    TestTreeViewNewMenu.class
})

public class TestAll {
}
