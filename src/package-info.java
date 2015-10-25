/** package-info.java 
 * 
 * nisqlite
 * 
 * A Java GUI for SQLite databases.
 * Created with JavaFX.
 * Developed as the final project of the x436 Java class. 
 * 
 * Information on SQLite: https://www.sqlite.org/
 * Information on SQLite JDBC driver: https://bitbucket.org/xerial/sqlite-jdbc
 * Information on JavaFX: https://en.wikipedia.org/wiki/JavaFX
 * 
 * Requirements:
 * 				1. Java 8u40 or later
 * 				2. sqlite-jdbc-3.8.10.1.jar
 * 
 * @since 0.1
 * @author Nick Hatzigeorgiu
 * @version 0.1
 */

package nisqlite;

@interface Revision {
	int major() default 0;

	int minor() default 1;
}

@interface ClassInfo {
	String created();

	String createdBy();

	String lastModified();

	String lastModifiedBy();

	Revision revision();
}
