package databaseService;

import api.Api;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;

/**
 * Created by gumo on 07/04/14.
 */
public class ThreadDAO {
    Connection con;
    public ThreadDAO(Connection con) {
        this.con = con;
    }

    public JSONObject setClosedState(int id, boolean isClosed) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        int affected;

        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE threads SET isClosed = ? WHERE id = ?");

            stmt.setBoolean(1, isClosed);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", response);
                response.put("thread", id);
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException| SQLException e) { }

        return result;
    }

    public JSONObject create(String forum, String title, String user, String date, String message, String slug, Boolean isClosed, Boolean isDeleted) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO threads (title, slug, message, date, isClosed, isDeleted, forum, user) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, title);
            stmt.setString(2, slug);
            stmt.setString(3, message);
            stmt.setString(4, date);
            stmt.setBoolean(5, isClosed);
            stmt.setBoolean(6, isDeleted);
            stmt.setString(7, forum);
            stmt.setString(8, user);

            stmt.executeUpdate();
            result.put("code", 0);
            result.put("response", response);
            response.put("id", Api.getId(stmt));
            response.put("title", title);
            response.put("slug", slug);
            response.put("message", message);
            response.put("date", date);
            response.put("isClosed", isClosed);
            response.put("isDeleted", isDeleted);
            response.put("forum", forum);
            response.put("user", user);
        } catch (JSONException|SQLException e) { }
        return result;
    }

    public JSONObject details(Integer id, boolean isForum, boolean isUser){
        JSONObject result = new JSONObject();
        JSONObject response = details(id, isForum, isUser, con);
        try {
            if (response != null) {
                result.put("code", 0);
                result.put("response", response);
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        }
        catch (JSONException e ) {}
        return result;
    }

    public static JSONObject details(Integer id, boolean isForum, boolean isUser, Connection con) {
        JSONObject response = new JSONObject();
        ResultSet resultSet;

        try {
            PreparedStatement stmt = con.prepareStatement("SELECT title, slug, message, date, likes, dislikes, points, isClosed, isDeleted, posts, forum, user FROM threads WHERE id = ?");
            stmt.setInt(1, id);
            resultSet = stmt.executeQuery();
            if (resultSet != null) {
                    resultSet.next();
                    response = new JSONObject();
                    response.put("id", id);
                    response.put("title", resultSet.getString("title"));
                    response.put("slug", resultSet.getString("slug"));
                    response.put("message", resultSet.getString("message"));
                    response.put("date", resultSet.getString("date").split("\\.")[0]);
                    response.put("likes", resultSet.getInt("likes"));
                    response.put("dislikes", resultSet.getInt("dislikes"));
                    response.put("points", resultSet.getInt("points"));
                    response.put("isClosed", resultSet.getBoolean("isClosed"));
                    response.put("isDeleted", resultSet.getBoolean("isDeleted"));
                    response.put("posts", resultSet.getInt("posts"));
                    if (isForum) {
                        JSONObject jforum = ForumDAO.details(resultSet.getString("forum"), null, con);
                        response.put("forum", jforum);
                    }
                    else { response.put("forum", resultSet.getString("forum")); }
                    if (isUser) {
                        JSONObject juser = UserDAO.details(resultSet.getString("user"), con);
                        response.put("user", juser);
                    }
                    else { response.put("user", resultSet.getString("user")); }
                }
        } catch (JSONException | SQLException e) { }
        return response;
    }


    public JSONObject list(String argument, boolean isUser, String since,Integer limit, String order, boolean relatedUser, boolean relatedForum) {
        return list(argument, isUser, since,limit,order, relatedUser, relatedForum,con);
    }


    public static JSONObject list(String argument, boolean isUser, String since,Integer limit, String order, boolean relatedUser, boolean relatedForum, Connection con) {
        JSONObject result = new JSONObject();
        StringBuilder querry = new StringBuilder("Select threads.id, threads.slug, threads.title, threads.message, threads.date, threads.isClosed, threads.isDeleted,threads.likes,threads.dislikes,threads.points,threads.posts, threads.user ");
        ResultSet resultSet;

        if(relatedForum) { querry.append(",forums.id, forums.name, forums.short_name, forums.user "); }
            else querry.append(",threads.forum ");
        if(relatedUser) { querry.append(",users.id, users.username, users.about, users.name, users.email, users.isAnonymous "); }
            else querry.append(",threads.user ");

        querry.append("from threads ");

        if(relatedUser){ querry.append("join users on users.email = threads.user "); }
        if(relatedForum) { querry.append("join forums on forums.short_name = threads.forum "); }

        if(isUser) { querry.append("where threads.user = ? "); }
            else{ querry.append("where threads.forum = ? "); }


        if(since != null){ querry.append("and threads.date >= ? "); }
        if (order.equals("asc")){ querry.append("order by threads.date asc "); }
        else if(order.equals("desc")) { querry.append("order by threads.date desc "); }
        if(limit != null){ querry.append("limit ? "); }
        try{
            PreparedStatement stmt = con.prepareStatement(querry.toString());
            int arg = 1;
            stmt.setString(arg,argument);
            ++arg;
            if(since != null) {
                stmt.setString(arg,since);
                ++arg;
            }
            if(limit != null) {
                stmt.setInt(arg,limit);
                ++arg;
            }

            resultSet = stmt.executeQuery();
            JSONArray response = new JSONArray();
            while (resultSet.next()) {
                JSONObject thread = new JSONObject();
                thread.put("id", resultSet.getInt("id"));
                thread.put("title", resultSet.getString("threads.title"));
                thread.put("slug", resultSet.getString("threads.slug"));
                thread.put("message", resultSet.getString("threads.message"));
                thread.put("date", resultSet.getString("threads.date").split("\\.")[0]);
                thread.put("likes", resultSet.getInt("threads.likes"));
                thread.put("dislikes", resultSet.getInt("threads.dislikes"));
                thread.put("points", resultSet.getInt("threads.points"));
                thread.put("isClosed", resultSet.getBoolean("threads.isClosed"));
                thread.put("isDeleted", resultSet.getBoolean("threads.isDeleted"));
                thread.put("posts",resultSet.getInt("threads.posts"));
                if (relatedForum) {
                    JSONObject jforum = new JSONObject();
                    jforum.put("id", resultSet.getLong("forums.id"));
                    jforum.put("name", resultSet.getString("forums.name"));
                    jforum.put("short_name",  resultSet.getString("forums.short_name"));
                    jforum.put("user", resultSet.getString("forums.user"));
                    thread.put("forum", jforum);
                }
                    else { thread.put("forum", resultSet.getString("threads.forum")); }

                if (relatedUser) {
                    JSONObject juser = new JSONObject();
                     juser.put("id", resultSet.getInt("users.id"));
                     juser.put("username", Api.checkNullValue(resultSet.getString("users.username")));
                     juser.put("about",Api.checkNullValue(resultSet.getString("users.about")));
                     juser.put("name", Api.checkNullValue(resultSet.getString("users.name")));
                     juser.put("email", resultSet.getString("email"));
                     juser.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
                     juser.put("following", UserDAO.getListFollowings(resultSet.getString("threads.user"), con));
                     juser.put("followers", UserDAO.getListFollowers(resultSet.getString("threads.user"), con));
                     juser.put("subscriptions", UserDAO.getListSubscriptions(resultSet.getString("threads.user"), con));
                     thread.put("user", juser);
                }
                   else { thread.put("user", resultSet.getString("threads.user")); }

                response.put(thread);
            }
                result.put("code",0);
                result.put("response", response);
        }
        catch (SQLException| JSONException e) {e.printStackTrace();}
        return result;
    }


    public JSONObject setDeletedState(int id, boolean isDeleted) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        int affected = 0;

        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE threads SET isDeleted = ? WHERE id = ?");

            stmt.setBoolean(1, isDeleted);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
        } catch (SQLException e) { }

        try {
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", response);
                response.put("thread", id);
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException e) { }

        return result;
    }

    public JSONObject subscribe(Integer id, String user) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO subscriptions (thread, user) VALUES (?, ?)");

            stmt.setInt(1, id);
            stmt.setString(2, user);

            stmt.executeUpdate();
            result.put("code", 0);
            response.put("thread", id);
            response.put("user", user);
            result.put("response",response);
        } catch (JSONException| SQLException e) { e.printStackTrace();}
        return result;
    }

    public JSONObject unsubscribe(int id, String user) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM subscriptions WHERE thread = ? AND user = ?");

            stmt.setInt(1, id);
            stmt.setString(2, user);
            stmt.executeUpdate();

            result.put("code", 0);
            response.put("thread", id);
            response.put("user", user);
            result.put("response", response);
        } catch (JSONException| SQLException e) { }
        return result;
    }

    public JSONObject update(Integer id, String message, String slug) {
        JSONObject result = new JSONObject();

        int affected;

        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE threads SET message = ?, slug = ? WHERE id = ?");

            stmt.setString(1, message);
            stmt.setString(2, slug);
            stmt.setInt(3, id);

            affected = stmt.executeUpdate();
            if (affected != 0) {
                result.put("code", 0);
                result.put("response", details(id, false, false));
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException| SQLException e) { }
        return result;
    }

    public JSONObject vote(int id, int vote) {
        JSONObject result = new JSONObject();

        int affected;

        try {
            PreparedStatement stmt = null;

            if (vote == 1) {
                stmt = con.prepareStatement("UPDATE threads SET likes = likes + 1, points = points + 1 WHERE id = ?");
            } else if (vote == -1) {
                stmt = con.prepareStatement("UPDATE threads SET dislikes = dislikes + 1, points = points - 1 WHERE id = ?");
            }

            stmt.setInt(1, id);

            affected = stmt.executeUpdate();

            if (affected != 0) {
                result.put("code", 0);
                result.put("response", details(id, false, false));
            } else {
                result.put("code", 1);
                result.put("message", "thread not found: " + id);
            }
        } catch (JSONException | SQLException e) { }
        return result;
    }
}


