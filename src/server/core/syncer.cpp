/*
** NetXMS - Network Management System
** Copyright (C) 2003-2020 Victor Kirhenshtein
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
** File: syncer.cpp
**
**/

#include "nxcore.h"

#define DEBUG_TAG_SYNC           _T("sync")
#define DEBUG_TAG_OBJECT_SYNC    _T("obj.sync")

/**
 * Externals
 */
void NetObjDelete(NetObj *pObject);

/**
 * Thread pool
 */
ThreadPool *g_syncerThreadPool = NULL;

/**
 * Outstanding save requests
 */
static VolatileCounter s_outstandingSaveRequests = 0;

/**
 * Object transaction lock
 */
static RWLOCK s_objectTxnLock = RWLockCreate();

/**
 * Syncer run time statistic
 */
static ManualGauge64 s_syncerRunTime(_T("Syncer"), 5, 900);
static Mutex s_syncerGaugeLock(true);
static time_t s_lastRunTime = 0;

/**
 * Get syncer run time
 */
int64_t GetSyncerRunTime(StatisticType statType)
{
   s_syncerGaugeLock.lock();
   int64_t value;
   switch(statType)
   {
      case StatisticType::AVERAGE:
         value = s_syncerRunTime.getAverage();
         break;
      case StatisticType::CURRENT:
         value = s_syncerRunTime.getCurrent();
         break;
      case StatisticType::MAX:
         value = s_syncerRunTime.getMax();
         break;
      case StatisticType::MIN:
         value = s_syncerRunTime.getMin();
         break;
   }
   s_syncerGaugeLock.unlock();
   return value;
}

/**
 * Show syncer stats on server debug console
 */
void ShowSyncerStats(ServerConsole *console)
{
   s_syncerGaugeLock.lock();
   TCHAR runTime[128];
   if (s_lastRunTime > 0)
   {
#if HAVE_LOCALTIME_R
      struct tm tmbuf;
      struct tm *ltm = localtime_r(&s_lastRunTime, &tmbuf);
#else
      struct tm *ltm = localtime(&s_lastRunTime);
#endif
      _tcsftime(runTime, 128, _T("%Y.%b.%d %H:%M:%S"), ltm);
   }
   else
   {
      _tcscpy(runTime, _T("never"));
   }
   console->printf(
            _T("Last run at .........: %s\n")
            _T("Average run time ....: %d ms\n")
            _T("Last run time .......: %d ms\n")
            _T("Max run time ........: %d ms\n")
            _T("Min run time ........: %d ms\n")
            _T("\n"), runTime,
            s_syncerRunTime.getCurrent(), s_syncerRunTime.getAverage(),
            s_syncerRunTime.getMax(), s_syncerRunTime.getMin());
   s_syncerGaugeLock.unlock();
}

/**
 * Start object transaction
 */
void NXCORE_EXPORTABLE ObjectTransactionStart()
{
   if (g_flags & AF_ENABLE_OBJECT_TRANSACTIONS)
      RWLockReadLock(s_objectTxnLock);
}

/**
 * End object transaction
 */
void NXCORE_EXPORTABLE ObjectTransactionEnd()
{
   if (g_flags & AF_ENABLE_OBJECT_TRANSACTIONS)
      RWLockUnlock(s_objectTxnLock);
}

/**
 * Save object to database on separate thread
 */
static void SaveObject(NetObj *object)
{
   DB_HANDLE hdb = DBConnectionPoolAcquireConnection();
   DBBegin(hdb);
   if (object->saveToDatabase(hdb))
   {
      DBCommit(hdb);
      object->markAsSaved();
   }
   else
   {
      DBRollback(hdb);
   }
   DBConnectionPoolReleaseConnection(hdb);
   InterlockedDecrement(&s_outstandingSaveRequests);
}

/**
 * Save objects to database
 */
void SaveObjects(DB_HANDLE hdb, UINT32 watchdogId, bool saveRuntimeData)
{
   s_outstandingSaveRequests = 0;

   if (g_flags & AF_ENABLE_OBJECT_TRANSACTIONS)
      RWLockWriteLock(s_objectTxnLock);

	ObjectArray<NetObj> *objects = g_idxObjectById.getObjects(false);
   nxlog_debug_tag(DEBUG_TAG_SYNC, 5, _T("%d objects to process"), objects->size());
	for(int i = 0; i < objects->size(); i++)
   {
	   WatchdogNotify(watchdogId);
   	NetObj *object = objects->get(i);
   	nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 8, _T("Object %s [%d] at index %d"), object->getName(), object->getId(), i);
      if (object->isDeleted())
      {
         nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 5, _T("Object %s [%d] marked for deletion"), object->getName(), object->getId());
         if (object->getRefCount() == 0)
         {
   		   DBBegin(hdb);
            if (object->deleteFromDatabase(hdb))
            {
               nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 4, _T("Object %d \"%s\" deleted from database"), object->getId(), object->getName());
               DBCommit(hdb);
               NetObjDelete(object);
            }
            else
            {
               DBRollback(hdb);
               nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 4, _T("Call to deleteFromDatabase() failed for object %s [%d], transaction rollback"), object->getName(), object->getId());
            }
         }
         else
         {
            nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 3, _T("Unable to delete object with id %d because it is being referenced %d time(s)"),
                      object->getId(), object->getRefCount());
         }
      }
		else if (object->isModified())
		{
		   nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 5, _T("Object %s [%d] modified"), object->getName(), object->getId());
		   if (g_syncerThreadPool != NULL)
		   {
		      InterlockedIncrement(&s_outstandingSaveRequests);
		      ThreadPoolExecute(g_syncerThreadPool, SaveObject, object);
		   }
		   else
		   {
            DBBegin(hdb);
            if (object->saveToDatabase(hdb))
            {
               DBCommit(hdb);
               object->markAsSaved();
            }
            else
            {
               DBRollback(hdb);
            }
		   }
		}
		else if (saveRuntimeData)
		{
         object->saveRuntimeData(hdb);
		}
   }

	if (g_syncerThreadPool != NULL)
	{
	   while(s_outstandingSaveRequests > 0)
	   {
	      nxlog_debug_tag(DEBUG_TAG_OBJECT_SYNC, 7, _T("Waiting for outstanding object save requests (%d requests in queue)"), (int)s_outstandingSaveRequests);
	      ThreadSleep(1);
	      WatchdogNotify(watchdogId);
	   }
	}

   if (g_flags & AF_ENABLE_OBJECT_TRANSACTIONS)
      RWLockUnlock(s_objectTxnLock);
	delete objects;
	nxlog_debug_tag(DEBUG_TAG_SYNC, 5, _T("Save objects completed"));
}

/**
 * Syncer thread
 */
THREAD_RESULT THREAD_CALL Syncer(void *arg)
{
   ThreadSetName("Syncer");

   int syncInterval = ConfigReadInt(_T("SyncInterval"), 60);
   UINT32 watchdogId = WatchdogAddThread(_T("Syncer Thread"), 30);

   nxlog_debug_tag(DEBUG_TAG_SYNC, 1, _T("Syncer thread started, sync_interval = %d"), syncInterval);

   // Main syncer loop
   WatchdogStartSleep(watchdogId);
   while(!SleepAndCheckForShutdown(syncInterval))
   {
      WatchdogNotify(watchdogId);
      nxlog_debug_tag(DEBUG_TAG_SYNC, 7, _T("Syncer wakeup"));
      if (!(g_flags & AF_DB_CONNECTION_LOST))    // Don't try to save if DB connection is lost
      {
         int64_t startTime = GetCurrentTimeMs();
         DB_HANDLE hdb = DBConnectionPoolAcquireConnection();
         SaveObjects(hdb, watchdogId, false);
         nxlog_debug_tag(DEBUG_TAG_SYNC, 5, _T("Saving user database"));
         SaveUsers(hdb, watchdogId);
         nxlog_debug_tag(DEBUG_TAG_SYNC, 5, _T("Saving NXSL persistent storage"));
         UpdatePStorageDatabase(hdb, watchdogId);
         DBConnectionPoolReleaseConnection(hdb);
         s_syncerGaugeLock.lock();
         s_syncerRunTime.update(GetCurrentTimeMs() - startTime);
         s_lastRunTime = static_cast<time_t>(startTime / 1000);
         s_syncerGaugeLock.unlock();
      }
      WatchdogStartSleep(watchdogId);
      nxlog_debug_tag(DEBUG_TAG_SYNC, 7, _T("Syncer run completed"));
   }

   nxlog_debug_tag(DEBUG_TAG_SYNC, 1, _T("Syncer thread terminated"));
   return THREAD_OK;
}
