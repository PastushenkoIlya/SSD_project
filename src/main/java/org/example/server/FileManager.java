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

    // Envía al cliente el archivo asociado a una tarea utilizando el protocolo basado en Message
    public static void sendFile(int id, ObjectOutputStream out) throws IOException {
        // Se busca el archivo correspondiente al ID de la tarea
        File file = encontrarFile(id);

        // Si no existe el archivo, se envía un mensaje de error al cliente
        if (file == null) {
            Message error = new Message(Type.ERROR);
            error.setErrorMessage("File not found");
            out.writeObject(error);
            out.flush();
            return;
        }

        // Se abre un flujo de entrada para leer el archivo desde disco
        try (FileInputStream fis = new FileInputStream(file)) {
            // Buffer utilizado para leer el archivo por bloques
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // Lectura del archivo hasta que no queden más datos
            while ((bytesRead = fis.read(buffer)) != -1) {

                // Se crea un mensaje de tipo DOWNLOAD_FILE para cada bloque leído
                Message block = new Message(Type.DOWNLOAD_FILE);

                // Se copia únicamente la parte válida del buffer
                block.setDataBlock(copyBuffer(buffer, bytesRead));

                // Se indica que este no es el último bloque
                block.setLastBlock(false);

                // Se envía el bloque al cliente
                out.writeObject(block);
                out.flush();

                // IMPORTANTE:
                // reset() evita que ObjectOutputStream reutilice objetos ya enviados
                // y fuerza la serialización completa de cada bloque
                out.reset();
            }

            // Cuando se han enviado todos los bloques, se envía un mensaje final
            Message end = new Message(Type.DOWNLOAD_FILE);

            // Este mensaje indica explícitamente el final de la transferencia
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
