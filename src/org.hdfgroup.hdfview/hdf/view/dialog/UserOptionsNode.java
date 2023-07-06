/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
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

package hdf.view.dialog;

import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * UserOptionsDialog displays components for choosing user options.
 */
public class UserOptionsNode extends PreferenceNode {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserOptionsNode.class);
    /**
     * The name of the class that implements the <code>PreferencePage</code>
     * bound to this <code>ExtendedPreferenceNode</code>
     */
    private String classname;

    /**
     * Creates an <code>UserOptionsNode</code> with the given id. The
     * new node has nosubnodes.
     *
     * @param id the node id
     */
    public UserOptionsNode(String id) {
        super(id);
    }

    /**
     * Creates an <code>UserOptionsNode</code> with the given id,
     * label, and image, and lazily-loaded preference page. The preference node
     * assumes (sole) responsibility for disposing of the image; this will
     * happen when the node is disposed.
     *
     * @param id the node id
     * @param label the label used to display the node in the preference
     *            dialog's tree
     * @param image the image displayed left of the label in the preference
     *            dialog's tree, or <code>null</code> if none
     * @param className the class name of the preference page; this class must
     *            implement <code>IPreferencePage</code>
     */
    public UserOptionsNode(String id, String label,
            ImageDescriptor image, String className) {
        super(id, label, image, className);
        this.classname = className;
    }

    /**
     * Creates an <code>UserOptionsNode</code> with the given id and
     * preference page. The title of the preference page is used for the node
     * label. The node will not have an image.
     *
     * @param id the node id
     * @param preferencePage the preference page
     */
    public UserOptionsNode(String id, IPreferencePage preferencePage) {
        super(id, preferencePage);
    }
}
