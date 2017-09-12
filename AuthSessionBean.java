package gr.myapp.model;

import com.google.gson.Gson;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.nio.charset.Charset;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;

import java.util.Base64;
import java.util.List;

import java.util.UUID;

import javax.annotation.Resource;

import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import javax.servlet.http.HttpServletRequest;

import javax.swing.SwingUtilities;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.hibernate.validator.constraints.Email;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.WebApplicationException;

@Stateless
@Path("/")
public class AuthSessionBean {
    @Resource
    SessionContext sessionContext;

//   private String baseUrl = "http://10.0.0.126:4985/zingy/";
     private String baseUrl = "http://81.171.24.208:4985/myapp/";
//   private String baseUrl = "http://127.0.0.1:4985/myapp/";
//   private String baseUrl = "http://212.32.242.133:4985/myapp/";
//   private String baseUrl = "http://127.0.0.1:4848";
          
          

    public AuthSessionBean() {
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/register")
    @SuppressWarnings({ "oracle.jdeveloper.webservice.rest.broken-resource-warning",
                        "oracle.jdeveloper.java.nested-assignment" })
    public Response register(String json, @Context HttpServletRequest httpRequest ) throws UnsupportedEncodingException,
                                                                                          MalformedURLException,
                                                                                          IOException {
        httpRequest.setCharacterEncoding("UTF-8");
        System.out.println(json);

        Gson gson = new Gson();

        User user = gson.fromJson(json, User.class);
        
        
        
        user.name = user.name.toLowerCase();
        user.admin_channels = new String[] { "public", "user", user.name };
        user.admin_roles = new String[] { "user" };

        
        
        
        //changes 
        //String Email = user.email;
        String eurl = "http://81.171.24.208:8090/api/search/"+user.email+"/";
        URL obj = new URL(eurl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + eurl);
        
        BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                
        while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    if (inputLine.contains("ok")){
                        responseCode = 401;
                        
                        throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
                                                                  .entity(gson.toJson(responseCode))
                                                                  .build()
                                                          ); 
                        
                        

                    }
                    else if(inputLine.contains("no")) {
                        responseCode = 200;
                        System.out.println("Response Code : " + responseCode);
                        
                        //TODO: register user in couchbase sync gw rest
                        CloseableHttpClient httpClient = HttpClients.custom()
                                                                    .setMaxConnTotal(5)
                                                                    .build();
                        
                        
                        try {
                            StringEntity entity = new StringEntity(gson.toJson(user));
                            entity.setContentType("application/json; charset=utf8");
                            
                            URL url = new URL(baseUrl + "_user/");
                            //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

                            HttpPost httpPost = new HttpPost(url.toString());
                            httpPost.setEntity(entity);

                            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                                @Override
                                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                                    int status = response.getStatusLine().getStatusCode();
                                    if (status == 201) {
                                        HttpEntity entity = response.getEntity();
                                        return entity != null ? EntityUtils.toString(entity) : null;
                                    } else {
                                        throw new ClientProtocolException(Integer.toString(status));
                                    }
                                }
                            };


                            String responseBody = httpClient.execute(httpPost, responseHandler);
                            System.out.println(responseBody);
                        } catch (MalformedURLException e) {
                            return Response.status(Status.INTERNAL_SERVER_ERROR)
                                           .entity(gson.toJson(e.getMessage()))
                                           .build();
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                            return Response.status(Integer.parseInt(e.getMessage())).build();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return Response.status(Status.INTERNAL_SERVER_ERROR)
                                           .entity(gson.toJson(e.getMessage()))
                                           .build();
                        }


                        //create userdata to DB
                        try {
                            user.admin_channels = null;
                            user.admin_roles = null;
                            user.channels = new String[] { "internal", user.name };
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            byte[] hash = digest.digest(user.password.getBytes(StandardCharsets.UTF_8));
                            user.password = Base64.getEncoder().encodeToString(hash);
                            user.validationEmailUuid = UUID.randomUUID()
                                                                       .toString()
                                                                       .replaceAll("-", "");
                            
                            StringEntity entity = new StringEntity(gson.toJson(user));
                            entity.setContentType("application/json; charset=utf8");
                            
                            URL url = new URL(baseUrl + "user-" + user.name);
                            //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

                            HttpPut httpPost = new HttpPut(url.toString());
                            System.out.println(url.toString());
                            httpPost.setHeader("Content-Type", "application/json; charset=utf8");
                            httpPost.setEntity(entity);

                            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                                @Override
                                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                                    int status = response.getStatusLine().getStatusCode();
                                    if (status >= 200 && status < 300) {
                                        HttpEntity entity = response.getEntity();
                                        return entity != null ? EntityUtils.toString(entity) : null;
                                    } else {
                                        throw new ClientProtocolException(Integer.toString(status));

                                    }
                                }
                            };

                            String responseBody = httpClient.execute(httpPost, responseHandler);
                            
                            //send email
                            EmailManager emgr = new EmailManager();
                            user.validationEmailUuid = UUID.randomUUID()
                                                                       .toString()
                                                                       .replaceAll("-", "");
                            if (!emgr.sendValidateEmail(user.email, user.validationEmailUuid, user.name)) {
                                throw new ClientProtocolException(Integer.toString(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
                            }
                            
                            System.out.println(responseBody);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            return Response.status(Status.INTERNAL_SERVER_ERROR)
                                           .entity(gson.toJson(e.getMessage()))
                                           .build();
                        } catch (ClientProtocolException e) {
                            return Response.status(Status.INTERNAL_SERVER_ERROR)
                                           .entity(gson.toJson(e.getMessage()))
                                           .build();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return Response.status(Status.INTERNAL_SERVER_ERROR)
                                           .entity(gson.toJson(e.getMessage()))
                                           .build();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                            return Response.status(Status.INTERNAL_SERVER_ERROR)
                                           .entity(gson.toJson(e.getMessage()))
                                           .build();
                        }

                        
                       return Response.ok(json).build();
                    }

                }
                in.close();
        
        
        return Response.ok(json).build();
        
    }


    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/reset-password")
    @SuppressWarnings("oracle.jdeveloper.webservice.rest.broken-resource-warning")
    public Response resetPassword(String json,
                                  @Context HttpServletRequest httpRequest) throws UnsupportedEncodingException {
        httpRequest.setCharacterEncoding("UTF-8");
        System.out.println(json);

        Gson gson = new Gson();

        User userIn = gson.fromJson(json, User.class);
        userIn.name = userIn.name.toLowerCase();

        if (userIn.resetPasswordUuid == null) {
            userIn.resetPasswordUuid = UUID.randomUUID()
                                           .toString()
                                           .replaceAll("-", "");

            CloseableHttpClient httpClient = HttpClients.custom()
                                                        .setMaxConnTotal(5)
                                                        .build();


            //fetch
            try {
                URL url = new URL(baseUrl + "user-" + userIn.name);
                //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

                HttpGet httpPost = new HttpGet(url.toString());

                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                     IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException(Integer.toString(status));
                        }
                    }
                };

                String responseBody = httpClient.execute(httpPost, responseHandler);
                System.out.println(responseBody);

                User userDB = gson.fromJson(responseBody, User.class);


                userDB.resetPasswordUuid = userIn.resetPasswordUuid;
                String _rev = userDB._rev;
                String _id = userDB._id;
                URL url2 = new URL(baseUrl + _id + "?rev=" + _rev);
                userDB._rev = null;
                userDB._id = null;

                StringEntity entity = new StringEntity(gson.toJson(userDB));
                entity.setContentType("application/json; charset=utf8");
                
                HttpPut httpPut = new HttpPut(url2.toString());
                httpPut.setHeader("Content-Type", "application/json; charset=utf8");
                httpPut.setEntity(entity);

                ResponseHandler<String> responseHandler2 = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                     IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException(Integer.toString(status));
                        }
                    }
                };

                String responseBody2 = httpClient.execute(httpPut, responseHandler2);

                //User userDB = gson.fromJson(responseBody, User.class);
                DocumentResponse dr = gson.fromJson(responseBody2, DocumentResponse.class);

                if (dr.ok) {
                    //send email
                    EmailManager emgr = new EmailManager();
                    if (!emgr.sendEmail(userDB.email, userIn.resetPasswordUuid, userIn.name)) {
                        throw new ClientProtocolException(Integer.toString(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity(gson.toJson(e.getMessage()))
                               .build();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return Response.status(Integer.parseInt(e.getMessage())).build();
            } catch (IOException e) {
                e.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity(gson.toJson(e.getMessage()))
                               .build();
            }
        } else {
            //Reset the passwords
            CloseableHttpClient httpClient = HttpClients.custom()
                                                        .setMaxConnTotal(5)
                                                        .build();


            //fetch
            try {
                URL url = new URL(baseUrl + "user-" + userIn.name);
                //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

                HttpGet httpPost = new HttpGet(url.toString());

                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                     IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException(Integer.toString(status));
                        }
                    }
                };

                String responseBody = httpClient.execute(httpPost, responseHandler);
                System.out.println(responseBody);

                User userDB = gson.fromJson(responseBody, User.class);

                if (userIn.resetPasswordUuid.equals(userDB.resetPasswordUuid)) {

                    String _rev = userDB._rev;
                    String _id = userDB._id;
                    URL url2 = new URL(baseUrl + _id + "?rev=" + _rev);

                    userDB._rev = null;
                    userDB._id = null;
                    userDB.resetPasswordUuid = null;

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(userIn.password.getBytes(StandardCharsets.UTF_8));
                    userDB.password = Base64.getEncoder().encodeToString(hash);

                    StringEntity entity = new StringEntity(gson.toJson(userDB));
                    entity.setContentType("application/json; charset=utf8");
                    
                    HttpPut httpPut = new HttpPut(url2.toString());
                    httpPut.setHeader("Content-Type", "application/json; charset=utf8");
                    httpPut.setEntity(entity);

                    ResponseHandler<String> responseHandler2 = new ResponseHandler<String>() {
                        @Override
                        public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                         IOException {
                            int status = response.getStatusLine().getStatusCode();
                            if (status >= 200 && status < 300) {
                                HttpEntity entity = response.getEntity();
                                return entity != null ? EntityUtils.toString(entity) : null;
                            } else {
                                throw new ClientProtocolException(Integer.toString(status));
                            }
                        }
                    };

                    String responseBody2 = httpClient.execute(httpPut, responseHandler2);
                    System.out.println(responseBody2);

                    DocumentResponse dr = gson.fromJson(responseBody2, DocumentResponse.class);

                    if (dr.ok) {
                        //update SG pswd
                        User userSG = new User();
                        userSG.name = userDB.name;
                        userSG.password = userIn.password;
                        userSG.admin_channels = new String[] { "public", "user", userDB.name };
                        userSG.admin_roles = new String[] { "user" };
                        userSG.email = userDB.email;
                        userSG.disabled = false;


                        StringEntity entity3 = new StringEntity(gson.toJson(userSG));
                        entity3.setContentType("application/json; charset=utf8");
                        
                        URL url3 = new URL(baseUrl + "_user/" + userSG.name);
                        //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

                        HttpPut httpPut3 = new HttpPut(url3.toString());
                        httpPut3.setHeader("Content-Type", "application/json; charset=utf8");
                        httpPut3.setEntity(entity3);

                        ResponseHandler<String> responseHandler3 = new ResponseHandler<String>() {
                            @Override
                            public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                             IOException {
                                int status = response.getStatusLine().getStatusCode();
                                if (status == 200) {
                                    HttpEntity entity = response.getEntity();
                                    return entity != null ? EntityUtils.toString(entity) : null;
                                } else {
                                    throw new ClientProtocolException(Integer.toString(status));
                                }
                            }
                        };

                        String responseBody3 = httpClient.execute(httpPut3, responseHandler3);
                        System.out.println(responseBody3);
                    }
                } else {
                    throw new ClientProtocolException("401");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity(gson.toJson(e.getMessage()))
                               .build();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return Response.status(Integer.parseInt(e.getMessage())).build();
            } catch (IOException e) {
                e.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity(gson.toJson(e.getMessage()))
                               .build();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity(gson.toJson(e.getMessage()))
                               .build();
            }
        }
        //ALL OK
        return Response.ok(json).build();
    }






    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/validate-email")
    @SuppressWarnings("oracle.jdeveloper.webservice.rest.broken-resource-warning")
    public Response validateEmail(String json,
                               @Context HttpServletRequest httpRequest) throws UnsupportedEncodingException {
        httpRequest.setCharacterEncoding("UTF-8");
        System.out.println(json);
        
        Gson gson = new Gson();

        User userIn = gson.fromJson(json, User.class);
        userIn.name = userIn.name.toLowerCase();
        
        //TODO: if match delete validation uuid from user
        CloseableHttpClient httpClient = HttpClients.custom()
                                                    .setMaxConnTotal(5)
                                                    .build();


        //fetch
        try {
            URL url = new URL(baseUrl + "user-" + userIn.name);
            //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

            HttpGet httpPost = new HttpGet(url.toString());

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                 IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException(Integer.toString(status));
                    }
                }
            };

            String responseBody = httpClient.execute(httpPost, responseHandler);
            System.out.println(responseBody);

            User userDB = gson.fromJson(responseBody, User.class);

            if (userIn.validationEmailUuid.equals(userDB.validationEmailUuid)) {

                String _rev = userDB._rev;
                String _id = userDB._id;
                URL url2 = new URL(baseUrl + _id + "?rev=" + _rev);

                userDB._rev = null;
                userDB._id = null;
                userDB.resetPasswordUuid = null;

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(userIn.password.getBytes(StandardCharsets.UTF_8));
                userDB.password = Base64.getEncoder().encodeToString(hash);

                StringEntity entity = new StringEntity(gson.toJson(userDB));
                entity.setContentType("application/json; charset=utf8");
                
                HttpPut httpPut = new HttpPut(url2.toString());
                httpPut.setHeader("Content-Type", "application/json; charset=utf8");
                httpPut.setEntity(entity);

                ResponseHandler<String> responseHandler2 = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                     IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException(Integer.toString(status));
                        }
                    }
                };

                String responseBody2 = httpClient.execute(httpPut, responseHandler2);
                System.out.println(responseBody2);

                //DocumentResponse dr = gson.fromJson(responseBody2, DocumentResponse.class);
            } else {
                throw new ClientProtocolException("401");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return Response.status(Integer.parseInt(e.getMessage())).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        }
        
        //ALL OK
        return Response.ok(json).build();
    }






    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/update-user")
    @SuppressWarnings("oracle.jdeveloper.webservice.rest.broken-resource-warning")
    public Response updateUser(String json,
                               @Context HttpServletRequest httpRequest) throws UnsupportedEncodingException {
        httpRequest.setCharacterEncoding("UTF-8");
        System.out.println(json);

        Gson gson = new Gson();

        User userIn = gson.fromJson(json, User.class);
        userIn.name = userIn.name.toLowerCase();


        //Reset the passwords
        CloseableHttpClient httpClient = HttpClients.custom()
                                                    .setMaxConnTotal(5)
                                                    .build();

        boolean changePassword = false;
        //fetch
        try {
            URL url = new URL(baseUrl + "user-" + userIn.name);

            HttpGet httpPost = new HttpGet(url.toString());

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException(Integer.toString(status));
                    }
                }
            };

            String responseBody = httpClient.execute(httpPost, responseHandler);
            System.out.println(responseBody);

            User userDB = gson.fromJson(responseBody, User.class);


            String _rev = userDB._rev;
            String _id = userDB._id;
            URL url2 = new URL(baseUrl + _id + "?rev=" + _rev);

            userDB._rev = null;
            userDB._id = null;
            userDB.resetPasswordUuid = null;
            if (!userDB.name.equals("internal")) {
                userDB.channels = new String[] { "internal", userDB.name };
            } else {
                userDB.channels = new String[] { userDB.name };
            }
            if (userIn.password != null) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(userIn.password.getBytes(StandardCharsets.UTF_8));
                String newPassword = Base64.getEncoder().encodeToString(hash);
                if (!userDB.password.equals(newPassword)) {
                    changePassword = true;
                    userDB.password = Base64.getEncoder().encodeToString(hash);
                }
            }
            
            if (userIn.firstname != null) {
                userDB.firstname = userIn.firstname;
            }
            
            if (userIn.surname != null) {
                userDB.surname = userIn.surname;
            }
            
            if (userIn.email != null && !userIn.email.equals(userDB.email)) {
                userDB.email = userIn.email;
                userDB.validationEmailUuid = UUID.randomUUID()
                                                           .toString()
                                                           .replaceAll("-", "");
            }
            
            if (userIn.dateOfBirth != null) {
                userDB.dateOfBirth = userIn.dateOfBirth;
            }
            
            if (userIn.nationality != null) {
                userDB.nationality = userIn.nationality;
            }
            
            if (userIn.phone != null){
                userDB.phone = userIn.phone;
            }
            
            StringEntity entity = new StringEntity(gson.toJson(userDB));
            entity.setContentType("application/json; charset=utf8");
            
            HttpPut httpPut = new HttpPut(url2.toString());
            httpPut.setHeader("Content-Type", "application/json; charset=utf8");
            httpPut.setEntity(entity);

            ResponseHandler<String> responseHandler2 = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException(Integer.toString(status));
                    }
                }
            };

            String responseBody2 = httpClient.execute(httpPut, responseHandler2);
            System.out.println(responseBody2);

            DocumentResponse dr = gson.fromJson(responseBody2, DocumentResponse.class);

            if (dr.ok && changePassword) {
                //update SG pswd
                User userSG = new User();
                userSG.name = userDB.name;
                userSG.password = userIn.password;
                userSG.admin_channels = new String[] { "public", "user", userDB.name };
                userSG.admin_roles = new String[] { "user" };
                userSG.email = userDB.email;
                userSG.disabled = false;

                StringEntity entity3 = new StringEntity(gson.toJson(userSG));
                entity3.setContentType("application/json; charset=utf8");
                
                URL url3 = new URL(baseUrl + "_user/" + userSG.name);

                HttpPut httpPut3 = new HttpPut(url3.toString());
                httpPut3.setHeader("Content-Type", "application/json; charset=utf8");
                httpPut3.setEntity(entity3);
                System.out.println(entity3);
                System.out.println(gson.toJson(userSG));
                
                ResponseHandler<String> responseHandler3 = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws ClientProtocolException,
                                                                                     IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status == 200) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException(Integer.toString(status));
                        }
                    }
                };

                String responseBody3 = httpClient.execute(httpPut3, responseHandler3);
                System.out.println(responseBody3);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return Response.status(Integer.parseInt(e.getMessage())).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        }

        //ALL OK
        return Response.ok(json).build();
    }










    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/login")
    @SuppressWarnings("oracle.jdeveloper.webservice.rest.broken-resource-warning")
    public Response login(String json, @Context HttpServletRequest httpRequest) throws UnsupportedEncodingException {
        httpRequest.setCharacterEncoding("UTF-8");
        System.out.println(json);

        Gson gson = new Gson();

        User userIn = gson.fromJson(json, User.class);
        userIn.name = userIn.name.toLowerCase();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        }
        byte[] hash = digest.digest(userIn.password.getBytes(StandardCharsets.UTF_8));
        userIn.password = Base64.getEncoder().encodeToString(hash);

        //TODO: register user in couchbase sync gw rest
        CloseableHttpClient httpClient = HttpClients.custom()
                                                    .setMaxConnTotal(5)
                                                    .build();


        //fetch
        try {
            URL url = new URL(baseUrl + "user-" + userIn.name);
            //            URL url = new URL("http://81.171.24.208:4985/myapp/_user/");

            HttpGet httpPost = new HttpGet(url.toString());

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException(Integer.toString(status));
                    }
                }
            };

            String responseBody = httpClient.execute(httpPost, responseHandler);

            User userDB = gson.fromJson(responseBody, User.class);

            if (!userDB.password.equals(userIn.password)) {
                return Response.status(Status.UNAUTHORIZED)
                               .entity("{}")
                               .build();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return Response.status(Integer.parseInt(e.getMessage())).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(gson.toJson(e.getMessage()))
                           .build();
        }

        return Response.ok(json).build();
    }

    
}


