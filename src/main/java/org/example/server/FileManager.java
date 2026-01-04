package org.example.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// Clase que se encarga de gestionar los archivos
public class FileManager {

    private static final String DIRECTORIO = "archivosTareas"; //nombre del directorio donde se guardan los archivos
    private static final int BUFFER_SIZE = 4096; //tamaño del buffer para leer/escribir archivos en bloques


    // Método que asegura que el directorio para los archivos existe
    private static void existeDirectorio() {
        File dir = new File(DIRECTORIO);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    // Guarda un archivo en bloques
    public static void guardarFile(int id, String extension, byte[] buffer, int bytesRead) throws IOException {
        // Asegurarse de que el directorio existe
        existeDirectorio();

        // Crear el archivo con el ID de la tarea y la extension
        File file = new File(DIRECTORIO + id + extension);

        // Escribimos por bloques
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(buffer, 0, bytesRead);
        }
    }

    // Buscar un archivo por su id de tarea
    private static File encontrarFile(int id) {
        File dir = new File(DIRECTORIO);
        // Lista todos los archivos en el directorio
        File[] files = dir.listFiles();

        if (files == null) return null;
        // Recorre los archivos buscando el que empieza con el id de la tarea
        for (File f : files) {
            if (f.getName().startsWith(String.valueOf(id))) {
                return f;
            }
        }
        return null;
    }
    
    // Enviar un archivo en bloques al cliente
    public static void sendFile(int id, OutputStream out) throws IOException {
        //Busca el archivo por su id de la tarea
        File file = encontrarFile(id);
        if (file == null) return;

        // Abre un flujo de entrada para leer el archivo 
        try (FileInputStream fis = new FileInputStream(file)) {
            // Buffer para leer el archivo en bloques
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            //Lee el archivo y lo envia en bloques
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Envia cada bloque al OutputStream del cliente
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

