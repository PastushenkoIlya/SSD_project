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
 * Clase ClientHandler
 *
 * Cada instancia de ClientHandler se encarga de atender a UN cliente.
 * Se ejecuta dentro de un hilo gestionado por un ExecutorService.
 *
 * Su función principal es:
 *  - Recibir mensajes del cliente
 *  - Interpretar el tipo de mensaje (Message.Type)
 *  - Ejecutar la operación correspondiente en el servidor
 *  - Enviar una respuesta al cliente
 */
public class ClientHandler implements Runnable {

    // Socket asociado al cliente que se está atendiendo
    private Socket socket;

    // Flujo de entrada para recibir objetos Message desde el cliente
    private ObjectInputStream in;

    // Flujo de salida para enviar objetos Message al cliente
    private ObjectOutputStream out;

    /**
     * Constructor.
     * Recibe el socket creado por el Server al aceptar una conexión.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Metodo principal del hilo.
     * Se ejecuta automáticamente cuando el ExecutorService lanza el ClientHandler.
     */
    @Override
    public void run() {
        try {
            // IMPORTANTE:
            // El ObjectOutputStream debe crearse antes que el ObjectInputStream
            // para evitar bloqueos durante el handshake
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            boolean connected = true;

            // Bucle principal: se mantiene activo mientras el cliente esté conectado
            while (connected) {

                // Se bloquea esperando un mensaje del cliente
                Message request = (Message) in.readObject();

                // Se analiza el tipo de mensaje recibido
                switch (request.getType()) {

                    case LIST -> handleList();
                    case FILTER -> handleFilter(request);
                    case CREATE -> handleCreate(request);
                    case DELETE -> handleDelete(request);
                    case UPLOAD_FILE -> handleUploadFile(request);
                    case DOWNLOAD_FILE -> handleDownloadFile(request);
                    case CHANGE_STATE -> handleChangeState(request);
                    case CLOSE -> connected = false; // El cliente solicita cerrar la conexión

                    // Tipo de mensaje no reconocido
                    default -> sendError("Tipo de petición desconocida");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            // Se produce cuando el cliente se desconecta abruptamente
            System.out.println("Cliente desconectado");
        } finally {
            // Cierre del socket y liberación de recursos
            close();
        }
    }



    /* =========================
       Operaciones sobre tareas
       ========================= */

    /**
     * Envía al cliente la lista completa de tareas.
     */
    private void handleList() throws IOException {

        // Se obtiene una copia del mapa de tareas del servidor
        Map<Integer, Task> allTasks = Server.getAllTasks();

        // Se convierte el mapa en una lista para enviarla al cliente
        List<Task> list = new ArrayList<>(allTasks.values());

        // Se construye el mensaje de respuesta
        Message response = new Message(Type.RESPONSE);
        response.setResult(list);

        // IMPORTANTE:
        // reset() fuerza la re-serialización de los objetos
        // y evita que el cliente reciba versiones antiguas cacheadas
        out.reset(); // <-- force re-serialization or in other words: avoid cached objects
        out.writeObject(response);
        out.flush();
    }

    /**
     * Filtra las tareas según los criterios enviados por el cliente.
     */
    private void handleFilter(Message request) throws IOException {
        Map<Integer, Task> allTasks = Server.getAllTasks();
        List<Task> result = new ArrayList<>();

        // Se recorren todas las tareas aplicando los filtros
        for (Task task : allTasks.values()) {

            // Filtro por estado completada
            if (request.isCompleted() != null &&
                    task.isCompletada() != request.isCompleted()) {
                continue;
            }

            // Filtro por prioridad
            if (request.getPriority() != null &&
                    task.getPrioridad() != request.getPriority()) {
                continue;
            }

            // Filtro por fecha máxima de vencimiento
            if (request.getMaxDueDate() != null &&
                    task.getFechaVencimiento().after(request.getMaxDueDate())) {
                continue;
            }

            // Si pasa todos los filtros, se añade al resultado
            result.add(task);
        }

        Message response = new Message(Type.RESPONSE);
        response.setResult(result);

        out.writeObject(response);
        out.flush();
    }

    /**
     * Crea una nueva tarea en el servidor.
     */
    private void handleCreate(Message request) throws IOException {
        Task task = request.getTask();

        // El servidor genera un id único
        int id = Server.generarTaskId();

        // Se crea una nueva tarea con el id asignado
        Task newTask = new Task(
                id,
                task.getDescripcion(),
                task.getFechaVencimiento(),
                task.getPrioridad()
        );

        // Se añade la tarea al repositorio del servidor
        Server.añadirTask(newTask);

        // Respuesta de confirmación
        Message response = new Message(Type.RESPONSE);
        response.setResult(true);

        out.writeObject(response);
        out.flush();
    }

    /**
     * Elimina una tarea existente según su id.
     */
    private void handleDelete(Message request) throws IOException {
        int id = request.getTaskId();

        // Se elimina la tarea del servidor
        Server.quitarTask(id);

        Message response = new Message(Type.RESPONSE);
        response.setResult(true);

        out.writeObject(response);
        out.flush();
    }

       /* =========================
          Operaciones con archivos
          ========================= */

    /**
     * Recibe un archivo enviado por el cliente y lo guarda en el servidor.
     * La transferencia se realiza por bloques.
     */
    private void handleUploadFile(Message request) throws IOException, ClassNotFoundException {
        int taskId = request.getTaskId();
        String fileName = request.getFileName();

        // Se extrae la extensión del archivo original
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        Task task = Server.getTask(taskId);
        if (task == null) {
            sendError("Tarea no encontrada");
            return;
        }

        // Recepción de bloques hasta que llegue el último
        while (true) {
            Message block = (Message) in.readObject();

            // Si es el último bloque, se termina la recepción
            if (block.isLastBlock()) break;

            // Se delega el guardado al FileManager
            FileManager.guardarFile(
                    taskId,
                    extension,
                    block.getDataBlock(),
                    block.getDataBlock().length
            );
        }

        // Se marca la tarea como que tiene archivo adjunto
        task.setTieneFichero(true);

        Message response = new Message(Type.RESPONSE);
        response.setResult(true);
        out.writeObject(response);
        out.flush();
    }

    /**
     * Envía al cliente el archivo asociado a una tarea.
     */
    private void handleDownloadFile(Message request) throws IOException {
        int taskId = request.getTaskId();

        // ClientHandler delega completamente la operación al FileManager
        FileManager.sendFile(taskId, out);
    }

    /**
     * Cambia el estado de una tarea (completada / no completada).
     */
    private void handleChangeState(Message request) { int id = request.getTaskId();
        boolean completed = request.isCompleted();

        Task task = Server.getTask(id);
        if (task != null) {
            task.setCompletada(completed);

            // Se actualiza la tarea en el servidor
            Server.actualizarTask(task);
            System.out.println("Cambiado el estado \"completado\" de la tarea");
        }
    }

   /* =========================
       Métodos auxiliares
       ========================= */

    /**
     * Envía un mensaje de error al cliente.
     */
    private void sendError(String text) throws IOException {
        Message error = new Message(Type.ERROR);
        error.setErrorMessage(text);
        out.writeObject(error);
        out.flush();
    }

    /**
     * Cierra el socket del cliente y libera recursos.
     */
    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}