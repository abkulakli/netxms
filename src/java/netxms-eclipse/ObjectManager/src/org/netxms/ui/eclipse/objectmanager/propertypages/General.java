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
package org.netxms.ui.eclipse.objectmanager.propertypages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.netxms.client.NXCObjectModificationData;
import org.netxms.client.NXCSession;
import org.netxms.client.objects.AbstractObject;
import org.netxms.ui.eclipse.jobs.ConsoleJob;
import org.netxms.ui.eclipse.objectmanager.Activator;
import org.netxms.ui.eclipse.objectmanager.Messages;
import org.netxms.ui.eclipse.objectmanager.widgets.ObjectCategorySelector;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;
import org.netxms.ui.eclipse.tools.WidgetHelper;
import org.netxms.ui.eclipse.widgets.AbstractSelector;

/**
 * "General" property page for NetMS objects 
 */
public class General extends PropertyPage
{
   private Text name;
   private Text alias;
   private ObjectCategorySelector categorySelector;
	private String initialName;
   private String initialAlias;
   private int initialCategory;
	private AbstractObject object;

   /**
    * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
    */
	@Override
	protected Control createContents(Composite parent)
	{
		Composite dialogArea = new Composite(parent, SWT.NONE);

		object = (AbstractObject)getElement().getAdapter(AbstractObject.class);

		GridLayout layout = new GridLayout();
		layout.verticalSpacing = WidgetHelper.OUTER_SPACING;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
      dialogArea.setLayout(layout);
      
      // Object ID
      WidgetHelper.createLabeledText(dialogArea, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, SWT.DEFAULT,
            Messages.get().General_ObjectID, Long.toString(object.getObjectId()), WidgetHelper.DEFAULT_LAYOUT_DATA);
      
		// Object class
      WidgetHelper.createLabeledText(dialogArea, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, SWT.DEFAULT,
            Messages.get().General_ObjectClass, object.getObjectClassName(), WidgetHelper.DEFAULT_LAYOUT_DATA);
		
		// Object name
      initialName = object.getObjectName();
      name = WidgetHelper.createLabeledText(dialogArea, SWT.SINGLE | SWT.BORDER, SWT.DEFAULT, Messages.get().General_ObjectName,
            initialName, WidgetHelper.DEFAULT_LAYOUT_DATA);
      
      // Object alias
      initialAlias = object.getAlias();
      alias = WidgetHelper.createLabeledText(dialogArea, SWT.SINGLE | SWT.BORDER, SWT.DEFAULT, Messages.get().General_Alias, initialAlias, WidgetHelper.DEFAULT_LAYOUT_DATA);

      // Category selector
      initialCategory = object.getCategoryId();
      categorySelector = new ObjectCategorySelector(dialogArea, SWT.NONE, AbstractSelector.SHOW_CLEAR_BUTTON);
      categorySelector.setLabel(Messages.get().General_Category);
      categorySelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      categorySelector.setCategoryId(initialCategory);

		return dialogArea;
	}

   /**
    * @see org.eclipse.jface.preference.PreferencePage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent)
   {
      super.createControl(parent);
      getDefaultsButton().setVisible(false);
   }

   /**
	 * Apply changes
	 * 
	 * @param isApply true if update operation caused by "Apply" button
	 */
	protected void applyChanges(final boolean isApply)
	{
      final String newName = name.getText();
      final String newAlias = alias.getText();
      final int newCategory = categorySelector.getCategoryId();
      if (newName.equals(initialName) && newAlias.equals(initialAlias) && (newCategory == initialCategory))
         return; // nothing to change

		if (isApply)
			setValid(false);

      final NXCSession session = ConsoleSharedData.getSession();
		final NXCObjectModificationData data = new NXCObjectModificationData(object.getObjectId());
		data.setName(newName);
      data.setAlias(newAlias);
      data.setCategoryId(newCategory);
		new ConsoleJob(Messages.get().General_JobName, null, Activator.PLUGIN_ID, null) {
			@Override
			protected void runInternal(IProgressMonitor monitor) throws Exception
			{
				session.modifyObject(data);
			}

			@Override
			protected String getErrorMessage()
			{
				return Messages.get().General_JobError;
			}

			@Override
			protected void jobFinalize()
			{
				if (isApply)
				{
					runInUIThread(new Runnable() {
						@Override
						public void run()
						{
							initialName = newName;
                     initialAlias = newAlias;
                     initialCategory = newCategory;
							General.this.setValid(true);
						}
					});
				}
			}
		}.start();
	}

   /**
    * @see org.eclipse.jface.preference.PreferencePage#performOk()
    */
	@Override
	public boolean performOk()
	{
		applyChanges(false);
		return true;
	}

   /**
    * @see org.eclipse.jface.preference.PreferencePage#performApply()
    */
	@Override
	protected void performApply()
	{
		applyChanges(true);
	}
}
