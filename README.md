3. Compile
We compile all sources into the out/ directory:

# Remove any previous build
rm -rf out

# Create output directory
mkdir out

# Compile server and client
javac -d out \
    src/main/java/server/Server.java \
    src/main/java/server/ClientHandler.java \
    src/main/java/client/Client.java

4. Run the Server
Open Terminal #1 and start the server on your chosen port (e.g. 7060):

java -cp out server.Server 7060

You should see: Server listening on port 7060
5. Run Clients
Open one or more additional terminals (simulating separate machines) to launch clients:

# In Terminal #2:

java -cp out client.Client <server-ip> 7060

# In Terminal #3 (or more):

java -cp out client.Client <server-ip> 7060
Replace <server-ip> with the IP address of the machine running the server (e.g. 127.0.0.1 for localhost).

Each client will:

Connect and print “Hello!”

Prompt you to type SEND

Download 10 BMPs in a random order

Print download progress and an RTT measurement

Exit cleanly after printing Server: disconnected

6. Cleaning Up Downloaded Files
To remove all downloaded BMPs:

rm downloads/*.bmp
