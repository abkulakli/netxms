/*
** NetXMS - Network Management System
** Copyright (C) 2003-2021 Victor Kirhenshtein
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
** File: dcobject.cpp
**
**/

#include "nxcore.h"

/**
 * Default retention time for collected data
 */
int __EXPORT DCObject::m_defaultRetentionTime = 30;

/**
 * Default data collection polling interval
 */
int __EXPORT DCObject::m_defaultPollingInterval = 60;

/**
 * Get storage class from retention time
 */
DCObjectStorageClass DCObject::storageClassFromRetentionTime(int retentionTime)
{
   if (retentionTime == 0)
      return DCObjectStorageClass::DEFAULT;
   if (retentionTime <= 7)
      return DCObjectStorageClass::BELOW_7;
   if (retentionTime <= 30)
      return DCObjectStorageClass::BELOW_30;
   if (retentionTime <= 90)
      return DCObjectStorageClass::BELOW_90;
   if (retentionTime <= 180)
      return DCObjectStorageClass::BELOW_180;
   return DCObjectStorageClass::OTHER;
}

/**
 * Get name of storage class
 */
const TCHAR *DCObject::getStorageClassName(DCObjectStorageClass storageClass)
{
   static const TCHAR *names[] = { _T("default"), _T("7"), _T("30"), _T("90"), _T("180"), _T("other") };
   return names[static_cast<int>(storageClass)];
}

/**
 * Default constructor for DCObject
 */
DCObject::DCObject(const shared_ptr<DataCollectionOwner>& owner) : m_owner(owner), m_mutex(MutexType::RECURSIVE)
{
   m_id = 0;
   m_guid = uuid::generate();
   m_ownerId = (owner != nullptr) ? owner->getId() : 0;
   m_dwTemplateId = 0;
   m_dwTemplateItemId = 0;
   m_busy = 0;
	m_scheduledForDeletion = 0;
	m_pollingScheduleType = DC_POLLING_SCHEDULE_DEFAULT;
   m_pollingInterval = 0;
   m_pollingIntervalSrc = nullptr;
   m_retentionType = DC_RETENTION_DEFAULT;
   m_retentionTime = 0;
   m_retentionTimeSrc = nullptr;
   m_source = DS_INTERNAL;
   m_status = ITEM_STATUS_NOT_SUPPORTED;
   m_lastPoll = 0;
   m_schedules = nullptr;
   m_tLastCheck = 0;
	m_flags = 0;
   m_dwErrorCount = 0;
	m_dwResourceId = 0;
	m_sourceNode = 0;
	m_pszPerfTabSettings = nullptr;
	m_snmpPort = 0;	// use default
	m_snmpVersion = SNMP_VERSION_DEFAULT;
   m_transformationScriptSource = nullptr;
   m_transformationScript = nullptr;
   m_lastScriptErrorReport = 0;
   m_comments = nullptr;
   m_doForcePoll = false;
   m_pollingSession = nullptr;
   m_instanceDiscoveryMethod = IDM_NONE;
   m_instanceFilterSource = nullptr;
   m_instanceFilter = nullptr;
   m_accessList = new IntegerArray<UINT32>(0, 16);
   m_instanceRetentionTime = -1;
   m_instanceGracePeriodStart = 0;
   m_startTime = 0;
   m_relatedObject = 0;
}

/**
 * Create DCObject from another DCObject
 */
DCObject::DCObject(const DCObject *src, bool shadowCopy) :
         m_owner(src->m_owner), m_name(src->m_name), m_description(src->m_description),
         m_systemTag(src->m_systemTag), m_instanceDiscoveryData(src->m_instanceDiscoveryData),
         m_instance(src->m_instance), m_mutex(MutexType::RECURSIVE)
{
   m_id = src->m_id;
   m_guid = src->m_guid;
   m_ownerId = src->m_ownerId;
   m_dwTemplateId = src->m_dwTemplateId;
   m_dwTemplateItemId = src->m_dwTemplateItemId;
   m_busy = shadowCopy ? src->m_busy : 0;
	m_scheduledForDeletion = 0;
	m_pollingScheduleType = src->m_pollingScheduleType;
   m_pollingInterval = src->m_pollingInterval;
   m_pollingIntervalSrc = MemCopyString(src->m_pollingIntervalSrc);
   m_retentionType = src->m_retentionType;
   m_retentionTime = src->m_retentionTime;
   m_retentionTimeSrc = MemCopyString(src->m_retentionTimeSrc);
   m_source = src->m_source;
   m_status = src->m_status;
   m_lastPoll = shadowCopy ? src->m_lastPoll : 0;
   m_tLastCheck = shadowCopy ? src->m_tLastCheck : 0;
   m_dwErrorCount = shadowCopy ? src->m_dwErrorCount : 0;
	m_flags = src->m_flags;
	m_dwResourceId = src->m_dwResourceId;
	m_sourceNode = src->m_sourceNode;
	m_pszPerfTabSettings = MemCopyString(src->m_pszPerfTabSettings);
	m_snmpPort = src->m_snmpPort;
   m_snmpVersion = src->m_snmpVersion;
	m_comments = MemCopyString(src->m_comments);
	m_doForcePoll = false;
	m_pollingSession = nullptr;

   m_transformationScriptSource = nullptr;
   m_transformationScript = nullptr;
   m_lastScriptErrorReport = 0;
   setTransformationScript(src->m_transformationScriptSource);

   m_schedules = (src->m_schedules != nullptr) ? new StringList(src->m_schedules) : nullptr;

   m_instanceDiscoveryMethod = src->m_instanceDiscoveryMethod;
   m_instanceFilterSource = nullptr;
   m_instanceFilter = nullptr;
   setInstanceFilter(src->m_instanceFilterSource);
   m_accessList = new IntegerArray<UINT32>(src->m_accessList);
   m_instanceRetentionTime = src->m_instanceRetentionTime;
   m_instanceGracePeriodStart = src->m_instanceGracePeriodStart;
   m_startTime = src->m_startTime;
   m_relatedObject = src->m_relatedObject;
}

/**
 * Constructor for creating new DCObject from scratch
 */
DCObject::DCObject(UINT32 id, const TCHAR *name, int source, const TCHAR *pollingInterval, const TCHAR *retentionTime,
         const shared_ptr<DataCollectionOwner>& owner, const TCHAR *description, const TCHAR *systemTag) :
         m_owner(owner), m_name(name), m_description(description), m_systemTag(systemTag),
         m_instanceDiscoveryData(_T("")), m_instance(_T("")), m_mutex(MutexType::RECURSIVE)
{
   m_id = id;
   m_guid = uuid::generate();
   m_ownerId = (owner != nullptr) ? owner->getId() : 0;
   m_dwTemplateId = 0;
   m_dwTemplateItemId = 0;
   m_source = source;
   m_pollingScheduleType = (pollingInterval != nullptr) ? DC_POLLING_SCHEDULE_CUSTOM : DC_POLLING_SCHEDULE_DEFAULT;
   m_pollingIntervalSrc = MemCopyString(pollingInterval);
   m_retentionTime = (retentionTime != nullptr) ? DC_RETENTION_CUSTOM : DC_RETENTION_DEFAULT;
   m_retentionTimeSrc = MemCopyString(retentionTime);
   m_status = ITEM_STATUS_ACTIVE;
   m_busy = 0;
   m_scheduledForDeletion = 0;
   m_lastPoll = 0;
   m_flags = 0;
   m_schedules = nullptr;
   m_tLastCheck = 0;
   m_dwErrorCount = 0;
   m_dwResourceId = 0;
   m_sourceNode = 0;
   m_pszPerfTabSettings = nullptr;
   m_snmpPort = 0;	// use default
   m_snmpVersion = SNMP_VERSION_DEFAULT;
   m_transformationScriptSource = nullptr;
   m_transformationScript = nullptr;
   m_lastScriptErrorReport = 0;
   m_comments = nullptr;
   m_doForcePoll = false;
   m_pollingSession = nullptr;
   m_instanceDiscoveryMethod = IDM_NONE;
   m_instanceFilterSource = nullptr;
   m_instanceFilter = nullptr;
   m_accessList = new IntegerArray<UINT32>(0, 16);
   m_instanceRetentionTime = -1;
   m_instanceGracePeriodStart = 0;
   m_startTime = 0;
   m_relatedObject = 0;

   updateTimeIntervalsInternal();
}

/**
 * Create DCObject from import file
 */
DCObject::DCObject(ConfigEntry *config, const shared_ptr<DataCollectionOwner>& owner) : m_owner(owner), m_mutex(MutexType::RECURSIVE)
{
   m_id = CreateUniqueId(IDG_ITEM);
   m_guid = config->getSubEntryValueAsUUID(_T("guid"));
   if (m_guid.isNull())
      m_guid = uuid::generate();
   m_ownerId = (owner != nullptr) ? owner->getId() : 0;
   m_dwTemplateId = 0;
   m_dwTemplateItemId = 0;
   m_name = config->getSubEntryValue(_T("name"), 0, _T("unnamed"));
   m_description = config->getSubEntryValue(_T("description"), 0, m_name);
   m_systemTag = config->getSubEntryValue(_T("systemTag"), 0, nullptr);
   m_source = (BYTE)config->getSubEntryValueAsInt(_T("origin"));
   m_flags = config->getSubEntryValueAsInt(_T("flags"));
   m_pollingIntervalSrc = MemCopyString(config->getSubEntryValue(_T("interval"), 0, _T("0")));
   if (config->findEntry(_T("scheduleType")) != nullptr)
   {
      m_pollingScheduleType = config->getSubEntryValueAsInt(_T("scheduleType"));
   }
   else
   {
      m_pollingScheduleType = !_tcscmp(m_pollingIntervalSrc, _T("")) || !_tcscmp(m_pollingIntervalSrc, _T("0")) ? DC_POLLING_SCHEDULE_DEFAULT : DC_POLLING_SCHEDULE_CUSTOM;
      if (m_flags & 1)  // for compatibility with old format
         m_pollingScheduleType = DC_POLLING_SCHEDULE_ADVANCED;
   }
   m_retentionTimeSrc = MemCopyString(config->getSubEntryValue(_T("retention"), 0, _T("0")));
   if (config->findEntry(_T("retentionType")) != nullptr)
   {
      m_retentionType = config->getSubEntryValueAsInt(_T("retentionType"));
   }
   else
   {
      m_retentionType = !_tcscmp(m_retentionTimeSrc, _T("")) || !_tcscmp(m_retentionTimeSrc, _T("0")) ? DC_RETENTION_DEFAULT : DC_RETENTION_CUSTOM;
      if (m_flags & 0x200) // for compatibility with old format
         m_retentionType = DC_RETENTION_NONE;
   }
   if (config->getSubEntryValueAsBoolean(_T("isDisabled")))
      m_status = ITEM_STATUS_DISABLED;
   else
      m_status = ITEM_STATUS_ACTIVE;
   m_busy = 0;
   m_scheduledForDeletion = 0;
   m_lastPoll = 0;
   m_tLastCheck = 0;
   m_dwErrorCount = 0;
   m_dwResourceId = 0;
   m_sourceNode = 0;
   const TCHAR *perfTabSettings = config->getSubEntryValue(_T("perfTabSettings"));
   m_pszPerfTabSettings = MemCopyString(perfTabSettings);
   m_snmpPort = static_cast<UINT16>(config->getSubEntryValueAsInt(_T("snmpPort")));
   m_snmpVersion = static_cast<SNMP_Version>(config->getSubEntryValueAsInt(_T("snmpVersion"), 0, SNMP_VERSION_DEFAULT));
   m_schedules = nullptr;

   m_transformationScriptSource = nullptr;
   m_transformationScript = nullptr;
   m_lastScriptErrorReport = 0;
   m_comments = MemCopyString(config->getSubEntryValue(_T("comments")));
   m_doForcePoll = false;
   m_pollingSession = nullptr;
   setTransformationScript(config->getSubEntryValue(_T("transformation")));

   // for compatibility with old format
   if (config->getSubEntryValueAsInt(_T("advancedSchedule")))
      m_pollingScheduleType = DC_POLLING_SCHEDULE_ADVANCED;

   ConfigEntry *schedules = config->findEntry(_T("schedules"));
   if (schedules != nullptr)
      schedules = schedules->findEntry(_T("schedule"));
   if ((schedules != nullptr) && (schedules->getValueCount() > 0))
   {
      m_schedules = new StringList();
      int count = schedules->getValueCount();
      for(int i = 0; i < count; i++)
      {
         m_schedules->add(schedules->getValue(i));
      }
   }

   m_instanceDiscoveryMethod = (WORD)config->getSubEntryValueAsInt(_T("instanceDiscoveryMethod"));
   m_instanceDiscoveryData = config->getSubEntryValue(_T("instanceDiscoveryData"));
   m_instanceFilterSource = nullptr;
   m_instanceFilter = nullptr;
   setInstanceFilter(config->getSubEntryValue(_T("instanceFilter")));
   m_instance = config->getSubEntryValue(_T("instance"));
   m_accessList = new IntegerArray<UINT32>(0, 16);
   m_instanceRetentionTime = config->getSubEntryValueAsInt(_T("instanceRetentionTime"), 0, -1);
   m_instanceGracePeriodStart = 0;
   m_startTime = 0;
   m_relatedObject = 0;

   updateTimeIntervalsInternal();
}

/**
 * Destructor
 */
DCObject::~DCObject()
{
   MemFree(m_retentionTimeSrc);
   MemFree(m_pollingIntervalSrc);
   MemFree(m_transformationScriptSource);
   delete m_transformationScript;
   delete m_schedules;
   MemFree(m_pszPerfTabSettings);
   MemFree(m_comments);
   MemFree(m_instanceFilterSource);
   delete m_instanceFilter;
   delete m_accessList;
}

/**
 * Load access list
 */
bool DCObject::loadAccessList(DB_HANDLE hdb)
{
   m_accessList->clear();

   DB_STATEMENT hStmt = DBPrepare(hdb, _T("SELECT user_id FROM dci_access WHERE dci_id=?"));
   if (hStmt == nullptr)
      return false;

   DBBind(hStmt, 1, DB_SQLTYPE_INTEGER, m_id);
   DB_RESULT hResult = DBSelectPrepared(hStmt);
   if (hResult != nullptr)
   {
      int count = DBGetNumRows(hResult);
      for(int i = 0; i < count; i++)
      {
         m_accessList->add(DBGetFieldULong(hResult, i, 0));
      }
      DBFreeResult(hResult);
   }
   DBFreeStatement(hStmt);
   return hResult != nullptr;
}

/**
 * Load custom schedules from database
 * (assumes that no schedules was created before this call)
 */
bool DCObject::loadCustomSchedules(DB_HANDLE hdb)
{
   if (m_pollingScheduleType != DC_POLLING_SCHEDULE_ADVANCED)
		return true;

   DB_STATEMENT hStmt = DBPrepare(hdb, _T("SELECT schedule FROM dci_schedules WHERE item_id=?"));
   if (hStmt == nullptr)
      return false;

   DBBind(hStmt, 1, DB_SQLTYPE_INTEGER, m_id);
   DB_RESULT hResult = DBSelectPrepared(hStmt);
   if (hResult != nullptr)
   {
      int count = DBGetNumRows(hResult);
      if (count > 0)
      {
         m_schedules = new StringList();
         for(int i = 0; i < count; i++)
         {
            m_schedules->addPreallocated(DBGetField(hResult, i, 0, nullptr, 0));
         }
      }
      DBFreeResult(hResult);
   }
   DBFreeStatement(hStmt);
	return hResult != nullptr;
}

/**
 * Check if associated cluster resource is active. Returns true also if
 * DCI has no resource association
 */
bool DCObject::matchClusterResource()
{
   auto owner = m_owner.lock();
   if ((m_dwResourceId == 0) || (owner->getObjectClass() != OBJECT_NODE))
		return true;

	shared_ptr<Cluster> cluster = static_cast<Node*>(owner.get())->getMyCluster();
	if (cluster == nullptr)
		return false;	// Has association, but cluster object cannot be found

	return cluster->isResourceOnNode(m_dwResourceId, m_ownerId);
}

/**
 * Expand macros in text
 */
StringBuffer DCObject::expandMacros(const TCHAR *src, size_t dstLen)
{
	ssize_t index = 0;
	StringBuffer dst = src;
	while((index = dst.find(_T("%{"), index)) != String::npos)
	{
		String head = dst.substring(0, index);
		ssize_t index2 = dst.find(_T("}"), index);
		if (index2 == String::npos)
			break;	// Missing closing }

		String rest = dst.substring(index2 + 1, -1);
		StringBuffer macro = dst.substring(index + 2, index2 - index - 2);
		macro.trim();

		dst = head;
		if (!_tcscmp(macro, _T("node_id")))
		{
			if (m_ownerId != 0)
			{
				dst.append(m_ownerId);
			}
			else
			{
            dst.append(_T("(error)"));
			}
		}
		else if (!_tcscmp(macro, _T("node_name")))
		{
         auto owner = m_owner.lock();
			if (owner != nullptr)
			{
				dst.append(owner->getName());
			}
			else
			{
				dst.append(_T("(error)"));
			}
		}
		else if (!_tcscmp(macro, _T("node_primary_ip")))
		{
		   auto owner = m_owner.lock();
			if ((owner != nullptr) && (owner->getObjectClass() == OBJECT_NODE))
			{
				TCHAR ipAddr[64];
				dst.append(static_cast<Node*>(owner.get())->getIpAddress().toString(ipAddr));
			}
			else
			{
            dst.append(_T("(error)"));
			}
		}
		else if (!_tcsncmp(macro, _T("script:"), 7))
		{
			NXSL_VM *vm = CreateServerScriptVM(&macro[7], m_owner.lock(), createDescriptorInternal());
			if (vm != nullptr)
			{
				if (vm->run(0, nullptr))
				{
					NXSL_Value *result = vm->getResult();
					if (result != nullptr)
						dst.append(result->getValueAsCString());
		         DbgPrintf(4, _T("DCObject::expandMacros(%d,\"%s\"): Script %s executed successfully"), m_id, src, &macro[7]);
				}
				else
				{
		         DbgPrintf(4, _T("DCObject::expandMacros(%d,\"%s\"): Script %s execution error: %s"),
					          m_id, src, &macro[7], vm->getErrorText());
					PostSystemEvent(EVENT_SCRIPT_ERROR, g_dwMgmtNode, "ssd", &macro[7], vm->getErrorText(), m_id);
				}
            delete vm;
			}
			else
			{
	         DbgPrintf(4, _T("DCObject::expandMacros(%d,\"%s\"): Cannot find script %s"), m_id, src, &macro[7]);
			}
		}
		dst.append(rest);
	}
	if (dst.length() > dstLen)
	   dst.shrink(dst.length() - dstLen);
	return dst;
}

/**
 * Delete all collected data
 */
bool DCObject::deleteAllData()
{
	return false;
}

/**
 * Delete single DCI entry
 */
bool DCObject::deleteEntry(time_t timestamp)
{
   return false;
}

/**
 * Set new ID and node/template association
 */
void DCObject::changeBinding(UINT32 newId, shared_ptr<DataCollectionOwner> newOwner, bool doMacroExpansion)
{
   lock();
   m_owner = newOwner;
   m_ownerId = (newOwner != nullptr) ? newOwner->getId() : 0;
	if (newId != 0)
	{
		m_id = newId;
		m_guid = uuid::generate();
	}

	if (doMacroExpansion)
	{
		m_name = expandMacros(m_name, MAX_ITEM_NAME);
		m_description = expandMacros(m_description, MAX_DB_STRING);
		m_instance = expandMacros(m_instance, MAX_ITEM_NAME);
	}

   updateTimeIntervalsInternal();

   unlock();
}

/**
 * Set DCI status
 */
void DCObject::setStatus(int status, bool generateEvent)
{
   auto owner = m_owner.lock();
   if ((owner != nullptr) && (m_status != (BYTE)status))
   {
      NotifyClientsOnDCIStatusChange(*owner, getId(), status);
      if (generateEvent && IsEventSource(owner->getObjectClass()))
      {
         static UINT32 eventCode[3] = { EVENT_DCI_ACTIVE, EVENT_DCI_DISABLED, EVENT_DCI_UNSUPPORTED };
         static const TCHAR *originName[11] =
            {
               _T("Internal"), _T("NetXMS Agent"), _T("SNMP"),
               _T("CheckPoint SNMP"), _T("Push"), _T("WinPerf"),
               _T("iLO"), _T("Script"), _T("SSH"), _T("MQTT"),
               _T("Device Driver")
            };
         PostSystemEvent(eventCode[status], owner->getId(), "dssds", m_id, m_name.cstr(), m_description.cstr(), m_source, originName[m_source]);
      }
   }
   m_status = (BYTE)status;
}

/**
 * Update polling schedule type
 */
void DCObject::setPollingIntervalType(BYTE pollingScheduleType)
{
   lock();
   m_pollingScheduleType = pollingScheduleType;
   if (m_pollingScheduleType != DC_POLLING_SCHEDULE_CUSTOM)
      MemFreeAndNull(m_pollingIntervalSrc);
   unlock();
}

/**
 * Update polling interval
 */
void DCObject::setPollingInterval(const TCHAR *schedule)
{
   lock();
   MemFree(m_pollingIntervalSrc);
   m_pollingIntervalSrc = MemCopyString(schedule);
   updateTimeIntervalsInternal();
   unlock();

}

/**
 * Update retention type
 */
void DCObject::setRetentionType(BYTE retentionType)
{
   lock();
   m_retentionType = retentionType;
   if (m_retentionType != DC_RETENTION_CUSTOM)
      MemFreeAndNull(m_retentionTimeSrc);
   unlock();
}

/**
 * Update retention time
 */
void DCObject::setRetention(const TCHAR *retention)
{
   lock();
   MemFree(m_retentionTimeSrc);
   m_retentionTimeSrc = MemCopyString(retention);
   updateTimeIntervalsInternal();
   unlock();

}

/**
 * Expand custom schedule string
 */
String DCObject::expandSchedule(const TCHAR *schedule)
{
   StringBuffer expandedSchedule;
   if (_tcslen(schedule) > 4 && !_tcsncmp(schedule, _T("%["), 2))
   {
      TCHAR *scriptName = _tcsdup(schedule + 2);
      if (scriptName != nullptr)
      {
         TCHAR *closingBracker = _tcschr(scriptName, _T(']'));
         if (closingBracker != nullptr)
         {
            *closingBracker = 0;

            NXSL_VM *vm = CreateServerScriptVM(scriptName, m_owner.lock(), createDescriptorInternal());
            if (vm != nullptr)
            {
               if (vm->run(0, nullptr))
               {
                  NXSL_Value *result = vm->getResult();
                  if (result != nullptr)
                  {
                     const TCHAR *temp = result->getValueAsCString();
                     if (temp != nullptr)
                     {
                        DbgPrintf(7, _T("DCObject::expandSchedule(%%[%s]) expanded to \"%s\""), scriptName, temp);
                        expandedSchedule = StringBuffer(temp);
                     }
                  }
               }
               else
               {
                  DbgPrintf(4, _T("DCObject::expandSchedule(%%[%s]) script execution failed (%s)"), scriptName, vm->getErrorText());
               }
               delete vm;
            }
         }
         else
         {
            DbgPrintf(4, _T("DCObject::expandSchedule: invalid script schedule syntax in %d [%s]"), m_id, m_name.cstr());
         }
         free(scriptName);
      }
      else
      {
         DbgPrintf(4, _T("DCObject::expandSchedule: invalid script schedule syntax in %d [%s]"), m_id, m_name.cstr());
      }
   }
   else
   {
      expandedSchedule = StringBuffer(schedule);
   }
   return expandedSchedule;
}

/**
 * Check if data collection object have to be polled
 */
bool DCObject::isReadyForPolling(time_t currTime)
{
   // Normally data collection object will be locked when it is being
   // changed or when it is processing new data
   // In both cases there is no point to block item poller and wait, it
   // is more effective to try schedule on next run
   if (!tryLock())
   {
      nxlog_debug_tag(_T("dc.scheduler"), 5, _T("DCObject::isReadyForPolling: cannot obtain lock for data collection object [%u]"), m_id);
      return false;
   }

   if (m_doForcePoll && !m_busy)
   {
      if ((m_status != ITEM_STATUS_DISABLED) &&
          isCacheLoaded() && (m_source != DS_PUSH_AGENT) &&
          matchClusterResource() && hasValue()) // Ignore agent cache mode for forced polls and always request data as if cache is off
      {
         unlock();
         return true;
      }
      else
      {
         // DCI cannot be force polled at the moment, clear force poll request
         nxlog_debug_tag(_T("dc.scheduler"), 6, _T("Forced poll of DC object %s [%u] on node %s [%u] cancelled"), m_name.cstr(), m_id, getOwnerName(), m_ownerId);
         if (m_pollingSession != nullptr)
         {
            m_pollingSession->decRefCount();
            m_pollingSession = nullptr;
         }
         m_doForcePoll = false;
         unlock();
         return false;
      }
   }

   bool result;
   if ((m_status != ITEM_STATUS_DISABLED) && (!m_busy) &&
       isCacheLoaded() && (m_source != DS_PUSH_AGENT) &&
       matchClusterResource() && hasValue() && (getAgentCacheMode() == AGENT_CACHE_OFF))
   {
      if (m_pollingScheduleType == DC_POLLING_SCHEDULE_ADVANCED)
      {
         if (m_schedules != nullptr)
         {
            struct tm tmCurrLocal, tmLastLocal;
#if HAVE_LOCALTIME_R
            localtime_r(&currTime, &tmCurrLocal);
            localtime_r(&m_tLastCheck, &tmLastLocal);
#else
            memcpy(&tmCurrLocal, localtime(&currTime), sizeof(struct tm));
            memcpy(&tmLastLocal, localtime(&m_tLastCheck), sizeof(struct tm));
#endif
            result = false;
            for(int i = 0; i < m_schedules->size(); i++)
            {
               bool withSeconds = false;

               String schedule = expandSchedule(m_schedules->get(i));
               if (MatchSchedule(schedule, &withSeconds, &tmCurrLocal, currTime))
               {
                  // TODO: do we have to take care about the schedules with seconds
                  // that trigger polling too often?
                  if (withSeconds || (currTime - m_tLastCheck >= 60) || (tmCurrLocal.tm_min != tmLastLocal.tm_min))
                  {
                     result = true;
                     break;
                  }
               }
            }
         }
         else
         {
            result = false;
         }
         m_tLastCheck = currTime;
      }
      else
      {
			if (m_status == ITEM_STATUS_NOT_SUPPORTED)
		      result = ((m_lastPoll + getEffectivePollingInterval() * 10 <= currTime) && (m_startTime <= currTime));
			else
		      result = ((m_lastPoll + getEffectivePollingInterval() <= currTime) && (m_startTime <= currTime));
      }
   }
   else
   {
      result = false;
   }
   unlock();
   return result;
}

/**
 * Returns true if internal cache is loaded. If data collection object
 * does not have cache should return true
 */
bool DCObject::isCacheLoaded()
{
	return true;
}

/**
 * Prepare object for deletion
 */
bool DCObject::prepareForDeletion()
{
	DbgPrintf(9, _T("DCObject::prepareForDeletion for DCO %d"), m_id);

	lock();
   m_status = ITEM_STATUS_DISABLED;   // Prevent future polls
	m_scheduledForDeletion = 1;
	bool canDelete = (m_busy ? false : true);
   unlock();
	DbgPrintf(9, _T("DCObject::prepareForDeletion: completed for DCO %d, canDelete=%d"), m_id, (int)canDelete);

	return canDelete;
}

/**
 * Create NXCP message with object data
 */
void DCObject::createMessage(NXCPMessage *pMsg)
{
	lock();
   pMsg->setField(VID_DCI_ID, m_id);
	pMsg->setField(VID_DCOBJECT_TYPE, (WORD)getType());
   pMsg->setField(VID_TEMPLATE_ID, m_dwTemplateId);
   pMsg->setField(VID_NAME, m_name);
   pMsg->setField(VID_DESCRIPTION, m_description);
   pMsg->setField(VID_TRANSFORMATION_SCRIPT, CHECK_NULL_EX(m_transformationScriptSource));
   pMsg->setField(VID_FLAGS, m_flags);
   pMsg->setField(VID_SYSTEM_TAG, m_systemTag);
   pMsg->setField(VID_POLLING_SCHEDULE_TYPE, static_cast<INT16>(m_pollingScheduleType));
   pMsg->setField(VID_POLLING_INTERVAL, m_pollingIntervalSrc);
   pMsg->setField(VID_RETENTION_TYPE, static_cast<INT16>(m_retentionType));
   pMsg->setField(VID_RETENTION_TIME, m_retentionTimeSrc);
   pMsg->setField(VID_DCI_SOURCE_TYPE, (WORD)m_source);
   pMsg->setField(VID_DCI_STATUS, (WORD)m_status);
	pMsg->setField(VID_RESOURCE_ID, m_dwResourceId);
	pMsg->setField(VID_AGENT_PROXY, m_sourceNode);
	pMsg->setField(VID_SNMP_PORT, m_snmpPort);
   pMsg->setField(VID_SNMP_VERSION, static_cast<INT16>(m_snmpVersion));
   pMsg->setField(VID_COMMENTS, m_comments);
   pMsg->setField(VID_PERFTAB_SETTINGS, m_pszPerfTabSettings);
	if (m_schedules != nullptr)
	{
      pMsg->setField(VID_NUM_SCHEDULES, static_cast<uint32_t>(m_schedules->size()));
      uint32_t fieldId = VID_DCI_SCHEDULE_BASE;
      for(int i = 0; i < m_schedules->size(); i++, fieldId++)
         pMsg->setField(fieldId, m_schedules->get(i));
	}
	else
	{
      pMsg->setField(VID_NUM_SCHEDULES, static_cast<uint32_t>(0));
	}
   pMsg->setField(VID_INSTD_METHOD, m_instanceDiscoveryMethod);
   pMsg->setField(VID_INSTD_DATA, m_instanceDiscoveryData);
   pMsg->setField(VID_INSTD_FILTER, m_instanceFilterSource);
   pMsg->setField(VID_INSTANCE, m_instance);
   pMsg->setFieldFromInt32Array(VID_ACL, m_accessList);
   pMsg->setField(VID_INSTANCE_RETENTION, m_instanceRetentionTime);
   pMsg->setField(VID_RELATED_OBJECT, m_relatedObject);
   unlock();
}

/**
 * Update data collection object from NXCP message
 */
void DCObject::updateFromMessage(const NXCPMessage& msg)
{
   lock();

   m_name = msg.getFieldAsSharedString(VID_NAME, MAX_ITEM_NAME);
   m_description = msg.getFieldAsSharedString(VID_DESCRIPTION, MAX_DB_STRING);
   m_systemTag = msg.getFieldAsSharedString(VID_SYSTEM_TAG, MAX_DB_STRING);
	m_flags = msg.getFieldAsUInt32(VID_FLAGS);
   m_source = (BYTE)msg.getFieldAsUInt16(VID_DCI_SOURCE_TYPE);
   setStatus(msg.getFieldAsUInt16(VID_DCI_STATUS), true);
	m_dwResourceId = msg.getFieldAsUInt32(VID_RESOURCE_ID);
	m_sourceNode = msg.getFieldAsUInt32(VID_AGENT_PROXY);
   m_snmpPort = msg.getFieldAsUInt16(VID_SNMP_PORT);
   m_snmpVersion = msg.isFieldExist(VID_SNMP_VERSION) ? static_cast<SNMP_Version>(msg.getFieldAsInt16(VID_SNMP_VERSION)) : SNMP_VERSION_DEFAULT;
	msg.getFieldAsString(VID_PERFTAB_SETTINGS, &m_pszPerfTabSettings);
   msg.getFieldAsString(VID_COMMENTS, &m_comments);

   m_pollingScheduleType = static_cast<BYTE>(msg.getFieldAsUInt16(VID_POLLING_SCHEDULE_TYPE));
   if (m_pollingScheduleType == DC_POLLING_SCHEDULE_CUSTOM)
      msg.getFieldAsString(VID_POLLING_INTERVAL, &m_pollingIntervalSrc);
   else
      MemFreeAndNull(m_pollingIntervalSrc);
   m_retentionType = static_cast<BYTE>(msg.getFieldAsUInt16(VID_RETENTION_TYPE));
   if (m_retentionType == DC_RETENTION_CUSTOM)
      msg.getFieldAsString(VID_RETENTION_TIME, &m_retentionTimeSrc);
   else
      MemFreeAndNull(m_retentionTimeSrc);
   updateTimeIntervalsInternal();

   TCHAR *pszStr = msg.getFieldAsString(VID_TRANSFORMATION_SCRIPT);
   setTransformationScript(pszStr);
   MemFree(pszStr);

   // Update schedules
   int count = msg.getFieldAsInt32(VID_NUM_SCHEDULES);
   if (count > 0)
   {
      if (m_schedules != nullptr)
         m_schedules->clear();
      else
         m_schedules = new StringList();

      UINT32 fieldId = VID_DCI_SCHEDULE_BASE;
      for(int i = 0; i < count; i++, fieldId++)
      {
         TCHAR *s = msg.getFieldAsString(fieldId);
         if (s != nullptr)
         {
            m_schedules->addPreallocated(s);
         }
      }
   }
   else
   {
      delete_and_null(m_schedules);
   }

   m_instanceDiscoveryMethod = msg.getFieldAsUInt16(VID_INSTD_METHOD);
   m_instanceDiscoveryData = msg.getFieldAsSharedString(VID_INSTD_DATA, MAX_INSTANCE_LEN);

   pszStr = msg.getFieldAsString(VID_INSTD_FILTER);
   setInstanceFilter(pszStr);
   MemFree(pszStr);

   m_instance = msg.getFieldAsSharedString(VID_INSTANCE, MAX_INSTANCE_LEN);

   m_accessList->clear();
   msg.getFieldAsInt32Array(VID_ACL, m_accessList);

   m_instanceRetentionTime = msg.getFieldAsInt32(VID_INSTANCE_RETENTION);
   m_relatedObject = msg.getFieldAsUInt32(VID_RELATED_OBJECT);

	unlock();
}

/**
 * Save to database
 */
bool DCObject::saveToDatabase(DB_HANDLE hdb)
{
	lock();

   // Save schedules
   bool success = ExecuteQueryOnObject(hdb, m_id, _T("DELETE FROM dci_schedules WHERE item_id=?"));
	if (success && (m_schedules != nullptr) && !m_schedules->isEmpty())
   {
	   DB_STATEMENT hStmt = DBPrepare(hdb, _T("INSERT INTO dci_schedules (item_id,schedule_id,schedule) VALUES (?,?,?)"));
	   if (hStmt != nullptr)
	   {
	      DBBind(hStmt, 1, DB_SQLTYPE_INTEGER, m_id);
         for(int i = 0; (i < m_schedules->size()) && success; i++)
         {
            DBBind(hStmt, 2, DB_SQLTYPE_INTEGER, i + 1);
            DBBind(hStmt, 3, DB_SQLTYPE_VARCHAR, m_schedules->get(i), DB_BIND_STATIC);
            success = DBExecute(hStmt);
         }
         DBFreeStatement(hStmt);
	   }
	   else
	   {
	      success = false;
	   }
   }

   // Save access list
	if (success)
	{
	   success = ExecuteQueryOnObject(hdb, m_id, _T("DELETE FROM dci_access WHERE dci_id=?"));
	}
   if (success && !m_accessList->isEmpty())
   {
      DB_STATEMENT hStmt = DBPrepare(hdb, _T("INSERT INTO dci_access (dci_id,user_id) VALUES (?,?)"));
      if (hStmt != nullptr)
      {
         DBBind(hStmt, 1, DB_SQLTYPE_INTEGER, m_id);
         for(int i = 0; (i < m_accessList->size()) && success; i++)
         {
            DBBind(hStmt, 2, DB_SQLTYPE_INTEGER, m_accessList->get(i));
            success = DBExecute(hStmt);
         }
         DBFreeStatement(hStmt);
      }
      else
      {
         success = false;
      }
   }

	unlock();

	return success;
}

/**
 * Terminate alarms related to DCI
 */
static void TerminateRelatedAlarms(void *arg)
{
   ResolveAlarmByDCObjectId(CAST_FROM_POINTER(arg, UINT32), true);
}

/**
 * Delete object and collected data from database
 */
void DCObject::deleteFromDatabase()
{
	TCHAR query[256];
   _sntprintf(query, sizeof(query) / sizeof(TCHAR), _T("DELETE FROM dci_schedules WHERE item_id=%d"), (int)m_id);
   QueueSQLRequest(query);

   _sntprintf(query, sizeof(query) / sizeof(TCHAR), _T("DELETE FROM dci_access WHERE dci_id=%d"), (int)m_id);
   QueueSQLRequest(query);

   if (ConfigReadBoolean(_T("DataCollection.OnDCIDelete.TerminateRelatedAlarms"), true))
      ThreadPoolExecuteSerialized(g_mainThreadPool, _T("TerminateDataCollectionAlarms"), TerminateRelatedAlarms, CAST_TO_POINTER(m_id, void*));
}

/**
 * Load data collection object thresholds from database
 */
bool DCObject::loadThresholdsFromDB(DB_HANDLE hdb)
{
	return true;
}

/**
 * Expand {instance} macro in name and description
 */
void DCObject::expandInstance()
{
   StringBuffer temp = m_name;
   temp.replace(_T("{instance}"), m_instanceDiscoveryData);
   temp.replace(_T("{instance-name}"), m_instance);
   m_name = temp;

   temp = m_description;
   temp.replace(_T("{instance}"), m_instanceDiscoveryData);
   temp.replace(_T("{instance-name}"), m_instance);
   m_description = temp;
}

/**
 * Update DC object from template object
 */
void DCObject::updateFromTemplate(DCObject *src)
{
	lock();

	m_name = expandMacros(src->m_name, MAX_ITEM_NAME);
	m_description = expandMacros(src->m_description, MAX_DB_STRING);
	m_systemTag = expandMacros(src->m_systemTag, MAX_DB_STRING);

	m_pollingScheduleType = src->m_pollingScheduleType;
	MemFree(m_pollingIntervalSrc);
   m_pollingIntervalSrc = MemCopyString(src->m_pollingIntervalSrc);
   m_retentionType = src->m_retentionType;
   MemFree(m_retentionTimeSrc);
   m_retentionTimeSrc = MemCopyString(src->m_retentionTimeSrc);
   updateTimeIntervalsInternal();

   m_source = src->m_source;
	m_flags = src->m_flags;
	m_sourceNode = src->m_sourceNode;
	m_dwResourceId = src->m_dwResourceId;
	m_snmpPort = src->m_snmpPort;
   m_snmpVersion = src->m_snmpVersion;

	MemFree(m_pszPerfTabSettings);
	m_pszPerfTabSettings = MemCopyString(src->m_pszPerfTabSettings);

   setTransformationScript(src->m_transformationScriptSource);

   delete m_accessList;
   m_accessList = new IntegerArray<UINT32>(src->m_accessList);

   // Copy schedules
   delete m_schedules;
   m_schedules = (src->m_schedules != nullptr) ? new StringList(src->m_schedules) : nullptr;

   if ((src->getInstanceDiscoveryMethod() != IDM_NONE) && (m_instanceDiscoveryMethod == IDM_NONE))
   {
      expandInstance();
   }
   else
   {
      m_instance = expandMacros(src->m_instance, MAX_ITEM_NAME);
      m_instanceDiscoveryMethod = src->m_instanceDiscoveryMethod;
      m_instanceDiscoveryData = src->m_instanceDiscoveryData;
      MemFreeAndNull(m_instanceFilterSource);
      delete_and_null(m_instanceFilter);
      setInstanceFilter(src->m_instanceFilterSource);
   }
   m_instanceRetentionTime = src->m_instanceRetentionTime;

   if (((m_status != ITEM_STATUS_DISABLED) || (g_flags & AF_APPLY_TO_DISABLED_DCI_FROM_TEMPLATE)) &&
       !((m_ownerId == m_dwTemplateId) && (m_instanceGracePeriodStart > 0))) // check if DCI is not disabled by instance discovery
   {
      setStatus(src->m_status, true);
   }

   unlock();
}

/**
 * Process new data collection error
 */
void DCObject::processNewError(bool noInstance)
{
   time_t now = time(nullptr);
   processNewError(noInstance, now);
}

/**
 * Process new data collection error
 */
void DCObject::processNewError(bool noInstance, time_t now)
{
}

/**
 * Save information about threshold state before maintenance
 */
void DCObject::updateThresholdsBeforeMaintenanceState()
{
}

/**
 * Generate events that persist after maintenance
 */
void DCObject::generateEventsBasedOnThrDiff()
{
}

/**
 * Should return true if object has (or can have) value
 */
bool DCObject::hasValue()
{
   auto owner = m_owner.lock();
   if ((owner != nullptr) && (owner->getObjectClass() == OBJECT_CLUSTER))
      return isAggregateOnCluster() && (m_instanceDiscoveryMethod == IDM_NONE);
   return m_instanceDiscoveryMethod == IDM_NONE;
}

/**
 * Set new transformation script
 */
void DCObject::setTransformationScript(const TCHAR *source)
{
   free(m_transformationScriptSource);
   delete m_transformationScript;
   if (source != nullptr)
   {
      m_transformationScriptSource = Trim(MemCopyString(source));
      if (m_transformationScriptSource[0] != 0)
      {
         TCHAR errorText[1024];
         m_transformationScript = NXSLCompile(m_transformationScriptSource, errorText, 1024, nullptr);
         if (m_transformationScript == nullptr)
         {
            TCHAR buffer[1024];
            _sntprintf(buffer, 1024, _T("DCI::%s::%d::TransformationScript"), getOwnerName(), m_id);
            PostSystemEvent(EVENT_SCRIPT_ERROR, g_dwMgmtNode, "ssd", buffer, errorText, m_id);
            nxlog_write(NXLOG_WARNING, _T("Failed to compile transformation script for object %s [%u] DCI %s [%u] (%s)"),
                     getOwnerName(), getOwnerId(), m_name.cstr(), m_id, errorText);
         }
      }
      else
      {
         m_transformationScript = nullptr;
      }
   }
   else
   {
      m_transformationScriptSource = nullptr;
      m_transformationScript = nullptr;
   }
   m_lastScriptErrorReport = 0;  // allow immediate error report after script change
}

/**
 * Get actual agent cache mode
 */
INT16 DCObject::getAgentCacheMode()
{
   if ((m_source != DS_NATIVE_AGENT) && (m_source != DS_SNMP_AGENT))
      return AGENT_CACHE_OFF;

   shared_ptr<Node> node;
   if (m_sourceNode != 0)
   {
      node = static_pointer_cast<Node>(FindObjectById(m_sourceNode, OBJECT_NODE));
   }
   else
   {
      auto owner = m_owner.lock();
      if (owner->getObjectClass() == OBJECT_NODE)
      {
         node = static_pointer_cast<Node>(owner);
      }
      else if (owner->getObjectClass() == OBJECT_CHASSIS)
      {
         node = static_pointer_cast<Node>(FindObjectById(static_cast<Chassis*>(owner.get())->getControllerId(), OBJECT_NODE));
      }
   }
   if (node == nullptr)
      return AGENT_CACHE_OFF;

   if ((m_source == DS_SNMP_AGENT) && (node->getEffectiveSnmpProxy() == 0))
      return AGENT_CACHE_OFF;

   uint32_t mode = DCF_GET_CACHE_MODE(m_flags);
   if (mode != AGENT_CACHE_DEFAULT)
      return mode;
   return node->getAgentCacheMode();
}

/**
 * Update data collection object from import file
 */
void DCObject::updateFromImport(ConfigEntry *config)
{
   lock();

   m_name = config->getSubEntryValue(_T("name"), 0, _T("unnamed"));
   m_description = config->getSubEntryValue(_T("description"), 0, m_name);
   m_systemTag = config->getSubEntryValue(_T("systemTag"), 0, nullptr);
   m_source = (BYTE)config->getSubEntryValueAsInt(_T("origin"));
   m_flags = config->getSubEntryValueAsInt(_T("flags"));
   const TCHAR *perfTabSettings = config->getSubEntryValue(_T("perfTabSettings"));
   MemFree(m_pszPerfTabSettings);
   m_pszPerfTabSettings = MemCopyString(perfTabSettings);
   m_snmpPort = static_cast<UINT16>(config->getSubEntryValueAsInt(_T("snmpPort")));
   m_snmpVersion = static_cast<SNMP_Version>(config->getSubEntryValueAsInt(_T("snmpVersion"), 0, SNMP_VERSION_DEFAULT));
   if (config->getSubEntryValueAsBoolean(_T("isDisabled")))
      m_status = ITEM_STATUS_DISABLED;

   MemFree(m_pollingIntervalSrc);
   MemFree(m_retentionTimeSrc);
   m_pollingIntervalSrc = MemCopyString(config->getSubEntryValue(_T("interval"), 0, _T("0")));
   if(config->findEntry(_T("scheduleType")) != nullptr)
   {
      m_pollingScheduleType = config->getSubEntryValueAsInt(_T("scheduleType"));
   }
   else
   {
      m_pollingScheduleType = !_tcscmp(m_pollingIntervalSrc, _T("")) || !_tcscmp(m_pollingIntervalSrc, _T("0")) ? DC_POLLING_SCHEDULE_DEFAULT : DC_POLLING_SCHEDULE_CUSTOM;
      if (m_flags & 1)  // for compatibility with old format
         m_pollingScheduleType = DC_POLLING_SCHEDULE_ADVANCED;
   }
   m_retentionTimeSrc = MemCopyString(config->getSubEntryValue(_T("retention"), 0, _T("0")));
   if(config->findEntry(_T("retentionType")) != nullptr)
   {
      m_retentionType = config->getSubEntryValueAsInt(_T("retentionType"));
   }
   else
   {
      m_retentionType = !_tcscmp(m_retentionTimeSrc, _T("")) || !_tcscmp(m_retentionTimeSrc, _T("0")) ? DC_RETENTION_DEFAULT : DC_RETENTION_CUSTOM;
      if (m_flags & 0x200) // for compatibility with old format
         m_retentionType = DC_RETENTION_NONE;
   }

   updateTimeIntervalsInternal();

   setTransformationScript(config->getSubEntryValue(_T("transformation")));

   ConfigEntry *schedules = config->findEntry(_T("schedules"));
   if (schedules != nullptr)
      schedules = schedules->findEntry(_T("schedule"));
   if ((schedules != nullptr) && (schedules->getValueCount() > 0))
   {
      if (m_schedules != nullptr)
         m_schedules->clear();
      else
         m_schedules = new StringList();

      int count = schedules->getValueCount();
      for(int i = 0; i < count; i++)
      {
         m_schedules->add(schedules->getValue(i));
      }
   }
   else
   {
      delete_and_null(m_schedules);
   }

   m_instanceDiscoveryMethod = (WORD)config->getSubEntryValueAsInt(_T("instanceDiscoveryMethod"));
   m_instanceDiscoveryData = config->getSubEntryValue(_T("instanceDiscoveryData"));
   setInstanceFilter(config->getSubEntryValue(_T("instanceFilter")));
   m_instance = config->getSubEntryValue(_T("instance"));
   m_instanceRetentionTime = config->getSubEntryValueAsInt(_T("instanceRetentionTime"), 0, -1);

   unlock();
}

/**
 * Get owner name
 */
const TCHAR *DCObject::getOwnerName() const
{
   auto owner = m_owner.lock();
   return (owner != nullptr) ? owner->getName() : _T("(null)");
}

/**
 * Create NXSL object for this data collection object
 */
NXSL_Value *DCObject::createNXSLObject(NXSL_VM *vm) const
{
   return vm->createValue(new NXSL_Object(vm, &g_nxslDciClass, new shared_ptr<DCObjectInfo>(createDescriptor())));
}

/**
 * Process force poll request
 */
ClientSession *DCObject::processForcePoll()
{
   lock();
   ClientSession *session = m_pollingSession;
   m_pollingSession = nullptr;
   m_doForcePoll = false;
   unlock();
   return session;
}

/**
 * Request force poll
 */
void DCObject::requestForcePoll(ClientSession *session)
{
   lock();
   if (m_pollingSession != nullptr)
      m_pollingSession->decRefCount();
   m_pollingSession = session;
   if (m_pollingSession != nullptr)
      m_pollingSession->incRefCount();
   m_doForcePoll = true;
   unlock();
}

/**
 * Filter callback data
 */
struct FilterCallbackData
{
   StringObjectMap<InstanceDiscoveryData> *filteredInstances;
   DCObject *dco;
   NXSL_VM *instanceFilter;
};

/**
 * Callback for filtering instances
 */
static EnumerationCallbackResult FilterCallback(const TCHAR *key, const TCHAR *value, FilterCallbackData *data)
{
   if (g_flags & AF_SHUTDOWN)
      return _STOP;

   NXSL_VM *instanceFilter = data->instanceFilter;
   DCObject *dco = data->dco;
   SharedString dcObjectName = dco->getName();

   SetupServerScriptVM(instanceFilter, dco->getOwner(), dco->createDescriptor());
   instanceFilter->setGlobalVariable("$targetObject", dco->getOwner()->createNXSLObject(instanceFilter));
   if (dco->getSourceNode() != 0)
   {
      shared_ptr<NetObj> sourceNode = FindObjectById(dco->getSourceNode(), OBJECT_NODE);
      if (sourceNode != nullptr)
         instanceFilter->setGlobalVariable("$sourceNode", sourceNode->createNXSLObject(instanceFilter));
   }

   NXSL_Value *argv[2];
   argv[0] = instanceFilter->createValue(key);
   argv[1] = instanceFilter->createValue(value);

   if (instanceFilter->run(2, argv))
   {
      bool accepted;
      const TCHAR *instance = key;
      const TCHAR *name = value;
      uint32_t relatedObject = 0;
      NXSL_Value *result = instanceFilter->getResult();
      if (result != nullptr)
      {
         if (result->isArray())
         {
            NXSL_Array *array = result->getValueAsArray();
            if (array->size() > 0)
            {
               accepted = array->get(0)->getValueAsBoolean();
               if (accepted && (array->size() > 1))
               {
                  // transformed value
                  const TCHAR *newValue = array->get(1)->getValueAsCString();
                  if ((newValue != nullptr) && (*newValue != 0))
                  {
                     DbgPrintf(5, _T("DCObject::filterInstanceList(%s [%d]): instance \"%s\" replaced by \"%s\""),
                              dcObjectName.cstr(), dco->getId(), instance, newValue);
                     instance = newValue;
                  }
               }
               if (array->size() > 2)
               {
                  // instance name
                  const TCHAR *newName = array->get(2)->getValueAsCString();
                  if ((newName != nullptr) && (*newName != 0))
                  {
                     DbgPrintf(5, _T("DCObject::filterInstanceList(%s [%d]): instance \"%s\" name set to \"%s\""),
                              dcObjectName.cstr(), dco->getId(), instance, newName);
                     name = newName;
                  }
               }
               if ((array->size() > 3) && array->get(3)->isObject(g_nxslNetObjClass.getName()))
               {
                  relatedObject = (*static_cast<shared_ptr<NetObj>*>(array->get(3)->getValueAsObject()->getData()))->getId();
               }
            }
            else
            {
               accepted = true;
            }
         }
         else
         {
            accepted = result->getValueAsBoolean();
         }
      }
      else
      {
         accepted = true;
      }
      if (accepted)
      {
         data->filteredInstances->set(instance, new InstanceDiscoveryData(name, relatedObject));
      }
      else
      {
         DbgPrintf(5, _T("DCObject::filterInstanceList(%s [%d]): instance \"%s\" removed by filtering script"),
                  dcObjectName.cstr(), dco->getId(), key);
      }
   }
   else
   {
      TCHAR szBuffer[1024];
      _sntprintf(szBuffer, 1024, _T("DCI::%s::%d::InstanceFilter"), dco->getOwnerName(), dco->getId());
      PostDciEvent(EVENT_SCRIPT_ERROR, g_dwMgmtNode, dco->getId(), "ssd", szBuffer, instanceFilter->getErrorText(), dco->getId());
      data->filteredInstances->set(key, new InstanceDiscoveryData((const TCHAR *)value, 0));
   }
   return _CONTINUE;
}

/**
 * Copy instance discovery data elements
 */
static EnumerationCallbackResult CopyElements(const TCHAR *key, const TCHAR *value, StringObjectMap<InstanceDiscoveryData> *map)
{
   map->set(key, new InstanceDiscoveryData(value, 0));
   return _CONTINUE;
}

/**
 * Filter instance list
 */
StringObjectMap<InstanceDiscoveryData> *DCObject::filterInstanceList(StringMap *instances)
{
   auto filteredInstances = new StringObjectMap<InstanceDiscoveryData>(Ownership::True);

   lock();
   if (m_instanceFilter == nullptr)
   {
      unlock();
      instances->forEach(CopyElements, filteredInstances);
      return filteredInstances;
   }

   FilterCallbackData data;
   data.instanceFilter = new NXSL_VM(new NXSL_ServerEnv());
   data.instanceFilter->setUserData(getOwner().get());
   if (!data.instanceFilter->load(m_instanceFilter))
   {
      TCHAR scriptIdentification[1024];
      _sntprintf(scriptIdentification, 1024, _T("DCI::%s::%d::InstanceFilter"), getOwnerName(), m_id);
      PostDciEvent(EVENT_SCRIPT_ERROR, g_dwMgmtNode, m_id, "ssd", scriptIdentification, data.instanceFilter->getErrorText(), m_id);
   }
   unlock();

   data.filteredInstances = filteredInstances;
   data.dco = this;
   instances->forEach(FilterCallback, &data);
   delete data.instanceFilter;
   return filteredInstances;
}

/**
 * Set new instance discovery filter script
 */
void DCObject::setInstanceFilter(const TCHAR *script)
{
   MemFree(m_instanceFilterSource);
   delete m_instanceFilter;
   if (script != nullptr)
   {
      m_instanceFilterSource = Trim(MemCopyString(script));
      if (m_instanceFilterSource[0] != 0)
      {
         TCHAR errorText[1024];
         m_instanceFilter = NXSLCompile(m_instanceFilterSource, errorText, 1024, nullptr);
         if (m_instanceFilter == nullptr)
         {
            // node can be nullptr if this DCO was just created from template
            // in this case compilation error will be reported on template level anyway
            auto owner = m_owner.lock();
            if (owner != nullptr)
            {
               nxlog_write(NXLOG_WARNING, _T("Failed to compile instance filter script for object %s [%u] DCI %s [%u] (%s)"),
                        owner->getName(), owner->getId(), m_name.cstr(), m_id, errorText);

               TCHAR scriptIdentification[1024];
               _sntprintf(scriptIdentification, 1024, _T("DCI::%s::%d::InstanceFilter"), owner->getName(), m_id);
               PostDciEvent(EVENT_SCRIPT_ERROR, g_dwMgmtNode, m_id, "ssd", scriptIdentification, errorText, m_id);
            }
         }
      }
      else
      {
         m_instanceFilter = nullptr;
      }
   }
   else
   {
      m_instanceFilterSource = nullptr;
      m_instanceFilter = nullptr;
   }
}

/**
 * Update time intervals (polling and retention) from sources
 */
void DCObject::updateTimeIntervalsInternal()
{
   if ((m_retentionTimeSrc != nullptr) && (*m_retentionTimeSrc != 0))
   {
      StringBuffer exp = m_owner.lock()->expandText(m_retentionTimeSrc, nullptr, nullptr, createDescriptorInternal(), nullptr, nullptr, m_instance, nullptr, nullptr);
      m_retentionTime = _tcstol(exp, nullptr, 10);
   }
   else
   {
      m_retentionTime = 0;
   }

   if ((m_pollingIntervalSrc != nullptr) && (*m_pollingIntervalSrc != 0))
   {
      StringBuffer exp = m_owner.lock()->expandText(m_pollingIntervalSrc, nullptr, nullptr, createDescriptorInternal(), nullptr, nullptr, m_instance, nullptr, nullptr);
      m_pollingInterval = _tcstol(exp, nullptr, 10);
   }
   else
   {
      m_pollingInterval = 0;
   }

   nxlog_debug(6, _T("DCObject::updateTimeIntervalsInternal(%s [%d]): retentionTime=%d, pollingInterval=%d"), m_name.cstr(), m_id, m_retentionTime, m_pollingInterval);
}

/**
 * Checks if this DCO object is visible by current user
 */
bool DCObject::hasAccess(UINT32 userId)
{
   if (m_accessList->isEmpty() || (userId == 0))
      return true;

   for(int i = 0; i < m_accessList->size(); i++)
   {
      UINT32 id = m_accessList->get(i);
      if (id & GROUP_FLAG)
      {
         if (CheckUserMembership(userId, id))
            return true;
      }
      else if (id == userId)
      {
         return true;
      }
   }
   return false;
}

/**
 * Serialize object to JSON
 */
json_t *DCObject::toJson()
{
   json_t *root = json_object();
   lock();
   json_object_set_new(root, "id", json_integer(m_id));
   json_object_set_new(root, "guid", m_guid.toJson());
   json_object_set_new(root, "name", json_string_t(m_name));
   json_object_set_new(root, "description", json_string_t(m_description));
   json_object_set_new(root, "systemTag", json_string_t(m_systemTag));
   json_object_set_new(root, "lastPoll", json_integer(m_lastPoll));
   json_object_set_new(root, "pollingInterval", json_string_t(m_pollingIntervalSrc));
   json_object_set_new(root, "retentionTime", json_string_t(m_retentionTimeSrc));
   json_object_set_new(root, "source", json_integer(m_source));
   json_object_set_new(root, "status", json_integer(m_status));
   json_object_set_new(root, "busy", json_integer(m_busy));
   json_object_set_new(root, "scheduledForDeletion", json_integer(m_scheduledForDeletion));
   json_object_set_new(root, "flags", json_integer(m_flags));
   json_object_set_new(root, "dwTemplateId", json_integer(m_dwTemplateId));
   json_object_set_new(root, "dwTemplateItemId", json_integer(m_dwTemplateItemId));
   json_object_set_new(root, "schedules", (m_schedules != nullptr) ? m_schedules->toJson() : json_array());
   json_object_set_new(root, "lastCheck", json_integer(m_tLastCheck));
   json_object_set_new(root, "errorCount", json_integer(m_dwErrorCount));
   json_object_set_new(root, "resourceId", json_integer(m_dwResourceId));
   json_object_set_new(root, "sourceNode", json_integer(m_sourceNode));
   json_object_set_new(root, "snmpPort", json_integer(m_snmpPort));
   json_object_set_new(root, "snmpVersion", json_integer(m_snmpVersion));
   json_object_set_new(root, "perfTabSettings", json_string_t(m_pszPerfTabSettings));
   json_object_set_new(root, "transformationScript", json_string_t(m_transformationScriptSource));
   json_object_set_new(root, "comments", json_string_t(m_comments));
   json_object_set_new(root, "instanceDiscoveryMethod", json_integer(m_instanceDiscoveryMethod));
   json_object_set_new(root, "instanceDiscoveryData", json_string_t(m_instanceDiscoveryData));
   json_object_set_new(root, "instanceFilter", json_string_t(m_instanceFilterSource));
   json_object_set_new(root, "instance", json_string_t(m_instance));
   json_object_set_new(root, "accessList", m_accessList->toJson());
   json_object_set_new(root, "instanceRetentionTime", json_integer(m_instanceRetentionTime));
   unlock();
   return root;
}

/**
 * Create descriptor for this object (external entry point)
 */
shared_ptr<DCObjectInfo> DCObject::createDescriptor() const
{
   lock();
   shared_ptr<DCObjectInfo> info = createDescriptorInternal();
   unlock();
   return info;
}

/**
 * Create descriptor for this object (internal implementation)
 */
shared_ptr<DCObjectInfo> DCObject::createDescriptorInternal() const
{
   return make_shared<DCObjectInfo>(*this);
}

/**
 * Fill message with scheduling data
 */
void DCObject::fillSchedulingDataMessage(NXCPMessage *msg, uint32_t base) const
{
   if (m_schedules != nullptr)
   {
      msg->setField(base++, static_cast<uint32_t>(m_schedules->size()));
      for(int i = 0; i < m_schedules->size(); i++)
      {
         msg->setField(base++, m_schedules->get(i));
      }
   }
   else
   {
      msg->setField(base++, static_cast<uint32_t>(0));
   }
}

/**
 * Add dependencies from library script with given name (and given script itself)
 */
static void AddScriptDependencies(StringSet *dependencies, const TCHAR *name)
{
   TCHAR buffer[256];
   const TCHAR *p = _tcschr(name, _T('('));
   const TCHAR *scriptName;
   if (p != nullptr)
   {
      _tcslcpy(buffer, name, p - name + 1);
      scriptName = buffer;
   }
   else
   {
      scriptName = name;
   }
   dependencies->add(scriptName);

   StringList *scriptDependencies = GetServerScriptLibrary()->getScriptDependencies(scriptName);
   if (scriptDependencies != nullptr)
   {
      for(int i = 0; i < scriptDependencies->size(); i++)
      {
         const TCHAR *s = scriptDependencies->get(i);
         if (!dependencies->contains(s))  // To avoid infinite loop on circular reference
         {
            dependencies->add(s);
            AddScriptDependencies(dependencies, s);
         }
      }
      delete scriptDependencies;
   }
}

/**
 * Add dependencies from compiled script
 */
static void AddScriptDependencies(StringSet *dependencies, const NXSL_Program *script)
{
   if (script != nullptr)
   {
      StringList *modules = script->getRequiredModules();
      if (modules != nullptr)
      {
         for(int i = 0; i < modules->size(); i++)
         {
            const TCHAR *s = modules->get(i);
            if (!dependencies->contains(s))  // To avoid infinite loop on circular reference
            {
               dependencies->add(s);
               AddScriptDependencies(dependencies, s);
            }
         }
         delete modules;
      }
   }
}

/**
 * Get all script dependencies for this object
 */
void DCObject::getScriptDependencies(StringSet *dependencies) const
{
   if (m_source == DS_SCRIPT)
      AddScriptDependencies(dependencies, m_name);

   if (m_instanceDiscoveryMethod == IDM_SCRIPT)
      AddScriptDependencies(dependencies, m_instanceDiscoveryData);

   AddScriptDependencies(dependencies, m_transformationScript);
   AddScriptDependencies(dependencies, m_instanceFilter);
}

/**
 * Get data source name
 */
const TCHAR *DCObject::getDataProviderName(int dataProvider)
{
   static const TCHAR *names[] = { _T("internal"), _T("nxagent"), _T("snmp"), _T("websvc"), _T("push"), _T("winperf"), _T("smclp"), _T("script"), _T("ssh"), _T("mqtt"), _T("driver") };
   return ((dataProvider >= DS_INTERNAL) && (dataProvider <= DS_DEVICE_DRIVER)) ? names[dataProvider] : _T("unknown");
}


String DCObject::getText() const
{
   return m_description;
}

SharedString DCObject::getAttribute(const String &attribute) const
{
   SharedString value;
   StringBuffer tmp;
   if (!_tcsicmp(attribute, _T("description")))
   {
      value = m_description;
   }
   else if (!_tcsicmp(attribute, _T("guid")))
   {
      value = m_guid.toString();
   }
   else if (!_tcsicmp(attribute, _T("id")))
   {
      tmp.append(m_id);
      value = tmp;
   }
   else if (!_tcsicmp(attribute, _T("name")))
   {
      value = m_name;
   }
   else if (!_tcsicmp(attribute, _T("pollingInterval")))
   {
      tmp.append(m_pollingInterval);
      value = tmp;
   }
   else if (!_tcsicmp(attribute, _T("retentionTime")))
   {
      tmp.append(m_retentionTime);
      value = tmp;
   }
   else if (!_tcsicmp(attribute, _T("sourceNode")))
   {
      auto owner = m_owner.lock();
      if (owner != nullptr)
         value = owner->getName();
   }
   return value;
}

/**
 * Data collection object info - constructor
 */
DCObjectInfo::DCObjectInfo(const DCObject& object)
{
   m_id = object.m_id;
   m_ownerId = object.getOwnerId();
   m_templateId = object.m_dwTemplateId;
   m_templateItemId = object.m_dwTemplateItemId;
   m_type = object.getType();
   _tcslcpy(m_name, object.m_name, MAX_ITEM_NAME);
   _tcslcpy(m_description, object.m_description, MAX_DB_STRING);
   _tcslcpy(m_systemTag, object.m_systemTag, MAX_DB_STRING);
   _tcslcpy(m_instance, object.m_instance, MAX_DB_STRING);
   m_instanceData = MemCopyString(object.m_instanceDiscoveryData);
   m_comments = MemCopyString(object.m_comments);
   m_dataType = (m_type == DCO_TYPE_ITEM) ? static_cast<const DCItem&>(object).m_dataType : -1;
   m_origin = object.m_source;
   m_status = object.m_status;
   m_errorCount = object.m_dwErrorCount;
   m_pollingInterval = object.getEffectivePollingInterval();
   m_lastPollTime = object.m_lastPoll;
   m_hasActiveThreshold = false;
   m_thresholdSeverity = SEVERITY_NORMAL;
   m_relatedObject = object.getRelatedObject();
}

/**
 * Data collection object info - constructor for client provided DCI info
 */
DCObjectInfo::DCObjectInfo(const NXCPMessage& msg, const DCObject *object)
{
   m_id = msg.getFieldAsUInt32(VID_DCI_ID);
   m_ownerId = msg.getFieldAsUInt32(VID_OBJECT_ID);
   m_templateId = (object != nullptr) ? object->getTemplateId() : 0;
   m_templateItemId = (object != nullptr) ? object->getTemplateItemId() : 0;
   m_type = msg.getFieldAsInt16(VID_DCOBJECT_TYPE);
   msg.getFieldAsString(VID_NAME, m_name, MAX_ITEM_NAME);
   msg.getFieldAsString(VID_DESCRIPTION, m_description, MAX_DB_STRING);
   msg.getFieldAsString(VID_SYSTEM_TAG, m_systemTag, MAX_DB_STRING);
   msg.getFieldAsString(VID_INSTANCE, m_instance, MAX_DB_STRING);
   m_instanceData = msg.getFieldAsString(VID_INSTD_DATA);
   m_comments = msg.getFieldAsString(VID_COMMENTS);
   m_dataType = (m_type == DCO_TYPE_ITEM) ? msg.getFieldAsInt16(VID_DCI_DATA_TYPE) : -1;
   m_origin = msg.getFieldAsInt16(VID_DCI_SOURCE_TYPE);
   m_status = msg.getFieldAsInt16(VID_DCI_STATUS);
   m_pollingInterval = (object != nullptr) ? object->getEffectivePollingInterval() : 0;
   m_errorCount = (object != nullptr) ? object->getErrorCount() : 0;
   m_lastPollTime = (object != nullptr) ? object->getLastPollTime() : 0;
   m_hasActiveThreshold = false;
   m_thresholdSeverity = SEVERITY_NORMAL;
   m_relatedObject = (object != nullptr) ? object->getRelatedObject() : 0;
}

/**
 * Data collection object info - destructor
 */
DCObjectInfo::~DCObjectInfo()
{
   MemFree(m_instanceData);
   MemFree(m_comments);
}
