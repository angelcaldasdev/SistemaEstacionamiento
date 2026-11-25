package service;

import config.ConexionMySQL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
        cargarDesdeBaseDeDatos();
    }

    private void cargarDesdeBaseDeDatos() {
        String sql = "SELECT codigo, numero_espacio, placa, tipo_vehiculo, hora_entrada, hora_salida, monto_total, estado "
                   + "FROM tickets ORDER BY id ASC";

        try (Connection conn = ConexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int maxNumeroTicket = 0;

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                int numEspacio = rs.getInt("numero_espacio");
                String placa = rs.getString("placa");
                TipoVehiculo tipo = TipoVehiculo.valueOf(rs.getString("tipo_vehiculo").toUpperCase());

                Timestamp tsEntrada = rs.getTimestamp("hora_entrada");
                Timestamp tsSalida = rs.getTimestamp("hora_salida");

                LocalDateTime entrada = tsEntrada != null ? tsEntrada.toLocalDateTime() : LocalDateTime.now();
                LocalDateTime salida = tsSalida != null ? tsSalida.toLocalDateTime() : null;
                double monto = rs.getDouble("monto_total");
                String estado = rs.getString("estado");

                Vehiculo vehiculo = new Vehiculo(placa, tipo);
                Ticket ticket = new Ticket(codigo, numEspacio, vehiculo, entrada, salida, monto);
                historialTickets.add(ticket);

                if ("ACTIVO".equalsIgnoreCase(estado)) {
                    for (EspacioEstacionamiento esp : espacios) {
                        if (esp.getNumero() == numEspacio) {
                            esp.asignarTicket(ticket);
                            break;
                        }
                    }
                }

                // Actualizar correlativo TCK-0001
                if (codigo != null && codigo.startsWith("TCK-")) {
                    try {
                        int n = Integer.parseInt(codigo.substring(4));
                        if (n > maxNumeroTicket) {
                            maxNumeroTicket = n;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            this.contadorTickets = maxNumeroTicket + 1;

        } catch (SQLException ex) {
            throw new RuntimeException("Error al cargar datos desde MySQL: " + ex.getMessage(), ex);
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
        LocalDateTime ahora = LocalDateTime.now();
        Ticket ticket = new Ticket(codigo, espacioLibre.getNumero(), vehiculo, ahora);

        String sqlInsert = "INSERT INTO tickets (codigo, numero_espacio, placa, tipo_vehiculo, tarifa_hora, hora_entrada, estado) "
                         + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')";

        try (Connection conn = ConexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsert)) {

            ps.setString(1, ticket.getCodigo());
            ps.setInt(2, ticket.getNumeroEspacio());
            ps.setString(3, vehiculo.getPlaca());
            ps.setString(4, tipo.name());
            ps.setDouble(5, tipo.getTarifaPorHora());
            ps.setTimestamp(6, Timestamp.valueOf(ahora));
            ps.executeUpdate();

        } catch (SQLException ex) {
            contadorTickets--;
            throw new RuntimeException("Error al guardar el ingreso en MySQL: " + ex.getMessage(), ex);
        }

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
        double montoTotal = horas * tarifaHora;
        ticket.setMontoTotal(montoTotal);

        String sqlUpdate = "UPDATE tickets SET hora_salida = ?, monto_total = ?, estado = 'COBRADO' "
                         + "WHERE codigo = ? AND estado = 'ACTIVO'";

        try (Connection conn = ConexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {

            ps.setTimestamp(1, Timestamp.valueOf(salida));
            ps.setDouble(2, montoTotal);
            ps.setString(3, ticket.getCodigo());
            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new RuntimeException("Error al actualizar el cobro en MySQL: " + ex.getMessage(), ex);
        }

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

    public List<Ticket> getTicketsCobrados() {
        return historialTickets.stream()
                .filter(t -> t.getHoraSalida() != null)
                .toList();
    }

    public double calcularTotalRecaudado() {
        return getTicketsCobrados().stream()
                .mapToDouble(Ticket::getMontoTotal)
                .sum();
    }

    public List<EspacioEstacionamiento> getEspacios() {
        return espacios;
    }

    public List<Ticket> getHistorialTickets() {
        return historialTickets;
    }
}