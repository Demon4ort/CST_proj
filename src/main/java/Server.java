import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import db.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    public static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private static final byte[] apiKeySecretBytes = "my-token-secret-key-aefsfdgsafghjmgfrdsefsgthjmgfdsghjmgbfvdcbgfhbfvdccfvbg".getBytes(StandardCharsets.UTF_8);
    public static final Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
    private static final byte MAGIC_BYTE = 0x13;
    private static final byte[] KEY_BYTES = "qwerty1234567890".getBytes(StandardCharsets.UTF_8);


    public static void main(String[] args) throws IOException, SQLException {
        ConnectionPool connectionPool = ConnectionPool.create();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        ObjectMapper objectMapper = new ObjectMapper();


        Authenticator authenticator = new Authenticator() {
            @Override
            public Result authenticate(HttpExchange exch) {
                String jwt = exch.getRequestHeaders().getFirst("Authorization");
                SqlOps sqlOps = new SqlOps(connectionPool.getConnection());

                if (jwt != null) {
                    try {
                        String login = getLogin(jwt);
                        System.out.println(login);
                        User user = sqlOps.getUser(login);
                        if (user != null) {
                            connectionPool.releaseConnection(sqlOps.getCon());
                            return new Success(new HttpPrincipal(login, "admin"));
                        }
                        connectionPool.releaseConnection(sqlOps.getCon());
                    } catch (Exception e) {
                        e.printStackTrace();
                        connectionPool.releaseConnection(sqlOps.getCon());
                    }
                }
                connectionPool.releaseConnection(sqlOps.getCon());
                return new Failure(403);
            }
        };


        server.createContext("/", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                byte[] response = "{\"status\": \"on\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } else {
                exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        });

        server.createContext("/login", exchange -> {
            SqlOps sqlOps = new SqlOps(connectionPool.getConnection());
            if (exchange.getRequestMethod().equals("POST")) {
                try {
                    byte[] request = decrypt(exchange.getRequestBody().readAllBytes());
                    User user = objectMapper.readValue(request, User.class);
                    User dbUSer = sqlOps.getUser(user.getLogin());
                    if (dbUSer != null && dbUSer.getId() != null) {
                        if (PasswordAuthentication.authenticate(user.getPassword().toCharArray(), dbUSer.getPassword())) {
                            String jwt = createJWT(dbUSer.getLogin());
                            exchange.getResponseHeaders().set("Authorization", jwt);
                            exchange.sendResponseHeaders(200, 0);
                            connectionPool.releaseConnection(sqlOps.getCon());
                        } else {
                            exchange.sendResponseHeaders(401, 0);
                            connectionPool.releaseConnection(sqlOps.getCon());
                        }
                    } else {
                        exchange.sendResponseHeaders(401, 0);
                        connectionPool.releaseConnection(sqlOps.getCon());
                    }
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(409, 0);
                    connectionPool.releaseConnection(sqlOps.getCon());
                }


            } else {
                connectionPool.releaseConnection(sqlOps.getCon());
                exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        });

        server.createContext("/api/good", exchange -> {
            SqlOps sqlOps = new SqlOps(connectionPool.getConnection());
            if (exchange.getRequestMethod().equals("PUT")) {
                try {
                    byte[] request = decrypt(exchange.getRequestBody().readNBytes(Integer.parseInt(exchange.getRequestHeaders().get("Length").get(0))));
                    Product product = objectMapper.readValue(request, Product.class);
                    //System.out.println(product);
                    if ((product.getName() != null) && (product.getPrice() > 0) && (product.getAmount() > 0)) {
                        product = sqlOps.insertProduct(product);
                        System.out.println(product);
                        byte[] response = encrypt(objectMapper.writeValueAsBytes("Created product: " + product.getId()));
                        exchange.sendResponseHeaders(201, response.length);
                        exchange.getResponseBody().write(response);
                    } else {
                        exchange.sendResponseHeaders(409, 0);
                    }
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(409, 0);
                }
            } else if (exchange.getRequestMethod().equals("GET")) {
                try {
                    List<Product> list = sqlOps.getAllProduct();
                    byte[] response = encrypt(objectMapper.writeValueAsBytes(list));
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(409, 0);
                }
            }

            connectionPool.releaseConnection(sqlOps.getCon());
            exchange.close();
        }).setAuthenticator(authenticator);


        server.createContext("/api/good/", exchange -> {
            byte[] response;
            String method = exchange.getRequestMethod();
            System.out.println(method);
            System.out.println("=======");
            System.out.println(exchange.getRequestURI());
            int id = getId(exchange.getRequestURI().getPath());
            System.out.println(id);
            SqlOps sqlOps = new SqlOps(connectionPool.getConnection());
            Product product = sqlOps.getProduct(id);
            System.out.println(product);
            if (product != null) {
                switch (method) {
                    case "GET":
                        try {
                            response = encrypt(objectMapper.writeValueAsBytes(product));
                            exchange.sendResponseHeaders(200, response.length);
                            exchange.getResponseBody().write(response);
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                            exchange.sendResponseHeaders(409, 0);
                        }
                        break;
                    case "POST":
                        try {
                            byte[] request = decrypt(exchange.getRequestBody().readAllBytes());
                            Product updated = objectMapper.readValue(request, Product.class);
                            if ((updated.getName() != null) && (updated.getPrice() > 0) && (updated.getAmount() > 0)) {
                                sqlOps.updateProduct(updated);
                                System.out.println(updated);
                                exchange.sendResponseHeaders(204, 0);
                            } else {
                                exchange.sendResponseHeaders(409, 0);
                            }
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                            exchange.sendResponseHeaders(409, 0);
                        }
                        break;
                    case "DELETE":
                        sqlOps.deleteProduct(id);
                        exchange.sendResponseHeaders(204, 0);
                        break;
                    default:
                        exchange.sendResponseHeaders(405, 0);
                        break;
                }
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
            connectionPool.releaseConnection(sqlOps.getCon());
            exchange.close();

        }).setAuthenticator(authenticator);

        server.createContext("/api/group/", exchange -> {
            byte[] response;
            String method = exchange.getRequestMethod();
            System.out.println(method);
            int id = getId(exchange.getRequestURI().getPath());
            SqlOps sqlOps = new SqlOps(connectionPool.getConnection());
            Group group = sqlOps.getGroup(id);
            if (group != null) {
                switch (method) {
                    case "GET":
                        try {
                            response = encrypt(objectMapper.writeValueAsBytes(group));
                            exchange.sendResponseHeaders(200, response.length);
                            exchange.getResponseBody().write(response);
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                            exchange.sendResponseHeaders(409, 0);
                        }
                        break;
                    case "POST":
                        try {
                            byte[] request = decrypt(exchange.getRequestBody().readAllBytes());
                            Group updated = objectMapper.readValue(request, Group.class);
                            if ((updated.getName() != null)) {
                                sqlOps.updateGroup(updated);
                                System.out.println(updated);
                                exchange.sendResponseHeaders(204, 0);
                            } else {
                                exchange.sendResponseHeaders(409, 0);
                            }
                        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                            exchange.sendResponseHeaders(409, 0);
                        }

                        break;
                    case "DELETE":
                        sqlOps.deleteGroup(group);
                        exchange.sendResponseHeaders(204, 0);
                        break;
                    default:
                        exchange.sendResponseHeaders(405, 0);
                        break;
                }
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
            connectionPool.releaseConnection(sqlOps.getCon());
            exchange.close();

        }).setAuthenticator(authenticator);

        server.createContext("/api/group", exchange -> {
            SqlOps sqlOps = new SqlOps(connectionPool.getConnection());
            if (exchange.getRequestMethod().equals("PUT")) {
                try {
                    byte[] request = decrypt(exchange.getRequestBody().readNBytes(Integer.parseInt(exchange.getRequestHeaders().get("Length").get(0))));
                    Group group = objectMapper.readValue(request, Group.class);
                    if ((group.getName() != null)) {
                        group = sqlOps.insertGroup(group);
                        System.out.println(group);
                        byte[] response = encrypt(objectMapper.writeValueAsBytes("Created group: " + group.getIdGroup()));
                        exchange.sendResponseHeaders(201, response.length);
                        exchange.getResponseBody().write(response);
                    } else {
                        exchange.sendResponseHeaders(409, 0);
                    }
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(409, 0);
                }
            } else if (exchange.getRequestMethod().equals("GET")) {
                try {
                    List<Group> list = sqlOps.getAllGroup();
                    byte[] response = encrypt(objectMapper.writeValueAsBytes(list));
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(409, 0);
                }
            }

            connectionPool.releaseConnection(sqlOps.getCon());
            exchange.close();
        }).setAuthenticator(authenticator);

    }

    private static int getId(String path) {
        return Integer.parseInt(path.split("/")[3]);

    }

    private static String getName(String path) {
        return path.split("/")[3];
    }

    private static String createJWT(String login) {

        //The JWT signature algorithm we will be using to sign the token


        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret

        //Let's set the JWT Claims
        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + TimeUnit.HOURS.toMillis(10)))
                .setSubject(login)
                .signWith(signingKey, signatureAlgorithm)
                .claim("username", login)
                .compact();
    }

    private static String parseJWT(String jwt) {

        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
        System.out.println("Subject: " + claims.getSubject());
        System.out.println("Expiration: " + claims.getExpiration());
        return claims.getSubject();
    }

    private static String getLogin(String jwt) {

        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
        System.out.println("Subject: " + claims.getSubject());
        System.out.println("Expiration: " + claims.getExpiration());
        return claims.getSubject();
    }

    private static byte[] encrypt(byte[] message) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return cipher.doFinal(message);
    }

    private static byte[] decrypt(byte[] message) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(message);
    }
}