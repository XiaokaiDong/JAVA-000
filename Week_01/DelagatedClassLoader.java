import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

//import TransformStrategy;

public class DelagatedClassLoader extends ClassLoader {
    
    private final TransformStrategy transformStrategy;
    private final URL url;

    public DelagatedClassLoader(TransformStrategy strategy, URL url) {
        this.transformStrategy = strategy;
        this.url = url;
    }
    
    public static void main(String[] args) {
        try {
            TransformStrategy transformStrategy = TransformStrategyFactory.getStrategy("NEG");
            URL url = new URL("file:/Users/xiaokaidong/envs/JAVA TRAINING/JAVA-000/Week_01/Hello/Hello.xlass");
            Class<?> clazz = new DelagatedClassLoader(transformStrategy, url).findClass("Hello");
            Object obj = clazz.newInstance();
            Method m = clazz.getMethod("hello");
            m.invoke(obj);
        } catch (MalformedURLException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected Class<?> findClass(String name) {
        Class<?> clazz = null;
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            try(InputStream is = url.openStream()){

                BufferedInputStream bis = new BufferedInputStream(is);
                bytesRead = bis.read(buffer);
                System.out.println("bytesRead is: " + bytesRead);
            }
            transformStrategy.transform(buffer, bytesRead);
            clazz = defineClass(name, buffer, 0, bytesRead);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return clazz;
    }
    
}
