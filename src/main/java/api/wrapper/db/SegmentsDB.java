package api.wrapper.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

public class SegmentsDB {

    private static final Logger LOG = LoggerFactory.getLogger(SegmentsDB.class);
    private static final HikariPool CONNECTION_POOL;

    static {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:resources/sqlite/db/segments.db");
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setPoolName("SQLiteConnectionPool");
//        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
//        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
//        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        DataSource dataSource = new HikariDataSource(hikariConfig);


        CONNECTION_POOL = new HikariPool(hikariConfig);
    }

    /**
     * Connect to the segments.db database
     *
     * @return
     * @throws ClassNotFoundException
     */
    private Connection connect() throws ClassNotFoundException {
        // SQLite connection string
        String url = "jdbc:sqlite:./resources/sqlite/db/segments.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            LOG.error("Error while connecting to DB.", e);
        }
        return conn;
    }

    private Connection pooledConnection() throws SQLException {
        return CONNECTION_POOL.getConnection();
    }

    /**
     * Insert a new row into the warehouses table
     *
     * @param sid
     * @param streetDut
     * @param streetFre
     * @param position
     * @throws ClassNotFoundException
     */
    public void insert(String sid, String streetDut, String streetFre, String position) throws ClassNotFoundException {
        String sql = "INSERT or IGNORE INTO segments(sid,streetDut,streetFre,position) VALUES(?,?,?,?)";
        PreparedStatement pstmt = null;
        try {
            Connection conn = this.pooledConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, sid);
            pstmt.setString(2, streetDut);
            pstmt.setString(3, streetFre);
            pstmt.setString(4, position);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error during query execution.", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
                LOG.error("Error closing statement.", e);
            }
        }
    }

    /**
     * @param sid
     * @return
     * @throws ClassNotFoundException
     */
    public String[] queryData(String sid)  {
        String sql = "SELECT * FROM segments WHERE sid = ?";
        String std = null;
        String stf = null;
        String pos = null;
        PreparedStatement pstmt = null;
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = this.pooledConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, sid);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                std = rs.getString("streetDut");
                stf = rs.getString("streetFre");
                pos = rs.getString("position");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                LOG.error("Error while closing resources.", e);
            }
        }
        return new String[] { std, stf, pos };
    }

    /**
     * @throws ClassNotFoundException
     */
    public void createTable() throws ClassNotFoundException {
        String sql = "CREATE TABLE IF NOT EXISTS segments" + "(sid text PRIMARY KEY," + "streetDut text NOT NULL,"
                + "streetFre text NOT NULL," + "position text NOT NULL)";
        Statement stmt = null;
        try {
            Connection conn = this.pooledConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            LOG.error("Error while creating table.", e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOG.error("Error closing statement.", e);
                }
            }
        }
    }

}
