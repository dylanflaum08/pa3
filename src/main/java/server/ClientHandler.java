package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket sock;
    public static final String BASE_DIR = "files" + System.getProperty("file.separator");
    private final List<Long> sharedRtts;

    public ClientHandler(Socket sock, List<Long> sharedRtts) {
        this.sock = sock;
        this.sharedRtts = sharedRtts;
    }

    @Override
    public void run() {
        List<Long> rtts = new ArrayList<>();

        try (
            BufferedReader in    = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter  out     = new PrintWriter(sock.getOutputStream(), true);
            OutputStream dataOut = sock.getOutputStream();
        ) {
            out.println("Hello!");

            String cmd;
            while ((cmd = in.readLine()) != null) {
                cmd = cmd.trim();
                if ("bye".equalsIgnoreCase(cmd)) {
                    out.println("disconnected");
                    break;
                }

                if (!cmd.toUpperCase().startsWith("SEND")) {
                    out.println("Please type a different command");
                    continue;
                }

                // extract optional batch size: "SEND" or "SEND N"
                int batch = 1;
                String[] parts = cmd.split("\\s+");
                if (parts.length > 1) {
                    try { batch = Integer.parseInt(parts[1]); }
                    catch (NumberFormatException ignored) { }
                }

                long startTime = System.currentTimeMillis();

                // read the next line if you expect indices, else remove this
                String seqLine = in.readLine();
                if (seqLine == null) break;

                for (String part : seqLine.split(",")) {
                    int idx;
                    try {
                        idx = Integer.parseInt(part.trim());
                    } catch (NumberFormatException e) {
                        out.println("Invalid index: " + part);
                        continue;
                    }

                    String fileName = String.format("sample%02d.bmp", idx);
                    File f = new File(BASE_DIR + fileName);
                    if (!f.exists() || !f.isFile()) {
                        out.println("File not found: " + fileName);
                        continue;
                    }

                    long size = f.length();
                    out.println("FOUND " + size + " " + fileName);

                    try (FileInputStream fis = new FileInputStream(f)) {
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = fis.read(buf)) != -1) {
                            dataOut.write(buf, 0, read);
                        }
                    }
                }

                dataOut.flush();

                // record RTT
                long rtt = System.currentTimeMillis() - startTime;
                sharedRtts.add(rtt);

                // after 5 sends, compute & report stats
                if (sharedRtts.size() >= 2) {
                    List<Long> snapshot = new ArrayList<>(sharedRtts);
                    sharedRtts.clear();
            
                    Collections.sort(snapshot);
                    long min = snapshot.get(0);
                    long max = snapshot.get(snapshot.size() - 1);
                    double mean = snapshot.stream().mapToLong(x -> x).average().orElse(0.0);
                    double median = snapshot.get(snapshot.size() / 2);
                    double var = snapshot.stream()
                                         .mapToDouble(x -> (x - mean)*(x - mean))
                                         .sum() / snapshot.size();
                    double stddev = Math.sqrt(var);
            
                    out.println("STATS_START");
                    out.println(String.format(
                        "STATS min=%d ms, max=%d ms, mean=%.2f ms, median=%.2f ms, stddev=%.2f ms",
                        min, max, mean, median, stddev));
                    out.println("STATS_END");
                }else{
                    out.println("END");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { sock.close(); } catch (IOException ignored) {}
            System.out.println("Done with client " + sock.getInetAddress());
        }
    }
}
