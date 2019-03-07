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
import org.eclipse.swtbot.nebula.nattable.finder.widgets.Position;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCanvas;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import hdf.HDFVersions;
import hdf.view.HDFView;

public abstract class AbstractWindowTest {
    protected static String HDF5VERSION = "HDF5 " + HDFVersions.HDF5_VERSION;
    protected static String HDF4VERSION = "HDF " + HDFVersions.HDF4_VERSION;
    // the version of the HDFViewer
    protected static String VERSION = HDFVersions.HDFVIEW_VERSION;

    protected static String workDir = System.getProperty("hdfview.workdir");

    protected SWTBot bot;

    protected static Thread uiThread;

    protected static Shell shell;

    private static final CyclicBarrier swtBarrier = new CyclicBarrier(2);

    private static int TEST_DELAY = 10;

    private static int open_files = 0;

    protected static Rectangle monitorBounds;

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

    @BeforeClass
    public static void setupApp() {
        clearRemovePropertyFile();

        if (uiThread == null) {
            uiThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Vector<File> fList = new Vector<>();
                        String rootDir = System.getProperty("hdfview.workdir");
                        if (rootDir == null) rootDir = System.getProperty("user.dir");

                        int W = 800, H = 600, X = 0, Y = 0;

                        while (true) {
                            // open and layout the shell
                            HDFView window = new HDFView(rootDir);

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

    protected File createFile(String name, boolean hdf4_type) {
        String file_ext;
        String file_type;
        if (hdf4_type) {
            file_ext = new String(".hdf");
            file_type = new String("HDF4");
        }
        else {
            file_ext = new String(".h5");
            file_type = new String("HDF5");
        }

        File hdf_file = new File(workDir, name + file_ext);
        if (hdf_file.exists())
            hdf_file.delete();

        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("New").menu(file_type);
            fileMenuItem.click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();

            SWTBotText text = shell.bot().text();
            text.setText(name + file_ext);

            String val = text.getText();
            assertTrue("createFile() wrong file name: expected '" + name + file_ext + "' but was '" + val + "'",
                    val.equals(name + file_ext));

            shell.bot().button("   &OK   ").click();
            shell.bot().waitUntil(shellCloses(shell));

            assertTrue("createFile() File '" + hdf_file + "' not created", hdf_file.exists());
            open_files++;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }

        return hdf_file;
    }

    protected File createHDF4File(String name) {
        return createFile(name, true);
    }

    protected File createHDF5File(String name) {
        return createFile(name, false);
    }

    protected void closeFile(File hdfFile, boolean deleteFile) {
        try {
            SWTBotTree filetree = bot.tree();

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

            assertTrue(constructWrongValueMessage("closeFile()", "filetree wrong row count", String.valueOf(open_files - 1), String.valueOf(filetree.rowCount())),
                    filetree.rowCount() == open_files - 1);

            open_files--;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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

    /*
     * Utility function to compare a given table position against an expected value.
     * Note that this is performed in terms of NAT Table "positions", or essentially
     * 1-based offsets, with (1, 1) being the very top-left value in the table.
     */
    /*
     * TODO:
     */
    protected void testTableLocation(SWTBotNatTable table, int rowPosition, int columnPosition, String expectedValRegex, String funcName)
            throws IllegalArgumentException, AssertionError {
        if (table == null)
            throw new IllegalArgumentException("SWTBotNatTable parameter is null");
        if (expectedValRegex == null)
            throw new IllegalArgumentException("expected value string parameter is null");
        if (funcName == null)
            throw new IllegalArgumentException("function name parameter is null");

        /*
         * Most likely a mistake. The value at position (0, 0) should always be empty
         * since this position represents the empty corner block between the row and
         * column headers.
         *
         * However, values like (X >= 1, 0) and (0, X >= 1) can be used to test the
         * String value of the row/column headers respectively.
         */
        if (rowPosition == 0 && columnPosition == 0)
            throw new IllegalArgumentException("(0, 0) is an invalid table position");

        // TODO: temporary workaround until the solution below works.
        Position cellPos = table.scrollViewport(new Position(1, 1), rowPosition - 1, columnPosition - 1);
        table.click(cellPos.row, cellPos.column);
        String val = bot.shells()[1].bot().text(0).getText();

        // Disabled until Data conversion can be figured out
        // String val = table.getCellDataValueByPosition(rowPosition, columnPosition);

        String errMsg = constructWrongValueMessage(funcName, "wrong value", expectedValRegex, val);
        assertTrue(errMsg, val.matches(expectedValRegex));
    }

    /*
     * Utility function wrapper around testTableLocation() for testing an entire
     * table.
     */
    protected void testAllTableLocations(SWTBotNatTable table, String[][] expectedValRegexArray, String funcName)
            throws IllegalArgumentException, AssertionError {
        int arrLen = Array.getLength(expectedValRegexArray);
        for (int i = 0; i < arrLen; i++) {
            String[] nestedArray = (String[]) Array.get(expectedValRegexArray, i);
            int nestedLen = Array.getLength(nestedArray);

            for (int j = 0; j < nestedLen; j++)
                testTableLocation(table, i + 1, j + 1, (String) Array.get(nestedArray, j), funcName);
        }
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

    protected SWTBotShell openDataObject(SWTBotTree tree, String filename, String objectName) {
        SWTBotTreeItem fileItem = tree.getTreeItem(filename);

        SWTBotTreeItem foundObject = locateItemByPath(fileItem, objectName);
        foundObject.click();
        foundObject.contextMenu("Open").click();

        String strippedObjectName = objectName;
        int slashLoc = objectName.lastIndexOf('/');
        if (slashLoc >= 0) {
            strippedObjectName = objectName.substring(slashLoc + 1);
        }

        Matcher<Shell> classMatcher = widgetOfType(Shell.class);
        Matcher<Shell> regexMatcher = withRegex(strippedObjectName + objectShellTitleRegex);
        Matcher<Shell> shellMatcher = allOf(classMatcher, regexMatcher);
        bot.waitUntil(Conditions.waitForShell(shellMatcher));

        final SWTBotShell botShell = new SWTBotShell(bot.widget(shellMatcher));

        botShell.activate();
        bot.waitUntil(Conditions.shellIsActive(botShell.getText()));

        /*
         * Due to testing issues where the values can't be retrieved from non-visible
         * table columns, we ensure that the table Shell is always maximized.
         */
//        Display.getDefault().syncExec(new Runnable() {
//            @Override
//            public void run() {
//                botShell.widget.setMaximized(true);
//            }
//        });

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

    protected SWTBotNatTable getNatTable(SWTBotShell theShell) {
        return new SWTBotNatTable(theShell.bot().widget(widgetOfType(NatTable.class)));
    }

    protected String constructWrongValueMessage(String methodName, String message, String expected, String actual) {
        return methodName.concat(" " + message + ": expected '" + expected + "' but was '" + actual + "'");
    }
}
