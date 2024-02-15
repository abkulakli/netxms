/*
** NetXMS - Network Management System
** Copyright (C) 2003-2024 Victor Kirhenshtein
**
** This program is free software; you can redistribute it and/or modify
** it under the terms of the GNU General Public License as published by
** the Free Software Foundation; either version 2 of the License, or
** (at your option) any later version.
**
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU General Public License for more details.
**
** You should have received a copy of the GNU General Public License
** along with this program; if not, write to the Free Software
** Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
**
** File: accesspoint.cpp
**/

#include "nxcore.h"
#include <asset_management.h>

/**
 * Default constructor
 */
AccessPoint::AccessPoint() : super(Pollable::CONFIGURATION), m_macAddress(MacAddress::ZERO), m_radioInterfaces(0, 4)
{
   m_domainId = 0;
   m_controllerId = 0;
   m_index = 0;
	m_vendor = nullptr;
	m_model = nullptr;
	m_serialNumber = nullptr;
	m_apState = AP_UP;
   m_prevState = m_apState;
}

/**
 * Constructor for creating new access point object
 */
AccessPoint::AccessPoint(const TCHAR *name, uint32_t index, const MacAddress& macAddr) : super(name, Pollable::CONFIGURATION), m_macAddress(macAddr), m_radioInterfaces(0, 4)
{
	m_domainId = 0;
   m_controllerId = 0;
   m_index = index;
	m_vendor = nullptr;
	m_model = nullptr;
	m_serialNumber = nullptr;
	m_apState = AP_UP;
   m_prevState = m_apState;
	m_isHidden = true;
}

/**
 * Destructor
 */
AccessPoint::~AccessPoint()
{
	MemFree(m_vendor);
	MemFree(m_model);
	MemFree(m_serialNumber);
}

/**
 * Create object from database data
 */
bool AccessPoint::loadFromDatabase(DB_HANDLE hdb, UINT32 dwId)
{
   m_id = dwId;

   if (!loadCommonProperties(hdb) || !super::loadFromDatabase(hdb, dwId))
      return false;

   if (Pollable::loadFromDatabase(hdb, m_id))
   {
      if (static_cast<uint32_t>(time(nullptr) - m_configurationPollState.getLastCompleted()) < g_configurationPollingInterval)
         m_runtimeFlags |= ODF_CONFIGURATION_POLL_PASSED;
   }

	TCHAR query[256];
	_sntprintf(query, 256, _T("SELECT mac_address,vendor,model,serial_number,domain_id,controller_id,ap_state,ap_index FROM access_points WHERE id=%u"), m_id);
	DB_RESULT hResult = DBSelect(hdb, query);
	if (hResult == nullptr)
		return false;

	m_macAddress = DBGetFieldMacAddr(hResult, 0, 0);
	m_vendor = DBGetField(hResult, 0, 1, nullptr, 0);
	m_model = DBGetField(hResult, 0, 2, nullptr, 0);
	m_serialNumber = DBGetField(hResult, 0, 3, nullptr, 0);
	m_domainId = DBGetFieldULong(hResult, 0, 4);
   m_controllerId = DBGetFieldULong(hResult, 0, 5);
	m_apState = (AccessPointState)DBGetFieldLong(hResult, 0, 6);
   m_prevState = (m_apState != AP_DOWN) ? m_apState : AP_UP;
   m_index = DBGetFieldULong(hResult, 0, 7);
	DBFreeResult(hResult);

   // Load DCI and access list
   loadACLFromDB(hdb);
   loadItemsFromDB(hdb);
   for(int i = 0; i < m_dcObjects.size(); i++)
      if (!m_dcObjects.get(i)->loadThresholdsFromDB(hdb))
         return false;
   loadDCIListForCleanup(hdb);

   return true;
}

/**
 * Save object to database
 */
bool AccessPoint::saveToDatabase(DB_HANDLE hdb)
{
   bool success = super::saveToDatabase(hdb);

   // Lock object's access
   if (success && (m_modified & MODIFY_OTHER))
   {
      static const TCHAR *columns[] = { _T("mac_address"), _T("vendor"), _T("model"), _T("serial_number"), _T("domain_id"), _T("controller_id"), _T("ap_state"), _T("ap_index"), nullptr };
      DB_STATEMENT hStmt = DBPrepareMerge(hdb, _T("access_points"), _T("id"), m_id, columns);
      if (hStmt != nullptr)
      {
         lockProperties();
         DBBind(hStmt, 1, DB_SQLTYPE_VARCHAR, m_macAddress);
         DBBind(hStmt, 2, DB_SQLTYPE_VARCHAR, m_vendor, DB_BIND_STATIC);
         DBBind(hStmt, 3, DB_SQLTYPE_VARCHAR, m_model, DB_BIND_STATIC);
         DBBind(hStmt, 4, DB_SQLTYPE_VARCHAR, m_serialNumber, DB_BIND_STATIC);
         DBBind(hStmt, 5, DB_SQLTYPE_INTEGER, m_domainId);
         DBBind(hStmt, 6, DB_SQLTYPE_INTEGER, m_controllerId);
         DBBind(hStmt, 7, DB_SQLTYPE_INTEGER, static_cast<int32_t>(m_apState));
         DBBind(hStmt, 8, DB_SQLTYPE_INTEGER, m_index);
         DBBind(hStmt, 9, DB_SQLTYPE_INTEGER, m_id);
         success = DBExecute(hStmt);
         DBFreeStatement(hStmt);
         unlockProperties();
      }
      else
      {
         success = false;
      }
   }

   return success;
}

/**
 * Delete object from database
 */
bool AccessPoint::deleteFromDatabase(DB_HANDLE hdb)
{
   bool success = super::deleteFromDatabase(hdb);
   if (success)
      success = executeQueryOnObject(hdb, _T("DELETE FROM access_points WHERE id=?"));
   return success;
}

/**
 * Create NXCP message with essential object's data
 */
void AccessPoint::fillMessageLockedEssential(NXCPMessage *msg, uint32_t userId)
{
   super::fillMessageLockedEssential(msg, userId);
   msg->setField(VID_MODEL, CHECK_NULL_EX(m_model));
   msg->setField(VID_MAC_ADDR, m_macAddress);

   msg->setField(VID_RADIO_COUNT, static_cast<uint16_t>(m_radioInterfaces.size()));
   uint32_t fieldId = VID_RADIO_LIST_BASE;
   for(int i = 0; i < m_radioInterfaces.size(); i++)
   {
      RadioInterfaceInfo *rif = m_radioInterfaces.get(i);
      msg->setField(fieldId++, rif->index);
      msg->setField(fieldId++, rif->name);
      msg->setField(fieldId++, rif->bssid, MAC_ADDR_LENGTH);
      msg->setField(fieldId++, rif->channel);
      msg->setField(fieldId++, rif->powerDBm);
      msg->setField(fieldId++, rif->powerMW);
      msg->setField(fieldId++, rif->ssid);
      fieldId += 3;
   }
}

/**
 * Create NXCP message with object's data
 */
void AccessPoint::fillMessageLocked(NXCPMessage *msg, uint32_t userId)
{
   super::fillMessageLocked(msg, userId);
   msg->setField(VID_DOMAIN_ID, m_domainId);
   msg->setField(VID_CONTROLLER_ID, m_controllerId);
   msg->setField(VID_IP_ADDRESS, m_ipAddress);
	msg->setField(VID_VENDOR, CHECK_NULL_EX(m_vendor));
	msg->setField(VID_SERIAL_NUMBER, CHECK_NULL_EX(m_serialNumber));
   msg->setField(VID_STATE, static_cast<uint16_t>(m_apState));
   msg->setField(VID_AP_INDEX, m_index);
}

/**
 * Attach access point to wireless domain
 */
void AccessPoint::attachToDomain(uint32_t domainId, uint32_t controllerId)
{
	if ((m_domainId == domainId) && (m_controllerId == controllerId))
		return;

	if (m_domainId != domainId)
	{
      if (m_domainId != 0)
      {
         shared_ptr<NetObj> currDomain = FindObjectById(m_domainId, OBJECT_WIRELESSDOMAIN);
         if (currDomain != nullptr)
         {
            currDomain->deleteChild(*this);
            deleteParent(*currDomain);
         }
      }

      shared_ptr<NetObj> newDomain = FindObjectById(domainId, OBJECT_WIRELESSDOMAIN);
      if (newDomain != nullptr)
      {
         newDomain->addChild(self());
         addParent(newDomain);
      }
	}

	lockProperties();
	m_domainId = domainId;
	m_controllerId = controllerId;
	setModified(MODIFY_OTHER);
	unlockProperties();
}

/**
 * Update radio interfaces information
 */
void AccessPoint::updateRadioInterfaces(const StructArray<RadioInterfaceInfo>& ri)
{
	lockProperties();
	m_radioInterfaces.clear();
	m_radioInterfaces.addAll(ri);
	unlockProperties();
}

/**
 * Check if given radio interface index (radio ID) is on this access point
 */
bool AccessPoint::isMyRadio(uint32_t rfIndex)
{
	bool result = false;
	lockProperties();
   for(int i = 0; i < m_radioInterfaces.size(); i++)
   {
      if (m_radioInterfaces.get(i)->index == rfIndex)
      {
         result = true;
         break;
      }
   }
	unlockProperties();
	return result;
}

/**
 * Check if given radio MAC address (BSSID) is on this access point
 */
bool AccessPoint::isMyRadio(const BYTE *bssid)
{
	bool result = false;
	lockProperties();
   for(int i = 0; i < m_radioInterfaces.size(); i++)
   {
      if (!memcmp(m_radioInterfaces.get(i)->bssid, bssid, MAC_ADDR_LENGTH))
      {
         result = true;
         break;
      }
   }
	unlockProperties();
	return result;
}

/**
 * Get radio name
 */
void AccessPoint::getRadioName(uint32_t rfIndex, TCHAR *buffer, size_t bufSize)
{
	buffer[0] = 0;
	lockProperties();
   for(int i = 0; i < m_radioInterfaces.size(); i++)
   {
      if (m_radioInterfaces.get(i)->index == rfIndex)
      {
         _tcslcpy(buffer, m_radioInterfaces.get(i)->name, bufSize);
         break;
      }
   }
	unlockProperties();
}

/**
 * Get name of controller node object
 */
String AccessPoint::getControllerName() const
{
   shared_ptr<NetObj> node = FindObjectById(m_controllerId, OBJECT_NODE);
   return (node != nullptr) ? String(node->getName()) : String(_T("<none>"));
}

/**
 * Get name of wireless domain object
 */
String AccessPoint::getWirelessDomainName() const
{
   shared_ptr<NetObj> domain = FindObjectById(m_domainId, OBJECT_WIRELESSDOMAIN);
   return (domain != nullptr) ? String(domain->getName()) : String(_T("<none>"));
}

/**
 * Update access point information
 */
void AccessPoint::updateInfo(const TCHAR *vendor, const TCHAR *model, const TCHAR *serialNumber)
{
	lockProperties();

	MemFree(m_vendor);
	m_vendor = MemCopyString(vendor);

	MemFree(m_model);
	m_model = MemCopyString(model);

	MemFree(m_serialNumber);
	m_serialNumber = MemCopyString(serialNumber);

	setModified(MODIFY_OTHER);
	unlockProperties();
}

/**
 * Update access point state
 */
void AccessPoint::updateState(AccessPointState state)
{
   if (state == m_apState)
      return;

	lockProperties();
   if (state == AP_DOWN)
      m_prevState = m_apState;
   m_apState = state;
   if (m_status != STATUS_UNMANAGED)
   {
      switch(state)
      {
         case AP_UP:
            m_status = STATUS_NORMAL;
            break;
         case AP_UNPROVISIONED:
            m_status = STATUS_MAJOR;
            break;
         case AP_DOWN:
            m_status = STATUS_CRITICAL;
            break;
         default:
            m_status = STATUS_UNKNOWN;
            break;
      }
   }
   setModified(MODIFY_OTHER);
	unlockProperties();

   if ((state == AP_UP) || (state == AP_UNPROVISIONED) || (state == AP_DOWN))
   {
      EventBuilder((state == AP_UP) ? EVENT_AP_ADOPTED : ((state == AP_UNPROVISIONED) ? EVENT_AP_UNADOPTED : EVENT_AP_DOWN), m_id)
         .param(_T("id"), m_id, EventBuilder::OBJECT_ID_FORMAT)
         .param(_T("name"), m_name)
         .param(_T("macAddr"), m_macAddress)
         .param(_T("ipAddr"), m_ipAddress)
         .param(_T("vendor"), CHECK_NULL_EX(m_vendor))
         .param(_T("model"), CHECK_NULL_EX(m_model))
         .param(_T("serialNumber"), CHECK_NULL_EX(m_serialNumber))
         .post();
   }
}

/**
 * Do status poll
 */
void AccessPoint::statusPollFromController(ClientSession *session, uint32_t rqId, ObjectQueue<Event> *eventQueue, Node *controller, SNMP_Transport *snmpTransport)
{
   m_pollRequestor = session;
   m_pollRequestId = rqId;

   sendPollerMsg(_T("   Starting status poll on access point %s\r\n"), m_name);
   sendPollerMsg(_T("      Current access point status is %s\r\n"), GetStatusAsText(m_status, true));

   AccessPointState state = controller->getAccessPointState(this, snmpTransport, m_radioInterfaces);
   if ((state == AP_UNKNOWN) && m_ipAddress.isValid())
   {
      nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 6, _T("AccessPoint::statusPoll(%s [%u]): unable to get AP state from driver"), m_name, m_id);
      sendPollerMsg(POLLER_WARNING _T("      Unable to get AP state from controller\r\n"));

		uint32_t icmpProxy = 0;

      if (IsZoningEnabled() && (controller->getZoneUIN() != 0))
		{
			shared_ptr<Zone> zone = FindZoneByUIN(controller->getZoneUIN());
			if (zone != nullptr)
			{
				icmpProxy = zone->getProxyNodeId(this);
			}
		}

		if (icmpProxy != 0)
		{
			sendPollerMsg(_T("      Starting ICMP ping via proxy\r\n"));
			nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::StatusPoll(%s [%u]): ping via proxy [%u]"), m_name, m_id, icmpProxy);
			shared_ptr<Node> proxyNode = static_pointer_cast<Node>(g_idxNodeById.get(icmpProxy));
			if ((proxyNode != nullptr) && proxyNode->isNativeAgent() && !proxyNode->isDown())
			{
			   nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::StatusPoll(%s [%u]): proxy node found: %s"), m_name, m_id, proxyNode->getName());
				shared_ptr<AgentConnection> conn = proxyNode->createAgentConnection();
				if (conn != nullptr)
				{
					TCHAR parameter[64], buffer[64];

					_sntprintf(parameter, 64, _T("Icmp.Ping(%s)"), m_ipAddress.toString(buffer));
					if (conn->getParameter(parameter, buffer, 64) == ERR_SUCCESS)
					{
					   nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::StatusPoll(%s [%u]): proxy response: \"%s\""), m_name, m_id, buffer);
						TCHAR *eptr;
						long value = _tcstol(buffer, &eptr, 10);
						if ((*eptr == 0) && (value >= 0))
						{
                     if (value < 10000)
                     {
                        sendPollerMsg(POLLER_ERROR _T("      responded to ICMP ping\r\n"));
                        if (m_apState == AP_DOWN)
                           state = m_prevState;  /* FIXME: get actual AP state here */
                     }
                     else
                     {
                        sendPollerMsg(POLLER_ERROR _T("      no response to ICMP ping\r\n"));
                        state = AP_DOWN;
                     }
						}
					}
					conn->disconnect();
				}
				else
				{
				   nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::statusPoll(%s [%u]): cannot connect to agent on proxy node"), m_name, m_id);
					sendPollerMsg(POLLER_ERROR _T("      Unable to establish connection with proxy node\r\n"));
				}
			}
			else
			{
			   nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::statusPoll(%s [%u]): proxy node not available"), m_name, m_id);
				sendPollerMsg(POLLER_ERROR _T("      ICMP proxy not available\r\n"));
			}
		}
		else	// not using ICMP proxy
		{
         TCHAR buffer[64];
			sendPollerMsg(_T("      Starting ICMP ping\r\n"));
         nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::statusPoll(%s [%u]): calling IcmpPing on %s, timeout=%u, size=%d"),
			      m_name, m_id, m_ipAddress.toString(buffer), g_icmpPingTimeout, g_icmpPingSize);
			uint32_t responseTime;
			uint32_t pingStatus = IcmpPing(m_ipAddress, 3, g_icmpPingTimeout, &responseTime, g_icmpPingSize, false);
			if (pingStatus == ICMP_SUCCESS)
         {
				sendPollerMsg(POLLER_ERROR _T("      responded to ICMP ping\r\n"));
            if (m_apState == AP_DOWN)
               state = m_prevState;  /* FIXME: get actual AP state here */
         }
         else
			{
				sendPollerMsg(POLLER_ERROR _T("      no response to ICMP ping\r\n"));
            state = AP_DOWN;
			}
			nxlog_debug_tag(DEBUG_TAG_STATUS_POLL, 7, _T("AccessPoint::StatusPoll(%s [%u]): ping result %d, state=%d"), m_name, m_id, pingStatus, state);
		}
   }

   updateState(state);

   sendPollerMsg(_T("      Access point status after poll is %s\r\n"), GetStatusAsText(m_status, true));
	sendPollerMsg(_T("   Finished status poll on access point %s\r\n"), m_name);
}

/**
 * Perform configuration poll on this access point.
 */
void AccessPoint::configurationPoll(PollerInfo *poller, ClientSession *session, uint32_t rqId)
{
   lockProperties();
   if (m_isDeleteInitiated || IsShutdownInProgress())
   {
      m_configurationPollState.complete(0);
      unlockProperties();
      return;
   }
   unlockProperties();

   poller->setStatus(_T("wait for lock"));
   pollerLock(configuration);

   if (IsShutdownInProgress())
   {
      pollerUnlock();
      return;
   }

   m_pollRequestor = session;
   m_pollRequestId = rqId;
   nxlog_debug(5, _T("Starting configuration poll of access point %s (ID: %d), m_flags: %d"), m_name, m_id, m_flags);

   // Execute hook script
   poller->setStatus(_T("hook"));
   executeHookScript(_T("ConfigurationPoll"));

   poller->setStatus(_T("autobind"));
   if (ConfigReadBoolean(_T("Objects.AccessPoints.TemplateAutoApply"), false))
      applyTemplates();
   if (ConfigReadBoolean(_T("Objects.AccessPoints.ContainerAutoBind"), false))
      updateContainerMembership();

   sendPollerMsg(_T("Finished configuration poll of access point %s\r\n"), m_name);

   UpdateAssetLinkage(this);
   autoFillAssetProperties();

   lockProperties();
   m_runtimeFlags &= ~ODF_CONFIGURATION_POLL_PENDING;
   m_runtimeFlags |= ODF_CONFIGURATION_POLL_PASSED;
   unlockProperties();

   pollerUnlock();
   nxlog_debug(5, _T("Finished configuration poll of access point %s (ID: %d)"), m_name, m_id);
}

/**
 * Create NXSL object for this object
 */
NXSL_Value *AccessPoint::createNXSLObject(NXSL_VM *vm)
{
   return vm->createValue(vm->createObject(&g_nxslAccessPointClass, new shared_ptr<AccessPoint>(self())));
}

/**
 * Create NXSL array with radio interfaces
 */
NXSL_Value *AccessPoint::getRadioInterfacesForNXSL(NXSL_VM *vm) const
{
   auto a = new NXSL_Array(vm);
   lockProperties();
   for(int i = 0; i < m_radioInterfaces.size(); i++)
   {
      a->append(vm->createValue(vm->createObject(&g_nxslRadioInterfaceClass, new RadioInterfaceInfo(*m_radioInterfaces.get(i)))));
   }
   unlockProperties();
   return vm->createValue(a);
}

/**
 * Get access point's zone UIN
 */
int32_t AccessPoint::getZoneUIN() const
{
   shared_ptr<Node> controller = getController();
   return (controller != nullptr) ? controller->getZoneUIN() : 0;
}

/**
 * Serialize object to JSON
 */
json_t *AccessPoint::toJson()
{
   json_t *root = super::toJson();

   lockProperties();
   json_object_set_new(root, "index", json_integer(m_index));
   json_object_set_new(root, "ipAddress", m_ipAddress.toJson());
   json_object_set_new(root, "domainId", json_integer(m_domainId));
   json_object_set_new(root, "controllerId", json_integer(m_controllerId));
   TCHAR macAddrText[64];
   json_object_set_new(root, "macAddr", json_string_t(m_macAddress.toString(macAddrText)));
   json_object_set_new(root, "vendor", json_string_t(m_vendor));
   json_object_set_new(root, "model", json_string_t(m_model));
   json_object_set_new(root, "serialNumber", json_string_t(m_serialNumber));
   json_object_set_new(root, "radioInterfaces", json_struct_array(m_radioInterfaces));
   json_object_set_new(root, "state", json_integer(m_apState));
   json_object_set_new(root, "prevState", json_integer(m_prevState));
   unlockProperties();

   return root;
}
