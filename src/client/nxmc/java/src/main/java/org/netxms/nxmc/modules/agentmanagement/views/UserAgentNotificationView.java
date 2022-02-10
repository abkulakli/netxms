package org.netxms.nxmc.modules.agentmanagement.views;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.netxms.client.NXCSession;
import org.netxms.client.SessionListener;
import org.netxms.client.SessionNotification;
import org.netxms.client.UserAgentNotification;
import org.netxms.nxmc.PreferenceStore;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.base.jobs.Job;
import org.netxms.nxmc.base.views.ConfigurationView;
import org.netxms.nxmc.base.widgets.SortableTableViewer;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.agentmanagement.views.helpers.UserAgentNotificationComparator;
import org.netxms.nxmc.modules.agentmanagement.views.helpers.UserAgentNotificationFilter;
import org.netxms.nxmc.modules.agentmanagement.views.helpers.UserAgentNotificationLabelProvider;
import org.netxms.nxmc.resources.ResourceManager;
import org.netxms.nxmc.tools.WidgetHelper;
import org.xnap.commons.i18n.I18n;

public class UserAgentNotificationView extends ConfigurationView implements SessionListener
{
   private static final I18n i18n = LocalizationHelper.getI18n(UserAgentNotificationView.class);
   public static final String ID = "UserAgentNotification";
   
   public static final int COL_ID = 0;
   public static final int COL_OBJECTS = 1;
   public static final int COL_MESSAGE = 2;
   public static final int COL_IS_RECALLED = 3;
   public static final int COL_IS_STARTUP = 4;
   public static final int COL_START_TIME = 5;
   public static final int COL_END_TIME = 6;
   public static final int COL_CREATION_TIME = 7;
   public static final int COL_CREATED_BY = 8;
   
   private SortableTableViewer viewer; 
   private UserAgentNotificationFilter filter;
   private Action actionShowAllOneTime;
   private Action actionShowAllOneScheduled;
   private Action actionRecall;
   private NXCSession session;

   
   /**
    * Constructor
    */
   public UserAgentNotificationView()
   {
      super(i18n.tr("User Agent Notifications"), ResourceManager.getImageDescriptor("icons/config-views/user_agent_messages.png"), ID, true);
   }

   @Override
   protected void createContent(Composite parent)
   {
      parent.setLayout(new FillLayout());
      session = Registry.getSession();
      
      final String[] names = { i18n.tr("ID"), i18n.tr("Objects"), i18n.tr("Message"), i18n.tr("Is recalled"), i18n.tr("Is startup"), i18n.tr("Start time"), i18n.tr("End time"), i18n.tr("Creation time"), i18n.tr("Created by") };
      final int[] widths = { 80, 300, 300, 80, 80, 100, 100, 100, 100 };
      viewer = new SortableTableViewer(parent, names, widths, 0, SWT.UP, SWT.FULL_SELECTION | SWT.MULTI);
      viewer.setContentProvider(new ArrayContentProvider());
      UserAgentNotificationLabelProvider lebleProvider = new UserAgentNotificationLabelProvider(viewer);
      viewer.setLabelProvider(lebleProvider);
      viewer.setComparator(new UserAgentNotificationComparator(lebleProvider));
      filter = new UserAgentNotificationFilter(lebleProvider);
      viewer.addFilter(filter);
      setFilterClient(viewer, filter);
      
      PreferenceStore settings = PreferenceStore.getInstance();
      
      WidgetHelper.restoreTableViewerSettings(viewer, ID);
      filter.setShowAllOneTime(settings.getAsBoolean("UserAgentNotification.showAllOneTime", false));
      filter.setShowAllOneScheduled(settings.getAsBoolean("UserAgentNotification.showAllOneScheduled", false));
      
      viewer.getTable().addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(DisposeEvent e)
         {
            WidgetHelper.saveTableViewerSettings(viewer, ID);
            settings.set("UserAgentNotification.showAllOneTime", filter.isShowAllOneTime());
            settings.set("UserAgentNotification.showAllOneScheduled", filter.isShowAllOneScheduled());
         }
      });
      
      createActions();     
      createPopupMenu(); 
      session.addListener(this);      
      refresh();
   }

   /* (non-Javadoc)
    * @see org.eclipse.ui.part.WorkbenchPart#dispose()
    */
   @Override
   public void dispose()
   {
      session.removeListener(this);
      super.dispose();
   }

   /**
    * Create actions
    */
   private void createActions()
   {      
      actionShowAllOneTime = new Action(i18n.tr("Show all &one time notifications"), IAction.AS_CHECK_BOX) {
         @Override
         public void run()
         {
            filter.setShowAllOneTime(actionShowAllOneTime.isChecked());
            viewer.refresh();
         }
      };
      actionShowAllOneTime.setChecked(filter.isShowAllOneTime());
      
      actionShowAllOneScheduled = new Action(i18n.tr("Show all &scheduler notifications"), IAction.AS_CHECK_BOX) {
         @Override
         public void run()
         {
            filter.setShowAllOneScheduled(actionShowAllOneScheduled.isChecked());
            viewer.refresh();
         }
      };
      actionShowAllOneScheduled.setChecked(filter.isShowAllOneScheduled());
      
      actionRecall = new Action(i18n.tr("Recall notification")) {
         @Override
         public void run()
         {
            recallMessage();
         }
      };
   }
   

   /**
    * Recall message 
    */
   private void recallMessage()
   {
      IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
      if ((selection.size() >= 1))
      {
         for(Object o : selection.toList())
         {            
            final UserAgentNotification msg = (UserAgentNotification)o;
            new Job(i18n.tr("Recall user agent notification"), this) {
               @Override
               protected void run(IProgressMonitor monitor) throws Exception
               {
                  session.recallUserAgentNotification(msg.getId());
               }
               
               @Override
               protected String getErrorMessage()
               {
                  return i18n.tr("Cannot recall user agent notification");
               }
            }.start();
         }
      }
   }

   /**
    * Create pop-up menu
    */
   private void createPopupMenu()
   {
      // Create menu manager.
      MenuManager menuMgr = new MenuManager();
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         public void menuAboutToShow(IMenuManager mgr)
         {
            fillContextMenu(mgr);
         }
      });

      // Create menu.
      Menu menu = menuMgr.createContextMenu(viewer.getControl());
      viewer.getControl().setMenu(menu);
   }
   
   /**
    * Fill context menu
    * @param manager Menu manager
    */
   protected void fillContextMenu(IMenuManager manager)
   {
      IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
      if ((selection.size() >= 1))
      {
         boolean recallIsActive = true;
         for(Object o : selection.toList())
         {
            if (((UserAgentNotification)o).isRecalled() || (((UserAgentNotification)o).getStartTime().getTime() == 0))
            {
               recallIsActive = false;
               break;
            }
         }
         if(recallIsActive)
            manager.add(actionRecall);
      }
      
      manager.add(new Separator());
      manager.add(actionShowAllOneTime);
      manager.add(actionShowAllOneScheduled);
   }
   
   /**
    * Refresh view
    */
   @Override
   public void refresh()
   {
      final NXCSession session = Registry.getSession();
      new Job(i18n.tr("Get list of user agent notifications"), this) {
         @Override
         protected void run(IProgressMonitor monitor) throws Exception
         {
            final List<UserAgentNotification> messages = session.getUserAgentNotifications();
            runInUIThread(new Runnable() {
               @Override
               public void run()
               {
                  viewer.setInput(messages);
               }
            });
         }
         
         @Override
         protected String getErrorMessage()
         {
            return i18n.tr("Cannot get list of user agent notifications");
         }
      }.start();
   }

   /* (non-Javadoc)
    * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
    */
   @Override
   public void setFocus()
   {
      viewer.getTable().setFocus();
   }

	@Override
	public void notificationHandler(SessionNotification n) {
		switch(n.getCode())
		{
			case SessionNotification.USER_AGENT_MESSAGE_CHANGED:
				refresh();
				break;
		}		
	}

   @Override
   public boolean isModified()
   {
      return false;
   }

   @Override
   public void save()
   {
   }
}