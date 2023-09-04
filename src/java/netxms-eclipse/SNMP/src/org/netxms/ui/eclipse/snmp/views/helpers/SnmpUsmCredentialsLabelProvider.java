/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2022 Raden Solutions
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
package org.netxms.ui.eclipse.snmp.views.helpers;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.netxms.client.snmp.SnmpUsmCredential;
import org.netxms.ui.eclipse.snmp.Messages;
import org.netxms.ui.eclipse.snmp.views.NetworkCredentialsEditor;
import org.netxms.ui.eclipse.tools.WidgetHelper;

/**
 * Label provider for SnmpUsmCredentials class
 */
public class SnmpUsmCredentialsLabelProvider extends LabelProvider implements ITableLabelProvider
{
	private static final String[] authMethodName = { Messages.get().SnmpUsmLabelProvider_AuthNone, Messages.get().SnmpUsmLabelProvider_AuthMD5, Messages.get().SnmpUsmLabelProvider_AuthSHA1, "SHA224", "SHA256", "SHA384", "SHA512" };
	private static final String[] privMethodName = { Messages.get().SnmpUsmLabelProvider_EncNone, Messages.get().SnmpUsmLabelProvider_EncDES, Messages.get().SnmpUsmLabelProvider_EncAES };

   private boolean maskMode = true;

   /**
    * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
    */
	@Override
	public Image getColumnImage(Object element, int columnIndex)
	{
		return null;
	}

   /**
    * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
    */
	@Override
	public String getColumnText(Object element, int columnIndex)
	{
		SnmpUsmCredential c = (SnmpUsmCredential)element;		
		switch(columnIndex)
      {
         case NetworkCredentialsEditor.COLUMN_SNMP_USERNAME:
		      return c.getName();
         case NetworkCredentialsEditor.COLUMN_SNMP_AUTHENTICATION:
		      return authMethodName[c.getAuthMethod()];
         case NetworkCredentialsEditor.COLUMN_SNMP_ENCRYPTION:
		      return privMethodName[c.getPrivMethod()];
         case NetworkCredentialsEditor.COLUMN_SNMP_AUTH_PASSWORD:
            return maskMode ? WidgetHelper.maskPassword(c.getAuthPassword()) : c.getAuthPassword();
         case NetworkCredentialsEditor.COLUMN_SNMP_ENCRYPTION_PASSWORD:
            return maskMode ? WidgetHelper.maskPassword(c.getPrivPassword()) : c.getPrivPassword();
         case NetworkCredentialsEditor.COLUMN_SNMP_COMMENTS:
		      return c.getComment();
      }
		return null;
	}

   /**
    * @return the maskMode
    */
   public boolean isMaskMode()
   {
      return maskMode;
   }

   /**
    * @param maskMode the maskMode to set
    */
   public void setMaskMode(boolean maskMode)
   {
      this.maskMode = maskMode;
   }
}
