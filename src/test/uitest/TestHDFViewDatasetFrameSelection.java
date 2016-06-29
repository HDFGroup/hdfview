package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import java.io.File;

import org.junit.Test;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Tests the Next Page, Previous Page, First Page and Last Page buttons of TableView.
 * Also tests manual page selection.
 * 
 * NatTable indices take the table row and column header into account.
 * Therefore, to get the cell data value for the cell at (row, col),
 * table.getCellDataValueByPosition(row + 1, col + 1) must be called.
 * 
 * @author Jordan Henderson
 */
public class TestHDFViewDatasetFrameSelection extends AbstractWindowTest {
    private String filename = "tframeselection";
    private String file_ext = ".h5";
    private String dataset_name = "test_dataset";
    
    @Test
    public void testNextFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, false);
        
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            
            assertTrue("testNextFrame filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==2);
            assertTrue("testNextFrame filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("testNextFrame filetree missing dataset ", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            
            items[0].getNode(0).click();
            filetree.contextMenu("Open").click();
            
            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            
            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            
            try {
                assertEquals(tableShell.bot().text(0).getText(), "0");
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);
                
                text.setText("0");
                
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }
            
            assertEquals(table.getCellDataValueByPosition(5, 3), "478");
            assertEquals(table.getCellDataValueByPosition(2, 4), "52");
            
            tableShell.bot().toolbarButtonWithTooltip("Next Page").click();
            
            assertEquals(tableShell.bot().text(0).getText(), "1");
            assertEquals(table.getCellDataValueByPosition(4, 5), "454");
            assertEquals(table.getCellDataValueByPosition(3, 2), "984");
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
            catch (Exception ex) {}
        }
    }
    
    @Test
    public void testPreviousFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, false);
        
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            
            assertTrue("testNextFrame filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==2);
            assertTrue("testNextFrame filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("testNextFrame filetree missing dataset ", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            
            items[0].getNode(0).click();
            filetree.contextMenu("Open").click();
            
            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            
            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            
            try {
                assertEquals(tableShell.bot().text(0).getText(), "1");
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);
                
                text.setText("1");
                
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }
            
            assertEquals(table.getCellDataValueByPosition(4, 2), "6");
            assertEquals(table.getCellDataValueByPosition(5, 3), "215");
            
            tableShell.bot().toolbarButtonWithTooltip("Previous Page").click();
            
            assertEquals(tableShell.bot().text(0).getText(), "0");
            assertEquals(table.getCellDataValueByPosition(1, 1), "13");
            assertEquals(table.getCellDataValueByPosition(5, 5), "4");
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
            catch (Exception ex) {}
        }
    }
    
    @Test
    public void testFirstFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, false);
        
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            
            assertTrue("testNextFrame filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==2);
            assertTrue("testNextFrame filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("testNextFrame filetree missing dataset ", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            
            items[0].getNode(0).click();
            filetree.contextMenu("Open").click();
            
            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            
            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            
            try {
                assertEquals(tableShell.bot().text(0).getText(), "0");
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);
                
                text.setText("0");
                
                Display.getDefault().syncExec(new Runnable() {
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
                tableShell.bot().toolbarButtonWithTooltip("Next Page").click();
            
            assertEquals(tableShell.bot().text(0).getText(), "3");
            
            assertEquals(table.getCellDataValueByPosition(4, 2), "456");
            assertEquals(table.getCellDataValueByPosition(1, 3), "7");
            
            tableShell.bot().toolbarButtonWithTooltip("First Page").click();
            
            assertEquals(tableShell.bot().text(0).getText(), "0");
            assertEquals(table.getCellDataValueByPosition(4, 5), "52");
            assertEquals(table.getCellDataValueByPosition(5, 2), "345");
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
            catch (Exception ex) {}
        }
    }
    
    @Test
    public void testLastFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, false);
        
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            
            assertTrue("testNextFrame filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==2);
            assertTrue("testNextFrame filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("testNextFrame filetree missing dataset ", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            
            items[0].getNode(0).click();
            filetree.contextMenu("Open").click();
            
            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            
            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            
            try {
                assertEquals(tableShell.bot().text(0).getText(), "0");
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);
                
                text.setText("0");
                
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }
            
            assertEquals(table.getCellDataValueByPosition(3, 4), "63");
            assertEquals(table.getCellDataValueByPosition(2, 1), "2");
            
            tableShell.bot().toolbarButtonWithTooltip("Last Page").click();
            
            assertEquals(tableShell.bot().text(0).getText(), "4");
            assertEquals(table.getCellDataValueByPosition(1, 5), "789");
            assertEquals(table.getCellDataValueByPosition(5, 2), "7945");
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
            catch (Exception ex) {}
        }
    }
    
    @Test
    public void testEnterFrame() {
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, false);
        
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            
            assertTrue("testNextFrame filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==2);
            assertTrue("testNextFrame filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("testNextFrame filetree missing dataset ", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            
            items[0].getNode(0).click();
            filetree.contextMenu("Open").click();
            
            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            
            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            
            try {
                assertEquals(tableShell.bot().text(0).getText(), "0");
            }
            catch (AssertionError e) {
                final SWTBotText text = tableShell.bot().text(0);
                
                text.setText("0");
                
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        text.widget.notifyListeners(SWT.Traverse, new Event() {
                            {
                                detail = SWT.TRAVERSE_RETURN;
                            }
                        });
                    }
                });
            }
            
            assertEquals(table.getCellDataValueByPosition(4, 3), "99");
            assertEquals(table.getCellDataValueByPosition(5, 4), "86");
            
            final SWTBotText text = tableShell.bot().text(0);
            text.setText("3");
            
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    text.widget.notifyListeners(SWT.Traverse, new Event() {
                        {
                            detail = SWT.TRAVERSE_RETURN;
                        }
                    });
                }
            });
            
            assertEquals(table.getCellDataValueByPosition(3, 2), "63");
            assertEquals(table.getCellDataValueByPosition(1, 3), "7");
            
            text.setText("2");
            
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    text.widget.notifyListeners(SWT.Traverse, new Event() {
                        {
                            detail = SWT.TRAVERSE_RETURN;
                        }
                    });
                }
            });
            
            assertEquals(table.getCellDataValueByPosition(3, 2), "88");
            assertEquals(table.getCellDataValueByPosition(1, 3), "66");
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
            catch (Exception ex) {}
        }
    }
}
