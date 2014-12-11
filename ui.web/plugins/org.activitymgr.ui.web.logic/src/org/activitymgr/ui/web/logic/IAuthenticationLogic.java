package org.activitymgr.ui.web.logic;

import org.activitymgr.ui.web.logic.ILogic.IView;

public interface IAuthenticationLogic extends ILogic<IAuthenticationLogic.View> {
	
	public void onAuthenticate(String login, String password, boolean rememberMe);
	
	public interface View extends IView<IAuthenticationLogic> {
		
		void setDefaults(String login, boolean rememberMe);
		
	}

}
