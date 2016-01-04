public class NewDatasetDialogOld extends JDialog implements ActionListener, ItemListener, HyperlinkListener {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewDatasetDialogOld(JFrame owner, Group pGroup, List<?> objs) {
        int w = 600 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 350 + (ViewProperties.getFontSize() - 12) * 10;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NewDatasetDialogOld(JFrame owner, Group pGroup, List<?> objs, DataView observer) {
        parentChoice = new JComboBox();
        groupList = new Vector<Object>();
        Object obj = null;
        Iterator<?> iterator = objs.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(obj);
                if (g.isRoot()) {
                    parentChoice.addItem(HObject.separator);
                }
                else {
                    parentChoice.addItem(g.getPath() + g.getName() + HObject.separator);
                }
            }
        }

        if (pGroup.isRoot()) {
            parentChoice.setSelectedItem(HObject.separator);
        }
        else {
            parentChoice.setSelectedItem(pGroup.getPath() + pGroup.getName() + HObject.separator);
        }

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        int w = 400 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 120 + (ViewProperties.getFontSize() - 12) * 10;
        contentPane.setPreferredSize(new Dimension(w, h));

        JButton okButton = new JButton("   Ok   ");
        okButton.setActionCommand("Ok");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);

        // set OK and CANCEL buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // set NAME and PARENT GROUP panel
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout(5, 5));
        JPanel tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(new JLabel("Dataset name: "));
        tmpP.add(new JLabel("Parent group: "));
        namePanel.add(tmpP, BorderLayout.WEST);
        tmpP = new JPanel();
        tmpP.setLayout(new GridLayout(2, 1));
        tmpP.add(nameField = new JTextField(((HObject) observer.getDataObject()).getName() + "~copy", 40));
        tmpP.add(parentChoice);
        namePanel.add(tmpP, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.CENTER);
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane) e.getSource();

            if (e instanceof HTMLFrameHyperlinkEvent) {
                HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                doc.processHTMLFrameHyperlinkEvent(evt);
            }
            else {
                try {
                    pane.setPage(e.getURL());
                }
                catch (Throwable t) {
                    log.debug("JEditorPane hyperlink:", t);
                }
            }
        }
    }
}
