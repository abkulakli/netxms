/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2020 Victor Kirhenshtein
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.netxms.client.NXCObjectModificationData;
import org.netxms.client.NXCSession;
import org.netxms.client.objects.AbstractNode;
import org.netxms.ui.eclipse.jobs.ConsoleJob;
import org.netxms.ui.eclipse.objectmanager.Activator;
import org.netxms.ui.eclipse.objectmanager.Messages;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;
import org.netxms.ui.eclipse.tools.MessageDialogHelper;
import org.netxms.ui.eclipse.widgets.LabeledText;

/**
 * "Communication" property page for nodes
 */
public class Communication extends PropertyPage
{
	private AbstractNode node;
	private LabeledText primaryName;
   private Button externalGateway;
   private Button enablePingOnPrimaryIP;
	private boolean primaryNameChanged = false;

   /**
    * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
    */
	@Override
	protected Control createContents(Composite parent)
	{
		node = (AbstractNode)getElement().getAdapter(AbstractNode.class);

		Composite dialogArea = new Composite(parent, SWT.NONE);
		GridLayout dialogLayout = new GridLayout();
		dialogLayout.marginWidth = 0;
		dialogLayout.marginHeight = 0;
		dialogArea.setLayout(dialogLayout);
		
		primaryName = new LabeledText(dialogArea, SWT.NONE);
		primaryName.setLabel(Messages.get().Communication_PrimaryHostName);
		primaryName.setText(node.getPrimaryName());
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		primaryName.setLayoutData(gd);
		primaryName.getTextControl().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e)
			{
				primaryNameChanged = true;
			}
		});

      externalGateway = new Button(dialogArea, SWT.CHECK);
      externalGateway.setText(Messages.get().Communication_RemoteAgent);
      externalGateway.setSelection((node.getFlags() & AbstractNode.NF_EXTERNAL_GATEWAY) != 0);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      externalGateway.setLayoutData(gd);
      		
      enablePingOnPrimaryIP = new Button(dialogArea, SWT.CHECK);
      enablePingOnPrimaryIP.setText(Messages.get().Communication_PingPrimaryIP);
      enablePingOnPrimaryIP.setSelection(node.isPingOnPrimaryIPEnabled());
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      enablePingOnPrimaryIP.setLayoutData(gd);
            
		return dialogArea;
	}

	/**
	 * Apply changes
	 * 
	 * @param isApply true if update operation caused by "Apply" button
	 */
	protected boolean applyChanges(final boolean isApply)
	{
		final NXCObjectModificationData md = new NXCObjectModificationData(node.getObjectId());
		
		if (primaryNameChanged)
		{
			// Validate primary name
			final String hostName = primaryName.getText().trim();
			if (!hostName.matches("^(([A-Za-z0-9_-]+\\.)*[A-Za-z0-9_-]+|[A-Fa-f0-9:]+)$")) //$NON-NLS-1$
			{
				MessageDialogHelper.openWarning(getShell(), Messages.get().Communication_Warning, 
				      String.format(Messages.get().Communication_WarningInvalidHostname, hostName));
				return false;
			}
			md.setPrimaryName(hostName);
		}
			
		if (isApply)
			setValid(false);
		
		int flags = node.getFlags();
      if (externalGateway.getSelection())
         flags |= AbstractNode.NF_EXTERNAL_GATEWAY;
      else
         flags &= ~AbstractNode.NF_EXTERNAL_GATEWAY;
      if (enablePingOnPrimaryIP.getSelection())
         flags |= AbstractNode.NF_PING_PRIMARY_IP;
      else
         flags &= ~AbstractNode.NF_PING_PRIMARY_IP;
      md.setObjectFlags(flags, AbstractNode.NF_EXTERNAL_GATEWAY | AbstractNode.NF_PING_PRIMARY_IP);

		final NXCSession session = ConsoleSharedData.getSession();
		new ConsoleJob(String.format(Messages.get().Communication_JobName, node.getObjectName()), null, Activator.PLUGIN_ID, null) {
			@Override
			protected void runInternal(IProgressMonitor monitor) throws Exception
			{
				session.modifyObject(md);
			}

			@Override
			protected String getErrorMessage()
			{
				return Messages.get().Communication_JobError;
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
							Communication.this.setValid(true);
						}
					});
				}
			}
		}.start();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk()
	{
		return applyChanges(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply()
	{
		applyChanges(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults()
	{
		super.performDefaults();
		externalGateway.setSelection(false);
	}
}
