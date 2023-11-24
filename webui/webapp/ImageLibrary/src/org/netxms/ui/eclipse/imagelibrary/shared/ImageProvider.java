/**
 * NetXMS - open source network management system
 * Copyright (C) 2003-2023 Raden Solutions
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
package org.netxms.ui.eclipse.imagelibrary.shared;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.netxms.client.LibraryImage;
import org.netxms.client.NXCException;
import org.netxms.client.NXCSession;
import org.netxms.ui.eclipse.imagelibrary.Activator;
import org.netxms.ui.eclipse.jobs.ConsoleJob;
import org.netxms.ui.eclipse.shared.ConsoleSharedData;

public class ImageProvider
{
   /**
    * Create image provider instance.
    *
    * @param display owning display
    * @param session communication session
    */
	public static void createInstance(final Display display, final NXCSession session)
	{
	   RWT.getUISession(display).exec(() -> {
         ImageProvider p = new ImageProvider(display, session);
         ConsoleSharedData.setProperty("ImageProvider", p);
      });
	}

	/**
	 * Get image provider's instance for specific display
	 * 
	 * @return
	 */
	public static ImageProvider getInstance(Display display)
	{
	   return (ImageProvider)ConsoleSharedData.getProperty(display, "ImageProvider");
	}

   /**
    * Get image provider's instance for current display
    * 
    * @return
    */
   public static ImageProvider getInstance()
   {
      return (ImageProvider)ConsoleSharedData.getProperty("ImageProvider");
   }

   private final NXCSession session;
   private final Display display;
   private final Image missingImage;
   private final Map<UUID, Image> imageCache = Collections.synchronizedMap(new HashMap<UUID, Image>());
   private final Map<UUID, Image> objectIconCache = Collections.synchronizedMap(new HashMap<UUID, Image>());
   private final Map<UUID, LibraryImage> libraryIndex = Collections.synchronizedMap(new HashMap<UUID, LibraryImage>());
   private final Set<ImageUpdateListener> updateListeners = new HashSet<ImageUpdateListener>();

   /**
    * Create image provider.
    *
    * @param display owning display
    * @param session communication session
    */
   private ImageProvider(Display display, NXCSession session)
	{
		this.display = display;
		this.session = session;
		final ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/missing.png"); //$NON-NLS-1$
		missingImage = imageDescriptor.createImage(display);
	}

	/**
    * Add update listener. Has no effect if same listener already added. Listener always called in UI thread.
    *
    * @param listener listener to add
    */
	public void addUpdateListener(final ImageUpdateListener listener)
	{
		updateListeners.add(listener);
	}

	/**
    * Remove update listener. Has no effect if same listener already removed or was not added.
    *
    * @param listener listener to remove
    */
	public void removeUpdateListener(final ImageUpdateListener listener)
	{
		updateListeners.remove(listener);
	}

   /**
    * Notify listeners on image update.
    *
    * @param guid image GUID
    */
   private void notifyListeners(final UUID guid)
   {
      for(final ImageUpdateListener listener : updateListeners)
         listener.imageUpdated(guid);
   }

	/**
    * Synchronize image library metadata.
    *
    * @throws IOException if socket I/O error occurs
    * @throws NXCException if NetXMS server returns an error or operation was timed out
    */
   public void syncMetaData() throws IOException, NXCException
	{
		List<LibraryImage> imageLibrary = session.getImageLibrary();
		libraryIndex.clear();
		clearCache();
		for(final LibraryImage libraryImage : imageLibrary)
		{
			libraryIndex.put(libraryImage.getGuid(), libraryImage);
		}
	}

	/**
    * Clear image cache
    */
	private void clearCache()
	{
		for(Image image : imageCache.values())
		{
			if (image != missingImage)
				image.dispose();
		}
		imageCache.clear();

      for(Image image : objectIconCache.values())
      {
         if ((image != missingImage) && !image.isDisposed())
            image.dispose();
      }
      objectIconCache.clear();
	}

	/**
    * Get image by GUID.
    *
    * @param guid image GUID
    * @return corresponding image or special image representing missing image
    */
	public Image getImage(final UUID guid)
	{
	   if (guid == null)
	      return missingImage;

		final Image image;
		if (imageCache.containsKey(guid))
		{
			image = imageCache.get(guid);
		}
		else
		{
			image = missingImage;
         loadImageFromServer(guid);
		}
		return image;
	}

   /**
    * Get image as object icon. Image is cut to square form if needed, and resized according to current device zoom level (16x16 on
    * 100% zoom).
    * 
    * @param guid image GUID
    * @return corresponding image as object icon or special image representing missing image
    */
   public Image getObjectIcon(final UUID guid)
   {
      if (guid == null)
         return missingImage;

      Image image;
      if (objectIconCache.containsKey(guid))
      {
         image = objectIconCache.get(guid);
      }
      else if (imageCache.containsKey(guid))
      {
         image = imageCache.get(guid);
         ImageData imageData = image.getImageData();
         if ((imageData.width == 16) && (imageData.height == 16))
         {
            objectIconCache.put(guid, image);
         }
         else
         {
            image = new Image(display, imageData.scaledTo(16, 16));
            objectIconCache.put(guid, image);
         }
      }
      else
      {
         image = missingImage;
         loadImageFromServer(guid);
      }
      return image;
   }

	/**
    * Load image from server.
    *
    * @param guid image GUID
    */
	private void loadImageFromServer(final UUID guid)
	{
      imageCache.put(guid, missingImage);
      if (!libraryIndex.containsKey(guid))
         return;

      ConsoleJob job = new ConsoleJob("Loading image from server", null, Activator.PLUGIN_ID) {
         @Override
         protected void runInternal(IProgressMonitor monitor) throws Exception
         {
            LibraryImage libraryImage;
            try
            {
               libraryImage = session.getImage(guid);
               final ByteArrayInputStream stream = new ByteArrayInputStream(libraryImage.getBinaryData());
               runInUIThread(new Runnable() {
                  @Override
                  public void run()
                  {
                     try
                     {
                        imageCache.put(guid, new Image(display, stream));
                        notifyListeners(guid);
                     }
                     catch(SWTException e)
                     {
                        Activator.logError("Cannot decode image", e);
                        imageCache.put(guid, missingImage);
                     }
                  }
               });
            }
            catch(Exception e)
            {
               Activator.logError("Cannot retrive image from server", e);
            }
         }

         @Override
         protected String getErrorMessage()
         {
            return "Cannot load image from server";
         }
      };
      job.setUser(false);
      job.start();
	}

	/**
	 * Get image library object
	 * 
	 * @param guid image GUID
	 * @return image library element or null if there are no image with given GUID
	 */
	public LibraryImage getLibraryImageObject(final UUID guid)
	{
		return libraryIndex.get(guid);
	}

	/**
	 * @return
	 */
	public List<LibraryImage> getImageLibrary()
	{
		return new ArrayList<LibraryImage>(libraryIndex.values());
	}

   /**
    * Update image
    * 
    * @param guid image GUID
    */
   public void updateImage(final UUID guid)
	{
      Image image = imageCache.remove(guid);
      if ((image != null) && (image != missingImage))
			image.dispose();

      image = objectIconCache.remove(guid);
      if ((image != null) && (image != missingImage) && !image.isDisposed())
         image.dispose();

      ConsoleJob job = new ConsoleJob("Update library image", null, Activator.PLUGIN_ID, null) {
         @Override
         protected void runInternal(IProgressMonitor monitor) throws Exception
         {
            LibraryImage imageHandle = session.getImage(guid);
            libraryIndex.put(guid, imageHandle);
            runInUIThread(new Runnable() {
               @Override
               public void run()
               {
                  notifyListeners(guid);
               }
            });
         }

         @Override
         protected IStatus createFailureStatus(Exception e)
         {
            return Status.OK_STATUS;
         }

         @Override
         protected String getErrorMessage()
         {
            return null;
         }
      };
      job.setUser(false);
      job.setSystem(true);
      job.start();
	}

   /**
    * Delete image
    * 
    * @param guid image GUID
    */
   public void deleteImage(UUID guid)
   {
      Image image = imageCache.remove(guid);
      if ((image != null) && (image != missingImage))
         image.dispose();

      image = objectIconCache.remove(guid);
      if ((image != null) && (image != missingImage) && !image.isDisposed())
         image.dispose();

      libraryIndex.remove(guid);

      display.asyncExec(new Runnable() {
         @Override
         public void run()
         {
            notifyListeners(guid);
         }
      });
   }
}
