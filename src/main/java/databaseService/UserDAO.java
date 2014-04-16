package databaseService;

import api.Api;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;

public class UserDAO {
	private Connection con;
	public UserDAO(Connection con){
		this.con = con;
	}



    public JSONObject create(String username,String about,String name,String email,Boolean isAnonymous) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();
        try {
            PreparedStatement stm = con.prepareStatement("INSERT INTO users (username, about, name, email, isAnonymous) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stm.setString(1, username);
            stm.setString(2, about);
            stm.setString(3, name);
            stm.setString(4, email);
            stm.setBoolean(5, isAnonymous);
            stm.executeUpdate();

            result.put("code", 0);
            result.put("response", response);
            response.put("id", Api.getId(stm));
            response.put("username", username);
            response.put("about", about);
            response.put("name", name);
            response.put("email", email);
            response.put("isAnonymous", isAnonymous);
        } catch (JSONException | SQLException  e){
            System.out.println("user create error");
        }
        return result;
    }



    public JSONObject details(String email) {
        JSONObject result = new JSONObject();
        try {
            JSONObject response = details(email, con);
            if(response != null) {
                result.put("code",0);
                result.put("response",response);
            }
            else {
                result.put("code",1);
                result.put("message","no user with email" + email);
            }
        }
        catch (JSONException e) {}
        return result;
    }



    public static JSONObject details(String email, Connection con) {
        JSONObject response = null;
        ResultSet resultSet;

        try {
            PreparedStatement stmt = con.prepareStatement("SELECT id, username, about, name, email, isAnonymous FROM users WHERE email = ?");
            stmt.setString(1, email);
            resultSet = stmt.executeQuery();


            if (resultSet.next()) {
                response = new JSONObject();
                response.put("id", resultSet.getInt("id"));
                response.put("username", Api.checkNullValue(resultSet.getString("username")));
                response.put("about", Api.checkNullValue(resultSet.getString("about")));
                response.put("name", Api.checkNullValue(resultSet.getString("name")));
                response.put("email", resultSet.getString("email"));
                response.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
                response.put("following", getListFollowings(email, con));
                response.put("followers", getListFollowers(email, con));
                response.put("subscriptions", getListSubscriptions(email, con));
            }
        }
        catch (JSONException | SQLException e) { }
        return response;
    }

    public  JSONObject list (String forum, Integer limit, String order, Integer since_id) {
        return list(forum, limit, order, since_id, con);
    }

    public static JSONObject list (String forum, Integer limit, String order, Integer since_id, Connection con) {
        JSONObject result = new JSONObject();
        JSONArray response = new JSONArray();
        StringBuilder query = new StringBuilder("SELECT users.id, username, about, users.name, email, isAnonymous FROM users JOIN posts ON users.email = posts.user where posts.forum = ?");

        if (since_id != null) {
            query.append(" AND users.id >= ?");
        }

        query.append(" ORDER BY users.id " + order);

        if (limit != null) {
            query.append(" LIMIT " + limit);
        }

        try {
            PreparedStatement stmt = con.prepareStatement(query.toString());

            stmt.setString(1, forum);

            if (since_id != null) {
                stmt.setInt(2, since_id);
            }

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                JSONObject user = new JSONObject();

                user.put("followers", getListFollowers(resultSet.getString("users.email"),con));
                user.put("following",  getListFollowings(resultSet.getString("users.email"), con));
                user.put("subscriptions",  getListSubscriptions(resultSet.getString("users.email"), con));
                user.put("username", Api.checkNullValue(resultSet.getString("username")));
                user.put("about", Api.checkNullValue(resultSet.getString("about")));
                user.put("name", Api.checkNullValue(resultSet.getString("name")));
                user.put("email", resultSet.getString("users.email"));
                user.put("id", resultSet.getInt("users.id"));
                user.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
                response.put(user);
            }

            result.put("code", 0);
            result.put("response", response);
        } catch (SQLException | JSONException e) { e.printStackTrace(); }

        return result;
    }

    public JSONObject follow(String follower, String followee) {
        JSONObject result = new JSONObject();

        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO followers (follower, followee) VALUES (?, ?)");
            stmt.setString(1, follower);
            stmt.setString(2, followee);
            stmt.executeUpdate();
            result.put("code", 0);
            result.put("response", details(follower));
        } catch (JSONException | SQLException e) { System.out.println("user follow error"); }
        return result;
    }

public JSONObject listFollowings(String email, Integer limit,String order,Integer since_id) {
    JSONObject result = new JSONObject();
    JSONArray response = new JSONArray();
    try {
        StringBuilder querry = new StringBuilder("SELECT id, username, about, name, email, isAnonymous FROM followers JOIN users ON email = followee WHERE follower = ? ");
        if(since_id != null) { querry.append("and id >= ? "); }
        if(order != null && order.equals("asc")) { querry.append("order by name asc "); }
        else { querry.append("order by name desc "); }
        if(limit != null) { querry.append("limit ? "); }

        PreparedStatement stmt = con.prepareStatement(querry.toString());

        Integer arg = 1;
        stmt.setString(arg,email);
        ++arg;
        if(since_id != null) {
            stmt.setInt(arg,since_id);
            ++arg;
        }

        if(limit != null) {
            stmt.setInt(arg,limit);
            ++arg;
        }

        ResultSet user = stmt.executeQuery();

        while (user.next()) {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("id", user.getInt("id"));

            jsonObject.put("username",user.getString("username"));
            jsonObject.put("about", user.getString("about"));
            jsonObject.put("name", user.getString("name"));
            jsonObject.put("email", user.getString("email"));
            jsonObject.put("isAnonymous", user.getBoolean("isAnonymous"));
            jsonObject.put("followers", getListFollowers(user.getString("email"), con));
            jsonObject.put("following", getListFollowings(user.getString("email"), con));
            jsonObject.put("subscriptions", getListSubscriptions(user.getString("email"),con));

            response.put(jsonObject);
        }
        result.put("code", 0);
        result.put("response", response);
    } catch (SQLException | JSONException e) { e.printStackTrace(); }
    return result;
}

    public JSONObject listFollowers(String email, Integer limit,String order,Integer since_id) {
        JSONObject result = new JSONObject();
        JSONArray response = new JSONArray();
        try {
            StringBuilder querry = new StringBuilder("SELECT id, username, about, name, email, isAnonymous FROM followers JOIN users ON email = follower WHERE followee = ? ");
            if(since_id != null) { querry.append("and id >= ? "); }
            if(order != null && order.equals("asc")) { querry.append("order by name asc "); }
            else { querry.append("order by name desc "); }
            if(limit != null) { querry.append("limit ? "); }

            PreparedStatement stmt = con.prepareStatement(querry.toString());

            Integer arg = 1;
            stmt.setString(arg,email);
            ++arg;
            if(since_id != null) {
                stmt.setInt(arg,since_id);
                ++arg;
            }

            if(limit != null) {
                stmt.setInt(arg,limit);
                ++arg;
            }

            ResultSet user = stmt.executeQuery();

            while (user.next()) {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("id", user.getInt("id"));

                jsonObject.put("username",user.getString("username"));
                jsonObject.put("about", user.getString("about"));
                jsonObject.put("name", user.getString("name"));
                jsonObject.put("email", user.getString("email"));
                jsonObject.put("isAnonymous", user.getBoolean("isAnonymous"));
                jsonObject.put("followers", getListFollowers(user.getString("email"), con));
                jsonObject.put("following", getListFollowings(user.getString("email"), con));
                jsonObject.put("subscriptions", getListSubscriptions(user.getString("email"),con));

                response.put(jsonObject);
            }
            result.put("code", 0);
            result.put("response", response);
        } catch (SQLException | JSONException e) { e.printStackTrace(); }
        return result;
    }

    public JSONObject unfollow(String follower, String followee) {
        JSONObject result = new JSONObject();

        try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM followers WHERE follower = ? AND followee = ?");

            stmt.setString(1, follower);
            stmt.setString(2, followee);
            stmt.executeUpdate();
            result.put("code", 0);
            result.put("response", details(follower));
        } catch (JSONException | SQLException e) { System.out.println("unfollow error");}
        return result;
    }

    public JSONObject updateProfile(String user, String about, String name) {
        JSONObject result = new JSONObject();
        int affected;

        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE users SET about = ?, name = ? WHERE email = ?");

            stmt.setString(1, about);
            stmt.setString(2, name);
            stmt.setString(3, user);

            affected = stmt.executeUpdate();
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", details(user));
            } else {
                result.put("code", 1);
                result.put("message", "user not found: " + user);
            }
        } catch (JSONException | SQLException e) { System.out.println("user update error");}
        return result;
    }

    public static JSONArray getListFollowers(String email, Connection con) {
        JSONArray followers = new JSONArray();
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT follower FROM followers WHERE followee = ?");
            stmt.setString(1,  email);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                followers.put(resultSet.getString(1));
            }
        }
        catch (SQLException e) { }
        return followers;
    }

    public static JSONArray getListFollowings(String email, Connection con) {
        JSONArray followings = new JSONArray();
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT followee FROM followers WHERE follower = ?");
            stmt.setString(1,  email);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                followings.put(resultSet.getString(1));
            }
        }
        catch (SQLException e) { }
        return followings;
    }

    public static JSONArray getListSubscriptions(String email, Connection con) {
        JSONArray subscriptions = new JSONArray();
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT thread FROM subscriptions WHERE user = ?");
            stmt.setString(1,  email);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                subscriptions.put(resultSet.getInt(1));
            }
        }
        catch (SQLException e) { }
        return subscriptions;
    }

}
