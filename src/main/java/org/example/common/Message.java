package org.example.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * La clase Message representa la unidad básica de comunicación
 * entre el cliente y el servidor.

 * Se envía a través de sockets usando ObjectOutputStream,
 * por lo que debe implementar Serializable.

 * Un mismo objeto Message sirve tanto para:
 *  - Peticiones del cliente
 *  - Respuestas del servidor
 */
public class Message implements Serializable {

    // Identificador de versión para la serialización
    @Serial
    private static final long serialVersionUID = 1L;

     /* =========================
       Tipo de mensaje
       ========================= */

    /**
     * Enumeración que indica la acción que se desea realizar
     * o el tipo de respuesta enviada por el servidor.
     */
    public enum Type {
        LIST,           // Solicitar listado de tareas
        FILTER,         // Solicitar filtrado de tareas
        CREATE,         // Crear una nueva tarea
        DELETE,         // Eliminar una tarea por su id
        UPLOAD_FILE,    // Subir un archivo asociado a una tarea
        DOWNLOAD_FILE,  // Descargar un archivo asociado a una tarea
        CHANGE_STATE,   // Cambiar el estado de una tarea (completada / no completada)
        RESPONSE,       // Respuesta genérica del servidor
        ERROR,          // Mensaje de error
        CLOSE           // Cierre de la conexión cliente-servidor
    }
    /**
     * Indica el tipo concreto del mensaje.
     * Es el primer campo que el servidor analiza
     * para decidir qué operación ejecutar.
     */
    private final Type type;

    /* =========================
       Campos de datos opcionales
       ========================= */

    /* ---------- Relacionados con Task ---------- */

    /**
     * Objeto Task completo.
     * Se utiliza principalmente en operaciones como CREATE
     * o en respuestas que devuelven tareas.
     */
    private Task task;
    private Integer taskId;

    /**
     * Identificador de la tarea.
     * Se usa en operaciones como DELETE, DOWNLOAD_FILE,
     * UPLOAD_FILE o CHANGE_STATE.
     */

    /* ------ Filtros --------- */
    /**
     * Indica si se desean tareas completadas o no completadas.
     * Se utiliza en operaciones de tipo FILTER.
     */
    private Boolean completed;
    /**
     * Prioridad de la tarea (1=Alta, 2=Media, 3=Baja).
     * Se utiliza como criterio de filtrado.
     */
    private Integer priority;
    /**
     * Fecha máxima de vencimiento.
     * Se utiliza para filtrar tareas por fecha.
     */
    private Date maxDueDate;

    /* ---------- Transferencia de archivos ------- */
    /**
     * Nombre del archivo que se va a subir o descargar.
     */
    private String fileName;
    /**
     * Bloque de datos del archivo.
     * Se envía en fragmentos para permitir archivos grandes.
     */
    private byte[] dataBlock;

    /**
     * Indica si el bloque actual es el último del archivo.
     * Permite al receptor saber cuándo termina la transferencia.
     */
    private boolean lastBlock;

    /* ---------- Respuestas del servidor ---------- */

    /**
     * Resultado genérico de una operación.
     * Puede contener:
     *  - List<Task>
     *  - Task
     *  - Boolean
     *  - null (si no hay datos que devolver)
     */
    private Object result;

    /**
     * Mensaje de error enviado por el servidor en caso de fallo.
     */
    private String errorMessage;

   /* =========================
       Constructores
       ========================= */

    /**
     * Constructor principal.
     * Crea un mensaje indicando únicamente su tipo.
     * El resto de campos se rellenan mediante setters
     * según la operación concreta.
     */

    public Message(Type type) {
        this.type = type;
    }

    /* =========================
       Getters and setters
       ========================= */

    public Type getType() {
        return type;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Boolean isCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Date getMaxDueDate() {
        return maxDueDate;
    }

    public void setMaxDueDate(Date maxDueDate) {
        this.maxDueDate = maxDueDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getDataBlock() {
        return dataBlock;
    }

    public void setDataBlock(byte[] dataBlock) {
        this.dataBlock = dataBlock;
    }

    public boolean isLastBlock() {
        return lastBlock;
    }

    public void setLastBlock(boolean lastBlock) {
        this.lastBlock = lastBlock;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
