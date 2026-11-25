package model;

import java.time.Duration;
import java.time.LocalDateTime;

public class Ticket {
    private String codigo;
    private int numeroEspacio;
    private Vehiculo vehiculo;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSalida;
    private double montoTotal;

    // Constructor para un ticket nuevo al ingresar un vehículo
    public Ticket(String codigo, int numeroEspacio, Vehiculo vehiculo, LocalDateTime horaEntrada) {
        this.codigo = codigo;
        this.numeroEspacio = numeroEspacio;
        this.vehiculo = vehiculo;
        this.horaEntrada = horaEntrada;
    }

    // Constructor para reconstruir un ticket desde MySQL (activo o cobrado)
    public Ticket(String codigo, int numeroEspacio, Vehiculo vehiculo,
                  LocalDateTime horaEntrada, LocalDateTime horaSalida, double montoTotal) {
        this.codigo = codigo;
        this.numeroEspacio = numeroEspacio;
        this.vehiculo = vehiculo;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.montoTotal = montoTotal;
    }

    public long calcularHorasFacturables() {
        LocalDateTime fin = (horaSalida != null) ? horaSalida : LocalDateTime.now();
        long minutos = Duration.between(horaEntrada, fin).toMinutes();
        if (minutos <= 0) {
            return 1; // Mínimo 1 hora cobrable
        }
        return (minutos + 59) / 60; // Redondeo hacia arriba por hora o fracción
    }

    public String getCodigo() {
        return codigo;
    }

    public int getNumeroEspacio() {
        return numeroEspacio;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public LocalDateTime getHoraEntrada() {
        return horaEntrada;
    }

    public LocalDateTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalDateTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }
}