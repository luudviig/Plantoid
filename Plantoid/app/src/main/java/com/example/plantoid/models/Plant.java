package com.example.plantoid.models;

public class Plant {

    private final int id;
    private String alias;
    private String lastWatered;
    private String lastPolled;
    private double soilHumidity;
    private double airHumidity;
    private double airTemp;
    private double lightExposure;
    private boolean waterTankEmpty;

    public Plant(int id, String alias, String lastWatered, String lastPolled, double soilHumidity, double airHumidity, double airTemp, double lightExposure, boolean waterTankEmpty) {
        this.id = id;
        this.alias = alias;
        this.lastWatered = lastWatered;
        this.lastPolled = lastPolled;
        this.soilHumidity = soilHumidity;
        this.airHumidity = airHumidity;
        this.airTemp = airTemp;
        this.lightExposure = lightExposure;
        this.waterTankEmpty = waterTankEmpty;
    }

    public int getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getLastWatered() {
        return lastWatered;
    }

    public void setLastWatered(String lastWatered) {
        this.lastWatered = lastWatered;
    }

    public String getLastPolled() {
        return lastPolled;
    }

    public void setLastPolled(String lastPolled) {
        this.lastPolled = lastPolled;
    }

    public double getSoilHumidity() {
        return soilHumidity;
    }

    public void setSoilHumidity(double soilHumidity) {
        this.soilHumidity = soilHumidity;
    }

    public double getAirHumidity() {
        return airHumidity;
    }

    public void setAirHumidity(double airHumidity) {
        this.airHumidity = airHumidity;
    }

    public double getAirTemp() {
        return airTemp;
    }

    public void setAirTemp(double airTemp) {
        this.airTemp = airTemp;
    }

    public double getLightExposure() {
        return lightExposure;
    }

    public void setLightExposure(double lightExposure) {
        this.lightExposure = lightExposure;
    }

    public boolean isWaterTankEmpty() {
        return waterTankEmpty;
    }

    public void setWaterTankEmpty(boolean waterTankEmpty) {
        this.waterTankEmpty = waterTankEmpty;
    }
}
