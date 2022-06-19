package myMVC;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;

public class Handler {
    private Map<String, String> realClassNameMap = new HashMap<>();
    private Map<String, Object> realClassMap = new HashMap<>();
    private Map<Object, Map<String, Method>> objectMethodMap = new HashMap<>();

    void loadPropertiesFile() {
        try {
            Properties properties = new Properties();
            String fileName = "ApplicationContext.properties";
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            properties.load(in);
            Enumeration en = properties.propertyNames();
            while (en.hasMoreElements()) {
                String key = (String) en.nextElement();
                String value = properties.getProperty(key);
                realClassNameMap.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    String parseURI(String uri) {

        /**
         *     解析uri 获取请求名
         */
        uri = uri.substring(uri.lastIndexOf("/") + 1);
        return uri.substring(0, uri.indexOf("."));

    }

    void putMethods(Object object) {
        Class clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        Map<String, Method> methodMap = new HashMap<>();
        for (Method method : methods) {
            String methodName = method.getName();
            methodMap.put(methodName, method);
        }
        objectMethodMap.put(object, methodMap);
    }

    Object findControllerObject(String askName) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, FileNotFoundException {

        Object obj = realClassMap.get(askName);
        if (obj == null) {

            String fullClassName = realClassNameMap.get(askName);
            if (fullClassName == null) {
                throw new FileNotFoundException();
            }
            Class clazz = Class.forName(fullClassName);

            Constructor constructor = clazz.getDeclaredConstructor();
            obj = constructor.newInstance();
            realClassMap.put(askName, obj);

            this.putMethods(obj);
        }
        return obj;

    }

    Method findMethod(Object obj, String methodName) {

        Map methods = objectMethodMap.get(obj);
        Method method = (Method) methods.get(methodName);

        return method;
    }

    private Map injectionMap(Object obj, HttpServletRequest request) {

        Map map = (Map) obj;
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            String value = request.getParameter(key);
            map.put(key, value);
        }
        return map;

    }

    private Object injectionDomain(Object obj, Class clazz, HttpServletRequest request) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String key = field.getName();
            String value = request.getParameter(key);
            Class type = field.getType();
            if (type == String.class) {
                field.set(obj, value);
            } else if (type == int.class || type == Integer.class) {
                field.set(obj, Integer.parseInt(value));

            } else if (type == float.class || type == Float.class) {
                field.set(obj, Float.parseFloat(value));

            } else if ((type == double.class || type == Double.class)) {
                field.set(obj, Double.parseDouble(value));
            } else if (type == boolean.class || type == Boolean.class) {
                field.set(obj, Boolean.parseBoolean(value));
            } else {
                Object fieldObject = type.getConstructor().newInstance();
                fieldObject = injectionDomain(fieldObject, type, request);
                field.set(obj, fieldObject);

            }

        }
        return obj;
    }

    //接受request参数 拼接为method需要的参数类型（从request.getparameter接回String  几个string拼接成一个USER ）
    //并将拼接的所有参数放入一个Object[] 然后执行
    Object[] injectionParameters(HttpServletResponse response, HttpServletRequest request, Method method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Parameter[] parameters = method.getParameters();
        if (parameters == null || parameters.length == 0) {
            return null;
        }
        Object[] objs = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class type = parameters[i].getType();
            RequestParam annotation = parameters[i].getAnnotation(RequestParam.class);
            if (annotation == null) {
                Object obj = type.getConstructor().newInstance();
                if (obj instanceof Map) {
                    obj = this.injectionMap(obj, request);
                } else if (type == HttpServletRequest.class) {
                    objs[i] = request;
                    continue;

                } else if (type == HttpServletResponse.class) {
                    objs[i] = response;
                    continue;

                } else {
                    obj = this.injectionDomain(obj, type, request);
                }
                objs[i] = obj;

            } else {
                Object object = null;
                String key = annotation.value();
                String value = request.getParameter(key);
                if (type == String.class) {
                    object = value;
                } else if (type == int.class || type == Integer.class) {
                    object = Integer.parseInt(value);

                } else if (type == float.class || type == Float.class) {
                    object = Float.parseFloat(value);

                } else if ((type == double.class || type == Double.class)) {
                    object = Double.parseDouble(value);
                } else if (type == boolean.class || type == Boolean.class) {
                    object = Boolean.parseBoolean(value);
                }
                objs[i] = object;
            }
        }
        return objs;
    }

    String invokeMethod(Method method, Object target, Object[] parameters) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        return (String) method.invoke(target, parameters);
    }

    void handleResponse(Object methodResult, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (methodResult != null) {
            if (methodResult instanceof ModelAndView) {
                ModelAndView modelAndView = (ModelAndView) methodResult;
                this.parseModelAndView(modelAndView, request);
                String viewName = ((ModelAndView) methodResult).getViewName();
                this.parseString(viewName, request, response);
            } else if (methodResult instanceof String) {
                this.parseString((String) methodResult, request, response);

            } else {
                //Json
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("jsonObject", methodResult);
                response.getWriter().write(jsonObject.toJSONString());
            }
        }
    }

    private void parseString(String methodResult, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("".equals(methodResult) || "null".equals(methodResult)) {
            return;
        }
        String[] value = methodResult.split(":");
        if (value.length == 1) {
            request.getRequestDispatcher(value[0]).forward(request, response);
        } else {
            if ("redirect".equals(value[0])) {
                response.sendRedirect(value[1]);
            } else {
                request.getRequestDispatcher(value[1]).forward(request, response);

            }

        }
    }

    private void parseModelAndView(ModelAndView modelAndView, HttpServletRequest request) {
        HashMap map = (HashMap) modelAndView.getAttributeMap();
        Set set = map.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = map.get(key);
            request.setAttribute(key, value);

        }

    }
}
