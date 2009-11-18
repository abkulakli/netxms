/**
 * 
 */
package org.netxms.ui.eclipse.objectbrowser.widgets.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.netxms.client.objects.GenericObject;
import org.netxms.ui.eclipse.shared.NXMCSharedData;

/**
 * @author Victor
 *
 */
public class ObjectTreeFilter extends ViewerFilter
{
	private String filterString = null;
	private Map<Long, GenericObject> objectList = null;
	private GenericObject lastMatch = null;
	private long[] rootObjects = null;
	private Set<Integer> classFilter = null;
	
	/**
	 * Constructor
	 */
	public ObjectTreeFilter(long[] rootObjects, Set<Integer> classFilter)
	{
		if (rootObjects != null)
		{
			this.rootObjects = new long[rootObjects.length];
			System.arraycopy(rootObjects, 0, this.rootObjects, 0, rootObjects.length);
		}
		this.classFilter = classFilter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element)
	{
		if (classFilter != null)
		{
			if (!classFilter.contains(((GenericObject)element).getObjectClass()))
				return false;
		}
		
		if (objectList == null)
			return true;

		boolean pass = objectList.containsKey(((GenericObject)element).getObjectId());
		if (!pass && (((GenericObject)element).getNumberOfChilds() > 0))
		{
			Iterator<GenericObject> it = objectList.values().iterator();
			while(it.hasNext())
			{
				GenericObject obj = it.next();
				if (obj.isChildOf(((GenericObject)element).getObjectId()))
				{
					pass = true;
					break;
				}
			}
		}
		return pass;
	}
	
	
	/**
	 * Set filter string
	 */
	public void setFilterString(final String filterString)
	{
		boolean fullSearch = true;
		if (this.filterString != null)
			if (filterString.startsWith(this.filterString))
				fullSearch = false;
		this.filterString = filterString.isEmpty() ? null : filterString.toLowerCase();
		updateObjectList(fullSearch);
	}
	
	
	/**
	 * Update list of matching objects
	 */
	private void updateObjectList(boolean doFullSearch)
	{
		if (filterString != null)
		{
			if (doFullSearch)
			{
				GenericObject[] fullList = NXMCSharedData.getInstance().getSession().getAllObjects();
				objectList = new HashMap<Long, GenericObject>();
				for(int i = 0; i < fullList.length; i++)
					if ((fullList[i].getObjectName().toLowerCase().startsWith(filterString)) &&
					    ((rootObjects == null) || fullList[i].isChildOf(rootObjects)))
					{
						objectList.put(fullList[i].getObjectId(), fullList[i]);
						lastMatch = fullList[i];
					}
			}
			else
			{
				lastMatch = null;
				Iterator<GenericObject> it = objectList.values().iterator();
				while(it.hasNext())
				{
					GenericObject obj = it.next();
					if (!obj.getObjectName().toLowerCase().startsWith(filterString))
						it.remove();
					else
						lastMatch = obj;
				}
			}
		}
		else
		{
			objectList = null;
			lastMatch = null;
		}
	}

	/**
	 * Get last matched object
	 * @return Last matched object
	 */
	public final GenericObject getLastMatch()
	{
		return lastMatch;
	}
	
	/**
	 * Get parent for given object
	 */
	public GenericObject getParent(final GenericObject childObject)
	{
		GenericObject[] parents = childObject.getParentsAsArray();
		for(GenericObject object : parents)
		{
			if (object != null)
			{
				if ((rootObjects == null) || object.isChildOf(rootObjects))
					return object;
			}
		}
		return null;
	}
}
