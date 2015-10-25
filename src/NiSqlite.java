/** NiSqlite.java 
 * 
 * The main file for the SQLite GUI.
 * 
 * @since 0.1
 * @author Nick Hatzigeorgiu
 * @version 0.1
 */

package nisqlite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * NiSqlite is a JavaFX Application - Opens the SQLite GUI
 * 
 * @author Nick
 *
 */
@ClassInfo(created = "July 5, 2015", createdBy = "Nick", lastModified = "August 5, 2015", lastModifiedBy = "Nick", revision = @Revision(major = 0, minor = 1))
public class NiSqlite extends Application {

	private final String APP_TITLE = "NiSqlite - An SQLite Manager";
	private static Stage pStage; // the main window stage
	SqliteJdbc sq = new SqliteJdbc(); // this is the sqlite class
	private String userPath = Paths.get(".").toAbsolutePath().normalize().toString(); // default path for open dialogs

	// GUI controls
	Label dbFileStr = new Label("");
	TextField sqlStr = new TextField("");
	Label lastRunStr = new Label("");
	Label resultStr = new Label();
	Tooltip tooltipResultMessage = new Tooltip(""); // tooltip for result message
	Tooltip tooltipLastSQL = new Tooltip(""); // tooltip for last SQL

	private ObservableList<String> tableList = FXCollections.observableArrayList();
	ListView<String> tablesListView = new ListView<String>(); // this is a list of tables in database

	ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
	private TableView<ObservableList<String>> resultsTableView = new TableView<ObservableList<String>>(data);

	/**
	 * runSQL - Runs an SQL and sets the results
	 */
	private void runSQL(String sql, String tableName) {
		String resMsg = "--- none ---";
		sql = sql.trim();
		sqlStr.clear();
		sqlStr.setPromptText("SELECT * FROM table");
		resultStr.setText("--- none ---");
		tooltipResultMessage.setText("--- none ---");

		if (sq == null || !sq.isLoaded()) {
			resMsg = "No database loaded.";
			resultStr.setText(resMsg);
			tooltipResultMessage.setText(resMsg);
			System.out.println("Nothing to run.");
			return;
		} else if (sql.equals("")) {
			resMsg = "No SQL to run.";
			resultStr.setText(resMsg);
			tooltipResultMessage.setText(resMsg);
			System.out.println("Nothing to run.");
			return;
		}

		// Run the sql and print the results
		sq.runSql(sql);
		lastRunStr.setText(sql);
		tooltipLastSQL.setText(sql);
		resMsg = sq.getResultMessage();
		if (sq.hasError())
			resMsg += ":\n" + sq.getError();
		resultStr.setText(resMsg);
		tooltipResultMessage.setText(resMsg);
		tableRefresh();

		// If it was a SELECT query, reset the table of results
		if (sq.isSelect()) {
			try {
				showResults();
			} catch (SQLException e) {
				System.err.println("Error in SELECT statement execution. Detailed information follows.");
				e.printStackTrace();
			}
			System.out.println("Select query: " + sql);
		}

		System.out.println("Finished running SQL: " + sql);
	}

	/**
	 * showResults - Fills the table of results.
	 */
	@SuppressWarnings("unchecked")
	private void showResults() throws SQLException {

		int columnCount = 0;
		String colName = "";

		ResultSet rsm = sq.getResult();
		ResultSetMetaData rsmd = null;

		rsmd = rsm.getMetaData();
		columnCount = rsmd.getColumnCount();
		// Get the headers and create columns
		for (int i = 0; i < columnCount; i++) {
			colName = rsmd.getColumnName(i + 1);
			final int k = i;
			if (colName != null && !colName.isEmpty()) {
				TableColumn<ObservableList<String>, String> col = new TableColumn<ObservableList<String>, String>(
						colName);
				col.setCellValueFactory(new Callback<CellDataFeatures<ObservableList<String>, String>, ObservableValue<String>>() {
					public ObservableValue<String> call(CellDataFeatures<ObservableList<String>, String> param) {
						return new SimpleStringProperty(param.getValue().get(k).toString());
					}
				});
				resultsTableView.getColumns().addAll(col);
			}
		}

		// Read the record set
		while (rsm.next()) {
			ObservableList<String> row = FXCollections.observableArrayList();

			for (int i = 0; i < columnCount; i++) {
				String sw = rsm.getString(i + 1);
				if (sw == null) {
					sw = "";
				}
				row.add(sw);
			}
			// System.out.println("Row added " + row);
			data.add(row);
		}

		resultsTableView.setItems(data);
	}

	/**
	 * menuOpenDatabase - Finds if the filename exists and is an SQLite file, and opens it
	 */
	private void menuOpenDatabase() {
		startDatabase(false);
		System.out.println("Open database.");
	}

	/**
	 * menuNewDatabase - Creates a new database.
	 */
	private void menuNewDatabase() {
		startDatabase(true);
		System.out.println("Create new database.");
	}

	/**
	 * startDatabase - Either creates a new or opens an existing database.
	 */
	private void startDatabase(boolean newDB) {
		final FileChooser fileChooser = new FileChooser();

		String userDirectoryString = "";
		if ((new File(userPath).exists()) && (new File(userPath).isDirectory())) {
			userDirectoryString = userPath;
		}
		userDirectoryString = userPath;
		File userDirectory = new File(userDirectoryString);
		if (!userDirectory.canRead()) {
			userDirectory = new File("");
		}
		fileChooser.setInitialDirectory(userDirectory);
		File file = null;
		if (newDB) {
			file = fileChooser.showSaveDialog(pStage);
		} else {
			file = fileChooser.showOpenDialog(pStage);
		}

		if (file != null) {
			initializeControls();
			String filename = file.toString();
			dbFileStr.setText(filename);
			sq = new SqliteJdbc(filename);

			listRefresh();
			if (sq.hasError()) {
				String msg = "Error:\n" + sq.getError();
				resultStr.setText(msg);
				tooltipResultMessage.setText(msg);
				userPath = Paths.get(".").toAbsolutePath().normalize().toString();
				System.out.println("Could not open: " + file);
			} else {
				userPath = file.getParent();
				resultStr.setText("--- none ---");
				tooltipResultMessage.setText("--- none ---");
				System.out.println("Selected database: " + file);
			}

		}
	}

	/**
	 * listRefresh - Updates the list of db tables
	 */
	private void listRefresh() {
		tablesListView.getSelectionModel().select(null);
		tableList.removeAll(tableList);
		if (sq != null)
			tableList.addAll(sq.getTableList());
	}

	/**
	 * tableRefresh - Clears the data table.
	 */
	private void tableRefresh() {
		data.removeAll(data);
		resultsTableView.getColumns().removeAll(resultsTableView.getColumns());
	}

	/**
	 * buttonSelectDatabase - Select and open the database
	 */
	private void buttonSelectDatabase() {
		startDatabase(false);
		System.out.println("Button select database.");
	}

	/**
	 * menuCloseDatabase - Close the database.
	 */
	private void menuCloseDatabase() {
		initializeControls();
		System.out.println("Database was closed.");
	}

	/**
	 * menuAbout - Show About information from help menu.
	 */
	private void menuAbout() {
		// Alert control requires JavaFX 8u40 or later!

		String title = "NiSqlite";
		String description = "Java GUI for SQLite Databases";
		String version = "Version 0.1\nAugust 2015";

		// Older versions than Java 8u40
		System.out.println("Opening an Alert requires Java 8u40 or later.");
		System.out.println(title);
		System.out.println(description);
		System.out.println(version);

		try {
			Class.forName("javafx.scene.control.Alert");
			javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
					javafx.scene.control.Alert.AlertType.INFORMATION);
			alert.setTitle(title);
			alert.setHeaderText(description);
			alert.setContentText(version);
			alert.showAndWait();
		} catch (Exception ex) {

		} catch (Error er) {

		}

	}

	/**
	 * buttonRunQuery - Run a query
	 */
	private void buttonRunQuery(Stage primaryStage) {
		String sql = sqlStr.getText().trim();
		runSQL(sql, "");
		System.out.println("Button run query.");
	}

	/**
	 * listSelectTable - Show table contains
	 */
	private void listSelectTable(String tableName) {
		String sql = "SELECT * FROM " + tableName;
		runSQL(sql, tableName);
		System.out.println("List table contents of table: " + tableName);
	}

	/**
	 * initializeControls - Initializes the strings of the interface and the objects.
	 * 
	 * This should only be run when: starting the GUI, opening a db, creating a new db, closing a db.
	 */
	private void initializeControls() {
		dbFileStr.setText("--- Select a file ---");
		sqlStr.setPromptText("SELECT * FROM table");
		lastRunStr.setText("--- none ---");
		tooltipLastSQL.setText("--- none ---");
		resultStr.setText("--- none ---");
		tooltipResultMessage.setText("--- none ---");

		if (sq != null)
			sq.close();
		sq = new SqliteJdbc();
		tablesListView = new ListView<String>();

		listRefresh();
		tableRefresh();
	}

	/**
	 * createMenus - Creates the Menu Bar for the GUI.
	 */
	private MenuBar createMenus() {
		MenuBar menuBar = new MenuBar();

		// File menu - new, open, close, save, saveAs, exit
		Menu fileMenu = new Menu("File");
		MenuItem newMenuItem = new MenuItem("New Database");
		newMenuItem.setOnAction(actionEvent -> menuNewDatabase());
		MenuItem openMenuItem = new MenuItem("Open Database");
		openMenuItem.setOnAction(actionEvent -> menuOpenDatabase());
		MenuItem closeMenuItem = new MenuItem("Close Database");
		closeMenuItem.setOnAction(actionEvent -> menuCloseDatabase());
		MenuItem exitMenuItem = new MenuItem("Exit");
		exitMenuItem.setOnAction(actionEvent -> Platform.exit());

		fileMenu.getItems().addAll(newMenuItem, openMenuItem, closeMenuItem, new SeparatorMenuItem(), exitMenuItem);

		// Help menu - about button
		Menu helpMenu = new Menu("Help");
		MenuItem aboutMenuItem = new MenuItem("About");
		helpMenu.getItems().addAll(aboutMenuItem);
		aboutMenuItem.setOnAction(actionEvent -> {
			menuAbout();
		});

		menuBar.getMenus().addAll(fileMenu, helpMenu);
		return menuBar;
	}

	/**
	 * start - Creates the main GUI.
	 */
	@Override
	public void start(Stage primaryStage) throws IOException {

		primaryStage.setTitle(APP_TITLE);
		BorderPane root = new BorderPane();
		Scene scene = new Scene(root, 800, 600, Color.GRAY);

		// Menus
		MenuBar menuBar = createMenus();
		root.setTop(menuBar);

		// Left side: DB tables list
		BorderPane bPaneL = new BorderPane();
		bPaneL.setPadding(new Insets(5));

		tablesListView.setPrefWidth(180);
		tablesListView.setMaxWidth(Double.MAX_VALUE);
		tablesListView.setPrefHeight(500);
		tablesListView.setMaxHeight(Double.MAX_VALUE);

		Label dbTablesLbl = new Label("DB Tables:");
		bPaneL.setTop(dbTablesLbl);
		bPaneL.setCenter(tablesListView);
		root.setLeft(bPaneL);

		// Right side: Selected DB file, SQL Command, Result, Result Grid

		BorderPane bPaneR = new BorderPane(); // spacing between child nodes
		bPaneR.setPadding(new Insets(5)); // padding between border and child nodes

		GridPane gridpane = new GridPane();
		gridpane.setPadding(new Insets(5));
		gridpane.setHgap(10);
		gridpane.setVgap(10);

		Label dbFileLbl = new Label("DB File:");

		Button fileBtn = new Button("Select File");
		fileBtn.setPrefWidth(70);
		gridpane.add(dbFileLbl, 0, 0);
		gridpane.add(dbFileStr, 1, 0);
		gridpane.add(fileBtn, 2, 0);

		// Select one SQLite file and open it
		fileBtn.setOnAction(actionEvent -> {
			buttonSelectDatabase();
		});

		Label sqlLbl = new Label("SQL Command:");

		dbFileStr.setPrefWidth(400);

		Button sqlRunBtn = new Button("Run");
		sqlRunBtn.setPrefWidth(70);
		gridpane.add(sqlLbl, 0, 1);
		gridpane.add(sqlStr, 1, 1);
		gridpane.add(sqlRunBtn, 2, 1);

		// Run the SQL command
		sqlRunBtn.setOnAction(actionEvent -> {
			buttonRunQuery(primaryStage);
		});

		Label lastRunLbl = new Label("Last SQL run:");
		gridpane.add(lastRunLbl, 0, 2);
		gridpane.add(lastRunStr, 1, 2);

		Label resultLbl = new Label("Result:");
		gridpane.add(resultLbl, 0, 3);
		resultStr.setPrefHeight(60);
		resultStr.setWrapText(true);

		gridpane.add(resultStr, 1, 3);

		bPaneR.setTop(gridpane);
		bPaneR.setCenter(resultsTableView);
		Tooltip.install(resultStr, tooltipResultMessage);
		Tooltip.install(lastRunStr, tooltipLastSQL);

		tablesListView.setPadding(new Insets(5));
		tablesListView.setItems(tableList);
		// What happens when we click a table name
		tablesListView.getSelectionModel().selectedItemProperty()
				.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
					listSelectTable(newValue);
				});

		root.setCenter(bPaneR);

		primaryStage.setScene(scene);
		primaryStage.show();
		pStage = primaryStage;
		initializeControls();

		// The following is needed to preserve width of text labels and buttons
		double len1 = sqlLbl.getLayoutBounds().getWidth() + 1;
		dbFileLbl.setPrefWidth(len1);
		dbFileLbl.setMinWidth(len1);
		dbFileLbl.setMaxWidth(len1);
		double len2 = sqlRunBtn.getLayoutBounds().getWidth() + 1;
		sqlRunBtn.setPrefWidth(len2);
		sqlRunBtn.setMinWidth(len2);
		sqlRunBtn.setMaxWidth(len2);
		double len3 = resultsTableView.getLayoutBounds().getWidth() - len2 - len1;
		dbFileStr.setPrefWidth(len3);

		resultsTableView.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth,
					Number newSceneWidth) {
				double len4 = resultsTableView.getLayoutBounds().getWidth() - len2 - len1;
				dbFileStr.setPrefWidth(len4);
			}
		});

	}

	public static void main(String[] args) {
		launch(args);
	}

}
