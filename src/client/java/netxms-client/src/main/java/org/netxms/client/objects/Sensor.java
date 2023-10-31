/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Victor Kirhenshtein
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
package org.netxms.client.objects;

import java.util.Date;
import java.util.Set;
import org.netxms.base.MacAddress;
import org.netxms.base.NXCPCodes;
import org.netxms.base.NXCPMessage;
import org.netxms.client.NXCSession;
import org.netxms.client.PollState;
import org.netxms.client.constants.AgentCacheMode;
import org.netxms.client.objects.interfaces.PollingTarget;
import org.netxms.client.sensor.configs.DlmsConfig;
import org.netxms.client.sensor.configs.LoraWanConfig;
import org.netxms.client.sensor.configs.LoraWanRegConfig;
import org.netxms.client.sensor.configs.SensorConfig;
import org.netxms.client.sensor.configs.SensorRegistrationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mobile device object
 */
public class Sensor extends DataCollectionTarget implements PollingTarget
{
   /**
    * Sensor flags
    */
   public final static int SENSOR_PROVISIONED         = 0x00010000;
   public final static int SENSOR_REGISTERED          = 0x00020000;
   public final static int SENSOR_ACTIVE              = 0x00040000;
   public final static int SENSOR_CONF_UPDATE_PENDING = 0x00080000;
   
   /**
    * Sensor communication protocol type
    */
   public final static int  SENSOR_PROTO_UNKNOWN   = 0;
   public final static int  COMM_LORAWAN           = 1;   
   public final static int  COMM_DLMS              = 2;
   public final static String[] COMM_METHOD = { "Unknown", "LoRaWAN", "DLMS" };
   
   /**
    * Sensor device class
    */
   public static final int SENSOR_CLASS_UNKNOWN = 0;
   public static final int SENSOR_UPS = 1;
   public static final int SENSOR_WATER_METER = 2;
   public static final int SENSOR_ELECTR_METER = 3;

   /**
    * Sensor device class names
    */
   public static final String[] DEV_CLASS_NAMES = { "Unknown", "UPS", "Water meter", "Electricity meter" };

   private static final Logger logger = LoggerFactory.getLogger(Sensor.class);

   protected MacAddress macAddress;
	private int deviceClass;
	private String vendor;
	private int  commProtocol;
	private SensorRegistrationConfig regConfig;
   private SensorConfig config;
	private String serialNumber;
	private String deviceAddress;
	private String metaType;
	private String description;
	private Date lastConnectionTime;
	private int frameCount; //zero when no info
   private int signalStrenght; //+1 when no information(cannot be +)
   private int signalNoise; //*10 from origin number
   private int frequency; //*10 from origin number
   private long proxyId;
	
	/**
    * Create from NXCP message.
    *
    * @param msg NXCP message
    * @param session owning client session
	 */
	public Sensor(NXCPMessage msg, NXCSession session)
	{
		super(msg, session);
		macAddress = new MacAddress(msg.getFieldAsBinary(NXCPCodes.VID_MAC_ADDR));
	   deviceClass  = msg.getFieldAsInt32(NXCPCodes.VID_DEVICE_CLASS);
	   vendor = msg.getFieldAsString(NXCPCodes.VID_VENDOR);
	   commProtocol = msg.getFieldAsInt32(NXCPCodes.VID_COMM_PROTOCOL);
	   setXmlRegConfig(msg.getFieldAsString(NXCPCodes.VID_XML_REG_CONFIG));
      setXmlConfig(msg.getFieldAsString(NXCPCodes.VID_XML_CONFIG));
	   serialNumber = msg.getFieldAsString(NXCPCodes.VID_SERIAL_NUMBER);
	   deviceAddress = msg.getFieldAsString(NXCPCodes.VID_DEVICE_ADDRESS);
	   metaType = msg.getFieldAsString(NXCPCodes.VID_META_TYPE);
	   description = msg.getFieldAsString(NXCPCodes.VID_DESCRIPTION);
	   lastConnectionTime  = msg.getFieldAsDate(NXCPCodes.VID_LAST_CONN_TIME);
	   frameCount = msg.getFieldAsInt32(NXCPCodes.VID_FRAME_COUNT); 
	   signalStrenght = msg.getFieldAsInt32(NXCPCodes.VID_SIGNAL_STRENGHT); 
	   signalNoise = msg.getFieldAsInt32(NXCPCodes.VID_SIGNAL_NOISE); 
	   frequency = msg.getFieldAsInt32(NXCPCodes.VID_FREQUENCY);
	   proxyId = msg.getFieldAsInt32(NXCPCodes.VID_SENSOR_PROXY);
	}

	/**
	 * @see org.netxms.client.objects.AbstractObject#isAllowedOnMap()
	 */
	@Override
	public boolean isAllowedOnMap()
	{
		return true;
	}

   /**
    * @see org.netxms.client.objects.AbstractObject#isAlarmsVisible()
    */
   @Override
   public boolean isAlarmsVisible()
   {
      return true;
   }

   /**
    * @see org.netxms.client.objects.AbstractObject#getObjectClassName()
    */
   @Override
   public String getObjectClassName()
   {
      return "Sensor";
   }

   /**
    * @see org.netxms.client.objects.AbstractObject#getStrings()
    */
   @Override
   public Set<String> getStrings()
   {      
      Set<String> strings = super.getStrings();
      addString(strings, macAddress.toString());
      //addString(strings, DEV_CLASS_NAMES[deviceClass]);
      addString(strings, vendor);
      addString(strings, serialNumber);
      addString(strings, deviceAddress);
      addString(strings, metaType);
      addString(strings, description);
      return strings;
   }
   
   /**
    *  Config class creator from XML 
    *  
    * @param xml xml for object creation
    */
   private void setXmlRegConfig(String xml)
   {
      switch(commProtocol)
      {
         case COMM_LORAWAN:
            try
            {
               regConfig = SensorRegistrationConfig.createFromXml(LoraWanRegConfig.class, xml);
            }
            catch(Exception e)
            {
               regConfig = new LoraWanRegConfig();
               logger.debug("Cannot create LoraWanRegConfig object from XML document", e);
            }
            break;
         default:
            config = null;
      }
   }
   
   /**
    *  Config class creator from XML 
    *  
    * @param xml xml for object creation
    */
   private void setXmlConfig(String xml)
   {
      switch(commProtocol)
      {
         case COMM_LORAWAN:
            try
            {
               config = SensorConfig.createFromXml(LoraWanConfig.class, xml);
            }
            catch(Exception e)
            {
               config = new LoraWanConfig();
               logger.debug("Cannot create LoraWanConfig object from XML document", e);
            }
            break;
         case COMM_DLMS:
            try
            {
               config = SensorConfig.createFromXml(DlmsConfig.class, xml);
            }
            catch(Exception e)
            {
               config = new DlmsConfig();
               //Logger.debug("Sensor.Sensor", "Cannot parse DlmsConfig XML", e);
            }
            break;
         default:
            config = null;
      }
   }

	/**
	 * @return the vendor
	 */
	public final String getVendor()
	{
		return vendor;
	}
	/**
	 * @return the serialNumber
	 */
	public final String getSerialNumber()
	{
		return serialNumber;
	}

   /**
    * @return the flags
    */
   public int getFlags()
   {
      return flags;
   }

   /**
    * @return the macAddress
    */
   public MacAddress getMacAddress()
   {
      return macAddress;
   }

   /**
    * @return the frameCount
    */
   public int getFrameCount()
   {
      return frameCount;
   }

   /**
    * @return the signalStrenght
    */
   public int getSignalStrenght()
   {
      return signalStrenght;
   }

   /**
    * @return the frequency
    */
   public int getFrequency()
   {
      return frequency;
   }

   /**
    * @return the deviceClass
    */
   public int getDeviceClass()
   {
      return deviceClass;
   }

   /**
    * @return the commProtocol
    */
   public int getCommProtocol()
   {
      return commProtocol;
   }

   /**
    * @return the deviceAddress
    */
   public String getDeviceAddress()
   {
      return deviceAddress;
   }

   /**
    * @return the metaType
    */
   public String getMetaType()
   {
      return metaType;
   }

   /**
    * @return the description
    */
   public String getDescription()
   {
      return description;
   }

   /**
    * @return the lastConnectionTime
    */
   public Date getLastConnectionTime()
   {
      return lastConnectionTime;
   }

   /**
    * @return the proxyId
    */
   public long getProxyId()
   {
      return proxyId;
   }

   /**
    * @return the signalNoise
    */
   public int getSignalNoise()
   {
      return signalNoise;
   }
   
   /**
    * @return the regConfig
    */
   public SensorRegistrationConfig getRegConfig()
   {
      return regConfig;
   }

   /**
    * @return the config
    */
   public SensorConfig getConfig()
   {
      return config;
   }

   @Override
   public int getIfXTablePolicy()
   {
      return 0;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#getAgentCacheMode()
    */
   @Override
   public AgentCacheMode getAgentCacheMode()
   {
      return null;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#getPollerNodeId()
    */
   @Override
   public long getPollerNodeId()
   {
      return 0;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#canHaveAgent()
    */
   @Override
   public boolean canHaveAgent()
   {
      return false;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#canHaveInterfaces()
    */
   @Override
   public boolean canHaveInterfaces()
   {
      return false;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#canHavePollerNode()
    */
   @Override
   public boolean canHavePollerNode()
   {
      return false;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#canUseEtherNetIP()
    */
   @Override
   public boolean canUseEtherNetIP()
   {
      return false;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#canUseModbus()
    */
   @Override
   public boolean canUseModbus()
   {
      return false;
   }

   /**
    * @see org.netxms.client.objects.interfaces.PollingTarget#getPollStates()
    */
   @Override
   public PollState[] getPollStates()
   {
      return pollStates;
   }
}
