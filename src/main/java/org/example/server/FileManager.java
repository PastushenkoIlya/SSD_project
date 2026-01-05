package org.example.server;

import org.example.common.Message;
import org.example.common.Message.Type;

import java.io.*;

// Clase que se encarga de gestionar los archivos
public class FileManager {

    private static final String DIRECTORIO = "archivosTareas/"; //nombre del directorio donde se guardan los archivos
    private static final int BUFFER_SIZE = 4096; //tamaño del buffer para leer/escribir archivos en bloques

    // Metodo que asegura que el directorio para los archivos existe
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

    // send file using Message protocol
    public static void sendFile(int id, ObjectOutputStream out) throws IOException {
        File file = encontrarFile(id);
        if (file == null) {
            Message error = new Message(Type.ERROR);
            error.setErrorMessage("File not found");
            out.writeObject(error);
            out.flush();
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                Message block = new Message(Type.DOWNLOAD_FILE);
                block.setDataBlock(copyBuffer(buffer, bytesRead));
                block.setLastBlock(false);
                out.writeObject(block);
                out.flush();
                out.reset();
            }

            Message end = new Message(Type.DOWNLOAD_FILE);
            end.setLastBlock(true);
            out.writeObject(end);
            out.flush();
        }
    }

    private static byte[] copyBuffer(byte[] buffer, int length) {
        byte[] data = new byte[length];
        System.arraycopy(buffer, 0, data, 0, length);
        return data;
    }
}
