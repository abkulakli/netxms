/*
** NetXMS - Network Management System
** Copyright (C) 2003-2021 Victor Kirhenshtein
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
** File: nms_common.h
**
**/

#ifndef _nms_common_h_
#define _nms_common_h_

#ifdef WIN32
#ifndef _WIN32
#define _WIN32
#endif
#endif

#if !defined(_WIN32)

#include <config.h>
#if defined(WITH_OPENSSL)
#define _WITH_ENCRYPTION   1
#endif

#else    /* _WIN32 */

#define _WITH_ENCRYPTION   1
#define WITH_OPENSSL       1
#define WITH_LDAP          1
#define VA_LIST_IS_POINTER 1

#if !defined(WINDOWS_ONLY) && !defined(_CRT_SECURE_NO_DEPRECATE)
#define _CRT_SECURE_NO_DEPRECATE
#endif

// Turn off C++ exceptions use in standard header files
#ifndef _HAS_EXCEPTIONS
#define _HAS_EXCEPTIONS    0
#endif

// prevent defining min and max macros in Windows headers
#ifndef NOMINMAX
#define NOMINMAX
#endif

// prevent defining ETIMEDOUT, ECONNRESET, etc. to wrong values
#ifndef ALLOW_CRT_POSIX_ERROR_CODES
#define _CRT_NO_POSIX_ERROR_CODES
#endif

#ifndef _CRT_SECURE_NO_WARNINGS
#define _CRT_SECURE_NO_WARNINGS
#endif

#endif   /* _WIN32 */

#if HAVE_JEMALLOC_JEMALLOC_H
#include <jemalloc/jemalloc.h>
#endif

#ifdef HAVE_STDARG_H
#include <stdarg.h>
#endif

#include <unicode.h>

#include <stdio.h>
#include <string.h>
#include <limits.h>
#include <assert.h>

#if HAVE_WIDEC_H
#include <widec.h>
#endif /* HAVE_WIDEC */

#include <symbol_visibility.h>

/******* export attribute for libnetxms symbols *******/
#ifdef LIBNETXMS_EXPORTS
#define LIBNETXMS_EXPORTABLE __EXPORT
#define LIBNETXMS_EXPORTABLE_VAR(v) __EXPORT_VAR(v)
#else
#define LIBNETXMS_EXPORTABLE __IMPORT
#define LIBNETXMS_EXPORTABLE_VAR(v) __IMPORT_VAR(v)
#endif

/**
 * Define __64BIT__ if compiling for 64bit platform with Visual C++
 */
#if defined(_M_X64) || defined(_M_IA64) || defined(__LP64__) || defined(__PPC64__) || defined(__x86_64__)
#ifndef __64BIT__
#define __64BIT__
#endif
#endif

/**
 * Common constants
 */
#define MAX_SECRET_LENGTH        89
#define MAX_DB_STRING            256
#define MAX_PARAM_NAME           256
#define MAX_CONFIG_VALUE         2000
#define MAX_COLUMN_NAME          64
#define MAX_DNS_NAME             256
#define MAX_HELPDESK_REF_LEN     64
#define MAX_PASSWORD             256
#define MAX_SSH_LOGIN_LEN        64
#define MAX_SSH_PASSWORD_LEN     64
#define GROUP_FLAG               ((uint32_t)0x40000000)

#define NETXMS_MAX_CIPHERS       6
#define NETXMS_RSA_KEYLEN        2048

#ifndef EVENTLOG_DEBUG_TYPE
#define EVENTLOG_DEBUG_TYPE		0x0080
#endif

#define INVALID_INDEX         0xFFFFFFFF
#define MD5_DIGEST_SIZE       16
#define SHA1_DIGEST_SIZE      20
#define SHA224_DIGEST_SIZE    28
#define SHA256_DIGEST_SIZE    32
#define SHA384_DIGEST_SIZE    48
#define SHA512_DIGEST_SIZE    64

#define FILE_BUFFER_SIZE      ((size_t)32768)

/**
 * Compatibility defines for C sources
 */
#if !defined(__cplusplus) && !defined(CORTEX) && !HAVE_BOOL
typedef int bool;
#endif

/**
 * Oracle Pro*C compatibility
 */
#ifdef ORA_PROC
#undef BYTE
#undef DWORD
#endif

/**
 * Java class path separator character
 */
#ifdef _WIN32
#define JAVA_CLASSPATH_SEPARATOR   _T(';')
#else
#define JAVA_CLASSPATH_SEPARATOR   _T(':')
#endif

/**
 * Hint for zlib to use "const" qualifier
 */
#define ZLIB_CONST


/***** Platform dependent includes and defines *****/

#if defined(_WIN32)

/********** WINDOWS ********************/

#ifdef _MSC_FULL_VER
#define __BUILD_VERSION_STRING(s,v) s v
#define __STR_NX(x) #x
#define __STR(x) __STR_NX(x)
#define CPP_COMPILER_VERSION __STR(__BUILD_VERSION_STRING(Microsoft C/C++ Optimizing Compiler Version,_MSC_FULL_VER))
#endif

#define _Noreturn __declspec(noreturn)

#define CAN_DELETE_COPY_CTOR      1
#define HAVE_FINAL_SPECIFIER      1
#define HAVE_OVERRIDE_SPECIFIER   1
#define HAVE_THREAD_LOCAL_STORAGE 1
#define HAVE_STD_MAKE_UNIQUE      1

// Disable some warnings:
//   4293 - 'function xxx' not available as intrinsic function
//   4530 - C++ exception handler used, but unwind semantics are not enabled
//   4577 - 'noexcept' used with no exception handling mode specified
#pragma warning(disable: 4293)
#pragma warning(disable: 4530)
#pragma warning(disable: 4577)

// Set API version to Windows 7
#ifndef _WIN32_WINNT
#define _WIN32_WINNT		0x0601
#endif

/**
 * Epoch offset for FILETIME structure
 */
#define EPOCHFILETIME           (116444736000000000i64)

#define WITH_IPV6               1
#define WITH_PYTHON             1

#define USE_BUNDLED_LIBTRE      1
#define USE_BUNDLED_GETOPT      1

#define SAFE_FGETWS_WITH_POPEN  1

#define FREE_IS_NULL_SAFE       1

#define FS_PATH_SEPARATOR           _T("\\")
#define FS_PATH_SEPARATOR_A         "\\"
#define FS_PATH_SEPARATOR_W         L"\\"
#define FS_PATH_SEPARATOR_CHAR      _T('\\')
#define FS_PATH_SEPARATOR_CHAR_A    '\\'
#define FS_PATH_SEPARATOR_CHAR_W    L'\\'

#define WEXITSTATUS(x)          (x)

#if _MSC_VER >= 1310
#define HAVE_SCPRINTF           1
#define HAVE_VSCPRINTF          1
#define HAVE_SCWPRINTF          1
#define HAVE_VSCWPRINTF         1
#endif

#if _MSC_VER >= 1800
#define HAVE_STRTOLL            1
#define HAVE_STRTOULL           1
#define HAVE_WCSTOLL            1
#define HAVE_WCSTOULL           1
#endif

#define HAVE_TOUPPER            1
#define HAVE_TOWUPPER           1
#define HAVE_TOLOWER            1
#define HAVE_TOWLOWER           1

#define HAVE_SNPRINTF           1
#define HAVE_VSNPRINTF          1

#define HAVE_GETADDRINFO        1

#define HAVE_STDARG_H           1

#define HAVE_ALLOCA             1

#define HAVE_WCSLEN             1
#define HAVE_WCSNCPY            1
#define HAVE_WCSDUP             1
#define HAVE_WUTIME             1

#define HAVE_STD_ATOMIC         1

#define HAVE_LIBCURL                      1
#define HAVE_LIBJQ                        1

#define HAVE_DIRENT_D_TYPE                1

#define HAVE_MOSQUITTO_THREADED_SET       1

#define HAVE_X509_STORE_SET_VERIFY_CB     1
#define HAVE_X509_STORE_CTX_SET_VERIFY_CB 1

#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>
#include <stdlib.h>
#include <malloc.h>

#include <stdarg.h>
#ifndef va_copy
#define va_copy(x,y)            (x = y)
#endif
#define HAVE_DECL_VA_COPY       1

#include <sys/stat.h>
#include <sys/utime.h>
#include <process.h>
#include <io.h>
#include <fcntl.h>
#include <direct.h>

#ifndef S_IRUSR
#define S_IRUSR      _S_IREAD
#endif
#ifndef S_IWUSR
#define S_IWUSR      _S_IWRITE
#endif
#ifndef S_IRGRP
#define S_IRGRP      _S_IREAD
#endif
#ifndef S_IWGRP
#define S_IWGRP      _S_IWRITE
#endif
#ifndef S_IROTH
#define S_IROTH      _S_IREAD
#endif
#ifndef S_IWOTH
#define S_IWOTH      _S_IWRITE
#endif

/* Mode flags for _access() */
#define F_OK   0
#define R_OK   4
#define W_OK   2
#define X_OK   R_OK  /* on Windows execute is granted if read is granted */

#define STDIN_FILENO    _fileno(stdin)
#define STDOUT_FILENO   _fileno(stdout)
#define STDERR_FILENO   _fileno(stderr)

#define snprintf     _snprintf
#define vsnprintf    _vsnprintf
#define snwprintf    _snwprintf
#define vsnwprintf   _vsnwprintf
#define scprintf     _scprintf
#define vscprintf    _vscprintf
#define scwprintf    _scwprintf
#define vscwprintf   _vscwprintf
#define stricmp      _stricmp
#define strnicmp     _strnicmp
#define wcsicmp      _wcsicmp
#define wcsnicmp     _wcsnicmp
#define strlwr(s)    _strlwr(s)
#define strupr(s)    _strupr(s)

typedef UINT64 QWORD;   // for compatibility
typedef int socklen_t;
typedef long pid_t;

#ifdef _WIN64
typedef __int64 ssize_t;
#else
typedef int ssize_t;
#endif

typedef __int64 off64_t;

typedef signed __int8 int8_t;
typedef signed __int16 int16_t;
typedef signed __int32 int32_t;
typedef signed __int64 int64_t;
typedef unsigned __int8 uint8_t;
typedef unsigned __int16 uint16_t;
typedef unsigned __int32 uint32_t;
typedef unsigned __int64 uint64_t;

#define INT64_FMT			_T("%I64d")
#define INT64_FMTA		"%I64d"
#define INT64_FMTW		L"%I64d"
#define UINT64_FMT		_T("%I64u")
#define UINT64_FMTA		"%I64u"
#define UINT64_FMTW		L"%I64u"
#define UINT64_FMT_ARGS(m)	_T("%") m _T("I64u")
#define UINT64X_FMT(m)  _T("%") m _T("I64X")
#if defined(__64BIT__) || (_MSC_VER > 1300)
#define TIME_T_FMT      _T("%I64u")
#define TIME_T_FCAST(x) ((UINT64)(x))
#else
#define TIME_T_FMT      _T("%u")
#define TIME_T_FCAST(x) ((UINT32)(x))
#endif

#ifndef __clang__

#define HAVE_DECL_BSWAP_16 1
#define HAVE_DECL_BSWAP_32 1
#define HAVE_DECL_BSWAP_64 1

#define bswap_16(n)  _byteswap_ushort(n)
#define bswap_32(n)  _byteswap_ulong(n)
#define bswap_64(n)  _byteswap_uint64(n)

#endif

#define HAVE_LIBEXPAT  1
#define XMPP_SUPPORTED 1
#define HAVE_LOCALE_H  1
#define HAVE_SETLOCALE 1

#ifdef __cplusplus

/**
 * Get UNIX time from Windows file time represented as 64 bit integer
 */
inline time_t FileTimeToUnixTime(uint64_t ft)
{
   return (ft > 0) ? static_cast<time_t>((ft - EPOCHFILETIME) / 10000000) : 0;
}

/**
 * Get UNIX time from Windows file time represented as LARGE_INTEGER structure
 */
inline time_t FileTimeToUnixTime(LARGE_INTEGER li)
{
   return (li.QuadPart > 0) ? static_cast<time_t>((li.QuadPart - EPOCHFILETIME) / 10000000) : 0;
}

/**
 * Get UNIX time from Windows file time represented as FILETIME structure
 */
inline time_t FileTimeToUnixTime(const FILETIME &ft)
{
   LARGE_INTEGER li;
   li.LowPart = ft.dwLowDateTime;
   li.HighPart = ft.dwHighDateTime;
   return (li.QuadPart > 0) ? static_cast<time_t>((li.QuadPart - EPOCHFILETIME) / 10000000) : 0;
}

#endif

#else    /* not _WIN32 */

/*********** UNIX *********************/

#ifndef PREFIX
#ifdef UNICODE
#define PREFIX  L"/usr/local"
#else
#define PREFIX  "/usr/local"
#endif
#warning Installation prefix not defined, defaulting to /usr/local
#endif

#ifndef SYSCONFDIR
#ifdef UNICODE
#define SYSCONFDIR   PREFIX L"/etc"
#else
#define SYSCONFDIR   PREFIX "/etc"
#endif
#warning SYSCONFDIR not defined, defaulting to $prefix/etc
#endif

#if HAVE_THREAD_LOCAL_SPECIFIER
#define HAVE_THREAD_LOCAL_STORAGE 1
#elif HAVE_THREAD_SPECIFIER
#define thread_local __thread
#define HAVE_THREAD_LOCAL_STORAGE 1
#else
#define thread_local
#define HAVE_THREAD_LOCAL_STORAGE 0
#endif

#if HAVE_STDBOOL_H
#include <stdbool.h>
#endif

#if HAVE_ALLOCA_H
#include <alloca.h>
#endif

#if HAVE_WCHAR_H
#include <wchar.h>
#endif

#if HAVE_WCTYPE_H
#include <wctype.h>
#endif

// Fix for wcs* functions visibility in Solaris 11
#if defined(__sun) && (__cplusplus >= 199711L)
#if HAVE_WCSDUP
using std::wcsdup;
#endif
#if HAVE_WCSCASECMP
using std::wcscasecmp;
#endif
#if HAVE_WCSNCASECMP
using std::wcsncasecmp;
#endif
#endif

#include <errno.h>

#define FS_PATH_SEPARATOR           _T("/")
#define FS_PATH_SEPARATOR_A         "/"
#define FS_PATH_SEPARATOR_W         L"/"
#define FS_PATH_SEPARATOR_CHAR      _T('/')
#define FS_PATH_SEPARATOR_CHAR_A    '/'
#define FS_PATH_SEPARATOR_CHAR_W    L'/'

#if HAVE_UNISTD_H
#include <unistd.h>
#endif

#if HAVE_STDLIB_H
#include <stdlib.h>
#endif

#if HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif

#if HAVE_INTTYPES_H
#include <inttypes.h>
#endif

#if HAVE_STDINT_H
#include <stdint.h>
#endif

#if HAVE_UTIME_H
#include <utime.h>
#endif

#if HAVE_SYS_INT_TYPES_H
#include <sys/int_types.h>
#endif

#if HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif

#if HAVE_SYS_SOCKET_H
#include <sys/socket.h>
#endif

#if HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif

#if HAVE_NETINET_IN_H
#include <netinet/in.h>
#endif

#if HAVE_NETINET_TCP_H
#include <netinet/tcp.h>
#endif

#if HAVE_NET_NH_H
#include <net/nh.h>
#endif

#if HAVE_ARPA_INET_H
#include <arpa/inet.h>
#endif

#if HAVE_NETDB_H
#include <netdb.h>
#endif

#if HAVE_FCNTL_H
#include <fcntl.h>
#endif

#include <dirent.h>

#if _USE_GNU_PTH
#include <pth.h>
#else
#include <pthread.h>
#endif

#ifdef __IBMCPP__
#include <builtins.h>
#endif

#ifdef __minix
#undef HAVE_ITOA  /* Minix has non-compatible itoa() */
#undef HAVE_GETHOSTBYNAME2_R
#endif

#include <sys/socket.h>
#include <sys/un.h>

#if !HAVE_INT64_T || !HAVE_UINT64_T

// We have to use long as INT64 on HP-UX - otherwise
// there will be compilation errors because of type redefinition in
// system includes
#ifdef _HPUX

#ifdef __LP64__
#if !HAVE_INT64_T
typedef long int64_t;
#endif
#if !HAVE_UINT64_T
typedef unsigned long uint64_t;
#endif
#else
#if !HAVE_INT64_T
typedef long long int64_t;
#endif
#if !HAVE_UINT64_T
typedef unsigned long long uint64_t;
#endif
#endif

#else    /* _HPUX */

#if !HAVE_INT64_T
#if HAVE_LONG_LONG && (SIZEOF_LONG_LONG == 8)
typedef long long int64_t;
#elif SIZEOF_LONG == 8
typedef long int64_t;
#else
#error Target system does not have signed 64bit integer type
#endif
#endif

#if !HAVE_UINT64_T
#if HAVE_UNSIGNED_LONG_LONG && (SIZEOF_LONG_LONG == 8)
typedef unsigned long long uint64_t;
#elif HAVE_U_INT64_T
typedef u_int64_t uint64_t;
#elif SIZEOF_LONG == 8
typedef unsigned long uint64_t;
#else
#error Target system does not have unsigned 64bit integer type
#endif
#endif

#endif   /* _HPUX */

#endif

#if (SIZEOF_LONG == 4)
#if (SIZEOF_INT == 4)
#if !HAVE_INT32_T
typedef int int32_t;
#endif
#if !HAVE_UINT32_T
typedef unsigned int uint32_t;
#endif
#else
#if !HAVE_INT32_T
typedef long int32_t;
#endif
#if !HAVE_UINT32_T
typedef unsigned long uint32_t;
#endif
#endif
#undef __64BIT__
#else
#if !HAVE_INT32_T
typedef int int32_t;
#endif
#if !HAVE_UINT32_T
typedef unsigned int uint32_t;
#endif
#ifndef __64BIT__
#define __64BIT__
#endif
#endif

#if !HAVE_INT16_T
typedef short int16_t;
#endif
#if !HAVE_UINT16_T
typedef unsigned short uint16_t;
#endif

#if !HAVE_INT8_T
typedef char int8_t;
#endif
#if !HAVE_UINT8_T
typedef unsigned char uint8_t;
#endif

#if !HAVE_INTPTR_T
#if SIZEOF_VOIDP == 8
typedef int64_t intptr_t;
#else
typedef int32_t intptr_t;
#endif
#endif

#if !HAVE_UINTPTR_T
#if SIZEOF_VOIDP == 8
typedef uint64_t uintptr_t;
#else
typedef uint32_t uintptr_t;
#endif
#endif

#if !HAVE_NULLPTR
#define nullptr NULL
#endif

/* Deprectaed compatibility types - to be removed */
typedef uint8_t BYTE;
typedef int16_t INT16;
typedef uint16_t UINT16;
typedef uint16_t WORD;
typedef int32_t INT32;
typedef int32_t LONG;
typedef uint32_t UINT32;
typedef uint32_t DWORD;
typedef int64_t INT64;
typedef uint64_t UINT64;
typedef uint64_t QWORD;

typedef void * HANDLE;
typedef void * HINSTANCE;
typedef void * HMODULE;

#if !HAVE_MODE_T
typedef int mode_t;
#endif

#if !HAVE_OFF64_T
typedef int64_t off64_t;
#endif

#if defined(PRId64)
#define INT64_FMT       _T("%") PRId64
#define INT64_FMTW      L"%" PRId64
#define INT64_FMTA      "%" PRId64
#elif SIZEOF_LONG == 8
#define INT64_FMT		_T("%ld")
#define INT64_FMTW		L"%ld"
#define INT64_FMTA		"%ld"
#else
#define INT64_FMT		_T("%lld")
#define INT64_FMTW		L"%lld"
#define INT64_FMTA		"%lld"
#endif

#if defined(PRIu64)
#define UINT64_FMT       _T("%") PRIu64
#define UINT64_FMTW      L"%" PRIu64
#define UINT64_FMTA      "%" PRIu64
#define UINT64_FMT_ARGS(m) _T("%") m PRIu64
#elif SIZEOF_LONG == 8
#define UINT64_FMT		_T("%lu")
#define UINT64_FMTW		L"%lu"
#define UINT64_FMTA		"%lu"
#define UINT64_FMT_ARGS(m)	_T("%") m _T("lu")
#else
#define UINT64_FMT		_T("%llu")
#define UINT64_FMTW		L"%llu"
#define UINT64_FMTA		"%llu"
#define UINT64_FMT_ARGS(m)	_T("%") m _T("llu")
#endif

#if defined(PRIX64)
#define UINT64X_FMT(m)  _T("%") m PRIX64
#elif SIZEOF_LONG == 8
#define UINT64X_FMT(m)  _T("%") m _T("lX")
#else
#define UINT64X_FMT(m)  _T("%") m _T("llX")
#endif

#ifdef __64BIT__
#define TIME_T_FMT		UINT64_FMT
#define TIME_T_FCAST(x)         ((uint64_t)(x))
#else
#define TIME_T_FMT		_T("%u")
#define TIME_T_FCAST(x)         ((uint32_t)(x))
#endif

typedef int BOOL;

#ifndef TRUE
#define TRUE   1
#endif
#ifndef FALSE
#define FALSE  0
#endif

// on AIX open_memstream only declared if _XOPEN_SOURCE >= 700
// which is incompatible with other parts of code
#ifdef _AIX
#ifdef __cplusplus
extern "C"
#endif
FILE *open_memstream(char **, size_t *);
#endif

// Some systems may define true and false which may break overloaded functions
#ifdef __cplusplus
#undef true
#undef false
#endif

#ifndef MAX_PATH
#ifdef PATH_MAX
#define MAX_PATH PATH_MAX
#else
#define MAX_PATH 1024
#endif
#endif

// Windows compatibility layer for standard C I/O functions
#ifdef __cplusplus

inline int _access(const char *pathname, int mode) { return ::access(pathname, mode); }
inline int _chdir(const char *path) { return ::chdir(path);  }
inline int _close(int fd) { return ::close(fd); }
inline int _fileno(FILE *stream) { return fileno(stream); }
inline char *_getcwd(char *buffer, size_t size) { return ::getcwd(buffer, size); }
inline int _isatty(int fd) { return ::isatty(fd); }
inline off_t _lseek(int fd, off_t offset, int whence) { return ::lseek(fd, offset, whence);  }
inline int _mkdir(const char *pathname, mode_t mode) { return ::mkdir(pathname, mode); }
inline int _open(const char *pathname, int flags) { return ::open(pathname, flags); }
inline int _open(const char *pathname, int flags, mode_t mode) { return ::open(pathname, flags, mode); }
inline int _pclose(FILE *stream) { return ::pclose(stream); }
inline FILE *_popen(const char *command, const char *type) { return ::popen(command, type); }
inline int _putenv(char *string) { return ::putenv(string); }
inline ssize_t _read(int fd, void *buf, size_t count) { return ::read(fd, buf, count); }
inline int _remove(const char *pathname) { return ::remove(pathname); }
inline char *_strdup(const char *s) { return ::strdup(s); }
inline int _unlink(const char *pathname) { return ::unlink(pathname); }
inline ssize_t _write(int fd, const void *buf, size_t count) { return ::write(fd, buf, count); }

#else

#define _access(p, m)      access((p), (m))
#define _chdir(p)          chdir(p)
#define _close(f)          close(f)
#define _fileno(f)         fileno(f)
#define _getcwd(b, s)      getcwd((b), (s))
#define _isatty(f)         isatty(f)
#define _lseek(f, o, w)    lseek((f), (o) ,(w))
#define _mkdir(p, m)       mkdir((p), (m))
#define _open              open
#define _pclose(f)         pclose(f)
#define _popen(c, m)       popen((c), (m))
#define _putenv(s)         putenv(s)
#define _read(f, b, l)     read((f), (b), (l))
#define _remove(f)         remove(f)
#define _unlink(f)         unlink(f)
#define _write(f, b, l)    write((f), (b), (l))

#endif

// Shared library suffix
#ifndef SHLIB_SUFFIX
#if defined(_HPUX) && !defined(__64BIT__)
#define SHLIB_SUFFIX	_T(".sl")
#else
#define SHLIB_SUFFIX	_T(".so")
#endif
#endif

typedef struct hostent HOSTENT;

#if HAVE_DECL___VA_COPY && !HAVE_DECL_VA_COPY
#define HAVE_DECL_VA_COPY 1
#define va_copy(d,s) __va_copy(d,s)
#endif

#endif   /* _WIN32 */

/**
 * Wrappers for 64-bit integer constants
 */
#if defined(__GNUC__) || defined(__HP_aCC) || defined(__IBMC__) || defined(__IBMCPP__) || defined(__SUNPRO_C) || defined(__SUNPRO_CC)
#ifdef INT64_C
#define _LL(x) INT64_C(x)
#else
#define _LL(x) (x ## LL)
#endif
#ifdef UINT64_C
#define _ULL(x) UINT64_C(x)
#else
#define _ULL(x) (x ## ULL)
#endif
#elif defined(_MSC_VER)
#define _LL(x) (x ## i64)
#define _ULL(x) (x ## ui64)
#else
#define _LL(x) (x)
#define _ULL(x) (x)
#endif

#ifndef LLONG_MAX
#define LLONG_MAX    _LL(9223372036854775807)
#endif

#ifndef LLONG_MIN
#define LLONG_MIN    (-LLONG_MAX - 1)
#endif

#ifndef ULLONG_MAX
#define ULLONG_MAX   _ULL(18446744073709551615)
#endif

/**
 * Value used to indicate invalid pointer where NULL is not appropriate
 */
#ifdef __64BIT__
#define INVALID_POINTER_VALUE    ((void *)_ULL(0xFFFFFFFFFFFFFFFF))
#else
#define INVALID_POINTER_VALUE    ((void *)0xFFFFFFFF)
#endif

/**
 * Casting between pointer and integer
 */
#define CAST_FROM_POINTER(p, t) ((t)((uintptr_t)(p)))
#define CAST_TO_POINTER(v, t) ((t)((uintptr_t)(v)))

/**
 * Macros for defining bit masks
 */
#define MASK_BIT(n)     static_cast<uint32_t>(1 << (n))
#define MASK_BIT64(n)   static_cast<uint64_t>(_ULL(1) << (n))

/**
 * open() flags compatibility
 */
#ifndef O_BINARY
#define O_BINARY 0
#endif

/**
 * Windows-specific structures for non-Windows platforms
 */
#ifndef _WIN32

typedef struct tagPOINT
{
   int x;
   int y;
} POINT;

#endif

/**
 * Event log severity codes
 */
#ifndef _WIN32
#define EVENTLOG_SUCCESS                0x0000
#define EVENTLOG_ERROR_TYPE             0x0001
#define EVENTLOG_WARNING_TYPE           0x0002
#define EVENTLOG_INFORMATION_TYPE       0x0004
#define EVENTLOG_AUDIT_SUCCESS          0x0008
#define EVENTLOG_AUDIT_FAILURE          0x0010
#endif   /* _WIN32 */

#define NXLOG_DEBUG     EVENTLOG_DEBUG_TYPE
#define NXLOG_INFO      EVENTLOG_INFORMATION_TYPE
#define NXLOG_WARNING   EVENTLOG_WARNING_TYPE
#define NXLOG_ERROR     EVENTLOG_ERROR_TYPE

/**
 * INADDR_NONE can be undefined on some systems
 */
#ifndef INADDR_NONE
#define INADDR_NONE	(0xFFFFFFFF)
#endif

/**
 * Check if given string is NULL and always return valid pointer
 */
#ifdef __cplusplus
static inline const TCHAR *CHECK_NULL(const TCHAR *x) { return (x == nullptr) ? _T("(null)") : x; }
static inline const char *CHECK_NULL_A(const char *x) { return (x == nullptr) ? "(null)" : x; }
static inline const WCHAR *CHECK_NULL_W(const WCHAR *x) { return (x == nullptr) ? L"(null)" : x; }
static inline const TCHAR *CHECK_NULL_EX(const TCHAR *x) { return (x == nullptr) ? _T("") : x; }
static inline const char *CHECK_NULL_EX_A(const char *x) { return (x == nullptr) ? "" : x; }
static inline const WCHAR *CHECK_NULL_EX_W(const WCHAR *x) { return (x == nullptr) ? L"" : x; }
#else
#define CHECK_NULL(x)      ((x) == NULL ? _T("(null)") : (x))
#define CHECK_NULL_A(x)    ((x) == NULL ? "(null)" : (x))
#define CHECK_NULL_W(x)    ((x) == NULL ? L"(null)" : (x))
#define CHECK_NULL_EX(x)   ((x) == NULL ? _T("") : (x))
#define CHECK_NULL_EX_A(x) ((x) == NULL ? "" : (x))
#define CHECK_NULL_EX_W(x) ((x) == NULL ? L"" : (x))
#endif

/***** Common C++ includes *****/

#ifdef __cplusplus

#if defined(__IBMCPP_TR1__)

#include <memory>
using std::tr1::shared_ptr;
using std::tr1::weak_ptr;
using std::tr1::static_pointer_cast;
using std::tr1::enable_shared_from_this;

// xlC++ implementation C++11 TR1 does not have make_shared
template<typename T, typename... Args>
inline shared_ptr<T> make_shared(Args&&... args)
{
   return shared_ptr<T>(new T(args...));
}

// xlC++ implementation C++11 TR1 does not have unique_ptr
#define unique_ptr std::auto_ptr

#if HAVE_STD_NULLPTR_T
using std::nullptr_t;
#else
typedef decltype(nullptr) nullptr_t;
#endif

// Implementation of == and != for comparing unique_ptr with nullptr_t
template<typename T> bool operator ==(const unique_ptr<T> &a, const nullptr_t &b)
{
        return a.get() == nullptr;
}

template<typename T> bool operator !=(const unique_ptr<T> &a, const nullptr_t &b)
{
        return a.get() != nullptr;
}

#elif defined(__HP_aCC)

// HP aC++ does not have shared_ptr implementation, use bundled one
#include "nx_shared_ptr.h"

#elif !defined(__clang__) && defined(__GNUC__) && (__GNUC__ < 4 || (__GNUC__ == 4 && __GNUC_MINOR__ < 8))

// GCC implementation before 4.8 does not work with RTTI disabled, use bundled one
#include "nx_shared_ptr.h"

#else

#include <memory>
using std::shared_ptr;
using std::weak_ptr;
using std::make_shared;
using std::static_pointer_cast;
using std::enable_shared_from_this;
using std::unique_ptr;

#endif

#if HAVE_STD_MAKE_UNIQUE
using std::make_unique;
#else
template<class T, class... Args> unique_ptr<T> make_unique(Args&&... args)
{
#if HAVE_STD_FORWARD
   return unique_ptr<T>(new T(std::forward<Args>(args)...));
#else
   return unique_ptr<T>(new T(args...));
#endif
}
#endif

#endif	/* __cplusplus */


/**
 * Heap functions
 */
#ifdef __cplusplus

static inline void *MemAlloc(size_t size) { return malloc(size); }
static inline void *MemAllocZeroed(size_t size) { return calloc(size, 1); }
static inline char *MemAllocStringA(size_t size) { return static_cast<char*>(MemAlloc(size)); }
static inline WCHAR *MemAllocStringW(size_t size) { return static_cast<WCHAR*>(MemAlloc(size * sizeof(WCHAR))); }
template <typename T> static inline T *MemAllocStruct() { return (T*)calloc(1, sizeof(T)); }
template <typename T> static inline T *MemAllocArray(size_t count) { return (T*)calloc(count, sizeof(T)); }
template <typename T> static inline T *MemAllocArrayNoInit(size_t count) { return (T*)MemAlloc(count * sizeof(T)); }
template <typename T> static inline T *MemRealloc(T *p, size_t size) { return (T*)realloc(p, size); }
template <typename T> static inline T *MemReallocArray(T *p, size_t count) { return (T*)realloc(p, count * sizeof(T)); }
#if FREE_IS_NULL_SAFE
static inline void MemFree(void *p) { free(p); }
template <typename T> static inline void MemFreeAndNull(T* &p) { free(p); p = nullptr; }
#else
static inline void MemFree(void *p) { if (p != nullptr) free(p); }
template <typename T> static inline void MemFreeAndNull(T* &p) { if (p != nullptr) { free(p); p = nullptr; } }
#endif

#else /* __cplusplus */

#define MemAlloc(size) malloc(size)
#define MemAllocZeroed(size) calloc(size, 1)
#define MemAllocStringA(size) MemAlloc(size)
#define MemAllocStringW(size) MemAlloc(size * sizeof(WCHAR))
#define MemAllocArray(count, size) calloc(count, size)
#define MemAllocArrayNoInit(count, size) MemAlloc((count) * (size))
#define MemRealloc(p, size) realloc(p, size)
#if FREE_IS_NULL_SAFE
#define MemFree(p) free(p);
#define MemFreeAndNull(p) do { free(p); p = nullptr; } while(0)
#else
#define MemFree(p) do { if ((p) != NULL) free(p); } while(0)
#define MemFreeAndNull(p) do { if ((p) != nullptr) { free(p); p = nullptr; } } while(0)
#endif

#endif /* __cplusplus */

#ifdef UNICODE
#define MemAllocString MemAllocStringW
#else
#define MemAllocString MemAllocStringA
#endif

#ifdef __cplusplus
extern "C" {
#endif

LIBNETXMS_EXPORTABLE void *MemCopyBlock(const void *data, size_t size);

#ifdef __cplusplus
}

template<typename T> T *MemCopyBlock(const T *data, size_t size)
{
   return static_cast<T*>(MemCopyBlock(static_cast<const void*>(data), size));
}

template<typename T> T *MemCopyArray(const T *data, size_t count)
{
   return static_cast<T*>(MemCopyBlock(static_cast<const void*>(data), count * sizeof(T)));
}

#endif

#if HAVE_ALLOCA
#define MemAllocLocal(size) ((void*)alloca(size))
#define MemFreeLocal(p)
#else
#define MemAllocLocal(size) MemAlloc(size)
#define MemFreeLocal(p) MemFree(p)
#endif   /* HAVE_ALLOCA */

/******* C string copy functions *******/

#ifdef __cplusplus

inline char *MemCopyStringA(const char *src)
{
   return (src != nullptr) ? MemCopyBlock(src, strlen(src) + 1) : nullptr;
}

inline WCHAR *MemCopyStringW(const WCHAR *src)
{
   return (src != nullptr) ? MemCopyBlock(src, (wcslen(src) + 1) * sizeof(WCHAR)) : nullptr;
}

#else

#define MemCopyStringA(s) (((s) != nullptr) ? static_cast<char*>(MemCopyBlock((s), strlen(s) + 1)) : nullptr)
#define MemCopyStringW(s) (((s) != nullptr) ? static_cast<WCHAR*>(MemCopyBlock((s), (wcslen(s) + 1) * sizeof(WCHAR))) : nullptr)

#endif

#ifdef UNICODE
#define MemCopyString MemCopyStringW
#else
#define MemCopyString MemCopyStringA
#endif

/******* malloc/free for uthash *******/
#define uthash_malloc(sz) MemAlloc(sz)
#define uthash_free(ptr,sz) MemFree(ptr)

/**
 * delete object and nullify pointer
 */
#define delete_and_null(x) do { delete x; x = nullptr; } while(0)

/**
 * Convert half-byte's value to hex digit and vice versa
 */
#define bin2hex(x) ((x) < 10 ? ((x) + _T('0')) : ((x) + (_T('A') - 10)))
#ifdef UNICODE
#define hex2bin(x) ((((x) >= _T('0')) && ((x) <= _T('9'))) ? ((x) - _T('0')) : \
                    (((towupper(x) >= _T('A')) && (towupper(x) <= _T('F'))) ? (towupper(x) - _T('A') + 10) : 0))
#else
#define hex2bin(x) ((((x) >= '0') && ((x) <= '9')) ? ((x) - '0') : \
                    (((toupper(x) >= 'A') && (toupper(x) <= 'F')) ? (toupper(x) - 'A' + 10) : 0))
#endif

/**
 * Define MIN() and MAX()
 */
#undef MIN
#define MIN(a, b) ((a) < (b) ? (a) : (b))
#undef MAX
#define MAX(a, b) ((a) > (b) ? (a) : (b))

/**
 * Define _tcsicmp() for non-windows
 */
#ifndef _WIN32
#define stricmp   strcasecmp
#define strnicmp  strncasecmp
#define wcsicmp   wcscasecmp
#define wcsnicmp  wcsncasecmp
#endif

/**
 * Compare two numbers and return -1, 0, or 1
 */
#define COMPARE_NUMBERS(n1,n2) (((n1) < (n2)) ? -1 : (((n1) > (n2)) ? 1 : 0))

/**
 * Increment pointer for given number of bytes
 */
#define inc_ptr(ptr, step, ptype) ptr = (ptype *)(((char *)ptr) + step)

/**
 * Validate numerical value
 */
#define VALIDATE_VALUE(var,x,y,z) { if ((var < x) || (var > y)) var = z; }

/**
 * DCI (data collection item) data types
 */
#define DCI_DT_INT         0
#define DCI_DT_UINT        1
#define DCI_DT_INT64       2
#define DCI_DT_UINT64      3
#define DCI_DT_STRING      4
#define DCI_DT_FLOAT       5
#define DCI_DT_NULL        6
#define DCI_DT_COUNTER32   7
#define DCI_DT_COUNTER64   8
#define DCI_DT_DEPRECATED  255	/* used internally by agent */

/**
 * Convert macro parameter to string (after expansion)
 */
#define __AS_STRING(x) _T(#x)
#define AS_STRING(x) __AS_STRING(x)

/**
 * Pipe handle
 */
#ifdef _WIN32
#define HPIPE HANDLE
#define INVALID_PIPE_HANDLE INVALID_HANDLE_VALUE
#else
#define HPIPE int
#define INVALID_PIPE_HANDLE (-1)
#endif

#include <nxsocket.h>

#ifdef __cplusplus

#include <algorithm>

/**
 * Session enumeration callback return codes
 */
enum EnumerationCallbackResult
{
   _STOP = 0,
   _CONTINUE = 1
};

/**
 * Virtualization types
 */
enum VirtualizationType
{
   VTYPE_NONE = 0,
   VTYPE_FULL = 1,
   VTYPE_CONTAINER = 2
};

/**
 * Ownership indicator
 */
enum class Ownership : bool
{
   False = false,
   True = true
};

/**
 * Disable copy constructor if compiler supports it
 */
#if CAN_DELETE_COPY_CTOR
#define DISABLE_COPY_CTOR(c) c (const c &s) = delete;
#else
#define DISABLE_COPY_CTOR(c)
#endif

/**
 * "final" specifier if compiler does not support it
 */
#if !HAVE_FINAL_SPECIFIER
#define final
#endif

/**
 * "override" specifier if compiler does not support it
 */
#if !HAVE_OVERRIDE_SPECIFIER
#define override
#endif

#endif

/**
 * "unused" attribute
 */
#if UNUSED_ATTRIBUTE_SUPPORTED
#define __UNUSED__ __attribute__((unused))
#else
#define __UNUSED__
#endif

/**
 * Header tags
 */
#define __NX_BINARY_VERSION_TAG static const char __UNUSED__ __netxms_tag_version[] = "$nxtag.version${" NETXMS_VERSION_STRING_A "}$";
#define __NX_BINARY_BUILD_TAG static const char __UNUSED__ __netxms_tag_build[] = "$nxtag.build${" NETXMS_BUILD_TAG_A "}$";
#define __NX_BINARY_APP_NAME(name) static const char __UNUSED__ __netxms_tag_name[] = "$nxtag.name${" #name "}$";

#define __NX_BINARY_ALL_TAGS(name) \
         __NX_BINARY_APP_NAME(name) \
         __NX_BINARY_VERSION_TAG \
         __NX_BINARY_BUILD_TAG

/**
 * Executable header
 */
#if WITH_JEMALLOC && defined(JEMALLOC_LOAD_WRAPPER)
#define NETXMS_EXECUTABLE_HEADER(name) __NX_BINARY_ALL_TAGS(name) JEMALLOC_LOAD_WRAPPER
#else
#define NETXMS_EXECUTABLE_HEADER(name) __NX_BINARY_ALL_TAGS(name)
#endif

#endif   /* _nms_common_h_ */
