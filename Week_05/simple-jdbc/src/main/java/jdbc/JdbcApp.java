package jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

public class JdbcApp {
    //数据库连接URL
    private static final String JDBC_URL = "jdbc:h2:~/test";
    //连接数据库时使用的用户名
    private static final String USER = "sa";
    //连接数据库时使用的密码
    private static final String PASSWORD = "sa";
    //连接H2数据库时使用的驱动类，org.h2.Driver这个类是由H2数据库自己提供的，在H2数据库的jar包中可以找到
    private static final String DRIVER_CLASS="org.h2.Driver";

    public static void main(String[] args) throws Exception {
        doWithoutConnectionPool();
        doWithConnectionPool();
    }

    private static void doWithoutConnectionPool() throws Exception {
        Class.forName(DRIVER_CLASS);
        Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);

        System.out.println("--------------WithoutConnectionPool---------------");

        worker(connection);
        //关闭连接
        connection.close();
    }

    private static void doWithConnectionPool() throws Exception{
        System.out.println("--------------WithConnectionPool---------------");

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DRIVER_CLASS);
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // HikariDataSource 也可以配置
        DataSource ds = new HikariDataSource(config);
        ((HikariDataSource) ds).setPassword("root");

        Connection connection = ds.getConnection();
        worker(connection);

        ((HikariDataSource) ds).close();
    }

    private static void worker(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("DROP TABLE IF EXISTS USER_INFO");
        //创建USER_INFO表
        stmt.execute("CREATE TABLE USER_INFO(id VARCHAR(36) PRIMARY KEY,name VARCHAR(100),sex VARCHAR(4))");

        //新增
        stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三1','男')");
        stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三2','男')");
        stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三3','男')");
        stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三4','女')");
        stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三5','男')");
        stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三6','男')");
        //删除
        stmt.executeUpdate("DELETE FROM USER_INFO WHERE name='张三6'");
        //修改
        stmt.executeUpdate("UPDATE USER_INFO SET name='张三55' WHERE name='张三5'");
        //查询
        ResultSet rs = stmt.executeQuery("SELECT * FROM USER_INFO");
        //遍历结果集
        while (rs.next()) {
            System.out.println(rs.getString("id") + "," + rs.getString("name")+ "," + rs.getString("sex"));
        }

        //使用PreperedStatement
        PreparedStatement st = null;
        String sql = "SELECT * FROM user_info WHERE name = ? AND sex = ?";

        st = connection.prepareStatement(sql);
        st.setString(1, "张三3");
        st.setString(2, "男");

        System.out.println("---------PreparedStatement---------");

        rs = st.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getString("id") + "," + rs.getString("name")+ "," + rs.getString("sex"));
        }

        //事物以及批量提交
        connection.setAutoCommit(false);
        stmt.executeUpdate("DELETE FROM USER_INFO");
        for(int i = 7; i < 7 + 7; i++){
            stmt.addBatch("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','张三"
                    + Integer.toString(i) + "','男')");
        }
        stmt.executeBatch();
        connection.commit();

        //查询
        rs = stmt.executeQuery("SELECT * FROM USER_INFO");
        //遍历结果集
        while (rs.next()) {
            System.out.println(rs.getString("id") + "," + rs.getString("name")+ "," + rs.getString("sex"));
        }

        //释放资源
        stmt.close();
        st.close();
    }
}
