package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;

import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

@SuppressWarnings("serial")
public class CollaboratorsPanel extends AbstractTabPanel<ICollaboratorsTabLogic> implements ICollaboratorsTabLogic.View {

	private Table collaboratorsTable;

	@Inject
	public CollaboratorsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		// Collaborators table
		collaboratorsTable = new Table();
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setSizeFull();
		return collaboratorsTable;
	}
	
    @Override
	public void setCollaboratorsProviderCallback(
			final ITableCellProviderCallback<Long> collaboratorsProvider) {
		TableDatasource<Long> dataSource = new TableDatasource<Long>(getResourceCache(), collaboratorsProvider);
		collaboratorsTable.setContainerDataSource(dataSource);
		
		ColumnGenerator cellProvider = (source, itemId, propertyId) -> 
			collaboratorsProvider.getCell((Long) itemId, (String) propertyId);
				
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, cellProvider);
			collaboratorsTable.setColumnWidth(propertyId, collaboratorsProvider.getColumnWidth(propertyId));
			collaboratorsTable.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(collaboratorsProvider.getColumnAlign(propertyId)));
		}
	}
    
}
