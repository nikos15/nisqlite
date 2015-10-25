NiSqlite

An SQLite database manager, build with JavaFX8. 


Requirements:
1. Java SE 8u40 or later.
2. The included JBDC driver: sqlite-jdbc-3.8.10.1.jar.


Included in the zip file:
1. Source code.
2. nisqli.bat, which can be used to compile and run the nisqlite sources (on Windows).
3. screen.png, a screen capture. 
4. sample_db1.sqlite, sample contacts database.
5. sample_db1.sqlite, sample DVD database.


Examples of JOIN SQL statements:
Here are two examples of complex SQL statements that can be run against the provided databases.
These work with copy-paste to the sql textbox in the GUI.

sample_db1.sqlite:
SELECT * FROM people AS p INNER JOIN people_addr AS pa ON pa.id=p.id INNER JOIN addresses AS a ON a.addr_id=pa.addr_id

sample_db2.sqlite:
SELECT * FROM dvds AS d INNER JOIN dvds_people AS dp ON dp.dvd_id=d.dvd_id INNER JOIN people AS p ON p.id=dp.id


Example of an error SQL statements:
SELECTT * FROM people


Notes:
1. Java 8u40 is needed only for the alert dialog
https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Alert.html
http://code.makery.ch/blog/javafx-dialogs-official/

2. Java 8 is needed thorough, because the application uses lambda expressions. 
 