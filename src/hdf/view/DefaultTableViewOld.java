public class DefaultTableViewOld implements TableView {
    public DefaultTableViewOld(ViewManager theView, HashMap map) {
    	// set cell height for large fonts
        //int cellRowHeight = table.getFontMetrics(table.getFont()).getHeight();
        //rowHeaders.setRowHeight(cellRowHeight);
        //table.setRowHeight(cellRowHeight);
    }

    // Implementing TableObserver.
    private void showLineplot() {
    	/*
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();

        if ((rows == null) || (cols == null) || (rows.length <= 0) || (cols.length <= 0)) {
            shell.getDisplay().beep();
            showError("Select rows/columns to draw line plot.", shell.getText());
            return;
        }

        int nrow = table.getRowCount();
        int ncol = table.getColumnCount();

        log.trace("DefaultTableView showLineplot: {} - {}", nrow, ncol);
        LineplotOption lpo = new LineplotOption((JFrame) viewer, "Line Plot Options -- " + dataset.getName(), nrow, ncol);
        lpo.setVisible(true);

        int plotType = lpo.getPlotBy();
        if (plotType == LineplotOption.NO_PLOT) {
            return;
        }

        boolean isRowPlot = (plotType == LineplotOption.ROW_PLOT);
        int xIndex = lpo.getXindex();

        // figure out to plot data by row or by column
        // Plot data by rows if all columns are selected and part of
        // rows are selected, otherwise plot data by column
        double[][] data = null;
        int nLines = 0;
        String title = "Lineplot - " + dataset.getPath() + dataset.getName();
        String[] lineLabels = null;
        double[] yRange = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
        double xData[] = null;

        if (isRowPlot) {
            title += " - by row";
            nLines = rows.length;
            if (nLines > 10) {
                shell.getDisplay().beep();
                nLines = 10;
                JOptionPane.showMessageDialog(this, "More than 10 rows are selected.\n" + "The first 10 rows will be displayed.",
                        getTitle(), JOptionPane.WARNING_MESSAGE);
            }
            lineLabels = new String[nLines];
            data = new double[nLines][cols.length];

            double value = 0.0;
            for (int i = 0; i < nLines; i++) {
                lineLabels[i] = String.valueOf(rows[i] + indexBase);
                for (int j = 0; j < cols.length; j++) {
                    data[i][j] = 0;
                    try {
                        value = Double.parseDouble(table.getValueAt(rows[i], cols[j]).toString());
                        data[i][j] = value;
                        yRange[0] = Math.min(yRange[0], value);
                        yRange[1] = Math.max(yRange[1], value);
                    }
                    catch (NumberFormatException ex) {
                        log.debug("rows[{}]:", i, ex);
                    }
                } // for (int j = 0; j < ncols; j++)
            } // for (int i = 0; i < rows.length; i++)

            if (xIndex >= 0) {
                xData = new double[cols.length];
                for (int j = 0; j < cols.length; j++) {
                    xData[j] = 0;
                    try {
                        value = Double.parseDouble(table.getValueAt(xIndex, cols[j]).toString());
                        xData[j] = value;
                    }
                    catch (NumberFormatException ex) {
                        log.debug("xIndex of {}:", xIndex, ex);
                    }
                }
            }
        } // if (isRowPlot)
        else {
            title += " - by column";
            nLines = cols.length;
            if (nLines > 10) {
                shell.getDisplay().beep();
                nLines = 10;
                MessageBox warning = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                warning.setText(shell.getText());
                warning.setMessage("More than 10 columns are selected.\n" + "The first 10 columns will be displayed.");
                warning.open();
            }
            lineLabels = new String[nLines];
            data = new double[nLines][rows.length];
            double value = 0.0;
            for (int j = 0; j < nLines; j++) {
                lineLabels[j] = table.getColumnName(cols[j] + indexBase);
                for (int i = 0; i < rows.length; i++) {
                    data[j][i] = 0;
                    try {
                        value = Double.parseDouble(table.getValueAt(rows[i], cols[j]).toString());
                        data[j][i] = value;
                        yRange[0] = Math.min(yRange[0], value);
                        yRange[1] = Math.max(yRange[1], value);
                    }
                    catch (NumberFormatException ex) {
                        log.debug("cols[{}]:", j, ex);
                    }
                } // for (int j=0; j<ncols; j++)
            } // for (int i=0; i<rows.length; i++)

            if (xIndex >= 0) {
                xData = new double[rows.length];
                for (int j = 0; j < rows.length; j++) {
                    xData[j] = 0;
                    try {
                        value = Double.parseDouble(table.getValueAt(rows[j], xIndex).toString());
                        xData[j] = value;
                    }
                    catch (NumberFormatException ex) {
                        log.debug("xIndex of {}:", xIndex, ex);
                    }
                }
            }
        } // else

        int n = removeInvalidPlotData(data, xData, yRange);
        if (n < data[0].length) {
            double[][] dataNew = new double[data.length][n];
            for (int i = 0; i < data.length; i++)
                System.arraycopy(data[i], 0, dataNew[i], 0, n);

            data = dataNew;

            if (xData != null) {
                double[] xDataNew = new double[n];
                System.arraycopy(xData, 0, xDataNew, 0, n);
                xData = xDataNew;
            }
        }

        // allow to draw a flat line: all values are the same
        if (yRange[0] == yRange[1]) {
            yRange[1] += 1;
            yRange[0] -= 1;
        }
        else if (yRange[0] > yRange[1]) {
            shell.getDisplay().beep();
            showError("Cannot show line plot for the selected data. \n" + "Please check the data range: ("
                    + yRange[0] + ", " + yRange[1] + ").", shell.getText());
            data = null;
            return;
        }
        if (xData == null) { // use array index and length for x data range
            xData = new double[2];
            xData[0] = indexBase; // 1- or zero-based
            xData[1] = data[0].length + indexBase - 1; // maximum index
        }

        Chart cv = new Chart((JFrame) viewer, title, Chart.LINEPLOT, data, xData, yRange);
        cv.setLineLabels(lineLabels);

        String cname = dataValue.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        if ((dname == 'B') || (dname == 'S') || (dname == 'I') || (dname == 'J')) {
            cv.setTypeToInteger();
        }

        cv.setVisible(true);
        */
    }
    
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
                super.editingStopped(e);
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
        
        return null; // Remove when fixed
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
            public boolean isCellEditable (int row, int column) {
                return !isReadOnly;
            }

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
                int row = getEditingRow();
                int col = getEditingColumn();
                super.editingStopped(e);
                startEditing[0] = false;

                Object source = e.getSource();

                if (source instanceof CellEditor) {
                    CellEditor editor = (CellEditor) source;
                    String cellValue = (String) editor.getCellEditorValue();

                    try {
                        updateValueInMemory(cellValue, row, col);
                    }
                    catch (Exception ex) {
                        toolkit.beep();
                        JOptionPane.showMessageDialog(this, ex, getTitle(), JOptionPane.ERROR_MESSAGE);
                    }
                } // if (source instanceof CellEditor)
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
    
    /**
     * Import data values from text file.
     */
    private void importTextData (String fname) {
    	/*
    	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
    	confirm.setText(shell.getText());
    	confirm.setMessage("Do you want to paste selected data?");
    	if (confirm.open() == SWT.NO) return;
    	
        int cols = table.getColumnCount();
        int rows = table.getRowCount();
        int r0 = table.getSelectedRow();
        int c0 = table.getSelectedColumn();

        if (c0 < 0) {
            c0 = 0;
        }
        if (r0 < 0) {
            r0 = 0;
        }

        // start at the first column for compound datasets
        if (dataset instanceof CompoundDS) c0 = 0;

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fname));
        }
        catch (FileNotFoundException ex) {
            log.debug("import data values from text file {}:", fname, ex);
            return;
        }

        String line = null;
        StringTokenizer tokenizer1 = null;

        try {
            line = in.readLine();
        }
        catch (IOException ex) {
            try {
                in.close();
            }
            catch (IOException ex2) {
                log.debug("close text file {}:", fname, ex2);
            }
            log.debug("read text file {}:", fname, ex);
            return;
        }

        String delName = ViewProperties.getDataDelimiter();
        String delimiter = "";

        // delimiter must include a tab to be consistent with copy/paste for
        // compound fields
        if (dataset instanceof CompoundDS)
            delimiter = "\t";
        else {
            if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_TAB)) {
                delimiter = "\t";
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SPACE)) {
                delimiter = " " + delimiter;
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COMMA)) {
                delimiter = ",";
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COLON)) {
                delimiter = ":";
            }
            else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SEMI_COLON)) {
                delimiter = ";";
            }
        }
        String token = null;
        int r = r0;
        int c = c0;
        while ((line != null) && (r < rows)) {
            if (fixedDataLength > 0) {
                // the data has fixed length
                int n = line.length();
                String theVal;
                for (int i = 0; i < n; i = i + fixedDataLength) {
                    try {
                        theVal = line.substring(i, i + fixedDataLength);
                        updateValueInMemory(theVal, r, c);
                    }
                    catch (Exception ex) {
                        continue;
                    }
                    c++;
                }
            }
            else {
                try {
                    tokenizer1 = new StringTokenizer(line, delimiter);
                    while (tokenizer1.hasMoreTokens() && (c < cols)) {
                        token = tokenizer1.nextToken();
                        if (dataset instanceof ScalarDS) {
                            StringTokenizer tokenizer2 = new StringTokenizer(token);
                            while (tokenizer2.hasMoreTokens() && (c < cols)) {
                                updateValueInMemory(tokenizer2.nextToken(), r, c);
                                c++;
                            }
                        }
                        else {
                            updateValueInMemory(token, r, c);
                            c++;
                        }
                    } // while (tokenizer1.hasMoreTokens() && index < size)
                }
                catch (Exception ex) {
                	showError(ex.getMessage(), shell.getText());
                    
                	try {
                        in.close();
                    }
                    catch (IOException ex2) {
                        log.debug("close text file {}:", fname, ex2);
                    }
                    return;
                }
            }

            try {
                line = in.readLine();
            }
            catch (IOException ex) {
                log.debug("read text file {}:", fname, ex);
                line = null;
            }
            c = 0;
            r++;
        } // while ((line != null) && (r < rows))

        try {
            in.close();
        }
        catch (IOException ex) {
            log.debug("close text file {}:", fname, ex);
        }

        table.updateUI();
        */
    }

    /**
     * Import data values from binary file.
     */
    private void importBinaryData() {
    	/*
        String currentDir = dataset.getFileFormat().getParent();
        JFileChooser fchooser = new JFileChooser(currentDir);
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterBinary());
        int returnVal = fchooser.showOpenDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File choosedFile = fchooser.getSelectedFile();
        if (choosedFile == null) {
            return;
        }
        String fname = choosedFile.getAbsolutePath();

        
        MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(shell.getText());
        confirm.setMessage("Do you want to paste selected data?");
        if (confirm.open() == SWT.NO) return;

        getBinaryDatafromFile(fname);
        */
    }

    /** Reads data from a binary file into a buffer and updates table. */
    private void getBinaryDatafromFile (String fileName) {
        String fname = fileName;
        FileInputStream inputFile = null;
        BufferedInputStream in = null;
        ByteBuffer byteBuffer = null;
        try {
            inputFile = new FileInputStream(fname);
            long fileSize = inputFile.getChannel().size();
            in = new BufferedInputStream(inputFile);

            Object data = dataset.getData();
            int datasetSize = Array.getLength(data);
            String cname = data.getClass().getName();
            char dname = cname.charAt(cname.lastIndexOf("[") + 1);

            if (dname == 'B') {
                long datasetByteSize = datasetSize;
                byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetByteSize);

                int remainingSize = bufferSize - (BYTE_BUFFER_SIZE);
                int allocValue = 0;
                int iterationNumber = 0;
                byte[] byteArray = new byte[BYTE_BUFFER_SIZE];
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (BYTE_BUFFER_SIZE);
                    }
                    else {
                        allocValue = (BYTE_BUFFER_SIZE);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);

                    byteBuffer.get(byteArray, 0, allocValue);
                    System.arraycopy(byteArray, 0, dataValue, (iterationNumber * BYTE_BUFFER_SIZE), allocValue);
                    byteBuffer.clear();
                    remainingSize = remainingSize - (BYTE_BUFFER_SIZE);
                    iterationNumber++;
                } while (remainingSize > -(BYTE_BUFFER_SIZE));

                isValueChanged = true;
            }
            else if (dname == 'S') {
                long datasetShortSize = datasetSize * 2;
                byteBuffer = ByteBuffer.allocate(SHORT_BUFFER_SIZE * 2);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetShortSize);
                int remainingSize = bufferSize - (SHORT_BUFFER_SIZE * 2);
                int allocValue = 0;
                int iterationNumber = 0;
                ShortBuffer sb = byteBuffer.asShortBuffer();
                short[] shortArray = new short[SHORT_BUFFER_SIZE];

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (SHORT_BUFFER_SIZE * 2);
                    }
                    else {
                        allocValue = (SHORT_BUFFER_SIZE * 2);
                    }
                    in.read(byteBuffer.array(), 0, allocValue);
                    sb.get(shortArray, 0, allocValue / 2);
                    System.arraycopy(shortArray, 0, dataValue, (iterationNumber * SHORT_BUFFER_SIZE), allocValue / 2);
                    byteBuffer.clear();
                    sb.clear();
                    remainingSize = remainingSize - (SHORT_BUFFER_SIZE * 2);
                    iterationNumber++;
                } while (remainingSize > -(SHORT_BUFFER_SIZE * 2));

                isValueChanged = true;
            }
            else if (dname == 'I') {
                long datasetIntSize = datasetSize * 4;
                byteBuffer = ByteBuffer.allocate(INT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetIntSize);
                int remainingSize = bufferSize - (INT_BUFFER_SIZE * 4);
                int allocValue = 0;
                int iterationNumber = 0;
                int[] intArray = new int[INT_BUFFER_SIZE];
                byte[] tmpBuf = byteBuffer.array();
                IntBuffer ib = byteBuffer.asIntBuffer();

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (INT_BUFFER_SIZE * 4);
                    }
                    else {
                        allocValue = (INT_BUFFER_SIZE * 4);
                    }
                    in.read(tmpBuf, 0, allocValue);
                    ib.get(intArray, 0, allocValue / 4);
                    System.arraycopy(intArray, 0, dataValue, (iterationNumber * INT_BUFFER_SIZE), allocValue / 4);
                    byteBuffer.clear();
                    ib.clear();
                    remainingSize = remainingSize - (INT_BUFFER_SIZE * 4);
                    iterationNumber++;
                } while (remainingSize > -(INT_BUFFER_SIZE * 4));

                isValueChanged = true;
            }
            else if (dname == 'J') {
                long datasetLongSize = datasetSize * 8;
                byteBuffer = ByteBuffer.allocate(LONG_BUFFER_SIZE * 8);

                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetLongSize);
                int remainingSize = bufferSize - (LONG_BUFFER_SIZE * 8);
                int allocValue = 0;
                int iterationNumber = 0;
                long[] longArray = new long[LONG_BUFFER_SIZE];
                LongBuffer lb = byteBuffer.asLongBuffer();

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (LONG_BUFFER_SIZE * 8);
                    }
                    else {
                        allocValue = (LONG_BUFFER_SIZE * 8);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);
                    lb.get(longArray, 0, allocValue / 8);
                    System.arraycopy(longArray, 0, dataValue, (iterationNumber * LONG_BUFFER_SIZE), allocValue / 8);
                    byteBuffer.clear();
                    lb.clear();
                    remainingSize = remainingSize - (LONG_BUFFER_SIZE * 8);
                    iterationNumber++;
                } while (remainingSize > -(LONG_BUFFER_SIZE * 8));

                isValueChanged = true;
            }
            else if (dname == 'F') {
                long datasetFloatSize = datasetSize * 4;
                byteBuffer = ByteBuffer.allocate(FLOAT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetFloatSize);
                int remainingSize = bufferSize - (FLOAT_BUFFER_SIZE * 4);
                int allocValue = 0;
                int iterationNumber = 0;
                FloatBuffer fb = byteBuffer.asFloatBuffer();
                float[] floatArray = new float[FLOAT_BUFFER_SIZE];
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (FLOAT_BUFFER_SIZE * 4);
                    }
                    else {
                        allocValue = (FLOAT_BUFFER_SIZE * 4);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);
                    fb.get(floatArray, 0, allocValue / 4);
                    System.arraycopy(floatArray, 0, dataValue, (iterationNumber * FLOAT_BUFFER_SIZE), allocValue / 4);
                    byteBuffer.clear();
                    fb.clear();
                    remainingSize = remainingSize - (FLOAT_BUFFER_SIZE * 4);
                    iterationNumber++;
                } while (remainingSize > -(FLOAT_BUFFER_SIZE * 4));

                isValueChanged = true;
            }
            else if (dname == 'D') {
                long datasetDoubleSize = datasetSize * 8;
                byteBuffer = ByteBuffer.allocate(DOUBLE_BUFFER_SIZE * 8);
                if (binaryOrder == 1)
                    byteBuffer.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) byteBuffer.order(ByteOrder.BIG_ENDIAN);

                int bufferSize = (int) Math.min(fileSize, datasetDoubleSize);
                int remainingSize = bufferSize - (DOUBLE_BUFFER_SIZE * 8);
                int allocValue = 0;
                int iterationNumber = 0;
                DoubleBuffer db = byteBuffer.asDoubleBuffer();
                double[] doubleArray = new double[DOUBLE_BUFFER_SIZE];

                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + (DOUBLE_BUFFER_SIZE * 8);
                    }
                    else {
                        allocValue = (DOUBLE_BUFFER_SIZE * 8);
                    }

                    in.read(byteBuffer.array(), 0, allocValue);
                    db.get(doubleArray, 0, allocValue / 8);
                    System.arraycopy(doubleArray, 0, dataValue, (iterationNumber * DOUBLE_BUFFER_SIZE), allocValue / 8);
                    byteBuffer.clear();
                    db.clear();
                    remainingSize = remainingSize - (DOUBLE_BUFFER_SIZE * 8);
                    iterationNumber++;
                } while (remainingSize > -(DOUBLE_BUFFER_SIZE * 8));

                isValueChanged = true;

            }

        }
        catch (Exception es) {
            es.printStackTrace();
        }
        finally {
            try {
                in.close();
                inputFile.close();
            }
            catch (IOException ex) {
                log.debug("close binary file {}:", fname, ex);
            }
        }
        
        //table.updateUI();
    }

    /** Save data as text. */
    private void saveAsText() throws Exception {
    	/*
        final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterText());
        // fchooser.changeToParentDirectory();
        fchooser.setDialogTitle("Save Current Data To Text File --- " + dataset.getName());

        File choosedFile = new File(dataset.getName() + ".txt");

        fchooser.setSelectedFile(choosedFile);
        int returnVal = fchooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        choosedFile = fchooser.getSelectedFile();
        if (choosedFile == null) {
            return;
        }
        String fname = choosedFile.getAbsolutePath();
        log.trace("DefaultTableView saveAsText: file={}", fname);

        // check if the file is in use
        List<?> fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<?> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    shell.getDisplay().beep();
                    showError("Unable to save data to file \"" + fname + "\". \nThe file is being used.", shell.getText());
                    return;
                }
            }
        }

        if (choosedFile.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText(shell.getText());
        	confirm.setMessage("File exists. Do you want to replace it?");
        	if (confirm.open() == SWT.NO) return;
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(choosedFile)));

        String delName = ViewProperties.getDataDelimiter();
        String delimiter = "";

        // delimiter must include a tab to be consistent with copy/paste for
        // compound fields
        if (dataset instanceof CompoundDS) delimiter = "\t";

        if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_TAB)) {
            delimiter = "\t";
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SPACE)) {
            delimiter = " " + delimiter;
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COMMA)) {
            delimiter = "," + delimiter;
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_COLON)) {
            delimiter = ":" + delimiter;
        }
        else if (delName.equalsIgnoreCase(ViewProperties.DELIMITER_SEMI_COLON)) {
            delimiter = ";" + delimiter;
        }

        int cols = table.getColumnCount();
        int rows = table.getRowCount();

        for (int i = 0; i < rows; i++) {
            out.print(table.getValueAt(i, 0));
            for (int j = 1; j < cols; j++) {
                out.print(delimiter);
                out.print(table.getValueAt(i, j));
            }
            out.println();
        }

        out.flush();
        out.close();

        viewer.showStatus("Data save to: " + fname);
        */
    }

    /** Save data as binary. */
    private void saveAsBinary() throws Exception {
    	/*
        final JFileChooser fchooser = new JFileChooser(dataset.getFile());
        fchooser.setFileFilter(DefaultFileFilter.getFileFilterBinary());
        // fchooser.changeToParentDirectory();
        fchooser.setDialogTitle("Save Current Data To Binary File --- " + dataset.getName());

        File choosedFile = new File(dataset.getName() + ".bin");
        fchooser.setSelectedFile(choosedFile);
        int returnVal = fchooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        choosedFile = fchooser.getSelectedFile();
        if (choosedFile == null) {
            return;
        }
        String fname = choosedFile.getAbsolutePath();
        log.trace("DefaultTableView saveAsBinary: file={}", fname);

        // check if the file is in use
        List<?> fileList = viewer.getTreeView().getCurrentFiles();
        if (fileList != null) {
            FileFormat theFile = null;
            Iterator<?> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    shell.getDisplay().beep();
                    showError("Unable to save data to file \"" + fname + "\". \nThe file is being used.", shell.getText());
                    return;
                }
            }
        }

        // check if the file exists
        if (choosedFile.exists()) {
        	MessageBox confirm = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        	confirm.setText(shell.getText());
        	confirm.setMessage("File exists. Do you want to replace it?");
        	if (confirm.open() == SWT.NO) return;
        }

        FileOutputStream outputFile = new FileOutputStream(choosedFile);
        DataOutputStream out = new DataOutputStream(outputFile);

        if (dataset instanceof ScalarDS) {
            ((ScalarDS) dataset).convertToUnsignedC();
            Object data = dataset.getData();
            String cname = data.getClass().getName();
            char dname = cname.charAt(cname.lastIndexOf("[") + 1);
            ByteBuffer bb = null;

            int size = Array.getLength(data);

            if (dname == 'B') {
                byte[] bdata = new byte[size];
                bdata = (byte[]) data;

                bb = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                int remainingSize = size - BYTE_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + BYTE_BUFFER_SIZE;
                    }
                    else {
                        allocValue = BYTE_BUFFER_SIZE;
                    }
                    bb.clear();
                    bb.put(bdata, (iterationNumber * BYTE_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue);
                    remainingSize = remainingSize - BYTE_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -BYTE_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'S') {
                short[] sdata = new short[size];
                sdata = (short[]) data;
                bb = ByteBuffer.allocate(SHORT_BUFFER_SIZE * 2);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                ShortBuffer sb = bb.asShortBuffer();
                int remainingSize = size - SHORT_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + SHORT_BUFFER_SIZE;
                    }
                    else {
                        allocValue = SHORT_BUFFER_SIZE;
                    }
                    bb.clear();
                    sb.clear();
                    sb.put(sdata, (iterationNumber * SHORT_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 2);
                    remainingSize = remainingSize - SHORT_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -SHORT_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'I') {
                int[] idata = new int[size];
                idata = (int[]) data;
                bb = ByteBuffer.allocate(INT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                IntBuffer ib = bb.asIntBuffer();
                int remainingSize = size - INT_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + INT_BUFFER_SIZE;
                    }
                    else {
                        allocValue = INT_BUFFER_SIZE;
                    }
                    bb.clear();
                    ib.clear();
                    ib.put(idata, (iterationNumber * INT_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 4);
                    remainingSize = remainingSize - INT_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -INT_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'J') {
                long[] ldata = new long[size];
                ldata = (long[]) data;

                bb = ByteBuffer.allocate(LONG_BUFFER_SIZE * 8);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                LongBuffer lb = bb.asLongBuffer();
                int remainingSize = size - LONG_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + LONG_BUFFER_SIZE;
                    }
                    else {
                        allocValue = LONG_BUFFER_SIZE;
                    }
                    bb.clear();
                    lb.clear();
                    lb.put(ldata, (iterationNumber * LONG_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 8);
                    remainingSize = remainingSize - LONG_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -LONG_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'F') {
                float[] fdata = new float[size];
                fdata = (float[]) data;

                bb = ByteBuffer.allocate(FLOAT_BUFFER_SIZE * 4);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                FloatBuffer fb = bb.asFloatBuffer();
                int remainingSize = size - FLOAT_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + FLOAT_BUFFER_SIZE;
                    }
                    else {
                        allocValue = FLOAT_BUFFER_SIZE;
                    }
                    bb.clear();
                    fb.clear();
                    fb.put(fdata, (iterationNumber * FLOAT_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 4);
                    remainingSize = remainingSize - FLOAT_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -FLOAT_BUFFER_SIZE);

                out.flush();
                out.close();
            }
            else if (dname == 'D') {
                double[] ddata = new double[size];
                ddata = (double[]) data;

                bb = ByteBuffer.allocate(DOUBLE_BUFFER_SIZE * 8);
                if (binaryOrder == 1)
                    bb.order(ByteOrder.nativeOrder());
                else if (binaryOrder == 2)
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                else if (binaryOrder == 3) bb.order(ByteOrder.BIG_ENDIAN);

                DoubleBuffer db = bb.asDoubleBuffer();
                int remainingSize = size - DOUBLE_BUFFER_SIZE;
                int allocValue = 0;
                int iterationNumber = 0;
                do {
                    if (remainingSize <= 0) {
                        allocValue = remainingSize + DOUBLE_BUFFER_SIZE;
                    }
                    else {
                        allocValue = DOUBLE_BUFFER_SIZE;
                    }
                    bb.clear();
                    db.clear();
                    db.put(ddata, (iterationNumber * DOUBLE_BUFFER_SIZE), allocValue);
                    out.write(bb.array(), 0, allocValue * 8);
                    remainingSize = remainingSize - DOUBLE_BUFFER_SIZE;
                    iterationNumber++;
                } while (remainingSize > -DOUBLE_BUFFER_SIZE);

                out.flush();
                out.close();
            }
        }

        viewer.showStatus("Data save to: " + fname);
        */
    }

    /**
     * Converting selected data based on predefined math functions.
     */
    private void mathConversion ( ) throws Exception {
    	/*
        if (isReadOnly) {
            return;
        }

        int cols = table.getSelectedColumnCount();
        // if (!(dataset instanceof ScalarDS)) return;
        if ((dataset instanceof CompoundDS) && (cols > 1)) {
            shell.getDisplay().beep();
            showError("Please select one column a time for math conversion for compound dataset.", shell.getText());
            return;
        }

        Object theData = getSelectedData();
        if (theData == null) {
            shell.getDisplay().beep();
            showError("No data is selected.", shell.getText());
            return;
        }

        MathConversionDialog dialog = new MathConversionDialog((JFrame) viewer, theData);
        dialog.setVisible(true);

        if (dialog.isConverted()) {
            if (dataset instanceof CompoundDS) {
                Object colData = null;
                try {
                    colData = ((List<?>) dataset.getData()).get(table.getSelectedColumn());
                }
                catch (Exception ex) {
                    log.debug("colData:", ex);
                }

                if (colData != null) {
                    int size = Array.getLength(theData);
                    System.arraycopy(theData, 0, colData, 0, size);
                }
            }
            else {
                int rows = table.getSelectedRowCount();
                int r0 = table.getSelectedRow();
                int c0 = table.getSelectedColumn();
                int w = table.getColumnCount();
                int idx_src = 0;
                int idx_dst = 0;
                for (int i = 0; i < rows; i++) {
                    idx_dst = (r0 + i) * w + c0;
                    System.arraycopy(theData, idx_src, dataValue, idx_dst, cols);
                    idx_src += cols;
                }
            }

            theData = null;
            System.gc();
            //table.updateUI();
            isValueChanged = true;
        }
		*/
    }

    private class LineplotOption
    //extends JDialog implements ActionListener, ItemListener 
    {
    	/*
        private static final long serialVersionUID = -3457035832213978906L;
        public static final int   NO_PLOT          = -1;
        public static final int   ROW_PLOT         = 0;
        public static final int   COLUMN_PLOT      = 1;

        private int               idx_xaxis        = -1, plotType = -1;
        private JRadioButton      rowButton, colButton;
        @SuppressWarnings("rawtypes")
        private JComboBox         rowBox, colBox;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public LineplotOption(JFrame owner, String title, int nrow, int ncol) {
            super(owner, title, true);

            rowBox = new JComboBox();
            rowBox.setEditable(false);
            colBox = new JComboBox();
            colBox.setEditable(false);

            JPanel contentPane = (JPanel) this.getContentPane();
            contentPane.setPreferredSize(new Dimension(400, 150));
            contentPane.setLayout(new BorderLayout(10, 10));

            long[] startArray = dataset.getStartDims();
            long[] strideArray = dataset.getStride();
            int[] selectedIndex = dataset.getSelectedIndex();
            int start = (int) startArray[selectedIndex[0]];
            int stride = (int) strideArray[selectedIndex[0]];

            rowBox.addItem("array index");
            for (int i = 0; i < nrow; i++) {
                rowBox.addItem("row " + (start + indexBase + i * stride));
            }

            colBox.addItem("array index");
            for (int i = 0; i < ncol; i++) {
                colBox.addItem("column " + table.getColumnName(i));
            }

            rowButton = new JRadioButton("Row");
            colButton = new JRadioButton("Column", true);
            rowButton.addItemListener(this);
            colButton.addItemListener(this);
            ButtonGroup rgroup = new ButtonGroup();
            rgroup.add(rowButton);
            rgroup.add(colButton);

            JPanel p1 = new JPanel();
            p1.setLayout(new GridLayout(2, 1, 5, 5));
            p1.add(new JLabel(" Series in:", SwingConstants.RIGHT));
            p1.add(new JLabel(" For abscissa use:", SwingConstants.RIGHT));

            JPanel p2 = new JPanel();
            p2.setLayout(new GridLayout(2, 1, 5, 5));
            // p2.setBorder(new LineBorder(Color.lightGray));
            p2.add(colButton);
            p2.add(colBox);

            JPanel p3 = new JPanel();
            p3.setLayout(new GridLayout(2, 1, 5, 5));
            // p3.setBorder(new LineBorder(Color.lightGray));
            p3.add(rowButton);
            p3.add(rowBox);

            JPanel p = new JPanel();
            p.setBorder(new LineBorder(Color.lightGray));
            p.setLayout(new GridLayout(1, 3, 20, 5));
            p.add(p1);
            p.add(p2);
            p.add(p3);

            JPanel bp = new JPanel();

            JButton okButton = new JButton("Ok");
            okButton.addActionListener(this);
            okButton.setActionCommand("Ok");
            bp.add(okButton);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(this);
            cancelButton.setActionCommand("Cancel");
            bp.add(cancelButton);

            contentPane.add(new JLabel(" Select plot options:"), BorderLayout.NORTH);
            contentPane.add(p, BorderLayout.CENTER);
            contentPane.add(bp, BorderLayout.SOUTH);

            colBox.setEnabled(colButton.isSelected());
            rowBox.setEnabled(rowButton.isSelected());

            Point l = getParent().getLocation();
            l.x += 450;
            l.y += 200;
            setLocation(l);
            pack();
        }

        int getXindex ( ) {
            return idx_xaxis;
        }

        int getPlotBy ( ) {
            return plotType;
        }

        //@Override
        public void actionPerformed (ActionEvent e) {
            e.getSource();
            String cmd = e.getActionCommand();

            if (cmd.equals("Cancel")) {
                plotType = NO_PLOT;
                this.dispose(); // terminate the application
            }
            else if (cmd.equals("Ok")) {
                if (colButton.isSelected()) {
                    idx_xaxis = colBox.getSelectedIndex() - 1;
                    plotType = COLUMN_PLOT;
                }
                else {
                    idx_xaxis = rowBox.getSelectedIndex() - 1;
                    plotType = ROW_PLOT;
                }

                this.dispose(); // terminate the application
            }
        }

        //@Override
        public void itemStateChanged (ItemEvent e) {
            Object source = e.getSource();

            if (source.equals(colButton) || source.equals(rowButton)) {
                colBox.setEnabled(colButton.isSelected());
                rowBox.setEnabled(rowButton.isSelected());
            }
        }
        */
    }

    private class ColumnHeader
    //extends JTableHeader 
    {
    	/*
        private static final long serialVersionUID   = -3179653809792147055L;
        private int               currentColumnIndex = -1;
        private int               lastColumnIndex    = -1;
        private JTable            parentTable;

        public ColumnHeader(JTable theTable) {
            super(theTable.getColumnModel());

            parentTable = theTable;
            setReorderingAllowed(false);
        }

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
                public int getColumnCount ( ) {
                    return 1;
                }

                //@Override
                public int getRowCount ( ) {
                    return rowCount;
                }

                @Override
                public String getColumnName (int col) {
                    return " ";
                }

                //@Override
                public Object getValueAt (int row, int column) {
                    log.trace("RowHeader:AbstractTableModel:getValueAt");
                    return String.valueOf(start + indexBase + row * stride);
                }
            };

            this.setModel(tm);

            // Get the only table column.
            TableColumn col = getColumnModel().getColumn(0);

            // Use the cell renderer in the column.
            col.setCellRenderer(new RowHeaderRenderer());
        }
        */

        /** Overridden to return false since the headers are not editable. */
        /*
    	@Override
        public boolean isCellEditable (int row, int col) {
            return false;
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
    //extends JLabel implements TableCellRenderer 
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

        // right mouse click
        if (e.isPopupTrigger()
                || (eMod == InputEvent.BUTTON3_MASK)
                || (System.getProperty("os.name").startsWith("Mac")
                && (eMod == (InputEvent.BUTTON1_MASK
                | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())))) {
            if (popupMenu != null) {
                popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
            }
        }
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