package name.abuchen.portfolio.ui.util.chart;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.events.CustomEventWizard;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

/* package */class ChartContextMenu implements Listener
{
    private static final String[] EXTENSIONS = new String[] { "*.jpeg", "*.jpg", "*.png" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private Chart chart;
    private Menu contextMenu;

    private static int lastUsedFileExtension = 0;

    public ChartContextMenu(Chart chart)
    {
        this.chart = chart;

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> configMenuAboutToShow(manager));

        contextMenu = menuMgr.createContextMenu(chart);
        chart.getPlotArea().setMenu(contextMenu);

        chart.getPlotArea().addDisposeListener(e -> dispose());

        chart.getPlotArea().addListener(SWT.MouseUp, this);
    }

    private void configMenuAboutToShow(IMenuManager manager)
    {
        Action actionAdjustRange = new Action(Messages.MenuChartAdjustRange)
        {
            @Override
            public void run()
            {
                if (chart instanceof ScatterChart)
                    ((ScatterChart) chart).adjustRange();
                else if (chart instanceof TimelineChart)
                    ((TimelineChart) chart).adjustRange();
                else
                    chart.getAxisSet().adjustRange();

                chart.redraw();
            }
        };
        actionAdjustRange.setAccelerator('0');
        manager.add(actionAdjustRange);

        manager.add(new Separator());
        addZoomActions(manager);

        manager.add(new Separator());
        addMoveActions(manager);

        manager.add(new Separator());
        exportMenuAboutToShow(manager, chart.getTitle().getText());

        manager.add(new Separator());
        addEventMenuAboutToShow(manager, chart.getTitle().getText());
    }

    private void addZoomActions(IMenuManager manager)
    {
        Action actionZoomIn = new Action(Messages.MenuChartYZoomIn)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.zoomIn();
                chart.redraw();
            }
        };
        actionZoomIn.setAccelerator(SWT.MOD1 | SWT.ARROW_UP);
        manager.add(actionZoomIn);

        Action actionZoomOut = new Action(Messages.MenuChartYZoomOut)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.zoomOut();
                chart.redraw();
            }
        };
        actionZoomOut.setAccelerator(SWT.MOD1 | SWT.ARROW_DOWN);
        manager.add(actionZoomOut);

        Action actionYZoomIn = new Action(Messages.MenuChartXZoomOut)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.zoomIn();
                chart.redraw();
            }
        };
        actionYZoomIn.setAccelerator(SWT.MOD1 | SWT.ARROW_LEFT);
        manager.add(actionYZoomIn);

        Action actionXZoomOut = new Action(Messages.MenuChartXZoomIn)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.zoomOut();
                chart.redraw();
            }
        };
        actionXZoomOut.setAccelerator(SWT.MOD1 | SWT.ARROW_RIGHT);
        manager.add(actionXZoomOut);

    }

    private void addMoveActions(IMenuManager manager)
    {
        Action actionMoveUp = new Action(Messages.MenuChartYScrollUp)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.scrollUp();
                chart.redraw();
            }
        };
        actionMoveUp.setAccelerator(SWT.ARROW_UP);
        manager.add(actionMoveUp);

        Action actionMoveDown = new Action(Messages.MenuChartYScrollDown)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.scrollDown();
                chart.redraw();
            }
        };
        actionMoveDown.setAccelerator(SWT.ARROW_DOWN);
        manager.add(actionMoveDown);

        Action actionMoveLeft = new Action(Messages.MenuChartXScrollDown)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.scrollDown();
                chart.redraw();
            }
        };
        actionMoveLeft.setAccelerator(SWT.ARROW_LEFT);
        manager.add(actionMoveLeft);

        Action actionMoveRight = new Action(Messages.MenuChartXScrollUp)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.scrollUp();
                chart.redraw();
            }
        };
        actionMoveRight.setAccelerator(SWT.ARROW_RIGHT);
        manager.add(actionMoveRight);
    }

    public void exportMenuAboutToShow(IMenuManager manager, final String label)
    {
        manager.add(new Action(Messages.MenuExportDiagram)
        {
            @Override
            public void run()
            {
                IRunnableWithProgress saveOperation = new IRunnableWithProgress()
                {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        FileDialog dialog = new FileDialog(chart.getShell(), SWT.SAVE);
                        dialog.setFileName(TextUtil.sanitizeFilename(label));
                        dialog.setFilterExtensions(EXTENSIONS);
                        dialog.setFilterIndex(lastUsedFileExtension);
                        dialog.setOverwrite(true);

                        String filename = dialog.open();
                        if (filename == null)
                            return;

                        int format;
                        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) //$NON-NLS-1$ //$NON-NLS-2$
                            format = SWT.IMAGE_JPEG;
                        else if (filename.endsWith(".png")) //$NON-NLS-1$
                            format = SWT.IMAGE_PNG;
                        else
                            format = SWT.IMAGE_UNDEFINED;

                        lastUsedFileExtension = dialog.getFilterIndex();
                        if (lastUsedFileExtension == -1)
                            lastUsedFileExtension = 0;

                        if (format != SWT.IMAGE_UNDEFINED)
                        {
                            boolean isChartTitleVisible = chart.getTitle().isVisible();
                            boolean isChartLegendVisible = chart.getLegend().isVisible();
                            try
                            {
                                chart.suspendUpdate(true);
                                chart.getTitle().setVisible(true);
                                chart.getLegend().setVisible(true);
                                chart.getLegend().setPosition(SWT.BOTTOM);
                                chart.suspendUpdate(false);
                                chart.save(filename, format);
                            }
                            finally
                            {
                                chart.suspendUpdate(true);
                                chart.getTitle().setVisible(isChartTitleVisible);
                                chart.getLegend().setVisible(isChartLegendVisible);
                                chart.suspendUpdate(false);
                            }
                        }
                    }

                };
                try
                {
                    new ProgressMonitorDialog(chart.getShell()).run(false, false, saveOperation);
                }
                catch (InvocationTargetException | InterruptedException e)
                {
                    PortfolioPlugin.log(e);
                }

            }
        });
    }

    public void addEventMenuAboutToShow(IMenuManager manager, final String label)
    {
        manager.add(new Action(Messages.SecurityMenuAddEvent)
        {
            @Override
            public void run()
            {
                CustomEventWizard wizard = new CustomEventWizard((Client) chart.getData("client"),
                                (Security) chart.getData("security"));
                wizard.getModel().setDate(convertToLocalDateViaInstant((Date) chart.getData("date")));
                if (null != chart.getData("price"))
                    wizard.getModel().setMessage(String.format("%.2f", (Double) chart.getData("price")));

                WizardDialog dialog = new WizardDialog(chart.getShell(), wizard);
                if (dialog.open() == Window.OK)
                {
                    // markDirty();
                    // notifyModelUpdated();
                }
            }
        });
    }

    private void dispose()
    {
        if (contextMenu != null && !contextMenu.isDisposed())
            contextMenu.dispose();
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event.button != 1) // use button 2 or 3 for context menu
        {
            retrieveDateAndSharePrice(event);
        }
    }

    private void retrieveDateAndSharePrice(Event event)
    {
        Date date = getFocusDateAt(event);

        List<Pair<ISeries, Double>> values = computeValues(chart.getSeriesSet().getSeries(), date);
        Double price = values.isEmpty() ? null : values.get(0).getValue();

        chart.setData("date", date);
        chart.setData("price", price);
    }

    // copy+simplify from TimelineChartToolTip
    private Date getFocusDateAt(Event event)
    {
        IAxis xAxis = chart.getAxisSet().getXAxes()[0];

        long time = (long) xAxis.getDataCoordinate(event.x);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    private Integer getFocusCategoryAt(Event event)
    {
        IAxis xAxis = chart.getAxisSet().getXAxes()[0];
        int coordinate = (int) xAxis.getDataCoordinate(event.x);

        String[] categories = xAxis.getCategorySeries();

        if (coordinate < 0)
            coordinate = 0;
        else if (coordinate > categories.length - 1)
            coordinate = categories.length - 1;

        return coordinate;
    }

    private List<Pair<ISeries, Double>> computeValues(ISeries[] allSeries, Date date)
    {
        List<Pair<ISeries, Double>> values = new ArrayList<>();

        for (ISeries series : allSeries) // NOSONAR
        {
            int line = Arrays.binarySearch(series.getXDateSeries(), date);
            if (line < 0)
                continue;
            double value = series.getYSeries()[line];

            values.add(new Pair<>(series, value));
        }

        return values;
    }

    public LocalDate convertToLocalDateViaInstant(Date dateToConvert)
    {
        return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
