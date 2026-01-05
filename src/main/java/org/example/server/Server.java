package org.example.server;
    import java.io.IOException;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    
    import org.example.common.Task;
    
public class Server {

    // Usamos el puerto 5050 para el cliente y el servidor, como en las practicas
    private static final int PUERTO = 5050;

   // HashMap para guardar las tareas (id y tarea)
    private static Map<Integer, Task> tasks = new HashMap<>();

    // Para asignar Ids a las tareas
    private static int taskIdContador = 1;

    // Pool de hilos para manejar multiples clientes
    private static ExecutorService pool = Executors.newCachedThreadPool();

    // METODO MAIN DEL SERVER
    public static void main(String[] args) {
        // Iniciar el servidor
        System.out.println("Iniciando servidor... puerto: " + PUERTO);

        // Servidor escuchando por el puerto 5050 y creacion de server socket
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {

            // Bucle infinito porque el servido debe estar siempre escuchando
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado");
                // Se crea un hilo para atender al cliente
                pool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // METODOS PARA GESTIONAR LAS TAREAS
    
    // Tenemos que usar synchronized para evitar problemas de concurrencia
    // (como que varios clientes creen y borren tareas al mismo tiempo)

    // Generar id unico para cada tarea
    public static synchronized int generarTaskId() {
        return taskIdContador++;
    }
    
    // Añadir una nueva tarea
    public static synchronized void añadirTask(Task task) {
        tasks.put(task.getId(), task);
    }

    // Quitar una tarea por su id
    public static synchronized void quitarTask(int id) {
        tasks.remove(id);
    }

    //Get por su id
    public static synchronized Task getTask(int id) {
        return tasks.get(id);
    }

    //se usa para actualizar el estado "completada" de una tarea
    public static synchronized void actualizarTask(Task task) {tasks.replace(task.getId(), task);}

    // Copia de seguridad del HashMap de tareas (persistencia)
    public static synchronized Map<Integer, Task> getAllTasks() {
        return new HashMap<>(tasks); 
    }
}
