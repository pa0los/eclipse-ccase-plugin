/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gunnar Wagenknecht - adaption for eclipse-ccase
 *******************************************************************************/
package net.sourceforge.eclipseccase.ui.viewsupport;

import java.util.List;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A specialized content provider to show a list of elements.
 */
@SuppressWarnings("rawtypes")
public class ListContentProvider implements IStructuredContentProvider {
	List fContents;

	public ListContentProvider() {
	}

	public Object[] getElements(Object input) {
		if (fContents != null && fContents == input)
			return fContents.toArray();
		return new Object[0];
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof List) {
			fContents = (List) newInput;
		} else {
			fContents = null;
			// we use a fixed set.
		}
	}

	public void dispose() {
	}

	public boolean isDeleted(Object o) {
		return fContents != null && !fContents.contains(o);
	}
}
