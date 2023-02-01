package io.github.mbr.hibernate.reactive.config;

import io.github.mbr.hibernate.reactive.ReactiveHibernateCrudRepository;
import io.github.mbr.hibernate.reactive.config.annotations.DefaultMethod;
import io.github.mbr.hibernate.reactive.data.jpql.Param;
import io.github.mbr.hibernate.reactive.data.jpql.Query;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryMethod;
import io.github.mbr.hibernate.reactive.impl.annotations.RepositoryPagedMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RepoInterfaceMetaData {
    public final Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass;
    private EntityMetaData entityMetaData;


    private Map<Method, MethodMetaData> methodMetaDataMap = new HashMap<>();
    private IMethodInvokers invokers;

    public static RepoInterfaceMetaData of(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass, IMethodInvokers invokers) {
        return new RepoInterfaceMetaData(repoInterfaceClass, invokers);
    }
    private RepoInterfaceMetaData(Class<? extends ReactiveHibernateCrudRepository<?,?>> repoInterfaceClass, IMethodInvokers invokers) {
        this.repoInterfaceClass = repoInterfaceClass;
        this.invokers = invokers;
        buildEntityMetaData();
        //buildQueryMethodMetaDatas();
        buildMethodMetaDatas();
    }

    private void buildEntityMetaData() {
        Class<?> entityClass = findEntityClass();
        Class<?> idClass = findIdClass();
        String idColName = getIdColumnName(entityClass);
        this.entityMetaData = new EntityMetaData(entityClass, idClass, idColName);
    }

    private void buildMethodMetaDatas() {
        List<Method> methods = new ArrayList<>();
        methods.addAll(List.of(repoInterfaceClass.getMethods()));
        methods.addAll(List.of(repoInterfaceClass.getDeclaredMethods()));

        methods.forEach(method->{
            if(isQueryMethod(method)){
                QueryMethodMetaData metaData = createMetaData(method, _reg_queryInvoker());
                methodMetaDataMap.put(method, metaData);
            }

            RepositoryMethod repositoryMethod = method.getAnnotation(RepositoryMethod.class);
            if(repositoryMethod != null) {
                methodMetaDataMap.put(method, new RepositoryMethodMetaData(method, _reg_RepositoryMethodInvoker()));
            }
            RepositoryPagedMethod repositoryPagedMethod = method.getAnnotation(RepositoryPagedMethod.class);
            if(repositoryPagedMethod != null) {
                methodMetaDataMap.put(method, new RepositoryMethodMetaData(method, _reg_positoryPagedMethodInvoker()));
            }

            if(method.isDefault() || method.isAnnotationPresent(DefaultMethod.class)) {
                methodMetaDataMap.put(method,new DefaultMethodMetaData(method,_reg_defaultMethodInvoker()));
            }
        });
    }

    private IMethodInvoker _reg_defaultMethodInvoker () {
        return (repoInterfaceMetaData, proxy,method, args) -> invokers.getDefaultMethodInvoker().invoke(repoInterfaceMetaData, proxy, method,args);

    }
    private IMethodInvoker _reg_queryInvoker() {
        return (repoInterfaceMetaData, proxy, method, args) -> invokers.getQueryInvoker().invoke(repoInterfaceMetaData, proxy, method,args);
    }
    private IMethodInvoker _reg_RepositoryMethodInvoker() {
        return (repoInterfaceMetaData, proxy,method, args) -> invokers.getRepositoryMethodInvoker().invoke(repoInterfaceMetaData, proxy,  method,args);
    }
    private IMethodInvoker _reg_positoryPagedMethodInvoker() {
        return (repoInterfaceMetaData, proxy,method, args) -> invokers.getRepositoryPagedMethodInvoker().invoke(repoInterfaceMetaData, proxy, method,args);
    }
    /*
    private void buildQueryMethodMetaDatas(){
        List<Method> methods = new ArrayList<>();
        methods.addAll(List.of(repoInterfaceClass.getMethods()));
        methods.addAll(List.of(repoInterfaceClass.getDeclaredMethods()));

        methods.forEach(method->{
            if(isQueryMethod(method)){
                QueryMethodMetaData metaData = createMetaData(method);
                methodMetaDataMap.put(method, metaData);
            }
        });
    }
     */
    private QueryMethodMetaData createMetaData(Method method, IMethodInvoker invoker) {
        log.debug("extracting method: {}", method.getName());
        Class<?> returnTypeClass = method.getReturnType();
        if(!isReactorType(returnTypeClass)){
            throw new RuntimeException(toString(method) + " return type must be either Mono<T>, Flux<T>, or Mono<Page<T>> if pageable is present");
        }
        QueryMethodMetaData metaData = new QueryMethodMetaData(method, invoker);
        Pair<String[], Class<?>[]> params = extractParamNamesAndTypes(method);
        metaData.params = params.getFirst(); //extractParamNames(method);
        metaData.paramTypes = params.getSecond();

        Type returnType = method.getGenericReturnType();

        metaData.isNoResult = isNestTypePresent(returnType, Mono.class, Void.class);
        metaData.isSingleResult = isNestTypePresent(returnType, Mono.class, entityMetaData.entityClass);

        boolean isMonoList = isNestTypePresent(returnType, Mono.class, Page.class, entityMetaData.entityClass);
        log.debug("isMonoList: {}", isMonoList);
        boolean isFlux = isNestTypePresent(returnType,Flux.class, entityMetaData.entityClass);
        metaData.isReturningFlux = isFlux;
        log.debug("isFlux: {}", isFlux);

        metaData.isListResult = isMonoList || isFlux;

        //boolean paged = isNestTypePresent(returnType, Mono.class, Page.class, entityMetaData.entityClass);
        boolean isPageablePresent = isPageArgPresent(method);
        if(isMonoList && !isPageablePresent) {
            throw new RuntimeException(toString(method) + " the last parameter must be Pageable");
        }

        Query query = method.getAnnotation(Query.class);
        metaData.rawQuery = query.value();
        metaData.prepare();
        return metaData;
    }

    private boolean isNestTypePresent(Type returnType, Class<?>... clazz ) {
        return isNestTypePresent(returnType,  0, clazz);
    }
    private boolean isNestTypePresent(Type type, int idx, Class<?>... clazz) {
        if(idx >= clazz.length) return true;

        Class<?> clzz;
        if(type instanceof ParameterizedType) {
            clzz = (Class<?>)((ParameterizedType) type).getRawType();
            if(!isClzz(clzz, clazz[idx])){
                return false;
            }
            log.debug("raw-type-0: {}", clzz);
            for(Type actualType : ((ParameterizedType) type).getActualTypeArguments()) {
                boolean match = isNestTypePresent(actualType, idx + 1, clzz);
                if(match) {
                    return true;
                }
            }
            //return false;
        }
        clzz = (Class<?>)type;
        boolean ret = idx == clazz.length - 1 && isClzz(clzz, clazz[idx]);
        log.debug("raw-type-1: {} {}", clzz, ret);
        return ret;
    }
    private Pair<String[], Class<?>[]> extractParamNamesAndTypes(Method method) {
        Parameter[] params = method.getParameters();
        List<String> names = new ArrayList<>();
        List<Class<?>> types = new ArrayList<>();

        //String[] ret = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            if(isNonSqlParamType(params[i])){
                continue;
            }
            Param param = params[i].getAnnotation(Param.class);
            String nm = params[i].getName();
            if(param != null) {
                nm = param.value();
            }
            names.add(nm);
            types.add(params[i].getType());
        }
        return Pair.of(names.toArray(new String[]{}), types.toArray(new Class[]{}));
    }
    private boolean isNonSqlParamType(Parameter p) {
        Class<?> type = p.getType();
        return isClzz(type, Pageable.class);
    }
    private boolean isReactorType(Class<?> clzz) {
        return isClzz(clzz, Mono.class) || isClzz(clzz, Flux.class);
    }
    private boolean isClzz(Class<?> clzz, Class<?>cls) {
        return clzz == cls || clzz.isAssignableFrom(cls);
    }
    private boolean isPageArgPresent(Method method) {
        Class<?> [] params = method.getParameterTypes();
        if(params.length == 0) return false;
        Class<?> last = params[params.length - 1];
        return last == Pageable.class || last.isAssignableFrom(Pageable.class);
    }
    private String toString(Method method) {
        return method.toString();
    }
    private boolean isQueryMethod(Method method) {
        Query query = method.getAnnotation(Query.class);
        return query != null;
    }
    public final EntityMetaData getEntityMetaData() {
        return entityMetaData;
    }

    private static String getIdColumnName(Class<?> entityClass) {
        log.debug("entityClass: {}", entityClass);
        Field idField = getIdColField(entityClass);
        return getIdColumnName(idField);
    }
    private static Field getIdColField(Class<?> entityClass) {
        Field idField = findByAnnotation(entityClass.getFields(), Id.class);
        if(idField == null) {
            idField = findByAnnotation(entityClass.getDeclaredFields(), Id.class);
        }
        return idField;
    }
    private static String getIdColumnName(Field idField) {
        Column idCol = idField.getAnnotation(Column.class);
        String colName = null;
        if(idCol == null){
            colName = idField.getName();
        }else{
            colName = idCol.name();
        }
        return colName;
    }
    private static Field findByAnnotation(Field[] fields, Class<? extends Annotation> annotationClass) {
        for(Field f : fields) {
            Annotation anno = f.getAnnotation(annotationClass);
            if(anno != null)
                return f;
            anno = f.getDeclaredAnnotation(annotationClass);
            if(anno != null)
                return f;
            if(f.getAnnotatedType().isAnnotationPresent(annotationClass)){
                return f;
            }
        }
        return null;
    }

    private Class<?> findEntityClass() {
        return (Class<?>)find(this.repoInterfaceClass).getActualTypeArguments()[0];//.getClass();
    }
    private Class<?> findIdClass() {
        return (Class<?>)find(this.repoInterfaceClass).getActualTypeArguments()[1];//.getClass();
    }

    private static ParameterizedType find(Class<?> repoInterface ) {
        ParameterizedType parameterizedTypes = extractParameterizedTypes(repoInterface);
        log.debug("method-param-types: {}", parameterizedTypes);
        return parameterizedTypes;
    }

    private static ParameterizedType extractParameterizedTypes(Class<?> repoInterface) {

        Type[] parameterizedTypes = repoInterface.getGenericInterfaces();
        for (Type parameterizedType : parameterizedTypes) {
            if(parameterizedType instanceof ParameterizedType) {
                return ((ParameterizedType)parameterizedType);
            }
        }
        return null;
    }

    public MethodMetaData getMethodMetaData(Method method) {
        return methodMetaDataMap.get(method);
    }

    public static class RepositoryMethodMetaData extends AbsMethodMetaData {
        public RepositoryMethodMetaData(Method method, IMethodInvoker invoker) {
            super(method, invoker);
        }
    }

    public static class DefaultMethodMetaData extends AbsMethodMetaData {
        private MethodHandle handle;
        public DefaultMethodMetaData(Method method, IMethodInvoker invoker) {
            super(method, invoker);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try{
                this.handle = MethodHandles
                        .privateLookupIn(method.getDeclaringClass(), lookup)
                        .unreflectSpecial(method, method.getDeclaringClass());
            }catch (Exception e) {
                //findHandle2(method, e);
                throw new RuntimeException(e);
            }
        }

        private void findHandle2(Method method, Exception e) {
            try {
                this.handle = MethodHandles
                        .lookup()
                        .findSpecial(
                                method.getDeclaringClass(),
                                method.getName(),
                                MethodType.methodType(method.getReturnType()),
                                method.getDeclaringClass());
            } catch (Exception ex) {
                e.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        public MethodHandle getMethodHandle() {
            return this.handle;
        }
    }

    public static class QueryMethodMetaData extends AbsMethodMetaData{
        @Setter
        private String rawQuery;
        @Setter
        private String[] params;
        private Class<?>[] paramTypes;

        @Getter
        private boolean isSingleResult;
        @Getter
        private boolean isNoResult;
        @Getter
        private boolean isListResult;
        @Getter
        private boolean isReturningFlux;

        @Getter
        private String query;
        private List<Map<String, String>> paramsExps;

        private QueryMethodMetaData(Method method, IMethodInvoker invoker) {
            super(method, invoker);
        }

        void prepare() {

            List<Map<String, String>> paramsExps = new ArrayList<>(params.length);
            String sql = rawQuery;
            for(String param : params) {

                Pair<String, Map<String, String>> pair = createNmToExp(param, sql);
                sql = pair.getFirst();
                paramsExps.add(pair.getSecond());
            }

            this.query = sql;
            this.paramsExps = paramsExps;
        }

        private Pair<String, Map<String, String>> createNmToExp(String paramName, String sql) {
            //user_name -> #{#user.name}
            Map<String, String> nmToExp = new HashMap<>();

            //Pattern pattern = Pattern.compile("[:?](#\\{#user.[a-z,A-Z,0-9]+\\})");
            Pattern p = Pattern.compile("[:?](#\\{#"+paramName+".[a-z,A-Z,0-9]+\\})");
            Matcher m = p.matcher(sql);

            String rSql = sql;
            while(m.find()) {
                 String exp = m.group(1);
                 String expName = expToName(exp);
                 nmToExp.put(expName, exp);

                rSql = rSql.replace(":" + exp, ":" + expName);
            }

            return Pair.of(rSql, nmToExp);
        }
        Pattern EXP_NM = Pattern.compile("#\\{#(.*)\\}");
        private String expToName(String exp) {
            Matcher m = EXP_NM.matcher(exp);
            if(m.find()) return m.group(1).replace(".", "_");
            throw new RuntimeException("Unkown expression: " + exp);
        }
        public boolean isResultList() {
            return isListResult;
        }

        public Map<String, Object> getParamsBindings(Object[] args) {
            Map<String, Object> retval = new HashMap<>();

            for (int i = 0; i < params.length; i++) {
                Map<String, Object> bindings = createBinding(i,args[i]);
                retval.putAll(bindings);
            }

            return retval;
        }

        private Map<String, Object> createBinding(int idx, Object arg) {
            Map<String, Object> retval = new HashMap<>();
            Map<String, String> exp = paramsExps.get(idx);
            String paramName = params[idx];

            if(exp.isEmpty()) {
                return Map.of(paramName, arg);
            }

            Class<?> paramType = paramTypes[idx];
            exp.forEach((k,spel)->{
                // k = user_name
                // v = #{#}

                Object actualValue = getSpelExpValue(spel, arg, paramName, paramType);
                retval.put(k, actualValue);
            });

            return retval;
        }
        private Object getSpelExpValue(String spel, Object obj, String paramName, Class<?> paramType){
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(spel, ParserContext.TEMPLATE_EXPRESSION); //"This is expression: :car #{#car.name} #{#car.name.length()}");
            EvaluationContext eCtx = new StandardEvaluationContext();
            eCtx.setVariable(paramName, obj);
            return exp.getValue(eCtx);
        }

        @Override
        public Method getMethod() {
            return null;
        }
    }

    private Map<Method, IMethodInvoker> methodInvokerMap = new HashMap<>();

    public interface IMethodInvokers {
        IMethodInvoker getRepositoryMethodInvoker();
        IMethodInvoker getRepositoryPagedMethodInvoker();
        IMethodInvoker getQueryInvoker();
        IMethodInvoker getDefaultMethodInvoker();

    }
    public interface IMethodInvoker {
        Object invoke(RepoInterfaceMetaData repoInterfaceMetaData, Object proxy, MethodMetaData metaData, Object[] args);
    }

    public interface MethodMetaData {
        Method getMethod();
        //IMethodInvoker getInvoker();
    }
}
