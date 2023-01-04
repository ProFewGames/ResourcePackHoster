package net.skyrath.rphoster.httpd;

import com.profewgames.prohelper.internal.LoaderUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniHttpd extends Thread {

    private volatile boolean running = true;

    protected final ServerSocket socket;

    public MiniHttpd(int port) throws IOException {
        this.socket = new ServerSocket(port);
        this.socket.setReuseAddress(true);
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                new Thread(new MiniConnection(this, socket.accept())).start();
            } catch (IOException e) {
                LoaderUtils.warn("[RPHost MiniHttp] A thread was interrupted in a mini http daemon!");
                e.printStackTrace();
            }
        }
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void terminate() {
        this.running = false;
        if (this.socket != null)
            if (!this.socket.isClosed())
                try {
                    this.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
    }

    /**
     * Executes whever a client requests something
     *
     * @param connection The connection that is requesting a file
     * @param request    The path to the file requested
     * @return A file to return to them; null if the file they requested is invalid
     */
    public File requestFileCallback(MiniConnection connection, String request) {
        return null;
    }

    /**
     * Occurs once the file has been sent
     *
     * @param connection The connection that successfully got sent a file
     * @param request    The request of the client
     */
    public void onSuccessfulRequest(MiniConnection connection, String request) {
    }

    /**
     * Called before the requestFileCallback Contains the original request
     *
     * @param connection The connection that has been requested
     * @param request    The raw request of the client
     */
    public void onClientRequest(MiniConnection connection, String request) {
    }

    /**
     * Handle http error response codes here
     *
     * @param connection The connection that is getting an error on request
     * @param code       The http response code
     */
    public void onRequestError(MiniConnection connection, int code) {
    }

    public static class MiniConnection implements Runnable {

        private final MiniHttpd server;
        private final Socket client;

        public MiniConnection(MiniHttpd server, Socket client) {
            this.server = server;
            this.client = client;
        }

        public Socket getClient() {
            return client;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "8859_1")); OutputStream out = client.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "8859_1"), true)) {
                String request = reader.readLine();
                this.server.onClientRequest(this, request);

                Matcher get = Pattern.compile("GET /?(\\S*).*").matcher(request);
                if (get.matches()) {
                    request = get.group(1);
                    File result = this.server.requestFileCallback(this, request);
                    if (result == null) {
                        writer.println("HTTP/1.0 400 Bad Request");
                        this.server.onRequestError(this, 400);
                    } else {
                        try (FileInputStream fis = new FileInputStream(result)) {
                            // Writes zip files specifically; Designed for resource pack hosting
                            out.write("HTTP/1.0 200 OK\r\n".getBytes());
                            out.write("Content-Type: application/zip\r\n".getBytes());
                            out.write(("Content-Length: " + result.length() + "\r\n").getBytes());
                            out.write(("Date: " + DateFormat.getInstance().format(new Date()) + "\r\n").getBytes());
                            out.write("Server: MiniHttpd\r\n\r\n".getBytes());
                            byte[] data = new byte[64 * 1024];
                            for (int read; (read = fis.read(data)) > -1; )
                                out.write(data, 0, read);
                            out.flush();
                            fis.close();
                            this.server.onSuccessfulRequest(this, request);
                        } catch (FileNotFoundException e) {
                            writer.println("HTTP/1.0 404 Object Not Found");
                            this.server.onRequestError(this, 404);
                        }
                    }
                } else {
                    writer.println("HTTP/1.0 400 Bad Request");
                    this.server.onRequestError(this, 400);
                }
            } catch (IOException e) {
                LoaderUtils.warn("[RPHost MiniConnection] I/O Error");
                e.printStackTrace();
            }
        }
    }
}