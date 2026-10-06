/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the COPYING file, which can be found  *
 * at the root of the source code distribution tree,                         *
 * or in https://www.hdfgroup.org/licenses.                                  *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view.TableView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.h5.H5CompoundDS;
import hdf.object.h5.H5File;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for DataFactoryUtils leaf counting and member filtering against a compound
 * of {int, vlen-of-int, vlen-of-compound}
 */
public class DataFactoryUtilsTest {

    @TempDir
    static Path tempDir;

    private static final String DATASET_NAME = "compound_of_vlen_compound";

    private static H5File testFile;
    private static H5CompoundDS testDataset;

    @BeforeAll
    static void createFile() throws Exception
    {
        String path = tempDir.resolve("data_factory_utils_test.h5").toString();

        long innerTid = -1, vlenIntTid = -1, vlenInnerTid = -1, outerTid = -1;
        long fid = -1, spaceId = -1, did = -1;
        try {
            innerTid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);
            H5.H5Tinsert(innerTid, "p", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(innerTid, "q", 4, HDF5Constants.H5T_NATIVE_INT);

            vlenIntTid   = H5.H5Tvlen_create(HDF5Constants.H5T_NATIVE_INT);
            vlenInnerTid = H5.H5Tvlen_create(innerTid);

            // Lay fields out back-to-back using actual sizes rather than assumed
            // struct-alignment offsets (hvl_t size is platform-dependent).
            long idOffset     = 0;
            long tagsOffset   = idOffset + H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
            long nestedOffset = tagsOffset + H5.H5Tget_size(vlenIntTid);
            long outerSize    = nestedOffset + H5.H5Tget_size(vlenInnerTid);

            outerTid = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, outerSize);
            H5.H5Tinsert(outerTid, "id", idOffset, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(outerTid, "tags", tagsOffset, vlenIntTid);
            H5.H5Tinsert(outerTid, "nested", nestedOffset, vlenInnerTid);

            fid     = H5.H5Fcreate(path, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT,
                                   HDF5Constants.H5P_DEFAULT);
            spaceId = H5.H5Screate_simple(1, new long[] {0}, null);
            did     = H5.H5Dcreate(fid, DATASET_NAME, outerTid, spaceId, HDF5Constants.H5P_DEFAULT,
                                   HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        }
        finally {
            if (did >= 0)
                H5.H5Dclose(did);
            if (spaceId >= 0)
                H5.H5Sclose(spaceId);
            if (fid >= 0)
                H5.H5Fclose(fid);
            if (outerTid >= 0)
                H5.H5Tclose(outerTid);
            if (vlenInnerTid >= 0)
                H5.H5Tclose(vlenInnerTid);
            if (vlenIntTid >= 0)
                H5.H5Tclose(vlenIntTid);
            if (innerTid >= 0)
                H5.H5Tclose(innerTid);
        }

        testFile = (H5File)(new H5File()).createInstance(path, FileFormat.READ);
        testFile.open();
        testDataset = (H5CompoundDS)testFile.get("/" + DATASET_NAME);
        testDataset.init();
    }

    @AfterAll
    static void closeFile() throws Exception
    {
        if (testFile != null)
            testFile.close();
    }

    private static Datatype memberType(String name)
    {
        List<String> names   = testDataset.getDatatype().getCompoundMemberNames();
        List<Datatype> types = testDataset.getDatatype().getCompoundMemberTypes();
        return types.get(names.indexOf(name));
    }

    @Test
    void countLeafNames_atomicMemberIsOne()
    {
        assertEquals(1, DataFactoryUtils.countLeafNames(memberType("id")));
    }

    @Test
    void countLeafNames_vlenOfAtomicIsOne()
    {
        assertEquals(1, DataFactoryUtils.countLeafNames(memberType("tags")));
    }

    @Test
    void countLeafNames_vlenOfCompoundIsOne()
    {
        // A vlen member is always a single column, regardless of what it wraps
        Datatype nested = memberType("nested");
        assertTrue(nested.isVLEN());
        assertTrue(nested.getDatatypeBase().isCompound());

        assertEquals(1, DataFactoryUtils.countLeafNames(nested));
    }

    @Test
    void countLeafNames_plainCompoundSumsItsChildren()
    {
        Datatype innerCompound = memberType("nested").getDatatypeBase();
        assertEquals(2, DataFactoryUtils.countLeafNames(innerCompound)); // p, q
    }

    @Test
    void countLeafNames_topLevelCompoundCountsTopLevelMembersOnly()
    {
        // id(1) + tags:VLEN(1) + nested:VLEN(1) = 3, not the fully-flattened leaf
        // count
        assertEquals(3, DataFactoryUtils.countLeafNames(testDataset.getDatatype()));
    }

    @Test
    void countLeafNames_agreesWithExtractCompoundInfoFlatNameList()
    {
        // countLeafNames exists to mirror the flat leaf-name list that
        // H5Datatype.extractCompoundInfo produces, which recursiveColumnHeaderSetup
        // walks using these counts to decide which top-level member each name belongs to.
        assertEquals(testDataset.getSelectedMemberNames().length,
                     DataFactoryUtils.countLeafNames(testDataset.getDatatype()));
    }

    @Test
    void filterNonSelectedMembers_innerCompoundKeepsAllMembers()
    {
        // The dataset's selected-member list names only id/tags/nested, so
        // the isTopLevel=false path must skip the filter entirely.
        Datatype innerCompound = memberType("nested").getDatatypeBase();

        List<Datatype> filtered =
            DataFactoryUtils.filterNonSelectedMembers(testDataset, innerCompound, false);

        assertNotNull(filtered);
        assertEquals(2, filtered.size(), "inner compound's members must survive unfiltered");
    }

    @Test
    void filterNonSelectedMembers_topLevelFiltersAgainstSelection()
    {
        Datatype idType = memberType("id");

        // All members are selected by default after init().
        List<Datatype> filtered =
            DataFactoryUtils.filterNonSelectedMembers(testDataset, testDataset.getDatatype(), true);
        assertEquals(3, filtered.size());

        // Deselect everything except "id" and confirm the top-level filter honors it.
        List<String> names = testDataset.getDatatype().getCompoundMemberNames();
        testDataset.setAllMemberSelection(false);
        testDataset.selectMember(names.indexOf("id"));

        try {
            filtered =
                DataFactoryUtils.filterNonSelectedMembers(testDataset, testDataset.getDatatype(), true);

            assertEquals(1, filtered.size());
            assertSame(idType, filtered.get(0));
        }
        finally {
            // testDataset is shared across test methods, so leave selection as init() set it.
            testDataset.setAllMemberSelection(true);
        }
    }
}
