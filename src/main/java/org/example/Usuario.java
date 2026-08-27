package org.example;

import java.net.InetAddress;

public class Usuario {
    private InetAddress direccion;
    private int puerto;
    private int nUsuario;

    public Usuario(InetAddress direccion, int puerto, int nUsuario) {
        this.direccion = direccion;
        this.puerto = puerto;
        this.nUsuario = nUsuario;
    }

    public Usuario(int puerto) {
        this.puerto = puerto;
    }

    public InetAddress getAddress() {
        return direccion;
    }

    public void setAddress(InetAddress direccion) {
        this.direccion = direccion;
    }

    public int getPort() {
        return puerto;
    }

    public int getNUsuario() {
        return nUsuario;
    }
}

