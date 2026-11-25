package model;

public class Vehiculo {
    private String placa;
    private TipoVehiculo tipo;

    public Vehiculo(String placa, TipoVehiculo tipo) {
        this.placa = placa.trim().toUpperCase();
        this.tipo = tipo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa.trim().toUpperCase();
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }
}