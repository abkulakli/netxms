/* 
** Project X - Network Management System
** Copyright (C) 2003 Victor Kirhenshtein
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
** $module: debug.cpp
**
**/

#include "nxcore.h"


//
// Test mutex state and print to stdout
//

void DbgTestMutex(MUTEX hMutex, TCHAR *szName, CONSOLE_CTX pCtx)
{
   ConsolePrintf(pCtx, _T("  %s: "), szName);
   if (MutexLock(hMutex, 100))
   {
      ConsolePrintf(pCtx, _T("unlocked\n"));
      MutexUnlock(hMutex);
   }
   else
   {
      ConsolePrintf(pCtx, _T("locked\n"));
   }
}


//
// Test read/write lock state and print to stdout
//

void DbgTestRWLock(RWLOCK hLock, TCHAR *szName, CONSOLE_CTX pCtx)
{
   ConsolePrintf(pCtx, _T("  %s: "), szName);
   if (RWLockWriteLock(hLock, 100))
   {
      ConsolePrintf(pCtx, _T("unlocked\n"));
      RWLockUnlock(hLock);
   }
   else
   {
      if (RWLockReadLock(hLock, 100))
      {
         ConsolePrintf(pCtx, _T("locked for reading\n"));
         RWLockUnlock(hLock);
      }
      else
      {
         ConsolePrintf(pCtx, _T("locked for writing\n"));
      }
   }
}


//
// Debug printf - write text to stdout if in standalone mode
// and specific application flag(s) is set
//

void DbgPrintf(DWORD dwFlags, TCHAR *szFormat, ...)
{
   va_list args;
   TCHAR szBuffer[1024];

   if (!(g_dwFlags & dwFlags))
      return;     // Required application flag(s) not set

   va_start(args, szFormat);
   _vsntprintf(szBuffer, 1024, szFormat, args);
   va_end(args);
   WriteLog(MSG_DEBUG, EVENTLOG_INFORMATION_TYPE, _T("s"), szBuffer);
}


//
// Print message to console, either local or remote
//

void ConsolePrintf(CONSOLE_CTX pCtx, char *pszFormat, ...)
{
   va_list args;

   va_start(args, pszFormat);
   if (pCtx->hSocket == -1)
   {
      vprintf(pszFormat, args);
   }
   else
   {
      CSCP_MESSAGE *pRawMsg;
      TCHAR szBuffer[1024];

      _vsntprintf(szBuffer, 1024, pszFormat, args);
      pCtx->pMsg->SetVariable(VID_MESSAGE, szBuffer);
      pRawMsg = pCtx->pMsg->CreateMessage();
      SendEx(pCtx->hSocket, pRawMsg, ntohl(pRawMsg->dwSize), 0);
      free(pRawMsg);
   }
   va_end(args);
}


//
// Show server statistics
//

void ShowServerStats(CONSOLE_CTX pCtx)
{
   DWORD i, dwNumItems;

   RWLockReadLock(g_rwlockNodeIndex, INFINITE);
   for(i = 0, dwNumItems = 0; i < g_dwNodeAddrIndexSize; i++)
      dwNumItems += ((Node *)g_pNodeIndexByAddr[i].pObject)->GetItemCount();
   RWLockUnlock(g_rwlockNodeIndex);

   ConsolePrintf(pCtx, "Total number of objects:   %ld\n"
                       "Number of monitored nodes: %ld\n"
                       "Number of collected DCIs:  %ld\n\n",
                 g_dwIdIndexSize, g_dwNodeAddrIndexSize, dwNumItems);
}
