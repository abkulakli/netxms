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
package org.netxms.nxmc.base.actions;

import javax.servlet.http.Cookie;
import org.eclipse.jface.action.Action;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.ExitConfirmation;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.rap.rwt.internal.service.ContextProvider;
import org.netxms.nxmc.Registry;
import org.netxms.nxmc.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logout current user session
 */
public class LogoutAction extends Action
{
   private static final Logger logger = LoggerFactory.getLogger(LogoutAction.class);

   /**
    * Create logout action.
    * 
    * @param text action text
    */
   public LogoutAction(String text)
   {
      super(text);
   }

   /**
    * @see org.eclipse.jface.action.Action#run()
    */
   @Override
   public void run()
   {
      ExitConfirmation exitConfirmation = RWT.getClient().getService(ExitConfirmation.class);
      exitConfirmation.setMessage(null);

      Cookie cookie = new Cookie(Startup.LOGIN_COOKIE_NAME, "");
      cookie.setSecure(ContextProvider.getRequest().isSecure());
      cookie.setMaxAge(0);
      cookie.setHttpOnly(true);
      ContextProvider.getResponse().addCookie(cookie);

      try
      {
         Registry.getSession().disconnect();
      }
      catch(Exception e)
      {
         logger.error("Error disconnecting from server", e);
      }
      Registry.getMainWindow().getShell().dispose();

      JavaScriptExecutor executor = RWT.getClient().getService(JavaScriptExecutor.class);
      executor.execute("window.location.reload();");
   }
}
