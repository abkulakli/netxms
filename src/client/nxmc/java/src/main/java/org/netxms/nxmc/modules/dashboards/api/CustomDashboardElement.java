/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2013 Victor Kirhenshtein
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
package org.netxms.nxmc.modules.dashboards.api;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Base class for custom dashboard widgets
 */
public abstract class CustomDashboardElement extends Composite
{
	/**
	 * @param parent
	 * @param style
	 * @param config
	 */
   public CustomDashboardElement(Composite parent, String config)
	{
      super(parent, SWT.NONE);
	}

   /**
    * @param parent
    * @param style
    * @param config
    */
   public CustomDashboardElement(Composite parent, int style, String config)
   {
      super(parent, style);
   }
}
