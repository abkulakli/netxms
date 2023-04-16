/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Raden Solutions
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
package org.netxms.nxmc.modules.businessservice.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.netxms.nxmc.base.widgets.LabeledText;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.businessservice.propertypages.InstanceDiscovery;
import org.netxms.nxmc.tools.MessageDialogHelper;
import org.netxms.nxmc.tools.WidgetHelper;
import org.xnap.commons.i18n.I18n;

/**
 * Dialog for creating business service prototype
 */
public class CreateBusinessServicePrototype extends Dialog
{
   private I18n i18n = LocalizationHelper.getI18n(CreateBusinessServicePrototype.class);
   
   private LabeledText nameField;
   private LabeledText aliasField;
   private Combo instanceDiscoveyMethodCombo;
   
   private String name;
   private String alias;
   private String instanceDiscoveyMethod;

   /**
    * Create dialog.
    *
    * @param parentShell parent shell
    */
   public CreateBusinessServicePrototype(Shell parentShell)
   {
      super(parentShell);
   }

   /**
    * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
    */
   @Override
   protected void configureShell(Shell newShell)
   {
      super.configureShell(newShell);
      newShell.setText(i18n.tr("Create Business Service Prototype"));
   }

   /**
    * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
    */
   @Override
   protected Control createDialogArea(Composite parent)
   {
      Composite dialogArea = (Composite)super.createDialogArea(parent);

      GridLayout layout = new GridLayout();
      layout.verticalSpacing = WidgetHelper.DIALOG_SPACING;
      layout.marginHeight = WidgetHelper.DIALOG_HEIGHT_MARGIN;
      layout.marginWidth = WidgetHelper.DIALOG_WIDTH_MARGIN;
      dialogArea.setLayout(layout);

      nameField = new LabeledText(dialogArea, SWT.NONE);
      nameField.setLabel(i18n.tr("Name"));
      nameField.getTextControl().setTextLimit(63);
      GridData gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      gd.widthHint = 300;
      nameField.setLayoutData(gd);

      aliasField = new LabeledText(dialogArea, SWT.NONE);
      aliasField.setLabel(i18n.tr("Alias"));
      aliasField.getTextControl().setTextLimit(63);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      aliasField.setLayoutData(gd);

      instanceDiscoveyMethodCombo = WidgetHelper.createLabeledCombo(dialogArea, SWT.READ_ONLY, i18n.tr("Instance discovery type"), new GridData(SWT.FILL, SWT.CENTER, true, false));

      for (String type : InstanceDiscovery.DISCOVERY_TYPES)
      {
         if (type != null)
            instanceDiscoveyMethodCombo.add(type);            
      }
      instanceDiscoveyMethodCombo.select(0);
      
      return dialogArea;
   }

   /**
    * @see org.eclipse.jface.dialogs.Dialog#okPressed()
    */
   @Override
   protected void okPressed()
   {
      instanceDiscoveyMethod = instanceDiscoveyMethodCombo.getText();
      alias = aliasField.getText().trim();
      name = nameField.getText().trim();
      if (name.isEmpty())
      {
         MessageDialogHelper.openWarning(getShell(), i18n.tr("Warning"), i18n.tr("Object name cannot be empty"));
         return;
      }
      super.okPressed();
   }

   /**
    * Get name for new object
    *
    * @return name for new object
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get alias for new object
    *
    * @return alias for new object
    */
   public String getAlias()
   {
      return alias;
   }

   /**
    * Get instance discovery method.
    *
    * @return instance discovery method
    */
   public int getInstanceDiscoveyMethod()
   {
      return InstanceDiscovery.DISCOVERY_TYPES.indexOf(instanceDiscoveyMethod);
   }
}
