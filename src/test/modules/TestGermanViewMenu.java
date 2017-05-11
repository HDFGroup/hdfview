package test.modules;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCanvas;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import hdf.HDFVersions;
import hdf.view.HDFView;

//@RunWith(SWTBotJunit4ClassRunner.class)
public class TestGermanViewMenu {
    protected static String HDF5VERSION = "HDF5 " + HDFVersions.HDF5_VERSION;
    protected static String HDF4VERSION = "HDF " + HDFVersions.HDF4_VERSION;
    // the version of the HDFViewer
    protected static String VERSION = HDFVersions.HDFVIEW_VERSION;

    protected static String workDir = System.getProperty("hdfview.workdir");

    protected SWTBot bot;

    protected static Thread uiThread;

    protected static Shell shell;

    private final static CyclicBarrier swtBarrier = new CyclicBarrier(2);

    private static int TEST_DELAY = 0;

    private static int open_files = 0;


    private static void clearRemovePropertyFile() {
        // the local property file name
        // look for the property file at the use home directory
        String fn = ".hdfview" + VERSION;

        File prop_file = new File(workDir, fn);
        if (prop_file.exists()) {
            prop_file.delete();
        }
    }

    protected File openFile(String name, boolean hdf4_type) {
        String file_ext;
        if (hdf4_type) {
            file_ext = new String(".hdf");
        }
        else {
            file_ext = new String(".h5");
        }

        File hdf_file = new File(workDir, name + file_ext);

        try {
            bot.toolbarButtonWithTooltip("Open").click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();

            SWTBotText text = shell.bot().text();
            text.setText(hdf_file.getName());

            String val = text.getText();
            assertTrue("openFile() wrong file name: expected '" + hdf_file.getName() + "' but was '" + val + "'",
                    val.equals(hdf_file.getName()));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            open_files++;
            assertTrue("openFile() filetree wrong row count: expected '" + String.valueOf(open_files) + "' but was '" + filetree.rowCount() + "'", filetree.rowCount() == open_files);
            assertTrue("openFile() filetree is missing file '" + hdf_file.getName() + "'", items[open_files - 1].getText().compareTo(hdf_file.getName()) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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

    protected void closeFile(File hdf_file, boolean delete_file) {
        //TODO: re-implement to only close given file, not all files
        try {
            bot.shells()[0].activate();
            bot.waitUntil(Conditions.shellIsActive(bot.shells()[0].getText()));

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close All");
            fileMenuItem.click();

            if(delete_file) {
                if (hdf_file.exists()) {
                    assertTrue("closeFile() File '" + hdf_file + "' not deleted", hdf_file.delete());
                    assertFalse("closeFile() File '" + hdf_file + "' not gone", hdf_file.exists());
                }
            }

            SWTBotTree filetree = bot.tree();
            assertTrue("closeFile() filetree wrong row count: expected '0' but was '" + filetree.rowCount() + "'", filetree.rowCount() == 0);

            open_files = 0;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    protected String constructWrongValueMessage(String methodName, String message, String expected, String actual) {
        return methodName.concat(" " + message + ": expected '" + expected + "' but was '" + actual + "'");
    }

    @BeforeClass
    public static void setupApp() {
        clearRemovePropertyFile();

        if (uiThread == null) {
            uiThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Vector<File> fList = new Vector<File>();
                        String rootDir = System.getProperty("hdfview.workdir");
                        if(rootDir == null) rootDir = System.getProperty("user.dir");

                        int W = 500,
                            H = 400,
                            X = 0,
                            Y = 0;

                        while (true) {
                            // open and layout the shell
                            HDFView window = new HDFView(rootDir);

                            // Set the testing state to handle the problem with testing
                            // of native dialogs
                            window.setTestState(true);

                            shell = window.openMainWindow(fList, W, H, X, Y);

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

    @Before
    public final void setupSWTBot() throws InterruptedException, BrokenBarrierException {
        // synchronize with the thread opening the shell
        swtBarrier.await();
        bot = new SWTBot(shell);

        SWTBotPreferences.PLAYBACK_DELAY = TEST_DELAY;
    }

    @After
    public void closeShell() throws InterruptedException {
        // close the shell
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                shell.close();
            }
        });
    }

    @Test
    public void verifyOpenAs() {
        String filename = "tintsize";
        String file_ext = ".h5";
        String datasetname = "DS08BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("verifyOpenAs()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("checkHDF5GroupDS08() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("checkHDF5GroupDS08() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            // Open dataset 'DS08BITS'
            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open As").click();

            SWTBotShell openAsShell = bot.shell("Dataset Selection - /" + datasetname);
            openAsShell.bot().comboBox(0).setSelection("test.modules.GermanTableView");
            openAsShell.bot().button("   &OK   ").click();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*an.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            tableShell.bot().menu("Hexadezimal Anzeigen").click();
            bot.sleep(3000);

            String val = table.getCellDataValueByPosition(1, 1);
            assertTrue(constructWrongValueMessage("verifyOpenAs()", "wrong data", "FF", val),
                    val.equals("FF"));

            tableShell.bot().menu("Schließen").click();
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
                tableShell.bot().menu("Schließen").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }
            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {}
        }
    }
}
