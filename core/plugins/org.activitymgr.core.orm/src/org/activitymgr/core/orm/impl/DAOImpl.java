package org.activitymgr.core.orm.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.orm.IConverter;
import org.activitymgr.core.orm.IDAO;
import org.activitymgr.core.orm.impl.converters.BigDecimalConverter;
import org.activitymgr.core.orm.impl.converters.BooleanConverter;
import org.activitymgr.core.orm.impl.converters.ByteConverter;
import org.activitymgr.core.orm.impl.converters.CalendarConverter;
import org.activitymgr.core.orm.impl.converters.CharacterConverter;
import org.activitymgr.core.orm.impl.converters.DoubleConverter;
import org.activitymgr.core.orm.impl.converters.FloatConverter;
import org.activitymgr.core.orm.impl.converters.IntegerConverter;
import org.activitymgr.core.orm.impl.converters.LongConverter;
import org.activitymgr.core.orm.impl.converters.ShortConverter;
import org.activitymgr.core.orm.impl.converters.StringConverter;
import org.activitymgr.core.orm.query.AbstractOrderByClause;
import org.activitymgr.core.orm.query.AbstractStatement;
import org.activitymgr.core.orm.query.AscendantOrderByClause;
import org.activitymgr.core.orm.query.BetweenStatement;
import org.activitymgr.core.orm.query.DescendantOrderByClause;
import org.activitymgr.core.orm.query.GreaterThanStatement;
import org.activitymgr.core.orm.query.InStatement;
import org.activitymgr.core.orm.query.LikeStatement;
import org.activitymgr.core.orm.query.LowerThanStatement;
import org.apache.log4j.Logger;

/**
 * Classe peremettant de mapper une classe Java avec une table dans une base
 * de donn�es.
 * @author jbrazeau
 * TODO Javadoc
 */
public class DAOImpl<TYPE> implements IDAO<TYPE> {
	
	/** Logger */
	private static Logger log = Logger.getLogger(DAOImpl.class);

	/** Logger */
	private static Logger sqlLog = Logger.getLogger("dbClassMapper.logsqlrequests");

	/** Default converters */
	private static final Map<Class<?>, IConverter<?>> DEFAULT_CONVERTERS = new HashMap<Class<?>, IConverter<?>>();
	
	private static final Map<Class<?>, Integer> SQL_TYPES = new HashMap<Class<?>, Integer>();
	
	static {
		DEFAULT_CONVERTERS.put(BigDecimal.class, new BigDecimalConverter());
		DEFAULT_CONVERTERS.put(Boolean.class, new BooleanConverter());
		DEFAULT_CONVERTERS.put(boolean.class, new BooleanConverter());
		DEFAULT_CONVERTERS.put(Byte.class, new ByteConverter());
		DEFAULT_CONVERTERS.put(byte.class, new ByteConverter());
		DEFAULT_CONVERTERS.put(Calendar.class, new CalendarConverter());
		DEFAULT_CONVERTERS.put(Character.class, new CharacterConverter());
		DEFAULT_CONVERTERS.put(char.class, new CalendarConverter());
		DEFAULT_CONVERTERS.put(Double.class, new DoubleConverter());
		DEFAULT_CONVERTERS.put(double.class, new DoubleConverter());
		DEFAULT_CONVERTERS.put(Float.class, new FloatConverter());
		DEFAULT_CONVERTERS.put(float.class, new FloatConverter());
		DEFAULT_CONVERTERS.put(Integer.class, new IntegerConverter());
		DEFAULT_CONVERTERS.put(int.class, new IntegerConverter());
		DEFAULT_CONVERTERS.put(Long.class, new LongConverter());
		DEFAULT_CONVERTERS.put(long.class, new LongConverter());
		DEFAULT_CONVERTERS.put(Short.class, new ShortConverter());
		DEFAULT_CONVERTERS.put(short.class, new ShortConverter());
		DEFAULT_CONVERTERS.put(String.class, new StringConverter());
		
		/*
		 * Source : https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs
		 * /guide/jdbc/getstart/mapping.doc.html#1005577
		 */
		SQL_TYPES.put(BigDecimal.class, Types.DECIMAL);
		SQL_TYPES.put(Boolean.class, Types.BOOLEAN);
		SQL_TYPES.put(boolean.class, Types.BOOLEAN);
		SQL_TYPES.put(Byte.class, Types.TINYINT);
		SQL_TYPES.put(byte.class, Types.TINYINT);
		SQL_TYPES.put(Calendar.class, Types.DATE);
		SQL_TYPES.put(Character.class, Types.CHAR);
		SQL_TYPES.put(char.class, Types.CHAR);
		SQL_TYPES.put(Double.class, Types.DOUBLE);
		SQL_TYPES.put(double.class, Types.DOUBLE);
		SQL_TYPES.put(Float.class, Types.REAL);
		SQL_TYPES.put(float.class,  Types.REAL);
		SQL_TYPES.put(Integer.class, Types.INTEGER);
		SQL_TYPES.put(int.class, Types.INTEGER);
		SQL_TYPES.put(Long.class, Types.BIGINT);
		SQL_TYPES.put(long.class, Types.BIGINT);
		SQL_TYPES.put(Short.class, Types.SMALLINT);
		SQL_TYPES.put(short.class, Types.SMALLINT);
		SQL_TYPES.put(String.class, Types.VARCHAR);
	}
	
	/** The class */
	private Class<TYPE> mappedClass;
	
	/** Nom de la table mapp�e */
	private String tableName;
	
	/** Liste des attributs mapp�s dans la table */
	private List<Field> attributes;
	
	/** Table contenant les associations entre nom d'attribut et colonne SQL */
	private Map<String, Field> attributesDictionnary = new HashMap<String, Field>();
	
	/** Table contenant les associations entre nom d'attribut et colonne SQL */
	private Map<Field, String> columnNamesDictionnary = new HashMap<Field, String>();
	
	/** Liste des noms d'attributs de la cl� primaire */
	private List<Field> pkAttributes; 
	
	/** Attribute names of the primary key */
	private String[] pkAttributeNames;

	/** Nom de l'attribut auto g�n�r�s par la BDD si il existe */
	private Field autoGeneratedAttribute; 

	/** Converters map */
	private Map<Field, IConverter<?>> converters = new HashMap<Field, IConverter<?>>();
	
	/** Requ�tes */
	private String selectAllRequest;
	private String selectWithPKRequest;
	private String deletAllRequest;
	private String deletWithPKRequest;
	private String updateRequest;
	private String insertRequest;
	private String countAllRequest;

	/** Class constructor */
	private Constructor<TYPE> constructor;

	/**
	 * Constructeur priv�.
	 * @param mapping mapping de la classe mapp�e.
	 * @param theClass la classe mapp�e.
	 * @throws DbClassMappingException lev�e en cas de mauvaise configuration du
	 * 		mapping.
	 */
	public DAOImpl(IMappgingConfiguration mapping, Class<TYPE> theClass) {
		if (log.isDebugEnabled())
			log.debug("Descriptor loaded");
		this.mappedClass = theClass;
		tableName = mapping.getSQLTableName(theClass);

		// Retrieve mapped class constuctor
		try {
			constructor = mappedClass.getDeclaredConstructor();
			constructor.setAccessible(true);
		} catch (SecurityException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Mapped class must have no args constructor", e);
		}
		
		// R�cup�ration de la liste des attributs de la cl� primaire
		pkAttributes = mapping.getPrimaryKeyAttributes(mappedClass);
		pkAttributeNames = new String[pkAttributes.size()];
		int i = 0;
		for (Field pkAttribute : pkAttributes) {
			pkAttribute.setAccessible(true);
			pkAttributeNames[i++] = pkAttribute.getName();
		}
		// R�cup�ration de l'�ventuel attribut auto g�n�r�
		autoGeneratedAttribute = mapping.getAutoGeneratedAttribute(theClass);
		if (autoGeneratedAttribute != null)
			autoGeneratedAttribute.setAccessible(true);
		// R�cup�ration des attributs de la classe
		attributes = ReflectionHelper.getFields(theClass);
		// Parcours des attributs, construction du dictionnaire de colonnes et
		// d�tection de l'�ventuel attribut auto g�n�r� par la BDD
		Collection<Field> fieldsToIgnore = new ArrayList<Field>();
		for (Field attribute : attributes) {
			String columnName = mapping.getSQLColumnName(theClass, attribute);
			// Si l'attribut n'est pas d�fini, il est ignor�
			if (columnName!=null) {
				columnNamesDictionnary.put(attribute, columnName);
			}
			else {
				fieldsToIgnore.add(attribute);
			}
		}
		// R�cup�ration de la liste des attributs filtr�e puis tri
		attributes.removeAll(fieldsToIgnore);

		// Set fields as acceessible and register converters
		for (Field attribute : attributes) {
			attribute.setAccessible(true);
			attributesDictionnary.put(attribute.getName(), attribute);
			try {
				Class<? extends IConverter<?>> converterClass = (Class<? extends IConverter<?>>) mapping.getAttributeConverter(theClass, attribute);
				IConverter<? extends Object> converter = converterClass == null ? DEFAULT_CONVERTERS.get(attribute.getType()) : converterClass.newInstance();
				if (converter == null) {
					throw new IllegalArgumentException("No converter found for " + attribute);
				}
				converters.put(attribute, converter);
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("Unable to instantiate a converter", e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("Unable to instantiate a converter", e);
			}
	
		}
		
		// Construction de la requ�te de s�lection de toute les valeurs
		// d'un table
		StringBuffer buf = new StringBuffer("select ");
		appendColumnNames(buf, null, true, true, false);
		buf.append(" from ").append(tableName);
		selectAllRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("selectAllRequest='" + selectAllRequest + "'");

		// Construction de la requ�te de s�lection � partir de la cl� primaire
		// (r�utilisation de la requ�te selectAllRequest)
		buf.setLength(0);
		buf.append("select ");
		appendColumnNames(buf, null, false, true, false);
		buf.append(" from ").append(tableName);
		appendWherePK(buf);
		selectWithPKRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("selectWithPKRequest='" + selectWithPKRequest + "'");
		
		// Construction de la requ�te de suppression � partir de la cl� primaire
		buf.setLength(0);
		buf.append("delete from ").append(tableName);
		deletAllRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("deletAllRequest='" + deletAllRequest + "'");

		// Construction de la requ�te de suppression � partir de la cl� primaire
		buf.setLength(0);
		buf.append(deletAllRequest);
		appendWherePK(buf);
		deletWithPKRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("deletWithPKRequest='" + deletWithPKRequest + "'");

		// Construction de la requ�te d'insertion
		buf.setLength(0);
		buf.append("insert into ").append(tableName).append(" (");
		appendColumnNames(buf, null, true, false, false);
		buf.append(") values (");
		int parameterIdx = 1;
		for (Field attribute : attributes) {
			if (!attribute.equals(autoGeneratedAttribute)) {
				buf.append(parameterIdx==1 ? "?" :", ?");
				parameterIdx++;
			}
		}
		buf.append(")");
		insertRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("insertRequest='" + insertRequest + "'");

		// Construction de la requ�te de mise � jour
		buf.setLength(0);
		buf.append("update ").append(tableName).append(" set ");
		appendColumnNames(buf, null, false, false, true);
		appendWherePK(buf);
		updateRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("updateRequest='" + updateRequest + "'");
		
		// Construction de la requ�te de comptage de toutes les lignes
		buf.setLength(0);
		buf.append("select count(*) from ").append(tableName);
		countAllRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("countAllRequest='" + countAllRequest + "'");
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#selectWithPK(java.sql.Connection, java.lang.Object[])
	 */
	@Override
	public TYPE selectByPK(Connection con, Object... pkValue) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(selectWithPKRequest);
		PreparedStatement pStmt = null;
		try {
			checkPkAttributeValues(pkValue);
			TYPE result = null;
			// Si le nombre d'attributs de la classe est �gal � la cl� primaire, 
			// la requ�te g�n�r�e est fausse (car elle ignore par d�faut les colonnes
			// de la cl� primaire puisqu'elles sont sp�cifi�es en param�tre)
			if (pkAttributes.size()==attributes.size()) {
				boolean exists = count(con, pkAttributeNames, pkValue)>0;
				if (exists) {
					result = ReflectionHelper.newInstance(constructor);
					for (int i=0; i<pkValue.length; i++) {
						pkAttributes.get(i).set(result, pkValue[i]);
					}
				}
			}
			// Autres cas
			else {
				pStmt = con.prepareStatement(selectWithPKRequest);
				for (int i=0; i<pkValue.length; i++) {
					Field attribute = pkAttributes.get(i);
					Object attibuteValue = pkValue[i];
					attributeValueToStatementColumn(attribute, attibuteValue, pStmt, i+1);
				}
				ResultSet rs = pStmt.executeQuery();
				if (rs.next()) {
					result = ReflectionHelper.newInstance(constructor);
					resultSetToInstanceAttributes(rs, result, false);
					for (int i=0; i<pkValue.length; i++) {
						Field pkAttribute = pkAttributes.get(i);
						pkAttribute.set(result, pkValue[i]);
					}
				}
	
				// Fermeture du statement
				pStmt.close();
				pStmt = null;
			}

			// Retour du r�sultat
			return result;
		} catch (IllegalArgumentException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		} catch (IllegalAccessException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	@Override
	public boolean delete(Connection con, TYPE instance) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(deletWithPKRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(deletWithPKRequest);
			int parameterIdx = 1;
			for (Field pkAttribute : pkAttributes) {
				instanceAttributeToStatementColumn(instance, pkAttribute, pStmt, parameterIdx);
				parameterIdx++;
			}
			// Construction du r�sultat
			boolean deleted = pStmt.executeUpdate()==1;

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du r�sultat
			return deleted;
		} catch (IllegalArgumentException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		} catch (IllegalAccessException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#deleteWithPK(java.sql.Connection, TYPE)
	 */
	@Override
	public boolean deleteByPK(Connection con, Object... pkValue) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(deletWithPKRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(deletWithPKRequest);
			int parameterIdx = 1;
			for (Field pkAttribute : pkAttributes) {
				attributeValueToStatementColumn(pkAttribute, pkValue[parameterIdx - 1], pStmt, parameterIdx);
				parameterIdx++;
			}
			// Construction du r�sultat
			boolean deleted = pStmt.executeUpdate()==1;

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du r�sultat
			return deleted;
		} catch (IllegalArgumentException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#delete(java.sql.Connection, java.lang.String[], java.lang.Object[])
	 */
	@Override
	public int delete(Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues) throws SQLException {
		StringBuffer buf = new StringBuffer(deletAllRequest);
		appendCustomWhereClause(buf, whereClauseAttributeNames, whereClauseAttributeValues);
		String request = buf.toString();
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("customDeleteRequest=" + request);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(request);
			bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			int deleted = pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du r�sultat
			return deleted;
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	private void checkPkAttributeValues(Object[] pkAttributeValues) {
		if (pkAttributeValues==null)
			throw new IllegalStateException("PK attributes must be specified", null);
		if (pkAttributeValues.length!=pkAttributes.size()) 
			throw new IllegalStateException("Wrong parameters number. Primary key contains " + pkAttributes.size() + " items", null);
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#selectAll(java.sql.Connection)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TYPE[] selectAll(Connection con) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(selectAllRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(selectAllRequest);
			ResultSet rs = pStmt.executeQuery();
			List<Object> result = new ArrayList<Object>();
			while (rs.next()) {
				TYPE newInstance = ReflectionHelper.newInstance(constructor);
				result.add(newInstance);
				if (log.isDebugEnabled())
					log.debug("newInstance=" + newInstance);
				resultSetToInstanceAttributes(rs, newInstance, true);
			}
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return result.toArray((TYPE[]) Array.newInstance(mappedClass, result.size()));
		} catch (IllegalAccessException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#dump(java.io.OutputStream, java.lang.String, java.sql.Connection, java.lang.String[], java.lang.Object[], java.lang.Object[], int)
	 */
	@Override
	public void dump(OutputStream out, String encoding, Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues, Object[] orderByClauseItems, int maxRows) throws SQLException {
		// Pr�paration de la requ�te de s�lection
		String request = builSelectRequest(whereClauseAttributeNames, whereClauseAttributeValues, orderByClauseItems, maxRows);
		PreparedStatement pStmt = null;
		try {
			// G�n�ration du dump (script SQL contenant les insert)
			PrintStream pOut = new PrintStream(out, true, encoding);
			pStmt = con.prepareStatement(request);
			// Binding de la clause where
			int parametersCount = bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			// Binding de la clause limit
			if (maxRows>0)
				pStmt.setInt(parametersCount, maxRows);
			ResultSet rs = pStmt.executeQuery();
			boolean requestBeginningFlusehd = false;
			while (rs.next()) {
				// Cr�ation de la base de la requ�te d'insertion
				if (!requestBeginningFlusehd) {
					StringBuffer buf = new StringBuffer("insert into ").append(tableName).append(" (");
					appendColumnNames(buf, null, true, true, false);
					buf.append(") values");
					pOut.println(buf);
					requestBeginningFlusehd = true;
				}
				// Lignes suivantes
				else {
					pOut.println(",");
				}
				// Ajout des donn�es
				pOut.print("(");
				for (int i=0; i<attributes.size(); i++) {
					pOut.flush();
					if (i!=0) pOut.print(", ");
					pOut.print('\'');
					pOut.print(rs.getString(i+1).replaceAll("'", "''"));
					pOut.print('\'');
				}
				pOut.print(")");
			}
			pOut.println(";");
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Flush
			pOut.flush();
		} catch (UnsupportedEncodingException e) {
			log.error("Error while initializing stream", e);
			throw new IllegalStateException("Error while initializing stream", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	private String builSelectRequest(String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues, Object[] orderByClauseItems, int maxRows) throws SQLException {
		StringBuffer buf = new StringBuffer(selectAllRequest);
		appendCustomWhereClause(buf, whereClauseAttributeNames, whereClauseAttributeValues);
		// Ajout de la clause 'order by'
		if (orderByClauseItems!=null && orderByClauseItems.length>0) {
			buf.append(" order by ");
			for (int i=0; i<orderByClauseItems.length; i++) {
				if (i!=0) buf.append(", ");
				Object orderByClauseItem = orderByClauseItems[i];
				if (!(orderByClauseItem instanceof AbstractOrderByClause)) {
					Field attribute = getAttributeByName((String) orderByClauseItem);
					buf.append(columnNamesDictionnary.get(attribute));
				}
				else {
					String attributeName = ((AbstractOrderByClause) orderByClauseItem).getAttributeName();
					Field attribute = getAttributeByName(attributeName);
					buf.append(columnNamesDictionnary.get(attribute));
					if (orderByClauseItem instanceof AscendantOrderByClause)
						buf.append(" asc");
					else if (orderByClauseItem instanceof DescendantOrderByClause)
						buf.append(" desc");
					else
						throw new IllegalStateException("Unknown order by clause item type : '" + orderByClauseItem + "'", null);
				}
			}
		}
		// Ajout de la clause limit
		if (maxRows>0)
			buf.append(" limit ?");
		String request = buf.toString();
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("customSelectRequest=" + request);
		return request;
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#select(java.sql.Connection, java.lang.String[], java.lang.Object[], java.lang.Object[], int)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TYPE[] select(Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues, Object[] orderByClauseItems, int maxRows) throws SQLException {
		String request = builSelectRequest(whereClauseAttributeNames, whereClauseAttributeValues, orderByClauseItems, maxRows);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(request);
			// Binding de la clause where
			int parametersCount = bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			// Binding de la clause limit
			if (maxRows>0)
				pStmt.setInt(parametersCount, maxRows);
			ResultSet rs = pStmt.executeQuery();
			List<Object> result = new ArrayList<Object>();
			while (rs.next()) {
				TYPE newInstance = newInstance();
				result.add(newInstance);
				if (log.isDebugEnabled())
					log.debug("newInstance=" + newInstance);
				resultSetToInstanceAttributes(rs, newInstance, true);
			}
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return result.toArray((TYPE[]) Array.newInstance(mappedClass, result.size()));
		} catch (IllegalStateException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		} catch (IllegalArgumentException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		} catch (IllegalAccessException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	@Override
	public TYPE newInstance() {
		return ReflectionHelper.newInstance(constructor);
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#update(java.sql.Connection, TYPE)
	 */
	@Override
	public TYPE update(Connection con, TYPE value) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(updateRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(updateRequest);
			int mappedParametersNb = instanceAttributesToStatement(value, pStmt, false, false);
			int parameterIdx = mappedParametersNb + 1;
			for (Field pkAttribute : pkAttributes) {
				instanceAttributeToStatementColumn(value, pkAttribute, pStmt, parameterIdx);
				parameterIdx++;
			}
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new IllegalStateException("Row update failed");
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return value;
		} catch (IllegalArgumentException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		} catch (IllegalAccessException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#insert(java.sql.Connection, TYPE)
	 */
	@Override
	public TYPE insert(Connection con, TYPE value) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(insertRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(insertRequest, Statement.RETURN_GENERATED_KEYS);
			instanceAttributesToStatement(value, pStmt, true, false);
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new IllegalStateException("Row insertion failed");
			getAutoGeneratedKey(pStmt, value);
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return value;
		} catch (IllegalArgumentException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		} catch (IllegalAccessException e) {
			log.error("Error while accessing instance attribute", e);
			throw new IllegalStateException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#countAll(java.sql.Connection)
	 */
	@Override
	public long countAll(Connection con) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(countAllRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(countAllRequest);
			ResultSet rs = pStmt.executeQuery();
			if (!rs.next())
				throw new IllegalStateException("Nothing returned form this count query!");
			long count = rs.getLong(1); 

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return count;
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#count(java.sql.Connection, java.lang.String[], java.lang.Object[])
	 */
	@Override
	public long count(Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues) throws SQLException {
		StringBuffer buf = new StringBuffer(countAllRequest).append(" ");
		appendCustomWhereClause(buf, whereClauseAttributeNames, whereClauseAttributeValues);
		String request = buf.toString();
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("customCountRequest=" + request);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(request);
			bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			ResultSet rs = pStmt.executeQuery();
			if (!rs.next())
				throw new IllegalStateException("Nothing returned form this count query!");
			long count = rs.getLong(1); 

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return count;
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	private int instanceAttributesToStatement(TYPE instance, PreparedStatement pStmt, boolean includePK, boolean includeAutoGenerated) throws SQLException, IllegalArgumentException, IllegalAccessException {
		if (log.isDebugEnabled())
			log.debug("instanceAttributesToStatement(" + instance + ", " + pStmt + ", " + includePK + ", " + includeAutoGenerated + ")");
		int parameterIdx = 1;
		for (Field attribute : attributes) {
			if ((includePK || !pkAttributes.contains(attribute)) 
					&& (includeAutoGenerated ||!attribute.equals(autoGeneratedAttribute))) {
				instanceAttributeToStatementColumn(instance, attribute, pStmt, parameterIdx);
				parameterIdx++;
			}
		}
		return parameterIdx-1;
	}

	private void instanceAttributeToStatementColumn(TYPE instance, Field attribute, PreparedStatement pStmt, int parameterIdx) throws SQLException, IllegalArgumentException, IllegalAccessException {
		Object attributeValue = attribute.get(instance);
		attributeValueToStatementColumn(attribute, attributeValue, pStmt, parameterIdx);
	}

	private int bindAttributeValueToStatement(PreparedStatement pStmt, String[] attributeNames, Object[] attributeValues) throws SQLException {
		int parameterIdx = 1;
		if (attributeNames!=null) {
			for (int i=0; i<attributeNames.length; i++) {
				String attributeName = attributeNames[i];
				Object attributeValue = attributeValues[i];
				Field attribute = getAttributeByName(attributeName);
				if (attributeValue instanceof AbstractStatement) {
					if (attributeValue instanceof InStatement) {
						for (Object value : ((InStatement) attributeValue).getValues()) {
							attributeValueToStatementColumn(attribute, value, pStmt, parameterIdx++);
						}
					}
					else if (attributeValue instanceof BetweenStatement) {
						BetweenStatement bs = (BetweenStatement) attributeValue;
						attributeValueToStatementColumn(attribute, bs.getLow(), pStmt, parameterIdx++);
						attributeValueToStatementColumn(attribute, bs.getHigh(), pStmt, parameterIdx++);
					}
					else if (attributeValue instanceof GreaterThanStatement) {
						GreaterThanStatement gts = (GreaterThanStatement) attributeValue;
						attributeValueToStatementColumn(attribute, gts.getValue(), pStmt, parameterIdx++);
					}
					else if (attributeValue instanceof LowerThanStatement) {
						LowerThanStatement lts = (LowerThanStatement) attributeValue;
						attributeValueToStatementColumn(attribute, lts.getValue(), pStmt, parameterIdx++);
					}
					else if (attributeValue instanceof LikeStatement) {
						LikeStatement lts = (LikeStatement) attributeValue;
						attributeValueToStatementColumn(attribute, lts.getValue(), pStmt, parameterIdx++);
					} else {
						throw new IllegalStateException("Unknown statement type : " + attributeValue);
					}
				} else if (attributeValue != null) {
					attributeValueToStatementColumn(attribute, attributeValue, pStmt, parameterIdx++);
				}
			}
			
		}
		return parameterIdx;
	}

	private Field getAttributeByName(String attributeName) {
		Field attribute = attributesDictionnary.get(attributeName);
		if (attribute == null) {
			throw new IllegalArgumentException("Unknown attribute '" + attributeName + "'");
		}
		return attribute;
	}

	private void attributeValueToStatementColumn(Field attribute, Object attributeValue, PreparedStatement pStmt, int parameterIdx) throws SQLException {
		if (log.isDebugEnabled())
			log.debug("  - attributeName='" + attribute.getName() + "'");
		@SuppressWarnings("unchecked")
		IConverter<Object> converter = (IConverter<Object>) converters.get(attribute);
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("    +-> attributeValue='" + attributeValue + "'");
		// Par d�faut le param�tre est mapp� sur la valeur directe de l'attribut
		if (attributeValue == null) {
			Class<?> type = attribute.getType();
			Integer sqlType = SQL_TYPES.get(type);
			if (sqlType == null) {
				throw new IllegalStateException(
						"Unexpected Java field type with null value : " + type);
			}
			pStmt.setNull(parameterIdx, sqlType);
		} else {
			converter.bind(pStmt, parameterIdx, attributeValue);
		}
	}

	@Override
	public TYPE read(ResultSet rs, int fromIndex) {
		try {
			TYPE instance = ReflectionHelper.newInstance(constructor);
			resultSetToInstanceAttributes(rs, fromIndex, instance, true);
			return instance;
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private void resultSetToInstanceAttributes(ResultSet rs, TYPE instance, boolean includePK) throws SQLException, IllegalArgumentException, IllegalAccessException {
		resultSetToInstanceAttributes(rs, 1, instance, includePK);
	}

	private void resultSetToInstanceAttributes(ResultSet rs, int fromIndex, TYPE instance, boolean includePK) throws SQLException, IllegalArgumentException, IllegalAccessException {
		int parameterIdx = fromIndex;
		for (Field attribute : attributes) {
			if (includePK || !pkAttributes.contains(attribute)) {
				resultSetColumnToInstanceAttribute(rs, parameterIdx, instance, attribute);
				parameterIdx++;
			}
		}
	}
	
	private void resultSetColumnToInstanceAttribute(ResultSet rs, int rsColumnIdx, TYPE instance, Field attribute) throws SQLException, IllegalArgumentException, IllegalAccessException {
		@SuppressWarnings("unchecked")
		IConverter<Object> converter = (IConverter<Object>) converters.get(attribute);
		Object attributeValue = converter.readValue(rs, rsColumnIdx);
		// Test if the value was null
		if (rs.wasNull()) {
			attributeValue = null;
		}
		if (log.isDebugEnabled())
			log.debug("    +-> '" + attribute.getName() + "'='" + attributeValue + "'");
		// If the atttibute value is null, retrieve the default value for a
		// primitive type
		if (attributeValue == null) {
			attribute.set(instance, defaultTypeValue(attribute.getType()));
		} else {
			attribute.set(instance, attributeValue);
		}
	}

	private Object defaultTypeValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		} else if (type.equals(boolean.class)) {
			return (boolean) false;
		} else {
			return 0;
		}
	}

	@Override
	public String getColumnNamesRequestFragment(String tableAliasToUse, boolean includePK) {
		StringWriter w = new StringWriter();
		appendColumnNames(w, tableAliasToUse, includePK, true, false);
		return w.toString();
	}

	public String getColumnName(String attributeName) {
		return columnNamesDictionnary.get(getAttributeByName(attributeName));
	};

	public void appendColumnNames(Appendable buf, String tableAliasToUse, boolean includePK, boolean includeAutoGenerated, boolean includeStamtementParameter) {
		try {
			boolean firstItem = true;
			for (Field attribute : attributes) {
				if ((includePK || !pkAttributes.contains(attribute))
						&& (includeAutoGenerated || !attribute.equals(autoGeneratedAttribute))) {
					if (!firstItem)
						buf.append(", ");
					String columnName = columnNamesDictionnary.get(attribute);
					if (tableAliasToUse != null) {
						buf.append(tableAliasToUse);
						buf.append('.');
					}
					buf.append(columnName);
					if (includeStamtementParameter) {
						buf.append("=?");
					}
					firstItem = false;
				}
			}
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private void appendWherePK(StringBuffer buf) {
		buf.append(" where ");
		boolean first = true;
		for (Field pkAttribute : pkAttributes) {
			String pkColumnName = columnNamesDictionnary.get(pkAttribute);
			if (!first) buf.append(" and ");
			buf.append(pkColumnName).append("=?");
			first = false;
		}
	}

	private void appendCustomWhereClause(StringBuffer buf, String[] whereClauseAttributeNames, Object[] whereClauseAttributesValues) {
		if (whereClauseAttributeNames!=null) {
			if (whereClauseAttributesValues==null || whereClauseAttributeNames.length!=whereClauseAttributesValues.length)
				throw new IllegalStateException("Wrong argument number", null);
			buf.append(" where ");
			for (int i=0; i<whereClauseAttributeNames.length; i++) {
				String whereClauseAttributeName = whereClauseAttributeNames[i];
				Object whereClauseAttributeValue = whereClauseAttributesValues[i];
				Field whereClauseAttribute = getAttributeByName(whereClauseAttributeName);
				String whereClausColumnName = columnNamesDictionnary.get(whereClauseAttribute);
				if (i!=0) buf.append(" and ");
				buf.append(whereClausColumnName);
				if (whereClauseAttributeValue instanceof AbstractStatement) {
					if (whereClauseAttributeValue instanceof InStatement) {
						InStatement stmt = (InStatement) whereClauseAttributeValue;
						if (stmt.getValues().length == 1) {
							buf.append("=?");
						}
						else {
							buf.append(" in (");
							boolean first = true;
							for (@SuppressWarnings("unused") Object value : stmt.getValues()) {
								buf.append(first ? "?" : ", ?");
								first = false;
							}
							buf.append(")");
						}
					}
					else if (whereClauseAttributeValue instanceof BetweenStatement) {
						buf.append(" between ? and ?");
					}
					else if (whereClauseAttributeValue instanceof GreaterThanStatement) {
						GreaterThanStatement gts = (GreaterThanStatement) whereClauseAttributeValue;
						buf.append(">");
						if (gts.getOrEquals())
							buf.append("=");
						buf.append("?");
					}
					else if (whereClauseAttributeValue instanceof LowerThanStatement) {
						LowerThanStatement lts = (LowerThanStatement) whereClauseAttributeValue;
						buf.append("<");
						if (lts.getOrEquals())
							buf.append("=");
						buf.append("?");
					}
					else if (whereClauseAttributeValue instanceof LikeStatement) {
						buf.append(" like ?");
					}
					else 
						throw new IllegalStateException("Unknown statement type : " + whereClauseAttributeValue);
				} else if (whereClauseAttributeValue != null) {
					buf.append("=?");
				} else {
					buf.append(" is null");
				}
			}
		}
	}
	
	/**
	 * Retourne l'identifiant g�n�r� automatiquement par la base de donn�es.
	 * @param pStmt le statement SQL.
	 * @return l'identifiant g�n�r�.
	 * @throws SQLException lev� en cas d'incident technique d'acc�s � la base.
	 * @throws ClassDescriptorException lev� en cas d'incident lors de l'acc�s
	 * 		aux attributs de l'instance. 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws DbClassMappingException 
	 */
	private void getAutoGeneratedKey(PreparedStatement pStmt, TYPE instance) throws SQLException, IllegalArgumentException, IllegalAccessException {
		PreparedStatement pStmt1 = null;
		try {
			// Pas de g�n�ration si aucun attribut auto g�n�r� n'est sp�cifi�
			if (autoGeneratedAttribute!=null) {
				// R�cup�ration de la connexion
				Connection con = pStmt.getConnection();
				// Cas de HSQLDB
				if (isHSQLDB(con)) {
					if (log.isDebugEnabled())
						log.debug("HSQL Database detected");
					pStmt1 = con.prepareStatement("call identity()");
					ResultSet rs = pStmt1.executeQuery();
					if (!rs.next())
						throw new IllegalStateException("Error while retrieving auto generated key");
					resultSetColumnToInstanceAttribute(rs, 1, instance, autoGeneratedAttribute);
					
					// Fermeture du statement
					pStmt1.close();
					pStmt1 = null;
				}
				else {
					if (log.isDebugEnabled())
						log.debug("Generic Database detected");
					// R�cup�ration de l'identifiant g�n�r�
					ResultSet rs = pStmt.getGeneratedKeys();
					if (!rs.next())
						throw new IllegalStateException("Error while retrieving auto generated key");
					resultSetColumnToInstanceAttribute(rs, 1, instance, autoGeneratedAttribute);
				}
			}
		}
		finally {
			if (pStmt1!=null) try { pStmt1.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Indique si la BDD de donn�es est une base HSQLDB.
	 * @param con la connexion SQL.
	 * @return un bool�en indiquant si la BDD est de type HSQLDB.
	 * @throws SQLException lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static boolean isHSQLDB(Connection con) throws SQLException {
		// R�cup�ration du nom de la base de donn�es
		String dbName = con.getMetaData().getDatabaseProductName();
		if (log.isDebugEnabled())
			log.debug("DbName=" + dbName);
		return "HSQL Database Engine".equals(dbName);
	}



}
