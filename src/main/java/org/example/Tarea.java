package org.example;

import java.io.Serializable;
import java.util.Date; //Necesario para usar la clase Date que pide en las especificaciones

public class Tarea implements Serializable {

    private int id;                     // Identificador unico (asignado por el servidor)
    private String descripcion;          // Breve descripcion de la tarea
    private Date fechaVencimiento;       // Fecha y hora límite
    private int prioridad;               // 1=Alta, 2=Media, 3=Baja
    private boolean completada;          // Estado de la tarea
    private boolean tieneFichero;        // ¿Tiene archivo adjunto?

    // Constructor de la clase
    public Tarea(int id, String descripcion, Date fechaVencimiento, int prioridad) {
        this.id = id;
        this.descripcion = descripcion;
        this.fechaVencimiento = fechaVencimiento;
        this.prioridad = prioridad;
        this.completada = false; 
        this.tieneFichero = false;
    } // Los atributos booleanos no se incluyen en el constructor y se inicializan a false por defecto

    // Getters
    public int getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Date getFechaVencimiento() {
        return fechaVencimiento;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public boolean isCompletada() {
        return completada;
    }

    public boolean isTieneFichero() {
        return tieneFichero;
    }

    // Setters
    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public void setTieneFichero(boolean tieneFichero) {
        this.tieneFichero = tieneFichero;
    }

    //toString para mostrar la informacion de la tarea
    @Override
    public String toString() {
        return "ID: " + id + ", Descripcion: " + descripcion + ", Prioridad: " + prioridad +
               ", Vence: " + fechaVencimiento + ", Completada: " + completada + ", Fichero: " + tieneFichero;
    }
}
