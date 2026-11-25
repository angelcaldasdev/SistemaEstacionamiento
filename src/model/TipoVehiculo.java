package model;

public enum TipoVehiculo {
    AUTO("Auto", 4.0),
    MOTO("Moto", 2.0),
    CAMIONETA("Camioneta", 6.0);

    private final String nombre;
    private final double tarifaPorHora;

    TipoVehiculo(String nombre, double tarifaPorHora) {
        this.nombre = nombre;
        this.tarifaPorHora = tarifaPorHora;
    }

    public String getNombre() {
        return nombre;
    }

    public double getTarifaPorHora() {
        return tarifaPorHora;
    }
}