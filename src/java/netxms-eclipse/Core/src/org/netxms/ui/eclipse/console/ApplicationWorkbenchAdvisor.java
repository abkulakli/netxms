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
package org.netxms.ui.eclipse.console;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.model.ContributionComparator;
import org.eclipse.ui.model.IContributionService;
import org.eclipse.ui.progress.UIJob;
import org.netxms.client.NXCSession;
import org.netxms.client.SessionListener;
import org.netxms.client.SessionNotification;
import org.netxms.ui.eclipse.console.dialogs.SelectPerspectiveDialog;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;
import org.netxms.ui.eclipse.widgets.PerspectiveSwitcher;

/**
 * Workbench advisor for NetXMS console application
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{
   /**
    * @see org.eclipse.ui.application.WorkbenchAdvisor#createWorkbenchWindowAdvisor(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
    */
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
	{
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

   /**
    * @see org.eclipse.ui.application.WorkbenchAdvisor#getInitialWindowPerspectiveId()
    */
	public String getInitialWindowPerspectiveId()
	{
		String p = BrandingManager.getInstance().getDefaultPerspective();
		if (p != null)
			return p;
		return Activator.getDefault().getPreferenceStore().getString("INITIAL_PERSPECTIVE"); //$NON-NLS-1$
	}

   /**
    * @see org.eclipse.ui.application.WorkbenchAdvisor#initialize(org.eclipse.ui.application.IWorkbenchConfigurer)
    */
	@Override
	public void initialize(IWorkbenchConfigurer configurer)
	{
		super.initialize(configurer);

		// Early creation of progress service to prevent NPE in Eclipse 4.2
		PlatformUI.getWorkbench().getProgressService();

		TweakletManager.initTweaklets();
		BrandingManager.create();

		final IPreferenceStore ps = Activator.getDefault().getPreferenceStore();
		configurer.setSaveAndRestore(ps.getBoolean("SAVE_AND_RESTORE")); //$NON-NLS-1$

		if (ps.getBoolean("HTTP_PROXY_ENABLED")) //$NON-NLS-1$
		{
			System.setProperty("http.proxyHost", ps.getString("HTTP_PROXY_SERVER")); //$NON-NLS-1$ //$NON-NLS-2$
			System.setProperty("http.proxyPort", ps.getString("HTTP_PROXY_PORT")); //$NON-NLS-1$ //$NON-NLS-2$
			System.setProperty("http.noProxyHosts", ps.getString("HTTP_PROXY_EXCLUSIONS")); //$NON-NLS-1$ //$NON-NLS-2$
         System.setProperty("https.proxyHost", ps.getString("HTTP_PROXY_SERVER")); //$NON-NLS-1$ //$NON-NLS-2$
         System.setProperty("https.proxyPort", ps.getString("HTTP_PROXY_PORT")); //$NON-NLS-1$ //$NON-NLS-2$
         System.setProperty("https.noProxyHosts", ps.getString("HTTP_PROXY_EXCLUSIONS")); //$NON-NLS-1$ //$NON-NLS-2$
			if (ps.getBoolean("HTTP_PROXY_AUTH")) //$NON-NLS-1$
			{
				Authenticator.setDefault(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(ps.getString("HTTP_PROXY_LOGIN"), ps.getString("HTTP_PROXY_PASSWORD").toCharArray()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
			}
		}
		else
		{
			System.clearProperty("http.proxyHost"); //$NON-NLS-1$
			System.clearProperty("http.proxyPort"); //$NON-NLS-1$
			System.clearProperty("http.noProxyHosts"); //$NON-NLS-1$
         System.clearProperty("https.proxyHost"); //$NON-NLS-1$
         System.clearProperty("https.proxyPort"); //$NON-NLS-1$
         System.clearProperty("https.noProxyHosts"); //$NON-NLS-1$
		}

      PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
         private Set<IWorkbenchWindow> windows = new HashSet<>();

         @Override
         public void windowOpened(IWorkbenchWindow window)
         {
            NXCSession session = ConsoleSharedData.getSession();
            if (!windows.contains(window) && session.getClientConfigurationHintAsBoolean("PerspectiveSwitcher.Enable", true))
            {
               new PerspectiveSwitcher(window);
               windows.add(window);
            }
            if (session.getClientConfigurationHintAsBoolean("PerspectiveSwitcher.Enable", true) && session.getClientConfigurationHintAsBoolean("PerspectiveSwitcher.ShowOnStartup", false))
            {
               new UIJob("Select perspective") {
                  @Override
                  public IStatus runInUIThread(IProgressMonitor monitor)
                  {
                     IPerspectiveDescriptor currPerspective = window.getActivePage().getPerspective();
                     IPerspectiveDescriptor p = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId("org.netxms.ui.eclipse.console.SwitcherPerspective");
                     if (p != null)
                        window.getActivePage().setPerspective(p);
                     SelectPerspectiveDialog dlg = new SelectPerspectiveDialog(null);
                     if (dlg.open() == Window.OK)
                     {
                        window.getActivePage().setPerspective(dlg.getSelectedPerspective());
                     }
                     else
                     {
                        window.getActivePage().setPerspective(currPerspective);
                     }
                     return Status.OK_STATUS;
                  }
               }.schedule();
            }
         }

         @Override
         public void windowDeactivated(IWorkbenchWindow window)
         {
         }

         @Override
         public void windowClosed(IWorkbenchWindow window)
         {
            windows.remove(window);
         }

         @Override
         public void windowActivated(IWorkbenchWindow window)
         {
            NXCSession session = ConsoleSharedData.getSession();
            if (!windows.contains(window) && session.getClientConfigurationHintAsBoolean("PerspectiveSwitcher.Enable", true))
            {
               new PerspectiveSwitcher(window);
               windows.add(window);
            }
         }
      });
	}

   /**
    * @see org.eclipse.ui.application.WorkbenchAdvisor#getComparatorFor(java.lang.String)
    */
	@Override
	public ContributionComparator getComparatorFor(String contributionType)
	{
		if (contributionType.equals(IContributionService.TYPE_PROPERTY))
			return new ExtendedContributionComparator();
		return super.getComparatorFor(contributionType);
	}

   /**
    * @see org.eclipse.ui.application.WorkbenchAdvisor#postStartup()
    */
	@Override
	public void postStartup()
	{
		super.postStartup();
		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		shell.addShellListener(new ShellAdapter()	{
			public void shellIconified(ShellEvent e)
			{
				if (Activator.getDefault().getPreferenceStore().getBoolean("HIDE_WHEN_MINIMIZED")) //$NON-NLS-1$
					shell.setVisible(false);
			}
		});
		
      final NXCSession session = ConsoleSharedData.getSession();
		session.addListener(new SessionListener() {
			@Override
			public void notificationHandler(final SessionNotification n)
			{
				if ((n.getCode() == SessionNotification.CONNECTION_BROKEN) ||
				    (n.getCode() == SessionNotification.SERVER_SHUTDOWN) || 
				    (n.getCode() == SessionNotification.SESSION_KILLED))
				{
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run()
						{
                    	String productName = BrandingManager.getInstance().getProductName();
                     MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								Messages.get().ApplicationWorkbenchAdvisor_CommunicationError,
                           ((n.getCode() == SessionNotification.CONNECTION_BROKEN) ? 
                                 String.format(Messages.get().ApplicationWorkbenchAdvisor_ConnectionLostMessage, productName)
                                 : ((n.getCode() == SessionNotification.SESSION_KILLED) ? 
                                       Messages.get().ApplicationWorkbenchAdvisor_SessionTerminated
                                       : String.format(Messages.get().ApplicationWorkbenchAdvisor_ServerShutdownMessage, productName)))
                                 + Messages.get().ApplicationWorkbenchAdvisor_OKToCloseMessage);
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().close();
						}
					});
				}
			}
		});
	}
}
