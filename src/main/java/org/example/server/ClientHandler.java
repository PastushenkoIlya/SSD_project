package org.example.server;

import org.example.common.Message;
import org.example.common.Message.Type;
import org.example.common.Task;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ClientHandler
 * One instance per client. Executed inside a thread from ExecutorService.
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            boolean connected = true;

            while (connected) {
                Message request = (Message) in.readObject();

                switch (request.getType()) {

                    case LIST -> handleList();
                    case FILTER -> handleFilter(request);
                    case CREATE -> handleCreate(request);
                    case DELETE -> handleDelete(request);
                    case UPLOAD_FILE -> handleUploadFile(request);
                    case DOWNLOAD_FILE -> handleDownloadFile(request);
                    case CHANGE_STATE -> handleChangeState(request);
                    case CLOSE -> connected = false;

                    default -> sendError("Tipo de petición desconocida");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Cliente desconectado");
        } finally {
            close();
        }
    }



    /* =========================
       Task operations
       ========================= */

    private void handleList() throws IOException {
        Map<Integer, Task> allTasks = Server.getAllTasks();
        List<Task> list = new ArrayList<>(allTasks.values());

        Message response = new Message(Type.RESPONSE);
        response.setResult(list);

        out.reset(); // <-- force re-serialization or in other words: avoid cached objects
        out.writeObject(response);
        out.flush();
    }

    private void handleFilter(Message request) throws IOException {
        Map<Integer, Task> allTasks = Server.getAllTasks();
        List<Task> result = new ArrayList<>();

        for (Task task : allTasks.values()) {

            if (request.isCompleted() != null &&
                    task.isCompletada() != request.isCompleted()) {
                continue;
            }

            if (request.getPriority() != null &&
                    task.getPrioridad() != request.getPriority()) {
                continue;
            }

            if (request.getMaxDueDate() != null &&
                    task.getFechaVencimiento().after(request.getMaxDueDate())) {
                continue;
            }

            result.add(task);
        }

        Message response = new Message(Type.RESPONSE);
        response.setResult(result);

        out.writeObject(response);
        out.flush();
    }

    private void handleCreate(Message request) throws IOException {
        Task task = request.getTask();

        int id = Server.generarTaskId();
        Task newTask = new Task(
                id,
                task.getDescripcion(),
                task.getFechaVencimiento(),
                task.getPrioridad()
        );

        Server.añadirTask(newTask);

        Message response = new Message(Type.RESPONSE);
        response.setResult(true);

        out.writeObject(response);
        out.flush();
    }

    private void handleDelete(Message request) throws IOException {
        int id = request.getTaskId();
        Server.quitarTask(id);

        Message response = new Message(Type.RESPONSE);
        response.setResult(true);

        out.writeObject(response);
        out.flush();
    }

    /* =========================
       File operations
       ========================= */

    private void handleUploadFile(Message request) throws IOException, ClassNotFoundException {
        int taskId = request.getTaskId();
        String fileName = request.getFileName();
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        Task task = Server.getTask(taskId);
        if (task == null) {
            sendError("Tarea no encontrada");
            return;
        }

        while (true) {
            Message block = (Message) in.readObject();
            if (block.isLastBlock()) break;

            FileManager.guardarFile(
                    taskId,
                    extension,
                    block.getDataBlock(),
                    block.getDataBlock().length
            );
        }

        task.setTieneFichero(true);

        Message response = new Message(Type.RESPONSE);
        response.setResult(true);
        out.writeObject(response);
        out.flush();
    }


    private void handleDownloadFile(Message request) throws IOException {
        int taskId = request.getTaskId();

        // ClientHandler delegates everything
        FileManager.sendFile(taskId, out);
    }
    private void handleChangeState(Message request) { int id = request.getTaskId();
        boolean completed = request.isCompleted();

        Task task = Server.getTask(id);
        if (task != null) {
            task.setCompletada(completed);
            Server.actualizarTask(task);
            System.out.println("Cambiado el estado \"completado\" de la tarea");
        }
    }

    /* =========================
       Utility methods
       ========================= */

    private void sendError(String text) throws IOException {
        Message error = new Message(Type.ERROR);
        error.setErrorMessage(text);
        out.writeObject(error);
        out.flush();
    }

    private Object readObjectSafe() throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Recibido el objeto incorrecto");
        }
    }

    private byte[] copyBuffer(byte[] buffer, int length) {
        byte[] data = new byte[length];
        System.arraycopy(buffer, 0, data, 0, length);
        return data;
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}