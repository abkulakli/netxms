/**
 * NetXMS - open source network management system
 * Copyright (C) 2016-2022 RadenSolutions
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.netxms.client.NXCSession;
import org.netxms.client.dashboards.DashboardElement;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.base.jobs.Job;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.dashboards.config.SyslogMonitorConfig;
import org.netxms.nxmc.modules.dashboards.views.AbstractDashboardView;
import org.netxms.nxmc.modules.events.widgets.SyslogTraceWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

/**
 * "Syslog monitor" dashboard element
 */
public class SyslogMonitorElement extends ElementWidget
{
   private static final Logger logger = LoggerFactory.getLogger(SyslogMonitorElement.class);

   private final I18n i18n = LocalizationHelper.getI18n(SyslogMonitorElement.class);

   private SyslogTraceWidget viewer;
   private SyslogMonitorConfig config;
   private final NXCSession session;

   /**
    * Create syslog monitor element.
    *
    * @param parent parent composite
    * @param element dashboard element
    * @param view owning view part
    */
   protected SyslogMonitorElement(DashboardControl parent, DashboardElement element, AbstractDashboardView view)
   {
      super(parent, element, view);

      try
      {
         config = SyslogMonitorConfig.createFromXml(element.getData());
      }
      catch(Exception e)
      {
         logger.error("Cannot parse dashboard element configuration", e);
         config = new SyslogMonitorConfig();
      }

      processCommonSettings(config);

      session = Registry.getSession();

      new Job(String.format(i18n.tr("Subscribing to channel %s"), NXCSession.CHANNEL_SYSLOG), view, this) {
         @Override
         protected void run(IProgressMonitor monitor) throws Exception
         {
            session.subscribe(NXCSession.CHANNEL_SYSLOG);
         }

         @Override
         protected String getErrorMessage()
         {
            return String.format(i18n.tr("Cannot subscribe to channel %s"), NXCSession.CHANNEL_SYSLOG);
         }
      }.start();

      viewer = new SyslogTraceWidget(getContentArea(), SWT.NONE, view);
      viewer.setRootObject(getEffectiveObjectId(config.getObjectId()));
      viewer.getViewer().getControl().addFocusListener(new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e)
         {
            setSelectionProviderDelegate(viewer.getSelectionProvider());
         }
      });

      addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(DisposeEvent e)
         {
            unsubscribe();
         }
      });
   }

   /**
    * Unsubscribe from notifications
    */
   private void unsubscribe()
   {
      Job job = new Job(String.format(i18n.tr("Unsuscribing from channel %s"), NXCSession.CHANNEL_SYSLOG), null) {
         @Override
         protected void run(IProgressMonitor monitor) throws Exception
         {
            session.unsubscribe(NXCSession.CHANNEL_SYSLOG);
         }

         @Override
         protected String getErrorMessage()
         {
            return String.format(i18n.tr("Cannot unsubscribe from channel %s"), NXCSession.CHANNEL_SYSLOG);
         }
      };
      job.setUser(false);
      job.setSystem(true);
      job.start();
      super.dispose();
   }
}