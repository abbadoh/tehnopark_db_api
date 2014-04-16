package frontend;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import api.Api;

public class Frontend extends HttpServlet {
    Api  api;
    public Frontend(){
        api = new Api();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject result;
        Pattern p = Pattern.compile("/db/api/([\\w]+)/([\\w]+)/");
        Matcher m = p.matcher(request.getRequestURI());
        try{
            if(request.getRequestURI().equals("/db/api/clear")){
                api.truncate();
            }
            else {
                if (!m.matches()) {
                    response.getWriter().println(badRequest("invalid url").toString());
                    return;
                }
                String entity = m.group(1);
                String method = m.group(2);

                Map<String,String[]> map = request.getParameterMap();
                Map<String,String> arguments = new HashMap<>();
                for (String key : map.keySet()) {
                    String value = map.get(key)[0];
                    arguments.put(key, parseAray(value));
                }

                System.out.println("////");
                System.out.println(entity+" | "+method);
                for(String arg : arguments.values())
                    System.out.println(arg);

                result = api.handleApiRequest(entity, method, arguments);

                if(result != null) {
                    System.out.println("\\\\");
                    System.out.println(result.toString());
                    response.getWriter().println(makeUnicodeCharactersEscaped(result.toString()));
                }
                else
                    response.getWriter().println(badRequest("api error"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject result;
        Pattern p = Pattern.compile("/db/api/([\\w]+)/([\\w]+)/");
        Matcher m = p.matcher(request.getRequestURI());
        try {
            if (!m.matches()) {
                response.getWriter().println(badRequest("invalid url").toString());
                return;
            }
            String entity = m.group(1);
            String method = m.group(2);

            Map<String,String> arguments = new HashMap<>();
            StringBuffer body = new StringBuffer();
            String line;

            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            JSONObject bodyJson = new JSONObject(body.toString());

            Iterator<String> iterator = bodyJson.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();

                String value = bodyJson.getString(key);
                arguments.put(key, parseAray(value));
            }

            System.out.println("////");
            System.out.println(entity+" | "+method);
            for(String arg : arguments.values())
                System.out.println(arg);

            result = api.handleApiRequest(entity, method, arguments);
            if(result != null) {
                System.out.println("\\\\");
                System.out.println(result.toString());

                response.getWriter().println(makeUnicodeCharactersEscaped(result.toString()));
            }
            else
                response.getWriter().println(badRequest("api error"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String parseAray(String array) {
        if (array.startsWith("[") && array.endsWith("]")) {
            StringBuilder result = new StringBuilder();
            String[] parsed = array.substring(1).substring(0, array.length() - 2).split(",");

            for (int i = 0; i < parsed.length; ++i) {
                parsed[i] = parsed[i].trim();
                String element = parsed[i].substring(1).substring(0, parsed[i].length() - 2);

                if (result.length() != 0) {
                    result.append(",");
                }

                result.append(element);
            }

            return result.toString();
        } else {
            return array;
        }
    }


    private String makeUnicodeCharactersEscaped(String str) {
        StringBuilder b = new StringBuilder();

        for (char c : str.toCharArray()) {
            if ((1024 <= c && c <= 1279) || (1280 <= c && c <= 1327) || (11744 <= c && c <= 11775) || (42560 <= c && c <= 42655)) {
                b.append("\\u").append("0").append(Integer.toHexString(c));
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    public static JSONObject badRequest(String error) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("code", 1);
        obj.put("message", error);
        return obj;
    }
}
