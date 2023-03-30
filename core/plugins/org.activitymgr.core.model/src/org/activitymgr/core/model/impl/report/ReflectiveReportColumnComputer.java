package org.activitymgr.core.model.impl.report;

import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.IReportColumnComputer;
import org.activitymgr.core.util.StringHelper;
import org.apache.commons.beanutils.PropertyUtils;

public class ReflectiveReportColumnComputer implements IReportColumnComputer {
	
	public static final String COLLABORATOR_PREFIX = "collaborator.";

	public static final String TASK_PREFIX = "task.";

	private static final Collection<String> SUMMABLE_FIELDS = List.of(
			IModelMgr.BUDGET_ATTRIBUTE, IModelMgr.INITIALLY_CONSUMED_ATTRIBUTE, IModelMgr.ETC_ATTRIBUTE);

	private static final String SUM_SUFFIX = "Sum";
	
	private final String id;

	private final String name;

	private final boolean isSummable;

	private final String fieldId;

	public ReflectiveReportColumnComputer(String id) {
		this.id = id;
		int idx = id.indexOf('.');
		String thefieldId = id.substring(idx + 1);
		this.isSummable = SUMMABLE_FIELDS.contains(thefieldId);
		this.name = StringHelper.camelCaseToPhrase(thefieldId);
		if (isSummable) {
			// FIXME 'todo' attribute should be renamed to 'etc' in Task object
			if (IModelMgr.ETC_ATTRIBUTE.equals(thefieldId)) {
				thefieldId = "todo";
			}
			thefieldId += SUM_SUFFIX;
		}
		fieldId = thefieldId;
	}
	
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object compute(ReportItem item) {
		Object object = null;
		
		if (isSummable()) {// use TaskSums
			object = item.getContributedTask();
		} else if (id.startsWith(TASK_PREFIX)) {
			object = item.getContributedTask().getTask();
		} else {
			object = item.getContributor();
		}
		// Collaborator my be null in task oriented report with a task that
		// has no contribution
		if (object != null) {
			try {
				Object value = PropertyUtils.getProperty(object, fieldId);
				if (value instanceof Long) {
					if (((Long) value).longValue() != 0) {
						return ((Long) value)/100d;
					}
				}
				else if (value != null) {
					return String.valueOf(value);
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

	@Override
	public boolean isSummable() {
		return isSummable;
	}
	
	
}
