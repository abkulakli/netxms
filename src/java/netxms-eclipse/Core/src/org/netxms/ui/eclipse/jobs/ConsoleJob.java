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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.netxms.client.NXCException;
import org.netxms.ui.eclipse.console.Activator;
import org.netxms.ui.eclipse.console.Messages;
import org.netxms.ui.eclipse.tools.MessageDialogHelper;
import org.netxms.ui.eclipse.widgets.MessageBar;

/**
 * Tailored Job class for NetXMS console. Callers must call start() instead of schedule() for correct execution.
 */
public abstract class ConsoleJob extends Job
{
   private IWorkbenchSiteProgressService siteService;
   private String pluginId;
   private MessageBar messageBar;
   private boolean passException = false;

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
      try
      {
         IWorkbenchPart part = (wbPart != null) ? wbPart : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
         siteService = (part != null) ? (IWorkbenchSiteProgressService)wbPart.getSite().getService(IWorkbenchSiteProgressService.class) : null;
      }
      catch(NullPointerException e)
      {
         siteService = null;
      }
      setUser(true);
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
      if (siteService != null)
         siteService.schedule(this, 0, true);
      else
         schedule();
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
    * Called from within Job.run() if job has failed to create failure status. Subclasses may override and return Status.OK_STATUS
    * to avoid standard job failure message to pop up.
    */
   protected IStatus createFailureStatus(Exception e)
   {
      return new Status(Status.ERROR, pluginId, (e instanceof NXCException) ? ((NXCException)e).getErrorCode()
            : 0, getErrorMessage() + ": " + e.getMessage(), passException ? e : null); //$NON-NLS-1$
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
         MessageDialogHelper.openError(null, Messages.get().ConsoleJob_ErrorDialogTitle, getErrorMessage() + ": " + cause.getLocalizedMessage()); //$NON-NLS-1$
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
      PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
   }

   /**
    * Get display associated with this job.
    *
    * @return display associated with this job
    */
   protected Display getDisplay()
   {
      return PlatformUI.getWorkbench().getDisplay();
   }
}
