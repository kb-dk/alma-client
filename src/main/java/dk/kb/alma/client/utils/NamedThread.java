package dk.kb.alma.client.utils;

public class NamedThread implements AutoCloseable {
    
    
    private final Thread currentThread;
    private final String oldName;
    private final String name;
    
    public NamedThread() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        this.name = caller.getClassName()+"(" + caller.getFileName() + ":" + caller.getLineNumber() + ") ";
        currentThread = Thread.currentThread();
        oldName = currentThread.getName();
        currentThread.setName(name);
    }
    
    
    public NamedThread(String name) {
        this.name = name;
        currentThread = Thread.currentThread();
        oldName = currentThread.getName();
        currentThread.setName(name);
    }
    
    public static NamedThread postfix(String name){
        String parentName = Thread.currentThread().getName();
        String threadName = parentName + "->" + name;
        return new NamedThread(threadName);
    }
    
    @Override
    public void close() {
        currentThread.setName(oldName);
    }
    
    public String postfixed(String newName){
        return name + "->" + newName;
    }
    public String getName(){
        return Thread.currentThread().getName();
    }
    
    public String getOldName() {
        return oldName;
    }
}
