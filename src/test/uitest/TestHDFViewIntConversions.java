package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

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
public class TestHDFViewIntConversions extends AbstractWindowTest {
    @Test
    public void checkHDF5GroupDS08() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DS08BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDS08() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDS08() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            // Open dataset 'DS08BITS'
            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "FF", val),
                    val.equals("FF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "80", val),
                    val.equals("80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "00", val),
                    val.equals("00"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "11111111", val),
                    val.equals("11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "10000000", val),
                    val.equals("10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "00000000", val),
                    val.equals("00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "-1", val),
                    val.equals("-1"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "-128", val),
                    val.equals("-128"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS08()", "wrong data", "0", val),
                    val.equals("0"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDU08() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DU08BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDU08() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDU08() filetree is missing dataset '" + datasetname + "'", items[0].getNode(4).getText().compareTo(datasetname)==0);

            // Open dataset 'DU08BITS'
            items[0].getNode(4).click();
            items[0].getNode(4).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "FF", val),
                    val.equals("FF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "80", val),
                    val.equals("80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "00", val),
                    val.equals("00"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "11111111", val),
                    val.equals("11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "10000000", val),
                    val.equals("10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "00000000", val),
                    val.equals("00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "255", val),
                    val.equals("255"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "128", val),
                    val.equals("128"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU08()", "wrong data", "0", val),
                    val.equals("0"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDS16() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DS16BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDS16() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDS16() filetree is missing dataset '" + datasetname + "'", items[0].getNode(1).getText().compareTo(datasetname)==0);

            // Open dataset 'DS16BITS'
            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "FFFF", val),
                    val.equals("FFFF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "FF80", val),
                    val.equals("FF80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "C000", val),
                    val.equals("C000"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "11111111 11111111", val),
                    val.equals("11111111 11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "11111111 10000000", val),
                    val.equals("11111111 10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "11000000 00000000", val),
                    val.equals("11000000 00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "-1", val),
                    val.equals("-1"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "-128", val),
                    val.equals("-128"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS16()", "wrong data", "-16384", val),
                    val.equals("-16384"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDU16() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DU16BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDU16() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDU16() filetree is missing dataset '" + datasetname + "'", items[0].getNode(5).getText().compareTo(datasetname)==0);

            // Open dataset 'DU16BITS'
            items[0].getNode(5).click();
            items[0].getNode(5).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "FFFF", val),
                    val.equals("FFFF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "FF80", val),
                    val.equals("FF80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "C000", val),
                    val.equals("C000"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "11111111 11111111", val),
                    val.equals("11111111 11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "11111111 10000000", val),
                    val.equals("11111111 10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "11000000 00000000", val),
                    val.equals("11000000 00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "65535", val),
                    val.equals("65535"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "65408", val),
                    val.equals("65408"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU16()", "wrong data", "49152", val),
                    val.equals("49152"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDS32() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DS32BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDS32() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDS32() filetree is missing dataset '" + datasetname + "'", items[0].getNode(2).getText().compareTo(datasetname)==0);

            // Open dataset 'DS32BITS'
            items[0].getNode(2).click();
            items[0].getNode(2).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "FFFFFFFF", val),
                    val.equals("FFFFFFFF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "FFFFFF80", val),
                    val.equals("FFFFFF80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "FFFFC000", val),
                    val.equals("FFFFC000"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "11111111 11111111 11111111 11111111", val),
                    val.equals("11111111 11111111 11111111 11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "11111111 11111111 11111111 10000000", val),
                    val.equals("11111111 11111111 11111111 10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "11111111 11111111 11000000 00000000", val),
                    val.equals("11111111 11111111 11000000 00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "-1", val),
                    val.equals("-1"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "-128", val),
                    val.equals("-128"));

            table.click(8, 8);
            val = table.getCellDataValueByPosition(8, 8);
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS32()", "wrong data", "-16384", val),
                    val.equals("-16384"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDU32() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DU32BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDU32() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDU32() filetree is missing dataset '" + datasetname + "'", items[0].getNode(6).getText().compareTo(datasetname)==0);

            // Open dataset 'DU32BITS'
            items[0].getNode(6).click();
            items[0].getNode(6).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data", "FFFFFFFF", val),
                    val.equals("FFFFFFFF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data", "FFFFFF80", val),
                    val.equals("FFFFFF80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data", "FFFFC000", val),
                    val.equals("FFFFC000"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data",
                    "00000000 00000000 00000000 00000000 11111111 11111111 11111111 11111111", val),
                    val.equals("00000000 00000000 00000000 00000000 11111111 11111111 11111111 11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data",
                    "00000000 00000000 00000000 00000000 11111111 11111111 11111111 10000000", val),
                    val.equals("00000000 00000000 00000000 00000000 11111111 11111111 11111111 10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data",
                    "00000000 00000000 00000000 00000000 11111111 11111111 11000000 00000000", val),
                    val.equals("00000000 00000000 00000000 00000000 11111111 11111111 11000000 00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data", "4294967295", val),
                    val.equals("4294967295"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data", "4294967168", val),
                    val.equals("4294967168"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU32()", "wrong data", "4294950912", val),
                    val.equals("4294950912"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDS64() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DS64BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDS64() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDS64() filetree is missing dataset '" + datasetname + "'", items[0].getNode(3).getText().compareTo(datasetname)==0);

            // Open dataset 'DS64BITS'
            items[0].getNode(3).click();
            items[0].getNode(3).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data", "FFFFFFFFFFFFFFFF", val),
                    val.equals("FFFFFFFFFFFFFFFF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data", "FFFFFFFFFFFFFF80", val),
                    val.equals("FFFFFFFFFFFFFF80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data", "FFFFFFFFFFFFC000", val),
                    val.equals("FFFFFFFFFFFFC000"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data",
                    "11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111", val),
                    val.equals("11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data",
                    "11111111 11111111 11111111 11111111 11111111 11111111 11111111 10000000", val),
                    val.equals("11111111 11111111 11111111 11111111 11111111 11111111 11111111 10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data",
                    "11111111 11111111 11111111 11111111 11111111 11111111 11000000 00000000", val),
                    val.equals("11111111 11111111 11111111 11111111 11111111 11111111 11000000 00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data", "-1", val),
                    val.equals("-1"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data", "-128", val),
                    val.equals("-128"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDS64()", "wrong data", "-16384", val),
                    val.equals("-16384"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void checkHDF5GroupDU64() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DU64BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDU64() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDU64() filetree is missing dataset '" + datasetname + "'", items[0].getNode(7).getText().compareTo(datasetname)==0);

            // Open dataset 'DU64BITS'
            items[0].getNode(7).click();
            items[0].getNode(7).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Show Hexadecimal").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data", "FFFFFFFFFFFFFFFF", val),
                    val.equals("FFFFFFFFFFFFFFFF"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data", "FFFFFFFFFFFFFF80", val),
                    val.equals("FFFFFFFFFFFFFF80"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data", "FFFFFFFFFFFFC000", val),
                    val.equals("FFFFFFFFFFFFC000"));

            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data",
                    "11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111", val),
                    val.equals("11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data",
                    "11111111 11111111 11111111 11111111 11111111 11111111 11111111 10000000", val),
                    val.equals("11111111 11111111 11111111 11111111 11111111 11111111 11111111 10000000"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data",
                    "11111111 11111111 11111111 11111111 11111111 11111111 11000000 00000000", val),
                    val.equals("11111111 11111111 11111111 11111111 11111111 11111111 11000000 00000000"));

            // Reset view to normal integer display
            tableShell.bot().menu("Show Binary").click();

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data", "18446744073709551615", val),
                    val.equals("18446744073709551615"));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data", "18446744073709551488", val),
                    val.equals("18446744073709551488"));

            table.click(8, 8);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("checkHDF5GroupDU64()", "wrong data", "18446744073709535232", val),
                    val.equals("18446744073709535232"));

            tableShell.bot().menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
