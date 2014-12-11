package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class TextFieldView extends TextField implements ITextFieldLogic.View {
	
	private ITextFieldLogic logic;
	
	public TextFieldView(IResourceCache resourceCache) {
	}

	@Override
	public void registerLogic(ITextFieldLogic newLogic) {
		this.logic = newLogic;
		setImmediate(true);
		addBlurListener(new FieldEvents.BlurListener() {
			@Override
			public void blur(BlurEvent event) {
				logic.onValueChanged(getValue());
			}
		});
	}

	@Override
	public void setNumericFieldStyle() {
		setStyleName("amount");
	}
	
	@Override
	public void setValue(String newValue)
			throws com.vaadin.data.Property.ReadOnlyException {
		super.setValue(newValue != null ? newValue : "");
	}

}
