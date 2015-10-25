/** SqliteJdbc.java 
 * 
 * JDBC communication with the SQL database.
 * 
 * This file is self-contained. It can be tested by itself.
 * 
 * @since 0.1
 * @author Nick Hatzigeorgiu
 * @version 0.1
 */

package nisqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@ClassInfo(created = "July 5, 2015", createdBy = "Nick", lastModified = "August 17, 2015", lastModifiedBy = "Nick", revision = @Revision(major = 0, minor = 1))
public class SqliteJdbc {

	// These parameters contain all the SQL information and results
	private String filename = "--- Select a file ---"; // SQLite Filename
	private Connection conn = null; // JDBC connection to DB
	private boolean dbLoaded = false; // SQLite DB is loaded or not
	private String sqlCommand = ""; // SQL command to run
	private boolean sqlIsSelect = false; // SQL command is SELECT or not, only for select we have a result set
	private ResultSet rs = null; // The SELECT result set
	private ObservableList<String> tableList = FXCollections.observableArrayList("");
	private String sqlResultMessage = ""; // SQL result string
	private boolean sqlHasError = false; // SQL execution resulted in an error
	private String sqlErrorMessage = ""; // the error message

	public SqliteJdbc() {
		super();
	}

	public SqliteJdbc(String path) {
		initialize();
		String fname = "jdbc:sqlite:" + path.replace("\\", "/");
		if (fname.startsWith("---")) {
			return;
		}
		try {
			conn = DriverManager.getConnection(fname);
			dbLoaded = true;
			filename = path;
			Statement statement = conn.createStatement();
			ResultSet rsTables = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
			rsTables.close();
		} catch (SQLException e) {
			// System.err.println(e.getMessage());
			conn = null;
			dbLoaded = false;
			filename = path;
			sqlHasError = true;
			sqlErrorMessage = e.getMessage();
		}
		// }
	}

	/**
	 * getFileName
	 * 
	 * @return filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * getFileName
	 * 
	 * @return filename
	 */
	public boolean isLoaded() {
		return dbLoaded;
	}

	/**
	 * getResultMessage
	 * 
	 * @return sqlResultMessage
	 */
	public String getResultMessage() {
		return sqlResultMessage;
	}

	/**
	 * getError
	 * 
	 * @return sqlResultMessage
	 */
	public String getError() {
		return sqlErrorMessage;
	}

	/**
	 * hasError
	 * 
	 * @return sqlHasError
	 */
	public boolean hasError() {
		return sqlHasError;
	}

	/**
	 * isSelect
	 * 
	 * @return sqlIsSelect
	 */
	public boolean isSelect() {
		return sqlIsSelect;
	}

	/**
	 * getSql
	 * 
	 * @return sqlCommand
	 */
	public String getSql() {
		return sqlCommand;
	}

	/**
	 * getTableList
	 * 
	 * @return tableList
	 */
	public final ObservableList<String> getTableList() {
		getTables();
		return tableList;
	}

	/**
	 * getResult
	 * 
	 * @return rs
	 */
	public ResultSet getResult() {
		return rs;
	}

	/**
	 * initialize - Initializes all parameters for a new connection
	 * 
	 */
	public void initialize() {
		sqlCommand = "";
		sqlIsSelect = false;
		rs = null;
		sqlResultMessage = "";
		sqlHasError = false;
		sqlErrorMessage = "";
		tableList.clear();
	}

	/**
	 * close - Closes the connection
	 * 
	 */
	public void close() {
		if (dbLoaded) {
			try {
				conn.close();
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
		conn = null;
		dbLoaded = false;
		filename = "";
		initialize();
	}

	public StringProperty getTablesString() {
		return (StringProperty) tableList;

	}

	/**
	 * getTables - Gets a list of database tables
	 * 
	 */
	public void getTables() {
		tableList.clear();
		if (dbLoaded) {
			Statement statement;
			try {
				statement = conn.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 sec.
				ResultSet rsTables = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
				while (rsTables.next()) {
					String s = rsTables.getString("name");
					if (!s.equals("sqlite_sequence")) {
						tableList.add(s);
					}
				}
			} catch (SQLException e) {
				// Something went wrong, initialize and report
				initialize();
				// e.printStackTrace();
			}
		}
	}

	/**
	 * isSelect - Returns true if it is a SELECT query
	 * 
	 */
	public boolean isSelect(String sql) {
		boolean result = false;
		if (sql.trim().toLowerCase().startsWith("select"))
			result = true;
		return result;
	}

	/**
	 * getFields - Gets a list of fields for a single table
	 * 
	 */
	public List<String> getFields(String tablename) {
		List<String> res = new ArrayList<String>();
		if (!isLoaded())
			return res;
		String sql = "select * from " + tablename + " LIMIT 0";
		Statement statement;
		try {
			statement = conn.createStatement();
			ResultSet rsFields = statement.executeQuery(sql);
			ResultSetMetaData mrs = rsFields.getMetaData();
			for (int i = 1; i <= mrs.getColumnCount(); i++) {
				Object row[] = new Object[3];
				row[0] = mrs.getColumnLabel(i);
				/*
				 * row[1] = mrs.getColumnTypeName(i); row[2] = mrs.getPrecision(i);
				 */
				res.add(row[0].toString());
			}
		} catch (SQLException e) {
			// Something went wrong, initialize and report
			initialize();
			e.printStackTrace();
		}

		return res;
	}

	public ResultSet getContents(String tablename) {
		ResultSet lc = null;
		if (!isLoaded())
			return lc;
		String sql = "select * from " + tablename;
		Statement statement;
		try {
			statement = conn.createStatement();
			lc = statement.executeQuery(sql);

		} catch (SQLException e) {
			// Something went wrong, initialize and report
			e.printStackTrace();
		}
		return lc;
	}

	/**
	 * runSql - Runs an SQL query
	 * 
	 */
	public void runSql(String sql) {
		initialize();
		sql = sql.trim();
		sqlCommand = sql;
		if (dbLoaded) {
			Statement statement;
			try {
				statement = conn.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 sec.
				if (isSelect(sql)) { // SELECT query
					sqlIsSelect = true;
					rs = statement.executeQuery(sql);
					sqlResultMessage = "OK";
					sqlHasError = false;
					sqlErrorMessage = "";
				} else {// not a SELECT query (UPDATE, INSERT, ALTER etc)
					sqlIsSelect = false;
					statement.execute(sql);
					rs = null;
					sqlResultMessage = "OK";
					sqlHasError = false;
					sqlErrorMessage = "";
				}
			} catch (SQLException e) {
				sqlResultMessage = "Error";
				sqlHasError = true;
				sqlErrorMessage = e.getMessage();
				if (sqlErrorMessage.startsWith("[SQLITE_NOTADB]")) { // db is not loaded
					rs = null;
					conn = null;
					dbLoaded = false;
				}
			}
		}
	}

	/**
	 * testMyCLass - Tests this class.
	 * 
	 */
	public static void testMyCLass() {

		SqliteJdbc sq = new SqliteJdbc("C:/Users/Nick/workspace/jsqliten/src/jsqliten/sample_db1.sqlite");

		if (!sq.isLoaded()) {
			System.out.println("Database could not be loaded!");
			return;
		}

		System.out.println("Database filename: " + sq.getFilename());
		System.out.println("Tables found: " + sq.getTableList());
		System.out.println("Fields for table 'people': " + sq.getFields("people"));

		String mysql = "select * from people";
		testPrintMyCLass(1, sq, mysql);

		mysql = "sselect * from people";
		testPrintMyCLass(2, sq, mysql);

		mysql = "insert into people (id, firstname, lastname, middlename, dob) values (999, 'John', 'Doe', 'N.', '1920-12-31')";
		testPrintMyCLass(3, sq, mysql);

		mysql = "delete from people where id = 999)";
		testPrintMyCLass(4, sq, mysql);

		mysql = "delete from people where id = 999";
		testPrintMyCLass(5, sq, mysql);

		mysql = "SELECT * FROM people AS p INNER JOIN people_addr AS pa ON pa.id=p.id INNER JOIN address AS a ON a.addr_id=pa.addr_id";
		testPrintMyCLass(6, sq, mysql);

		sq = new SqliteJdbc("C:/Users/Nick/workspace/jsqliten/src/jsqliten/example_sql.txt");
		mysql = "select * from people";
		testPrintMyCLass(7, sq, mysql);

		sq.close();
	}

	public static void testPrintMyCLass(int n, SqliteJdbc sq, String mysql) {
		sq.runSql(mysql);
		System.out.println("\nTest Query " + n);
		System.out.println("IsLoaded: " + sq.isLoaded());
		System.out.println("Query: " + sq.getSql());
		System.out.println("Result message: " + sq.getResultMessage());
		System.out.println("Is it a select query? " + sq.isSelect());
		System.out.println("Has error?  " + sq.hasError());
		System.out.println("Error: " + sq.getError());
		System.out.println("ResultSet: " + sq.getResult());
	}

	public static void main(String[] args) {
		System.out.println("START");
		testMyCLass();
		System.out.println("END");
	}
}
