import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Test {

    static interface MyIf {
        default void test(){
            System.out.println("test");
        }
    }

    static class MyIfImpl implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println(method.getName() + " is called");
            return null;//method.invoke(new MyIfImpl(), args);
        }
    }

    public static void main(String[] args) throws Exception{
        MyIf myIf = (MyIf) Proxy.newProxyInstance(MyIf.class.getClassLoader(), new Class[]{MyIf.class}, new MyIfImpl());
        myIf.test();

        MyIf.class.getMethod("test").invoke(myIf);

    }
}
