package org.netxms.nxmc.modules.objects.actions;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.netxms.client.NXCSession;
import org.netxms.client.objects.NetworkMap;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.base.jobs.Job;
import org.netxms.nxmc.base.views.ViewPlacement;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.objects.dialogs.CreateObjectDialog;
import org.xnap.commons.i18n.I18n;

/**
 * Clone network map
 */
public class CloneNetworkMap extends ObjectAction<NetworkMap>
{
   private static final I18n i18n = LocalizationHelper.getI18n(CloneNetworkMap.class);

   /**
    * Create action to clone network map
    *
    * @param viewPlacement view placement information
    * @param selectionProvider associated selection provider
    */
   public CloneNetworkMap(ViewPlacement viewPlacement, ISelectionProvider selectionProvider)
   {
      super(NetworkMap.class, i18n.tr("Clone..."), viewPlacement, selectionProvider);
   }

   /**
    * @see org.netxms.nxmc.modules.objects.actions.ObjectAction#run(java.util.List)
    */
   @Override
   protected void run(List<NetworkMap> objects)
   {      
      NetworkMap object = objects.get(0);
      
      final CreateObjectDialog dlg = new CreateObjectDialog(getShell(), i18n.tr("Clone network map"));
      if (dlg.open() != Window.OK)
         return;
      
      final NXCSession session = Registry.getSession();
      new Job(i18n.tr("Create new network map"), null, getMessageArea()) {
         @Override
         protected void run(IProgressMonitor monitor) throws Exception
         {
            session.cloneNetworkMap(object.getObjectId(), dlg.getObjectName(), dlg.getObjectAlias());
         }

         @Override
         protected String getErrorMessage()
         {
            return String.format(i18n.tr("Cannot create network map object \"%s\""), dlg.getObjectName());
         }
      }.start();
   }

   /**
    * @see org.netxms.nxmc.modules.objects.actions.ObjectAction#isValidForSelection(org.eclipse.jface.viewers.IStructuredSelection)
    */
   @Override
   public boolean isValidForSelection(IStructuredSelection selection)
   {
      return (selection.size() == 1) && (selection.getFirstElement() instanceof NetworkMap);
   }
}
