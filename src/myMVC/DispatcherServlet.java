package myMVC;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DispatcherServlet extends HttpServlet {
    private Handler handler = new Handler();

    public void init() {
        handler.loadPropertiesFile();

    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            //1.读取请求名对应真实类名
            //2. 解析uri 获取请求名
            String URI = request.getRequestURI();
            String askName = handler.parseURI(URI);
            //3.获取真实类对象
            Object obj = handler.findControllerObject(askName);

            //3.获取方法名
            String methodName = request.getParameter("method");
            if (methodName == null) {
                //返回一个异常页
                System.out.println("methodName为空");
            }
            //4.反射找方法
            Method method = handler.findMethod(obj, methodName);
            //5.执行方法
            Object[] parameters = handler.injectionParameters(response, request, method);
            Object result = handler.invokeMethod(method, obj, parameters);
            //6.做出响应
            handler.handleResponse(result, request, response);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }
}
