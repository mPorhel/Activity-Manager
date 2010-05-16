/*
 * Copyright (c) 2004-2006, Jean-Fran�ois Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.ui.dialogs;

import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.util.Strings;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class TaskChooserDialog extends AbstractDialog {

	/** Logger */
	private static Logger log = Logger.getLogger(TaskChooserDialog.class);

	/** Tableau contenant les derni�res taches s�lectionn�es */
	private TaskChooserTable tasksTable;
	
	/** Liste des taches � afficher */
	private Task[] tasks;
	
	/**
	 * Constructeur par d�faut.
	 * @param parentShell shell parent.
	 */
	public TaskChooserDialog(Shell parentShell) {
		super(parentShell, Strings.getString("TaskChooserDialog.texts.TITLE"), null, null); //$NON-NLS-1$
		setShellStyle(SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.util.AbstractDialog#validateUserEntry()
	 */
	protected Object validateUserEntry() throws DialogException {
		log.debug("validateUserEntry"); //$NON-NLS-1$
		Task selectedTask = (Task) ((IStructuredSelection)tasksTable.getTableViewer().getSelection()).getFirstElement();
		// Validation du choix de la tache
		return selectedTask;
	}
	
	/* (non-Javadoc)
	 * @see jfb.tools.activitymgr.ui.util.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		// Ajout de la liste des taches
		// Ajout du titre de s�lection des taches pr�c�demment s�lectionn�es
		Label label = new Label(parentComposite, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);
		label.setText(Strings.getString("TaskChooserDialog.labels.FOUND_TASKS")); //$NON-NLS-1$
		// Ajout de la liste des s�lections pr�c�dentes
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		gridData.heightHint = 300;
		tasksTable = new TaskChooserTable(parentComposite, gridData, tasks);
		final Table table = tasksTable.getTableViewer().getTable();
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				okPressed();
			}
		});

		// Retour du composant parent
		return parentComposite;
	}

	/**
	 * D�finit la liste des taches qui doivent �tre affich�es dans le dialogue.
	 * @param tasks la liste des taches.
	 */
	public void setTasks(Task[] tasks) {
		this.tasks = tasks;
	}

}
