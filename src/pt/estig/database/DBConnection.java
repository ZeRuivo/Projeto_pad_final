package pt.estig.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private String dbName;
    private Connection connection;

    public DBConnection(String dbName) {
        this.dbName = dbName;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public boolean connect() {
        // Altera para a tua senha do MySQL se necessário
        String user = "root";
        String password = "";
        String url = "jdbc:mysql://localhost:3306/" + this.dbName;

        try {
            // Carrega o Driver (O tal que causou o erro ClassNotFoundException)
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Erro de conexão: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
