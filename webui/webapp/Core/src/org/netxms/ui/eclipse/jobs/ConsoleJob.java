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
package org.netxms.ui.eclipse.jobs;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.netxms.client.NXCException;
import org.netxms.ui.eclipse.console.Activator;
import org.netxms.ui.eclipse.console.Messages;
import org.netxms.ui.eclipse.widgets.MessageBar;

/**
 * Tailored Job class for NetXMS console. Callers must call start() instead of schedule() for correct execution.
 */
public abstract class ConsoleJob extends Job
{
   private IWorkbenchSiteProgressService siteService;
   private String pluginId;
   private MessageBar messageBar;
   private boolean passException = true;
   private Display display;

   /**
    * Constructor for console job object
    * 
    * @param name Job's name
    * @param viewPart Related view part or null
    * @param pluginId Related plugin ID
    */
   public ConsoleJob(String name, IWorkbenchPart wbPart, String pluginId)
   {
      this(name, wbPart, pluginId, null);
   }

   /**
    * Constructor for console job object
    * 
    * @param name Job's name
    * @param viewPart Related view part or null
    * @param pluginId Related plugin ID
    * @param messageBar Message bar for displaying error or null
    */
   public ConsoleJob(String name, IWorkbenchPart wbPart, String pluginId, MessageBar messageBar)
   {
      super(name);
      this.pluginId = (pluginId != null) ? pluginId : Activator.PLUGIN_ID;
      this.messageBar = messageBar;
      siteService = (wbPart != null) ? 
         (IWorkbenchSiteProgressService)wbPart.getSite().getService(IWorkbenchSiteProgressService.class) : null;
      setUser(true);
      display = Display.getCurrent();
      if (display == null)
         throw new IllegalThreadStateException("ConsoleJob constructor called from non-UI thread");
   }

	/**
	 * Constructor for console job object
	 * 
	 * @param name Job's name
	 * @param viewPart Related view part or null
	 * @param pluginId Related plugin ID
	 * @param jobFamily Job's family or null
	 */
	public ConsoleJob(String name, IWorkbenchPart wbPart, String pluginId, MessageBar messageBar, Display display)
	{
		super(name);
		this.pluginId = (pluginId != null) ? pluginId : Activator.PLUGIN_ID;
		this.messageBar = messageBar;
		siteService = (wbPart != null) ? 
					(IWorkbenchSiteProgressService)wbPart.getSite().getService(IWorkbenchSiteProgressService.class) : null;
		setUser(true);
		this.display = display;
	}

   /**
    * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
    */
   @Override
   protected IStatus run(IProgressMonitor monitor)
   {
      IStatus status;
      try
      {
         runInternal(monitor);
         status = Status.OK_STATUS;
         if (messageBar != null)
         {
            runInUIThread(new Runnable() {
               @Override
               public void run()
               {
                  messageBar.hideMessage();
               }
            });
         }
      }
      catch(Exception e)
      {
         Activator.logError("Exception in ConsoleJob", e); //$NON-NLS-1$
         jobFailureHandler();
         if (messageBar != null)
         {
            status = Status.OK_STATUS;
            runInUIThread(new Runnable() {
               @Override
               public void run()
               {
                  messageBar.showMessage(MessageBar.ERROR, getErrorMessage() + ": " + e.getMessage());
               }
            });
         }
         else
         {
            status = createFailureStatus(e);
         }
      }
      finally
      {
         jobFinalize();
      }
      return status;
   }

   /**
    * Start job.
    */
   public void start()
   {
      display.asyncExec(new Starter());
   }

   /**
    * Starter class
    */
   public class Starter implements Runnable
   {
      @Override
      public void run()
      {
         if (siteService != null)
            siteService.schedule(ConsoleJob.this, 0, true);
         else
            schedule();
      }
   }

   /**
    * Executes job. Called from within Job.run(). If job fails, this method should throw appropriate exception.
    * 
    * @param monitor the monitor to be used for reporting progress and responding to cancellation. The monitor is never null.
    * @throws Exception in case of any failure.
    */
   protected abstract void runInternal(IProgressMonitor monitor) throws Exception;

   /**
    * Should return error message which will be shown in case of job failure.
    * Result of exception's getMessage() will be appended to this message.
    * 
    * @return Error message
    */
   protected abstract String getErrorMessage();

	/**
	 * Helper class to call from non-UI thread
	 */
	private static class CallHelper implements Runnable
	{
		ConsoleJob job;
		String message;
		
		public CallHelper(ConsoleJob job)
		{
			this.job = job;
		}

		@Override
		public void run()
		{
			message = job.getErrorMessage();
		}
	}

	/**
	 * Called from within Job.run() if job has failed to create failure status. Subclasses may override
	 * and return Status.OK_STATUS to avoid standard job failure message to pop up.
	 */
	protected IStatus createFailureStatus(final Exception e)
	{
		CallHelper ch = new CallHelper(this);
		display.syncExec(ch);
		
		return new Status(Status.ERROR, pluginId, 
            (e instanceof NXCException) ? ((NXCException)e).getErrorCode() : 0,
            ch.message + ": " + e.getLocalizedMessage(), passException ? e : null); //$NON-NLS-1$
	}

   /**
    * Called from within Job.run() if job has failed. Default implementation does nothing.
    */
   protected void jobFailureHandler()
   {
   }

   /**
    * Called from within Job.run() if job completes, either successfully or not. Default implementation does nothing.
    */
   protected void jobFinalize()
   {
   }

   /**
    * Run job in foreground using IProgressService.busyCursorWhile
    * 
    * @return true if job was successful
    */
   public boolean runInForeground()
   {
      passException = true;
      IProgressService service = PlatformUI.getWorkbench().getProgressService();
      boolean success = true;
      try
      {
         service.run(true, false, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
               IStatus status = ConsoleJob.this.run(monitor);
               if (!status.isOK())
               {
                  throw new InvocationTargetException(status.getException());
               }
            }
         });
      }
      catch(InvocationTargetException e)
      {
         Throwable cause = e.getCause();
         if (cause == null)
            cause = e;
         MessageDialog.openError(null, Messages.get().ConsoleJob_ErrorDialogTitle, getErrorMessage() + ": " + cause.getLocalizedMessage()); //$NON-NLS-1$
      }
      catch(InterruptedException e)
      {
      }
      return success;
   }

   /**
    * Run job in background thread, not using job manager
    *
    * @param monitor progress monitor
    */
   public void runInBackground(IProgressMonitor monitor)
   {
      Thread thread = new Thread(new Runnable() {
         @Override
         public void run()
         {
            ConsoleJob.this.run(monitor);
         }
      });
      thread.setDaemon(true);
      thread.start();
   }

   /**
    * Run code in UI thread
    * 
    * @param runnable
    */
   protected void runInUIThread(final Runnable runnable)
   {
      display.asyncExec(runnable);
   }

   /**
    * Get display associated with this job.
    *
    * @return display associated with this job
    */
   protected Display getDisplay()
   {
      return display;
   }
}
