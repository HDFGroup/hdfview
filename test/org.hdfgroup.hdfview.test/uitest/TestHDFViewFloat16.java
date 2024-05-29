package uitest;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

/*
 * Tests the ability of HDFView to display integer values as Hexadecimal
 * or Binary values.
 *
 * Note: Since the conversion of data values in the table to Hexadecimal
 * or Binary is simply a display conversion, the underlying data doesn't
 * change and calling something like table.getCellDataValueByPosition()
 * will not return the converted value. The displayed text values of
 * individual cells in the table also cannot be conveniently retrieved,
 * so as a workaround, the text field above the table is checked for
 * the correct value. So long as this text field is functioning properly,
 * it is guaranteed that we are retrieving the correct converted data
 * value.
 */
public class TestHDFViewFloat16 extends AbstractWindowTest {
    @Test
    public void checkHDF5DS16BITS()
    {
        String[][] expectedData = {
            {"16.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0", "5.5", "6.0",
             "6.5", "7.0", "7.5"},
            {"15.0", "0.5625", "1.0625", "1.5625", "2.0625", "2.5625", "3.0625", "3.5625", "4.0625", "4.5625",
             "5.0625", "5.5625", "6.0625", "6.5625", "7.0625", "7.5625"},
            {"14.0", "0.625", "1.125", "1.625", "2.125", "2.625", "3.125", "3.625", "4.125", "4.625", "5.125",
             "5.625", "6.125", "6.625", "7.125", "7.625"},
            {"13.0", "0.6875", "1.1875", "1.6875", "2.1875", "2.6875", "3.1875", "3.6875", "4.1875", "4.6875",
             "5.1875", "5.6875", "6.1875", "6.6875", "7.1875", "7.6875"},
            {"12.0", "0.75", "1.25", "1.75", "2.25", "2.75", "3.25", "3.75", "4.25", "4.75", "5.25", "5.75",
             "6.25", "6.75", "7.25", "7.75"},
            {"11.0", "0.8125", "1.3125", "1.8125", "2.3125", "2.8125", "3.3125", "3.8125", "4.3125", "4.8125",
             "5.3125", "5.8125", "6.3125", "6.8125", "7.3125", "7.8125"},
            {"10.0", "0.875", "1.375", "1.875", "2.375", "2.875", "3.375", "3.875", "4.375", "4.875", "5.375",
             "5.875", "6.375", "6.875", "7.375", "7.875"},
            {"9.0", "0.9375", "1.4375", "1.9375", "2.4375", "2.9375", "3.4375", "3.9375", "4.4375", "4.9375",
             "5.4375", "5.9375", "6.4375", "6.9375", "7.4375", "7.9375"}};
        String[][] expectedDataSci = {
            {"1.6E1", "5.0E-1", "1.0E0", "1.5E0", "2.0E0", "2.5E0", "3.0E0", "3.5E0", "4.0E0", "4.5E0",
             "5.0E0", "5.5E0", "6.0E0", "6.5E0", "7.0E0", "7.5E0"},
            {"1.5E1", "5.625E-1", "1.0625E0", "1.5625E0", "2.0625E0", "2.5625E0", "3.0625E0", "3.5625E0",
             "4.0625E0", "4.5625E0", "5.0625E0", "5.5625E0", "6.0625E0", "6.5625E0", "7.0625E0", "7.5625E0"},
            {"1.4E1", "6.25E-1", "1.125E0", "1.625E0", "2.125E0", "2.625E0", "3.125E0", "3.625E0", "4.125E0",
             "4.625E0", "5.125E0", "5.625E0", "6.125E0", "6.625E0", "7.125E0", "7.625E0"},
            {"1.3E1", "6.875E-1", "1.1875E0", "1.6875E0", "2.1875E0", "2.6875E0", "3.1875E0", "3.6875E0",
             "4.1875E0", "4.6875E0", "5.1875E0", "5.6875E0", "6.1875E0", "6.6875E0", "7.1875E0", "7.6875E0"},
            {"1.2E1", "7.5E-1", "1.25E0", "1.75E0", "2.25E0", "2.75E0", "3.25E0", "3.75E0", "4.25E0",
             "4.75E0", "5.25E0", "5.75E0", "6.25E0", "6.75E0", "7.25E0", "7.75E0"},
            {"1.1E1", "8.125E-1", "1.3125E0", "1.8125E0", "2.3125E0", "2.8125E0", "3.3125E0", "3.8125E0",
             "4.3125E0", "4.8125E0", "5.3125E0", "5.8125E0", "6.3125E0", "6.8125E0", "7.3125E0", "7.8125E0"},
            {"1.0E1", "8.75E-1", "1.375E0", "1.875E0", "2.375E0", "2.875E0", "3.375E0", "3.875E0", "4.375E0",
             "4.875E0", "5.375E0", "5.875E0", "6.375E0", "6.875E0", "7.375E0", "7.875E0"},
            {"9.0E0", "9.375E-1", "1.4375E0", "1.9375E0", "2.4375E0", "2.9375E0", "3.4375E0", "3.9375E0",
             "4.4375E0", "4.9375E0", "5.4375E0", "5.9375E0", "6.4375E0", "6.9375E0", "7.4375E0", "7.9375E0"}};
        SWTBotShell tableShell   = null;
        final String filename    = "tfloat16.h5";
        final String datasetName = "/DS16BITS";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5DS16BITS()", 4, filename);

            // Open dataset 'DS08BITS'
            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5DS16BITS()", false);

            retriever.testAllTableLocations(expectedData);

            tableShell.bot().menu().menu("Data Display").menu("Show Scientific Notation").click();
            retriever.testAllTableLocations(expectedDataSci);
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
    public void checkHDF5AttrDS16BITS()
    {
        String[][] expectedAttrData = {
            {"DS16BITS", "16-bit floating-point", "128",
             "16.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 15.0, 0.5625, 1.0625, 1.5625, 2.0625, 2.5625, 3.0625, 3.5625, 4.0625, 4.5625, 5.0625, 5.5625, 6.0625, 6.5625, 7.0625, 7.5625, 14.0, 0.625, 1.125, 1.625, 2.125, 2.625, 3.125, 3.625, 4.125, 4.625, 5.125, 5.625, 6.125, 6.625, 7.125, 7.625, 13.0, 0.6875"}};
        String[][] expectedData = {
            {"16.0"},   {"0.5"},    {"1.0"},    {"1.5"},    {"2.0"},    {"2.5"},    {"3.0"},    {"3.5"},
            {"4.0"},    {"4.5"},    {"5.0"},    {"5.5"},    {"6.0"},    {"6.5"},    {"7.0"},    {"7.5"},
            {"15.0"},   {"0.5625"}, {"1.0625"}, {"1.5625"}, {"2.0625"}, {"2.5625"}, {"3.0625"}, {"3.5625"},
            {"4.0625"}, {"4.5625"}, {"5.0625"}, {"5.5625"}, {"6.0625"}, {"6.5625"}, {"7.0625"}, {"7.5625"},
            {"14.0"},   {"0.625"},  {"1.125"},  {"1.625"},  {"2.125"},  {"2.625"},  {"3.125"},  {"3.625"},
            {"4.125"},  {"4.625"},  {"5.125"},  {"5.625"},  {"6.125"},  {"6.625"},  {"7.125"},  {"7.625"},
            {"13.0"},   {"0.6875"}, {"1.1875"}, {"1.6875"}, {"2.1875"}, {"2.6875"}, {"3.1875"}, {"3.6875"},
            {"4.1875"}, {"4.6875"}, {"5.1875"}, {"5.6875"}, {"6.1875"}, {"6.6875"}, {"7.1875"}, {"7.6875"},
            {"12.0"},   {"0.75"},   {"1.25"},   {"1.75"},   {"2.25"},   {"2.75"},   {"3.25"},   {"3.75"},
            {"4.25"},   {"4.75"},   {"5.25"},   {"5.75"},   {"6.25"},   {"6.75"},   {"7.25"},   {"7.75"},
            {"11.0"},   {"0.8125"}, {"1.3125"}, {"1.8125"}, {"2.3125"}, {"2.8125"}, {"3.3125"}, {"3.8125"},
            {"4.3125"}, {"4.8125"}, {"5.3125"}, {"5.8125"}, {"6.3125"}, {"6.8125"}, {"7.3125"}, {"7.8125"},
            {"10.0"},   {"0.875"},  {"1.375"},  {"1.875"},  {"2.375"},  {"2.875"},  {"3.375"},  {"3.875"},
            {"4.375"},  {"4.875"},  {"5.375"},  {"5.875"},  {"6.375"},  {"6.875"},  {"7.375"},  {"7.875"},
            {"9.0"},    {"0.9375"}, {"1.4375"}, {"1.9375"}, {"2.4375"}, {"2.9375"}, {"3.4375"}, {"3.9375"},
            {"4.4375"}, {"4.9375"}, {"5.4375"}, {"5.9375"}, {"6.4375"}, {"6.9375"}, {"7.4375"}, {"7.9375"}};
        String[][] expectedDataSci = {
            {"1.6E1"},    {"5.0E-1"},   {"1.0E0"},    {"1.5E0"},    {"2.0E0"},    {"2.5E0"},    {"3.0E0"},
            {"3.5E0"},    {"4.0E0"},    {"4.5E0"},    {"5.0E0"},    {"5.5E0"},    {"6.0E0"},    {"6.5E0"},
            {"7.0E0"},    {"7.5E0"},    {"1.5E1"},    {"5.625E-1"}, {"1.0625E0"}, {"1.5625E0"}, {"2.0625E0"},
            {"2.5625E0"}, {"3.0625E0"}, {"3.5625E0"}, {"4.0625E0"}, {"4.5625E0"}, {"5.0625E0"}, {"5.5625E0"},
            {"6.0625E0"}, {"6.5625E0"}, {"7.0625E0"}, {"7.5625E0"}, {"1.4E1"},    {"6.25E-1"},  {"1.125E0"},
            {"1.625E0"},  {"2.125E0"},  {"2.625E0"},  {"3.125E0"},  {"3.625E0"},  {"4.125E0"},  {"4.625E0"},
            {"5.125E0"},  {"5.625E0"},  {"6.125E0"},  {"6.625E0"},  {"7.125E0"},  {"7.625E0"},  {"1.3E1"},
            {"6.875E-1"}, {"1.1875E0"}, {"1.6875E0"}, {"2.1875E0"}, {"2.6875E0"}, {"3.1875E0"}, {"3.6875E0"},
            {"4.1875E0"}, {"4.6875E0"}, {"5.1875E0"}, {"5.6875E0"}, {"6.1875E0"}, {"6.6875E0"}, {"7.1875E0"},
            {"7.6875E0"}, {"1.2E1"},    {"7.5E-1"},   {"1.25E0"},   {"1.75E0"},   {"2.25E0"},   {"2.75E0"},
            {"3.25E0"},   {"3.75E0"},   {"4.25E0"},   {"4.75E0"},   {"5.25E0"},   {"5.75E0"},   {"6.25E0"},
            {"6.75E0"},   {"7.25E0"},   {"7.75E0"},   {"1.1E1"},    {"8.125E-1"}, {"1.3125E0"}, {"1.8125E0"},
            {"2.3125E0"}, {"2.8125E0"}, {"3.3125E0"}, {"3.8125E0"}, {"4.3125E0"}, {"4.8125E0"}, {"5.3125E0"},
            {"5.8125E0"}, {"6.3125E0"}, {"6.8125E0"}, {"7.3125E0"}, {"7.8125E0"}, {"1.0E1"},    {"8.75E-1"},
            {"1.375E0"},  {"1.875E0"},  {"2.375E0"},  {"2.875E0"},  {"3.375E0"},  {"3.875E0"},  {"4.375E0"},
            {"4.875E0"},  {"5.375E0"},  {"5.875E0"},  {"6.375E0"},  {"6.875E0"},  {"7.375E0"},  {"7.875E0"},
            {"9.0E0"},    {"9.375E-1"}, {"1.4375E0"}, {"1.9375E0"}, {"2.4375E0"}, {"2.9375E0"}, {"3.4375E0"},
            {"3.9375E0"}, {"4.4375E0"}, {"4.9375E0"}, {"5.4375E0"}, {"5.9375E0"}, {"6.4375E0"}, {"6.9375E0"},
            {"7.4375E0"}, {"7.9375E0"}};
        SWTBotShell tableShell = null;
        String filename        = "tfloat16.h5";
        String datasetName     = "/DS16BITS";
        String attributeName   = "DS16BITS";
        File hdf_file          = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5AttrDS16BITS()", 4, filename);

            // Open dataset Attribute Table
            SWTBotTable attrTable = openAttributeTable(filetree, filename, datasetName);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(attrTable, "checkHDF5AttrDS16BITS()", false);

            retriever.testAllTableLocations(expectedAttrData);

            // Open attribute 1D
            tableShell               = openAttributeObject(attrTable, attributeName, 0);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5AttrDS16BITS()", false);

            retriever.testAllTableLocations(expectedData);

            tableShell.bot().menu().menu("Data Display").menu("Show Scientific Notation").click();
            retriever.testAllTableLocations(expectedDataSci);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
    public void checkHDF5DS16Array()
    {
        String[][] expectedData = {
            {"^\\[16.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, .*\\]"}};
        String[][] expectedDataSci = {
            {"^\\[1.6E1, 5.0E-1, 1.0E0, 1.5E0, 2.0E0, 2.5E0, 3.0E0, 3.5E0, 4.0E0, 4.5E0, .*\\]"}};
        SWTBotShell tableShell   = null;
        final String filename    = "tfloat16.h5";
        final String datasetName = "/DS16BITS_array";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5DS16Array()", 4, filename);

            // Open dataset 'DS08BITS'
            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5DS16Array()", false);

            retriever.testAllTableLocations(expectedData);

            /*
             * TODO: not supported yet
             */
            /*
             * tableShell.bot().menu().menu("Data Display").menu("Show Scientific Notation").click();
             * retriever.testAllTableLocations(expectedDataSci);
             */
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
    public void checkHDF5DS16Compound()
    {
        String[][] expectedData = {
            {"16.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0", "5.5", "6.0",
             "6.5", "7.0", "7.5"},
            {"15.0", "0.5625", "1.0625", "1.5625", "2.0625", "2.5625", "3.0625", "3.5625", "4.0625", "4.5625",
             "5.0625", "5.5625", "6.0625", "6.5625", "7.0625", "7.5625"},
            {"14.0", "0.625", "1.125", "1.625", "2.125", "2.625", "3.125", "3.625", "4.125", "4.625", "5.125",
             "5.625", "6.125", "6.625", "7.125", "7.625"},
            {"13.0", "0.6875", "1.1875", "1.6875", "2.1875", "2.6875", "3.1875", "3.6875", "4.1875", "4.6875",
             "5.1875", "5.6875", "6.1875", "6.6875", "7.1875", "7.6875"},
            {"12.0", "0.75", "1.25", "1.75", "2.25", "2.75", "3.25", "3.75", "4.25", "4.75", "5.25", "5.75",
             "6.25", "6.75", "7.25", "7.75"},
            {"11.0", "0.8125", "1.3125", "1.8125", "2.3125", "2.8125", "3.3125", "3.8125", "4.3125", "4.8125",
             "5.3125", "5.8125", "6.3125", "6.8125", "7.3125", "7.8125"},
            {"10.0", "0.875", "1.375", "1.875", "2.375", "2.875", "3.375", "3.875", "4.375", "4.875", "5.375",
             "5.875", "6.375", "6.875", "7.375", "7.875"},
            {"9.0", "0.9375", "1.4375", "1.9375", "2.4375", "2.9375", "3.4375", "3.9375", "4.4375", "4.9375",
             "5.4375", "5.9375", "6.4375", "6.9375", "7.4375", "7.9375"}};
        String[][] expectedDataSci = {
            {"1.6E1", "5.0E-1", "1.0E0", "1.5E0", "2.0E0", "2.5E0", "3.0E0", "3.5E0", "4.0E0", "4.5E0",
             "5.0E0", "5.5E0", "6.0E0", "6.5E0", "7.0E0", "7.5E0"},
            {"1.5E1", "5.625E-1", "1.0625E0", "1.5625E0", "2.0625E0", "2.5625E0", "3.0625E0", "3.5625E0",
             "4.0625E0", "4.5625E0", "5.0625E0", "5.5625E0", "6.0625E0", "6.5625E0", "7.0625E0", "7.5625E0"},
            {"1.4E1", "6.25E-1", "1.125E0", "1.625E0", "2.125E0", "2.625E0", "3.125E0", "3.625E0", "4.125E0",
             "4.625E0", "5.125E0", "5.625E0", "6.125E0", "6.625E0", "7.125E0", "7.625E0"},
            {"1.3E1", "6.875E-1", "1.1875E0", "1.6875E0", "2.1875E0", "2.6875E0", "3.1875E0", "3.6875E0",
             "4.1875E0", "4.6875E0", "5.1875E0", "5.6875E0", "6.1875E0", "6.6875E0", "7.1875E0", "7.6875E0"},
            {"1.2E1", "7.5E-1", "1.25E0", "1.75E0", "2.25E0", "2.75E0", "3.25E0", "3.75E0", "4.25E0",
             "4.75E0", "5.25E0", "5.75E0", "6.25E0", "6.75E0", "7.25E0", "7.75E0"},
            {"1.1E1", "8.125E-1", "1.3125E0", "1.8125E0", "2.3125E0", "2.8125E0", "3.3125E0", "3.8125E0",
             "4.3125E0", "4.8125E0", "5.3125E0", "5.8125E0", "6.3125E0", "6.8125E0", "7.3125E0", "7.8125E0"},
            {"1.0E1", "8.75E-1", "1.375E0", "1.875E0", "2.375E0", "2.875E0", "3.375E0", "3.875E0", "4.375E0",
             "4.875E0", "5.375E0", "5.875E0", "6.375E0", "6.875E0", "7.375E0", "7.875E0"},
            {"9.0E0", "9.375E-1", "1.4375E0", "1.9375E0", "2.4375E0", "2.9375E0", "3.4375E0", "3.9375E0",
             "4.4375E0", "4.9375E0", "5.4375E0", "5.9375E0", "6.4375E0", "6.9375E0", "7.4375E0", "7.9375E0"}};
        SWTBotShell tableShell   = null;
        final String filename    = "tfloat16.h5";
        final String datasetName = "/DS16BITS_compound";
        File hdf_file            = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "checkHDF5DS16Compound()", 4, filename);

            // Open dataset 'DS08BITS'
            tableShell                     = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever =
                DataRetrieverFactory.getTableDataRetriever(dataTable, "checkHDF5DS16Compound()", false);
            retriever.setContainerHeaderOffset(2, 0);

            retriever.testAllTableLocations(expectedData);

            /*
             * TODO: not supported yet
             */
            /*
             * tableShell.bot().menu().menu("Data Display").menu("Show Scientific Notation").click();
             * retriever.testAllTableLocations(expectedDataSci);
             */
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
