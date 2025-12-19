package uitest;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

/*
 * Tests the ability of HDFView to display complex datatypes:
 * - Float32 complex
 * - Float64 complex
 * - Long double complex (LE and BE)
 * - VLEN complex (should show error dialog)
 * - Complex arrays and compounds
 */
@Tag("ui")
@Tag("integration")
public class TestHDFViewComplex extends AbstractWindowTest {

    // ==================== Float32 Complex Tests ====================

    @Test
    public void checkHDF5Float32Complex()
    {
        // Test first 3 rows of F32 complex dataset: 10+0i, 1+1i, 2+2i, ...
        String[][] expectedData = {
            {"10.0+0.0i", "1.0+1.0i", "2.0+2.0i", "3.0+3.0i", "4.0+4.0i", "5.0+5.0i", "6.0+6.0i", "7.0+7.0i",
             "8.0+8.0i", "9.0+9.0i"},
            {"9.0+0.0i", "1.1+1.1i", "2.1+2.1i", "3.1+3.1i", "4.1+4.1i", "5.1+5.1i", "6.1+6.1i", "7.1+7.1i",
             "8.1+8.1i", "9.1+9.1i"},
            {"8.0+0.0i", "1.2+1.2i", "2.2+2.2i", "3.2+3.2i", "4.2+4.2i", "5.2+5.2i", "6.2+6.2i", "7.2+7.2i",
             "8.2+8.2i", "9.2+9.2i"}};

        SWTBotShell tableShell   = null;
        final String filename    = "tcomplex.h5";
        final String datasetName = "/DatasetFloatComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5Float32Complex()", 7, filename);

            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5Float32Complex()", false);

            // Test first 3 rows
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < expectedData[row].length; col++) {
                    retriever.testTableLocation(row, col, expectedData[row][col]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ==================== Float64 Complex Tests ====================

    @Test
    public void checkHDF5Float64Complex()
    {
        // Test first 3 rows of F64 complex dataset
        String[][] expectedData = {
            {"10.0+0.0i", "1.0+1.0i", "2.0+2.0i", "3.0+3.0i", "4.0+4.0i", "5.0+5.0i", "6.0+6.0i", "7.0+7.0i",
             "8.0+8.0i", "9.0+9.0i"},
            {"9.0+0.0i", "1.1+1.1i", "2.1+2.1i", "3.1+3.1i", "4.1+4.1i", "5.1+5.1i", "6.1+6.1i", "7.1+7.1i",
             "8.1+8.1i", "9.1+9.1i"},
            {"8.0+0.0i", "1.2+1.2i", "2.2+2.2i", "3.2+3.2i", "4.2+4.2i", "5.2+5.2i", "6.2+6.2i", "7.2+7.2i",
             "8.2+8.2i", "9.2+9.2i"}};

        SWTBotShell tableShell   = null;
        final String filename    = "tcomplex.h5";
        final String datasetName = "/DatasetDoubleComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5Float64Complex()", 7, filename);

            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5Float64Complex()", false);

            // Test first 3 rows
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < expectedData[row].length; col++) {
                    retriever.testTableLocation(row, col, expectedData[row][col]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ==================== Long Double Complex Tests ====================

    @Test
    public void checkHDF5LongDoubleComplex()
    {
        // Test first 3 rows of long double complex dataset (LE)
        String[][] expectedData = {
            {"10.0+0.0i", "1.0+1.0i", "2.0+2.0i", "3.0+3.0i", "4.0+4.0i", "5.0+5.0i", "6.0+6.0i", "7.0+7.0i",
             "8.0+8.0i", "9.0+9.0i"},
            {"9.0+0.0i", "1.1+1.1i", "2.1+2.1i", "3.1+3.1i", "4.1+4.1i", "5.1+5.1i", "6.1+6.1i", "7.1+7.1i",
             "8.1+8.1i", "9.1+9.1i"},
            {"8.0+0.0i", "1.2+1.2i", "2.2+2.2i", "3.2+3.2i", "4.2+4.2i", "5.2+5.2i", "6.2+6.2i", "7.2+7.2i",
             "8.2+8.2i", "9.2+9.2i"}};

        SWTBotShell tableShell   = null;
        final String filename    = "tcomplex.h5";
        final String datasetName = "/DatasetLongDoubleComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5LongDoubleComplex()", 7, filename);

            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5LongDoubleComplex()", false);

            // Test first 3 rows
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < expectedData[row].length; col++) {
                    retriever.testTableLocation(row, col, expectedData[row][col]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5LongDoubleComplexBE()
    {
        // Test first 3 rows of long double complex dataset (BE)
        String[][] expectedData = {
            {"10.0+0.0i", "1.0+1.0i", "2.0+2.0i", "3.0+3.0i", "4.0+4.0i", "5.0+5.0i", "6.0+6.0i", "7.0+7.0i",
             "8.0+8.0i", "9.0+9.0i"},
            {"9.0+0.0i", "1.1+1.1i", "2.1+2.1i", "3.1+3.1i", "4.1+4.1i", "5.1+5.1i", "6.1+6.1i", "7.1+7.1i",
             "8.1+8.1i", "9.1+9.1i"},
            {"8.0+0.0i", "1.2+1.2i", "2.2+2.2i", "3.2+3.2i", "4.2+4.2i", "5.2+5.2i", "6.2+6.2i", "7.2+7.2i",
             "8.2+8.2i", "9.2+9.2i"}};

        SWTBotShell tableShell   = null;
        final String filename    = "tcomplex_be.h5";
        final String datasetName = "/DatasetLongDoubleComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5LongDoubleComplexBE()", 7, filename);

            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5LongDoubleComplexBE()", false);

            // Test first 3 rows
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < expectedData[row].length; col++) {
                    retriever.testTableLocation(row, col, expectedData[row][col]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ==================== VLEN Complex Error Test ====================

    @Test
    public void checkHDF5VLENComplexError()
    {
        // Test that VLEN complex dataset shows error dialog
        SWTBotShell tableShell   = null;
        SWTBotShell errorShell   = null;
        final String filename    = "tcomplex.h5";
        final String datasetName = "/VariableLengthDatasetFloatComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5VLENComplexError()", 7, filename);

            // Try to open VLEN complex dataset - should trigger error dialog
            filetree.getTreeItem(filename).getNode(datasetName).doubleClick();

            // Wait for error dialog
            bot.waitUntil(Conditions.shellIsActive("Error"), 5000);
            errorShell = bot.shell("Error");

            // Verify error message mentions VLEN complex limitation
            String errorMessage = errorShell.bot().label(1).getText();
            assertTrue(errorMessage.contains("Variable-length complex") ||
                          errorMessage.contains("not currently supported"),
                      "Error dialog should mention VLEN complex limitation");

            // Close error dialog
            errorShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(errorShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            if (errorShell != null && errorShell.isOpen()) {
                errorShell.close();
            }
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ==================== Complex Array Test ====================

    @Test
    public void checkHDF5ComplexArray()
    {
        // Test first 3 rows of complex array dataset
        // This is a 1x1 dataset containing a 10x10 array of complex values
        String[][] expectedData = {
            {"10.0+0.0i", "1.0+1.0i", "2.0+2.0i", "3.0+3.0i", "4.0+4.0i", "5.0+5.0i", "6.0+6.0i", "7.0+7.0i",
             "8.0+8.0i", "9.0+9.0i"},
            {"9.0+0.0i", "1.1+1.1i", "2.1+2.1i", "3.1+3.1i", "4.1+4.1i", "5.1+5.1i", "6.1+6.1i", "7.1+7.1i",
             "8.1+8.1i", "9.1+9.1i"},
            {"8.0+0.0i", "1.2+1.2i", "2.2+2.2i", "3.2+3.2i", "4.2+4.2i", "5.2+5.2i", "6.2+6.2i", "7.2+7.2i",
             "8.2+8.2i", "9.2+9.2i"}};

        SWTBotShell tableShell   = null;
        final String filename    = "tcomplex.h5";
        final String datasetName = "/ArrayDatasetFloatComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5ComplexArray()", 7, filename);

            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5ComplexArray()", false);

            // Test first 3 rows
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < expectedData[row].length; col++) {
                    retriever.testTableLocation(row, col, expectedData[row][col]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ==================== Complex Compound Test ====================

    @Test
    public void checkHDF5ComplexCompound()
    {
        // Test first 3 rows of complex compound dataset
        // Each element is a compound with one complex field
        String[][] expectedData = {
            {"{10.0+0.0i}", "{1.0+1.0i}", "{2.0+2.0i}", "{3.0+3.0i}", "{4.0+4.0i}", "{5.0+5.0i}", "{6.0+6.0i}",
             "{7.0+7.0i}", "{8.0+8.0i}", "{9.0+9.0i}"},
            {"{9.0+0.0i}", "{1.1+1.1i}", "{2.1+2.1i}", "{3.1+3.1i}", "{4.1+4.1i}", "{5.1+5.1i}", "{6.1+6.1i}",
             "{7.1+7.1i}", "{8.1+8.1i}", "{9.1+9.1i}"},
            {"{8.0+0.0i}", "{1.2+1.2i}", "{2.2+2.2i}", "{3.2+3.2i}", "{4.2+4.2i}", "{5.2+5.2i}", "{6.2+6.2i}",
             "{7.2+7.2i}", "{8.2+8.2i}", "{9.2+9.2i}"}};

        SWTBotShell tableShell   = null;
        final String filename    = "tcomplex.h5";
        final String datasetName = "/CompoundDatasetFloatComplex";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5ComplexCompound()", 7, filename);

            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5ComplexCompound()", false);

            // Test first 3 rows
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < expectedData[row].length; col++) {
                    retriever.testTableLocation(row, col, expectedData[row][col]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
