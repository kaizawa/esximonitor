package com.cafeform.esxi.esximonitor;

public enum CommandType 
{
    POWER_OFF("power off"),
    POWER_ON("power on"),
    RESET("reset"),
    SHUTDOWN("shutdown");
    
    private final String text;
    
    private CommandType(String text)
    {
        this.text = text;
    }
    
    @Override
    public String toString ()
    {
        return text;
    }
}
