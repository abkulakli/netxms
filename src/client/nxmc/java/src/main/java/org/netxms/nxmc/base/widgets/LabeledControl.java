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
package org.netxms.nxmc.base.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.netxms.nxmc.tools.WidgetHelper;

/**
 * Composite widget - text with label
 */
public abstract class LabeledControl extends Composite
{
	protected Label label;
	protected Control control;
   protected int controlWidthHint;
	
	/**
	 * @param parent
	 * @param style
	 */
	public LabeledControl(Composite parent, int style)
	{
      super(parent, style);
      controlWidthHint = SWT.DEFAULT;
      createContent(getDefaultControlStyle(), null);
	}

	/**
	 * @param parent
	 * @param style
	 * @param controlStyle
	 */
	public LabeledControl(Composite parent, int style, int controlStyle)
	{
      this(parent, style, controlStyle, SWT.DEFAULT, null);
	}
	
   /**
    * @param parent
    * @param style
    * @param controlStyle
    * @param controlWidthHint width hint for inner control
    */
   public LabeledControl(Composite parent, int style, int controlStyle, int controlWidthHint)
   {
      this(parent, style, controlStyle, controlWidthHint, null);
   }

   /**
    * @param parent
    * @param style
    * @param controlStyle
    * @param controlWidthHint width hint for inner control
    * @param parameters optional creation parameters (passed as is to createControl)
    */
   public LabeledControl(Composite parent, int style, int controlStyle, int controlWidthHint, Object parameters)
   {
      super(parent, style);
      this.controlWidthHint = controlWidthHint;
      createContent(controlStyle, parameters);
   }

   /**
    * Create enclosed control
    * 
    * @param controlStyle style for the control
    * @param parameters optional creation parameters (can be null)
    * @return created control
    */
   protected abstract Control createControl(int controlStyle, Object parameters);
   
   /**
    * Get default style for control
    * 
    * @return
    */
   protected int getDefaultControlStyle()
   {
      return SWT.BORDER;
   }

   /**
    * Should return true if extra vertical space is needed by control
    * 
    * @param controlStyle style of control being created
    * @return
    */
   protected boolean isExtraVerticalSpaceNeeded(int controlStyle)
   {
      return false;
   }

	/**
    * Do widget creation.
    * 
    * @param controlStyle style for the control
    * @param parameters optional creation parameters (can be null)
    */
   private void createContent(int controlStyle, Object parameters)
	{
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = WidgetHelper.INNER_SPACING;
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);
		
      label = new Label(this, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		label.setLayoutData(gd);
		
      control = createControl(controlStyle, parameters);
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		if (isExtraVerticalSpaceNeeded(controlStyle))
		{
			gd.verticalAlignment = SWT.FILL;
			gd.grabExcessVerticalSpace = true;
		}
		else
		{
			gd.verticalAlignment = SWT.TOP;
		}
      gd.widthHint = controlWidthHint;
		control.setLayoutData(gd);
	}

	/**
	 * Set label
	 * 
	 * @param newLabel New label
	 */
	public void setLabel(final String newLabel)
	{
		label.setText(newLabel);
	}
	
	/**
	 * Get label
	 * 
	 * @return Current label
	 */
	public String getLabel()
	{
		return label.getText();
	}
	
	/**
	 * Get label control
	 * 
	 * @return label control
	 */
	public Label getLabelControl()
	{
	   return label;
	}

	/**
	 * Set text in control
	 * 
	 * @param newText
	 */
	public abstract void setText(final String newText);

	/**
	 * Get text from control
	 * 
	 * @return Text
	 */
	public abstract String getText();

	/**
	 * Get encapsulated control
	 * 
	 * @return text control
	 */
	public Control getControl()
	{
		return control;
	}

   /**
    * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
    */
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		control.setEnabled(enabled);
		label.setEnabled(enabled);
	}

   /**
    * @see org.eclipse.swt.widgets.Composite#setFocus()
    */
	@Override
	public boolean setFocus()
	{
		return control.setFocus();
	}

   /**
    * @see org.eclipse.swt.widgets.Control#setBackground(org.eclipse.swt.graphics.Color)
    */
   @Override
   public void setBackground(Color color)
   {
      super.setBackground(color);
      label.setBackground(color);
   }

   /**
    * @see org.eclipse.swt.widgets.Widget#toString()
    */
   @Override
   public String toString()
   {
      return getClass().getName() + " [label=" + label.getText() + ", control=" + control + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }
}
