import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MethodGenTest {
    static class E {

    }
    static class D<T> {

    }
    static class C<T> {

    }
    static class B<T>{

    }
    static class A<T>{

    }

    public A<B<C<D<E>>>> test() {
        return new A<>();
    }

    public String test1() {
        return "aa";
    }

    public static void main(String[] args) throws Exception{
        Method m = MethodGenTest.class.getMethod("test");
        Class<?> returnType = m.getReturnType();
        A<B<C<D<E>>>> aRef = new A<>();
        Type type = m.getGenericReturnType();
        gen(type);
    }

    static void gen(Type type) {
        if(!(type instanceof ParameterizedType)) {
            Class<?> clazz = (Class<?>)type;
            System.out.println(clazz.getCanonicalName());
            return;
        }
        System.out.println(((ParameterizedType) type).getRawType());
        Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
        for(Type actualType : actualTypes) {
            gen(actualType);
        }
    }
}
