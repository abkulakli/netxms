/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2022 Raden Solutions
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
package org.netxms.nxmc.modules.datacollection.propertypages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.netxms.client.NXCSession;
import org.netxms.client.constants.DataOrigin;
import org.netxms.client.constants.DataType;
import org.netxms.client.datacollection.DataCollectionItem;
import org.netxms.client.datacollection.DataCollectionObject;
import org.netxms.client.datacollection.DataCollectionTable;
import org.netxms.client.objects.Node;
import org.netxms.client.snmp.SnmpObjectId;
import org.netxms.client.snmp.SnmpObjectIdFormatException;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.base.propertypages.PropertyDialog;
import org.netxms.nxmc.base.widgets.Hyperlink;
import org.netxms.nxmc.base.widgets.LabeledText;
import org.netxms.nxmc.base.widgets.events.HyperlinkAdapter;
import org.netxms.nxmc.base.widgets.events.HyperlinkEvent;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.modules.datacollection.DataCollectionObjectEditor;
import org.netxms.nxmc.modules.datacollection.dialogs.IParameterSelectionDialog;
import org.netxms.nxmc.modules.datacollection.dialogs.SelectAgentParamDlg;
import org.netxms.nxmc.modules.datacollection.dialogs.SelectInternalParamDlg;
import org.netxms.nxmc.modules.datacollection.dialogs.SelectParameterScriptDialog;
import org.netxms.nxmc.modules.datacollection.dialogs.SelectSnmpParamDlg;
import org.netxms.nxmc.modules.datacollection.dialogs.SelectWebServiceDlg;
import org.netxms.nxmc.modules.datacollection.dialogs.WinPerfCounterSelectionDialog;
import org.netxms.nxmc.modules.datacollection.widgets.helpers.DataCollectionDisplayInfo;
import org.netxms.nxmc.modules.objects.widgets.ObjectSelector;
import org.netxms.nxmc.tools.WidgetHelper;
import org.xnap.commons.i18n.I18n;

/**
 * "General" property page for DCO
 */
public class General extends AbstractDCIPropertyPage
{	
   private static final I18n i18n = LocalizationHelper.getI18n(General.class);
   
   public static final String[] DATA_UNITS = 
   {
      "B/s",
      "b/s",
      "B (Metric)",
      "B (IEC)",
      "b (Metric)",
      "b (IEC)",
      "Uptime", //special
      "Epoch time", //special
      "dBm",
      "sr",
      "Hz",
      "N",
      "Pa",
      "J",
      "W",
      "V",
      "Ω",
      "Wb",
      "T",
      "°C",
      "°F",
      "lm",
      "lx",
      "%"
   };
   private static final int SUB_ELEMENT_INDENT = 20;
   
	private DataCollectionObject dco;
   private Combo origin;
   private Text parameter;
	private LabeledText description;
	private Button selectButton;
	private ObjectSelector sourceNode;
   private Combo dataType;
   private Combo useMultipliers;
   private Combo dataUnit;
	private Button scheduleDefault;
   private Button scheduleFixed;
   private Button scheduleAdvanced;
   private Label serverDefaultIntervalLabel;
   private Composite pollingIntervalComposite;
   private Label pollingIntervalLabel;
   private Hyperlink scheduleLink;
   private Button storageDefault;
   private Button storageFixed;
   private Button storageNoStorage;
   private Composite retentionTimeComposite;
   private Label serverDefaultRetentionLabel;
	private Text pollingInterval;
	private Text retentionTime;
	private Button checkSaveOnlyChangedValues;
   
   public General(DataCollectionObjectEditor editor)
   {
      super(i18n.tr("General"), editor);
   }
	
   /**
    * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
    */
	@Override
	protected Control createContents(Composite parent)
	{		
	   Composite dialogArea = (Composite)super.createContents(parent);
		dco = editor.getObject();

		GridLayout layout = new GridLayout();
		layout.verticalSpacing = WidgetHelper.OUTER_SPACING;
		layout.marginWidth = 0;
		layout.marginRight = WidgetHelper.OUTER_SPACING;
		layout.marginHeight = 0;
      dialogArea.setLayout(layout);
      
      Group groupMetricConfig = new Group(dialogArea, SWT.NONE);
      groupMetricConfig.setText("Metric to collect");
      layout = new GridLayout();
      layout.marginHeight = WidgetHelper.OUTER_SPACING;
      layout.marginWidth = WidgetHelper.OUTER_SPACING;
      layout.numColumns = 3;
      groupMetricConfig.setLayout(layout);
      GridData gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      gd.verticalAlignment = SWT.FILL;
      groupMetricConfig.setLayoutData(gd);
      
      origin = new Combo(groupMetricConfig, SWT.READ_ONLY);
      origin.add(i18n.tr("Internal"));
      origin.add(i18n.tr("NetXMS Agent"));
      origin.add(i18n.tr("SNMP"));
      origin.add(i18n.tr("Web Service"));
      origin.add(i18n.tr("Push"));
      origin.add(i18n.tr("Windows Performance Counters"));
      origin.add(i18n.tr("SM-CLP"));
      origin.add(i18n.tr("Script"));
      origin.add(i18n.tr("SSH"));
      origin.add(i18n.tr("MQTT"));
      origin.add(i18n.tr("Network Device Driver"));
      origin.select(dco.getOrigin().getValue());
      origin.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetDefaultSelected(SelectionEvent e)
         {
            widgetSelected(e);
         }

         @Override
         public void widgetSelected(SelectionEvent e)
         {
            onOriginChange();
         }
      });
      
      parameter = new Text(groupMetricConfig, SWT.BORDER);
      parameter.setTextLimit(255);
      parameter.setText(dco.getName());
      gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;    
      parameter.setLayoutData(gd);
      
      selectButton = new Button(groupMetricConfig, SWT.PUSH);
      selectButton.setText("Lookup");
      selectButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetDefaultSelected(SelectionEvent e)
         {
            widgetSelected(e);
         }

         @Override
         public void widgetSelected(SelectionEvent e)
         {
            selectParameter();
         }
      });
      gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      gd.verticalAlignment = SWT.END;
      selectButton.setLayoutData(gd);      

      description = new LabeledText(groupMetricConfig, SWT.NONE);
      description.setLabel("Display name");
      description.getTextControl().setTextLimit(255);
      description.setText(dco.getDescription());
      gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      gd.horizontalSpan = 3;
      description.setLayoutData(gd);

      sourceNode = new ObjectSelector(groupMetricConfig, SWT.NONE, true);
      sourceNode.setLabel(i18n.tr("Source node"));
      sourceNode.setObjectClass(Node.class);
      sourceNode.setObjectId(dco.getSourceNode());
      sourceNode.setEnabled(dco.getOrigin() != DataOrigin.PUSH);
      gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      gd.verticalAlignment = SWT.FILL;
      gd.horizontalSpan = 3;
      sourceNode.setLayoutData(gd);
      
      if (dco instanceof DataCollectionItem)
      {
         DataCollectionItem dci = (DataCollectionItem)dco;
         
         Group groupProcessingAndVisualization = new Group(dialogArea, SWT.NONE);
         groupProcessingAndVisualization.setText(i18n.tr("Polling"));
         layout = new GridLayout();
         layout.marginHeight = WidgetHelper.OUTER_SPACING;
         layout.marginWidth = WidgetHelper.OUTER_SPACING;
         layout.numColumns = 3;
         groupProcessingAndVisualization.setLayout(layout);
         gd = new GridData();
         gd.grabExcessHorizontalSpace = true;
         gd.horizontalAlignment = SWT.FILL;
         gd.verticalAlignment = SWT.FILL;
         groupProcessingAndVisualization.setLayoutData(gd);
   
         gd = new GridData();
         gd.grabExcessHorizontalSpace = true;
         gd.horizontalAlignment = SWT.FILL;
         dataType = WidgetHelper.createLabeledCombo(groupProcessingAndVisualization, SWT.READ_ONLY, i18n.tr("Data Type"), gd);
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.INT32));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.UINT32));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.COUNTER32));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.INT64));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.UINT64));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.COUNTER64));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.FLOAT));
         dataType.add(DataCollectionDisplayInfo.getDataTypeName(DataType.STRING));
         dataType.select(getDataTypePosition(dci.getDataType()));
         
         gd = new GridData();
         gd.grabExcessHorizontalSpace = true;
         gd.horizontalAlignment = SWT.FILL;
         dataUnit = WidgetHelper.createLabeledCombo(groupProcessingAndVisualization, SWT.NONE, "Units", gd);
         int selection = -1;
         int index = 0;
         for (; index < DATA_UNITS.length; index++)
         {
            dataUnit.add(DATA_UNITS[index]);
            if (dci.getUnitName() != null  && dci.getUnitName().equals(DATA_UNITS[index]))
            {
               selection = index;
            }
         }
         if (selection == -1 && dci.getUnitName() != null)
         {
            dataUnit.add(dci.getUnitName()); 
            selection = index;
         }
         dataUnit.select(selection);
         
         useMultipliers = WidgetHelper.createLabeledCombo(groupProcessingAndVisualization, SWT.READ_ONLY, "Use multipliers", new GridData());
         useMultipliers.add("Default");
         useMultipliers.add("Yes");
         useMultipliers.add("No");
         useMultipliers.select(dci.getMultipliersSelection());
      }
         
      Group groupPolling = new Group(dialogArea, SWT.NONE);
      groupPolling.setText("Collection schedule");
      layout = new GridLayout();
      layout.marginTop = WidgetHelper.OUTER_SPACING;
      layout.marginBottom = WidgetHelper.OUTER_SPACING * 2;
      layout.marginWidth = WidgetHelper.OUTER_SPACING;
      layout.numColumns = 3;
      layout.makeColumnsEqualWidth = true;
      groupPolling.setLayout(layout);
      gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      gd.verticalAlignment = SWT.FILL;
      groupPolling.setLayoutData(gd);
      
      SelectionListener pollingButtons = new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            pollingInterval.setEnabled(scheduleFixed.getSelection());
            if (scheduleFixed.getSelection())
            {
               pollingInterval.setFocus();
            }
            serverDefaultIntervalLabel.setVisible(scheduleDefault.getSelection());
            pollingIntervalComposite.setVisible(scheduleFixed.getSelection());
            scheduleLink.setVisible(scheduleAdvanced.getSelection());
         }
      };

      final NXCSession session = Registry.getSession();
      scheduleDefault = new Button(groupPolling, SWT.RADIO);
      scheduleDefault.setText("Server default interval");
      scheduleDefault.setSelection(dco.getPollingScheduleType() == DataCollectionObject.POLLING_SCHEDULE_DEFAULT);
      scheduleDefault.addSelectionListener(pollingButtons);
      scheduleDefault.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

      scheduleFixed = new Button(groupPolling, SWT.RADIO);
      scheduleFixed.setText("Custom interval");
      scheduleFixed.setSelection(dco.getPollingScheduleType() == DataCollectionObject.POLLING_SCHEDULE_CUSTOM);
      scheduleFixed.addSelectionListener(pollingButtons);
      scheduleFixed.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

      scheduleAdvanced = new Button(groupPolling, SWT.RADIO);
      scheduleAdvanced.setText("Advanced schedule");
      scheduleAdvanced.setSelection(dco.getPollingScheduleType() == DataCollectionObject.POLLING_SCHEDULE_ADVANCED);
      scheduleAdvanced.addSelectionListener(pollingButtons);
      scheduleAdvanced.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

      serverDefaultIntervalLabel = new Label(groupPolling, SWT.NONE);
      serverDefaultIntervalLabel.setText(String.format("%d seconds", session.getDefaultDciPollingInterval()));
      gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      gd.horizontalIndent = SUB_ELEMENT_INDENT;
      serverDefaultIntervalLabel.setLayoutData(gd);

      pollingIntervalComposite = new Composite(groupPolling, SWT.NONE);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.horizontalSpacing = WidgetHelper.INNER_SPACING;
      layout.numColumns = 2;
      pollingIntervalComposite.setLayout(layout);
      pollingInterval = new Text(pollingIntervalComposite, SWT.BORDER);
      pollingInterval.setText(dco.getPollingInterval() != null ? dco.getPollingInterval() : "");
      pollingInterval.setEnabled((dco.getPollingScheduleType() == DataCollectionObject.POLLING_SCHEDULE_CUSTOM) && (dco.getOrigin() != DataOrigin.PUSH));
      pollingIntervalLabel = new Label(pollingIntervalComposite, SWT.NONE);
      pollingIntervalLabel.setText("seconds");
      gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      gd.horizontalIndent = SUB_ELEMENT_INDENT;
      pollingIntervalComposite.setLayoutData(gd);

      scheduleLink = new Hyperlink(groupPolling, SWT.NONE);
      scheduleLink.setText("Configure");
      scheduleLink.setUnderlined(true);
      scheduleLink.addHyperlinkListener(new HyperlinkAdapter() {
         @Override
         public void linkActivated(HyperlinkEvent e)
         {
            ((PropertyDialog)getContainer()).showPage("customSchedule");
         }
      });      
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd.horizontalIndent = SUB_ELEMENT_INDENT;
      scheduleLink.setLayoutData(gd);

      serverDefaultIntervalLabel.setVisible(scheduleDefault.getSelection());
      pollingIntervalComposite.setVisible(scheduleFixed.getSelection());
      scheduleLink.setVisible(scheduleAdvanced.getSelection());

      Group groupRetention = new Group(dialogArea, SWT.NONE);
      groupRetention.setText("Retention");
      layout = new GridLayout();
      layout.marginTop = WidgetHelper.OUTER_SPACING;
      layout.marginBottom = WidgetHelper.OUTER_SPACING * 2;
      layout.marginWidth = WidgetHelper.OUTER_SPACING;
      layout.numColumns = 3;
      layout.makeColumnsEqualWidth = true;
      groupRetention.setLayout(layout);
      gd = new GridData();
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = SWT.FILL;
      gd.verticalAlignment = SWT.FILL;
      groupRetention.setLayoutData(gd);

      SelectionListener storageButtons = new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            retentionTime.setEnabled(storageFixed.getSelection());
            if (storageFixed.getSelection())
               retentionTime.setFocus();

            serverDefaultRetentionLabel.setVisible(storageDefault.getSelection());
            retentionTimeComposite.setVisible(storageFixed.getSelection());
         }
      };

      storageDefault = new Button(groupRetention, SWT.RADIO);
      storageDefault.setText("Server default");
      storageDefault.setSelection(dco.getRetentionType() == DataCollectionObject.RETENTION_DEFAULT);
      storageDefault.addSelectionListener(storageButtons);  

      storageFixed = new Button(groupRetention, SWT.RADIO);
      storageFixed.setText("Custom");
      storageFixed.setSelection(dco.getRetentionType() == DataCollectionObject.RETENTION_CUSTOM);
      storageFixed.addSelectionListener(storageButtons);

      storageNoStorage = new Button(groupRetention, SWT.RADIO);
      storageNoStorage.setText("Do not save to the database");
      storageNoStorage.setSelection(dco.getRetentionType() == DataCollectionObject.RETENTION_NONE);
      storageNoStorage.addSelectionListener(storageButtons);

      serverDefaultRetentionLabel = new Label(groupRetention, SWT.NONE);
      serverDefaultRetentionLabel.setText(String.format("%d days", session.getDefaultDciRetentionTime()));
      gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      gd.horizontalIndent = SUB_ELEMENT_INDENT;
      serverDefaultRetentionLabel.setLayoutData(gd);

      retentionTimeComposite = new Composite(groupRetention, SWT.NONE);
      layout = new GridLayout();
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.horizontalSpacing = WidgetHelper.INNER_SPACING;
      layout.numColumns = 2;
      retentionTimeComposite.setLayout(layout);
      retentionTime = new Text(retentionTimeComposite, SWT.BORDER);
      retentionTime.setText(dco.getRetentionTime() != null ? dco.getRetentionTime() : "");
      retentionTime.setEnabled(dco.getRetentionType() == DataCollectionObject.RETENTION_CUSTOM);
      Label label = new Label(retentionTimeComposite, SWT.NONE);
      label.setText("days");
      gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      gd.horizontalIndent = SUB_ELEMENT_INDENT;
      retentionTimeComposite.setLayoutData(gd);

      serverDefaultRetentionLabel.setVisible(storageDefault.getSelection());
      retentionTimeComposite.setVisible(storageFixed.getSelection());

      if (dco instanceof DataCollectionItem)
      {
         DataCollectionItem dci = (DataCollectionItem)dco;

         checkSaveOnlyChangedValues = new Button(groupRetention, SWT.CHECK);
         checkSaveOnlyChangedValues.setText("Save only &changed values");
         checkSaveOnlyChangedValues.setSelection(dci.isStoreChangesOnly());
         gd = new GridData();
         gd.horizontalSpan = 3;
         checkSaveOnlyChangedValues.setLayoutData(gd); 
      }

      onOriginChange();
      return dialogArea;
	}

	/**
	 * Handler for changing item origin
	 */
	private void onOriginChange()
	{
      DataOrigin dataOrigin = DataOrigin.getByValue(origin.getSelectionIndex());
		sourceNode.setEnabled(dataOrigin != DataOrigin.PUSH);
		
		boolean enableSchedule = (dataOrigin != DataOrigin.PUSH) && (dataOrigin != DataOrigin.MQTT);
		scheduleDefault.setEnabled(enableSchedule);
      scheduleFixed.setEnabled(enableSchedule);
      scheduleAdvanced.setEnabled(enableSchedule);
      serverDefaultIntervalLabel.setEnabled(enableSchedule);
      pollingInterval.setEnabled(enableSchedule);
      pollingIntervalLabel.setEnabled(enableSchedule);
      scheduleLink.setEnabled(enableSchedule);
      
		selectButton.setEnabled(
		      (dataOrigin == DataOrigin.AGENT) || 
		      (dataOrigin == DataOrigin.SNMP) || 
		      (dataOrigin == DataOrigin.INTERNAL) || 
		      (dataOrigin == DataOrigin.WINPERF) || 
		      (dataOrigin == DataOrigin.WEB_SERVICE) || 
            (dataOrigin == DataOrigin.DEVICE_DRIVER) || 
		      (dataOrigin == DataOrigin.SCRIPT));
	}
	
	/**
	 * Select parameter
	 */
	private void selectParameter()
	{
		Dialog dlg = null;
      DataOrigin dataOrigin = DataOrigin.getByValue(origin.getSelectionIndex());
      boolean isTable = dco instanceof DataCollectionTable;
      switch(dataOrigin)
		{
         case INTERNAL:
			   if (sourceNode.getObjectId() != 0)
			      dlg = new SelectInternalParamDlg(getShell(), sourceNode.getObjectId(), isTable);
			   else
			      dlg = new SelectInternalParamDlg(getShell(), dco.getNodeId(), isTable);
				break;
         case AGENT:
         case DEVICE_DRIVER:
			   if (sourceNode.getObjectId() != 0)
               dlg = new SelectAgentParamDlg(getShell(), sourceNode.getObjectId(), dataOrigin, isTable);
			   else
               dlg = new SelectAgentParamDlg(getShell(), dco.getNodeId(), dataOrigin, isTable);
				break;
         case SNMP:
				SnmpObjectId oid;
				try
				{
					oid = SnmpObjectId.parseSnmpObjectId(parameter.getText());
				}
				catch(SnmpObjectIdFormatException e)
				{
					oid = null;
				}
				if (sourceNode.getObjectId() != 0)
				   dlg = new SelectSnmpParamDlg(getShell(), oid, sourceNode.getObjectId());
				else
				   dlg = new SelectSnmpParamDlg(getShell(), oid, dco.getNodeId());
				break;
         case WINPERF:
            if (!isTable)
   			   if (sourceNode.getObjectId() != 0)
   			      dlg = new WinPerfCounterSelectionDialog(getShell(), sourceNode.getObjectId());
   			   else
   			      dlg = new WinPerfCounterSelectionDialog(getShell(), dco.getNodeId());
				break;
         case SCRIPT:
            dlg = new SelectParameterScriptDialog(getShell());
            break;
         case WEB_SERVICE:
            if (!isTable)
               dlg = new SelectWebServiceDlg(getShell(), false);
            break;
			default:
				dlg = null;
				break;
		}
		
		if ((dlg != null) && (dlg.open() == Window.OK))
		{
			IParameterSelectionDialog pd = (IParameterSelectionDialog)dlg;
			description.setText(pd.getParameterDescription());
			parameter.setText(pd.getParameterName());
			dataType.select(getDataTypePosition(pd.getParameterDataType()));
         editor.fireOnSelectItemListeners(DataOrigin.getByValue(origin.getSelectionIndex()), pd.getParameterName(),
               pd.getParameterDescription(), pd.getParameterDataType());
		}
	}
   
   /**
    * Apply changes
    * 
    * @param isApply true if update operation caused by "Apply" button
    */
   @Override
   protected boolean applyChanges(final boolean isApply)
   {
      dco.setOrigin(DataOrigin.getByValue(origin.getSelectionIndex()));
      dco.setName(parameter.getText().trim());
		dco.setDescription(description.getText().trim());
      dco.setSourceNode(sourceNode.getObjectId());
      dco.setPollingScheduleType(getScheduleType());
      dco.setPollingInterval(scheduleFixed.getSelection() ? pollingInterval.getText() : null);
      dco.setRetentionType(getRetentionType());
      dco.setRetentionTime(storageFixed.getSelection() ? retentionTime.getText() : null);

      if (dco instanceof DataCollectionItem)
      {
         DataCollectionItem dci = (DataCollectionItem)dco;
         
         dci.setDataType(getDataTypeByPosition(dataType.getSelectionIndex()));
         dci.setUnitName(dataUnit.getText());      
         dci.setMultiplierSelection(useMultipliers.getSelectionIndex());
         
         dci.setStoreChangesOnly(checkSaveOnlyChangedValues.getSelection());
      }
		editor.modify();
		return true;
	}
	
	/**
	 * Get selected schedule type
	 * 
	 * @return int representation of schedule type
	 */
	private int getScheduleType()
	{
	   int type = 0;
	   if (scheduleFixed.getSelection())
	   {
	      type = 1;
	   }
	   else if (scheduleAdvanced.getSelection())
	   {
         type = 2;	      
	   }
	   return type;
	}
   
   /**
    * Get selected retention type
    * 
    * @return int representation of retention type
    */
   private int getRetentionType()
   {
      int type = 0;
      if (storageFixed.getSelection())
      {
         type = 1;
      }
      else if (storageNoStorage.getSelection())
      {
         type = 2;         
      }
      return type;
   }

   /**
    * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
    */
	@Override
	protected void performDefaults()
	{
		super.performDefaults();
		
		NXCSession session = Registry.getSession();
		
		scheduleDefault.setSelection(true);
		scheduleFixed.setSelection(false);
		scheduleAdvanced.setSelection(false);
		pollingInterval.setText(Integer.toString(session.getDefaultDciPollingInterval()));
      storageDefault.setSelection(true);
      storageFixed.setSelection(false);
      storageNoStorage.setSelection(false);
		retentionTime.setText(Integer.toString(session.getDefaultDciRetentionTime()));
      useMultipliers.select(0);
	}

	/**
	 * Get selector position for given data type
	 * 
	 * @param type data type
	 * @return corresponding selector's position
	 */
	private static int getDataTypePosition(DataType type)
	{
	   switch(type)
	   {
	      case COUNTER32:
	         return 2;
	      case COUNTER64:
	         return 5;
	      case FLOAT:
	         return 6;
	      case INT32:
	         return 0;
	      case INT64:
	         return 3;
	      case STRING:
	         return 7;
	      case UINT32:
	         return 1;
	      case UINT64:
	         return 4;
         default:
            return 0;  // fallback to int32
	   }
	}
	
   /**
    * Data type positions in selector
    */
   private static final DataType[] TYPES = { 
      DataType.INT32, DataType.UINT32, DataType.COUNTER32, DataType.INT64,
      DataType.UINT64, DataType.COUNTER64, DataType.FLOAT, DataType.STRING
      };
   
	/**
	 * Get data type by selector position
	 *  
	 * @param position selector position
	 * @return corresponding data type
	 */
	private static DataType getDataTypeByPosition(int position)
	{
      return TYPES[position];
	}
}
