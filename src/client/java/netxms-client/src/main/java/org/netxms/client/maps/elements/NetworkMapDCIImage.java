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
package org.netxms.client.maps.elements;

import org.netxms.base.NXCPMessage;
import org.netxms.client.maps.configs.DCIImageConfiguration;
import org.netxms.client.xml.XMLTools;

/**
 * Network map element representing NetXMS DCI container
 *
 */
public class NetworkMapDCIImage  extends NetworkMapElement
{
   private DCIImageConfiguration imageOptions;
	
	/**
	 * Create DCI image element from NXCP message.
	 *
	 * @param msg NXCP message
	 * @param baseId base field ID
	 */
	protected NetworkMapDCIImage(NXCPMessage msg, long baseId)
	{
		super(msg, baseId);
		String DCIImageConfigXML = msg.getFieldAsString(baseId+10);
		try
      {
         imageOptions = XMLTools.createFromXml(DCIImageConfiguration.class, DCIImageConfigXML);
      }
      catch(Exception e)
      { 
         imageOptions = new DCIImageConfiguration();
      }
		
	}

	/**
	 * Create new DCI image element
	 * 
	 * @param id element ID
	 */
	public NetworkMapDCIImage(long id)
	{
		super(id);
		type = MAP_ELEMENT_DCI_IMAGE;
		imageOptions = new DCIImageConfiguration();
	}

	/**
	 * @see org.netxms.client.maps.elements.NetworkMapElement#fillMessage(org.netxms.base.NXCPMessage, long)
	 */
	@Override
	public void fillMessage(NXCPMessage msg, long baseId)
	{
		super.fillMessage(msg, baseId);
      
      String xml = "";
		try
      {
		   xml = imageOptions.createXml();
      }
      catch(Exception e)
      {
      }
      msg.setField(baseId + 10, xml);
	}

   /**
    * Get image configuration.
    *
    * @return iamge configuration
    */
   public DCIImageConfiguration getImageOptions()
   {
      return imageOptions;
   }

   /**
    * Set image configuration.
    *
    * @param imageOptions new image configuration
    */
   public void setImageOptions(DCIImageConfiguration imageOptions)
   {
      this.imageOptions = imageOptions;
   }
}
