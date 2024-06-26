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
package org.netxms.ui.eclipse.alarmviewer.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.netxms.client.NXCSession;
import org.netxms.client.events.AlarmComment;
import org.netxms.client.users.AbstractUserObject;
import org.netxms.ui.eclipse.alarmviewer.Activator;
import org.netxms.ui.eclipse.alarmviewer.Messages;
import org.netxms.ui.eclipse.console.resources.RegionalSettings;
import org.netxms.ui.eclipse.console.resources.SharedIcons;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;
import org.netxms.ui.eclipse.tools.ImageCache;

/**
 * Editor for alarm comments
 */
public class AlarmCommentsEditor extends Composite
{
   private NXCSession session = ConsoleSharedData.getSession();
	private Text text;
	
	/**
	 * @param parent
	 * @param style
	 * @param comment
	 */
   public AlarmCommentsEditor(Composite parent, ImageCache imageCache, AlarmComment comment, HyperlinkAdapter editAction, HyperlinkAdapter deleteAction)
	{
		super(parent, SWT.BORDER);
		
      setBackground(parent.getBackground());

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		setLayout(layout);

		final AbstractUserObject userObject = session.findUserDBObjectById(comment.getUserId(), null);
		
		final CLabel user = new CLabel(this, SWT.NONE);
		user.setImage(imageCache.add(Activator.getImageDescriptor("icons/user.png"))); //$NON-NLS-1$
		user.setText((userObject != null) ? userObject.getName() : Messages.get().AlarmCommentsEditor_Unknown);
      user.setBackground(parent.getBackground());
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = false;
		user.setLayoutData(gd);

      final Label time = new Label(this, SWT.NONE);
      time.setText(RegionalSettings.getDateTimeFormat().format(comment.getLastChangeTime()));
      time.setBackground(parent.getBackground());
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		time.setLayoutData(gd);

      final Composite controlArea = new Composite(this, SWT.NONE);
      controlArea.setBackground(parent.getBackground());
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		controlArea.setLayout(layout);
      gd = new GridData();
      gd.horizontalAlignment = SWT.RIGHT;
      gd.verticalAlignment = SWT.TOP;
      gd.verticalSpan = 2;
      controlArea.setLayoutData(gd);

      final ImageHyperlink linkEdit = new ImageHyperlink(controlArea, SWT.NONE);
		linkEdit.setText(Messages.get().AlarmCommentsEditor_Edit);
		linkEdit.setImage(SharedIcons.IMG_EDIT);
      linkEdit.setBackground(parent.getBackground());
		linkEdit.addHyperlinkListener(editAction);
		gd = new GridData();
		gd.horizontalAlignment = SWT.LEFT;
		linkEdit.setLayoutData(gd);

      final ImageHyperlink linkDelete = new ImageHyperlink(controlArea, SWT.NONE);
		linkDelete.setText(Messages.get().AlarmCommentsEditor_DeleteLabel);
		linkDelete.setImage(SharedIcons.IMG_DELETE_OBJECT);
      linkDelete.setBackground(parent.getBackground());
		linkDelete.addHyperlinkListener(deleteAction);
      gd = new GridData();
      gd.horizontalAlignment = SWT.LEFT;
      linkDelete.setLayoutData(gd);

		text = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
		text.setText(comment.getText());
      text.setBackground(parent.getBackground());
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		text.setLayoutData(gd);
	}
}
