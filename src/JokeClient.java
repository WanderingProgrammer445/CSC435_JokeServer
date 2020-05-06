/**-----------------------------------------------------------------
1. Henry Zhou / Date:

2. Using Java 1.8 Update 241

3. Precise command-line compilation examples / instructions:



> javac JokeClient.java


4. Precise examples / instructions to run this program:


In separate shell windows:
On a Windows Machine:
> java JokeServer 
> java JokeClient 
> java JokeClientAdmin 

Console with display potential user input options once upon program start up.
JokeClient will prompt for name first which cannot be blank.

If a secondary server is being used, "s" cannot be a name
and will instead prompt the Client and ClientAdmin to switch servers.

You can quit the Client and ClientAdmin at any time by typing quit.


> java JokeClient
> java JokeClientAdmin

This runs across machines, in which case you have to pass the IP address of
the server (two IP addresses if you want to connect to a secondary server) to the clients.

> java JokeClient <JOKE_SERVER_ADDRESS>
> java JokeClientAdmin <JOKE_SERVER_ADDRESS>

> java JokeClient <PRIMARY_JOKE_SERVER_ADDRESS> <SECONDARY_JOKE_SERVER_ADDRESS>
> java JokeClientAdmin <PRIMARY_JOKE_SERVER_ADDRESS> <SECONDARY_JOKE_SERVER_ADDRESS>

5. List of files needed for running the program.


  
 a. JokeServer.java
 b. JokeClient.java
 c. JokeClientAdmin.java

5. Notes:

In theory, this is not the most secure implementation.
If a hacker were to theoretically obtain the UUID for a connection, they could spoof the server.

I do not explicitly communicate to the client if a server is a primary or secondary.
Instead, I simply let the client determine by port conventions and what mode it is in when making a connection.
This means that the client can only communicate to one server per connection.

For the client, if a secondary server exists, the username cannot be "s" as that is reserved for switching servers.
Otherwise, if no secondary server exists, the username can be "s" and we tell the user that no secondary server exists.

If you specify a secondary server on the command line, "s" at any time will switch connecting to the secondary server.
Otherwise, you will get a message that you cannot switch to a secondary.

More precisely... when entering a username, if you enter "s" and there is no secondary server, your username will be "s".
Otherwise "s" will switch the server you are connecting to and you will be re-prompted for a name.

In the main loop, if a secondary server is being used, typing "s" and <enter> will switch servers and not connect to the server.
In the above scenario, the user will have to press <enter> a second time to connect to the server and get a joke.


----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;
/**JokeClient class which communicates with the server
 * */
public class JokeClient {
	static String[] serverNames = new String[2];  //serverName[0] refers to the primary server name and serverName[1] is secondary.
	static String name = "";
	static int base_server_port = 4545; //Base Joke Server Port that client connects to.
	static ConnectionInfo [] connections = new ConnectionInfo[2];
	static int secondary_mode = 0;  //Am I talking to a secondary server?
	static boolean has_secondary = false;  //Does this client have a secondary server it can talk to?
	
	//Simple logging method that logs a date and message;
	public static void log(String message) {
		Date d = new Date();

		System.out.println(d.toString() + "\nClient Log: " + message+"\n"); 
	}
	
	//Entry point into the JokeClient application
	public static void main (String args[]) throws IOException {
		
		String command;
		
		if(args.length < 1) serverNames[0] = "localhost";  //default host
		else serverNames[0] = args[0];  //If there is at least one argument, the first argument is the primary server.
		
		
		connections[0] = new ConnectionInfo(); //Set up a connection information structure for the primary server connection.
		
		System.out.println("Server one: "+ serverNames[0] +", port "+(base_server_port+0)+".");
		
		if(args.length == 2) {//If there are two arguments, set up the secondary server and make note of the fact that we have a secondary server.
			serverNames[1] = args[1];
			has_secondary = true;
			connections[1] = new ConnectionInfo();
			System.out.println("Server two: "+ serverNames[1] +", port "+(base_server_port+1)+".");
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		do {
		System.out.println("Please enter a name.");

		name = in.readLine().trim();
		if(name.equals("s")){//User seemingly wants to switch servers
			if(has_secondary) {//If there is a secondary, "s" will be used strictly to switch servers.
				int new_mode = (secondary_mode+1)%2;//Calculates the new mode we are going into.
				System.out.println("Now communicating with: " +  serverNames[new_mode] + ", port " +(base_server_port+new_mode) );
				secondary_mode = new_mode;
			}
			else {//If no secondary server is being used, we will allow the user to have a name of "s"
				System.out.println("No secondary server being used");
				System.out.println("Your name is now 's'.");
				break;
			}
		}
		}while(has_secondary && name.equals("s"));//If secondary server is being used, name cannot be "s".
		
		if(name.equals("quit")) {//User wants to quit.
			return;
		}
		
		while(name.equals("")) {//User cannot enter a blank name
			System.out.println("Please enter a name. (You entered a blank name)");
			name = in.readLine().trim();
		}
		
		System.out.println(
				"Type <enter> to 'connect', "
				+ "'quit' to exit, "
				+ "or 's' to switch servers."
				+ "(if a secondary server exists) "
				);
		System.out.println("");
		do {
			command = in.readLine().trim();
			if(command.equals("quit")) {//User wants to quit
				return;
				}
			if(command.equals("s")) {//User wants to switch servers?
				if(has_secondary) {//Do we have a secondary server?
					int new_mode = (secondary_mode+1)%2;
					System.out.println("Now communicating with: " +  serverNames[new_mode] + ", port " +(base_server_port+new_mode));
					secondary_mode = new_mode;
					continue;
				}
				else {
					/**If we don't have a secondary server,
					 * we will just connect to the primary server
					 * after informing the user that no secondary exists*/
					
					System.out.println("No secondary server being used");
					System.out.println("Using primary server");
				}
			}
			getServerRequest(connections[secondary_mode],serverNames[secondary_mode], secondary_mode); //Delegate server connection to helper
			
		}while(true);
	}
	
	
	/**The getServerRequest method takes care of all the server connection from the client.
	 * It is passed in a ConnectionInfo object which has the UUID for the connection which starts out as blank.
	 * The server name and mode variable tell us which server we connect to and whether or not we are in secondary mode.
	 * 
	 * */
	static void getServerRequest(ConnectionInfo conn, String serverName, int mode) {
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		
		String headerFromServer;
		String textFromServer;
		String clientOutput;
		try {
			//Create a socket with serverName and port number which in turn creates a connection to the server and port
			sock = new Socket(serverName, base_server_port+mode);
			
			//Sets up the data streams for communication
			fromServer = new BufferedReader(
					new InputStreamReader(
							sock.getInputStream()
							)
					);
			toServer = new PrintStream(sock.getOutputStream());
			
			//Prints to stream and flushes out the buffer to ensure data is sent to the server.
			toServer.println(conn.uuid); toServer.flush();
			String id= fromServer.readLine().trim();
			if (conn.uuid.equals("")) {
				conn.setUUID(id);  //First line of Server Response is a UUID.  If UUID is not set for this conversation, set it for future use.
				log("UUID for the conversation between this client to: " + serverName+ " on port " + (base_server_port+mode) + " is " + id);
			}
			headerFromServer = fromServer.readLine(); //Joke or proverb label
			textFromServer = fromServer.readLine(); //Actual joke
			
			clientOutput = headerFromServer + " " + name +": " + textFromServer; //Client assembles the console output for the user.
			if(mode == 1) {//If we were connecting to a secondary server, we will add <S2> to the console output.
				clientOutput= "<S2> " + clientOutput; 
			}
			System.out.println(clientOutput);
			sock.close();
		}catch(IOException ioe) {
			if(ioe instanceof ConnectException) {log(
					"You are either connecting to a non-existent server and port or the server is refusing your connection. Server is " + serverName+ " and port is " + (base_server_port+mode)
				);
			} 
			ioe.printStackTrace();
			
			}
	}
}

/**
 * The ConnectionInfo class stores the UUID for each connection from the client to the server.
 * */

class ConnectionInfo{
	String uuid;
	ConnectionInfo(){
		uuid = "";
	}
	public void setUUID(String id) {
		uuid = id;
	}
	
}
