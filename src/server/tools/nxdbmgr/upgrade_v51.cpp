/*
** nxdbmgr - NetXMS database manager
** Copyright (C) 2022-2024 Raden Solutions
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
** File: upgrade_v51.cpp
**
**/

#include "nxdbmgr.h"
#include <nxevent.h>

/**
 * Upgrade from 51.0 to 51.1
 */
static bool H_UpgradeFromV0()
{
   CHK_EXEC(CreateEventTemplate(EVENT_CIRCUIT_AUTOBIND, _T("SYS_CIRCUIT_AUTOBIND"),
         EVENT_SEVERITY_NORMAL, EF_LOG, _T("53372caa-f00b-40d9-961b-ca41109c91c7"),
         _T("Interface %2 on node %6 automatically bound to circuit %4"),
         _T("Generated when interface bound to circuit by autobind rule.\r\n")
         _T("Parameters:\r\n")
         _T("   1) interfaceId - Interface ID\r\n")
         _T("   2) interfaceName - Interface name\r\n")
         _T("   3) circuitId - Circuit ID\r\n")
         _T("   4) circuitName - Circuit name\r\n")
         _T("   5) nodeId - Interface owning node ID\r\n")
         _T("   6) nodeName - Interface owning node name")
      ));

   CHK_EXEC(CreateEventTemplate(EVENT_CIRCUIT_AUTOUNBIND, _T("SYS_CIRCUIT_AUTOUNBIND"),
         EVENT_SEVERITY_NORMAL, EF_LOG, _T("bc04d7c0-f2f6-4558-9842-6f751c3657b1"),
         _T("Interface %2 on node %6 automatically unbound from circuit %4"),
         _T("Generated when interface unbound from circuit by autobind rule.\r\n")
         _T("Parameters:\r\n")
         _T("   1) interfaceId - Interface ID\r\n")
         _T("   2) interfaceName - Interface name\r\n")
         _T("   3) circuitId - Circuit ID\r\n")
         _T("   4) circuitName - Circuit name\r\n")
         _T("   5) nodeId - Interface owning node ID\r\n")
         _T("   6) nodeName - Interface owning node name")
      ));

   CHK_EXEC(SetMinorSchemaVersion(1));
   return true;
}

/**
 * Upgrade map
 */
static struct
{
   int version;
   int nextMajor;
   int nextMinor;
   bool (*upgradeProc)();
} s_dbUpgradeMap[] = {
   { 0,  51, 1,  H_UpgradeFromV0  },
   { 0,  0,  0,  nullptr }
};

/**
 * Upgrade database to new version
 */
bool MajorSchemaUpgrade_V51()
{
   int32_t major, minor;
   if (!DBGetSchemaVersion(g_dbHandle, &major, &minor))
      return false;

   while ((major == 51) && (minor < DB_SCHEMA_VERSION_V51_MINOR))
   {
      // Find upgrade procedure
      int i;
      for (i = 0; s_dbUpgradeMap[i].upgradeProc != nullptr; i++)
         if (s_dbUpgradeMap[i].version == minor)
            break;
      if (s_dbUpgradeMap[i].upgradeProc == nullptr)
      {
         _tprintf(_T("Unable to find upgrade procedure for version 51.%d\n"), minor);
         return false;
      }
      _tprintf(_T("Upgrading from version 51.%d to %d.%d\n"), minor, s_dbUpgradeMap[i].nextMajor, s_dbUpgradeMap[i].nextMinor);
      DBBegin(g_dbHandle);
      if (s_dbUpgradeMap[i].upgradeProc())
      {
         DBCommit(g_dbHandle);
         if (!DBGetSchemaVersion(g_dbHandle, &major, &minor))
            return false;
      }
      else
      {
         _tprintf(_T("Rolling back last stage due to upgrade errors...\n"));
         DBRollback(g_dbHandle);
         return false;
      }
   }
   return true;
}
