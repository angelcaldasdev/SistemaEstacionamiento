package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import model.EspacioEstacionamiento;
import model.Ticket;
import model.TipoVehiculo;
import service.EstacionamientoService;

public class FrmEstacionamiento extends JFrame {

    private final EstacionamientoService servicio;
    private final DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private JTextField txtPlacaEntrada;
    private JComboBox<TipoVehiculo> cboTipoVehiculo;
    private JTextField txtPlacaSalida;
    private JLabel lblDisponibilidad;
    private JLabel lblTotalRecaudado;

    private JTable tablaEspacios;
    private DefaultTableModel modeloEspacios;

    private JTable tablaHistorial;
    private DefaultTableModel modeloHistorial;

    public FrmEstacionamiento(EstacionamientoService servicio) {
        this.servicio = servicio;
        inicializarComponentes();
        actualizarVista();
    }

    private void inicializarComponentes() {
        setTitle("Sistema de Gestión de Estacionamiento");
        setSize(900, 650);
        setMinimumSize(new Dimension(780, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- PANEL SUPERIOR: TÍTULO, DISPONIBILIDAD Y CAJA ---
        JPanel panelSuperior = new JPanel(new BorderLayout(15, 0));
        panelSuperior.setBackground(new Color(30, 41, 59));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblTitulo = new JLabel("CONTROL DE ESTACIONAMIENTO");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel panelMetricas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        panelMetricas.setOpaque(false);

        lblDisponibilidad = new JLabel("Espacios libres: 0 / 0", SwingConstants.RIGHT);
        lblDisponibilidad.setForeground(new Color(74, 222, 128));
        lblDisponibilidad.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblTotalRecaudado = new JLabel("Total recaudado: S/ 0.00", SwingConstants.RIGHT);
        lblTotalRecaudado.setForeground(new Color(250, 204, 21));
        lblTotalRecaudado.setFont(new Font("Segoe UI", Font.BOLD, 14));

        panelMetricas.add(lblDisponibilidad);
        panelMetricas.add(lblTotalRecaudado);

        panelSuperior.add(lblTitulo, BorderLayout.WEST);
        panelSuperior.add(panelMetricas, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // --- PANEL CENTRAL: FORMULARIOS + DIVISOR DE TABLAS ---
        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        // Panel de acciones (Entrada y Salida)
        JPanel panelAcciones = new JPanel(new GridLayout(1, 2, 12, 0));

        // 1. Subpanel Entrada
        JPanel panelEntrada = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelEntrada.setBorder(BorderFactory.createTitledBorder("Registrar Ingreso"));

        panelEntrada.add(new JLabel("Placa:"));
        txtPlacaEntrada = new JTextField(8);
        txtPlacaEntrada.addActionListener(e -> registrarIngreso()); // Enter también registra ingreso
        panelEntrada.add(txtPlacaEntrada);

        panelEntrada.add(new JLabel("Tipo:"));
        cboTipoVehiculo = new JComboBox<>(TipoVehiculo.values());
        panelEntrada.add(cboTipoVehiculo);

        JButton btnIngresar = new JButton("Ingresar");
        btnIngresar.setBackground(new Color(22, 163, 74));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.addActionListener(e -> registrarIngreso());
        panelEntrada.add(btnIngresar);

        // 2. Subpanel Salida
        JPanel panelSalida = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelSalida.setBorder(BorderFactory.createTitledBorder("Registrar Salida / Cobro"));

        panelSalida.add(new JLabel("Placa:"));
        txtPlacaSalida = new JTextField(10);
        txtPlacaSalida.addActionListener(e -> registrarSalida()); // Enter también procesa salida
        panelSalida.add(txtPlacaSalida);

        JButton btnSalida = new JButton("Cobrar y Liberar");
        btnSalida.setBackground(new Color(220, 38, 38));
        btnSalida.setForeground(Color.WHITE);
        btnSalida.setFocusPainted(false);
        btnSalida.addActionListener(e -> registrarSalida());
        panelSalida.add(btnSalida);

        panelAcciones.add(panelEntrada);
        panelAcciones.add(panelSalida);
        panelCentral.add(panelAcciones, BorderLayout.NORTH);

        // --- TABLA 1: ESPACIOS ACTUALES ---
        String[] columnasEspacios = {
            "Espacio N°", "Estado", "Ticket", "Placa", "Tipo", "Tarifa/Hora", "Hora Ingreso"
        };
        modeloEspacios = new DefaultTableModel(columnasEspacios, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaEspacios = new JTable(modeloEspacios);
        tablaEspacios.setRowHeight(24);
        tablaEspacios.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Autocompletar placa de salida al seleccionar una fila ocupada
        tablaEspacios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = tablaEspacios.getSelectedRow();
                if (fila >= 0) {
                    Object valorPlaca = modeloEspacios.getValueAt(fila, 3);
                    if (valorPlaca != null && !valorPlaca.toString().equals("-")) {
                        txtPlacaSalida.setText(valorPlaca.toString());
                    }
                }
            }
        });

        JPanel panelTablaEspacios = new JPanel(new BorderLayout(0, 4));
        panelTablaEspacios.add(new JLabel("Estado de Espacios"), BorderLayout.NORTH);
        panelTablaEspacios.add(new JScrollPane(tablaEspacios), BorderLayout.CENTER);

        // --- TABLA 2: HISTORIAL DE TICKETS COBRADOS ---
        String[] columnasHistorial = {
            "Código", "Placa", "Tipo", "Entrada", "Salida", "Horas", "Monto (S/)"
        };
        modeloHistorial = new DefaultTableModel(columnasHistorial, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaHistorial = new JTable(modeloHistorial);
        tablaHistorial.setRowHeight(24);
        tablaHistorial.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel panelTablaHistorial = new JPanel(new BorderLayout(0, 4));
        panelTablaHistorial.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        panelTablaHistorial.add(new JLabel("Historial de Tickets Cobrados"), BorderLayout.NORTH);
        panelTablaHistorial.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);

        // Divisor vertical ajustable entre ambas tablas
        JSplitPane divisorTablas = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                panelTablaEspacios,
                panelTablaHistorial
        );
        divisorTablas.setResizeWeight(0.52);
        divisorTablas.setOneTouchExpandable(true);

        panelCentral.add(divisorTablas, BorderLayout.CENTER);
        add(panelCentral, BorderLayout.CENTER);
    }

    private void registrarIngreso() {
        try {
            String placa = txtPlacaEntrada.getText();
            TipoVehiculo tipo = (TipoVehiculo) cboTipoVehiculo.getSelectedItem();

            Ticket ticket = servicio.registrarEntrada(placa, tipo);
            txtPlacaEntrada.setText("");
            txtPlacaEntrada.requestFocusInWindow();
            actualizarVista();

            JOptionPane.showMessageDialog(
                    this,
                    "Vehículo registrado exitosamente.\n"
                    + "Ticket: " + ticket.getCodigo() + "\n"
                    + "Espacio asignado: N° " + ticket.getNumeroEspacio(),
                    "Ingreso Exitoso",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Error de Ingreso",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void registrarSalida() {
        try {
            String placa = txtPlacaSalida.getText();
            Ticket ticket = servicio.registrarSalida(placa);
            txtPlacaSalida.setText("");
            actualizarVista();

            String resumen = String.format(
                    "COMPROBANTE DE SALIDA\n"
                    + "-----------------------------\n"
                    + "Ticket: %s\n"
                    + "Placa: %s (%s)\n"
                    + "Entrada: %s\n"
                    + "Salida: %s\n"
                    + "Horas cobradas: %d\n"
                    + "TOTAL A PAGAR: S/ %.2f",
                    ticket.getCodigo(),
                    ticket.getVehiculo().getPlaca(),
                    ticket.getVehiculo().getTipo().getNombre(),
                    ticket.getHoraEntrada().format(formatoHora),
                    ticket.getHoraSalida().format(formatoHora),
                    ticket.calcularHorasFacturables(),
                    ticket.getMontoTotal()
            );

            JOptionPane.showMessageDialog(
                    this,
                    resumen,
                    "Cobro Realizado",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Error de Salida",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void actualizarVista() {
        // 1. Actualizar tabla de espacios y contador de libres
        modeloEspacios.setRowCount(0);
        long libres = 0;

        for (EspacioEstacionamiento espacio : servicio.getEspacios()) {
            if (espacio.isOcupado()) {
                Ticket t = espacio.getTicketActual();
                modeloEspacios.addRow(new Object[]{
                    espacio.getNumero(),
                    "OCUPADO",
                    t.getCodigo(),
                    t.getVehiculo().getPlaca(),
                    t.getVehiculo().getTipo().getNombre(),
                    String.format("S/ %.2f", t.getVehiculo().getTipo().getTarifaPorHora()),
                    t.getHoraEntrada().format(formatoHora)
                });
            } else {
                libres++;
                modeloEspacios.addRow(new Object[]{
                    espacio.getNumero(),
                    "LIBRE",
                    "-", "-", "-", "-", "-"
                });
            }
        }

        lblDisponibilidad.setText("Espacios libres: " + libres + " / " + servicio.getEspacios().size());

        // 2. Actualizar tabla de historial de cobrados
        modeloHistorial.setRowCount(0);
        for (Ticket t : servicio.getTicketsCobrados()) {
            modeloHistorial.addRow(new Object[]{
                t.getCodigo(),
                t.getVehiculo().getPlaca(),
                t.getVehiculo().getTipo().getNombre(),
                t.getHoraEntrada().format(formatoHora),
                t.getHoraSalida() != null ? t.getHoraSalida().format(formatoHora) : "-",
                t.calcularHorasFacturables(),
                String.format("%.2f", t.getMontoTotal())
            });
        }

        // 3. Actualizar caja total recaudada
        double total = servicio.calcularTotalRecaudado();
        lblTotalRecaudado.setText(String.format("Total recaudado: S/ %.2f", total));
    }
}