package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import java.io.File;

import org.junit.Test;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class TestTreeViewFiles extends AbstractWindowTest {
    @Test
    public void openHDF5ScalarGroup() {
        String filename = "tscalarintsize";
        String file_ext = ".h5";
        String dataset_name = "DS08BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, false);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("openHDF5ScalarGroup filetree row count: " + filetree.visibleRowCount(), filetree.visibleRowCount()==10);
            assertTrue("openHDF5ScalarGroup filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5ScalarGroup filetree is missing dataset " + dataset_name, items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            assertTrue(table.getCellDataValueByPosition(1, 1).matches("^-1, .*"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(7).click();
            items[0].getNode(7).contextMenu().menu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            assertTrue(table.getCellDataValueByPosition(1, 1).matches("^18446744073709551615, .*"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if(tableShell != null) tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5ScalarAttribute() {
        String filename = "tscalarattrintsize";
        String file_ext = ".h5";
        String[] attribute_names = {"DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS", "DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS"};
        File hdf_file = openFile(filename, false);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("openHDF5ScalarAttribute filetree row count: " + filetree.visibleRowCount(), filetree.visibleRowCount()==1);
            assertTrue("openHDF5ScalarAttribute filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /");
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().tabItem("Attributes").activate();

            SWTBotTable attrTable = metaDataShell.bot().table();

            assertEquals(attrTable.cell(0, 0), attribute_names[0]);
            assertEquals(attrTable.cell(1, 0), attribute_names[1]);
            assertEquals(attrTable.cell(2, 0), attribute_names[2]);
            assertEquals(attrTable.cell(3, 0), attribute_names[3]);
            assertEquals(attrTable.cell(4, 0), attribute_names[4]);
            assertEquals(attrTable.cell(5, 0), attribute_names[5]);
            assertEquals(attrTable.cell(6, 0), attribute_names[6]);
            assertEquals(attrTable.cell(7, 0), attribute_names[7]);

            assertTrue(attrTable.cell(0, 1).matches("^. -1, .*"));
            assertTrue(attrTable.cell(1, 1).matches("^. -1, .*"));
            assertTrue(attrTable.cell(2, 1).matches("^. -1, .*"));
            assertTrue(attrTable.cell(3, 1).matches("^. -1, .*"));
            assertTrue(attrTable.cell(4, 1).matches("^. 255, .*"));
            assertTrue(attrTable.cell(5, 1).matches("^. 65535, .*"));
            assertTrue(attrTable.cell(6, 1).matches("^. 4294967295, .*"));
            assertTrue(attrTable.cell(7, 1).matches("^. 18446744073709551615, .*"));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
