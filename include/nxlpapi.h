/* 
** NetXMS - Network Management System
** Log Parsing Library
** Copyright (C) 2003-2010 Victor Kirhenshtein
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
** File: nxlpapi.h
**
**/

#ifndef _nxlpapi_h_
#define _nxlpapi_h_

#ifdef _WIN32
#ifdef LIBNXLP_EXPORTS
#define LIBNXLP_EXPORTABLE __declspec(dllexport)
#else
#define LIBNXLP_EXPORTABLE __declspec(dllimport)
#endif
#else    /* _WIN32 */
#define LIBNXLP_EXPORTABLE
#endif

#include <netxms-regex.h>


//
// Parser status
//

#define MAX_PARSER_STATUS_LEN	64

#define LPS_INIT              _T("INIT")
#define LPS_RUNNING           _T("RUNNING")
#define LPS_NO_FILE           _T("FILE MISSING")
#define LPS_OPEN_ERROR        _T("FILE OPEN ERROR")


//
// Context actions
//

#define CONTEXT_SET_MANUAL    0
#define CONTEXT_SET_AUTOMATIC 1
#define CONTEXT_CLEAR         2


//
// Callback
// Parameters:
//    event id, original text, number of parameters, list of parameters,
//    object id, user arg
//

typedef void (* LogParserCallback)(DWORD, const char *, int, char **, DWORD, void *);


//
// Log parser rule
//

class LIBNXLP_EXPORTABLE LogParser;

class LIBNXLP_EXPORTABLE LogParserRule
{
private:
	LogParser *m_parser;
	regex_t m_preg;
	DWORD m_event;
	bool m_isValid;
	int m_numParams;
	regmatch_t *m_pmatch;
	char *m_regexp;
	char *m_source;
	DWORD m_level;
	DWORD m_idStart;
	DWORD m_idEnd;
	char *m_context;
	int m_contextAction;
	char *m_contextToChange;
	bool m_isInverted;
	bool m_breakOnMatch;
	char *m_description;

	void expandMacros(const char *regexp, String &out);

public:
	LogParserRule(LogParser *parser,
	              const char *regexp, DWORD event = 0, int numParams = 0,
	              const char *source = NULL, DWORD level = 0xFFFFFFFF,
					  DWORD idStart = 0, DWORD idEnd = 0xFFFFFFFF);
	~LogParserRule();

	bool isValid() { return m_isValid; }
	bool match(const char *line, LogParserCallback cb, DWORD objectId, void *userArg);
	bool matchEx(const char *source, DWORD eventId, DWORD level,
	             const char *line, LogParserCallback cb, DWORD objectId, void *userArg); 
	
	void setContext(const char *context) { safe_free(m_context); m_context = (context != NULL) ? strdup(context) : NULL; }
	void setContextToChange(const char *context) { safe_free(m_contextToChange); m_contextToChange = (context != NULL) ? strdup(context) : NULL; }
	void setContextAction(int action) { m_contextAction = action; }

	void setInverted(bool flag) { m_isInverted = flag; }
	BOOL isInverted() { return m_isInverted; }

	void setBreakFlag(bool flag) { m_breakOnMatch = flag; }
	BOOL getBreakFlag() { return m_breakOnMatch; }

	const char *getContext() { return m_context; }
	const char *getContextToChange() { return m_contextToChange; }
	int getContextAction() { return m_contextAction; }

	void setDescription(const char *descr) { safe_free(m_description); m_description = (descr != NULL) ? strdup(descr) : NULL; }
	const char *getDescription() { return CHECK_NULL_EX_A(m_description); }

	void setSource(const char *source) { safe_free(m_source); m_source = (source != NULL) ? strdup(source) : NULL; }
	const char *getSource() { return CHECK_NULL_EX_A(m_source); }

	void setLevel(DWORD level) { m_level = level; }
	DWORD getLevel() { return m_level; }

	void setIdRange(DWORD start, DWORD end) { m_idStart = start; m_idEnd = end; }
	QWORD getIdRange() { return ((QWORD)m_idStart << 32) | (QWORD)m_idEnd; }

	const char *getRegexpSource() { return CHECK_NULL_A(m_regexp); }
};


//
// Log parser class
//

class LIBNXLP_EXPORTABLE LogParser
{
	friend bool LogParserRule::match(const char *, LogParserCallback, DWORD, void *);
	friend bool LogParserRule::matchEx(const char *, DWORD, DWORD, const char *, LogParserCallback, DWORD, void *);

private:
	int m_numRules;
	LogParserRule **m_rules;
	StringMap m_contexts;
	StringMap m_macros;
	LogParserCallback m_cb;
	void *m_userArg;
	char *m_fileName;
	char *m_name;
	CODE_TO_TEXT *m_eventNameList;
	bool (*m_eventResolver)(const char *, DWORD *);
	THREAD m_thread;	// Associated thread
	int m_recordsProcessed;
	int m_recordsMatched;
	bool m_processAllRules;
	int m_traceLevel;
	void (*m_traceCallback)(const char *, va_list);
	TCHAR m_status[MAX_PARSER_STATUS_LEN];
	
	const char *checkContext(LogParserRule *rule);
	void trace(int level, const char *format, ...);
	bool matchLogRecord(bool hasAttributes, const char *source, DWORD eventId, DWORD level, const char *line, DWORD objectId);

public:
	LogParser();
	~LogParser();
	
	bool createFromXml(const char *xml, int xmlLen = -1, char *errorText = NULL, int errBufSize = 0);

	void setFileName(const char *name);
	const char *getFileName() { return m_fileName; }

	void setName(const char *name);
	const char *getName() { return m_name; }

	void setStatus(const TCHAR *status) { nx_strncpy(m_status, status, MAX_PARSER_STATUS_LEN); }
	const TCHAR *getStatus() { return m_status; }

	void setThread(THREAD th) { m_thread = th; }
	THREAD getThread() { return m_thread; }

	void setProcessAllFlag(bool flag) { m_processAllRules = flag; }
	bool getProcessAllFlag() { return m_processAllRules; }

	bool addRule(const char *regexp, DWORD event = 0, int numParams = 0);
	bool addRule(LogParserRule *rule);
	void setCallback(LogParserCallback cb) { m_cb = cb; }
	void setUserArg(void *arg) { m_userArg = arg; }
	void setEventNameList(CODE_TO_TEXT *ctt) { m_eventNameList = ctt; }
	void setEventNameResolver(bool (*cb)(const char *, DWORD *)) { m_eventResolver = cb; }
	DWORD resolveEventName(const char *name, DWORD defVal = 0);

	void addMacro(const char *name, const char *value);
	const char *getMacro(const char *name);

	bool matchLine(const char *line, DWORD objectId = 0);
	bool matchEvent(const char *source, DWORD eventId, DWORD level, const char *line, DWORD objectId = 0);

	int getProcessedRecordsCount() { return m_recordsProcessed; }
	int getMatchedRecordsCount() { return m_recordsMatched; }

	int getTraceLevel() { return m_traceLevel; }
	void setTraceLevel(int level) { m_traceLevel = level; }
	void setTraceCallback(void (*cb)(const char *, va_list)) { m_traceCallback = cb; }

	bool monitorFile(CONDITION stopCondition, void (*logger)(int, const char *, ...), bool readFromCurrPos = true);
};

#endif
