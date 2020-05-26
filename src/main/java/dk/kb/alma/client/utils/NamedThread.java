package dk.kb.alma.client.utils;

public class NamedThread implements AutoCloseable {
    
    
    private final Thread currentThread;
    private final String oldName;
    
    public NamedThread(String name) {
        currentThread = Thread.currentThread();
        oldName = currentThread.getName();
        currentThread.setName(name);
    }
    
    public static NamedThread postfix(String name){
        String parentName = Thread.currentThread().getName();
        return new NamedThread(parentName+"-"+name);
    }
    
    @Override
    public void close() {
        currentThread.setName(oldName);
    }
}
