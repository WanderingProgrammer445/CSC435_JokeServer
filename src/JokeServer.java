/**-----------------------------------------------------------------
1. Henry Zhou / Date:

2. Using Java 1.8 Update 241

3. Precise command-line compilation examples / instructions:



> javac JokeServer.java


4.


In separate shell windows:
On a Windows machine:
> java JokeServer
> java JokeClient
> java JokeClientAdmin

Client and ClientAdmin will display options once upon start up.

This runs across machines, in which case you have to pass either the IP address or domain name of
the server to the clients.

> java JokeServer //Starts the JokeServer as a primary node
> java JokeServer secondary //Starts the JokeServer as a secondary node

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

The Server cannot be stopped aside from Control-C whereas the Client and ClientAdmin can be stopped by typing "quit".
To implement a way to stop the server, I could move the Server Socket code to another thread.
The main thread would then take user input to shutdown.

One edge case that behaves oddly is the case when the client tries to bombard a server that hasn't started yet
with lots of requests.  If this happens and the server starts up in the middle of all this,
the server may throw a null exception and the client may hang.  The only way out of this would be to stop and restart
both the server and client.

----------------------------------------------------------*/


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;


/**The Joke Server which is the starting point for the Server portion of the assignment.
 * */
public class JokeServer {
	
	
	//Simple method to log some messages to the console with a date and time.
	public static void log(String message) {
		Date d = new Date();

		System.out.println(d.toString() + "\nServer Log: " + message);
		System.out.println("");
	}
	
	//Entry point into JokeServer application
	public static void main(String args[]) throws IOException {
		
		int queue_length = 6;
		int base_server_port = 4545;
		int secondary = 0;
		Socket sock;
		
		if(args.length > 0 && args[0].trim().equals("secondary")) {//Found secondary token among command line arguments.
			log("I am a Joke Server and I am running as a secondary");  
			secondary = 1;
		}
		
		ServerSocket servsock = new ServerSocket(base_server_port+secondary, queue_length);
		
		log("Joke Server starting up and listening on port "+(base_server_port+secondary)+".");
		
		//Started the thread for the ClientAdmin connection handler and tell the thread if we are a secondary or primary
		new ClientAdminMaster(secondary).start();  
		
		while(true) {
			sock = servsock.accept();
			new JokeWorker(sock).start(); //Dispatch to the Worker thread.
		}
		
	}
	
	//Array of jokes and labels are constant and I just index into these arrays when I want to send data.
	static final String[] jokes = {
			"What do you call a foodie after 4 years of med school?  A resident macrophage (macrophage = 'big eater')",
			"What are Finance majors so fit and grateful?  They send out a 10Q every quarter and run a 10K every year.",
			"What is a love-struck lawyer's favorite dance?  The courting dance.",
			"What did the pizza delivery guy say when he interrupted a trial?  Order in the court."
			};
	static final String[] jokeLabels = {"JA", "JB", "JC", "JD"};
	static final String[] proverbLabels = {"PA", "PB", "PC", "PD"};
	static final String[] proverbs = {
			"Everyone is free to dream of living in a fantasy world but everyone has to live in the real world.",
			"If you know that you can't achieve your goal, is it worse to fail or worse to never have tried at all?",
			"No one is perfect but that doesn't mean you should purposely try to underachieve.",
			"If money and attractiveness were all that women wanted, Brad Pitt would have all the women in the world.  (He does not)"
			};
	

	
	static int mode = 0; // 0 = JOKE, 1 = PROVERB  (Probably better to use Enum)
	

}

/**Worker Thread for dealing with Client Admin connections.
 * */
class ClientAdminSlave extends Thread{
	Socket sock;
	ClientAdminSlave(Socket s){
		sock = s;
	}
	
	//Entry point into ClientAdmin request worker thread
	public void run() {
		PrintStream out = null;
		BufferedReader in = null;
		
		String clientCommand;
		try {
			//Allow reading input from the client through buffered reader
			in = new BufferedReader(
					new InputStreamReader(
							sock.getInputStream()
							)
					);
			//Allow writing responses to the client through a print stream
			out = new PrintStream(sock.getOutputStream());
			
			clientCommand = in.readLine().trim();
			if(clientCommand.equals("Toggle")) {//ClientAdmin tells us that we should toggle state.
				JokeServer.mode = (JokeServer.mode+1)%2;  //Somewhat hard-coded way of toggling between 0 and 1. 
			}
			
			if(0==JokeServer.mode) {
				JokeServer.log("The mode for this Joke Server is now Joke.");
				out.println("The mode for the JokeServer is Joke."); //Tell the ClientAdmin that we are now in Joke mode.
			}
			else {
				JokeServer.log("The mode for this Joke Server is now Proverb.");
				out.println("The mode for the JokeServer is Proverb."); //Tell the ClientAdmin that we are now in Proverb mode.

			}
			sock.close();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}


/**ClientAdminMaster class takes connections on port 5050 if running on primary mode (secondary = 0).
 * If running in secondary mode (secondary = 1), then ClientAdminMaster takes connections on port 5051.
 * */
class ClientAdminMaster extends Thread{
	
	int secondary; //Am I running as a secondary when getting ClientAdmin requests?  -> Set on startup
	
	ClientAdminMaster(int sec){
		secondary = sec;  //Initialize the master for the ClientAdmin requests.
	}
	
	/**Run method which is the starting point for the ClientAdmin master thread.
	 * */
	public void run(){
		int queue_length = 6;
		int base_server_port = 5050;
		Socket sock;
		ServerSocket servsock;
	try {
	servsock = new ServerSocket(base_server_port+secondary, queue_length); //Start listening for ClientAdmin requests.

		
		JokeServer.log(
				"Joke Client Admin Server starting up and listening on port "+(base_server_port+secondary)+"."
				);
		while(true) {
			sock = servsock.accept();
			new ClientAdminSlave(sock).start();  //Dispatch connection the Slave (Worker thread) for ClientAdmin requests.
		}
		
	}catch(IOException ioe) {ioe.printStackTrace();}
}	
		
	
}







