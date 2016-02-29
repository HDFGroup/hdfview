public class DefaultTableViewOld implements TableView {
    /**
     * Creates a NatTable to hold a scalar dataset.
     */
    private NatTable createTable (ScalarDS d) {
        /*
        theTable = new JTable(tm) {
            private static final long serialVersionUID = -145476220959400488L;
            private final Datatype    dtype            = dataset.getDatatype();
            private final boolean     isArray          = (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY);
            
            @Override
            public boolean editCellAt (int row, int column, java.util.EventObject e) {
                if (!isCellEditable(row, column)) {
                    return super.editCellAt(row, column, e);
                }

                if (e instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent) e;
                    if (ke.getID() == KeyEvent.KEY_PRESSED) {
                        startEditing[0] = true;
                    }
                }
                else if (e instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) e;
                    int mc = me.getClickCount();
                    if (mc > 1) {
                        currentEditingCellValue = getValueAt(row, column);
                    }
                }

                return super.editCellAt(row, column, e);
            }

            @Override
            public void editingStopped (ChangeEvent e) {
                startEditing[0] = false;
            }

            @Override
            public boolean isCellSelected (int row, int column) {
                if ((getSelectedRow() == row) && (getSelectedColumn() == column)) {
                    cellLabel.setText(String.valueOf(rowStart + indexBase + row * rowStride) + ", " + table.getColumnName(column)
                            + "  =  ");

                    log.trace("JTable.ScalarDS isCellSelected isRegRef={} isObjRef={}", isRegRef, isObjRef);
                    Object val = getValueAt(row, column);
                    String strVal = null;

                    if (isRegRef) {
                        boolean displayValues = ViewProperties.showRegRefValues();
                        log.trace("JTable.ScalarDS isCellSelected displayValues={}", displayValues);
                        if (displayValues && val != null && ((String) val).compareTo("NULL") != 0) {
                            String reg = (String) val;
                            boolean isPointSelection = (reg.indexOf('-') <= 0);

                            // find the object location
                            String oidStr = reg.substring(reg.indexOf('/'), reg.indexOf(' '));
                            log.trace("JTable.ScalarDS isCellSelected: isPointSelection={} oidStr={}", isPointSelection, oidStr);

                            // decode the region selection
                            String regStr = reg.substring(reg.indexOf('{') + 1, reg.indexOf('}'));
                            if (regStr == null || regStr.length() <= 0) { // no
                                                                          // selection
                                strVal = null;
                            }
                            else {
                                reg.substring(reg.indexOf('}') + 1);

                                StringTokenizer st = new StringTokenizer(regStr);
                                int nSelections = st.countTokens();
                                if (nSelections <= 0) { // no selection
                                    strVal = null;
                                }
                                else {
                                    log.trace("JTable.ScalarDS isCellSelected: nSelections={}", nSelections);

                                    HObject obj = FileFormat.findObject(dataset.getFileFormat(), oidStr);
                                    if (obj == null || !(obj instanceof ScalarDS)) { // no
                                                                                     // selection
                                        strVal = null;
                                    }
                                    else {
                                        ScalarDS dset = (ScalarDS) obj;
                                        try {
                                            dset.init();
                                        }
                                        catch (Exception ex) {
                                            log.debug("reference dset did not init()", ex);
                                        }
                                        StringBuffer selectionSB = new StringBuffer();
                                        StringBuffer strvalSB = new StringBuffer();

                                        int idx = 0;
                                        while (st.hasMoreTokens()) {
                                            log.trace("JTable.ScalarDS isCellSelected: st.hasMoreTokens() begin");

                                            int rank = dset.getRank();
                                            long start[] = dset.getStartDims();
                                            long count[] = dset.getSelectedDims();
                                            // long count[] = new long[rank];

                                            // set the selected dimension sizes
                                            // based on the region selection
                                            // info.
                                            String sizeStr = null;
                                            String token = st.nextToken();

                                            selectionSB.setLength(0);
                                            selectionSB.append(token);
                                            log.trace("JTable.ScalarDS isCellSelected: selectionSB={}", selectionSB);

                                            token = token.replace('(', ' ');
                                            token = token.replace(')', ' ');
                                            if (isPointSelection) {
                                                // point selection
                                                String[] tmp = token.split(",");
                                                for (int x = 0; x < tmp.length; x++) {
                                                    count[x] = 1;
                                                    sizeStr = tmp[x].trim();
                                                    start[x] = Long.valueOf(sizeStr);
                                                    log.trace("JTable.ScalarDS isCellSelected: point sel={}", tmp[x]);
                                                }
                                            }
                                            else {
                                                // rectangle selection
                                                String startStr = token.substring(0, token.indexOf('-'));
                                                String endStr = token.substring(token.indexOf('-') + 1);
                                                log.trace("JTable.ScalarDS isCellSelected: rect sel with startStr={} endStr={}",
                                                        startStr, endStr);
                                                String[] tmp = startStr.split(",");
                                                log.trace("JTable.ScalarDS isCellSelected: tmp with length={} rank={}", tmp.length,
                                                        rank);
                                                for (int x = 0; x < tmp.length; x++) {
                                                    sizeStr = tmp[x].trim();
                                                    start[x] = Long.valueOf(sizeStr);
                                                    log.trace("JTable.ScalarDS isCellSelected: rect start={}", tmp[x]);
                                                }
                                                tmp = endStr.split(",");
                                                for (int x = 0; x < tmp.length; x++) {
                                                    sizeStr = tmp[x].trim();
                                                    count[x] = Long.valueOf(sizeStr) - start[x] + 1;
                                                    log.trace("JTable.ScalarDS isCellSelected: rect end={} count={}", tmp[x],
                                                            count[x]);
                                                }
                                            }
                                            log.trace("JTable.ScalarDS isCellSelected: selection inited");

                                            Object dbuf = null;
                                            try {
                                                dbuf = dset.getData();
                                            }
                                            catch (Exception ex) {
                                                JOptionPane.showMessageDialog(this, ex, "Region Reference:" + getTitle(),
                                                        JOptionPane.ERROR_MESSAGE);
                                            }

                                            // Convert dbuf to a displayable
                                            // string
                                            String cName = dbuf.getClass().getName();
                                            int cIndex = cName.lastIndexOf("[");
                                            if (cIndex >= 0) {
                                                NT = cName.charAt(cIndex + 1);
                                            }
                                            log.trace("JTable.ScalarDS isCellSelected: cName={} NT={}", cName, NT);

                                            if (idx > 0) strvalSB.append(',');

                                            // convert numerical data into char
                                            // only possible cases are byte[]
                                            // and short[] (converted from
                                            // unsigned
                                            // byte)
                                            Datatype dtype = dset.getDatatype();
                                            Datatype baseType = dtype.getBasetype();
                                            log.trace("JTable.ScalarDS isCellSelected: dtype={} baseType={}",
                                                    dtype.getDatatypeDescription(), baseType);
                                            if (baseType == null) baseType = dtype;
                                            if ((dtype.getDatatypeClass() == Datatype.CLASS_ARRAY && baseType.getDatatypeClass() == Datatype.CLASS_CHAR)
                                                    && ((NT == 'B') || (NT == 'S'))) {
                                                int n = Array.getLength(dbuf);
                                                log.trace("JTable.ScalarDS isCellSelected charData length = {}", n);
                                                char[] charData = new char[n];
                                                for (int i = 0; i < n; i++) {
                                                    if (NT == 'B') {
                                                        charData[i] = (char) Array.getByte(dbuf, i);
                                                    }
                                                    else if (NT == 'S') {
                                                        charData[i] = (char) Array.getShort(dbuf, i);
                                                    }
                                                }

                                                strvalSB.append(charData);
                                                log.trace("JTable.ScalarDS isCellSelected charData");// =
                                                                                                     // {}",
                                                                                                     // strvalSB);
                                            }
                                            else {
                                                // numerical values
                                                if (dtype.getDatatypeClass() == Datatype.CLASS_ARRAY) dtype = baseType;
                                                boolean is_unsigned = dtype.isUnsigned();
                                                int n = Array.getLength(dbuf);
                                                if (is_unsigned) {
                                                    switch (NT) {
                                                        case 'B':
                                                            byte[] barray = (byte[]) dbuf;
                                                            short sValue = barray[0];
                                                            if (sValue < 0) {
                                                                sValue += 256;
                                                            }
                                                            strvalSB.append(sValue);
                                                            for (int i = 1; i < n; i++) {
                                                                strvalSB.append(',');
                                                                sValue = barray[i];
                                                                if (sValue < 0) {
                                                                    sValue += 256;
                                                                }
                                                                strvalSB.append(sValue);
                                                            }
                                                            break;
                                                        case 'S':
                                                            short[] sarray = (short[]) dbuf;
                                                            int iValue = sarray[0];
                                                            if (iValue < 0) {
                                                                iValue += 65536;
                                                            }
                                                            strvalSB.append(iValue);
                                                            for (int i = 1; i < n; i++) {
                                                                strvalSB.append(',');
                                                                iValue = sarray[i];
                                                                if (iValue < 0) {
                                                                    iValue += 65536;
                                                                }
                                                                strvalSB.append(iValue);
                                                            }
                                                            break;
                                                        case 'I':
                                                            int[] iarray = (int[]) dbuf;
                                                            long lValue = iarray[0];
                                                            if (lValue < 0) {
                                                                lValue += 4294967296L;
                                                            }
                                                            strvalSB.append(lValue);
                                                            for (int i = 1; i < n; i++) {
                                                                strvalSB.append(',');
                                                                lValue = iarray[i];
                                                                if (lValue < 0) {
                                                                    lValue += 4294967296L;
                                                                }
                                                                strvalSB.append(lValue);
                                                            }
                                                            break;
                                                        case 'J':
                                                            long[] larray = (long[]) dbuf;
                                                            Long l = (Long) larray[0];
                                                            String theValue = Long.toString(l);
                                                            if (l < 0) {
                                                                l = (l << 1) >>> 1;
                                                                BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                                                                BigInteger big2 = new BigInteger(l.toString());
                                                                BigInteger big = big1.add(big2);
                                                                theValue = big.toString();
                                                            }
                                                            strvalSB.append(theValue);
                                                            for (int i = 1; i < n; i++) {
                                                                strvalSB.append(',');
                                                                l = (Long) larray[i];
                                                                theValue = Long.toString(l);
                                                                if (l < 0) {
                                                                    l = (l << 1) >>> 1;
                                                                    BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                                                                    BigInteger big2 = new BigInteger(l.toString());
                                                                    BigInteger big = big1.add(big2);
                                                                    theValue = big.toString();
                                                                }
                                                                strvalSB.append(theValue);
                                                            }
                                                            break;
                                                        default:
                                                            strvalSB.append(Array.get(dbuf, 0));
                                                            for (int i = 1; i < n; i++) {
                                                                strvalSB.append(',');
                                                                strvalSB.append(Array.get(dbuf, i));
                                                            }
                                                            break;
                                                    }
                                                }
                                                else {
                                                    for (int x = 0; x < n; x++) {
                                                        Object theValue = Array.get(dbuf, x);
                                                        if (x > 0) strvalSB.append(',');
                                                        strvalSB.append(theValue);
                                                    }
                                                }
                                                log.trace("JTable.ScalarDS isCellSelected byteString");// =
                                                                                                       // {}",
                                                                                                       // strvalSB);
                                            }
                                            idx++;
                                            dset.clearData();
                                            log.trace("JTable.ScalarDS isCellSelected: st.hasMoreTokens() end");// strvalSB
                                                                                                                // =
                                                                                                                // {}",
                                                                                                                // strvalSB);
                                        } // while (st.hasMoreTokens())
                                        strVal = strvalSB.toString();
                                        log.trace("JTable.ScalarDS isCellSelected: st.hasMoreTokens() end");// value
                                                                                                            // =
                                                                                                            // {}",
                                                                                                            // strVal);
                                    }
                                }
                            }
                        }
                        else {
                            strVal = null;
                        }
                    }
                    else if (isObjRef) {
                        Long ref = (Long) val;
                        long oid[] = { ref.longValue() };

                        // decode object ID
                        try {
                            HObject obj = FileFormat.findObject(dataset.getFileFormat(), oid);
                            strVal = obj.getFullName();
                        }
                        catch (Exception ex) {
                            strVal = null;
                        }
                    }

                    if (strVal == null && val != null) strVal = val.toString();

                    log.trace("JTable.ScalarDS isCellSelected finish");// value
                                                                       // =
                                                                       // {}",strVal);
                    cellValueField.setText(strVal);
                }

                return super.isCellSelected(row, column);
            }
        };
        */
    }

    /**
     * Creates a NatTable to hold a compound dataset.
     */
    private NatTable createTable (CompoundDS d) {
        /*
        theTable = new JTable(tm) {
            private static final long serialVersionUID   = 3221288637329958074L;
            int                       lastSelectedRow    = -1;
            int                       lastSelectedColumn = -1;

            @Override
            public boolean editCellAt (int row, int column, java.util.EventObject e) {
                if (!isCellEditable(row, column)) {
                    return super.editCellAt(row, column, e);
                }

                if (e instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent) e;
                    if (ke.getID() == KeyEvent.KEY_PRESSED) startEditing[0] = true;
                }
                else if (e instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) e;
                    int mc = me.getClickCount();
                    if (mc > 1) {
                        currentEditingCellValue = getValueAt(row, column);
                    }
                }

                return super.editCellAt(row, column, e);
            }

            @Override
            public void editingStopped (ChangeEvent e) {
                startEditing[0] = false;
            }

            @Override
            public boolean isCellSelected (int row, int column) {
                if ((lastSelectedRow == row) && (lastSelectedColumn == column)) {
                    return super.isCellSelected(row, column);
                }
                log.trace("JTable.CompoundDS isCellSelected row={} column={}", row, column);

                lastSelectedRow = row;
                lastSelectedColumn = column;
                if ((getSelectedRow() == row) && (getSelectedColumn() == column)) {
                    cellLabel.setText(String.valueOf(rowStart + indexBase + row * rowStride) + ", " + table.getColumnName(column)
                            + "  =  ");
                    cellValueField.setText(getValueAt(row, column).toString());
                }

                return super.isCellSelected(row, column);
            }
        };
        */

        /*
        if (columns > 1) {
            // multi-dimension compound dataset
            MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer(columns, columnNames.length);
            Enumeration<?> local_enum = theTable.getColumnModel().getColumns();
            while (local_enum.hasMoreElements()) {
                ((TableColumn) local_enum.nextElement()).setHeaderRenderer(renderer);
            }
        }
        */
    } /* createTable */

    private class ColumnHeader
    //extends JTableHeader 
    {
    	/*
        private static final long serialVersionUID   = -3179653809792147055L;
        private int               currentColumnIndex = -1;
        private int               lastColumnIndex    = -1;
        private JTable            parentTable;

        @Override
        protected void processMouseMotionEvent (MouseEvent e) {
            super.processMouseMotionEvent(e);

            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                // do not do anything, just resize the column
                if (getResizingColumn() != null) return;

                int colEnd = columnAtPoint(e.getPoint());

                if (colEnd < 0) {
                    colEnd = 0;
                }
                if (currentColumnIndex < 0) {
                    currentColumnIndex = 0;
                }

                parentTable.clearSelection();

                if (colEnd > currentColumnIndex) {
                    parentTable.setColumnSelectionInterval(currentColumnIndex, colEnd);
                }
                else {
                    parentTable.setColumnSelectionInterval(colEnd, currentColumnIndex);
                }

                parentTable.setRowSelectionInterval(0, parentTable.getRowCount() - 1);
            }
        }

        @Override
        protected void processMouseEvent (MouseEvent e) {
            super.processMouseEvent(e);

            int mouseID = e.getID();

            if (mouseID == MouseEvent.MOUSE_CLICKED) {
                if (currentColumnIndex < 0) {
                    return;
                }

                if (e.isControlDown()) {
                    // select discontinuous columns
                    parentTable.addColumnSelectionInterval(currentColumnIndex, currentColumnIndex);
                }
                else if (e.isShiftDown()) {
                    // select continuous columns
                    if (lastColumnIndex < 0) {
                        parentTable.addColumnSelectionInterval(0, currentColumnIndex);
                    }
                    else if (lastColumnIndex < currentColumnIndex) {
                        parentTable.addColumnSelectionInterval(lastColumnIndex, currentColumnIndex);
                    }
                    else {
                        parentTable.addColumnSelectionInterval(currentColumnIndex, lastColumnIndex);
                    }
                }
                else {
                    // clear old selection and set new column selection
                    parentTable.clearSelection();
                    parentTable.setColumnSelectionInterval(currentColumnIndex, currentColumnIndex);
                }

                lastColumnIndex = currentColumnIndex;
                parentTable.setRowSelectionInterval(0, parentTable.getRowCount() - 1);
            }
            else if (mouseID == MouseEvent.MOUSE_PRESSED) {
                currentColumnIndex = columnAtPoint(e.getPoint());
            }
        }
        */
    } // private class ColumnHeader

    /** RowHeader defines the row header component of the Spreadsheet. */
    private class RowHeader 
    //extends JTable 
    {
    	/*
        private static final long serialVersionUID = -1548007702499873626L;
        private int               currentRowIndex  = -1;
        private int               lastRowIndex     = -1;
        private JTable            parentTable;
        */

    	/*
        public RowHeader(JTable pTable, Dataset dset) {
            // Create a JTable with the same number of rows as
            // the parent table and one column.
            // super( pTable.getRowCount(), 1 );

            final long[] startArray = dset.getStartDims();
            final long[] strideArray = dset.getStride();
            final int[] selectedIndex = dset.getSelectedIndex();
            final int start = (int) startArray[selectedIndex[0]];
            final int stride = (int) strideArray[selectedIndex[0]];
            final int rowCount = pTable.getRowCount();
            parentTable = pTable;

            AbstractTableModel tm = new AbstractTableModel() {
                private static final long serialVersionUID = -8117073107569884677L;

                //@Override
                public Object getValueAt (int row, int column) {
                    log.trace("RowHeader:AbstractTableModel:getValueAt");
                    return String.valueOf(start + indexBase + row * stride);
                }
            };
        }
        */

        /** This is called when the selection changes in the row headers. */
        /*
    	@Override
        public void valueChanged (ListSelectionEvent e) {
            if (parentTable == null) {
                return;
            }

            int rows[] = getSelectedRows();
            if ((rows == null) || (rows.length == 0)) {
                return;
            }

            parentTable.clearSelection();
            parentTable.setRowSelectionInterval(rows[0], rows[rows.length - 1]);
            parentTable.setColumnSelectionInterval(0, parentTable.getColumnCount() - 1);
        }
        */

    	/*
        @Override
        protected void processMouseMotionEvent (MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                int colEnd = rowAtPoint(e.getPoint());

                if (colEnd < 0) {
                    colEnd = 0;
                }
                if (currentRowIndex < 0) {
                    currentRowIndex = 0;
                }

                parentTable.clearSelection();

                if (colEnd > currentRowIndex) {
                    parentTable.setRowSelectionInterval(currentRowIndex, colEnd);
                }
                else {
                    parentTable.setRowSelectionInterval(colEnd, currentRowIndex);
                }

                parentTable.setColumnSelectionInterval(0, parentTable.getColumnCount() - 1);
            }
        }
        */

		/*
        @Override
        protected void processMouseEvent (MouseEvent e) {
            int mouseID = e.getID();

            if (mouseID == MouseEvent.MOUSE_CLICKED) {
                if (currentRowIndex < 0) {
                    return;
                }

                if (e.isControlDown()) {
                    // select discontinuous rows
                    parentTable.addRowSelectionInterval(currentRowIndex, currentRowIndex);
                }
                else if (e.isShiftDown()) {
                    // select contiguous columns
                    if (lastRowIndex < 0) {
                        parentTable.addRowSelectionInterval(0, currentRowIndex);
                    }
                    else if (lastRowIndex < currentRowIndex) {
                        parentTable.addRowSelectionInterval(lastRowIndex, currentRowIndex);
                    }
                    else {
                        parentTable.addRowSelectionInterval(currentRowIndex, lastRowIndex);
                    }
                }
                else {
                    // clear old selection and set new column selection
                    parentTable.clearSelection();
                    parentTable.setRowSelectionInterval(currentRowIndex, currentRowIndex);
                }

                lastRowIndex = currentRowIndex;

                parentTable.setColumnSelectionInterval(0, parentTable.getColumnCount() - 1);
            }
            else if (mouseID == MouseEvent.MOUSE_PRESSED) {
                currentRowIndex = rowAtPoint(e.getPoint());
            }
        }
        */
    } // private class RowHeader extends JTable

    /**
     * RowHeaderRenderer is a custom cell renderer that displays cells as buttons.
     */
    private class RowHeaderRenderer
    {
        private static final long serialVersionUID = -8963879626159783226L;

        /*
        public RowHeaderRenderer( ) {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);

            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setBackground(Color.lightGray);
        }
        */

        /** Configures the button for the current cell, and returns it. */
        /*
        @Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            setFont(table.getFont());

            if (value != null) {
                setText(value.toString());
            }

            return this;
        }
        */
    } // private class RowHeaderRenderer extends JLabel implements
      // TableCellRenderer

    @SuppressWarnings("rawtypes")
    private class MultiLineHeaderRenderer
    //extends JList implements TableCellRenderer 
    {
        /*
    	private static final long    serialVersionUID = -3697496960833719169L;
        private final CompoundBorder subBorder        = new CompoundBorder(new MatteBorder(1, 0, 1, 0, java.awt.Color.darkGray),
                                                              new MatteBorder(1, 0, 1, 0, java.awt.Color.white));
        private final CompoundBorder majorBorder      = new CompoundBorder(new MatteBorder(1, 1, 1, 0, java.awt.Color.darkGray),
                                                              new MatteBorder(1, 2, 1, 0, java.awt.Color.white));
        Vector<String>                       lines            = new Vector<String>();
        int                          nSubcolumns      = 1;

        public MultiLineHeaderRenderer(int majorColumns, int subColumns) {
            nSubcolumns = subColumns;
            setOpaque(true);
            setForeground(UIManager.getColor("TableHeader.foreground"));
            setBackground(UIManager.getColor("TableHeader.background"));
        }

        //@Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            setFont(table.getFont());
            String str = (value == null) ? "" : value.toString();
            BufferedReader br = new BufferedReader(new StringReader(str));
            String line;

            lines.clear();
            try {
                while ((line = br.readLine()) != null) {
                    lines.addElement(line);
                }
            }
            catch (IOException ex) {
                log.debug("string read:", ex);
            }

            if ((column / nSubcolumns) * nSubcolumns == column) {
                setBorder(majorBorder);
            }
            else {
                setBorder(subBorder);
            }
            setListData(lines);

            return this;
        }
        */
    }

    // ////////////////////////////////////////////////////////////////////////
    // //
    // The code below was added to deal with region references //
    // Peter Cao, 4/30/2009 //
    // //
    // ////////////////////////////////////////////////////////////////////////

    //@Override
    public void mouseClicked (MouseEvent e) {
        /*
    	// only deal with reg. ref
        if (!(isRegRef || isObjRef)) return;

        int eMod = e.getModifiers();

        // provide two options here: double click to show data in table, or
        // right mouse to choose to show data in table or in image
        else if (e.getClickCount() == 2) {
            // double click
            viewType = ViewType.TABLE;
            Object theData = null;
            try {
                theData = ((Dataset) getDataObject()).getData();
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
            }

            if (theData == null) {
                toolkit.beep();
                JOptionPane.showMessageDialog(this, "No data selected.", getTitle(), JOptionPane.ERROR_MESSAGE);
                return;

            }

            int[] selectedRows = table.getSelectedRows();
            if (selectedRows == null || selectedRows.length <= 0) {
                return;
            }
            int len = Array.getLength(selectedRows);
            for (int i = 0; i < len; i++) {
                if (isRegRef)
                    showRegRefData((String) Array.get(theData, selectedRows[i]));
                else if (isObjRef) showObjRefData(Array.getLong(theData, selectedRows[i]));
            }
        }
        */
    }
}