package org.example;

public class Servidor {
    public static void main(String[] args) {
        System.out.println("Iniciando Servidor");
        HiloServidor servidor = new HiloServidor();
        servidor.start();
        System.out.println("Servidor corriendo en el puerto 25565");
    }
}
