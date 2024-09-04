package tricatch.gotpache.console;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConsoleResponseBuilder {

    public static ConsoleResponse _404() throws IOException {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>").append("\n");
        html.append("<html>").append("\n");
        html.append("<head><title>404 Not Found</title></head>").append("\n");
        html.append("<body><h1>404 Not Found</h1>").append("\n");
        html.append("<p>sorry it didn&#39;t work out :(</p>").append("\n");
        html.append("</body>").append("\n");
        html.append("</html>");

        byte[] body = html.toString().getBytes(StandardCharsets.UTF_8);

        List<String> headers = new ArrayList<>();
        headers.add("HTTP/1.1 404 Not Found");
        headers.add("Content-Type: text/html; charset=utf-8");
        headers.add("Content-Length: " + body.length);
        headers.add("Date: " + date());
        headers.add("Server: " + server());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(body);
        bout.write("\n".getBytes(StandardCharsets.UTF_8));

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setResponse(bout.toByteArray());

        return res;
    }

    public static ConsoleResponse ok(String text) throws IOException {

        byte[] body = text.getBytes(StandardCharsets.UTF_8);

        List<String> headers = new ArrayList<>();
        headers.add("HTTP/1.1 200 OK");
        headers.add("Content-Type: text/html; charset=utf-8");
        headers.add("Content-Length: " + body.length);
        headers.add("Date: " + date());
        headers.add("Server: " + server());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(body);
        bout.write("\n".getBytes(StandardCharsets.UTF_8));

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setResponse(bout.toByteArray());

        return res;
    }

    public static ConsoleResponse ok(byte[] body) throws IOException {

        List<String> headers = new ArrayList<>();
        headers.add("HTTP/1.1 200 OK");
        headers.add("Content-Type: application/octet-stream");
        headers.add("Content-Length: " + body.length);
        headers.add("Date: " + date());
        headers.add("Server: " + server());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(body);
        bout.write("\n".getBytes(StandardCharsets.UTF_8));

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setResponse(bout.toByteArray());

        return res;
    }

    public static ConsoleResponse file(byte[] body, String filename) throws IOException {

        List<String> headers = new ArrayList<>();
        headers.add("HTTP/1.1 200 OK");
        headers.add("Content-Type: application/octet-stream");
        headers.add("Content-Disposition: attachment; filename=\"" + filename + "\"");
        headers.add("Content-Length: " + body.length);
        headers.add("Date: " + date());
        headers.add("Server: " + server());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(body);
        bout.write("\n".getBytes(StandardCharsets.UTF_8));

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setResponse(bout.toByteArray());

        return res;
    }


    private static String date() {

        Date now = new Date();

        SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        return httpDateFormat.format(now);
    }

    private static String server(){

        StringBuilder sb = new StringBuilder();
        sb.append("gotpache");

        String version = ProxyPassServer.class.getPackage().getImplementationVersion();
        if(!StringUtils.isEmpty(version) ){
            sb.append( "/" ).append( version );
        }

        sb.append( " (" + System.getProperty("os.name") +")" );

        return sb.toString();
    }
}
