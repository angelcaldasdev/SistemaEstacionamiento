package service;

import config.ConexionMySQL;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import model.EspacioEstacionamiento;
import model.Ticket;
import model.TipoVehiculo;
import model.Vehiculo;

public class EstacionamientoService {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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

    public String generarResumenCaja() {
        int totalActivos = 0;
        int totalCobrados = 0;
        double recaudadoGeneral = 0.0;

        int cobradosAuto = 0;
        int cobradosMoto = 0;
        int cobradosCamioneta = 0;

        double montoAuto = 0.0;
        double montoMoto = 0.0;
        double montoCamioneta = 0.0;

        for (EspacioEstacionamiento esp : espacios) {
            if (esp.isOcupado()) {
                totalActivos++;
            }
        }

        for (Ticket t : getTicketsCobrados()) {
            totalCobrados++;
            double monto = t.getMontoTotal();
            recaudadoGeneral += monto;

            TipoVehiculo tipo = t.getVehiculo().getTipo();
            switch (tipo) {
                case AUTO -> {
                    cobradosAuto++;
                    montoAuto += monto;
                }
                case MOTO -> {
                    cobradosMoto++;
                    montoMoto += monto;
                }
                case CAMIONETA -> {
                    cobradosCamioneta++;
                    montoCamioneta += monto;
                }
            }
        }

        return String.format(
                "RESUMEN DE CAJA - ESTACIONAMIENTO%n"
                + "----------------------------------------%n"
                + "Espacios ocupados actualmente: %d / %d%n"
                + "Espacios libres actualmente:   %d / %d%n"
                + "Tickets cobrados en historial: %d%n%n"
                + "Desglose por tipo de vehículo (cobrado):%n"
                + "• Auto:      %d ticket(s)  ->  S/ %.2f%n"
                + "• Moto:      %d ticket(s)  ->  S/ %.2f%n"
                + "• Camioneta: %d ticket(s)  ->  S/ %.2f%n"
                + "----------------------------------------%n"
                + "TOTAL RECAUDADO: S/ %.2f",
                totalActivos, espacios.size(),
                espacios.size() - totalActivos, espacios.size(),
                totalCobrados,
                cobradosAuto, montoAuto,
                cobradosMoto, montoMoto,
                cobradosCamioneta, montoCamioneta,
                recaudadoGeneral
        );
    }

    public Path guardarComprobanteTxt(Ticket ticket) throws IOException {
        Path carpeta = Paths.get("comprobantes");
        Files.createDirectories(carpeta);

        String salidaFormateada = ticket.getHoraSalida() != null
                ? ticket.getHoraSalida().format(FORMATO_FECHA_HORA)
                : "-";

        String contenido = String.format(
                "========================================%n"
                + "       COMPROBANTE DE ESTACIONAMIENTO%n"
                + "========================================%n"
                + "Ticket:         %s%n"
                + "Espacio N°:     %d%n"
                + "Placa:          %s%n"
                + "Tipo Vehículo:  %s%n"
                + "Tarifa/Hora:    S/ %.2f%n"
                + "Hora Entrada:   %s%n"
                + "Hora Salida:    %s%n"
                + "Horas Cobradas: %d%n"
                + "----------------------------------------%n"
                + "TOTAL PAGADO:   S/ %.2f%n"
                + "========================================%n",
                ticket.getCodigo(),
                ticket.getNumeroEspacio(),
                ticket.getVehiculo().getPlaca(),
                ticket.getVehiculo().getTipo().getNombre(),
                ticket.getVehiculo().getTipo().getTarifaPorHora(),
                ticket.getHoraEntrada().format(FORMATO_FECHA_HORA),
                salidaFormateada,
                ticket.calcularHorasFacturables(),
                ticket.getMontoTotal()
        );

        Path archivo = carpeta.resolve(ticket.getCodigo() + ".txt");
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        return archivo;
    }

    public Path exportarHistorialCsv(Path archivoDestino) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Codigo,Espacio,Placa,Tipo,Hora Entrada,Hora Salida,Horas,Monto (S/)\n");

        for (Ticket t : getTicketsCobrados()) {
            String salidaStr = t.getHoraSalida() != null ? t.getHoraSalida().format(FORMATO_FECHA_HORA) : "";
            sb.append(escaparCsv(t.getCodigo())).append(",")
              .append(t.getNumeroEspacio()).append(",")
              .append(escaparCsv(t.getVehiculo().getPlaca())).append(",")
              .append(escaparCsv(t.getVehiculo().getTipo().getNombre())).append(",")
              .append(escaparCsv(t.getHoraEntrada().format(FORMATO_FECHA_HORA))).append(",")
              .append(escaparCsv(salidaStr)).append(",")
              .append(t.calcularHorasFacturables()).append(",")
              .append(String.format(Locale.US, "%.2f", t.getMontoTotal())).append("\n");
        }

        sb.append("\n,,,,Total Recaudado (S/),,,")
          .append(String.format(Locale.US, "%.2f", calcularTotalRecaudado())).append("\n");

        Files.writeString(archivoDestino, sb.toString(), StandardCharsets.UTF_8);
        return archivoDestino;
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}