/* $Id: hpux.h,v 1.1 2006-10-04 14:59:14 alk Exp $ */

/*
** NetXMS subagent for HP-UX
** Copyright (C) 2006 Alex Kirhenshtein
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
**/

#ifndef _hpux_subagent_h_
#define _hpux_subagent_h_

#include <nms_common.h>
#include <nms_agent.h>
#include <nms_threads.h>

#include "net.h"
#include "system.h"
#include "disk.h"


//
// Request types for H_MemoryInfo
//

enum
{
	MEMINFO_PHYSICAL_FREE,
	MEMINFO_PHYSICAL_TOTAL,
	MEMINFO_PHYSICAL_USED,
	MEMINFO_SWAP_FREE,
	MEMINFO_SWAP_TOTAL,
	MEMINFO_SWAP_USED,
	MEMINFO_VIRTUAL_FREE,
	MEMINFO_VIRTUAL_TOTAL,
	MEMINFO_VIRTUAL_USED
};


//
// Types for Process.XXX() parameters
//

enum
{
	PROCINFO_IO_READ_B,
	PROCINFO_IO_READ_OP,
	PROCINFO_IO_WRITE_B,
	PROCINFO_IO_WRITE_OP,
	PROCINFO_KTIME,
	PROCINFO_PF,
	PROCINFO_UTIME,
	PROCINFO_VMSIZE,
	PROCINFO_WKSET
};


//
// Process list entry structure
//

typedef struct t_ProcEnt
{
	unsigned int nPid;
	char szProcName[128];
} PROC_ENT;


//
// Global variables
//

extern BOOL g_bShutdown;


#endif

///////////////////////////////////////////////////////////////////////////////
/*

$Log: not supported by cvs2svn $

*/
