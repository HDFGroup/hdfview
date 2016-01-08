public class NewTableDataDialogOld extends JDialog implements ActionListener, ItemListener {
    
    public NewTableDataDialogOld(JFrame owner, Group pGroup, List<?> objs) {
        memberTypeChoice = new JComboBox(DATATYPE_NAMES);
        cellEditor = new DefaultCellEditor(memberTypeChoice);
        rowEditorModel = new RowEditorModel(numberOfMembers, cellEditor);
        tableModel = new DefaultTableModel(colNames, numberOfMembers);
        table = new JTable(tableModel) {

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                TableCellEditor cellEditor = rm.getEditor(row);

                if ((cellEditor == null) || !(col == 1)) {
                    cellEditor = super.getCellEditor(row, col);
                }

                return cellEditor;
            }
        };
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();

        else if (source.equals(memberTypeChoice)) {
            String item = (String) memberTypeChoice.getSelectedItem();
            if ((item == null) || !item.equals("enum")) {
                return;
            }

            int row = table.getSelectedRow();
            table.setValueAt("mb1=0,mb=1,...", row, 2);
        }
        else if (source.equals(templateChoice)) {
            Object obj = templateChoice.getSelectedItem();
            if (!(obj instanceof CompoundDS)) {
                return;
            }

            CompoundDS dset = (CompoundDS) obj;
            int rank = dset.getRank();
            if (rank < 1) {
                dset.init();
            }

            rank = dset.getRank();
            rankChoice.setSelectedIndex(rank - 1);
            long[] dims = dset.getDims();
            String[] mNames = dset.getMemberNames();
            int[] mOrders = dset.getMemberOrders();
            Datatype[] mTypes = dset.getMemberTypes();

            String sizeStr = String.valueOf(dims[0]);
            for (int i = 1; i < rank; i++) {
                sizeStr += "x" + dims[i];
            }
            currentSizeField.setText(sizeStr);

            try {
                dset.getMetadata();
            } // get chunking and compression info
            catch (Exception ex) {
            	log.debug("get chunking and compression info:", ex);
            }
            long[] chunks = dset.getChunkSize();
            if (chunks != null) {
                checkChunked.setSelected(true);
                sizeStr = String.valueOf(chunks[0]);
                for (int i = 1; i < rank; i++) {
                    sizeStr += "x" + chunks[i];
                }
                chunkSizeField.setText(sizeStr);
            }

            String compression = dset.getCompression();
            if (compression != null) {
                int clevel = -1;
                int comp_pos = Dataset.compression_gzip_txt.length();
                int idx = compression.indexOf(Dataset.compression_gzip_txt);
                if (idx >= 0) {
                    try {
                        clevel = Integer.parseInt(compression.substring(idx + comp_pos, idx + comp_pos +1));
                    }
                    catch (NumberFormatException ex) {
                        clevel = -1;
                    }
                }
                if (clevel > 0) {
                    checkCompression.setSelected(true);
                    compressionLevel.setSelectedIndex(clevel);
                }
            }

            numberOfMembers = dset.getMemberCount();
            nFieldBox.setSelectedIndex(numberOfMembers - 1);
            tableModel.setRowCount(numberOfMembers);
            for (int i = 0; i < numberOfMembers; i++) {
                rowEditorModel.addEditorForRow(i, cellEditor);

                tableModel.setValueAt(mNames[i], i, 0);

                int typeIdx = -1;
                int tclass = mTypes[i].getDatatypeClass();
                int tsize = mTypes[i].getDatatypeSize();
                int tsigned = mTypes[i].getDatatypeSign();
                if (tclass == Datatype.CLASS_ARRAY) {
                    tclass = mTypes[i].getBasetype().getDatatypeClass();
                    tsize = mTypes[i].getBasetype().getDatatypeSize();
                    tsigned = mTypes[i].getBasetype().getDatatypeSign();
                }
                if (tclass == Datatype.CLASS_CHAR) {
                    if (tsigned == Datatype.SIGN_NONE) {
                        if (tsize == 1) {
                            typeIdx = 3;
                        }
                    }
                    else {
                        if (tsize == 1) {
                            typeIdx = 0;
                        }
                    }
                }
                if (tclass == Datatype.CLASS_INTEGER) {
                    if (tsigned == Datatype.SIGN_NONE) {
                        if (tsize == 1) {
                            typeIdx = 3;
                        }
                        else if (tsize == 2) {
                            typeIdx = 4;
                        }
                        else if (tsize == 4) {
                            typeIdx = 5;
                        }
                        else {
                            typeIdx = 11;
                        }
                    }
                    else {
                        if (tsize == 1) {
                            typeIdx = 0;
                        }
                        else if (tsize == 2) {
                            typeIdx = 1;
                        }
                        else if (tsize == 4) {
                            typeIdx = 2;
                        }
                        else {
                            typeIdx = 6;
                        }
                    }
                }
                else if (tclass == Datatype.CLASS_FLOAT) {
                    if (tsize == 4) {
                        typeIdx = 7;
                    }
                    else {
                        typeIdx = 8;
                    }
                }
                else if (tclass == Datatype.CLASS_STRING) {
                    typeIdx = 9;
                }
                else if (tclass == Datatype.CLASS_ENUM) {
                    typeIdx = 10;
                }
                if (typeIdx < 0) {
                    continue;
                }

                memberTypeChoice.setSelectedIndex(typeIdx);
                tableModel.setValueAt(memberTypeChoice.getSelectedItem(), i, 1);

                if (tclass == Datatype.CLASS_STRING) {
                    tableModel.setValueAt(String.valueOf(tsize), i, 2);
                }
                else if (tclass == Datatype.CLASS_ENUM) {
                    tableModel.setValueAt(mTypes[i].getEnumMembers(), i, 2);
                }
                else {
                    tableModel.setValueAt(String.valueOf(mOrders[i]), i, 2);
                }

            } // for (int i=0; i<numberOfMembers; i++)
        } // else if (source.equals(templateChoice))
    }

    private class RowEditorModel {
        private Hashtable<Integer, TableCellEditor> data;

        public RowEditorModel() {
            data = new Hashtable<Integer, TableCellEditor>();
        }

        // all rows has the same cell editor
        public RowEditorModel(int rows, TableCellEditor ed) {
            data = new Hashtable<Integer, TableCellEditor>();
            for (int i = 0; i < rows; i++) {
                data.put(new Integer(i), ed);
            }
        }

        public void addEditorForRow(int row, TableCellEditor e) {
            data.put(new Integer(row), e);
        }

        public void removeEditorForRow(int row) {
            data.remove(new Integer(row));
        }

        public TableCellEditor getEditor(int row) {
            return (TableCellEditor) data.get(new Integer(row));
        }
    }
}
