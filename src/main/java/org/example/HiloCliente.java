package org.example;

import com.badlogic.gdx.Gdx;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HiloCliente extends Thread {
    private Usuario usuario = new Usuario(25565);
    private boolean conectado = false;
    private DatagramSocket socket;
    private NetworkListener listener;

    public HiloCliente(NetworkListener listener) {
        this.listener = listener;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            usuario.setAddress(InetAddress.getByName("255.255.255.255"));
            conectado = true;
            enviarMensaje("0$hola");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        while (conectado) {
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
        String msg = new String(dp.getData(), 0, dp.getLength()).trim();

        Gdx.app.postRunnable(() -> {
            if (listener == null) return;

            // 1. Recepción de confirmación del servidor ("Buenas$1")[cite: 4]
            if (msg.startsWith("Buenas$")) {
                String[] partes = msg.split("\\$");
                if (partes.length > 1) {
                    int idAsignado = Integer.parseInt(partes[1]);
                    listener.onJugadorConectado(idAsignado);
                }
            }
            // 2. Recepción de datos de oponentes ("POS:ID:x:y:angulo:sprite:lap")[cite: 4]
            else if (msg.startsWith("POS:")) {
                String[] partes = msg.substring(4).split(":");
                if (partes.length >= 5) {
                    int idJugador = Integer.parseInt(partes[0]);

                    // Reconstruir la cadena con x:y:angulo:sprite:lap
                    StringBuilder datosVehiculo = new StringBuilder();
                    for (int i = 1; i < partes.length; i++) {
                        datosVehiculo.append(partes[i]);
                        if (i < partes.length - 1) datosVehiculo.append(":");
                    }

                    listener.onMovimientoRecibido(idJugador, datosVehiculo.toString());
                }
            }
        });
    }

    public void enviarMensaje(String data) {
        byte[] msg = data.getBytes();
        DatagramPacket dp = new DatagramPacket(msg, msg.length, usuario.getAddress(), usuario.getPort());
        try {
            socket.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}