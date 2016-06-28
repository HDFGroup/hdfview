package test.uitest;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import hdf.HDFVersions;
import hdf.view.HDFView;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.AfterClass;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

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
    
    private static int TEST_DELAY = 10;


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
            assertEquals(hdf_file.getName(), text.getText());

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("Button-Open filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("Button-Open filetree is missing file " + hdf_file.getName(), items[0].getText().compareTo(hdf_file.getName()) == 0);
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
            assertEquals(name + file_ext, text.getText());
            
            shell.bot().button("   &OK   ").click();
            shell.bot().waitUntil(shellCloses(shell));
            
            assertTrue("File- " + hdf_file + " file created", hdf_file.exists());
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
        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close All");
            fileMenuItem.click();

            if(delete_file) {
                assertTrue("closeFile File " + hdf_file + " not deleted", hdf_file.delete());
                assertFalse("closeFile File " + hdf_file + " not gone", hdf_file.exists());
            }

            SWTBotTree filetree = bot.tree();
            assertTrue("closeHDFFile filetree shows:"+filetree.rowCount(), filetree.rowCount() == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
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
}
