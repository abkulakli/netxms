/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Victor Kirhenshtein
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
package org.netxms.ui.eclipse.dashboard.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;
import org.netxms.client.dashboards.DashboardElement;
import org.netxms.ui.eclipse.dashboard.widgets.internal.LabelConfig;
import org.netxms.ui.eclipse.tools.ColorConverter;
import org.netxms.ui.eclipse.tools.FontTools;

/**
 * Dashboard label element
 */
public class LabelElement extends ElementWidget
{
	private LabelConfig config; 
	private Label label;
	private Font font;

	/**
	 * @param parent
	 * @param data
	 */
	public LabelElement(DashboardControl parent, DashboardElement element, IViewPart viewPart)
	{
		super(parent, element, viewPart);

		try
		{
			config = LabelConfig.createFromXml(element.getData());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			config = new LabelConfig();
		}

      // Skip common settings processing

      FillLayout layout = new FillLayout();
      layout.marginHeight = 4;
      getContentArea().setLayout(layout);
      label = new Label(getContentArea(), SWT.CENTER);
		label.setText(config.getTitle());
      label.setBackground(colors.create(ColorConverter.parseColorDefinition(config.getTitleBackground())));
      label.setForeground(colors.create(ColorConverter.parseColorDefinition(config.getTitleForeground())));
      getContentArea().setBackground(label.getBackground());

      font = FontTools.createAdjustedFont(FontTools.getTitleFont(), config.getTitleFontSize());
      label.setFont(font);

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e)
			{
				font.dispose();
			}
		});
	}

   /**
    * @see org.netxms.nxmc.modules.dashboards.widgets.ElementWidget#getPreferredHeight()
    */
   @Override
   protected int getPreferredHeight()
   {
      return 16;
   }
}
