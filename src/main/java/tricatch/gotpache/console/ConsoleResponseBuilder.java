package tricatch.gotpache.console;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.util.HtmlUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConsoleResponseBuilder {

    public static ConsoleResponse _404() throws IOException {

        List<String> bodyHtml = new ArrayList<>();
        bodyHtml.add("<!DOCTYPE html>");
        bodyHtml.add("<html>");
        bodyHtml.add("<head><title>404 Not Found</title></head>");
        bodyHtml.add("<body><h1>404 Not Found</h1>");
        bodyHtml.add("<p>sorry it didn&#39;t work out :(</p>");
        bodyHtml.add("</body>");
        bodyHtml.add("</html>");

        byte[] body = HtmlUtil.toBytes(bodyHtml);

        List<String> headers = new ArrayList<>();
        headers.add("HTTP/1.1 404 Not Found");
        headers.add("Content-Type: text/html; charset=utf-8");
        headers.add("Content-Length: " + body.length);
        headers.add("Date: " + date());
        headers.add("Server: " + server());

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setBody(body);

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

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setBody(body);

        return res;
    }

    public static ConsoleResponse ok(byte[] body) throws IOException {

        List<String> headers = new ArrayList<>();
        headers.add("HTTP/1.1 200 OK");
        headers.add("Content-Type: application/octet-stream");
        headers.add("Content-Length: " + body.length);
        headers.add("Date: " + date());
        headers.add("Server: " + server());

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setBody(body);

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

        ConsoleResponse res = new ConsoleResponse();
        res.setHeaders(headers);
        res.setBody(body);

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
        if( version!=null && !version.isEmpty() ){
            sb.append( "/" ).append( version );
        }

        sb.append(" (").append(System.getProperty("os.name")).append(")");

        return sb.toString();
    }
}
