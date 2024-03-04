/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2024 Raden Solutions
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
package org.netxms;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.netxms.base.VersionInfo;
import org.netxms.client.NXCException;
import org.netxms.client.NXCSession;
import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.util.InteractiveConsole;

/**
 * nxshell launcher
 */
public class Shell
{
   private static final String DEFAULT_SERVER = "127.0.0.1";
   private static final String DEFAULT_LOGIN = "admin";

   private String optServer;
   private String optPort;
   private String optLogin;
   private String optPassword;
   private String optToken;

   /**
    * @param args
    */
   public static void main(String[] args)
   {
      final Shell shell = new Shell();
      try
      {
         shell.run(args);
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }

   /**
    * @param args
    * @throws IOException
    * @throws NetXMSClientException
    */
   private void run(String[] args) throws IOException, NXCException
   {
      initJython(args);

      readCredentials((args.length == 0) && isInteractive());

      final NXCSession session = connect();

      final InteractiveConsole console = createInterpreter(args);

      console.set("session", session);
      console.set("s", session);

      if (args.length == 0)
      {
         console.interact(getBanner(), null);
      }
      else
      {
         console.execfile(args[0]);
      }
      console.cleanup();

      session.disconnect();
   }

   /**
    * @param interactive
    */
   private void readCredentials(boolean interactive)
   {
      optServer = System.getProperty("netxms.server");
      optPort = System.getProperty("netxms.port");
      optLogin = System.getProperty("netxms.login");
      optPassword = System.getProperty("netxms.password");
      optToken = System.getProperty("netxms.token");

      if (interactive)
      {
         final Console console = System.console();
         if (optServer == null)
         {
            optServer = console.readLine("Server IP [127.0.0.1]: ");
            if (optPort == null)
            {
               optPort = console.readLine("Server TCP port [4701]: ");
            }
         }
         if ((optLogin == null) && (optToken == null))
         {
            optLogin = console.readLine("Login [admin]: ");
         }
         if ((optPassword == null) && (optToken == null))
         {
            final char[] passwordChars = console.readPassword("Password: ");
            if (passwordChars == null)
            {
               optPassword = "";
            }
            else
            {
               optPassword = new String(passwordChars);
            }
         }
      }
      if (optServer == null || optServer.length() == 0)
      {
         optServer = DEFAULT_SERVER;
      }
      if (optLogin == null || optLogin.length() == 0)
      {
         optLogin = DEFAULT_LOGIN;
      }
      if (optPassword == null)
      {
         optPassword = "";
      }
   }

   /**
    * @return
    */
   private String getBanner()
   {
      return "NetXMS " + VersionInfo.version() + " Interactive Shell";
   }

   /**
    * @return
    * @throws IOException
    * @throws NetXMSClientException
    */
   private NXCSession connect() throws IOException, NXCException
   {
      boolean encrypt = true;
      String encryptOption = System.getProperty("netxms.encryptSession");
      if (encryptOption != null)
      {
         encrypt = Boolean.parseBoolean(encryptOption);
      }
      boolean sync = true;
      String syncOption = System.getProperty("netxms.syncObjects");
      if (syncOption != null)
      {
         sync = Boolean.parseBoolean(syncOption);
      }

      final String hostName;
      int port = NXCSession.DEFAULT_CONN_PORT;
      if ((optPort != null) && !optPort.isEmpty())
      {
         hostName = optServer;
         try
         {
            port = Integer.valueOf(optPort);
         }
         catch(NumberFormatException e)
         {
            // ignore
         }
      }
      else
      {
         final String[] parts = optServer.split(":"); //$NON-NLS-1$
         if (parts.length == 2)
         {
            hostName = parts[0];
            try
            {
               port = Integer.valueOf(parts[1]);
            }
            catch(NumberFormatException e)
            {
               // ignore
            }
         }
         else
         {
            hostName = optServer;
         }
      }

      final NXCSession session = new NXCSession(hostName, port, encrypt);
      session.connect();
      if (optToken != null)
         session.login(optToken);
      else
         session.login(optLogin, optPassword);
      if (sync)
      {
         session.syncObjects();
         session.syncUserDatabase();
      }
      return session;
   }

   /**
    * @return
    */
   protected boolean isInteractive()
   {
      PySystemState systemState = Py.getSystemState();
      boolean interactive = ((PyFile)Py.defaultSystemState.stdin).isatty();
      if (!interactive)
      {
         systemState.ps1 = systemState.ps2 = Py.EmptyString;
      }
      return interactive;
   }

   /**
    * @param args
    * @return
    */
   private InteractiveConsole createInterpreter(String args[])
   {
      PySystemState systemState = Py.getSystemState();

      final InteractiveConsole console;
      if (!isInteractive())
      {
         systemState.ps1 = systemState.ps2 = Py.EmptyString;
      }
      console = new InteractiveConsole();
      Py.getSystemState().__setattr__("_jy_interpreter", Py.java2py(console));
      imp.load("site");

      console.exec("from org.netxms.base import *");
      console.exec("from org.netxms.client import *");
      console.exec("from org.netxms.client.agent.config import *");
      console.exec("from org.netxms.client.asset import *");
      console.exec("from org.netxms.client.businessservices import *");
      console.exec("from org.netxms.client.constants import *");
      console.exec("from org.netxms.client.dashboards import *");
      console.exec("from org.netxms.client.datacollection import *");
      console.exec("from org.netxms.client.events import *");
      console.exec("from org.netxms.client.log import *");
      console.exec("from org.netxms.client.maps import *");
      console.exec("from org.netxms.client.maps.configs import *");
      console.exec("from org.netxms.client.maps.elements import *");
      console.exec("from org.netxms.client.mt import *");
      console.exec("from org.netxms.client.objects import *");
      console.exec("from org.netxms.client.objects.configs import *");
      console.exec("from org.netxms.client.objects.interfaces import *");
      console.exec("from org.netxms.client.objects.queries import *");
      console.exec("from org.netxms.client.objecttools import *");
      console.exec("from org.netxms.client.packages import *");
      console.exec("from org.netxms.client.reporting import *");
      console.exec("from org.netxms.client.search import *");
      console.exec("from org.netxms.client.sensor import *");
      console.exec("from org.netxms.client.server import *");
      console.exec("from org.netxms.client.services import *");
      console.exec("from org.netxms.client.snmp import *");
      console.exec("from org.netxms.client.topology import *");
      console.exec("from org.netxms.client.users import *");
      console.exec("from org.netxms.client.xml import *");

      console.set("__name__", "__nxshell__");

      return console;
   }

   /**
    * @param args
    */
   private void initJython(String[] args)
   {
      final Properties postProperties = new Properties();
      final File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
      final File cacheDir = new File(tempDirectory, "nxshell");
      postProperties.setProperty("python.cachedir", cacheDir.getPath());
      postProperties.setProperty("python.cachedir.skip", "false");
      PySystemState.initialize(PySystemState.getBaseProperties(), postProperties, args);
   }
}
