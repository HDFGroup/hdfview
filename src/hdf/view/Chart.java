/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.lang.reflect.Array;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * ChartView displays a histogram/line chart of selected row/column of table data
 * or image data. There are two types of chart, histogram and line plot.
 *
 * @author Jordan T. Henderson
 * @version 2.4 2/27/16
 */
public class Chart extends Dialog {

    private Shell shell;

    private String windowTitle;

    /** histogram style chart */
    public static final int HISTOGRAM = 0;

    /** line style chart */
    public static final int LINEPLOT = 1;

    /** The default colors of lines for selected columns */
    public static final int[] LINE_COLORS = { SWT.COLOR_BLACK, SWT.COLOR_RED,
            SWT.COLOR_DARK_GREEN, SWT.COLOR_BLUE, SWT.COLOR_MAGENTA, /*Pink*/
            SWT.COLOR_YELLOW, /*Orange*/ SWT.COLOR_GRAY, SWT.COLOR_CYAN };

    /** the data values of line points or histogram */
    protected double data[][];

    /** Panel that draws plot of data values. */
    protected ChartCanvas chartP;

    /** number of data points */
    protected int numberOfPoints;

    /** the style of chart: histogram or line */
    private int chartStyle;

    /** the maximum value of the Y axis */
    private double ymax;

    /** the minimum value of the Y axis */
    private double ymin;

    /** the maximum value of the X axis */
    private double xmax;

    /** the minimum value of the X axis */
    private double xmin;

    /** line labels */
    private String lineLabels[];

    /** line colors */
    private int lineColors[];

    /** number of lines */
    private int numberOfLines;

    /* the data to plot against */
    private double[] xData = null;

    /**
    * True if the original data is integer (byte, short, integer, long).
    */
    private boolean isInteger;

    private java.text.DecimalFormat format;


    /**
    * Constructs a new ChartView given data and data ranges.
    *
    * @param parent
    *            the parent of this dialog.
    * @param title
    *            the title of this dialog.
    * @param style
    *            the style of the chart. Valid values are: HISTOGRAM and LINE
    * @param data
    *            the two dimensional data array: data[linenumber][datapoints]
    * @param xData
    *            the range of the X values, xRange[0]=xmin, xRange[1]=xmax.
    * @param yRange
    *            the range of the Y values, yRange[0]=ymin, yRange[1]=ymax.
    */
    public Chart(Shell parent, String title, int style, double[][] data, double[] xData, double[] yRange) {
        super(parent, style);

        if (data == null) {
            return;
        }

        this.windowTitle = title;

        format = new java.text.DecimalFormat("0.00E0");
        this.chartStyle = style;
        this.data = data;

        if (style == HISTOGRAM) {
            isInteger = true;
        }
        else {
            isInteger = false;
        }

        if (xData != null) {
            int len = xData.length;
            if (len == 2) {
                this.xmin = xData[0];
                this.xmax = xData[1];
            }
            else {
                this.xData = xData;
                xmin = xmax = xData[0];
                for (int i = 0; i < len; i++) {
                    if (xData[i] < xmin) {
                        xmin = xData[i];
                    }

                    if (xData[i] > xmax) {
                        xmax = xData[i];
                    }
                }
            }
        }
        else {
            this.xmin = 1;
            this.xmax = data[0].length;
        }

        this.numberOfLines = Array.getLength(data);
        this.numberOfPoints = Array.getLength(data[0]);
        this.lineColors = LINE_COLORS;

        if (yRange != null) {
            // data range is given
            this.ymin = yRange[0];
            this.ymax = yRange[1];
        }
        else {
            // search data range from the data
            findDataRange();
        }

        if ((ymax < 0.0001) || (ymax > 100000)) {
            format = new java.text.DecimalFormat("###.####E0#");
        }
    }

    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM);
        shell.setText(windowTitle);
        shell.setImage(ViewProperties.getHdfIcon());
        shell.setLayout(new GridLayout(1, true));

        chartP = new ChartCanvas(shell, SWT.DOUBLE_BUFFERED);
        chartP.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        chartP.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));


        // Add close button
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(1, true));
        buttonComposite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));

        Button closeButton = new Button(buttonComposite, SWT.PUSH);
        closeButton.setText("   &Close   ");
        closeButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
        closeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });

        shell.pack();

        int w = 640 + (ViewProperties.getFontSize() - 12) * 15;
        int h = 400 + (ViewProperties.getFontSize() - 12) * 10;

        shell.setMinimumSize(w, h);

        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();

        Display display = parent.getDisplay();
        while(!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    /** Sets the color of each line of a line plot
    *
    * @param c the list of colors
    */
    public void setLineColors(int c[]) {
        lineColors = c;
    }

    /** Sets the labels of each line.
    *
    * @param l the list of line labels
    */
    public void setLineLabels(String l[]) {
        lineLabels = l;
    }

    /** Sets the data type of the plot data to be integer. */
    public void setTypeToInteger() {
        isInteger = true;
    }

    /** Find and set the minimum and maximum values of the data */
    private void findDataRange() {
        if (data == null) {
            return;
        }

        ymin = ymax = data[0][0];
        for (int i = 0; i < numberOfLines; i++) {
            for (int j = 0; j < numberOfPoints; j++) {
                if (data[i][j] < ymin) {
                    ymin = data[i][j];
                }

                if (data[i][j] > ymax) {
                    ymax = data[i][j];
                }
            }
        }
    }

    /** The canvas that paints the data lines. */
    private class ChartCanvas extends Canvas {
        public ChartCanvas(Composite parent, int style) {
            super(parent, style | SWT.BORDER);

            this.addPaintListener(new PaintListener() {
                public void paintControl(PaintEvent e) {
                    // Get the graphics context for this paint event
                    GC g = e.gc;

                    if (numberOfLines <= 0) {
                        return; // no data
                    }

                    Point p = getSize();
                    int gap = 20;
                    int xgap = 2 * gap;
                    int ygap = 2 * gap;
                    int legendSpace = 0;
                    if ((chartStyle == LINEPLOT) && (lineLabels != null)) {
                        legendSpace = 60;
                    }

                    int h = p.y - gap;
                    int w = p.x - (3 * gap + legendSpace);
                    int xnpoints = Math.min(10, numberOfPoints - 1);
                    int ynpoints = 10;

                    // draw the X axis
                    g.drawLine(xgap, h, w + xgap, h);

                    // draw the Y axis
                    g.drawLine(ygap, h, ygap, 0);

                    // draw x labels
                    double xp = 0, x = xmin;
                    double dw = (double) w / (double) xnpoints;
                    double dx = (xmax - xmin) / xnpoints;
                    boolean gtOne = (dx >= 1);
                    for (int i = 0; i <= xnpoints; i++) {
                        x = xmin + i * dx;
                        xp = xgap + i * dw;
                        g.drawLine((int) xp, h, (int) xp, h - 5);
                        if (gtOne) {
                            g.drawString(String.valueOf((int) x), (int) xp - 5, h + gap);
                        }
                        else {
                            g.drawString(String.valueOf(x), (int) xp - 5, h + gap);
                        }
                    }

                    // draw y labels
                    double yp = 0, y = ymin;
                    double dh = (double) h / (double) ynpoints;
                    double dy = (ymax - ymin) / (ynpoints);
                    if (dy > 1) {
                        dy = Math.round(dy * 10.0) / 10.0;
                    }
                    for (int i = 0; i <= ynpoints; i++) {
                        yp = i * dh;
                        y = i * dy + ymin;
                        g.drawLine(ygap, h - (int) yp, ygap + 5, h - (int) yp);
                        if (isInteger) {
                            g.drawString(String.valueOf((int) y), 0, h - (int) yp + 8);
                        }
                        else {
                            g.drawString(format.format(y), 0, h - (int) yp + 8);
                        }
                    }

                    Color c = g.getForeground();
                    double x0, y0, x1, y1;
                    if (chartStyle == LINEPLOT) {
                        dw = (double) w / (double) (numberOfPoints - 1);

                        // use y = a + b* x to calculate pixel positions
                        double b = h / (ymin - ymax);
                        double a = -b * ymax;
                        boolean hasXdata = ((xData != null) && (xData.length >= numberOfPoints));
                        double xRatio = (1 / (xmax - xmin)) * w;
                        double xD = (xmin / (xmax - xmin)) * w;

                        // draw lines for selected spreadsheet columns
                        for (int i = 0; i < numberOfLines; i++) {
                            if ((lineColors != null)
                                    && (lineColors.length >= numberOfLines)) {

                                g.setForeground(Display.getCurrent().getSystemColor(lineColors[i]));
                            }

                            // set up the line data for drawing one line a time
                            if (hasXdata) {
                                x0 = xgap + xData[0] * xRatio - xD;
                            }
                            else {
                                x0 = xgap;
                            }
                            y0 = a + b * data[i][0];

                            for (int j = 1; j < numberOfPoints; j++) {
                                if (hasXdata) {
                                    x1 = xgap + xData[j] * xRatio - xD;
                                }
                                else {
                                    x1 = xgap + j * dw;
                                }

                                y1 = a + b * data[i][j];
                                g.drawLine((int) x0, (int) y0, (int) x1, (int) y1);

                                x0 = x1;
                                y0 = y1;
                            }

                            // draw line legend
                            if ((lineLabels != null)
                                    && (lineLabels.length >= numberOfLines)) {
                                x0 = w + legendSpace;
                                y0 = gap + gap * i;
                                g.drawLine((int) x0, (int) y0, (int) x0 + 7, (int) y0);
                                g.drawString(lineLabels[i], (int) x0 + 10, (int) y0 + 3);
                            }
                        }

                        g.setForeground(c); // set the color back to its default

                        // draw a box on the legend
                        if ((lineLabels != null)
                                && (lineLabels.length >= numberOfLines)) {
                            g.drawRectangle(w + legendSpace - 10, 10, legendSpace, 10 * gap);
                        }

                    } // if (chartStyle == LINEPLOT)
                    else if (chartStyle == HISTOGRAM) {
                        // draw histogram for selected image area
                        xp = xgap;
                        yp = 0;
                        g.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
                        int barWidth = w / numberOfPoints;
                        if (barWidth <= 0) {
                            barWidth = 1;
                        }
                        dw = (double) w / (double) numberOfPoints;
                        for (int j = 0; j < numberOfPoints; j++) {
                            xp = xgap + j * dw;
                            yp = (int) (h * (data[0][j] - ymin) / (ymax - ymin));
                            g.fillRectangle((int) xp, (int) (h - yp), barWidth, (int) yp);
                        }

                        g.setForeground(c); // set the color back to its default
                    } // else if (chartStyle == HISTOGRAM)
                }
            });
        }
    }
}
