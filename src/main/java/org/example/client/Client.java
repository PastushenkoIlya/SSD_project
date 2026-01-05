package org.example.client;

import org.example.common.Message;
import org.example.common.Message.Type;
import org.example.common.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 * Clase Client
 * Representa la aplicación cliente que se conecta al servidor
 * y permite al usuario interactuar con el gestor de tareas.
 */

public class Client implements Runnable {
    //sirve para configurar ip y puerto a que see conecta el cliente -> localhost, ip privada para arrancar ambos dentro de la LAN,
    //ip publica para camprobar caso práctico
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5050;

    //creación de objetos utilitarios
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Scanner scanner;

    //constructor público, único
    public Client() throws IOException {

        //socket contiene el IP y puerto del servidor
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        //socket contiene el IPy por tanto es capaz de crear los streams necesarios para la conexión
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        scanner = new Scanner(System.in);
    }

    /**
     * Metodo principal del cliente
     * Muestra el menú y gestiona las opciones del usuario
     */
    public void run() {
        boolean exit = false;

        while (!exit) {
            printMenu();
            int option = readInt("Elige una opción: ");
            try {
                switch (option) {
                    case 1 -> listTasks();
                    case 2 -> filterTasks();
                    case 3 -> createTask();
                    case 4 -> deleteTask();
                    case 5 -> uploadFile();
                    case 6 -> downloadFile();
                    case 7 -> changeState();
                    case 0 -> {
                        sendClose();
                        exit = true;
                    }
                    default -> System.out.println("Opción no válida");
                }
            } catch (Exception e) {
                // Captura de errores generales durante la ejecución
                System.err.println("Error: " + e.getMessage());
            }
        }
        // Cierre del socket al salir del programa
        close();
    }

    /* =========================
       Menu operaciones
       ========================= */
    /**
     * Solicita al servidor la lista completa de tareas
     */
    private void listTasks() throws IOException, ClassNotFoundException {
        Message msg = new Message(Type.LIST);
        out.writeObject(msg);
        out.flush();

        // Recepción de la respuesta del servidor
        Message response = (Message) in.readObject();
        printTaskList(response);
    }
    /**
     * Solicita al servidor una lista de tareas filtradas
     *
     * Hay 3 opciones de filtrado: solamente tareas no completadas, hasta cierta fecha de vencimiento, de una cierta prioridad
     *
     */
    private void filterTasks() throws IOException, ClassNotFoundException, ParseException {

        //se crea el mensaje de tipo FILTER que se enviará al servidor
        Message msg = new Message(Type.FILTER);

        System.out.println("Filtrar por:");
        System.out.println("1. Tareas pendientes");
        System.out.println("2. Fecha máxima de vencimiento");
        System.out.println("3. Prioridad");

        int option = readInt("Elige el filtro: ");


        switch (option) {
            case 1 -> msg.setCompleted(false);
            case 2 -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                sdf.setLenient(false);

                String input = readLine("Introduce la fecha límite (dd/MM/yyyy HH:mm): ");
                msg.setMaxDueDate(sdf.parse(input));
            }
            case 3 -> msg.setPriority(readInt("Introduce la prioridad (1=Alta, 2=Media, 3=Baja): "));
            default -> {
                System.out.println("Filtro no válido");
                return;
            }
        }
        //envío el mensaje creado
        out.writeObject(msg);
        out.flush();
        //escucha del puerto por el que estamos conectados al servidor
        Message response = (Message) in.readObject();
        printTaskList(response);
    }
    /// Crea una nueva tarea y la envía al servidor
    private void createTask() throws IOException {
        System.out.print("Descripción: ");
        String descripcion = scanner.nextLine();
        //clase util de Java que nos sirve para variables de
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        //para evitar problemas con la guardación de hora exácta de fecha límite
        sdf.setLenient(false);

        Date dueDate;
        while (true) {
            String input = readLine("Fecha de vencimiento (dd/MM/yyyy HH:mm): ");
            try {
                dueDate = sdf.parse(input);
                break;
            } catch (ParseException e) {
                System.out.println("Formato de fecha inválido. Ejemplo: 07/01/2026 14:30");
            }
        }

        int prioridad = readInt("Prioridad (1=Alta,2=Media,3=Baja): ");

        // El ID se establece en 0 porque lo asigna el servidor
        Task task = new Task(
                0,
                descripcion,
                dueDate,
                prioridad
        );

        Message msg = new Message(Type.CREATE);
        msg.setTask(task);

        out.writeObject(msg);
        out.flush();

        System.out.println("Solicitud de eliminación enviada.");
    }
    /// Solicita al servidor eliminar una tarea por ID
    private void deleteTask() throws IOException {
        int id = readInt("ID de la tarea a eliminar: ");

        Message msg = new Message(Type.DELETE);
        msg.setTaskId(id);

        out.writeObject(msg);
        out.flush();
        //no espera ninguna respuesta del servidor
        System.out.println("Solicitud a eliminar enviada");
    }

    ///Envía un archivo al servidor para adjuntarlo a una tarea
    private void uploadFile() throws IOException {
        int taskId = readInt("ID de la tarea: ");
        System.out.print("Ruta local del archivo: ");
        String path = scanner.nextLine();

        File file = new File(path);
        if (!file.exists()) {
            System.out.println("El archivo no existe.");
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
                out.flush();
                out.reset();
            }

            Message end = new Message(Type.UPLOAD_FILE);
            end.setLastBlock(true);
            out.writeObject(end);
            out.flush();
        }

        System.out.println("Archivo subido correctamente.");
    }

    private void downloadFile() throws IOException, ClassNotFoundException {
        int taskId = readInt("Task ID: ");

        Message msg = new Message(Type.DOWNLOAD_FILE);
        msg.setTaskId(taskId);

        out.writeObject(msg);
        out.flush();

        System.out.print("Guardar archivo como: ");
        String fileName = scanner.nextLine();

        try (FileOutputStream fos = new FileOutputStream(fileName, true)) {
            while (true) {
                Message block = (Message) in.readObject();
                 if (block.getDataBlock() != null) {
                     fos.write(block.getDataBlock());
                }
                   // Cuando llega el mensaje final, se termina
                if (block.isLastBlock()) {
                    break;
                }
               
            }
        }

        System.out.println("Archivo descargado correctamente.");
    }

    private void changeState() throws IOException {
        int taskId = readInt("ID de la tarea: ");

        System.out.print("Marcar como completada (true/false): ");
        boolean completed = Boolean.parseBoolean(scanner.nextLine());

        //se crea el mensaje saliente al servidor con ID de la tarea y el estado deseado de la tarea
        Message msg = new Message(Type.CHANGE_STATE);
        msg.setTaskId(taskId);
        msg.setCompleted(completed);

        //envío de la tarea
        out.writeObject(msg);
        out.flush();

        System.out.println("Solicitud de cambio de estado enviada.");
    }

     /* =========================
       Métodos auxiliares
       ========================= */

    /// Envía al servidor un mensaje de cierre de conexión
    private void sendClose() throws IOException {
        out.writeObject(new Message(Type.CLOSE));
        out.flush();
    }
    /// Cierra el socket del cliente
    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    /// Muestra el menú principal del cliente
    private void printMenu() {
         System.out.println("\n--- CLIENTE GESTOR DE TAREAS ---");
        System.out.println("1. Listar tareas");
        System.out.println("2. Filtrar tareas");
        System.out.println("3. Crear tarea");
        System.out.println("4. Eliminar tarea");
        System.out.println("5. Subir archivo");
        System.out.println("6. Descargar archivo");
        System.out.println("7. Cambiar estado de la tarea (completada / no completada)");
        System.out.println("0. Salir");
}
    ///Imprime por consola la lista de tareas recibida del servidor
    @SuppressWarnings("unchecked")
    private void printTaskList(Message response) {
        Object result = response.getResult();
        if (result instanceof List<?>) {
            List<Task> tasks = (List<Task>) result;
            tasks.forEach(System.out::println);
        } else {
            System.out.println("No se recibieron tareas.");
        }
    }
    //Lee un entero desde consola mostrando un mensaje
    private int readInt(String prompt) {
        System.out.print(prompt);
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }
    // Lee una línea de texto desde consola mostrando un mensaje
    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    //Copia un buffer de bytes con el tamaño exacto leído
    private byte[] copyBuffer(byte[] buffer, int length) {
        byte[] data = new byte[length];
        System.arraycopy(buffer, 0, data, 0, length);
        return data;
    }
}
