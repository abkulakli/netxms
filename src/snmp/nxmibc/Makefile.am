# Copyright (C) 2004, 2005 NetXMS Team <bugs@netxms.org>
#  
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without 
# modifications, as long as this notice is preserved.
# 
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

bin_PROGRAMS = nxmibc
nxmibc_SOURCES = lex.parser.cpp mibparse.cpp nxmibc.cpp parser.tab.cpp
nxmibc_CPPFLAGS = -I@top_srcdir@/include -I@top_srcdir@/build
nxmibc_LDFLAGS = @EXEC_LDFLAGS@
nxmibc_LDADD = ../../libnetxms/libnetxms.la ../libnxsnmp/libnxsnmp.la @EXEC_LIBS@

if USE_INTERNAL_JANSSON
nxmibc_LDADD += @top_srcdir@/src/jansson/libnxjansson.la
else
nxmibc_LDADD += -ljansson
endif

EXTRA_DIST = nxmibc.vcxproj nxmibc.vcxproj.filters nxmibc.h mibparse.h parser.l parser.y parser.tab.hpp

lex.parser.cpp: parser.l parser.tab.cpp parser.tab.hpp
	$(LEX) -Pmp -B -f -olex.parser.cpp parser.l

parser.tab.hpp: parser.tab.cpp

parser.tab.cpp: parser.y
	$(YACC) -b parser -o parser.tab.cpp -d -p mp -t -v parser.y
