package test.uitest;

import static org.hamcrest.Matcher.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.Ignore;

public class TestTreeViewNewVLDatatypes extends AbstractWindowTest {
    @Test
    public void createNewHDF5VLDatatype() {
        String filename = "testvldt";
        String file_ext = ".h5";
        String dtname = "testvldtname";
        SWTBotShell tableShell = null;
        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDatatype()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLDatatype() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("New").menu("Datatype").click();

            SWTBotShell dtShell = bot.shell("New Datatype...");
            dtShell.activate();
            bot.waitUntil(Conditions.shellIsActive(dtShell.getText()));

            dtShell.bot().text(0).setText(dtname);

            dtShell.bot().comboBox(1).setSelection("VLEN_INTEGER");
            dtShell.bot().comboBox(2).setSelection("16");

            dtShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(dtShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDatatype()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createNewHDF5VLDatatype() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("createNewHDF5VLDatatype() filetree is missing group '" + dtname + "'", items[0].getNode(0).getText().compareTo(dtname)==0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void createNewHDF5VLDataset() {
        String filename = "testvldataset";
        String file_ext = ".h5";
        String dsname = "testvldatasetname";
        SWTBotShell tableShell = null;
        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLDataset() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("New").menu("Dataset").click();

            SWTBotShell dsShell = bot.shell("New Dataset...");
            dsShell.activate();
            bot.waitUntil(Conditions.shellIsActive(dsShell.getText()));

            dsShell.bot().text(0).setText(dsname);

            dsShell.bot().comboBox(1).setSelection("VLEN_FLOAT");
            dsShell.bot().comboBox(2).setSelection("32");

            dsShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(dsShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createNewHDF5VLDataset() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("createNewHDF5VLDataset() filetree is missing dataset '" + dsname + "'", items[0].getNode(0).getText().compareTo(dsname)==0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void createNewHDF5VLAttribute() {
        String filename = "testvlattr";
        String file_ext = ".h5";
        String daname = "testvlattrname";
        SWTBotShell tableShell = null;
        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLAttribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("Show Attributes").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /");
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().button("  &Add  ").click();

            SWTBotShell daShell = bot.shell("New Attribute...");
            daShell.activate();
            bot.waitUntil(Conditions.shellIsActive(daShell.getText()));

            daShell.bot().text(0).setText(daname);

            daShell.bot().comboBox(0).setSelection("VLEN_STRING");
            daShell.bot().text(2).setText("ABC");

            daShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(daShell));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLAttribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }
}
