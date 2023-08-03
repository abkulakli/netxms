/* 
 ** NetXMS - Network Management System
 ** Driver for Mikrotik routers
 ** Copyright (C) 2003-2023 Victor Kirhenshtein
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
 ** File: mikrotik.h
 **
 **/

#ifndef _mikrotik_h_
#define _mikrotik_h_

#include <nddrv.h>

#define DEBUG_TAG _T("ndd.mikrotik")

/**
 * Driver's class
 */
class MikrotikDriver : public NetworkDeviceDriver
{
public:
   virtual const TCHAR* getName() override;
   virtual const TCHAR* getVersion() override;

   virtual int isPotentialDevice(const TCHAR *oid) override;
   virtual bool isDeviceSupported(SNMP_Transport *snmp, const TCHAR *oid) override;
   virtual void analyzeDevice(SNMP_Transport *snmp, const TCHAR *oid, NObject *node, DriverData **driverData) override;
   virtual bool getHardwareInformation(SNMP_Transport *snmp, NObject *node, DriverData *driverData, DeviceHardwareInfo *hwInfo) override;
   virtual InterfaceList* getInterfaces(SNMP_Transport *snmp, NObject *node, DriverData *driverData, bool useIfXTable) override;
   virtual bool lldpNameToInterfaceId(SNMP_Transport *snmp, NObject *node, DriverData *driverData, const TCHAR *lldpName, InterfaceId *id) override;
   virtual bool isFdbUsingIfIndex(const NObject *node, DriverData *driverData) override;
   virtual bool isWirelessController(SNMP_Transport *snmp, NObject *node, DriverData *driverData) override;
   virtual ObjectArray<AccessPointInfo>* getAccessPoints(SNMP_Transport *snmp, NObject *node, DriverData *driverData) override;
   virtual ObjectArray<WirelessStationInfo>* getWirelessStations(SNMP_Transport *snmp, NObject *node, DriverData *driverData) override;
   virtual AccessPointState getAccessPointState(SNMP_Transport *snmp, NObject *node, DriverData *driverData, UINT32 apIndex, const MacAddress &macAddr, const InetAddress &ipAddr, const ObjectArray<RadioInterfaceInfo> *radioInterfaces) override;
   virtual bool hasMetrics() override;
   virtual DataCollectionError getMetric(SNMP_Transport *snmp, NObject *node, DriverData *driverData, const TCHAR *name, TCHAR *value, size_t size) override;
   virtual ObjectArray<AgentParameterDefinition>* getAvailableMetrics(SNMP_Transport *snmp, NObject *node, DriverData *driverData) override;
};

/**
 * Mikrotik Device data
 */
class MikrotikDriverData : public DriverData
{
private:
   StringMap m_oidCache;
   time_t m_cacheTimestamp = 0;
   Mutex m_cacheLock;
   void updateDeviceInfoInternal(SNMP_Transport *snmp);
   uint32_t metricInfoWalkCallback(SNMP_Variable *v, SNMP_Transport *snmp);

public:
   bool getMetricOID(const TCHAR *name, SNMP_Transport *snmp, TCHAR *oid);
   void updateDeviceInfo(SNMP_Transport *snmp);
   void registerMetrics(ObjectArray<AgentParameterDefinition> *metrics);
};

#endif
