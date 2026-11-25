package service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.EspacioEstacionamiento;
import model.Ticket;
import model.TipoVehiculo;
import model.Vehiculo;

public class EstacionamientoService {
    private final List<EspacioEstacionamiento> espacios = new ArrayList<>();
    private final List<Ticket> historialTickets = new ArrayList<>();
    private int contadorTickets = 1;

    public EstacionamientoService(int capacidadTotal) {
        for (int i = 1; i <= capacidadTotal; i++) {
            espacios.add(new EspacioEstacionamiento(i));
        }
    }

    public Optional<EspacioEstacionamiento> buscarEspacioLibre() {
        return espacios.stream().filter(e -> !e.isOcupado()).findFirst();
    }

    public Ticket registrarEntrada(String placa, TipoVehiculo tipo) {
        if (placa == null || placa.trim().isEmpty()) {
            throw new IllegalArgumentException("La placa no puede estar vacía.");
        }
        if (buscarTicketActivoPorPlaca(placa).isPresent()) {
            throw new IllegalStateException("Ya existe un vehículo activo con esa placa.");
        }

        EspacioEstacionamiento espacioLibre = buscarEspacioLibre()
                .orElseThrow(() -> new IllegalStateException("No hay espacios disponibles."));

        String codigo = String.format("TCK-%04d", contadorTickets++);
        Vehiculo vehiculo = new Vehiculo(placa, tipo);
        Ticket ticket = new Ticket(codigo, espacioLibre.getNumero(), vehiculo, LocalDateTime.now());

        espacioLibre.asignarTicket(ticket);
        historialTickets.add(ticket);
        return ticket;
    }

    public Ticket registrarSalida(String placa) {
        Ticket ticket = buscarTicketActivoPorPlaca(placa)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un ticket activo para la placa ingresada."));

        LocalDateTime salida = LocalDateTime.now();
        ticket.setHoraSalida(salida);

        long horas = ticket.calcularHorasFacturables();
        double tarifaHora = ticket.getVehiculo().getTipo().getTarifaPorHora();
        ticket.setMontoTotal(horas * tarifaHora);

        espacios.stream()
                .filter(e -> e.getNumero() == ticket.getNumeroEspacio())
                .findFirst()
                .ifPresent(EspacioEstacionamiento::liberarEspacio);

        return ticket;
    }

    public Optional<Ticket> buscarTicketActivoPorPlaca(String placa) {
        String placaNormalizada = placa.trim().toUpperCase();
        return espacios.stream()
                .filter(EspacioEstacionamiento::isOcupado)
                .map(EspacioEstacionamiento::getTicketActual)
                .filter(t -> t != null && t.getVehiculo().getPlaca().equals(placaNormalizada))
                .findFirst();
    }

    public List<EspacioEstacionamiento> getEspacios() {
        return espacios;
    }

    public List<Ticket> getHistorialTickets() {
        return historialTickets;
    }
}