package db;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConnectionPool {
    private static int INITIAL_POOL_SIZE = 3;
    private List<Connection> connectionPool;
    private List<Connection> usedConnections = new ArrayList<>();

    private ConnectionPool(List<Connection> pool) {
        connectionPool = pool;
    }

    public static ConnectionPool create() throws SQLException {

        List<Connection> pool = new ArrayList<>(INITIAL_POOL_SIZE);
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            pool.add(createConnection());
        }
        return new ConnectionPool(pool);
    }

    private static MysqlDataSource getMySQLDataSource() {
        Properties props = new Properties();
        FileInputStream fis;
        MysqlDataSource mysqlDS = null;
        try {
            fis = new FileInputStream("db.properties");
            props.load(fis);
            mysqlDS = new MysqlDataSource();
            mysqlDS.setURL(props.getProperty("MYSQL_DB_URL"));
            mysqlDS.setUser(props.getProperty("MYSQL_DB_USERNAME"));
            mysqlDS.setPassword(props.getProperty("MYSQL_DB_PASSWORD"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mysqlDS;
    }

    private static Connection createConnection() throws SQLException {
        MysqlDataSource dataSource = getMySQLDataSource();
        try {
            Connection con = dataSource.getConnection();
            System.out.print("Successfully connected: ");
            System.out.println(con.getMetaData().getDatabaseProductName());
            return con;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't get connection. Incorrect URL");
        }
    }

    public Connection getConnection() {
        Connection connection = connectionPool
                .remove(connectionPool.size() - 1);
        usedConnections.add(connection);
        return connection;
    }

    public boolean releaseConnection(Connection connection) {
        connectionPool.add(connection);
        return usedConnections.remove(connection);
    }

    public int getSize() {
        return connectionPool.size() + usedConnections.size();
    }
}
