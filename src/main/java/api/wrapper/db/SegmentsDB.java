package api.wrapper.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SegmentsDB {

	/**
	 * Connect to the segments.db database 
	 */
	private Connection connect() throws ClassNotFoundException {
		// SQLite connection string
		String url = "jdbc:sqlite:./resources/sqlite/db/segments.db";
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} 
		return conn;
	}

	/**
	 * Insert a new row into the warehouses table
	 *
	 */
	public void insert(String sid, String streetDut, String streetFre, String position) throws ClassNotFoundException {
		String sql = "INSERT or IGNORE INTO segments(sid,streetDut,streetFre,position) VALUES(?,?,?,?)";

		try (Connection conn = this.connect();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, sid);
			pstmt.setString(2, streetDut);
			pstmt.setString(3, streetFre);
			pstmt.setString(4, position);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public String[] queryData(String sid) throws ClassNotFoundException {
		String sql = "SELECT * FROM segments WHERE sid = ?";
		String std = null;
		String stf = null;
		String pos = null;
		try (Connection conn = this.connect();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, sid);

			ResultSet rs  = pstmt.executeQuery();

			while (rs.next()) {
				std = rs.getString("streetDut");
				stf = rs.getString("streetFre");
				pos = rs.getString("position");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return new String[] {std, stf, pos};
	}

	public void createTable() throws ClassNotFoundException {
		String sql = "CREATE TABLE IF NOT EXISTS segments" +
				"(sid text PRIMARY KEY," +
				"streetDut text NOT NULL," +
				"streetFre text NOT NULL," +
				"position text NOT NULL)";

		Connection conn = this.connect();
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

}
