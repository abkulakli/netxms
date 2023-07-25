/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2021 Raden Solutions
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
package org.netxms.nxmc.modules.objects.preferencepages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.netxms.nxmc.PreferenceStore;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.xnap.commons.i18n.I18n;

/**
 * Preferences page for object browser
 */
public class ObjectBrowserPreferences extends FieldEditorPreferencePage
{
   private static final I18n i18n = LocalizationHelper.getI18n(ObjectBrowserPreferences.class);

   private BooleanFieldEditor useServerFilter;
   private BooleanFieldEditor makeFullSync;
   private Composite autoApplyParent;
   private Composite delayParent;
   private Composite minLengthParent;
   private BooleanFieldEditor autoApply;
   private StringFieldEditor delay;
   private StringFieldEditor minLength;

   public ObjectBrowserPreferences()
   {
      super(i18n.tr("Object Browser"), FieldEditorPreferencePage.FLAT);
      setPreferenceStore(PreferenceStore.getInstance());
   }

   /**
    * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
    */
   @Override
   protected void createFieldEditors()
   {
      makeFullSync = new BooleanFieldEditor("ObjectsFullSync", "Full object synchronization on startup", getFieldEditorParent()); //$NON-NLS-1$
      addField(makeFullSync); 

      addField(new BooleanFieldEditor("ObjectBrowser.showStatusIndicator", "Show &status indicator", getFieldEditorParent())); //$NON-NLS-1$
      addField(new BooleanFieldEditor("ObjectBrowser.showFilter", "Show &filter", getFieldEditorParent())); //$NON-NLS-1$

      useServerFilter = new BooleanFieldEditor("ObjectBrowser.useServerFilterSettings", "&Use server settings for object filter", getFieldEditorParent()); //$NON-NLS-1$
      addField(useServerFilter);

      autoApplyParent = getFieldEditorParent();
      autoApply = new BooleanFieldEditor("ObjectBrowser.filterAutoApply", "&Apply filter automatically", autoApplyParent); //$NON-NLS-1$
      addField(autoApply);

      delayParent = getFieldEditorParent();
      delay = new StringFieldEditor("ObjectBrowser.filterDelay", "Filter &delay", delayParent); //$NON-NLS-1$
      addField(delay);
      delay.setEmptyStringAllowed(false);
      delay.setTextLimit(5);

      minLengthParent = getFieldEditorParent();
      minLength = new StringFieldEditor("ObjectBrowser.filterMinLength", "Filter &minimal length", minLengthParent); //$NON-NLS-1$
      addField(minLength);
      minLength.setEmptyStringAllowed(false);
      minLength.setTextLimit(3);
   }

   /**
    * @see org.eclipse.jface.preference.FieldEditorPreferencePage#initialize()
    */
   @Override
   protected void initialize()
   {
      super.initialize();
      boolean enabled = !useServerFilter.getBooleanValue();
      autoApply.setEnabled(enabled, autoApplyParent);
      delay.setEnabled(enabled, delayParent);
      minLength.setEnabled(enabled, minLengthParent);
   }

   /**
    * @see org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
    */
   @Override
   public void propertyChange(PropertyChangeEvent event)
   {
      super.propertyChange(event);
      boolean enabled = !useServerFilter.getBooleanValue();
      autoApply.setEnabled(enabled, autoApplyParent);
      delay.setEnabled(enabled, delayParent);
      minLength.setEnabled(enabled, minLengthParent);

      boolean fullySync = makeFullSync.getBooleanValue();
      IPreferenceStore globalStore = PreferenceStore.getInstance();
      globalStore.setValue("ObjectsFullSync", fullySync);
   }
}