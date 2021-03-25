/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2021 Raden Solutions
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.netxms.ui.eclipse.widgets;

import java.util.Date;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.netxms.client.TimePeriod;
import org.netxms.client.constants.TimeFrameType;
import org.netxms.client.constants.TimeUnit;
import org.netxms.ui.eclipse.console.Messages;
import org.netxms.ui.eclipse.tools.WidgetFactory;
import org.netxms.ui.eclipse.tools.WidgetHelper;

/**
 * Time period selection control
 */
public class TimePeriodSelector extends Composite
{
   private Button radioBackFromNow;
   private Button radioFixedInterval;
   private Spinner timeRange;
   private Combo timeUnits;
   private DateTimeSelector timeFrom;
   private DateTimeSelector timeTo;
   
   public TimePeriodSelector(Composite parent, int style, TimePeriod period)
   {
      super(parent, style);
      
      setLayout(new FillLayout());
      
      Group timeGroup = new Group(parent, SWT.NONE);
      timeGroup.setText(Messages.get().TimePeriodSelector_TimePeriod);
      GridLayout layout = new GridLayout();
      layout.marginWidth = WidgetHelper.OUTER_SPACING;
      layout.marginHeight = WidgetHelper.OUTER_SPACING;
      layout.horizontalSpacing = 16;
      layout.makeColumnsEqualWidth = true;
      layout.numColumns = 2;
      timeGroup.setLayout(layout);
      
      final SelectionListener listener = new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            timeRange.setEnabled(radioBackFromNow.getSelection());
            timeUnits.setEnabled(radioBackFromNow.getSelection());
            timeFrom.setEnabled(radioFixedInterval.getSelection());
            timeTo.setEnabled(radioFixedInterval.getSelection());
         }
         
         @Override
         public void widgetDefaultSelected(SelectionEvent e)
         {
            widgetSelected(e);
         }
      };

      radioBackFromNow = new Button(timeGroup, SWT.RADIO);
      radioBackFromNow.setText(Messages.get().TimePeriodSelector_BackFromNow);
      radioBackFromNow.setSelection(period.getTimeFrameType() == TimeFrameType.BACK_FROM_NOW);
      radioBackFromNow.addSelectionListener(listener);

      radioFixedInterval = new Button(timeGroup, SWT.RADIO);
      radioFixedInterval.setText(Messages.get().TimePeriodSelector_FixedTimeFrame);
      radioFixedInterval.setSelection(period.getTimeFrameType() == TimeFrameType.FIXED);
      radioFixedInterval.addSelectionListener(listener);

      Composite timeBackGroup = new Composite(timeGroup, SWT.NONE);
      layout = new GridLayout();
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.horizontalSpacing = WidgetHelper.OUTER_SPACING;
      layout.numColumns = 2;
      timeBackGroup.setLayout(layout);
      GridData gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      gd.verticalAlignment = SWT.TOP;
      timeBackGroup.setLayoutData(gd);
      
      timeRange = WidgetHelper.createLabeledSpinner(timeBackGroup, SWT.BORDER, Messages.get().TimePeriodSelector_TimeInterval, 1, 10000, WidgetHelper.DEFAULT_LAYOUT_DATA);
      timeRange.setSelection(period.getTimeRange());
      timeRange.setEnabled(radioBackFromNow.getSelection());
      
      timeUnits = WidgetHelper.createLabeledCombo(timeBackGroup, SWT.READ_ONLY, Messages.get().TimePeriodSelector_TimeUnits, WidgetHelper.DEFAULT_LAYOUT_DATA);
      timeUnits.add(Messages.get().TimePeriodSelector_Minutes);
      timeUnits.add(Messages.get().TimePeriodSelector_Hours);
      timeUnits.add(Messages.get().TimePeriodSelector_Days);
      timeUnits.select(period.getTimeUnit().getValue());
      timeUnits.setEnabled(radioBackFromNow.getSelection());

      Composite timeFixedGroup = new Composite(timeGroup, SWT.NONE);
      layout = new GridLayout();
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.verticalSpacing = WidgetHelper.OUTER_SPACING;
      timeFixedGroup.setLayout(layout);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      gd.verticalAlignment = SWT.TOP;
      timeFixedGroup.setLayoutData(gd);
      
      final WidgetFactory factory = new WidgetFactory() {
         @Override
         public Control createControl(Composite parent, int style)
         {
            return new DateTimeSelector(parent, style);
         }
      };
      
      timeFrom = (DateTimeSelector)WidgetHelper.createLabeledControl(timeFixedGroup, SWT.NONE, factory, Messages.get().TimePeriodSelector_8, WidgetHelper.DEFAULT_LAYOUT_DATA);
      timeFrom.setValue(period.getTimeFrom());
      timeFrom.setEnabled(radioFixedInterval.getSelection());

      timeTo = (DateTimeSelector)WidgetHelper.createLabeledControl(timeFixedGroup, SWT.NONE, factory, Messages.get().TimePeriodSelector_9, WidgetHelper.DEFAULT_LAYOUT_DATA);
      timeTo.setValue(period.getTimeTo());
      timeTo.setEnabled(radioFixedInterval.getSelection());
   }
   
   /**
    * Get selected time period.
    *
    * @return selected time period
    */
   public TimePeriod getTimePeriod()
   {
      TimePeriod tp = new TimePeriod();
      tp.setTimeFrameType(radioBackFromNow.getSelection() ? TimeFrameType.BACK_FROM_NOW : TimeFrameType.FIXED);
      tp.setTimeRange(timeRange.getSelection());
      tp.setTimeUnit(TimeUnit.getByValue(timeUnits.getSelectionIndex()));
      tp.setTimeFrom(timeFrom.getValue());
      tp.setTimeTo(timeTo.getValue());
      return tp;
   }

   /**
    * Get time frame type for selected time period.
    *
    * @return time frame type for selected time period
    */
   public TimeFrameType getTimeFrameType()
   {
      return radioBackFromNow.getSelection() ? TimeFrameType.BACK_FROM_NOW : TimeFrameType.FIXED;
   }

   /**
    * Get time range in units.
    *
    * @return time range in units
    */
   public int getTimeRange()
   {
      return timeRange.getSelection();
   }
   
   /**
    * Get time unit.
    *
    * @return time unit
    */
   public TimeUnit getTimeUnit()
   {
      return TimeUnit.getByValue(timeUnits.getSelectionIndex());
   }
   
   /**
    * Get start time
    *
    * @return start time
    */
   public Date getTimeFrom()
   {
      return timeFrom.getValue();
   }
   
   /**
    * Get end time
    * 
    * @return end time
    */
   public Date getTimeTo()
   {
      return timeTo.getValue();
   }
   
   /**
    * Set default values
    */
   public void setDefaults()
   {
      radioBackFromNow.setSelection(true);
      radioFixedInterval.setSelection(false);
      
      timeRange.setSelection(60);
      timeRange.setEnabled(true);
      timeUnits.select(0);
      timeUnits.setEnabled(true);
      timeFrom.setEnabled(false);
      timeTo.setEnabled(false);
   }
}
