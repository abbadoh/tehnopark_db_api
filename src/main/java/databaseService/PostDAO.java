package databaseService;

import api.Api;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by gumo on 03/04/14.
 */
public class PostDAO {
    private Connection con;
    public PostDAO(Connection con) {
        this.con = con;
    }

    public JSONObject create(String forumShortName, int threadId, String userEmail, String date, String message, boolean isApproved, boolean isHighlighted, boolean isEdited, boolean isSpam, boolean isDeleted, Integer parent) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        try {
           PreparedStatement stm;
            if(parent != null) {
                stm = con.prepareStatement("INSERT INTO posts (message, date, isApproved, isHighlighted, isEdited, isSpam, isDeleted, parent, user, thread, forum) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                stm.setString(1, message);
                stm.setString(2, date);
                stm.setBoolean(3, isApproved);
                stm.setBoolean(4, isHighlighted);
                stm.setBoolean(5, isEdited);
                stm.setBoolean(6, isSpam);
                stm.setBoolean(7, isDeleted);
                stm.setInt(8, parent);
                stm.setString(9, userEmail);
                stm.setInt(10, threadId);
                stm.setString(11, forumShortName);
            }
            else {
                stm = con.prepareStatement("INSERT INTO posts (message, date, isApproved, isHighlighted, isEdited, isSpam, isDeleted, user, thread, forum) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                stm.setString(1, message);
                stm.setString(2, date);
                stm.setBoolean(3, isApproved);
                stm.setBoolean(4, isHighlighted);
                stm.setBoolean(5, isEdited);
                stm.setBoolean(6, isSpam);
                stm.setBoolean(7, isDeleted);
                stm.setString(8, userEmail);
                stm.setInt(9, threadId);
                stm.setString(10, forumShortName);
            }

            stm.executeUpdate();

            result.put("code", 0);

            if(parent != null) { response.put("parent", parent); }
            response.put("id",  Api.getId(stm));
            response.put("message", message);
            response.put("date", date);
            response.put("isApproved", isApproved);
            response.put("isHighlighted", isHighlighted);
            response.put("isEdited", isEdited);
            response.put("isSpam", isSpam);
            response.put("isDeleted", isDeleted);
            response.put("user", userEmail);
            response.put("thread", threadId);
            response.put("forum", forumShortName);
            result.put("response", response);

            stm = con.prepareStatement("UPDATE threads SET posts = posts + 1  WHERE id = ?");
            stm.setInt(1,threadId);
            stm.executeUpdate();
        } catch (SQLException | JSONException e) { e.printStackTrace(); }
        return  result;
    }

    public JSONObject details(int id, boolean isUser, boolean isForum, boolean isThread) {
        JSONObject result = new JSONObject();
        JSONObject response = null;
        ResultSet resultSet;

        try {
            PreparedStatement stmt = con.prepareStatement("SELECT message, date, likes, dislikes, points, isApproved, isHighlighted, isEdited, isSpam, isDeleted, parent, user, thread, forum FROM posts WHERE id = ?");
            stmt.setInt(1, id);

            resultSet = stmt.executeQuery();
        if (resultSet.next()) {

            response = new JSONObject();

            response.put("id", id);
            response.put("message", resultSet.getString("message"));
            response.put("date", resultSet.getString("date").split("\\.")[0]);
            response.put("likes", resultSet.getInt("likes"));
            response.put("dislikes", resultSet.getInt("dislikes"));
            response.put("points", resultSet.getInt("points"));
            response.put("isApproved", resultSet.getBoolean("isApproved"));
            response.put("isHighlighted", resultSet.getBoolean("isHighlighted"));
            response.put("isEdited", resultSet.getBoolean("isEdited"));
            response.put("isSpam", resultSet.getBoolean("isSpam"));
            response.put("isDeleted", resultSet.getBoolean("isDeleted"));
            response.put("parent", Api.checkNullValue(resultSet.getInt("parent")));

            if (isUser) {
                response.put("user", UserDAO.details(resultSet.getString("user"), con));
            }
            else { response.put("user", resultSet.getString("user")); }
            if (isThread) {
                response.put("thread", ThreadDAO.details(Integer.parseInt(resultSet.getString("thread")), false, false, con));
            }
            else { response.put("thread", resultSet.getInt("thread")); }
            if (isForum) {
                response.put("forum", ForumDAO.details(resultSet.getString("forum"), null, con));
            }
            else { response.put("forum", resultSet.getString("forum")); }
        }


            if (response != null) {
                result.put("code", 0);
                result.put("response", response);
            } else {
                result.put("code", 1);
                result.put("message", "post not found: " + id);
            }
        } catch (JSONException | SQLException e) { }

        return result;
    }
//*/

    public JSONObject list(String argument, String argType, String since,Integer limit, String order, boolean relatedUser, boolean relatedForum, boolean relatedThread) {
        return list(argument,argType,since,limit,order,relatedUser,relatedForum,relatedThread,con);
    }

    public static JSONObject list(String argument, String argType, String since,Integer limit, String order, boolean relatedUser, boolean relatedForum, boolean relatedThread, Connection con) {
        JSONObject result = new JSONObject();
        StringBuilder querry = new StringBuilder("Select posts.id, posts.message, posts.date, posts.isApproved, posts.isHighlighted,posts.isEdited,posts.isSpam,posts.isDeleted,posts.likes,posts.dislikes,posts.points,posts.parent ");
        ResultSet resultSet;

        if(relatedForum) { querry.append(",forums.id, forums.name, forums.short_name, forums.user "); }
        else querry.append(",posts.forum ");
        if(relatedUser) { querry.append(",users.id, users.username, users.about, users.name, users.email, users.isAnonymous "); }
        else querry.append(",posts.user ");
        if(relatedThread) { querry.append(",threads.id, threads.title, threads.slug, threads.message, threads.date, threads.likes, threads.dislikes, threads.points, threads.isClosed, threads.isDeleted, threads.posts, threads.forum, threads.user "); }
        else querry.append(",posts.thread ");

        querry.append("from posts ");

        if(relatedUser){ querry.append("join users on users.id = posts.user "); }
        if(relatedForum) { querry.append("join forums on forums.short_name = posts.forum "); }
        if(relatedThread) { querry.append("join threads on threads.id = posts.thread "); }

        if(argType) { querry.append("where posts.forum= ? "); }
        else{ querry.append("where posts.thread = ? "); }


        if(since != null){ querry.append("and posts.date >=? "); }
        if (order.equals("asc")) { querry.append("order by posts.date ASC "); }
        else if(order.equals("desc")) { querry.append("order by posts.date DESC "); }
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
                JSONObject post = new JSONObject();
                post.put("id", resultSet.getInt("posts.id"));
                post.put("message", resultSet.getString("posts.message"));
                post.put("date", resultSet.getString("posts.date").split("\\.")[0]);
                post.put("likes", resultSet.getInt("posts.likes"));
                post.put("dislikes", resultSet.getInt("posts.dislikes"));
                post.put("points", resultSet.getInt("posts.points"));
                post.put("isApproved", resultSet.getBoolean("posts.isApproved"));
                post.put("isHighlighted", resultSet.getBoolean("posts.isHighlighted"));
                post.put("isEdited", resultSet.getBoolean("posts.isEdited"));
                post.put("isSpam", resultSet.getBoolean("posts.isSpam"));
                post.put("isDeleted", resultSet.getBoolean("posts.isDeleted"));
                post.put("parent", Api.checkNullValue(resultSet.getInt("posts.parent")));
                if (relatedForum) {
                    JSONObject jforum = new JSONObject();
                    jforum.put("id", resultSet.getLong("forums.id"));
                    jforum.put("name", resultSet.getString("forums.name"));
                    jforum.put("short_name",  resultSet.getString("forums.short_name"));
                    jforum.put("user", resultSet.getString("forums.user"));
                    post.put("forum", jforum);
                }
                else { post.put("forum", resultSet.getString("posts.forum")); }

                if (relatedUser) {
                    JSONObject juser = new JSONObject();
                    juser.put("id", resultSet.getInt("users.id"));
                    juser.put("username", resultSet.getString("users.username"));
                    juser.put("about",resultSet.getString("users.about"));
                    juser.put("name", resultSet.getString("users.name"));
                    juser.put("email", resultSet.getString("email"));
                    juser.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
                    juser.put("following", UserDAO.getListFollowings(resultSet.getString("threads.user"), con));
                    juser.put("followers", UserDAO.getListFollowers(resultSet.getString("threads.user"), con));
                    juser.put("subscriptions", UserDAO.getListSubscriptions(resultSet.getString("threads.user"), con));
                    post.put("user", juser);
                }
                else { post.put("user", resultSet.getString("posts.user")); }

                if(relatedThread) {
                    JSONObject jthread = new JSONObject();
                    jthread.put("id", resultSet.getInt("threads.id"));
                    jthread.put("title", resultSet.getString("threads.title"));
                    jthread.put("slug", resultSet.getString("threads.slug"));
                    jthread.put("message", resultSet.getString("threads.message"));
                    jthread.put("date", resultSet.getString("threads.date").split("\\.")[0]);
                    jthread.put("likes", resultSet.getInt("threads.likes"));
                    jthread.put("dislikes", resultSet.getInt("threads.dislikes"));
                    jthread.put("points", resultSet.getInt("threads.points"));
                    jthread.put("isClosed", resultSet.getBoolean("threads.isClosed"));
                    jthread.put("isDeleted", resultSet.getBoolean("threads.isDeleted"));
                    jthread.put("post", resultSet.getInt("threads.posts"));
                    jthread.put("forum", resultSet.getString("threads.forum"));
                    jthread.put("user", resultSet.getString("threads.user"));
                    post.put("thread",jthread);
                }
                else { post.put("thread", resultSet.getInt("posts.thread")); }

                response.put(post);
            }
                result.put("code",0);
                result.put("response", response);
        }
        catch (SQLException| JSONException e) { e.printStackTrace(); }
        return result;
    }

//*/

    public JSONObject setDeletedState(int id, boolean isDeleted) {
        JSONObject result = new JSONObject();
        JSONObject response = new JSONObject();

        int affected;

        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE posts SET isDeleted = ? WHERE id = ?");

            stmt.setBoolean(1, isDeleted);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();

            if (affected != 0) {
                result.put("code", 0);
                response.put("post", id);
                result.put("response", response);
            } else {
                result.put("code", 1);
                result.put("message", "post not found: " + id);
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
                stmt = con.prepareStatement("UPDATE posts SET likes = likes + 1, points = points + 1 WHERE id = ?");
            } else if (vote == -1) {
                stmt = con.prepareStatement("UPDATE posts SET dislikes = dislikes + 1, points = points - 1 WHERE id = ?");
            }

            stmt.setInt(1, id);

            affected = stmt.executeUpdate();
            if (affected != 0) {
                result = this.details(id, false, false, false);
            } else {
                result.put("code", 1);
                result.put("message", "post not found: " + id);
            }
        } catch (JSONException| SQLException e) { }
        return result;
    }

    public JSONObject update(int id, String message) {
        JSONObject result = new JSONObject();

        int affected;

        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE posts SET message = ? WHERE id = ?");

            stmt.setString(1, message);
            stmt.setInt(2, id);

            affected = stmt.executeUpdate();
            if (affected != 0) {
                result = this.details(id, false, false, false);
            } else {
                result.put("code", 1);
                result.put("message", "post not found: " + id);
            }
        } catch (JSONException| SQLException e) { }

        return result;
    }
}
