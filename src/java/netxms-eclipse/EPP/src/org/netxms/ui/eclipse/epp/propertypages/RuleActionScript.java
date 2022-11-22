/**
 * NetXMS - open source network management system
 * Copyright (C) 2022 Raden Solutions
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
package org.netxms.ui.eclipse.epp.propertypages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.netxms.client.events.EventProcessingPolicyRule;
import org.netxms.ui.eclipse.epp.widgets.RuleEditor;
import org.netxms.ui.eclipse.nxsl.widgets.ScriptEditor;

/**
 * "Action Script" property page
 */
public class RuleActionScript extends PropertyPage
{
	private RuleEditor editor;
	private EventProcessingPolicyRule rule;
	private ScriptEditor scriptEditor;

   /**
    * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
    */
	@Override
	protected Control createContents(Composite parent)
	{
		editor = (RuleEditor)getElement().getAdapter(RuleEditor.class);
		rule = editor.getRule();
		
		Composite dialogArea = new Composite(parent, SWT.NONE);
		dialogArea.setLayout(new FillLayout());

      scriptEditor = new ScriptEditor(dialogArea, SWT.BORDER, SWT.H_SCROLL | SWT.V_SCROLL, false, 
            "Variables:\r\n\t$object\tevent source object\r\n\t$node\tevent source object if it's class is Node\r\n\t$event\tevent being processed\r\n\t$dci\tDCI associated with this event");
		scriptEditor.setText(rule.getActionScript());
		
		return dialogArea;
	}
	
	/**
	 * Apply changes
	 * 
	 * @param isApply true if update operation caused by "Apply" button
	 */
	protected void applyChanges(final boolean isApply)
	{
		rule.setActionScript(scriptEditor.getText());
		editor.setModified(true);
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply()
	{
		applyChanges(true);
	}
}
