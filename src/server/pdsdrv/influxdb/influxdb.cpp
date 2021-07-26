/**
 ** NetXMS - Network Management System
 ** Performance Data Storage Driver for InfluxDB
 ** Copyright (C) 2019 Sebastian YEPES FERNANDEZ & Julien DERIVIERE
 ** Copyright (C) 2021 Raden Solutions
 **
 ** This program is free software; you can redistribute it and/or modify
 ** it under the terms of the GNU Lesser General Public License as published by
 ** the Free Software Foundation; either version 3 of the License, or
 ** (at your option) any later version.
 **
 ** This program is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 ** GNU General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this program; if not, write to the Free Software
 ** Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 **
 **              ,.-----__
 **          ,:::://///,:::-.
 **         /:''/////// ``:::`;/|/
 **        /'   ||||||     :://'`\
 **      .' ,   ||||||     `/(  e \
 **-===~__-'\__X_`````\_____/~`-._ `.
 **            ~~        ~~       `~-'
 ** Armadillo, The Metric Eater - https://www.nationalgeographic.com/animals/mammals/group/armadillos/
 **
 ** File: influxdb.cpp
 **/

#include "influxdb.h"

/**
 * Driver name
 */
static const TCHAR *s_driverName = _T("InfluxDB");

/**
 * Constructor
 */
InfluxDBStorageDriver::InfluxDBStorageDriver() : m_senders(0, 16, Ownership::True)
{
   m_enableUnsignedType = false;
}

/**
 * Destructor
 */
InfluxDBStorageDriver::~InfluxDBStorageDriver()
{
}

/**
 * Get name
 */
const TCHAR *InfluxDBStorageDriver::getName()
{
   return s_driverName;
}

/**
 * Initialize driver
 */
bool InfluxDBStorageDriver::init(Config *config)
{
   const TCHAR *protocol = config->getValue(_T("/InfluxDB/Protocol"), _T("udp"));
   if (_tcsicmp(protocol, _T("udp")) && _tcsicmp(protocol, _T("api-v1")) && _tcsicmp(protocol, _T("api-v2")))
   {
      nxlog_write_tag(NXLOG_ERROR, DEBUG_TAG, _T("Invalid protocol specification %s"), protocol);
      return false;
   }

   if (!_tcsnicmp(protocol, _T("api"), 3))
   {
#if HAVE_LIBCURL
      if (!InitializeLibCURL())
      {
         nxlog_write_tag(NXLOG_ERROR, DEBUG_TAG, _T("cURL initialization failed"));
         return false;
      }
#else
      nxlog_write_tag(NXLOG_ERROR, DEBUG_TAG, _T("Driver built without libcurl, API protocols are not supported"));
      return false;
#endif
   }

   m_enableUnsignedType = config->getValueAsBoolean(_T("/InfluxDB/EnableUnsignedType"), m_enableUnsignedType);
   nxlog_debug_tag(DEBUG_TAG, 2, _T("Unsigned integer data type is %s"), m_enableUnsignedType ? _T("enabled") : _T("disabled"));

   int queueCount = config->getValueAsInt(_T("/InfluxDB/Queues"), 1);
   if (queueCount < 1)
      queueCount = 1;
   else if (queueCount > 32)
      queueCount = 32;

   nxlog_debug_tag(DEBUG_TAG, 2, _T("Using %d queue%s"), queueCount, queueCount > 1 ? _T("s") : _T(""));
   for(int i = 0; i < queueCount; i++)
   {
      InfluxDBSender *sender;
      if (!_tcsicmp(protocol, _T("udp")))
         sender = new UDPSender(*config);
#if HAVE_LIBCURL
      else if (!_tcsicmp(protocol, _T("api-v1")))
         sender = new APIv1Sender(*config);
      else if (!_tcsicmp(protocol, _T("api-v2")))
         sender = new APIv2Sender(*config);
#endif
      sender->start();
      m_senders.add(sender);
   }

   return true;
}

/**
 * Shutdown driver
 */
void InfluxDBStorageDriver::shutdown()
{
   for(int i = 0; i < m_senders.size(); i++)
      m_senders.get(i)->stop();
   nxlog_debug_tag(DEBUG_TAG, 1, _T("Shutdown completed"));
}

/**
 * Normalize all the metric and tag names
 */
static StringBuffer NormalizeString(const TCHAR *src)
{
   StringBuffer dst(src);
   dst.toLowercase();
   dst.trim();
   dst.replace(_T(":"), _T("_"));
   dst.replace(_T("-"), _T("_"));
   dst.replace(_T("."), _T("_"));
   dst.replace(_T(","), _T("_"));
   dst.replace(_T("#"), _T("_"));
   dst.replace(_T("\\"), _T("/"));
   ssize_t index = dst.find(_T("("));
   if (index != String::npos)
      dst.shrink(dst.length() - index);
   return dst;
}

/**
 * Find and replace all occurrences of given sub-string
 */
static void FindAndReplaceAll(StringBuffer *data, const TCHAR *toSearch, const TCHAR *replaceStr)
{
   // Get the first occurrence
   ssize_t pos = data->find(toSearch);

   // Repeat till end is reached
   while (pos != String::npos)
   {
      // Replace this occurrence of Sub String
      data->replace(toSearch, replaceStr);
      // Get the next occurrence from the current position
      pos = pos = data->find(toSearch);
   }
}

/**
 * Get tags from given object. Returns true if
 */
static bool GetTagsFromObject(const NetObj& object, StringBuffer *tags)
{
   StringMap *ca = object.getCustomAttributes();
   if (ca == nullptr)
      return false;

   nxlog_debug_tag(DEBUG_TAG, 7, _T("Object: %s - CMA: #%d"), object.getName(), ca->size());

   bool ignoreMetric = false;
   StringList *keys = ca->keys();
   for (int i = 0; i < keys->size(); i++)
   {
      const TCHAR *key = keys->get(i);
      if (!_tcsnicmp(_T("ignore_influxdb"), key, 15))
      {
         ignoreMetric = true;
         break;
      }

      // Only include CA's with the prefix tag_
      if (!_tcsnicmp(_T("tag_"), key, 4))
      {
         StringBuffer value = NormalizeString(ca->get(keys->get(i)));
         if (!value.isEmpty())
         {
            StringBuffer name = NormalizeString(&key[4]);
            nxlog_debug_tag(DEBUG_TAG, 7, _T("Object: %s - CA: K:%s = V:%s"), object.getName(), name.cstr(), value.cstr());
            tags->append(_T(','));
            tags->append(key);
            tags->append(_T('='));
            tags->append(value);
         }
         else
         {
            nxlog_debug_tag(DEBUG_TAG, 7, _T("Object: %s - CA: K:%s (Ignored)"), object.getName(), key);
         }
      }
      else
      {
         nxlog_debug_tag(DEBUG_TAG, 7, _T("Object: %s - CA: K:%s (Ignored)"), object.getName(), key);
      }
   }
   delete keys;
   delete ca;
   return ignoreMetric;
}

/**
 * Build and queue metric from item DCI's
 */
bool InfluxDBStorageDriver::saveDCItemValue(DCItem *dci, time_t timestamp, const TCHAR *value)
{
   nxlog_debug_tag(DEBUG_TAG, 8,
            _T("Raw metric: OwnerName:%s DataSource:%i Type:%i Name:%s Description: %s Instance:%s DataType:%i DeltaCalculationMethod:%i RelatedObject:%i Value:%s timestamp:") INT64_FMT,
            dci->getOwnerName(), dci->getDataSource(), dci->getType(), dci->getName().cstr(), dci->getDescription().cstr(),
            dci->getInstance().cstr(), dci->getDataType(), dci->getDeltaCalculationMethod(), dci->getRelatedObject(),
            value, static_cast<INT64>(timestamp));

   // Dont't try to send empty values
   if (*value == 0)
   {
      nxlog_debug_tag(DEBUG_TAG, 7, _T("Metric %s [%u] not sent: empty value"), dci->getName().cstr(), dci->getId());
      return true;
   }

   const TCHAR *ds; // Data sources
   switch (dci->getDataSource())
   {
      case DS_DEVICE_DRIVER:
         ds = _T("device");
         break;
      case DS_INTERNAL:
         ds = _T("internal");
         break;
      case DS_MQTT:
         ds = _T("mqtt");
         break;
      case DS_NATIVE_AGENT:
         ds = _T("agent");
         break;
      case DS_PUSH_AGENT:
         ds = _T("push");
         break;
      case DS_SCRIPT:
         ds = _T("script");
         break;
      case DS_SMCLP:
         ds = _T("smclp");
         break;
      case DS_SNMP_AGENT:
         ds = _T("snmp");
         break;
      case DS_SSH:
         ds = _T("ssh");
         break;
      case DS_WEB_SERVICE:
         ds = _T("websvc");
         break;
      case DS_WINPERF:
         ds = _T("wmi");
         break;
      default:
         ds = _T("unknown");
         break;
   }

   const TCHAR *dct; // Delta Calculation types
   switch (dci->getDeltaCalculationMethod())
   {
      case DCM_ORIGINAL_VALUE:
         dct = _T("original");
         break;
      case DCM_SIMPLE:
         dct = _T("simple");
         break;
      case DCM_AVERAGE_PER_SECOND:
         dct = _T("avg_sec");
         break;
      case DCM_AVERAGE_PER_MINUTE:
         dct = _T("avg_min");
         break;
      default:
         dct = _T("unknown");
         break;
   }

   const TCHAR *dt; // DCI (data collection item) data types
   bool isInteger, isUnsigned = false;
   switch (dci->getDataType())
   {
      case DCI_DT_INT:
         dt = _T("signed-integer32");
         isInteger = true;
         break;
      case DCI_DT_UINT:
         dt = _T("unsigned-integer32");
         isInteger = true;
         isUnsigned = true;
         break;
      case DCI_DT_INT64:
         dt = _T("signed-integer64");
         isInteger = true;
         break;
      case DCI_DT_UINT64:
         dt = _T("unsigned-integer64");
         isInteger = true;
         isUnsigned = true;
         break;
      case DCI_DT_FLOAT:
         dt = _T("float");
         isInteger = false;
         break;
      case DCI_DT_STRING:
         dt = _T("string");
         isInteger = false;
         break;
      case DCI_DT_NULL:
         dt = _T("null");
         isInteger = false;
         break;
      case DCI_DT_COUNTER32:
         dt = _T("counter32");
         isInteger = true;
         isUnsigned = true;
         break;
      case DCI_DT_COUNTER64:
         dt = _T("counter64");
         isInteger = true;
         isUnsigned = true;
         break;
      default:
         dt = _T("unknown");
         isInteger = false;
         break;
   }

   // Get Host CA's
   StringBuffer tags;
   if (GetTagsFromObject(static_cast<NetObj&>(*dci->getOwner()), &tags))
   {
      nxlog_debug_tag(DEBUG_TAG, 7, _T("Metric not sent: ignore flag set on owner object"));
      return true;
   }

   // Get RelatedObject (Interface) CA's
   const TCHAR *relatedObject_type = _T("none");

   shared_ptr<NetObj> relatedObject = FindObjectById(dci->getRelatedObject());
   if (relatedObject != nullptr)
   {
      if (GetTagsFromObject(static_cast<NetObj&>(*relatedObject), &tags))
      {
         nxlog_debug_tag(DEBUG_TAG, 7, _T("Metric not sent: ignore flag set on related object %s"), relatedObject->getName());
         return true;
      }
   }

   // Metric name
   // If it's a MIB or Dummy metric use the Description if not use the Name
   StringBuffer name;
   if ((dci->getDataSource() == DS_SNMP_AGENT) ||
       ((dci->getDataSource() == DS_INTERNAL) && !_tcsnicmp(dci->getName(), _T("Dummy"), 5)))
   {
      name = NormalizeString(dci->getDescription());
   }
   else
   {
      name = NormalizeString(dci->getName());
   }

   // Instance
   StringBuffer instance = NormalizeString(dci->getInstance());
   if (instance.isEmpty())
   {
      instance = _T("none");
   }
   else
   {
      // Remove instance from the metric name
      ssize_t i = name.find(instance);
      if (i != String::npos)
      {
         name.shrink(instance.length() - i);
         FindAndReplaceAll(&name, _T("__"), _T("_"));
      }
   }

   // Host
   StringBuffer host(dci->getOwner()->getName());
   host.replace(_T(" "), _T("_"));
   host.replace(_T(","), _T("_"));
   host.replace(_T(":"), _T("_"));
   FindAndReplaceAll(&host, _T("__"), _T("_"));
   host.toLowercase();

   // Build final metric structure
   StringBuffer data(name);
   data.append(_T(",host="));
   data.append(host);
   data.append(_T(",instance="));
   data.append(instance);
   data.append(_T(",datasource="));
   data.append(ds);
   data.append(_T(",dataclass=item,datatype="));
   data.append(dt);
   data.append(_T(",deltatype="));
   data.append(dct);
   data.append(_T(",relatedobjecttype="));
   data.append((relatedObject != nullptr) ? relatedObject->getObjectClassName() : _T("none"));
   data.append(tags);
   if (dci->getDataType() == DCI_DT_STRING)
   {
      data.append(_T(" value=\""));
      data.append(value);
      data.append(_T("\" "));
   }
   else
   {
      data.append(_T(" value="));
      data.append(value);
      if (isInteger)
         data.append((isUnsigned && m_enableUnsignedType) ? _T("u ") : _T("i "));
      else
         data.append(_T(' '));
   }
   data.append(static_cast<uint64_t>(timestamp));
   data.append(_T("000000000")); // Use nanosecond precision

   int senderIndex = dci->getId() % m_senders.size();
   nxlog_debug_tag(DEBUG_TAG, 7, _T("Queuing data to sender #%d: %s"), senderIndex, data.cstr());
   m_senders.get(senderIndex)->enqueue(data);

   return true;
}

/**
 * Save table DCI's
 */
bool InfluxDBStorageDriver::saveDCTableValue(DCTable *dcObject, time_t timestamp, Table *value)
{
   return true;
}

/**
 * Get driver's internal metrics
 */
DataCollectionError InfluxDBStorageDriver::getInternalMetric(const TCHAR *metric, TCHAR *value)
{
   DataCollectionError rc = DCE_SUCCESS;
   if (!_tcsicmp(metric, _T("senders")))
   {
      ret_int(value, m_senders.size());
   }
   else if (!_tcsicmp(metric, _T("overflowQueues")))
   {
      uint32_t count = 0;
      for(int i = 0; i < m_senders.size(); i++)
         if (m_senders.get(i)->isFull())
            count++;
      ret_uint(value, count);
   }
   else if (!_tcsicmp(metric, _T("messageDrops")))
   {
      uint64_t count = 0;
      for(int i = 0; i < m_senders.size(); i++)
         count += m_senders.get(i)->getMessageDrops();
      ret_uint64(value, count);
   }
   else if (!_tcsicmp(metric, _T("queueSize.bytes")))
   {
      uint64_t size = 0;
      for(int i = 0; i < m_senders.size(); i++)
         size += m_senders.get(i)->getQueueSizeInBytes();
      ret_uint64(value, size);
   }
   else if (!_tcsicmp(metric, _T("queueSize.messages")))
   {
      uint32_t size = 0;
      for(int i = 0; i < m_senders.size(); i++)
         size += m_senders.get(i)->getQueueSizeInMessages();
      ret_uint(value, size);
   }
   else
   {
      value[0] = 0;
      rc = DCE_NOT_SUPPORTED;
   }
   nxlog_debug_tag(DEBUG_TAG, 7, _T("getInternalMetric(%s): rc=%d, value=%s"), metric, rc, value);
   return rc;
}

/**
 * Driver entry point
 */
DECLARE_PDSDRV_ENTRY_POINT(s_driverName, InfluxDBStorageDriver);

#ifdef _WIN32

/**
 * DLL entry point
 */
BOOL WINAPI DllMain(HINSTANCE hInstance, DWORD dwReason, LPVOID lpReserved)
{
   if (dwReason == DLL_PROCESS_ATTACH)
      DisableThreadLibraryCalls(hInstance);
   return TRUE;
}

#endif
