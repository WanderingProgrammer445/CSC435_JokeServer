import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.UUID;
//Paste into JokeServer file
/**
 * Joke Worker class which is responsible for the actual communication with the client after the JokeServer initiates the connection.
 * */
class JokeWorker extends Thread{
	Socket sock;
	JokeWorker(Socket s){
		sock = s;
	};
	/**Run function which is the entry point for the JokeWorker class.
	 * */
	public void run() {
		PrintStream out = null;
		BufferedReader in = null;
		String retVal = "";
		try {
			//Allow reading input from the client through buffered reader
			in = new BufferedReader(
					new InputStreamReader(
							sock.getInputStream()
							)
					);
			//Allow writing responses to the client through a print stream
			out = new PrintStream(sock.getOutputStream());
			
			try {
				int index = 0;
				UUID req;
				String uuid = in.readLine().trim(); //Parse the UUID
				if (uuid.equals("")) {//If the user has no UUID yet... generate one.
					req = ConnectionState.addConnection();
				}
				else {//If the user has a UUID... convert UUID from String form to UUID form.
					req= UUID.fromString(uuid);
					
				}
				retVal += req +"\n";//Add UUID to the Server Response.
				
				if(0==JokeServer.mode) {// 0 = JOKE MODE, 1 = PROVERB MODE
					
					index = ConnectionState.getConnection(req).getJokeIndex();
					//Joke mode
					retVal+=JokeServer.jokeLabels[index] + "\n"; //Add joke label to Server Response
					
					retVal+=JokeServer.jokes[index]; //Add joke to Server Response
					
					
				}
				else {
					index = ConnectionState.getConnection(req).getProverbIndex();
					//Proverb mode
					retVal+=JokeServer.proverbLabels[index] + "\n"; //Add proverb label to Server Response
					
					retVal+=JokeServer.proverbs[index];  //Add proverb to Server Response
					
				}
				JokeServer.log("Server Response To Client:\n" + retVal);//Log the Server Response.
				out.println(retVal);
				
			}catch(IOException ioe) {ioe.printStackTrace();}
			sock.close();
	}catch(IOException ioe) {ioe.printStackTrace();}
	}
}

