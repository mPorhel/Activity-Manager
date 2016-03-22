package org.activitymgr.ui.web.logic.impl.internal;

import java.sql.Connection;
import java.util.Set;

import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.logic.ITransactionalWrapperBuilder;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.spi.ITabFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class RootLogicImpl implements IRootLogic {

	private Injector userInjector;
	
	private IRootLogic.View view;
	
	public RootLogicImpl(IRootLogic.View rootView, Injector mainInjector) {
		userInjector = mainInjector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IEventBus.class).to(EventBusImpl.class).in(Singleton.class);
				bind(ILogicContext.class).to(LogicContextImpl.class).in(Singleton.class);
				// and Transactional wrapper builder
				bind(ITransactionalWrapperBuilder.class).to(TransactionalManagerImpl.class);
				bind(IRootLogic.class).toInstance(RootLogicImpl.this);
			}
		});

		// View registration
		this.view = rootView;
		view.registerLogic(this);

		// Model manager retrieval
		// Create authentication logic
		getView().setContentView(new AuthenticationLogicImpl(this).getView());
		
		// Event listeners registration
		IEventBus eventBus = userInjector.getInstance(IEventBus.class);
		eventBus.register(ConnectedCollaboratorEvent.class, new IEventListener<ConnectedCollaboratorEvent>() {
			@Override
			public void handle(ConnectedCollaboratorEvent event) {
				// Create the tab container
				TabFolderLogicImpl tabFolderLogic = new TabFolderLogicImpl(RootLogicImpl.this);
				getView().setContentView(tabFolderLogic.getView());
				
				// Add tabs
				IFeatureAccessManager accessMgr = userInjector.getInstance(IFeatureAccessManager.class);
				Set<ITabFactory> tabFactories = userInjector.getInstance(Key.get(new TypeLiteral<Set<ITabFactory>>() {}));
				for (ITabFactory tabFactory : tabFactories) {
					if (accessMgr.hasAccessToTab(event.getConnectedCollaborator(), tabFactory.getTabId())) {
						ITabLogic<?> tabLogic = tabFactory.create(tabFolderLogic);
						tabFolderLogic.addTab(tabLogic.getLabel(), tabLogic);
					}
				}
				
			}

		});
	}
	
	
	@Override
	public ILogic<?> getParent() {
		return null;
	}

	@Override
	public View getView() {
		return view;
	}
	
	@Override
	public <T> T injectMembers(T instance) {
		userInjector.injectMembers(instance);
		return instance;
	}

	@Override
	public void dispose() {
		// TODO unregister listeners, dispose event bus
	}
}
