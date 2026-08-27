package org.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

public class HiloServidor extends Thread {
    private DatagramSocket socket;
    private ArrayList<Usuario> usuarios = new ArrayList<>();
    private static final int MAX_JUGADORES = 4;

    public HiloServidor() {
        try {
            socket = new DatagramSocket(25565);
            System.out.println("Servidor UDP iniciado en el puerto 25565...");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                socket.receive(dp);
                procesarDatagrama(dp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void procesarDatagrama(DatagramPacket dp) {
        String contenido = new String(dp.getData(), 0, dp.getLength()).trim();
        String[] mensajeComplejo = contenido.split("\\$");

        // Handshake: si el cliente envía "0$hola"
        if (mensajeComplejo.length > 1 && mensajeComplejo[1].equals("hola")) {
            if (usuarios.size() < MAX_JUGADORES) {
                Usuario nuevoUsuario = new Usuario(dp.getAddress(), dp.getPort(), usuarios.size() + 1);
                usuarios.add(nuevoUsuario);

                // Confirmamos la conexión enviando "Buenas" y su ID asignado
                enviarMensaje("Buenas$" + nuevoUsuario.getNUsuario(), nuevoUsuario);
                System.out.println("Jugador conectado ID: " + nuevoUsuario.getNUsuario() + " (" + usuarios.size() + "/" + MAX_JUGADORES + ")");
            } else {
                // Sala llena: notificar rechazo
                Usuario usuarioRechazado = new Usuario(dp.getAddress(), dp.getPort(), -1);
                enviarMensaje("RECHAZADO$SalaLlena", usuarioRechazado);
                System.out.println("Conexión rechazada de " + dp.getAddress() + ":" + dp.getPort() + " - Sala Llena");
            }
            return;
        }

        // Reenviar mensajes de posición o acciones a los demás jugadores
        // Admite formatos como "1$POS:100:200:0:BlackOut.png" o "POS:100:200:0:BlackOut.png"
        if (mensajeComplejo.length >= 1) {
            for (Usuario u : usuarios) {
                // Reenviar a todos los usuarios excepto al emisor
                if (!u.getAddress().equals(dp.getAddress()) || u.getPort() != dp.getPort()) {
                    enviarMensaje(contenido, u);
                }
            }
        }
    }

    public void enviarMensaje(String data, Usuario usuario) {
        byte[] msg = data.getBytes();
        DatagramPacket dp = new DatagramPacket(msg, msg.length, usuario.getAddress(), usuario.getPort());
        try {
            socket.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}