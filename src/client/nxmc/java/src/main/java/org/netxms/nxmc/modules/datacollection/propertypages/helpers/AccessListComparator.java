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
package org.netxms.nxmc.modules.datacollection.propertypages.helpers;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Comparator for access list elements
 */
public class AccessListComparator extends ViewerComparator
{
   /**
    * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
    */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2)
	{
      ITableLabelProvider lp = (ITableLabelProvider)((TableViewer)viewer).getLabelProvider();
      TableColumn sortColumn = ((TableViewer)viewer).getTable().getSortColumn();
      int column;
      if (sortColumn != null)
      {
         Integer id = (Integer)sortColumn.getData("ID");
         column = (id != null) ? id : 0;
      }
      else
      {
         column = 0;
      }
		String text1 = lp.getColumnText(e1, column);
		String text2 = lp.getColumnText(e2, column);
		if (text1 == null)
         text1 = "";
		if (text2 == null)
         text2 = "";
		int result = text1.compareToIgnoreCase(text2);
      return (((TableViewer)viewer).getTable().getSortDirection() == SWT.DOWN) ? -result : result;
	}
}
