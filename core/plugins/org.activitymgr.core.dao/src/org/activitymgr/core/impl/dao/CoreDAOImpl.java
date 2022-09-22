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
package org.activitymgr.core.impl.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.activitymgr.core.dao.AbstractDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ICoreDAO;
import org.activitymgr.core.util.DbHelper;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

/**
 * Classe offrant les services de base de persistence de l'application.
 * TODO 2236 -> 1865 -> 1558 -> 1125
 */
public class CoreDAOImpl extends AbstractDAOImpl implements ICoreDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(CoreDAOImpl.class);

	@Override
	public boolean tablesExist() throws DAOException {
		boolean tablesExist = true;
		tablesExist &= tableExists("COLLABORATOR"); //$NON-NLS-1$
		tablesExist &= tableExists("CONTRIBUTION"); //$NON-NLS-1$
		tablesExist &= tableExists("DURATION"); //$NON-NLS-1$
		tablesExist &= tableExists("TASK"); //$NON-NLS-1$
		return tablesExist;
	}

	@Override
	public boolean tableExists(String tableName) throws DAOException {
		// Récupération de la connexion
		Connection con = tx();

		String[] metatypes = { "TABLE" };
		// Recherche de la table
		try(ResultSet rs = con.getMetaData().getTables(null, null, tableName, metatypes)) {
			return rs.next();
		} catch (SQLException e) {
			return critical(e, "SQL_TABLES_DETECTION_FAILURE");  //$NON-NLS-1$
		}
	}

	@Override
	public void createTables() throws DAOException {
		// Lecture du fichier SQL de création de la BDD
		String batchName = isHsqlOrH2() ? "hsqldb.sql" : "mysqldb.sql"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		InputStream in = CoreDAOImpl.class.getResourceAsStream(batchName);
		executeScript(in);

		// Test de l'existence des tables
		if (!tablesExist()) {
			critical(null, "SQL_TABLE_CREATION_FAILURE"); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isHsqlOrH2() {
		try {
			return DbHelper.isHsqlOrH2(tx());
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void executeScript(InputStream scriptContent) throws DAOException {
		try {
			// Script content retrieval
			String sql = StringHelper.fromInputStream(scriptContent);

			// Execute the script
			executeScript(sql);
		} catch (IOException e) {
			critical(e, "SQL_SCRIPT_LOAD_FAILURE"); //$NON-NLS-1$
		}
	}

	@Override
	public void executeScript(String scriptContent) throws DAOException {

		try(Statement stmt = tx().createStatement()) {
			// Découpage et exécution du batch

			LineNumberReader lnr = new LineNumberReader(new StringReader(
					scriptContent));
			StringBuffer buf = new StringBuffer();
			boolean proceed = true;
			do { 
				// XXX Suspicious 
				//  :: last statement is not executed if it does end with ";"
				//  :: No escape for ';' character
				
				String line = null;
				// On ne lit dans le flux que si la ligne courante n'est pas
				// encore totalement traitée
				if (line == null) {
					try {
						line = lnr.readLine();
					} catch (IOException e) {
						log.info(
								"Unexpected I/O error while reading memory stream!", e); //$NON-NLS-1$
						throw new DAOException(
								Strings.getString("DbMgr.errors.MEMORY_IO_FAILURE"), null); //$NON-NLS-1$
					}
					log.debug("Line read : '" + line + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				// Si le flux est vide, on sort de la boucle
				if (line == null) {
					proceed = false;
				} else { // Sinon on traite la ligne
					line = line.trim();
					// Si la ligne est un commentaire on l'ignore
					if (line.startsWith("--")) { //$NON-NLS-1$
						line = null;
					} else {
						// Sinon on regarde si la ligne possède
						// un point virgule
						int idx = line.indexOf(';');
						// Si c'est le cas, on découpe la chaîne et on
						// exécute la requête
						if (idx >= 0) {
							buf.append(line.subSequence(0, idx));
							line = line.substring(idx);
							String sql = buf.toString();
							buf.setLength(0);
							log.debug(" - sql='" + sql + "'"); //$NON-NLS-1$ //$NON-NLS-2$
							if (!"".equals(sql)) //$NON-NLS-1$
								stmt.executeUpdate(sql);
						}
						// sinon on ajoute la ligne au buffer de requête
						else {
							buf.append(line);
							buf.append('\n');
						}
					}
				}

			} while (proceed);


		} catch (SQLException e) {
			 critical(e);
		}
	}

}
