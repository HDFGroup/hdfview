package test.uitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.LayerCommandUtil;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.nebula.widgets.nattable.viewport.command.ShowColumnInViewportCommand;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;

import static org.hamcrest.Matcher.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.Ignore;

public class TestTreeViewFilters extends AbstractWindowTest {
    @Ignore
    public void openHDF5Filters() {
        String filename = "tfilters";
        String file_ext = ".h5";
        String filtername = "fletcher32";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5Filters()", "filetree wrong row count", "17", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==17);
            assertTrue("openHDF5Filters() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5Filters() filetree is missing dataset '" + filtername + "'", items[0].getNode(9).getText().compareTo(filtername)==0);

            items[0].getNode(13).click();
            items[0].getNode(13).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex("scaleoffset" + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            NatTable ntable = tableShell.bot().widget(widgetOfType(NatTable.class));
            SWTBotNatTable table = new SWTBotNatTable(ntable);

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5Filters()", "wrong data", "0", val),
                    val.equals("0"));

            //SelectionLayer selectionLayer = ((GridLayer)ntable.getLayer()).getBodyLayer().getSelectionLayer();
            //SelectionLayer selectionLayer = ((BodyLayerStack)((GridLayer)nTable.getLayer()).getBodyLayer()).getSelectionLayer();
            //ViewportLayer viewportLayer = new ViewportLayer(ntable.getViewportLayer());
            //ntable.getUnderlyingLayer().doCommand(new ShowColumnInViewportCommand(selectionLayer, 10));
            table.click(1, 10);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5Filters()", "wrong data", "9", val),
                    val.equals("9"));

            table.click(12, 5);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5Filters()", "wrong data", "114", val),
                    val.equals("114"));

            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(14).click();
            items[0].getNode(14).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex("shuffle" + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(10, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5Filters()", "wrong data", "91", val),
                    val.equals("91"));

            table.click(20, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5Filters()", "wrong data", "191", val),
                    val.equals("191"));

            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void checkHDF5Filters() {
        String filename = "tfilters";
        String file_ext = ".h5";
        String filtername = "fletcher32";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "filetree wrong row count", "17", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==17);
            assertTrue("checkHDF5Filters() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5Filters() filetree is missing dataset '" + filtername + "'", items[0].getNode(9).getText().compareTo(filtername)==0);

            items[0].getNode(10).setFocus();
            items[0].getNode(10).click();

            SWTBotTabItem tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            String val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "USERDEFINED myfilter(405): 5, 6", val),
                    val.equals("USERDEFINED myfilter(405): 5, 6"));

            items[0].getNode(14).setFocus();
            items[0].getNode(14).click();

            tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
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
            catch (Exception ex) {}
        }
    }
}
