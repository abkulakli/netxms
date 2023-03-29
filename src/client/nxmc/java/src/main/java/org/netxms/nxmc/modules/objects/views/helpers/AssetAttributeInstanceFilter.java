/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Reden Solutions
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

import java.util.Map.Entry;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.netxms.nxmc.base.views.AbstractViewerFilter;

/**
 * Asset instance filter
 */
public class AssetAttributeInstanceFilter extends ViewerFilter implements AbstractViewerFilter
{
   private String filterString = null;
   private AssetAttributeInstanceLabelProvider labelProvider;

   /**
    * Asset instance comparator
    * 
    * @param labelProvider asset instance comparator
    */
   public AssetAttributeInstanceFilter(AssetAttributeInstanceLabelProvider labelProvider)
   {
      this.labelProvider = labelProvider;
   }

   /**
    * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
    */
   @Override
   public boolean select(Viewer viewer, Object parentElement, Object element)
   {
      if ((filterString == null) || (filterString.isEmpty()))
         return true;

      @SuppressWarnings("unchecked")
      Entry<String, String> entry = (Entry<String, String>)element;

      return entry.getKey().toUpperCase().contains(filterString) || labelProvider.getValue(entry).toUpperCase().contains(filterString);
   }

   /**
    * @see org.netxms.nxmc.base.views.AbstractViewerFilter#setFilterString(java.lang.String)
    */
   @Override
   public void setFilterString(String string)
   {
      this.filterString = (string != null) ? string.toUpperCase() : null;
   }
}
