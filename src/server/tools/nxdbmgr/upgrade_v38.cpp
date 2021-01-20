/*
** nxdbmgr - NetXMS database manager
** Copyright (C) 2020-2021 Raden Solutions
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
** File: upgrade_v38.cpp
**
**/

#include "nxdbmgr.h"

/**
 * Upgrade from 38.1 to 38.2
 */
static bool H_UpgradeFromV1()
{
   if (DBIsTableExist(g_dbHandle, _T("report_notifications")))
   {
      CHK_EXEC(SQLQuery(_T("DROP TABLE report_notifications")));
   }
   CHK_EXEC(SQLQuery(_T("ALTER TABLE report_results ADD is_success char(1)")));
   CHK_EXEC(SQLQuery(_T("UPDATE report_results SET is_success='T'")));
   CHK_EXEC(DBSetNotNullConstraint(g_dbHandle, _T("report_results"), _T("is_success")));
   CHK_EXEC(SetMinorSchemaVersion(2));
   return true;
}

/**
 * Upgrade from 38.0 to 38.1
 */
static bool H_UpgradeFromV0()
{
   static const TCHAR *deprecatedTables[] = {
      _T("qrtz_fired_triggers"),
      _T("qrtz_paused_trigger_grps"),
      _T("qrtz_scheduler_state"),
      _T("qrtz_locks"),
      _T("qrtz_simple_triggers"),
      _T("qrtz_cron_triggers"),
      _T("qrtz_simprop_triggers"),
      _T("qrtz_blob_triggers"),
      _T("qrtz_triggers"),
      _T("qrtz_job_details"),
      _T("qrtz_calendars"),
      _T("report_notification"),
      nullptr
   };
   for(int i = 0; deprecatedTables[i] != nullptr; i++)
   {
      if (DBIsTableExist(g_dbHandle, deprecatedTables[i]))
      {
         TCHAR query[256];
         _sntprintf(query, 256, _T("DROP TABLE %s"), deprecatedTables[i]);
         CHK_EXEC(SQLQuery(query));
      }
   }

   if (DBIsTableExist(g_dbHandle, _T("reporting_results")))
   {
      CHK_EXEC(DBRenameTable(g_dbHandle, _T("reporting_results"), _T("report_results")));
   }
   else
   {
      const TCHAR *dtType;
      switch(g_dbSyntax)
      {
         case DB_SYNTAX_ORACLE:
            dtType = _T("timestamp");
            break;
         case DB_SYNTAX_PGSQL:
         case DB_SYNTAX_TSDB:
            dtType = _T("timestamp without time zone");
            break;
         default:
            dtType = _T("datetime");
            break;
      }

      TCHAR query[1024];
      _sntprintf(query, 1024,
         _T("CREATE TABLE report_results (")
         _T("   id integer not null%s,")
         _T("   executiontime %s null,")
         _T("   jobid char(36) null,")
         _T("   reportid char(36) null,")
         _T("   userid integer null,")
         _T("   PRIMARY KEY(id))"), (g_dbSyntax == DB_SYNTAX_MYSQL) ? _T(" auto_increment") : _T(""), dtType);
      CHK_EXEC(CreateTable(query));

      if (g_dbSyntax == DB_SYNTAX_ORACLE)
      {
         CHK_EXEC(SQLQuery(_T("CREATE SEQUENCE HIBERNATE_SEQUENCE INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999999999999999999999999 CACHE 20 NOCYCLE")));
      }
      else if (g_dbSyntax == DB_SYNTAX_MSSQL)
      {
         CHK_EXEC(SQLQuery(_T("CREATE SEQUENCE HIBERNATE_SEQUENCE INCREMENT BY 1 MINVALUE 1 CACHE 20")));
      }
      else if (g_dbSyntax != DB_SYNTAX_MYSQL)
      {
         CHK_EXEC(SQLQuery(_T("CREATE SEQUENCE HIBERNATE_SEQUENCE INCREMENT BY 1 MINVALUE 1")));
      }
   }

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
} s_dbUpgradeMap[] =
{
   { 1,  38, 2,  H_UpgradeFromV1  },
   { 0,  38, 1,  H_UpgradeFromV0  },
   { 0,  0,  0,  nullptr          }
};

/**
 * Upgrade database to new version
 */
bool MajorSchemaUpgrade_V38()
{
   int32_t major, minor;
   if (!DBGetSchemaVersion(g_dbHandle, &major, &minor))
      return false;

   while((major == 38) && (minor < DB_SCHEMA_VERSION_V38_MINOR))
   {
      // Find upgrade procedure
      int i;
      for(i = 0; s_dbUpgradeMap[i].upgradeProc != nullptr; i++)
         if (s_dbUpgradeMap[i].version == minor)
            break;
      if (s_dbUpgradeMap[i].upgradeProc == nullptr)
      {
         _tprintf(_T("Unable to find upgrade procedure for version 38.%d\n"), minor);
         return false;
      }
      _tprintf(_T("Upgrading from version 38.%d to %d.%d\n"), minor, s_dbUpgradeMap[i].nextMajor, s_dbUpgradeMap[i].nextMinor);
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
