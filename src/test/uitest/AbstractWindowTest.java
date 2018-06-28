package test.uitest;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCanvas;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
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
            bot.menu("Open As").menu("Read/Write").click();

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
                        Vector<File> fList = new Vector<>();
                        String rootDir = System.getProperty("hdfview.workdir");
                        if(rootDir == null) rootDir = System.getProperty("user.dir");

                        int W = 800,
                            H = 600,
                            X = 0,
                            Y = 0;

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

                            shell.setBounds(activeMonitor.getBounds());

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
            @Override
            public void run() {
                shell.close();
            }
        });
    }
}
