package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket sock;

    public ClientHandler(Socket sock) {
        this.sock = sock;
    }

    @Override
    public void run() {
        try (
                BufferedReader in      = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                PrintWriter out        = new PrintWriter(sock.getOutputStream(), true);
                OutputStream dataOut   = sock.getOutputStream();
        ) {
            out.println("Hello!");

            String cmd = in.readLine();
            if (cmd == null || "bye".equalsIgnoreCase(cmd.trim())) {
                out.println("disconnected");
                sock.close();
                return;
            }
            if (!"SEND".equalsIgnoreCase(cmd.trim())) {
                out.println("Please type a different command");
                sock.close();
                return;
            }

            String seqLine = in.readLine();
            if (seqLine == null) {
                sock.close();
                return;
            }

            for (String part : seqLine.split(",")) {
                int idx;
                try {
                    idx = Integer.parseInt(part.trim());
                } catch (NumberFormatException e) {
                    out.println("Invalid index: " + part);
                    continue;
                }
                String fileName = String.format("sample%02d.bmp", idx);
                File f = new File(Server.BASE_DIR + fileName);
                if (!f.exists() || !f.isFile()) {
                    out.println("File not found: " + fileName);
                    continue;
                }

                long size = f.length();
                out.println("FOUND " + size + " " + fileName);

                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        dataOut.write(buf, 0, r);
                    }
                    dataOut.flush();
                }
            }
            String finalCmd = in.readLine();
            if ("bye".equalsIgnoreCase(finalCmd)) {
                out.println("disconnected");
            }

            sock.close();
            System.out.println("Done with client " + sock.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
