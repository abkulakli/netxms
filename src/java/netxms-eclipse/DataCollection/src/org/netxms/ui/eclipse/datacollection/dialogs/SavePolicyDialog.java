/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2022 Victor Kirhenshtein
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
package org.netxms.ui.eclipse.datacollection.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.netxms.ui.eclipse.datacollection.Activator;
import org.netxms.ui.eclipse.tools.WidgetHelper;

/**
 * Shows prompt for saving agent's policy
 */
public class SavePolicyDialog extends Dialog
{
   public static final int SAVE_ID = 100;
   public static final int DISCARD_ID = 102;

   /**
    * @param parentShell
    */
   public SavePolicyDialog(Shell parentShell)
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
      newShell.setText("Unsaved Changes");
   }

   /**
    * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
    */
   @Override
   protected void createButtonsForButtonBar(Composite parent)
   {
      createButton(parent, SAVE_ID, "Save", false);
      createButton(parent, DISCARD_ID, "Discard", false);
      createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
   }

   /**
    * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
    */
   @Override
   protected Control createDialogArea(Composite parent)
   {
      Composite dialogArea = (Composite)super.createDialogArea(parent);

      GridLayout layout = new GridLayout();
      layout.horizontalSpacing = WidgetHelper.DIALOG_SPACING;
      layout.marginWidth = WidgetHelper.DIALOG_WIDTH_MARGIN;
      layout.marginHeight = WidgetHelper.DIALOG_HEIGHT_MARGIN;
      layout.numColumns = 2;
      dialogArea.setLayout(layout);

      final Label imageLabel = new Label(dialogArea, SWT.NONE);
      final Image image = Activator.getImageDescriptor("icons/unsaved-document.png").createImage(); //$NON-NLS-1$
      imageLabel.setImage(image);
      imageLabel.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(DisposeEvent e)
         {
            image.dispose();
         }
      });

      final CLabel text = new CLabel(dialogArea, SWT.LEFT);
      text.setText("Policy is not saved.\nDo you want to save it, discard changes, or return to the editor?");
      GridData gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      text.setLayoutData(gd);

      return dialogArea;
   }

   /**
    * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
    */
   @Override
   protected void buttonPressed(int buttonId)
   {
      if (buttonId >= 100)
      {
         setReturnCode(buttonId);
         close();
      }
      super.buttonPressed(buttonId);
   }

}
