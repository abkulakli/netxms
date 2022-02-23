/**
 * NetXMS - open source network management system
 * Copyright (C) 2016-2021 Raden Solutions
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.IViewPart;
import org.netxms.client.NXCSession;
import org.netxms.client.dashboards.DashboardElement;
import org.netxms.ui.eclipse.console.Activator;
import org.netxms.ui.eclipse.dashboard.widgets.internal.EventMonitorConfig;
import org.netxms.ui.eclipse.eventmanager.widgets.EventTraceWidget;
import org.netxms.ui.eclipse.jobs.ConsoleJob;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;

public class EventMonitorElement extends ElementWidget
{
   private EventTraceWidget viewer;
   private EventMonitorConfig config;
   private final NXCSession session;

   /**
    * Create event monitor dashboard element.
    *
    * @param parent parent widget
    * @param element dashboard element definition
    * @param viewPart owning view part
    */
   protected EventMonitorElement(DashboardControl parent, DashboardElement element, IViewPart viewPart)
   {
      super(parent, element, viewPart);

      try
      {
         config = EventMonitorConfig.createFromXml(element.getData());
      }
      catch(Exception e)
      {
         e.printStackTrace();
         config = new EventMonitorConfig();
      }

      session = ConsoleSharedData.getSession();

      new ConsoleJob(String.format("Subscribing to channel ", NXCSession.CHANNEL_EVENTS), viewPart, Activator.PLUGIN_ID, null) {
         @Override
         protected void runInternal(IProgressMonitor monitor) throws Exception
         {
            session.subscribe(NXCSession.CHANNEL_EVENTS);
         }

         @Override
         protected String getErrorMessage()
         {
            return String.format("Cannot subscribe to channel ", NXCSession.CHANNEL_EVENTS);
         }
      }.start();

      FillLayout layout = new FillLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      setLayout(layout);

      viewer = new EventTraceWidget(this, SWT.NONE, viewPart);
      viewer.setRootObject(config.getObjectId());
      viewer.getViewer().getControl().addFocusListener(new FocusListener() {
         @Override
         public void focusLost(FocusEvent e)
         {
         }

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
      ConsoleJob job = new ConsoleJob(String.format("Unsuscribing from channel ", NXCSession.CHANNEL_EVENTS), null,
            Activator.PLUGIN_ID, null) {
         @Override
         protected void runInternal(IProgressMonitor monitor) throws Exception
         {
            session.unsubscribe(NXCSession.CHANNEL_EVENTS);
         }

         @Override
         protected String getErrorMessage()
         {
            return String.format("Cannot unsubscribe from channel ", NXCSession.CHANNEL_EVENTS);
         }
      };
      job.setUser(false);
      job.setSystem(true);
      job.start();
      super.dispose();
   }
}
