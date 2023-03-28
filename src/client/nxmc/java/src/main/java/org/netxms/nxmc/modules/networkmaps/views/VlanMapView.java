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
package org.netxms.nxmc.modules.networkmaps.views;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.netxms.client.NXCException;
import org.netxms.client.maps.NetworkMapLink;
import org.netxms.client.maps.NetworkMapPage;
import org.netxms.client.maps.elements.NetworkMapObject;
import org.netxms.client.objects.AbstractObject;
import org.netxms.client.objects.Interface;
import org.netxms.client.objects.Node;
import org.netxms.client.topology.Port;
import org.netxms.client.topology.VlanInfo;
import org.netxms.nxmc.base.jobs.Job;
import org.netxms.nxmc.base.views.View;
import org.netxms.nxmc.localization.LocalizationHelper;
import org.netxms.nxmc.resources.ResourceManager;
import org.xnap.commons.i18n.I18n;

/**
 * IP neighbors for given node
 *
 */
public class VlanMapView extends AbstractNetworkMapView
{
   private static final I18n i18n = LocalizationHelper.getI18n(VlanMapView.class);
   public static final String ID = "VlanMap";
	
	private int vlanId;
   private AbstractObject rootObject;

   /**
    * Create "VPNs" view
    */
   public VlanMapView(AbstractObject rootObject, int vlanId)
   {
      super(String.format(i18n.tr("Vlan Map - %d@%s"), vlanId, rootObject.getObjectName()), ResourceManager.getImageDescriptor("icons/object-views/vpn.png"), String.format(i18n.tr("VlanMap%d@%s"), vlanId, rootObject.getObjectName()));
      this.rootObject = rootObject;
      this.vlanId = vlanId;
   }

   /**
    * Create "VPNs" view
    */
   protected VlanMapView()
   {
      super(null, null, "");
   }

   /**
    * @see org.netxms.nxmc.base.views.View#cloneView()
    */
   @Override
   public View cloneView()
   {
      VlanMapView view = (VlanMapView)super.cloneView();
      view.vlanId = vlanId;
      view.rootObject = rootObject;
      return view;
   }

	/**
	 * Build map page
	 */
	protected void buildMapPage()
	{
		if (mapPage == null)
			mapPage = new NetworkMapPage(ID+vlanId);
		
		new Job(String.format(i18n.tr("Get VLAN information for %s"), rootObject.getObjectName()), this) {
			@Override
			protected void run(IProgressMonitor monitor) throws Exception
			{
				NetworkMapPage page = new NetworkMapPage(ID+vlanId);
				collectVlanInfo(page, (Node)rootObject);
				replaceMapPage(page, getDisplay());
			}

         @Override
         protected void jobFailureHandler(Exception e)
			{
				// On failure, create map with root object only
				NetworkMapPage page = new NetworkMapPage(ID+vlanId);
				page.addElement(new NetworkMapObject(mapPage.createElementId(), rootObject.getObjectId()));
				replaceMapPage(page, getDisplay());
			}

			@Override
			protected String getErrorMessage()
			{
				return String.format(i18n.tr("Cannot get VLAN information for %s"), rootObject.getObjectName());
			}
		}.start();
	}
	
	/**
	 * Recursively collect VLAN information starting from given node
	 * 
	 * @param page map page
	 * @param root root node
	 * @throws Exception 
	 * @throws NXCException 
	 */
	private long collectVlanInfo(NetworkMapPage page, Node root) throws Exception
	{
		if (page.findObjectElement(root.getObjectId()) != null)
			return -1;
		
		long rootElementId = mapPage.createElementId();
		page.addElement(new NetworkMapObject(rootElementId, root.getObjectId()));
		
		List<VlanInfo> vlans = session.getVlans(root.getObjectId());
		for(VlanInfo vlan : vlans)
		{
			if (vlan.getVlanId() == vlanId)
			{
				Port[] ports = vlan.getPorts();
				for(Port port : ports)
				{
					processVlanPort(page, root, port, rootElementId);
				}
				break;
			}
		}
		
		return rootElementId;
	}
	
	/**
	 * Process single member port of VLAN. Will add connected switch on other site to the map.
	 * 
	 * @param page
	 * @param root
	 * @param port
	 * @throws Exception 
	 */
	private void processVlanPort(NetworkMapPage page, Node root, Port port, long rootElementId) throws Exception
	{
		Interface iface = (Interface)session.findObjectById(port.getObjectId(), Interface.class);
		if (iface != null)
		{
			Node peerNode = (Node)session.findObjectById(iface.getPeerNodeId(), Node.class);
			if ((peerNode != null) && ((peerNode.getCapabilities() & Node.NC_IS_BRIDGE) != 0))
			{
				try
				{
					long nodeElementId = collectVlanInfo(page, peerNode);
					if (nodeElementId != -1)
					{
						Interface peerIf = (Interface)session.findObjectById(iface.getPeerInterfaceId(), Interface.class);
                  page.addLink(new NetworkMapLink(page.createLinkId(), null, NetworkMapLink.NORMAL, rootElementId, nodeElementId,
								       iface.getObjectName(), (peerIf != null) ? peerIf.getObjectName() : "???", 0)); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				catch(NXCException e)
				{
					// Ignore NetXMS errors for remote bridges 
				}
			}
		}
	}

   /**
    * @see org.netxms.nxmc.base.views.View#isCloseable()
    */
   @Override
   public boolean isCloseable()
   {
      return true;
   }

   /**
    * @see org.netxms.nxmc.modules.objects.views.ObjectView#isValidForContext(java.lang.Object)
    */
   @Override
   public boolean isValidForContext(Object context)
   {
      return (context != null) && ((AbstractObject)context).getObjectId() == rootObject.getObjectId();
   }

   /**
    * @see org.netxms.nxmc.modules.objects.views.ObjectView#onObjectChange(org.netxms.client.objects.AbstractObject)
    */
   @Override
   protected void onObjectChange(AbstractObject object)
   {
      //Do nothing
   }	
}
