package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionMySQL {

    private static final String ARCHIVO_CONFIG = "config.properties";
    private static final Properties PROPIEDADES = cargarPropiedades();

    private ConexionMySQL() {
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(ARCHIVO_CONFIG)) {
            props.load(input);
        } catch (IOException e) {
            System.err.println("Aviso: no se encontró '" + ARCHIVO_CONFIG
                    + "'. Usando valores por defecto/entorno.");
        }
        return props;
    }

    public static Connection getConnection() throws SQLException {
        String url = PROPIEDADES.getProperty("db.url",
                "jdbc:mysql://127.0.0.1:3306/estacionamiento_db?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true");
        String user = PROPIEDADES.getProperty("db.user", "estacionamiento_app");
        String password = PROPIEDADES.getProperty("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }
}