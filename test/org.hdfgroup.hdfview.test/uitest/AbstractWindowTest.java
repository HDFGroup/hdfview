/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see https://support.hdfgroup.org/products/licenses.html               *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withRegex;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Array;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.Position;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCanvas;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import hdf.HDFVersions;
import hdf.view.HDFView;

public abstract class AbstractWindowTest {
    protected static String HDF5VERSION = "HDF5 " + HDFVersions.getPropertyVersionHDF5();
    protected static String HDF4VERSION = "HDF " + HDFVersions.getPropertyVersionHDF4();
    // the version of the HDFViewer
    protected static String VERSION = HDFVersions.getPropertyVersionView();

    protected static String workDir = System.getProperty("hdfview.workdir");

    protected static SWTBot bot;

    protected static Thread uiThread;

    protected static Shell shell;

    private static final CyclicBarrier swtBarrier = new CyclicBarrier(2);

    private static int TEST_DELAY = 0;

    private static int open_files = 0;

    protected static Rectangle monitorBounds;

    @Rule public TestName testName = new TestName();

    protected static enum FILE_MODE {
        READ_ONLY, READ_WRITE
    }

    private static final String objectShellTitleRegex = ".*at.*\\[.*in.*\\]";

    @Before
    public final void setupSWTBot() throws InterruptedException, BrokenBarrierException {
        // synchronize with the thread opening the shell
        swtBarrier.await();
        bot = new SWTBot();

        SWTBotPreferences.PLAYBACK_DELAY = TEST_DELAY;
    }

    @After
    public void closeShell() throws InterruptedException {
        // close the shell
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                shell.close();
            }
        });
    }

    @After
    public void checkOpenFiles() {
        if (open_files > 0) {
            String failMsg = "Test " + testName.getMethodName() + " still had " + open_files + " files open!";

            open_files = 0;

            fail(failMsg);
        }

        open_files = 0;
    }

    @BeforeClass
    public static void setupApp() {
        clearRemovePropertyFile();

        if (uiThread == null) {
            uiThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Vector<File> fList = new Vector<>();
                        String rootDir = System.getProperty("hdfview.rootdir");
                        if (rootDir == null) rootDir = System.getProperty("user.dir");
                        String startDir = System.getProperty("hdfview.workdir");

                        int W = 800, H = 600, X = 0, Y = 0;

                        while (true) {
                            // open and layout the shell
                            HDFView window = new HDFView(rootDir, startDir);

                            // Set the testing state to handle the problem with testing
                            // of native dialogs
                            window.setTestState(true);

                            shell = window.openMainWindow(fList, W, H, X, Y);

                            // Force the HDFView window to open fullscreen on the active
                            // monitor so that certain tests don't encounter weird issues
                            // due to the window being too small
                            Monitor[] monitors = shell.getDisplay().getMonitors();
                            Monitor activeMonitor = null;

                            Rectangle r = shell.getBounds();
                            for (int i = 0; i < monitors.length; i++) {
                                if (monitors[i].getBounds().intersects(r)) {
                                    activeMonitor = monitors[i];
                                }
                            }

                            monitorBounds = activeMonitor.getBounds();

                            shell.setBounds(monitorBounds);

                            // wait for the test setup
                            swtBarrier.await();

                            // run the event loop
                            window.runMainWindow();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            shell.getDisplay().dispose();
                        }
                    });
                }
            });
            uiThread.setDaemon(true);
            uiThread.start();
        }
    }

    private static void clearRemovePropertyFile() {
        // the local property file name
        // look for the property file at the use home directory
        String fn = ".hdfview" + VERSION;

        File prop_file = new File(workDir, fn);
        if (prop_file.exists()) {
            prop_file.delete();
        }
    }

    protected File openFile(String name, FILE_MODE openMode) {
        SWTBotShell fileNameShell = null;
        File hdf_file = new File(workDir, name);

        try {
            if (openMode == FILE_MODE.READ_ONLY)
                bot.menu("Open As").menu("Read-Only").click();
            else
                bot.menu("Open As").menu("Read/Write").click();

            fileNameShell = bot.shell("Enter a file name");
            fileNameShell.activate();

            SWTBotText text = fileNameShell.bot().text();
            text.setText(hdf_file.getName());

            String val = text.getText();
            assertTrue("openFile() wrong file name: expected '" + hdf_file.getName() + "' but was '" + val + "'",
                    val.equals(hdf_file.getName()));

            fileNameShell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(fileNameShell));

            SWTBotTree filetree = bot.tree();
            bot.waitUntil(Conditions.treeHasRows(filetree, open_files + 1));

            /*
             * TODO: difference here between rowCount() and visibleRowCount(). Can't use
             * checkFileTree().
             */
            assertTrue("openFile() filetree wrong row count: expected '" + String.valueOf(open_files) + "' but was '"
                    + filetree.rowCount() + "'", filetree.rowCount() == open_files + 1);
            assertTrue("openFile() filetree is missing file '" + hdf_file.getName() + "'",
                    filetree.getAllItems()[open_files].getText().compareTo(hdf_file.getName()) == 0);

            /*
             * Increment the open_files value last, in case an error occurs when opening a
             * file.
             */
            open_files++;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if (fileNameShell != null && fileNameShell.isOpen())
                fileNameShell.close();
        }

        return hdf_file;
    }

    protected File createFile(String name) {
        boolean hdf4Type = (name.lastIndexOf(".hdf") >= 0);
        boolean hdf5Type = (name.lastIndexOf(".h5") >= 0);

        File hdfFile = new File(workDir, name);
        if (hdfFile.exists())
            hdfFile.delete();

        try {
            SWTBotMenu fileMenuItem;

            if (hdf4Type)
                fileMenuItem = bot.menu("File").menu("New").menu("HDF4");
            else if (hdf5Type)
                fileMenuItem = bot.menu("File").menu("New").menu("HDF5");
            else
                throw new IllegalArgumentException("unknown file type");

            fileMenuItem.click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();

            SWTBotText text = shell.bot().text();
            text.setText(name);

            String val = text.getText();
            assertTrue("createFile() wrong file name: expected '" + name + "' but was '" + val + "'", val.equals(name));

            shell.bot().button("   &OK   ").click();
            shell.bot().waitUntil(shellCloses(shell));

            assertTrue("createFile() File '" + hdfFile + "' not created", hdfFile.exists());
            open_files++;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }

        return hdfFile;
    }

    protected void closeFile(File hdfFile, boolean deleteFile) {
        try {
            SWTBotTree filetree = bot.tree();

            filetree.select(hdfFile.getName());
            filetree.getTreeItem(hdfFile.getName()).click();

            bot.shells()[0].activate();
            bot.waitUntil(Conditions.shellIsActive(bot.shells()[0].getText()));

            bot.menu("File").menu("Close").click();

            if (deleteFile) {
                if (hdfFile.exists()) {
                    assertTrue("closeFile() File '" + hdfFile + "' not deleted", hdfFile.delete());
                    assertFalse("closeFile() File '" + hdfFile + "' not gone", hdfFile.exists());
                }
            }

            if (open_files > 0) {
                assertTrue(constructWrongValueMessage("closeFile()", "filetree wrong row count", String.valueOf(open_files - 1), String.valueOf(filetree.rowCount())),
                    filetree.rowCount() == open_files - 1);

                open_files--;
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
    }

    protected void checkFileTree(SWTBotTree tree, String funcName, int expectedRowCount, String filename)
            throws IllegalArgumentException, AssertionError {
        if (tree == null)
            throw new IllegalArgumentException("SWTBotTree parameter is null");
        if (filename == null)
            throw new IllegalArgumentException("file name parameter is null");

        String expectedRowCountStr = String.valueOf(expectedRowCount);
        int visibleRowCount = tree.visibleRowCount();
        assertTrue(constructWrongValueMessage(funcName, "filetree wrong row count", expectedRowCountStr,
                String.valueOf(visibleRowCount)), visibleRowCount == expectedRowCount);

        String curFilename = tree.getAllItems()[0].getText();
        assertTrue(constructWrongValueMessage(funcName, "filetree is missing file", filename, curFilename),
                curFilename.compareTo(filename) == 0);
    }

    protected void testSamplePixel(final int theX, final int theY, String requiredValue) {
        try {
            SWTBotShell botshell = bot.activeShell();
            SWTBot thisbot = botshell.bot();
            final SWTBotCanvas imageCanvas = thisbot.canvas(1);

            // Make sure Show Values is selected
            SWTBotMenu showValuesMenuItem = thisbot.menu("Image").menu("Show Values");
            if(!showValuesMenuItem.isChecked()) {
                showValuesMenuItem.click();
            }

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    imageCanvas.widget.notifyListeners(SWT.MouseMove, new Event() {
                        {
                            x = theX;
                            y = theY;
                        }
                    });
                }
            });

            String val = thisbot.text().getText();
            assertTrue(constructWrongValueMessage("testSamplePixel()", "wrong pixel value", requiredValue, val), val.equals(requiredValue));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    protected SWTBotTable openAttributeTable(SWTBotTree tree, String filename, String objectName) {
        SWTBotTreeItem fileItem = tree.getTreeItem(filename);

        SWTBotTreeItem foundObject = locateItemByPath(fileItem, objectName);
        foundObject.click();

        SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
        tabItem.activate();

        return new SWTBotTable(bot.widget(widgetOfType(Table.class)));
    }

    protected SWTBotTabItem openMetadataTab(SWTBotTree tree, String filename, String objectName, String tabName) {
        SWTBotTreeItem fileItem = tree.getTreeItem(filename);

        SWTBotTreeItem foundObject = locateItemByPath(fileItem, objectName);
        foundObject.click();

        return bot.tabItem(tabName);
    }

    protected SWTBotShell openAttributeObject(SWTBotTable attrTable, String objectName, int rowIndex) {
        attrTable.doubleClick(rowIndex, 0);

        return openDataObject(objectName);
    }

    protected SWTBotShell openAttributeContext(SWTBotTable attrTable, String objectName, int rowIndex) {
        attrTable.click(rowIndex, 0);
        attrTable.contextMenu("View/Edit Attribute Value").click();

        return openDataObject(objectName);
    }

    protected SWTBotShell openTreeviewObject(SWTBotTree tree, String filename, String objectName) {
        SWTBotTreeItem fileItem = tree.getTreeItem(filename);

        SWTBotTreeItem foundObject = locateItemByPath(fileItem, objectName);
        foundObject.click();
        foundObject.contextMenu("Open").click();

        return openDataObject(objectName);
    }

    private SWTBotShell openDataObject(String objectName) {
        String strippedObjectName = objectName;
        int slashLoc = objectName.lastIndexOf('/');
        if (slashLoc >= 0) {
            strippedObjectName = objectName.substring(slashLoc + 1);
        }

        Matcher<Shell> classMatcher = widgetOfType(Shell.class);
        Matcher<Shell> regexMatcher = withRegex(strippedObjectName + objectShellTitleRegex);
        @SuppressWarnings("unchecked")
        Matcher<Shell> shellMatcher = allOf(classMatcher, regexMatcher);
        bot.waitUntil(Conditions.waitForShell(shellMatcher));

        final SWTBotShell botShell = new SWTBotShell(bot.widget(shellMatcher));

        botShell.activate();
        bot.waitUntil(Conditions.shellIsActive(botShell.getText()));

        /*
         * Due to testing issues where the values can't be retrieved from non-visible
         * table columns, we ensure that the table Shell is always maximized.
         */
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                botShell.widget.setMaximized(true);
            }
        });

        return botShell;
    }

    private SWTBotTreeItem locateItemByPath(SWTBotTreeItem startNode, String objPath) {
        StringTokenizer st = new StringTokenizer(objPath, "/");
        SWTBotTreeItem node = startNode;

        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            node = node.getNode(nextToken);
            if (node.getItems().length > 0)
                node.expand();
        }

        return node;
    }

    /*
     * A factory class to return concrete instances of TableDataRetriever classes,
     * which will retrieve the data value at the specified row and column position
     * in the given Table object.
     */
    public static class DataRetrieverFactory {

        public static TableDataRetriever getTableDataRetriever(AbstractSWTBot<?> tableObject, String funcName) {
            if (tableObject == null)
                throw new IllegalArgumentException("AbstractSWTBot parameter is null");
            if (funcName == null)
                throw new IllegalArgumentException("function name parameter is null");

            if (tableObject instanceof SWTBotNatTable)
                return new NatTableDataRetriever((SWTBotNatTable) tableObject, funcName);
            else
                return new SWTTableDataRetriever((SWTBotTable) tableObject, funcName);
        }

        public static class TableDataRetriever {

            protected final StringBuilder sb;

            protected final String funcName;

            public TableDataRetriever(String funcName) {
                this.funcName = funcName;

                this.sb = new StringBuilder();
            }

            /*
             * Utility function to offset the table row position for extra header info.
             */
            public void setContainerHeaderOffset(int containerHeaderOffset) {
                throw new UnsupportedOperationException("subclasses must implement setContainerHeaderOffset()");
            }

            public void setPagingActive(boolean pagingActive) {
                throw new UnsupportedOperationException("subclasses must implement setPagingActive()");
            }

            /*
             * Utility function to compare a given table position against an expected value.
             */
            public void testTableLocation(int rowIndex, int colIndex, String expectedValRegex) {
                throw new UnsupportedOperationException("subclasses must implement testTableLocation()");
            }

            /*
             * Utility function wrapper around testTableLocations() for testing an entire
             * table.
             */
            public void testAllTableLocations(String[][] expectedValRegexArray) {
                testTableLocations(0, 0, expectedValRegexArray);
            }

            public void testTableLocations(int rowOffset, int colOffset, String[][] expectedValRegexArray) {
                int arrLen = Array.getLength(expectedValRegexArray);
                for (int i = 0; i < arrLen; i++) {
                    String[] nestedArray = (String[]) Array.get(expectedValRegexArray, i);
                    int nestedLen = Array.getLength(nestedArray);

                    for (int j = 0; j < nestedLen; j++)
                        testTableLocation(rowOffset + i, colOffset + j, (String) Array.get(nestedArray, j));
                }
            }

        }

        private static class NatTableDataRetriever extends TableDataRetriever {

            private final SWTBotNatTable table;
            private int containerHeaderOffset = 0;
            boolean pagingActive = false;

            NatTableDataRetriever(SWTBotNatTable tableObj, String funcName) {
                super(funcName);

                this.table = tableObj;
            }

            @Override
            public void testTableLocation(int rowIndex, int colIndex, String expectedValRegex) {
                if (expectedValRegex == null)
                    throw new IllegalArgumentException("expected value string parameter is null");

                int textboxIndex = 0;
                if (pagingActive) textboxIndex = 2;

                // TODO: temporary workaround until the solution below works.
                Position cellPos = table.scrollViewport(new Position(1 + containerHeaderOffset, 1), rowIndex, colIndex);
                table.click(cellPos.row, cellPos.column);
                String val = bot.shells()[1].bot().text(textboxIndex).getText();

                // Disabled until Data conversion can be figured out
                // String val = table.getCellDataValueByPosition(rowPosition, columnPosition);

                sb.setLength(0);
                sb.append("wrong value at table index ").append("(").append(rowIndex).append(", ").append(colIndex).append(")");
                String errMsg = constructWrongValueMessage(funcName, sb.toString(), expectedValRegex, val);
                assertTrue(errMsg, val.matches(expectedValRegex));
            }

            @Override
            public void setPagingActive(boolean pagingActive) {
                this.pagingActive = pagingActive;
            }

            @Override
            public void setContainerHeaderOffset(int containerHeaderOffset) {
                this.containerHeaderOffset = containerHeaderOffset;
            }

        }

        private static class SWTTableDataRetriever extends TableDataRetriever {

            private final SWTBotTable table;

            SWTTableDataRetriever(SWTBotTable tableObj, String funcName) {
                super(funcName);

                this.table = tableObj;
            }

            @Override
            public void testTableLocation(int rowIndex, int colIndex, String expectedValRegex) {
                if (expectedValRegex == null)
                    throw new IllegalArgumentException("expected value string parameter is null");

                table.click(rowIndex, colIndex);
                String val = table.cell(rowIndex, colIndex);

                sb.setLength(0);
                sb.append("wrong value at table index ").append("(").append(rowIndex).append(", ").append(colIndex).append(")");
                String errMsg = constructWrongValueMessage(funcName, sb.toString(), expectedValRegex, val);
                assertTrue(errMsg, val.matches(expectedValRegex));
            }
        }
    }

    protected SWTBotNatTable getNatTable(SWTBotShell theShell) {
        return new SWTBotNatTable(theShell.bot().widget(widgetOfType(NatTable.class)));
    }

    protected final void closeShell(SWTBotShell theShell) {
        if (theShell == null || !theShell.isOpen()) return;

        SWTBotMenu closeButton = null;
        try {
            closeButton = theShell.bot().menu("Close");
        }
        catch (WidgetNotFoundException ex) {
            closeButton = null;
        }

        if (closeButton != null) {
            closeButton.click();
            bot.waitUntil(Conditions.shellCloses(theShell));
        }
    }

    /*
     * Only useful when testing certain Menu items which open files in a different
     * manner than the openFile() method.
     */
    protected final void refreshOpenFileCount() {
        open_files = bot.tree().getAllItems().length;
    }

    /*
     * Only useful when testing certain Menu items which close files in a different
     * manner than the closeFile() method.
     */
    protected final void resetOpenFileCount() {
        open_files = 0;
    }

    protected static String constructWrongValueMessage(String methodName, String message, String expected, String actual) {
        StringBuilder builder = new StringBuilder(methodName);
        builder.append(" " + message + ": expected '" + expected + "' but was '" + actual + "'");

        if (expected.equals(actual))
            builder.append(" - possible regex mismatch due to non-escaped characters \\^${}[]()*+?|<>-&");

        return builder.toString();
    }
}
