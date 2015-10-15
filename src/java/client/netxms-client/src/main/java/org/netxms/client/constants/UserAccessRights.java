/**
 * 
 */
package org.netxms.client.constants;

/**
 * User access rights
 *
 */
public class UserAccessRights
{
	// System-wide rights
	public static final long SYSTEM_ACCESS_MANAGE_USERS          = 0x000000001;
	public static final long SYSTEM_ACCESS_SERVER_CONFIG         = 0x000000002;
	public static final long SYSTEM_ACCESS_CONFIGURE_TRAPS       = 0x000000004;
	public static final long SYSTEM_ACCESS_MANAGE_SESSIONS       = 0x000000008;
	public static final long SYSTEM_ACCESS_VIEW_EVENT_DB         = 0x000000010;
	public static final long SYSTEM_ACCESS_EDIT_EVENT_DB         = 0x000000020;
	public static final long SYSTEM_ACCESS_EPP                   = 0x000000040;
	public static final long SYSTEM_ACCESS_MANAGE_ACTIONS        = 0x000000080;
	public static final long SYSTEM_ACCESS_DELETE_ALARMS         = 0x000000100;
	public static final long SYSTEM_ACCESS_MANAGE_PACKAGES       = 0x000000200;
	public static final long SYSTEM_ACCESS_VIEW_EVENT_LOG        = 0x000000400;
	public static final long SYSTEM_ACCESS_MANAGE_TOOLS          = 0x000000800;
	public static final long SYSTEM_ACCESS_MANAGE_SCRIPTS        = 0x000001000;
	public static final long SYSTEM_ACCESS_VIEW_TRAP_LOG         = 0x000002000;
	public static final long SYSTEM_ACCESS_VIEW_AUDIT_LOG        = 0x000004000;
	public static final long SYSTEM_ACCESS_MANAGE_AGENT_CFG      = 0x000008000;
	public static final long SYSTEM_ACCESS_MANAGE_SITUATIONS     = 0x000010000;
	public static final long SYSTEM_ACCESS_SEND_SMS              = 0x000020000;
	public static final long SYSTEM_ACCESS_MOBILE_DEVICE_LOGIN   = 0x000040000;
	public static final long SYSTEM_ACCESS_REGISTER_AGENTS       = 0x000080000;
	public static final long SYSTEM_ACCESS_READ_FILES            = 0x000100000;
	public static final long SYSTEM_ACCESS_SERVER_CONSOLE        = 0x000200000;
	public static final long SYSTEM_ACCESS_MANAGE_FILES          = 0x000400000;
	public static final long SYSTEM_ACCESS_MANAGE_MAPPING_TBLS   = 0x000800000;
   public static final long SYSTEM_ACCESS_MANAGE_SUMMARY_TBLS   = 0x001000000;
   public static final long SYSTEM_ACCESS_REPORTING_SERVER      = 0x002000000;
   public static final long SYSTEM_ACCESS_XMPP_COMMANDS         = 0x004000000;
   public static final long SYSTEM_ACCESS_MANAGE_IMAGE_LIB      = 0x008000000;
   public static final long SYSTEM_ACCESS_UNLINK_ISSUES         = 0x010000000;
   public static final long SYSTEM_ACCESS_VIEW_SYSLOG           = 0x020000000;
   public static final long SYSTEM_ACCESS_USERS_SCHEDULED_TASKS = 0x040000000;
   public static final long SYSTEM_ACCESS_OWN_SCHEDULED_TASKS   = 0x080000000;
   public static final long SYSTEM_ACCESS_ALL_SCHEDULED_TASKS   = 0x100000000L;
   public static final long SYSTEM_ACCESS_SCHEDULE_SCRIPT       = 0x200000000L;
   public static final long SYSTEM_ACCESS_SCHEDULE_FILE_UPLOAD  = 0x400000000L;
   public static final long SYSTEM_ACCESS_SCHEDULE_MAINTENANCE  = 0x800000000L;

	
	public static final int OBJECT_ACCESS_READ          = 0x00000001;
	public static final int OBJECT_ACCESS_MODIFY        = 0x00000002;
	public static final int OBJECT_ACCESS_CREATE        = 0x00000004;
	public static final int OBJECT_ACCESS_DELETE        = 0x00000008;
	public static final int OBJECT_ACCESS_READ_ALARMS   = 0x00000010;
	public static final int OBJECT_ACCESS_ACL           = 0x00000020;
	public static final int OBJECT_ACCESS_UPDATE_ALARMS = 0x00000040;
	public static final int OBJECT_ACCESS_SEND_EVENTS   = 0x00000080;
	public static final int OBJECT_ACCESS_CONTROL       = 0x00000100;
	public static final int OBJECT_ACCESS_TERM_ALARMS   = 0x00000200;
	public static final int OBJECT_ACCESS_PUSH_DATA     = 0x00000400;
   public static final int OBJECT_ACCESS_CREATE_ISSUE  = 0x00000800;
   public static final int OBJECT_ACCESS_DOWNLOAD      = 0x00001000;
   public static final int OBJECT_ACCESS_UPLOAD        = 0x00002000;
   public static final int OBJECT_ACCESS_MANAGE_FILES  = 0x00004000;
   
}
