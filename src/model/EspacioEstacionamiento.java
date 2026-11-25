package model;

public class EspacioEstacionamiento {
    private int numero;
    private boolean ocupado;
    private Ticket ticketActual;

    public EspacioEstacionamiento(int numero) {
        this.numero = numero;
        this.ocupado = false;
        this.ticketActual = null;
    }

    public int getNumero() {
        return numero;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public Ticket getTicketActual() {
        return ticketActual;
    }

    public void asignarTicket(Ticket ticket) {
        this.ticketActual = ticket;
        this.ocupado = true;
    }

    public void liberarEspacio() {
        this.ticketActual = null;
        this.ocupado = false;
    }
}