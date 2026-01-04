package org.example.client;

import org.example.common.Message;
import org.example.common.Message.Type;
import org.example.common.Task;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Scanner;


public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5050;

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Scanner scanner;

    public Client() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        scanner = new Scanner(System.in);
    }

    public void run() {
        boolean exit = false;

        while (!exit) {
            printMenu();
            int option = readInt("Choose an option: ");

            try {
                switch (option) {
                    case 1 -> listTasks();
                    case 2 -> filterTasks();
                    case 3 -> createTask();
                    case 4 -> deleteTask();
                    case 5 -> uploadFile();
                    case 6 -> downloadFile();
                    case 0 -> {
                        sendClose();
                        exit = true;
                    }
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        close();
    }

    /* =========================
       Menu operations
       ========================= */

    private void listTasks() throws IOException, ClassNotFoundException {
        Message msg = new Message(Type.LIST);
        out.writeObject(msg);
        out.flush();

        Message response = (Message) in.readObject();
        printTaskList(response);
    }

    private void filterTasks() throws IOException, ClassNotFoundException {
        Message msg = new Message(Type.FILTER);

        System.out.println("Filter by:");
        System.out.println("1. Pending tasks");
        System.out.println("2. Max due date");
        System.out.println("3. Priority");

        int option = readInt("Choose filter: ");

        switch (option) {
            case 1 -> msg.setCompleted(false);
            case 2 -> {
                long millis = readLong("Enter max due date (timestamp): ");
                msg.setMaxDueDate(new Date(millis));
            }
            case 3 -> msg.setPriority(readInt("Enter priority (1=Alta,2=Media,3=Baja): "));
            default -> {
                System.out.println("Invalid filter");
                return;
            }
        }

        out.writeObject(msg);
        out.flush();

        Message response = (Message) in.readObject();
        printTaskList(response);
    }

    private void createTask() throws IOException {
        System.out.print("Descripcion: ");
        String descripcion = scanner.nextLine();

        long millis = readLong("Fecha de vencimiento (timestamp): ");
        int prioridad = readInt("Prioridad (1=Alta,2=Media,3=Baja): ");

        // id = 0 → el servidor lo asigna
        Task task = new Task(
                0,
                descripcion,
                new Date(millis),
                prioridad
        );

        Message msg = new Message(Type.CREATE);
        msg.setTask(task);

        out.writeObject(msg);
        out.flush();

        System.out.println("Task creation request sent.");
    }

    private void deleteTask() throws IOException {
        int id = readInt("Task ID to delete: ");

        Message msg = new Message(Type.DELETE);
        msg.setTaskId(id);

        out.writeObject(msg);
        out.flush();

        System.out.println("Delete request sent.");
    }

    private void uploadFile() throws IOException {
        int taskId = readInt("Task ID: ");
        System.out.print("Local file path: ");
        String path = scanner.nextLine();

        File file = new File(path);
        if (!file.exists()) {
            System.out.println("File not found.");
            return;
        }

        Message start = new Message(Type.UPLOAD_FILE);
        start.setTaskId(taskId);
        start.setFileName(file.getName());

        out.writeObject(start);
        out.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                Message block = new Message(Type.UPLOAD_FILE);
                block.setDataBlock(copyBuffer(buffer, bytesRead));
                block.setLastBlock(false);
                out.writeObject(block);
            }

            Message end = new Message(Type.UPLOAD_FILE);
            end.setLastBlock(true);
            out.writeObject(end);
            out.flush();
        }

        System.out.println("File uploaded.");
    }

    private void downloadFile() throws IOException, ClassNotFoundException {
        int taskId = readInt("Task ID: ");

        Message msg = new Message(Type.DOWNLOAD_FILE);
        msg.setTaskId(taskId);

        out.writeObject(msg);
        out.flush();

        System.out.print("Save file as: ");
        String fileName = scanner.nextLine();

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            while (true) {
                Message block = (Message) in.readObject();
                if (block.isLastBlock()) break;
                fos.write(block.getDataBlock());
            }
        }

        System.out.println("File downloaded.");
    }

    /* =========================
       Utility methods
       ========================= */

    private void sendClose() throws IOException {
        out.writeObject(new Message(Type.CLOSE));
        out.flush();
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    private void printMenu() {
        System.out.println("\n--- TASK MANAGER CLIENT ---");
        System.out.println("1. List tasks");
        System.out.println("2. Filter tasks");
        System.out.println("3. Create task");
        System.out.println("4. Delete task");
        System.out.println("5. Upload file");
        System.out.println("6. Download file");
        System.out.println("0. Exit");
    }

    @SuppressWarnings("unchecked")
    private void printTaskList(Message response) {
        Object result = response.getResult();
        if (result instanceof List<?>) {
            List<Task> tasks = (List<Task>) result;
            tasks.forEach(System.out::println);
        } else {
            System.out.println("No tasks received.");
        }
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private long readLong(String prompt) {
        System.out.print(prompt);
        long value = scanner.nextLong();
        scanner.nextLine();
        return value;
    }

    private byte[] copyBuffer(byte[] buffer, int length) {
        byte[] data = new byte[length];
        System.arraycopy(buffer, 0, data, 0, length);
        return data;
    }
}
