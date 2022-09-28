package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.google.inject.Inject;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.TreeTable;

@SuppressWarnings("serial")
public class TasksPanel extends AbstractTabPanel<ITasksTabLogic> implements ITasksTabLogic.View {

	private TreeTable taskTree;

	@Inject
	public TasksPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		taskTree = new TreeTable();
		addComponent(taskTree);
		taskTree.setImmediate(true);
		taskTree.setMultiSelect(false);
		taskTree.setSizeFull();
		taskTree.addValueChangeListener(event ->
				getLogic().onTaskSelected(event.getProperty().getValue()));
		return taskTree;
	}
	
    @Override
	public void setTreeContentProviderCallback(
			final ITreeContentProviderCallback<Long> tasksProvider) {
		TreeTableDatasource<Long> dataSource = new TreeTableDatasource<Long>(getResourceCache(), tasksProvider);
		taskTree.setContainerDataSource(dataSource);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			taskTree.addGeneratedColumn(propertyId, (source, itemId, prop) 
					-> tasksProvider.getCell((Long) itemId, (String) prop));
			int columnWidth = tasksProvider.getColumnWidth(propertyId);
			taskTree.setColumnWidth(propertyId, columnWidth);
			taskTree.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(tasksProvider.getColumnAlign(propertyId)));
		}
	}
    
}
