/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2012 Victor Kirhenshtein
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.netxms.tests.mobileagent;

import org.junit.jupiter.api.Test;
import org.netxms.mobile.agent.Session;

/**
 * Basic connection tests
 */
public class ConnectionTest extends SessionTest
{
   @Test
	public void testConnect() throws Exception
	{
		final Session session = connect();

		Thread.sleep(2000);
		session.disconnect();
	}

   @Test
	public void testEncryptedConnect() throws Exception
	{
		final Session session = connect(true);

		Thread.sleep(2000);
		session.disconnect();
	}
}
