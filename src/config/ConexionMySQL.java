package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionMySQL {

    private static final String URL =
            "jdbc:mysql://localhost:3306/estacionamiento_db?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true";
    private static final String USER = "estacionamiento_app";
    private static final String PASSWORD = "Estacionamiento2026!"; // <-- Cambia esto por tu clave real

    private ConexionMySQL() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}