package org.activitymgr.core.impl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.IntervalRequestHelper;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributionsSums;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

public class ContributionDAOImpl extends AbstractORMDAOImpl<Contribution> implements
		IContributionDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(ContributionDAOImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getContributions(org.activitymgr.core.beans
	 * .Collaborator, org.activitymgr.core.beans.Task, java.util.Calendar,
	 * java.util.Calendar)
	 */
	@Override
	public Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the request
			String orderBy = "ctb_year, ctb_month, ctb_day, ctb_contributor";
			if (task != null) {
				orderBy += ", tsk_path, tsk_number";
			}
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate,
					getColumnNamesRequestFragment(null), orderBy);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Extraction du résultat
			ArrayList<Contribution> list = new ArrayList<Contribution>();
			while (rs.next()) {
				list.add(read(rs, 1));
			}
			Contribution[] result = (Contribution[]) list.toArray(new Contribution[list.size()]);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString("DbMgr.errors.CONTRIBUTIONS_SELECTION_FAILURE"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.activitymgr.core.IDbMgr#getContributionsSum(org.activitymgr.core.
	 * beans.Collaborator, org.activitymgr.core.beans.Task, java.util.Calendar,
	 * java.util.Calendar)
	 */
	@Override
	public long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the SQL request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate, "sum(ctb_duration)", null);

			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DAOException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			long contributionSums = rs.getLong(1);
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contributionSums;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					"Erreur lors du calcul du consommé d'un collaborateur sur un intervalle de temps donné",
					e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}


	@Override
	public int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Build the SQL request
			pStmt = buildContributionsRequest(task, contributor, fromDate,
					toDate, "count(ctb_duration)", null);

			// Exécution de le requête et extraction du résultat
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DAOException(
						Strings.getString("DbMgr.errors.SQL_EMPTY_QUERY_RESULT"), null); //$NON-NLS-1$
			int count = rs.getInt(1);
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return count;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					"Erreur lors du calcul du consommé d'un collaborateur sur un intervalle de temps donné",
					e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	@Override
	public Map<Long, TaskContributionsSums> getTasksSums(Long taskId, String tasksPath, Calendar fromDate, Calendar toDate)
			throws DAOException {
		// At least one argument must be specified
		if (taskId != null && tasksPath != null) {
			throw new IllegalStateException("Both task Id and task path cannot be specified");
		}
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			Map<Long, TaskContributionsSums> result = new HashMap<Long, TaskContributionsSums>();
			IntervalRequestHelper interval = new IntervalRequestHelper(fromDate, toDate);
			
			// Prepare the request
			StringBuilder request = new StringBuilder(
					"select pt.tsk_id, pt.tsk_number, sum(ctb_duration), count(ctb_duration) ");
			request.append("from TASK");
			request.append(" pt left join (TASK lt left join CONTRIBUTION on (ctb_task=lt.tsk_id");
			if (interval.hasIntervalCriteria()) {
				request.append(" and ");
				interval.appendIntervalCriteria(request);
			}
			request.append(")");
			
			request.append(") on (pt.tsk_id=lt.tsk_id or lt.tsk_path like concat(pt.tsk_path, pt.tsk_number, '%'))");
			request.append(" where ");
			if (taskId != null) {
				request.append("pt.tsk_id=?");
			} else {
				request.append("pt.tsk_path=?");
			}
			request.append(" group by pt.tsk_id");
			request.append(" order by pt.tsk_number");

			// Bind parameters			
			pStmt = tx().prepareStatement(request.toString()); //$NON-NLS-1$
			int paramIdx = 1;
			if (interval.hasIntervalCriteria()) {
				paramIdx = interval.bindParameters(paramIdx, pStmt);
			}
			if (taskId != null) {
				pStmt.setLong(paramIdx++, taskId);
			} else  {
				pStmt.setString(paramIdx++, tasksPath);
			}
			rs = pStmt.executeQuery();

			// Retrieve the result
			while (rs.next()) {
				TaskContributionsSums sums = new TaskContributionsSums();
				sums.setTaskId(rs.getLong(1));
				sums.setConsumedSum(rs.getLong(3));
				sums.setContributionsNb(rs.getLong(4));
				result.put(sums.getTaskId(), sums);
			}
			// Close the statement
			pStmt.close();
			pStmt = null;
			
			// Return the result
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString(
							"DbMgr.errors.TASK_SUMS_COMPUTATION_FAILURE", taskId != null ? taskId : tasksPath), e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			try {
				if (pStmt != null)
					pStmt.close();
			} catch (Throwable ignored) {
			}
		}
	}
	/**
	 * Builds a request that selects contributions using a given task,
	 * contributor and date interval.
	 * 
	 * <p>
	 * All parameters are optionnal.
	 * </p>
	 * 
	 * @param task
	 *            a parent task of the contributions tasks.
	 * @param contributor
	 *            the contributor.
	 * @param fromDate
	 *            start date of the interval.
	 * @param toDate
	 *            end date of the interval.
	 * @param fieldsToSelect
	 *            fields to select.
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement buildContributionsRequest(Task task,
			Collaborator contributor, Calendar fromDate, Calendar toDate,
			String fieldsToSelect, String orderBy) throws SQLException {
		// Préparation de la requête
		StringBuilder request = new StringBuilder("select ")
				.append(fieldsToSelect)
				.append(" from CONTRIBUTION");
		if (task != null) {
			request.append(", TASK");
		}
		return buildIntervalRequest(request, contributor, task, fromDate,
				toDate, true, orderBy);
	}


	@Override
	public Collection<Integer> getContributionYears() {
		
		try(PreparedStatement pStmt = 
				tx().prepareStatement("select distinct(ctb_year) as year from CONTRIBUTION order by year")) {


			// Exécution de le requête et extraction du résultat
			ResultSet rs = pStmt.executeQuery();
			
			Collection<Integer> years = new ArrayList<Integer>();
			while (rs.next()) {
				years.add(rs.getInt(1));
			}

			return years;
		} catch (SQLException e) {
			return critical(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.dao.IContributionDAO#getContributionsInterval(java.lang.String)
	 */
	@Override
	public Calendar[] getContributionsInterval(String taskPath) {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			boolean filterByTaskPath = taskPath != null && !"".equals(taskPath);
			// Build the SQL request
			String request = "select count(*),min(ctb_year*10000+ctb_month*100+ctb_day),max(ctb_year*10000+ctb_month*100+ctb_day) from CONTRIBUTION";
			if (filterByTaskPath) {
				request += " join TASK on ctb_task=tsk_id where tsk_path like ? or concat(tsk_path, tsk_number)=?";
			}
			pStmt = tx().prepareStatement(request);
			if (filterByTaskPath) {
				pStmt.setString(1, taskPath + '%');
				pStmt.setString(2, taskPath);
			}
			// Exécution de le requête et extraction du résultat
			Calendar[] result = null;
			rs = pStmt.executeQuery();
			if (rs.next()) {
				int contributionsCount = rs.getInt(1);
				// If there is no contribution, simply return null
				if (contributionsCount > 0) {
					// Else parse the result
					result = new Calendar[2];
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date start = sdf.parse(rs.getString(2));
					result[0] = new GregorianCalendar();
					result[0].setTime(start);
					Date end = sdf.parse(rs.getString(3));
					result[1] = new GregorianCalendar();
					result[1].setTime(end);
				}
			}
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return result;
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					"Erreur lors de la récupération des années de contributions",
					e);
		} catch (ParseException e) {
			log.info("Unexpected parse error", e); //$NON-NLS-1$
			throw new DAOException(
					"Unexpected error while parsing a date",
					e);
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

}