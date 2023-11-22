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
package org.netxms.ui.eclipse.reporter.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.netxms.client.NXCException;
import org.netxms.client.NXCSession;
import org.netxms.client.constants.RCC;
import org.netxms.client.reporting.ReportDefinition;
import org.netxms.ui.eclipse.actions.RefreshAction;
import org.netxms.ui.eclipse.jobs.ConsoleJob;
import org.netxms.ui.eclipse.reporter.Activator;
import org.netxms.ui.eclipse.reporter.widgets.internal.ReportTreeContentProvider;
import org.netxms.ui.eclipse.reporter.widgets.internal.ReportTreeLabelProvider;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;

/**
 * Report navigation view
 */
public class ReportNavigator extends ViewPart
{
	public static final String ID = "org.netxms.ui.eclipse.reporter.views.ReportNavigator"; //$NON-NLS-1$
	
	private NXCSession session;
	private TreeViewer reportTree;
	private RefreshAction actionRefresh;

   /**
    * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
    */
	@Override
	public void createPartControl(Composite parent)
	{
      session = ConsoleSharedData.getSession();

		reportTree = new TreeViewer(parent, SWT.NONE);
		reportTree.setContentProvider(new ReportTreeContentProvider());
		reportTree.setLabelProvider(new ReportTreeLabelProvider());

		createActions();
		contributeToActionBars();
		createPopupMenu();

		getSite().setSelectionProvider(reportTree);

		refresh();
	}

	/**
	 * Create actions
	 */
	private void createActions()
	{
		actionRefresh = new RefreshAction() {
			@Override
			public void run()
			{
				refresh();
			}
		};
	}

	/**
	 * Contribute actions to action bar
	 */
	private void contributeToActionBars()
	{
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * Fill local pull-down menu
	 * 
	 * @param manager
	 *           Menu manager for pull-down menu
	 */
	private void fillLocalPullDown(IMenuManager manager)
	{
		manager.add(actionRefresh);
	}

	/**
	 * Fill local tool bar
	 * 
	 * @param manager
	 *           Menu manager for local toolbar
	 */
	private void fillLocalToolBar(IToolBarManager manager)
	{
		manager.add(actionRefresh);
	}

	/**
	 * Create popup menu for report list
	 */
	private void createPopupMenu()
	{
		// Create menu manager.
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr)
			{
				fillContextMenu(mgr);
			}
		});

		// Create menu.
		Menu menu = manager.createContextMenu(reportTree.getTree());
		reportTree.getTree().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(manager, reportTree);
	}

	/**
	 * Fill context menu
	 * @param manager Menu manager
	 */
	protected void fillContextMenu(IMenuManager manager)
	{
	}

   /**
    * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
    */
	@Override
	public void setFocus()
	{
		reportTree.getTree().setFocus();
	}

	/**
	 * Refresh reports tree
	 */
	private void refresh()
	{
      new ConsoleJob("Loading report definitions", this, Activator.PLUGIN_ID) {
			@Override
			protected void runInternal(IProgressMonitor monitor) throws Exception
			{
				final List<ReportDefinition> definitions = new ArrayList<ReportDefinition>();
				final List<UUID> reportIds = session.listReports();
				for(UUID reportId : reportIds)
				{
					try
					{
						final ReportDefinition definition = session.getReportDefinition(reportId);
						definitions.add(definition);
					}
					catch (NXCException e)
					{
						if (e.getErrorCode() == RCC.INTERNAL_ERROR)
                     definitions.add(new ReportDefinition(reportId));
					}
				}
				Collections.sort(definitions, (d1, d2) -> d1.getName().compareTo(d2.getName()));
            runInUIThread(() -> {
               reportTree.setInput(definitions);
				});
			}

			@Override
			protected String getErrorMessage()
			{
            return "Error loading report definitions";
			}
		}.start();
	}
}
