package uitest.HDF5UITests.BugFixTests;

import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;

import uitest.AbstractWindowTest;
import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestBoatload extends AbstractWindowTest {

    @Test
    public void testBoatload() {
        String[][] expectedData =
            { { "Josie", "Zavala", "17.003594445574507", "-5.634300979511977", "\\[Ark Royal, Vanguard, Bounty, , \\]", "01" },
              { "Willetta", "Nipper", "15.96895460656718", "-41.42621008069324", "\\[Hood, Golden Hind, Warrior, Mary Rose, Ark Royal\\]", "01" },
              { "Lieselotte", "Good", "63.82398283343315", "51.82597200775095", "\\[Resolution, Warrior, , , \\]", "01" },
              { "Vallie", "Rasmusson", "-4.778374785714632", "-33.995909014594645", "\\[Bismarck, Vanguard, Golden Hind, Intrepid, \\]", "01" },
              { "Janeth", "Beers", "29.904658515009032", "86.45103752483442", "\\[Revenge, Golden Hind, Invincible, , \\]", "01" },
              { "Adah", "Dudley", "33.942801461729545", "-93.57347883025659", "\\[Ark Royal, Warspite, , , \\]", "01" },
              { "Jamee", "Imhoff", "-0.37648478939568975", "-120.22289089605276", "\\[Hood, Bismarck, Vanguard, Bounty, \\]", "01" },
              { "Cameron", "Arias", "84.76826993233132", "111.31524858913076", "\\[Hood, Dreadnought, Ark Royal, , \\]", "00" },
              { "Maryam", "Horiuchi", "19.717326603788734", "139.34575234449477", "\\[Endeavour, Warrior, Mary Rose, Victory, \\]", "01" },
              { "Heike", "Jessen", "-21.880434582351384", "177.28636919215216", "\\[Victory, Dreadnought, Golden Hind, , \\]", "00" } };
        SWTBotShell tableShell = null;
        String filename = "boatload.h5";
        String datasetName = "/sailors";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "testBoatload()", 6, filename);

            testNEGroup();
            testNWGroup();
            testSEGroup();
            testSWGroup();

            // Open dataset '/sailors'
            tableShell = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "testBoatload()", false);
            retriever.setContainerHeaderOffset(2, 0);

            retriever.testAllTableLocations(expectedData);
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
            closeShell(tableShell);

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void testNEGroup() {
        // TODO
    }

    private void testNWGroup() {
        // TODO
    }

    private void testSEGroup() {
        // TODO
    }

    private void testSWGroup() {
        // TODO
    }
}
