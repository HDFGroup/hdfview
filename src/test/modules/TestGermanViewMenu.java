package test.modules;

import static org.fest.swing.data.TableCell.row;
import static org.fest.swing.finder.WindowFinder.findFrame;
import static org.fest.swing.launcher.ApplicationLauncher.application;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JFrame;

import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.finder.JFileChooserFinder;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JTableCellFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestGermanViewMenu {
    private static FrameFixture mainFrameFixture;
    private static String HDF5VERSION;
    private static String HDF4VERSION;
    // the version of the HDFViewer
    private static String VERSION;

    private static String workDir = System.getProperty("hdfview.workdir");

    private static File openHDF5File(String name, int initrows) {
        File hdf_file = new File(workDir, name + ".h5");
        if (hdf_file.exists())
            hdf_file.delete();

        try {
            JMenuItemFixture fileMenuItem = mainFrameFixture.menuItemWithPath("File","Open");
            mainFrameFixture.robot.waitForIdle();

            fileMenuItem.requireVisible();
            fileMenuItem.click();
            mainFrameFixture.robot.waitForIdle();

            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(mainFrameFixture.robot);
            fileChooser.fileNameTextBox().setText(name+".h5");
            fileChooser.approve();
            mainFrameFixture.robot.waitForIdle();

            assertTrue("File-HDF5 file opened", hdf_file.exists());

            JTreeFixture filetree = mainFrameFixture.tree().focus();
            assertTrue("openHDF5File filetree shows:"+filetree.target.getRowCount(), filetree.target.getRowCount() == initrows);
            assertTrue("openHDF5File filetree has file",(filetree.valueAt(0)).compareTo(name+".h5") == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }

        return hdf_file;
    }

    private static void closeHDFFile(File hdf_file, boolean delete_file) {
        try {
            JMenuItemFixture fileMenuItem = mainFrameFixture.menuItemWithPath("File", "Close All");
            mainFrameFixture.robot.waitForIdle();

            fileMenuItem.requireVisible();
            fileMenuItem.click();
            mainFrameFixture.robot.waitForIdle();

            if(delete_file) {
                assertTrue("closeFile File " + hdf_file + " not deleted", hdf_file.delete());
                assertFalse("closeFile File " + hdf_file + " not gone", hdf_file.exists());
            }

            JTreeFixture filetree = mainFrameFixture.tree().focus();
            assertTrue("closeHDFFile filetree shows:"+filetree.target.getRowCount(), filetree.target.getRowCount() == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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

    @BeforeClass
    public static void setUpOnce() {
        try {
            Properties prop = new Properties();
            InputStream stream = new FileInputStream("UIinclude.properties");

            prop.load(stream);

            HDF5VERSION = "HDF5 " + prop.getProperty("HDF5-Version");
            HDF4VERSION = "HDF " + prop.getProperty("HDF4-Version");
            VERSION = prop.getProperty("HDFView-Version");

            stream.close();
        }
        catch (Exception ex) {
            System.err.println("Property file UIinclude not found in " + System.getProperty("user.dir"));
            ex.printStackTrace();
        }

        clearRemovePropertyFile();
        FailOnThreadViolationRepaintManager.install();
        Robot robot = BasicRobot.robotWithNewAwtHierarchy();
        application("hdf.view.HDFView").withArgs("-root", workDir).start();
        mainFrameFixture = findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
            protected boolean isMatching(JFrame frame) {
                return frame.getTitle().equals("HDFView "+VERSION)
                        && frame.isShowing();
            }
        }).withTimeout(10000).using(robot);
        mainFrameFixture.robot.waitForIdle();
        mainFrameFixture.requireVisible();
    }

    @AfterClass
    public static void finishApplication() {
        mainFrameFixture.robot.waitForIdle();
        //mainFrameFixture.requireNotVisible();
        mainFrameFixture.cleanUp();
    }
    @Test
    public void verifyOpenAs() {
        File hdf_file = openHDF5File("tintsize", 10);

        try {
            JTreeFixture filetree = mainFrameFixture.tree().focus();
            filetree.requireVisible();
            assertTrue("createCustomFormat filetree shows:", filetree.target.getRowCount()==10);
            assertTrue("createCustomFormat filetree has file", (filetree.valueAt(0)).compareTo("tintsize.h5")==0);
            assertTrue("createCustomFormat filetree has group", (filetree.valueAt(1)).compareTo("DS08BITS")==0);

            JMenuItemFixture dataset1MenuItem = filetree.showPopupMenuAt(9).menuItemWithPath("Open As");
            mainFrameFixture.robot.waitForIdle();

            dataset1MenuItem.requireVisible();
            dataset1MenuItem.click();
            mainFrameFixture.robot.waitForIdle();
            mainFrameFixture.dialog().comboBox("moduletable").selectItem("test.modules.GermanTableView");
            mainFrameFixture.dialog().button("OK").click();
            mainFrameFixture.robot.waitForIdle();

            JTableFixture dataset1table = mainFrameFixture.table("data");
            JTableCellFixture cell1 = dataset1table.cell(row(0).column(0));
            cell1.requireValue("0.0");
            cell1 = dataset1table.cell(row(7).column(0));
            cell1.requireValue("7.0");
            cell1 = dataset1table.cell(row(7).column(7));
            cell1.requireValue("7.0007");
            mainFrameFixture.robot.waitForIdle();

            mainFrameFixture.menuItemWithPath("Tabelle", "Erstellen von benutzerdefinierten Notation").click();
            mainFrameFixture.robot.waitForIdle();

            mainFrameFixture.dialog().optionPane().textBox("OptionPane.textField").setText("00.00");
            mainFrameFixture.dialog().optionPane().okButton().click();
            mainFrameFixture.robot.waitForIdle();

            mainFrameFixture.menuItemWithPath("Tabelle", "Benutzerdefinierte Darstellung").click();
            mainFrameFixture.robot.waitForIdle();
            dataset1table = mainFrameFixture.table("data");
            cell1 = dataset1table.cell(row(0).column(0));
            cell1.requireValue("00.00");
            cell1 = dataset1table.cell(row(7).column(0));
            cell1.requireValue("07.00");
            cell1 = dataset1table.cell(row(7).column(7));
            cell1.requireValue("07.00");
            mainFrameFixture.robot.waitForIdle();

            mainFrameFixture.menuItemWithPath("Tabelle", "Schließen").click();
            mainFrameFixture.robot.waitForIdle();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeHDFFile(hdf_file, false);
            }
            catch (Exception ex) {}
        }
    }
}
