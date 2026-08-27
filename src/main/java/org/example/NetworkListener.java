package org.example;

public interface NetworkListener {
    void onJugadorConectado(int id);
    void onMovimientoRecibido(int idJugador, String datos);
}