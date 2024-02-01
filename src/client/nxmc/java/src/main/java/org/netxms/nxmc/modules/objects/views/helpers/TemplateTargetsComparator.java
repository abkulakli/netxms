/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2024 Raden Solutions
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
package org.netxms.nxmc.modules.objects.views.helpers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.netxms.client.objects.AbstractObject;
import org.netxms.nxmc.base.widgets.SortableTableViewer;
import org.netxms.nxmc.modules.objects.views.TemplateTargets;

/**
 * Template targets label comparator
 */
public class TemplateTargetsComparator extends ViewerComparator
{
   private TemplateTargetsLabelProvider labelProvider;
   
   /**
    * Constructor
    * 
    * @param lablelProvider lable provider
    */
   public TemplateTargetsComparator(TemplateTargetsLabelProvider lablelProvider)
   {
      this.labelProvider = lablelProvider;
   }   

   /**
    * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
    */
   @Override
   public int compare(Viewer viewer, Object e1, Object e2)
   {
      AbstractObject c1 = (AbstractObject)e1;
      AbstractObject c2 = (AbstractObject)e2;
      final int column = (Integer)((SortableTableViewer)viewer).getTable().getSortColumn().getData("ID"); //$NON-NLS-1$

      int result;
      switch(column)
      {
         case TemplateTargets.COLUMN_ID:
            result = Long.compare(c1.getObjectId(), c2.getObjectId());
            break;
         case TemplateTargets.COLUMN_NAME:
            result = labelProvider.getName(c1).compareTo(labelProvider.getName(c2));
            break;
         case TemplateTargets.COLUMN_ZONE:
            result = TemplateTargetsLabelProvider.getZone(c1).compareTo(TemplateTargetsLabelProvider.getZone(c2));
            break;
         case TemplateTargets.COLUMN_PRIMARY_HOST_NAME:
            result = TemplateTargetsLabelProvider.getPrimaryHostName(c1).compareTo(TemplateTargetsLabelProvider.getPrimaryHostName(c2));
            break;
         case TemplateTargets.COLUMN_DESCRIPTION:
            result = TemplateTargetsLabelProvider.getDescription(c1).compareTo(TemplateTargetsLabelProvider.getDescription(c2));
            break;
         default:
            result = 0;
            break;
      }
      return (((SortableTableViewer)viewer).getTable().getSortDirection() == SWT.UP) ? result : -result;
   }
}
