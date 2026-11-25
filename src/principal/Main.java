package principal;

import model.TipoVehiculo;
import service.EstacionamientoService;

public class Main {
    public static void main(String[] args) {
        EstacionamientoService servicio = new EstacionamientoService(20);
        var ticket = servicio.registrarEntrada("ABC-123", TipoVehiculo.AUTO);
        System.out.println("Ticket generado: " + ticket.getCodigo() + " | Espacio: " + ticket.getNumeroEspacio());
    }
}