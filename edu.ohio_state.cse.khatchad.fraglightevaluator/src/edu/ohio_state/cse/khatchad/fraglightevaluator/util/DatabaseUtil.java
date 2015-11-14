/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglightevaluator.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.core.javaelements.IAJCodeElement;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.postgresql.ds.PGPoolingDataSource;

import ca.mcgill.cs.swevo.jayfx.model.IElement;
import edu.ohio_state.cse.khatchad.fraglight.core.analysis.Constants;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.IntentionArc;
import edu.ohio_state.cse.khatchad.fraglight.core.graph.Pattern;
import edu.ohio_state.cse.khatchad.fraglight.core.util.AJUtil;

/**
 * @author raffi
 * 
 */
public class DatabaseUtil {
	
	private static boolean DATABASE_ON = true;

	public enum AdviceShadowRelationship {
		ADVISES("advises_shadow"), HAS_BEEN_SUGGESTED_TO_ADVISE(
				"suggested_shadows");

		private String relationshipTableName;

		AdviceShadowRelationship(String relationshipTableName) {
			this.relationshipTableName = relationshipTableName;
		}

		public String getRelationshipTableName() {
			return this.relationshipTableName;
		}
	}

	private DatabaseUtil() {
	}

	/**
	 * @param javaElem
	 * @return
	 */
	public static String getKey(IJavaElement javaElem) {
		StringBuilder key = new StringBuilder(javaElem.getHandleIdentifier());
		key.delete(0, key.indexOf("<") + 1);
		int pos = key.indexOf("!");
		if (pos != -1)
			key.delete(pos, key.length() - 1);
		return key.toString();
	}

	/**
	 * @param adviceKey
	 * @param shadowKey
	 * @param version
	 * @throws Exception
	 */
	public static void insertAdviceShadowRelationshipIntoDatabase(
			String adviceKey, String shadowKey, int version,
			AdviceShadowRelationship relationship) throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		Connection conn = null;
		Statement st = null;
		ResultSet res = null;
		try {
			conn = getConnection();
			st = conn.createStatement();

			res = st.executeQuery("select 1 from "
					+ relationship.getRelationshipTableName()
					+ " where advice_key = '" + adviceKey + "' and version = "
					+ version + " and shadow_key = '" + shadowKey + "'");
			if (!res.next())
				st.executeUpdate("insert into "
						+ relationship.getRelationshipTableName()
						+ " values ('" + adviceKey + "', " + "'" + shadowKey
						+ "', " + version + ")");
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			closeConnections(conn, st, res);
		}
	}

	/**
	 * @param conn
	 * @param st
	 * @param res
	 * @throws SQLException
	 */
	private static void closeConnections(Connection conn, Statement st,
			ResultSet res) throws SQLException {
		
		if ( !DATABASE_ON )
			return;
		
		if (res == null)
			throw new IllegalArgumentException(
					"Trying to close a null result set.");

		res.close();
		closeConnections(conn, st);
	}

	/**
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static Connection getConnection() throws ClassNotFoundException,
			SQLException {
		Class.forName(Constants.DB_DRIVER);

		PGPoolingDataSource source = PGPoolingDataSource
				.getDataSource(Constants.DB_DATASOURCE_NAME);
		if (source == null) {
			source = new PGPoolingDataSource();
			source.setDatabaseName(Constants.DB_NAME);
			source.setDataSourceName(Constants.DB_DATASOURCE_NAME);
			source.setPassword(Constants.DB_PASS);
			source.setServerName(Constants.DB_SERVER);
			source.setUser(Constants.DB_USER);
		}

		return source.getConnection();
	}

	public static void insertAdviceShadowRelationshipIntoDatabase(
			String adviceKey, String shadowKey, int version,
			Pattern<IntentionArc<IElement>> pattern, double confidence,
			AdviceShadowRelationship relationship) throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		Connection conn = null;
		Statement st = null;
		ResultSet res = null;
		try {
			conn = getConnection();
			st = conn.createStatement();

			res = st.executeQuery("select 1 from "
					+ relationship.getRelationshipTableName()
					+ " where advice_key = '" + adviceKey + "' and version = "
					+ version + " and shadow_key = '" + shadowKey
					+ "' and confidence = " + confidence);
			if (!res.next())
				st.executeUpdate("insert into "
						+ relationship.getRelationshipTableName()
						+ " values ('" + adviceKey + "', " + "'" + shadowKey
						+ "', " + version + ", " + confidence + ", " + "'"
						+ pattern + "')");
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			closeConnections(conn, st, res);
		}
	}

	/**
	 * @param javaElem
	 * @param benchmark
	 * @param shadowKey
	 * @param elemName
	 * @param enclosingMethodName
	 * @param className
	 * @param packageName
	 * @param version
	 * @throws Exception
	 */
	public static void insertShadowIntoDatabase(IJavaElement javaElem,
			String benchmark, String shadowKey, String elemName,
			String enclosingMethodName, String className, String packageName,
			int version) throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		final String query = "insert into shadows values ("
				+ "'"
				+ shadowKey
				+ "', "
				+ "'"
				+ javaElem.getClass().getSimpleName()
				+ "', "
				+ "'"
				+ benchmark
				+ "', "
				+ "'"
				+ packageName
				+ "', "
				+ "'"
				+ className
				+ "', "
				+ (enclosingMethodName == null ? "NULL" : "'"
						+ enclosingMethodName + "'") + "," + "'" + elemName
				+ "', " + version + ")";

		Connection conn = null;
		Statement st = null;
		ResultSet res = null;

		try {
			conn = getConnection();
			st = conn.createStatement();

			res = st.executeQuery("select 1 from shadows where key = '"
					+ shadowKey + "' and version = " + version);
			if (!res.next())
				st.executeUpdate(query);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			closeConnections(conn, st, res);
		}
	}

	/**
	 * @param adviceKey
	 * @param benchmark
	 * @param packageName
	 * @param aspectName
	 * @throws Exception
	 */
	private static void insertAdviceIntoDatabase(String adviceKey,
			String benchmark, String packageName, String aspectName)
			throws Exception {

		if ( !DATABASE_ON )
			return;
		
		Connection conn = null;
		Statement st = null;
		ResultSet res = null;

		try {
			conn = getConnection();
			st = conn.createStatement();

			res = st.executeQuery("select 1 from advice where key = '"
					+ adviceKey + "'");
			if (!res.next())
				st.executeUpdate("insert into advice values (" + "'"
						+ adviceKey + "', " + "'" + benchmark + "', " + "'"
						+ packageName + "', " + "'" + aspectName + "')");
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			closeConnections(conn, st, res);
		}
	}

	private static void updateAdviceInDatabase(String adviceKey,
			String benchmark, String packageName, String aspectName)
			throws Exception {
		
		if ( !DATABASE_ON )
			return;

		Connection conn = null;
		Statement st = null;

		try {
			conn = getConnection();
			st = conn.createStatement();

			String sql = "update advice set " + "benchmark = " + "'"
					+ benchmark + "', " + "package = " + "'" + packageName
					+ "', " + "aspect = " + "'" + aspectName + "' "
					+ "where key = '" + adviceKey + "'";

			st.executeUpdate(sql);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			closeConnections(conn, st);
		}
	}

	/**
	 * @param conn
	 * @param st
	 * @throws SQLException
	 */
	private static void closeConnections(Connection conn, Statement st)
			throws SQLException {
		
		if ( !DATABASE_ON )
			return;
		
		if (st == null)
			throw new IllegalArgumentException(
					"Trying to close a null statement.");
		else
			st.close();
		if (conn == null)
			throw new IllegalArgumentException(
					"Trying to close a null connection.");
		else
			conn.close();
	}

	/**
	 * @param benchmark
	 * @return
	 */
	public static int getVersionNumber(String benchmark) {
		int underscorePos = benchmark.indexOf('_');

		if (benchmark.startsWith("HealthWatcher")) {
			String number;
			try {
				number = benchmark.substring(underscorePos + 1,
						underscorePos + 3);
			}
			catch (IndexOutOfBoundsException e) {
				return 0;
			}

			try {
				return Integer.valueOf(number);
			}
			catch (NumberFormatException e) {
				return 0;
			}
		}

		else {
			String number;
			try {
				number = String.valueOf(benchmark.charAt(underscorePos + 2));
			}
			catch (IndexOutOfBoundsException e) {
				return 0;
			}

			try {
				return Integer.valueOf(number);
			}
			catch (NumberFormatException e) {
				return 0;
			}
		}
	}

	/**
	 * @param adviceKey
	 * @param javaElem
	 * @throws Exception
	 */
	public static void insertShadowAndRelationshipIntoDatabase(
			String adviceKey, IJavaElement javaElem,
			AdviceShadowRelationship relationship) throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		String benchmark = null;
		try {
			benchmark = javaElem.getJavaProject().getProject().getName();
		}
		catch (RuntimeException e) {
			return;
		}
		String shadowKey = getKey(javaElem);

		String elemName = javaElem.getElementName();

		String enclosingMethodName = null;
		String className = null;
		String packageName = null;

		if (javaElem instanceof IAJCodeElement)
			enclosingMethodName = javaElem.getParent().getElementName();

		if (javaElem instanceof IAJCodeElement) {
			className = javaElem.getParent().getParent().getElementName();
			if (javaElem.getParent().getParent() instanceof ICompilationUnit) //its a field set/get
				if (javaElem.getParent() instanceof IType)
					packageName = ((IType) javaElem.getParent())
							.getPackageFragment().getElementName();
				else if (javaElem.getParent().getParent().getParent() instanceof IPackageFragment)
					packageName = ((IPackageDeclaration) javaElem.getParent())
							.getElementName();
				else {
					if (!(javaElem.getParent().getParent() instanceof IType))
						packageName = ((IType) javaElem.getParent().getParent()
								.getParent()).getPackageFragment()
								.getElementName();
					else
						packageName = ((IType) javaElem.getParent().getParent())
								.getPackageFragment().getElementName();
				}
		}
		else {
			className = javaElem.getParent().getElementName();
			packageName = ((IType) javaElem.getParent()).getPackageFragment()
					.getElementName();
		}

		int version = getVersionNumber(benchmark);

		insertShadowIntoDatabase(javaElem, benchmark, shadowKey, elemName,
				enclosingMethodName, className, packageName, version);

		insertAdviceShadowRelationshipIntoDatabase(adviceKey, shadowKey,
				version, relationship);
	}

	public static void insertShadowAndRelationshipIntoDatabase(
			String adviceKey, IJavaElement javaElem,
			Pattern<IntentionArc<IElement>> pattern, double confidence,
			AdviceShadowRelationship relationship) throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		String benchmark = javaElem.getJavaProject().getProject().getName();
		String shadowKey = getKey(javaElem);

		String elemName = javaElem.getElementName();

		String enclosingMethodName = null;
		String className = null;
		String packageName = null;

		if (javaElem instanceof IAJCodeElement)
			enclosingMethodName = javaElem.getParent().getElementName();

		if (javaElem instanceof IAJCodeElement) {
			className = javaElem.getParent().getParent().getElementName();
			packageName = ((IType) javaElem.getParent().getParent())
					.getPackageFragment().getElementName();
		}
		else {
			className = javaElem.getParent().getElementName();
			if (javaElem instanceof IType) //most likely a default ctor.
				packageName = ((IType) javaElem).getPackageFragment()
						.getElementName();
			else
				packageName = ((IType) javaElem.getParent())
						.getPackageFragment().getElementName();
		}

		int version = getVersionNumber(benchmark);

		insertShadowIntoDatabase(javaElem, benchmark, shadowKey, elemName,
				enclosingMethodName, className, packageName, version);

		insertAdviceShadowRelationshipIntoDatabase(adviceKey, shadowKey,
				version, pattern, confidence, relationship);
	}

	/**
	 * @param advElem
	 * @throws Exception
	 */
	@SuppressWarnings("restriction")
	public static void insertIntoDatabase(final AdviceElement advElem)
			throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		String adviceKey = getKey(advElem);
		try {
			String benchmark = advElem.getJavaProject().getProject().getName();
			String aspectName = advElem.getParent().getElementName();
			String packageName = ((IType) advElem.getParent())
					.getPackageFragment().getElementName();

			insertAdviceIntoDatabase(adviceKey, benchmark, packageName,
					aspectName);

			for (IJavaElement javaElem : AJUtil.getAdvisedJavaElements(advElem))
				insertShadowAndRelationshipIntoDatabase(adviceKey, javaElem,
						AdviceShadowRelationship.ADVISES);
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param advElem
	 */
	@SuppressWarnings("restriction")
	public static void updateInDatabase(AdviceElement advElem) {
		
		if ( !DATABASE_ON )
			return;
		
		String adviceKey = getKey(advElem);
		try {
			String benchmark = advElem.getJavaProject().getProject().getName();
			String aspectName = advElem.getParent().getElementName();
			String packageName = ((IType) advElem.getParent())
					.getPackageFragment().getElementName();

			updateAdviceInDatabase(adviceKey, benchmark, packageName,
					aspectName);
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param adviceKey
	 * @param suggestedJavaElement
	 * @param pattern
	 * @param confidence
	 * @throws Exception
	 */
	public static void updatePatternInDatabase(String adviceKey,
			String shadowKey, int version,
			Pattern<IntentionArc<IElement>> pattern, double confidence)
			throws Exception {
		
		if ( !DATABASE_ON )
			return;
		
		Connection conn = null;
		Statement st = null;

		try {
			conn = getConnection();
			st = conn.createStatement();

			String sql = "update "
					+ AdviceShadowRelationship.HAS_BEEN_SUGGESTED_TO_ADVISE
							.getRelationshipTableName() + " set "
					+ "pattern = " + "'" + pattern + "' "
					+ "where advice_key = '" + adviceKey + "' "
					+ "and shadow_key = '" + shadowKey + "' "
					+ "and version = " + version + " " + "and confidence = "
					+ confidence;

			st.executeUpdate(sql);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			closeConnections(conn, st);
		}
	}
}