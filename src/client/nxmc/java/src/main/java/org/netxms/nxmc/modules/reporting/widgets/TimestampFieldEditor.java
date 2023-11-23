/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2022 Victor Kirhenshtein
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
package org.netxms.nxmc.modules.reporting.widgets;

import java.util.Calendar;
import java.util.Date;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.netxms.client.reporting.ReportParameter;
import org.netxms.nxmc.tools.WidgetHelper;

/**
 * Editor for timestamp parameters
 */
public class TimestampFieldEditor extends ReportFieldEditor
{
	private DateTime datePicker;
	private DateTime timePicker;

	/**
	 * @param parameter
	 * @param toolkit
	 * @param parent
	 */
   public TimestampFieldEditor(ReportParameter parameter, Composite parent)
	{
      super(parameter, parent);
	}

   /**
    * @see org.netxms.ReportFieldEditor.eclipse.reporter.widgets.FieldEditor#createContent(org.eclipse.swt.widgets.Composite)
    */
	@Override
	protected Control createContent(Composite parent)
	{
		final Composite area = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = WidgetHelper.INNER_SPACING;
		area.setLayout(layout);

		Date date;
		try
		{
			date = new Date(Long.parseLong(parameter.getDefaultValue()) * 1000);
		}
		catch(NumberFormatException e)
		{
			date = new Date();
		}

		final Calendar c = Calendar.getInstance();
		c.setTime(date);

		datePicker = new DateTime(area, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
		datePicker.setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

      timePicker = new DateTime(area, SWT.TIME | SWT.BORDER);
      timePicker.setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));

		return area;
	}

   /**
    * @see org.netxms.ReportFieldEditor.eclipse.reporter.widgets.FieldEditor#getValue()
    */
	@Override
	public String getValue()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.clear();
      calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDay(), timePicker.getHours(), timePicker.getMinutes(), timePicker.getSeconds());
		return Long.toString(calendar.getTimeInMillis() / 1000);
	}
}
