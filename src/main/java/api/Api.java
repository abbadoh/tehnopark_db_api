package api;

import databaseService.ForumDAO;
import databaseService.PostDAO;
import databaseService.ThreadDAO;
import databaseService.UserDAO;
import frontend.Frontend;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
* Created by gumo on 06/04/14.
*/
public class Api {
    ForumDAO forumDAO;
    PostDAO postDAO;
    UserDAO userDAO;
    ThreadDAO threadDAO;

    public Api() {
        forumDAO = new ForumDAO(getConnection());
        postDAO = new PostDAO(getConnection());
        userDAO = new UserDAO(getConnection());
        threadDAO = new ThreadDAO(getConnection());
    }

    public JSONObject handleApiRequest(String entity, String method, Map<String, String> arguments){
        JSONObject result = null;
        try{
            switch (entity) {
                case "forum":
                    result = forumMethod(method, arguments);
                    break;
                case "user":
                    result = userMethod(method, arguments);
                    break;
                case "thread":
                    result = threadMethod(method, arguments);
                    break;
                case "post":
                    result = postMethod(method, arguments);
                    break;
                default:
                    result = Frontend.badRequest("wrong entity");
            }
        } catch (JSONException | SQLException e) { }
        return  result;
    }

    public JSONObject forumMethod(String method, Map<String, String> arguments) throws SQLException, JSONException {
        JSONObject result = null;
        String name;
        String short_name;
        String user;
        String since;
        String order;
        Integer limit;
        boolean relatedUser;
        boolean relatedForum;
        boolean relatedThread;
        List<String> related;
        switch (method) {
            case "create":
                name = arguments.get("name");
                short_name = arguments.get("short_name");
                user = arguments.get("user");
                result = forumDAO.create(name,short_name,user);
                break;
            case "details":
                short_name = arguments.get("forum");
                if(arguments.get("related") != null) { user = arguments.get("related"); }
                    else { user = null; }
                result = forumDAO.details(short_name, user);
                break;
            case "listPosts":
                relatedUser = false;
                relatedForum = false;
                relatedThread = false;
                short_name = arguments.get("forum");
                if(arguments.get("since") != null) { since = arguments.get("since"); }
                    else { since = null; }
                if(arguments.get("limit") != null) limit = Integer.parseInt(arguments.get("limit"));
                    else { limit = null; }
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                    else { order = "desc"; }

                if(arguments.get("related") != null) {
                    related = Arrays.asList(arguments.get("related").split(","));
                    for(String arg: related) {
                        if(arg.equals("user")) { relatedUser = true;}
                        if(arg.equals("forum")) { relatedForum = true;}
                        if(arg.equals("thread")) { relatedThread = true;}
                    }
                }

                result = postDAO.list(short_name, true, since, limit, order, relatedUser, relatedForum, relatedThread);
                break;
            case "listThreads":
                relatedUser = false;
                relatedForum = false;
                short_name = arguments.get("forum");
                if(arguments.get("since") != null) { since = arguments.get("since"); }
                    else { since = null; }
                if(arguments.get("limit") != null) limit = Integer.parseInt(arguments.get("limit"));
                    else { limit = null; }
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                    else { order = "desc"; }


                if (arguments.get("related") != null) {
                    related = Arrays.asList(arguments.get("related").split(","));
                    for(String arg : related) {
                        if(arg.equals("user")) { relatedUser = true;}
                        if(arg.equals("forum")) { relatedForum = true;}
                    }
                }

                result = threadDAO.list(short_name,false,since,limit,order,relatedUser,relatedForum);
                break;
            case "listUsers":
                Integer since_id = null;
                if(arguments.get("since_id") != null) { since_id = Integer.parseInt(arguments.get("since_id")); }
                if(arguments.get("limit") != null) limit = Integer.parseInt(arguments.get("limit"));
                else { limit = null; }
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                else { order = "desc"; }
                short_name = arguments.get("forum");

                result = userDAO.list(short_name,limit,order,since_id);
                break;
            default:
                result = Frontend.badRequest("wrong method");
        }
        return result;
    }

    public JSONObject userMethod(String method, Map<String, String> arguments) throws SQLException, JSONException {
        JSONObject result = null;
        String follower;
        String followee;
        String about;
        String name;
        String email;
        String order;
        Integer since_id;
        Integer limit;
//        List<String> related;

        switch (method) {
            case "create":
                String username = arguments.get("username");
                about = arguments.get("about");
                name = arguments.get("name");
                email = arguments.get("email");
                Boolean isAnonymous;
                if(arguments.get("isAnonymous") != null) {
                    isAnonymous = Boolean.parseBoolean(arguments.get("isAnonymous"));
                } else { isAnonymous = false; }
                result = userDAO.create(username,about,name,email,isAnonymous);
                break;
            case"details":
                email = arguments.get("user");
                result = userDAO.details(email);
                break;
            case"follow":
                follower = arguments.get("follower");
                followee = arguments.get("followee");
                result = userDAO.follow(follower, followee);
                break;
            case"listFollowers":
                limit = null;
                since_id = null;
                if(arguments.get("limit") != null) { limit = Integer.parseInt(arguments.get("limit")); }
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                    else { order = "desc"; }
                if(arguments.get("since_id") != null) { since_id = Integer.parseInt(arguments.get("since_id")); }
                email = arguments.get("user");
                result = userDAO.listFollowers(email,limit,order,since_id);
                break;
            case"listFollowing":
                limit = null;
                since_id = null;
                if(arguments.get("limit") != null) { limit = Integer.parseInt(arguments.get("limit")); }
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                    else { order = "desc"; }
                if(arguments.get("since_id") != null) { since_id = Integer.parseInt(arguments.get("since_id")); }
                email = arguments.get("user");
                result = userDAO.listFollowings(email, limit, order, since_id);
                break;
            case "listPosts":
                limit = null;
                String since = null;
                if(arguments.get("limit") != null) { limit = Integer.parseInt(arguments.get("limit")); }
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                else { order = "desc"; }
                if(arguments.get("since_id") != null) { since_id = Integer.parseInt(arguments.get("since_id")); }
                email = arguments.get("user");

                result = postDAO.list(email, false,since,limit,order,false,false,false);
                break;
            case "unfollow":
                follower = arguments.get("follower");
                followee = arguments.get("followee");
                result = userDAO.unfollow(follower, followee);
                break;
            case"updateProfile":
                String user = arguments.get("user");
                about = arguments.get("about");
                name = arguments.get("name");
                result = userDAO.updateProfile(user, about, name);
                break;
            default:
                result = Frontend.badRequest("wrong method");
        }
        return result;
    }

    public JSONObject threadMethod(String method, Map<String, String> arguments) throws SQLException, JSONException {
        JSONObject result;
        Integer id;
        String user;
        String message;
        String slug;
        boolean isForum;
        boolean isUser;
        List<String> related;
        switch (method) {
            case "create":
                String forum = arguments.get("forum");
                String title = arguments.get("title");
                user = arguments.get("user");
                String date = arguments.get("date");
                message = arguments.get("message");
                slug = arguments.get("slug");
                Boolean isClosed = Boolean.parseBoolean(arguments.get("isClosed"));
                Boolean isDeleted;
                if(arguments.get("isDeleted") != null) {
                    isDeleted = Boolean.parseBoolean(arguments.get("isDeleted"));
                }
                else {
                    isDeleted = false;
                }
                result = threadDAO.create(forum,title,user,date,message,slug,isClosed,isDeleted);
                break;
            case"close":
                id = Integer.parseInt(arguments.get("thread"));
                result = threadDAO.setClosedState(id, true);
                break;
            case"details":
                id = Integer.parseInt(arguments.get("thread"));
                isForum = false;
                isUser = false;
                related = (arguments.get("related") != null ? Arrays.asList(arguments.get("related").split(",")) : null);
                if(related != null) {
                    for(String arg: related)
                    {
                        if(arg.equals("forum")) isForum = true;
                        if(arg.equals("user")) isUser = true;
                    }
                }
                result = threadDAO.details(id, isForum, isUser);
                break;
            case"list":
                String since = null;
                if(arguments.get("since") != null) { since = arguments.get("since"); }
                Integer limit = null;
                if(arguments.get("limit") != null) limit = Integer.parseInt(arguments.get("limit"));
                String order;
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                    else { order = "desc"; }

                String argument ;
                if(arguments.get("user") != null) {
                    argument = arguments.get("user");
                    isUser = true;
                }
                else {
                    argument = arguments.get("forum");
                    isUser = false;
                }

                result = threadDAO.list(argument, isUser, since,limit,order, false, false);
                break;
            case"listPosts":
                String stringId = arguments.get("thread");
                since = null;
                if(arguments.get("since") != null) { since = arguments.get("since"); }
                limit = null;
                if(arguments.get("limit") != null) limit = Integer.parseInt(arguments.get("limit"));
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                else { order = "desc"; }

                result = postDAO.list(stringId, false, since, limit, order, false, false, false);
                break;
            case"open":
                id = Integer.parseInt(arguments.get("thread"));
                result = threadDAO.setClosedState(id, false);
                break;
            case"remove":
                id = Integer.parseInt(arguments.get("thread"));
                result = threadDAO.setDeletedState(id, true);
                break;
            case"restore":
                id = Integer.parseInt(arguments.get("thread"));
                result = threadDAO.setDeletedState(id, false);
                break;
            case"subscribe":
                id = Integer.parseInt(arguments.get("thread"));
                user = arguments.get("user");
                result = threadDAO.subscribe(id, user);
                break;
            case"unsubscribe":
                id = Integer.parseInt(arguments.get("thread"));
                user = arguments.get("user");
                result = threadDAO.unsubscribe(id, user);
                break;
            case"update":
                id = Integer.parseInt(arguments.get("thread"));
                message = arguments.get("message");
                slug = arguments.get("slug");
                result = threadDAO.update(id, message, slug);
                break;
            case"vote":
                id = Integer.parseInt(arguments.get("thread"));
                Integer vote = Integer.parseInt(arguments.get("vote"));
                result = threadDAO.vote(id, vote);
                break;
            default:
                result = Frontend.badRequest("wrong method");
        }
        return result;
    }

    public JSONObject postMethod(String method, Map<String, String> arguments) throws SQLException, JSONException {
        JSONObject result;
        Integer id;
        String message;
        List<String> related;
        switch (method) {
            case "create":

                String date = arguments.get("date");
                Integer threadId = Integer.parseInt(arguments.get("thread"));
                message = arguments.get("message");
                String userEmail = arguments.get("user");
                String forumShortName = arguments.get("forum");
                Integer parent = null;
                if(arguments.get("parent") != null){  parent = Integer.parseInt(arguments.get("parent")); }
                Boolean isApproved = arguments.get("isApproved") != null && Boolean.parseBoolean(arguments.get("isApproved"));
                Boolean isHighlighted = arguments.get("isHighlighted") != null && Boolean.parseBoolean(arguments.get("isHighlighted"));
                Boolean isEdited = arguments.get("isEdited") != null && Boolean.parseBoolean(arguments.get("isEdited"));
                Boolean isSpam = arguments.get("isSpam") != null && Boolean.parseBoolean(arguments.get("isSpam"));
                Boolean isDeleted = arguments.get("isDeleted") != null && Boolean.parseBoolean(arguments.get("isDeleted"));
                result = postDAO.create(forumShortName, threadId, userEmail, date, message, isApproved,isHighlighted,isEdited,isSpam,isDeleted, parent);
                break;
            case "details":
                id = Integer.parseInt(arguments.get("post"));
                boolean isForum = false;
                boolean isThread = false;
                if(arguments.get("related") != null){
                    related = Arrays.asList(arguments.get("related").split(","));
                    for(String arg: related){
                        if(arg.equals("user")) { isForum = true;}
                        if(arg.equals("forum")) {isForum = true;}
                        if(arg.equals("thread")) {isThread = true;}
                    }
                }
                result = postDAO.details(id, isForum, isForum, isThread);
                break;
            case "list":
                String since = null;
                if(arguments.get("since") != null) { since = arguments.get("since"); }
                Integer limit = null;
                if(arguments.get("limit") != null) limit = Integer.parseInt(arguments.get("limit"));
                String order;
                if(arguments.get("order") != null ) { order = arguments.get("order"); }
                else { order = "desc"; }
                String argument ;
                if(arguments.get("forum") != null) {
                    argument = arguments.get("forum");
                    isForum = true;
                }
                else {
                    argument = arguments.get("thread");
                    isForum = false;
                }
                result = postDAO.list(argument,isForum,since,limit,order,false,false,false);
                break;
            case "remove":
                id = Integer.parseInt(arguments.get("post"));
                result = postDAO.setDeletedState(id, true);
                break;
            case "restore":
                id = Integer.parseInt(arguments.get("post"));
                result = postDAO.setDeletedState(id, false);
                break;
            case "vote":
                id = Integer.parseInt(arguments.get("post"));
                Integer vote = Integer.parseInt(arguments.get("vote"));
                result = postDAO.vote(id, vote);
                break;
            case "update":
                id = Integer.parseInt(arguments.get("post"));
                message = arguments.get("message");
                result = postDAO.update(id, message);
                break;
            default:
                result = Frontend.badRequest("wrong method");
        }
        return result;
    }


    public static int getId(PreparedStatement stm){
        int id = -1;
        try{
            ResultSet resultSet = stm.getGeneratedKeys();
            if (resultSet != null && resultSet.next()) {
                id = resultSet.getInt(1);
            } else {
                id = -1;
            }
        }
        catch (SQLException e) { }
        return id;
    }


    public void truncate() {
        try {
        Connection con = getConnection();
        con.prepareStatement("TRUNCATE followers").executeUpdate();
        con.prepareStatement("TRUNCATE forums").executeUpdate();
        con.prepareStatement("TRUNCATE posts").executeUpdate();
        con.prepareStatement("TRUNCATE subscriptions").executeUpdate();
        con.prepareStatement("TRUNCATE threads").executeUpdate();
        con.prepareStatement("TRUNCATE users").executeUpdate();
        }
        catch (SQLException e) {}
    }

//    public static Object checkNullValue(Object value) {
//        if (value != null) { return value; }
//        else { return  JSONObject.NULL;}
//    }


    public static Object checkNullValue(String value) {
        if (value == null || value.equals("null")) { return JSONObject.NULL; }
        else { return  value; }
    }
    public static Object checkNullValue(Integer value) {
        if (value == null || value == 0 ) { return  JSONObject.NULL; }
        else { return  value; }
    }

    public Connection getConnection() {
        try {
            DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
            String url = "jdbc:mysql://127.0.0.1:3306/api_db?user=user&password=user&characterEncoding=UTF-8";
            return DriverManager.getConnection(url);
        } catch (SQLException | InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
