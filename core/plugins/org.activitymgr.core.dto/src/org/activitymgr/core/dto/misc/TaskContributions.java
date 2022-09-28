package org.activitymgr.core.dto.misc;

import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;

/**
 * Contains the contributions of a given task on a given period.
 * @author jbrazeau
 */
public class TaskContributions {
	
	/** The task */
	private Task task;
	
	/** The task code path */
	private String taskCodePath;
	
	/** The contributions */
	private Contribution[] contributions;
	
	private boolean closed;

	/**
	 * @return the task
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * @param task the task to set
	 */
	public void setTask(Task task) {
		this.task = task;
	}

	/**
	 * @return the contributions
	 */
	public Contribution[] getContributions() {
		return contributions;
	}

	/**
	 * @param contributions the contributions to set
	 */
	public void setContributions(Contribution[] contributions) {
		this.contributions = contributions;
	}

	/**
	 * @return the task code path
	 */
	public String getTaskCodePath() {
		return taskCodePath;
	}

	/**
	 * @param taskCodePath the new task code path
	 */
	public void setTaskCodePath(String taskCodePath) {
		this.taskCodePath = taskCodePath;
	}
	
	

	/**
	 * Sets the task is closed (directly or indirectly).
	 * 
	 * @param closed task flag
	 */
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	/**
	 * Returns if the task is closed for contribution.
	 * 
	 * @return closed
	 */
	public boolean isClosed() {
		return closed;
	}

}