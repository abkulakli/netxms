/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2018 Raden Solutions
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
package org.netxms.ui.eclipse.alarmviewer.preferencepages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.netxms.client.NXCSession;
import org.netxms.ui.eclipse.alarmviewer.Activator;
import org.netxms.ui.eclipse.alarmviewer.Messages;
import org.netxms.ui.eclipse.alarmviewer.editors.AcknowledgeTimeEditor;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;

/**
 * "Alarms" preference page
 */
public class Alarms extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench)
	{
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors()
	{
		addField(new BooleanFieldEditor("BLINK_OUTSTANDING_ALARMS", Messages.get().Alarms_Blinking, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor("SHOW_TRAY_POPUPS", Messages.get().Alarms_ShowPopup, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor("OUTSTANDING_ALARMS_REMINDER", Messages.get().Alarms_ShowReminder, getFieldEditorParent())); //$NON-NLS-1$
      if (((NXCSession)ConsoleSharedData.getSession()).isTimedAlarmAckEnabled())
      {
         addField(new AcknowledgeTimeEditor("ALARM_TIME_EDITOR", Messages.get().Alarms_AcknowledgeTimeEditor, getFieldEditorParent())); //$NON-NLS-1$
      }
	}
}
