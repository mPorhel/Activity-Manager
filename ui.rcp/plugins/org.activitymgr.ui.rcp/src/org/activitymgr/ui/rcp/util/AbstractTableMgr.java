/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.ui.rcp.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

/**
 * Classe de base implémentant une partie des méthodes qui doivent être
 * implémentées pour gérer des tableaux.
 */
public abstract class AbstractTableMgr
		implements ITableLabelProvider, IStructuredContentProvider {

	/** Logger */
	private static Logger log = Logger.getLogger(AbstractTableMgr.class);

	/** Noeud racine */
	protected static Object ROOT_NODE = "ROOT_NODE"; //$NON-NLS-1$

	/** Liste des ILabelProviderListener */
	private List<ILabelProviderListener> labelProviderListeners = new ArrayList<>();

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		log.debug("ITreeContentProvider.inputChanged(" + viewer  //$NON-NLS-1$
				+ ", " + oldInput  //$NON-NLS-1$
				+ ", " + newInput + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		log.debug("ContentProvider.addListener(" + listener + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!labelProviderListeners.contains(listener)) {
			labelProviderListeners.add(listener);
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		log.debug("ContentProvider.addListener(" + listener + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		labelProviderListeners.remove(listener);
	}

	/**
	 * Notifie les listeners du changement de valeur d'un label.
	 *
	 * @param e
	 *            l'évènement.
	 */
	protected void notifyLabelProviderListener(LabelProviderChangedEvent e) {
		Iterator<ILabelProviderListener> it = labelProviderListeners.iterator();
		while (it.hasNext()) {
			ILabelProviderListener listener = it.next();
			log.debug("notifying listener(" + listener + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			listener.labelProviderChanged(e);
		}
	}

}
