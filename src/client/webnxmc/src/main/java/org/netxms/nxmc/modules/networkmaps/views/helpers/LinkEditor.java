/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2021 Victor Kirhenshtein
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
package org.netxms.nxmc.modules.networkmaps.views.helpers;

import java.util.List;
import org.netxms.client.maps.NetworkMapLink;
import org.netxms.client.maps.NetworkMapPage;
import org.netxms.client.maps.configs.SingleDciConfig;
import org.netxms.client.maps.elements.NetworkMapObject;

/**
 * Utility class for editing map links 
 */
public class LinkEditor
{
	private NetworkMapLink link;
	private NetworkMapPage mapPage;
	private String name;
	private int type;
	private String connectorName1;
	private String connectorName2;
	private int color;
	private int colorSource;
   private String colorProvider;
	private List<Long> statusObjects;
	private int routingAlgorithm;
	private boolean modified = false;
	private List<SingleDciConfig> dciList;
	private boolean useActiveThresholds;
	
	/**
	 * @param link
	 * @param mapPage
	 */
	public LinkEditor(NetworkMapLink link, NetworkMapPage mapPage)
	{
		this.link = link;
		this.mapPage = mapPage;
		name = link.getName();
		type = link.getType();
		connectorName1 = link.getConnectorName1();
		connectorName2 = link.getConnectorName2();
		color = link.getColor();
		colorSource = link.getColorSource();
      colorProvider = link.getColorProvider();
		statusObjects = link.getStatusObjects();
		routingAlgorithm = link.getRouting();
		dciList = link.getDciAsList();
		useActiveThresholds = link.getConfig().isUseActiveThresholds();
	}
	
	/**
	 * Update network map link
	 */
	public void update()
	{
		mapPage.removeLink(link);
		long[] bp = link.getBendPoints();
		link = new NetworkMapLink(link.getId(), name, type, link.getElement1(), link.getElement2(), connectorName1, connectorName2, dciList.toArray(new SingleDciConfig[dciList.size()]), link.getFlags(), link.isLocked()); 
		link.setColor(color);
		link.setColorSource(colorSource);
      link.setColorProvider(colorProvider);
		link.setStatusObjects(statusObjects);
		link.setRouting(routingAlgorithm);
		link.setBendPoints(bp);
		link.getConfig().setUseActiveThresholds(useActiveThresholds);
		mapPage.addLink(link);
		modified = true;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type)
	{
		this.type = type;
	}

	/**
	 * @return the connectorName1
	 */
	public String getConnectorName1()
	{
		return connectorName1;
	}

	/**
	 * @param connectorName1 the connectorName1 to set
	 */
	public void setConnectorName1(String connectorName1)
	{
		this.connectorName1 = connectorName1;
	}

	/**
	 * @return the connectorName2
	 */
	public String getConnectorName2()
	{
		return connectorName2;
	}

	/**
	 * @param connectorName2 the connectorName2 to set
	 */
	public void setConnectorName2(String connectorName2)
	{
		this.connectorName2 = connectorName2;
	}

	/**
	 * @return the color
	 */
	public int getColor()
	{
		return color;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(int color)
	{
		this.color = color;
	}

	/**
    * @return the colorSource
    */
   public int getColorSource()
   {
      return colorSource;
   }

   /**
    * @param colorSource the colorSource to set
    */
   public void setColorSource(int colorSource)
   {
      this.colorSource = colorSource;
   }

   /**
    * @return the colorProvider
    */
   public String getColorProvider()
   {
      return colorProvider;
   }

   /**
    * @param colorProvider the colorProvider to set
    */
   public void setColorProvider(String colorProvider)
   {
      this.colorProvider = colorProvider;
   }

   /**
    * @return the statusObject
    */
	public List<Long> getStatusObjects()
	{
		return statusObjects;
	}

	/**
	 * @param statusObject the statusObject to set
	 */
	public void setStatusObjects(List<Long> statusObject)
	{
		this.statusObjects = statusObject;
	}
	
	/**
    * @param id the id of status object to be added
    */
	public void addStatusObject(Long id)
	{
	   statusObjects.add(id);
	}
	
	/**
    * @param index the index of status the object to be removed
    */
	public void removeStatusObjectByIndex(int index)
	{
	   statusObjects.remove(index);
	}

	/**
	 * @return the routingAlgorithm
	 */
	public int getRoutingAlgorithm()
	{
		return routingAlgorithm;
	}

	/**
	 * @param routingAlgorithm the routingAlgorithm to set
	 */
	public void setRoutingAlgorithm(int routingAlgorithm)
	{
		this.routingAlgorithm = routingAlgorithm;
	}

	/**
	 * @return the mapPage
	 */
	public NetworkMapPage getMapPage()
	{
		return mapPage;
	}
	
	/**
	 * @return
	 */
	public long getObject1()
	{
		NetworkMapObject e = (NetworkMapObject)mapPage.getElement(link.getElement1(), NetworkMapObject.class);
		return (e != null) ? e.getObjectId() : 0;
	}

	/**
	 * @return
	 */
	public long getObject2()
	{
		NetworkMapObject e = (NetworkMapObject)mapPage.getElement(link.getElement2(), NetworkMapObject.class);
		return (e != null) ? e.getObjectId() : 0;
	}

	/**
	 * @return the modified
	 */
	public boolean isModified()
	{
		return modified;
	}

   /**
    * @return the config
    */
   public List<SingleDciConfig> getDciList()
   {
      return dciList;
   }

   /**
    * @param config the config to set
    */
   public void setDciList(List<SingleDciConfig> dciList)
   {
      this.dciList = dciList;
   }
   
   /**
    * @return are active thresholds used
    */
   public boolean isUseActiveThresholds()
   {
      return useActiveThresholds;
   }
   
   /**
    * @param useActiveThresholds set to use active thresholds
    */
   public void setUseActiveThresholds(boolean useActiveThresholds)
   {
      this.useActiveThresholds = useActiveThresholds;
   }
}
