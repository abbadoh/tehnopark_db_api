package databaseService;

import api.Api;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;

/**
* Created by gumo on 28/03/14.
*/
public class ForumDAO {
    Connection con;
    public ForumDAO(Connection con) {
        this.con = con;
    }


    public JSONObject create(String name, String short_name, String user) throws SQLException, JSONException {

        PreparedStatement stm = con.prepareStatement("insert into forums (name, short_name, user) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        stm.setString(1, name);
        stm.setString(2, short_name);
        stm.setString(3, user);
        stm.executeUpdate();

        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();


        response.put("id", Api.getId(stm));
        response.put("name", name);
        response.put("short_name", short_name);
        response.put("user", user);

        result.put("code", 0);
        result.put("response", response);


        return result;
    }

    public JSONObject details(String short_name, String user) throws SQLException, JSONException {
        JSONObject result = new JSONObject();
        JSONObject response = details(short_name, user, con);
        if(response != null) {
            result.put("code", 0);
            result.put("response", response);
        }
        else {
            result.put("code", 1);
            result.put("message", "details forum error");
        }
            return result;
    }

    public static JSONObject details(String short_name, String user, Connection con) throws SQLException, JSONException {
        JSONObject response = null;
        if(user == null){
            PreparedStatement stm = con.prepareStatement("select * from forums where short_name=?");
            stm.setObject(1,short_name);
            ResultSet resultSet = stm.executeQuery();
            if(resultSet.next()){
                response = new JSONObject();
                response.put("id", resultSet.getLong("id"));
                response.put("name", Api.checkNullValue(resultSet.getString("name")));
                response.put("short_name", short_name);
                response.put("user", resultSet.getString("user"));
            }
            resultSet.close();
            stm.close();
        } else {
            PreparedStatement stm = con.prepareStatement("select forums.id,forums.name,short_name,users.id,users.username,users.email,users.about,users.isAnonymous, users.name from forums join users on forums.user = users.email where short_name=? ");
            stm.setObject(1,short_name);
            ResultSet resultSet = stm.executeQuery();
            if(resultSet.next()){
                response = new JSONObject();
                response.put("id", resultSet.getLong("forums.id"));
                response.put("name", Api.checkNullValue(resultSet.getString("forums.name")));
                response.put("short_name", short_name);

                JSONObject jUser = new JSONObject();
                jUser.put("id", resultSet.getLong("users.id"));
                jUser.put("username", Api.checkNullValue(resultSet.getString("users.username")));
                jUser.put("email", resultSet.getString("users.email"));
                jUser.put("about", Api.checkNullValue(resultSet.getString("users.about")));
                jUser.put("isAnonymous", resultSet.getBoolean("users.isAnonymous"));
                jUser.put("name", Api.checkNullValue(resultSet.getString("users.name")));


                response.put("user", jUser);
            }
        }
        return  response;
    }
}


