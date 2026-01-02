package org.example.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileManager {

    private static final String DIRECTORIO = "archivosTareas";
    private static final int BUFFER_SIZE = 4096;

    static {
        File dir = new File(DIRECTORIO);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    // Save file blocks sent by the client
    public static void saveFile(int id, String extension, byte[] buffer, int bytesRead)
            throws IOException {

        File file = new File(DIRECTORIO + "/" + id + extension);

        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(buffer, 0, bytesRead);
        }
    }

    // Send file in blocks
    public static void sendFile(int id, OutputStream out) throws IOException {

        File file = findFile(id);
        if (file == null) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    // Find file by task ID
    private static File findFile(int id) {
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
}

