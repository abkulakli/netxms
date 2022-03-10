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
package org.netxms.nxmc.modules.nxsl.widgets;

import java.util.Collection;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.netxms.client.NXCSession;
import org.netxms.client.ScriptCompilationResult;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.base.jobs.Job;
import org.netxms.nxmc.base.widgets.CompositeWithMessageArea;
import org.netxms.nxmc.base.widgets.MessageArea;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.resources.ResourceManager;
import org.netxms.nxmc.resources.SharedIcons;
import org.netxms.nxmc.resources.ThemeEngine;
import org.xnap.commons.i18n.I18n;

/**
 * NXSL script editor
 */
public class ScriptEditor extends CompositeWithMessageArea
{
   private static final I18n i18n = LocalizationHelper.getI18n(ScriptEditor.class);

   private Composite content;
   private Text editor;
	private String hintText;
	private Composite hintArea;
	private Text hintTextControl = null;
	private Label hintsExpandButton = null;
	private Button compileButton;

   /**
    * @param parent
    * @param style
    * @param editorStyle
    */
   public ScriptEditor(Composite parent, int style, int editorStyle)
   {
      this(parent, style, editorStyle, true, null, true);
   }

   /**
    * @param parent
    * @param style
    * @param editorStyle
    * @param showLineNumbers
    */
   public ScriptEditor(Composite parent, int style, int editorStyle, boolean showLineNumbers)
   {
      this(parent, style, editorStyle, showLineNumbers, null, true);
   }

   /**
    * 
    * @param parent
    * @param style
    * @param editorStyle
    * @param showLineNumbers
    * @param hints
    * @param showCompileButton
    */
   public ScriptEditor(Composite parent, int style, int editorStyle, boolean showLineNumbers, String hints)
   {
      this(parent, style, editorStyle, showLineNumbers, hints, true);
   }

   /**
    * 
    * @param parent
    * @param style
    * @param editorStyle
    * @param showLineNumbers
    * @param showCompileButton
    */
   public ScriptEditor(Composite parent, int style, int editorStyle, boolean showLineNumbers, boolean showCompileButton)
   {
      this(parent, style, editorStyle, showLineNumbers, null, showCompileButton);
   }

	/**
	 * @param parent
	 * @param style
	 * @param editorStyle
	 * @param showLineNumbers
	 * @param hints
	 * @param showCompileButton
	 */
	public ScriptEditor(Composite parent, int style, int editorStyle, boolean showLineNumbers, String hints, boolean showCompileButton)
	{
		super(parent, style);

      hintText = hints;

      content = new Composite(this, SWT.NONE);
      setContent(content);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
      content.setLayout(layout);

      if (hints != null)
      {
         createHintsArea();
      }
      
      editor = new Text(content, editorStyle | SWT.MULTI);
      editor.setData(RWT.CUSTOM_VARIANT, "monospace");
      editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      if (showCompileButton)
      {
         final Image compileImage = ResourceManager.getImage("icons/compile.gif");
         compileButton = new Button(content, SWT.PUSH | SWT.FLAT);
         compileButton.setToolTipText("Compile script");
         compileButton.setImage(compileImage);
         compileButton.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
               compileImage.dispose();
            }
         });

         GridData gd = new GridData();
         gd.exclude = true;
         compileButton.setLayoutData(gd);

         editor.addControlListener(new ControlListener() {
            @Override
            public void controlResized(ControlEvent e)
            {
               positionCompileButton();
            }

            @Override
            public void controlMoved(ControlEvent e)
            {
               positionCompileButton();
            }
         });

         compileButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
               widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e)
            {
               compileScript();
            }
         });

         compileButton.setSize(compileButton.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         positionCompileButton();
      }
	}

   /**
    * Position "Compile" button
    */
   private void positionCompileButton()
   {
      compileButton.moveAbove(null);
      Point location = editor.getLocation();
      int editorWidth = editor.getSize().x;
      ScrollBar sb = editor.getVerticalBar();
      if (sb != null)
         editorWidth -= sb.getSize().x;
      compileButton.setLocation(location.x + editorWidth - compileButton.getSize().x - 3, location.y + 3);
   }

	/**
	 * Create hints area
	 */
	private void createHintsArea()
	{
      hintArea = new Composite(content, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.verticalSpacing = 0;
      layout.numColumns = 2;
      hintArea.setLayout(layout);
      hintArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      hintArea.setBackground(ThemeEngine.getBackgroundColor("MessageBar"));

      CLabel hintsTitle = new CLabel(hintArea, SWT.NONE);
      hintsTitle.setBackground(hintArea.getBackground());
      hintsTitle.setForeground(ThemeEngine.getForegroundColor("MessageBar"));
      hintsTitle.setImage(SharedIcons.IMG_INFORMATION);
      hintsTitle.setText(i18n.tr("Hints"));
      hintsTitle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      hintsTitle.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseDoubleClick(MouseEvent e)
         {
            if (e.button == 1)
               toggleHints();
         }
      });
      
      hintsExpandButton = new Label(hintArea, SWT.NONE);
      hintsExpandButton.setBackground(hintArea.getBackground());
      hintsExpandButton.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
      hintsExpandButton.setImage(SharedIcons.IMG_EXPAND);
      hintsExpandButton.setToolTipText(i18n.tr("Hide message"));
      GridData gd = new GridData();
      gd.verticalAlignment = SWT.CENTER;
      hintsExpandButton.setLayoutData(gd);
      hintsExpandButton.addMouseListener(new MouseListener() {
         private boolean doAction = false;

         @Override
         public void mouseDoubleClick(MouseEvent e)
         {
            if (e.button == 1)
               doAction = false;
         }

         @Override
         public void mouseDown(MouseEvent e)
         {
            if (e.button == 1)
               doAction = true;
         }

         @Override
         public void mouseUp(MouseEvent e)
         {
            if ((e.button == 1) && doAction)
               toggleHints();
         }
      });
      
      Label separator = new Label(content, SWT.SEPARATOR | SWT.HORIZONTAL);
      separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
   }

	/**
	 * Get underlying text widget
	 * @return text widget
	 */
   public Text getTextWidget()
	{
      return editor;
	}
	
	/**
	 * Set text for editing
	 * @param text
	 */
	public void setText(String text)
	{
      editor.setText(text);
	}

	/**
	 * Get editor's content
	 * @return
	 */
	public String getText()
	{
      return editor.getText();
	}

	/**
	 * @param functions the functions to set
	 */
	public void setFunctions(Set<String> functions)
	{
	}
	
	/**
	 * Add functions
	 * 
	 * @param fc
	 */
	public void addFunctions(Collection<String> fc)
	{
	}

	/**
	 * @param variables the variables to set
	 */
	public void setVariables(Set<String> variables)
	{
	}

	/**
	 * Add variables
	 * 
	 * @param vc
	 */
	public void addVariables(Collection<String> vc)
	{
	}

   /**
    * @param constants new constant set
    */
   public void setConstants(Set<String> constants)
   {
   }

   /**
    * Add constants
    * 
    * @param cc constants to add
    */
   public void addConstants(Collection<String> cc)
   {
   }

	/**
	 * @return the functionsCache
	 */
	public String[] getFunctions()
	{
      return new String[0];
	}

	/**
	 * @return the variablesCache
	 */
	public String[] getVariables()
	{
      return new String[0];
	}

   /**
    * @return constants cache
    */
   public String[] getConstants()
   {
      return new String[0];
   }

   /**
    * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
    */
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
      Point p = editor.computeSize(wHint, hHint, changed);
		p.y += 4;
		return p;
	}

	/**
	 * Show/hide line numbers in editor
	 * 
	 * @param show
	 */
	public void showLineNumbers(boolean show)
	{
	}

	/**
	 * Toggle hints area
	 */
	private void toggleHints()
	{
	   if (hintTextControl != null)
	   {
	      hintTextControl.dispose();
	      hintTextControl = null;
	      hintsExpandButton.setImage(SharedIcons.IMG_EXPAND);
	   }
	   else
	   {
         hintTextControl = new Text(hintArea, SWT.MULTI | SWT.WRAP);
         hintTextControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
         hintTextControl.setEditable(false);
         hintTextControl.setText(hintText);
         hintTextControl.setBackground(ThemeEngine.getBackgroundColor("MessageBar"));
         hintTextControl.setForeground(ThemeEngine.getForegroundColor("MessageBar"));
         hintsExpandButton.setImage(SharedIcons.IMG_COLLAPSE);
      }
      layout(true, true);
	}

   /**
    * Compile script
    */
   public void compileScript()
   {
      final String source = getText();
      editor.setEditable(false);
      final NXCSession session = Registry.getSession();
      new Job(i18n.tr("Compile script"), null) {
         @Override
         protected void run(IProgressMonitor monitor) throws Exception
         {
            final ScriptCompilationResult result = session.compileScript(source, false);
            runInUIThread(new Runnable() {
               @Override
               public void run()
               {
                  if (result.success)
                  {
                     clearMessages();
                     addMessage(MessageArea.SUCCESS, i18n.tr("Script compiled successfully"), true);
                  }
                  else
                  {
                     clearMessages();
                     addMessage(MessageArea.ERROR, result.errorMessage, true);
                  }
                  editor.setEditable(true);
               }
            });
         }

         @Override
         protected String getErrorMessage()
         {
            return i18n.tr("Cannot compile script");
         }
      }.start();
   }

   /**
    * Highlight error line.
    *
    * @param lineNumber line number to highlight
    */
   public void highlightErrorLine(int lineNumber)
   {
   }

   /**
    * Clear line highlighting
    */
   public void clearHighlighting()
   {
   }

   /**
    * Get number of lines in editor.
    *
    * @return number of lines in editor
    */
   public int getLineCount()
   {
      String text = editor.getText();
      if (text.isEmpty())
         return 0;
      return text.split(editor.getLineDelimiter()).length + 1;
   }

   /**
    * Get current line number.
    *
    * @return current line number
    */
   public int getCurrentLine()
   {
      int i = editor.getCaretPosition();
      String text = editor.getText().substring(0, i);
      if (text.isEmpty())
         return 0;
      return text.split(editor.getLineDelimiter()).length + 1;
   }

   /**
    * Set caret to line with given number.
    *
    * @param lineNumber line number
    */
   public void setCaretToLine(int lineNumber)
   {
      // TODO: implement if possible
   }
}
