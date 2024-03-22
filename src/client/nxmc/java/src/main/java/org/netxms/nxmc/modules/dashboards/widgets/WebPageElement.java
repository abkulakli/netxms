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
package org.netxms.nxmc.modules.dashboards.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.netxms.client.dashboards.DashboardElement;
import org.netxms.client.xml.XMLTools;
import org.netxms.nxmc.modules.dashboards.config.WebPageConfig;
import org.netxms.nxmc.modules.dashboards.views.AbstractDashboardView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded web page element for dashboard
 */
public class WebPageElement extends ElementWidget
{
   private static final Logger logger = LoggerFactory.getLogger(WebPageElement.class);
   
	private Browser browser;
	private WebPageConfig config;
	
	/**
    * @param parent
    * @param element
    * @param view
    */
   public WebPageElement(DashboardControl parent, DashboardElement element, AbstractDashboardView view)
	{
      super(parent, element, view);

		try
		{
         config = XMLTools.createFromXml(WebPageConfig.class, element.getData());
		}
		catch(Exception e)
		{
         logger.error("Cannot parse dashboard element configuration", e);
			config = new WebPageConfig();
		}

      processCommonSettings(config);

      browser = new Browser(getContentArea(), SWT.NONE);
		browser.setUrl(config.getUrl());
	}
}
