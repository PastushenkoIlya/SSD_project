package org.example.server;

import org.example.common.Message;
import org.example.common.Message.Type;

import java.io.*;

public class FileManager {

    private static final String DIRECTORIO = "archivosTareas/";
    private static final int BUFFER_SIZE = 4096;

    // Ensure directory exists
    private static void existeDirectorio() {
        File dir = new File(DIRECTORIO);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    // Save file blocks
    public static void guardarFile(int id, String extension, byte[] buffer, int bytesRead) throws IOException {
        existeDirectorio();
        File file = new File(DIRECTORIO + id + extension);

        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(buffer, 0, bytesRead);
        }
    }

    // INTERNAL: find file by task id
    private static File encontrarFile(int id) {
        File dir = new File(DIRECTORIO);
        File[] files = dir.listFiles();
        if (files == null) return null;

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
