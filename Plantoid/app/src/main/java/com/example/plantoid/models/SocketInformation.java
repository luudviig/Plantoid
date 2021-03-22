package com.example.plantoid.models;

import java.io.Serializable;

public class SocketInformation implements Serializable {
    private String ip;
    private int port;

    public SocketInformation(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "SocketInformation{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
