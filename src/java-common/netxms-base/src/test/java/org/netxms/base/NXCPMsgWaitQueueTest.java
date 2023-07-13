/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Victor Kirhenshtein
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
package org.netxms.base;

import junit.framework.TestCase;

/**
 * Tests for message wait queue
 */
public class NXCPMsgWaitQueueTest extends TestCase
{
	public void testWaitForMessage()
	{
		final NXCPMsgWaitQueue mwq = new NXCPMsgWaitQueue(5000, 10000);
		
		mwq.putMessage(new NXCPMessage(10, 1L));
		mwq.putMessage(new NXCPMessage(10, 2L));
		mwq.putMessage(new NXCPMessage(10, 3L));
		
		final NXCPMessage msg = mwq.waitForMessage(10, 2L);
		assertEquals(true, msg != null);
		assertEquals(10, msg.getMessageCode());
		assertEquals(2, msg.getMessageId());
		
		mwq.shutdown();
	}

	public void testHousekeeper()
	{
		final NXCPMsgWaitQueue mwq = new NXCPMsgWaitQueue(5000, 5000);
		
		mwq.putMessage(new NXCPMessage(10, 1L));
		mwq.putMessage(new NXCPMessage(10, 2L));
		mwq.putMessage(new NXCPMessage(10, 3L));
		
		// Sleep for 8 seconds - housekeeper should remove message from queue
		try
		{
			Thread.sleep(8000);
		}
		catch(InterruptedException e)
		{
		}
		
		final NXCPMessage msg = mwq.waitForMessage(10, 2L);
		assertEquals(true, msg == null);
		
		mwq.shutdown();
	}
}
