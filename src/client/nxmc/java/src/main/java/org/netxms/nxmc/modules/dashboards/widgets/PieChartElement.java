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
import org.netxms.client.dashboards.DashboardElement;
import org.netxms.client.datacollection.ChartConfiguration;
import org.netxms.client.datacollection.ChartDciConfig;
import org.netxms.client.xml.XMLTools;
import org.netxms.nxmc.modules.charts.api.ChartType;
import org.netxms.nxmc.modules.charts.widgets.Chart;
import org.netxms.nxmc.modules.dashboards.config.PieChartConfig;
import org.netxms.nxmc.modules.dashboards.views.AbstractDashboardView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pie chart element
 */
public class PieChartElement extends ComparisonChartElement
{
   private static final Logger logger = LoggerFactory.getLogger(PieChartElement.class);
   private PieChartConfig elementConfig;

	/**
	 * @param parent
	 * @param data
	 */
   public PieChartElement(DashboardControl parent, DashboardElement element, AbstractDashboardView view)
	{
      super(parent, element, view);

		try
		{
         elementConfig = XMLTools.createFromXml(PieChartConfig.class, element.getData());
		}
		catch(Exception e)
		{
         logger.error("Cannot parse dashboard element configuration", e);
         elementConfig = new PieChartConfig();
		}

      processCommonSettings(elementConfig);

      refreshInterval = elementConfig.getRefreshRate();

      ChartConfiguration chartConfig = new ChartConfiguration();
      chartConfig.setTitleVisible(false);
      chartConfig.setLegendPosition(elementConfig.getLegendPosition());
      chartConfig.setLegendVisible(elementConfig.isShowLegend());
      chartConfig.setExtendedLegend(elementConfig.isExtendedLegend());
      chartConfig.setTranslucent(elementConfig.isTranslucent());
      chartConfig.setDoughnutRendering(elementConfig.isDoughnutRendering());
      chartConfig.setShowTotal(elementConfig.isShowTotal());

      chart = new Chart(getContentArea(), SWT.NONE, ChartType.PIE, chartConfig);
      chart.setDrillDownObjectId(elementConfig.getDrillDownObjectId());

      configureMetrics();
	}

   /**
    * @see org.netxms.ui.eclipse.dashboard.widgets.ComparisonChartElement#getDciList()
    */
	@Override
	protected ChartDciConfig[] getDciList()
	{
      return elementConfig.getDciList();
	}
}
