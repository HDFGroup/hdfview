package uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

/**
 * Tests the Next Frame, Previous Frame, First Frame and Last Frame buttons of TableView. Also tests
 * manual page selection.
 *
 * NatTable indices take the table row and column header into account. Therefore, to get the cell
 * data value for the cell at (row, col), table.getCellDataValueByPosition(row + 1, col + 1) must be
 * called.
 *
 * @author Jordan Henderson
 */
public class TestHDFViewDatasetFrameSelection extends AbstractWindowTest {
    private String filename = "tframeselection.h5";
    private String dataset_name = "test_dataset";

    @Test
    public void testNextFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testNextFrame()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("testNextFrame() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("testNextFrame() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            try {
                assertTrue(tableShell.bot().text(0).getText().equals("0"));
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);

                text.setText("0");

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }

            String val = table.getCellDataValueByPosition(5, 3);
            assertTrue(constructWrongValueMessage("testNextFrame()", "wrong data", "478", val), val.equals("478"));

            val = table.getCellDataValueByPosition(2, 4);
            assertTrue(constructWrongValueMessage("testNextFrame()", "wrong data", "52", val), val.equals("52"));

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testNextFrame()", "frame field shows wrong value", "1", val), val.equals("1"));

            val = table.getCellDataValueByPosition(4, 5);
            assertTrue(constructWrongValueMessage("testNextFrame()", "wrong data", "454", val), val.equals("454"));

            val = table.getCellDataValueByPosition(3, 2);
            assertTrue(constructWrongValueMessage("testNextFrame()", "wrong data", "984", val), val.equals("984"));
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
    public void testPreviousFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testPreviousFrame()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("testPreviousFrame() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("testPreviousFrame() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            try {
                assertTrue(tableShell.bot().text(0).getText().equals("1"));
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);

                text.setText("1");

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }

            String val = table.getCellDataValueByPosition(4, 2);
            assertTrue(constructWrongValueMessage("testPreviousFrame()", "wrong data", "6", val), val.equals("6"));

            val = table.getCellDataValueByPosition(5, 3);
            assertTrue(constructWrongValueMessage("testPreviousFrame()", "wrong data", "215", val), val.equals("215"));

            tableShell.bot().toolbarButtonWithTooltip("Previous Frame").click();

            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testPreviousFrame()", "frame field shows wrong value", "0", val), val.equals("0"));

            val = table.getCellDataValueByPosition(1, 1);
            assertTrue(constructWrongValueMessage("testPreviousFrame()", "wrong data", "13", val), val.equals("13"));

            val = table.getCellDataValueByPosition(5, 5);
            assertTrue(constructWrongValueMessage("testPreviousFrame()", "wrong data", "4", val), val.equals("4"));
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
    public void testFirstFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testFirstFrame()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("testFirstFrame() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("testFirstFrame() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            try {
                assertTrue(tableShell.bot().text(0).getText().equals("0"));
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);

                text.setText("0");

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }

            for (int i = 0; i < 3; i++)
                tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            String val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testFirstFrame()", "frame field shows wrong value", "3", val), val.equals("3"));

            val = table.getCellDataValueByPosition(4, 2);
            assertTrue(constructWrongValueMessage("testFirstFrame()", "wrong data", "456", val), val.equals("456"));

            val = table.getCellDataValueByPosition(1, 3);
            assertTrue(constructWrongValueMessage("testFirstFrame()", "wrong data", "7", val), val.equals("7"));

            tableShell.bot().toolbarButtonWithTooltip("First Frame").click();

            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testFirstFrame()", "frame field shows wrong value", "0", val), val.equals("0"));

            val = table.getCellDataValueByPosition(4, 5);
            assertTrue(constructWrongValueMessage("testFirstFrame()", "wrong data", "52", val), val.equals("52"));

            val = table.getCellDataValueByPosition(5, 2);
            assertTrue(constructWrongValueMessage("testFirstFrame()", "wrong data", "345", val), val.equals("345"));
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
    public void testLastFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testLastFrame()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("testLastFrame() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("testLastFrame() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            try {
                assertTrue(tableShell.bot().text(0).getText().equals("0"));
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);

                text.setText("0");

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }

            String val = table.getCellDataValueByPosition(3, 4);
            assertTrue(constructWrongValueMessage("testLastFrame()", "wrong data", "63", val), val.equals("63"));

            val = table.getCellDataValueByPosition(2, 1);
            assertTrue(constructWrongValueMessage("testLastFrame()", "wrong data", "2", val), val.equals("2"));

            tableShell.bot().toolbarButtonWithTooltip("Last Frame").click();

            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testLastFrame()", "frame field shows wrong value", "4", val), val.equals("4"));

            val = table.getCellDataValueByPosition(1, 5);
            assertTrue(constructWrongValueMessage("testLastFrame()", "wrong data", "789", val), val.equals("789"));

            val = table.getCellDataValueByPosition(5, 2);
            assertTrue(constructWrongValueMessage("testLastFrame()", "wrong data", "7945", val), val.equals("7945"));
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
    public void testEnterFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testEnterFrame()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("testEnterFrame() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("testEnterFrame() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            try {
                assertTrue(tableShell.bot().text(0).getText().equals("0"));
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);

                text.setText("0");

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }

            String val = table.getCellDataValueByPosition(4, 3);
            assertTrue(constructWrongValueMessage("testEnterFrame()", "wrong data", "99", val), val.equals("99"));

            val = table.getCellDataValueByPosition(5, 4);
            assertTrue(constructWrongValueMessage("testEnterFrame()", "wrong data", "86", val), val.equals("86"));

            final SWTBotText text = tableShell.bot().text(0);
            text.setText("3");

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    text.widget.notifyListeners(SWT.Traverse, new Event() {
                        {
                            detail = SWT.TRAVERSE_RETURN;
                        }
                    });
                }
            });

            val = table.getCellDataValueByPosition(3, 2);
            assertTrue(constructWrongValueMessage("testEnterFrame()", "wrong data", "63", val), val.equals("63"));

            val = table.getCellDataValueByPosition(1, 3);
            assertTrue(constructWrongValueMessage("testEnterFrame()", "wrong data", "7", val), val.equals("7"));

            text.setText("2");

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    text.widget.notifyListeners(SWT.Traverse, new Event() {
                        {
                            detail = SWT.TRAVERSE_RETURN;
                        }
                    });
                }
            });

            val = table.getCellDataValueByPosition(3, 2);
            assertTrue(constructWrongValueMessage("testEnterFrame()", "wrong data", "88", val), val.equals("88"));

            val = table.getCellDataValueByPosition(1, 3);
            assertTrue(constructWrongValueMessage("testEnterFrame()", "wrong data", "66", val), val.equals("66"));
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
