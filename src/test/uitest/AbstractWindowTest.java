package test.uitest;

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


    private static void clearRemovePropertyFile() {
        // the local property file name
        // look for the property file at the use home directory
        String fn = ".hdfview" + VERSION;

        File prop_file = new File(workDir, fn);
        if (prop_file.exists()) {
            prop_file.delete();
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
                        while (true) {
                            Vector<File> fList = new Vector<File>();
                            String rootDir = System.getProperty("hdfview.workdir");
                            if(rootDir == null) rootDir = System.getProperty("user.dir");

                            int W = 500,
                                H = 200,
                                X = 0,
                                Y = 0;

                             // open and layout the shell
                            HDFView window = new HDFView(rootDir);
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
