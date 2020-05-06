/**-----------------------------------------------------------------
1. Henry Zhou / Date:

2. Using Java 1.8 Update 241

3. Precise command-line compilation examples / instructions:



> javac JokeClientAdmin.java


4. Precise examples / instructions to run this program:


In separate shell windows:

> java JokeServer >> [ServerLogFileName]
> java JokeClient >> [ClientLogFileName]
> java JokeClientAdmin >> [ClientAdminLogFileName]

Console will display the the possible options once upon start up.

This runs across machines, in which case you have to pass the IP address(es) or domain name(s) of
the server to the clients.

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

If you specify a secondary server on the command line, "s" at any time will switch connecting to the secondary server.
Otherwise, you will get a message that you cannot switch to a secondary.

If a secondary server is being used, "s" followed by <enter> switches servers and the user will have to press <enter> again
to make an actual connection to the toggle the server state.

If no secondary server exists, the ClientAdmin will inform the user that no secondary server exists.
After that, the ClientAdmin will attempt to connect to the primary server.

If any server connection fails, a ConnectException will be caught and the user will be informed that a connection has failed.
Possible causes of failure include a server that has not been started or a firewall/security blocking a connection.


----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

/**The JokeClientAdmin class that connects to the Server to toggle the server state
 * */
public class JokeClientAdmin {
	static String[] serverNames = new String[2];
	static int base_server_port = 5050;
	static int secondary_mode = 0;  //Am I talking to a secondary server?
	static boolean has_secondary = false; //Do I have a secondary server to talk to?
	public static void main (String args[]) throws IOException {
		
		String command;
		//If there are no arguments use default server
		if(args.length < 1) serverNames[0] = "localhost";
		
		//If there is at least one argument, set the primary server
		else serverNames[0] = args[0];
		System.out.println("Server one: "+ serverNames[0] +", port "+(base_server_port+0)+".");
		
		/**If there are two arguments, we have a secondary server so set that server up as well.
		 * Also make note of the fact that we have a secondary server.
		 */
		if(args.length == 2) {
			serverNames[1]= args[1];
			has_secondary = true;
			System.out.println("Server two: "+ serverNames[1] +", port "+(base_server_port+1)+".");
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("");
		System.out.println(
				"Type <enter> to 'connect', "
				+ "'quit' to exit, "
				+ "or 's' to switch servers."
				+ "(if a secondary server exists) "
				);
		System.out.println("");
		
		
		do {
			command = in.readLine();
			if(command.equals("quit")) {//User wants to quit
				return;
			}
			if(command.equals("s")){//User wants to switch servers.
				if(has_secondary) {//We have a secondary server
					int new_mode = (secondary_mode+1)%2;
					System.out.println("Now communicating with: " +  serverNames[new_mode] + ", port " +(base_server_port+new_mode) );
					secondary_mode = new_mode;
					continue; //After switching servers, user will have to press enter again to toggle the server.
				}
				else {
					/**If we don't have a secondary server,
					 * we will just connect to the primary server
					 * after informing the user that no secondary exists*/
				
					System.out.println("No secondary server being used");
					System.out.println("Using primary server");
				}
			}
			toggleServerState(serverNames[secondary_mode],secondary_mode); //Delegate the server connection to helper function.
			
			
		}while(true);
	}
	
	
	/**Simplified server log that simply prints a provided message to the console along with a time stamp for tracking.
	*/
	public static void log(String message) {
		Date d = new Date();

		System.out.println(d.toString() + "\nClient Admin Log: " + message +"\n"); 
	}


	/**
	 * The toggleServerState method takes care of all the communication with the server.
	 * We pass in a serverName and whether we are communicating with a secondary or primary.
	 * */
	static void toggleServerState(String serverName, int mode) {
		 Socket clientAdminConnection;
		 BufferedReader fromServer;
		 PrintStream toServer;
		 String newState;
		 try {
			clientAdminConnection = new Socket(serverName,base_server_port + mode);
			fromServer = new BufferedReader(new InputStreamReader(clientAdminConnection.getInputStream()));
			toServer = new PrintStream(clientAdminConnection.getOutputStream());
			toServer.println("Toggle");toServer.flush(); //Tell the server we want to toggle the server state.
			newState = fromServer.readLine(); //Response from the server with the new state.
			log("The Joke Server at " + serverName + ", port " + (base_server_port+mode) + " responded with: " + newState);
			clientAdminConnection.close();
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException ioe) {
			if(ioe instanceof ConnectException) {log(
					"You are either connecting to a non-existent server and port or the server is refusing your connection. Server is " + serverName+ " and port is " + (base_server_port+mode)
					);
			//ConnectException happens when the server refuses a connection or when the server has not started yet.
				} 
			
			ioe.printStackTrace();
		}
		
		
	}
}
