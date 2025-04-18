// src/main/java/client/Client.java
package client;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Client {
    public static final String DOWNLOAD_DIR = "downloads" + File.separator;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java client.Client <serverIP> <port>");
            return;
        }
        String serverIP = args[0];
        int port = Integer.parseInt(args[1]);

        // ensure download directory exists
        new File(DOWNLOAD_DIR).mkdirs();

        try (Socket sock = new Socket(serverIP, port);
             // Single stream for reading both lines and raw bytes:
             BufferedInputStream inRaw = new BufferedInputStream(sock.getInputStream());
             PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            // helper to read a line terminated by '\n'
            final BufferedInputStream in = inRaw;
            String greeting = readLine(in);
            System.out.println("Server: " + greeting);

            System.out.print("Type SEND (or bye): ");
            String cmd = scanner.nextLine().trim();
            if ("bye".equalsIgnoreCase(cmd)) {
                out.println("bye");
                System.out.println("Server: " + readLine(in));
                return;
            }
            if (!"SEND".equalsIgnoreCase(cmd)) {
                System.out.println("Invalid command");
                return;
            }

            // generate random permutation 1..10
            List<Integer> seq = IntStream.rangeClosed(1,10).boxed().collect(Collectors.toList());
            Collections.shuffle(seq);
            String seqLine = seq.stream().map(String::valueOf).collect(Collectors.joining(","));

            // send request and start timing
            long start = System.nanoTime();
            out.println("SEND");
            out.println(seqLine);

            int filesReceived = 0;
            while (filesReceived < seq.size()) {
                String header = readLine(in);
                if (header == null) throw new IOException("Connection closed");
                if (!header.startsWith("FOUND")) {
                    System.out.println("Server: " + header);
                    continue;
                }
                // header: FOUND <size> <name>
                String[] parts = header.split(" ");
                long size = Long.parseLong(parts[1]);
                String name = parts[2];

                // receive raw bytes
                try (FileOutputStream fos = new FileOutputStream(DOWNLOAD_DIR + name)) {
                    byte[] buf = new byte[4096];
                    long remaining = size;
                    while (remaining > 0) {
                        int toRead = (int)Math.min(buf.length, remaining);
                        int r = in.read(buf, 0, toRead);
                        if (r < 0) throw new EOFException();
                        fos.write(buf, 0, r);
                        remaining -= r;
                    }
                }
                System.out.println("Downloaded " + name);
                filesReceived++;
            }

            long end = System.nanoTime();
            System.out.println("RTT = " + ((end-start)/1_000_000) + " ms");

            // graceful shutdown
            out.println("bye");
            System.out.println("Server: " + readLine(in));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Read a line (up to \\n) from the InputStream, returning trimmed (no \\r, no \\n). */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') buf.write(b);
        }
        if (b == -1 && buf.size() == 0) return null;
        return buf.toString("UTF-8");
    }
}
