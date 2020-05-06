import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

//Copy and paste into JokeServer.java

/**ConnectionState class has all functionality for keeping track of the state of the conversations.
 * This was implemented in a separate file and then merged with the JokeServer file to make development less daunting.
 * */
class ConnectionState{
	static HashMap<UUID,ConnectionState> stateInfo = new HashMap<UUID, ConnectionState>();
	
	
	List <Integer> jokeIndex; //Index into constant joke array
	int jokeOffSet; //How many jokes have I cycled through already?
	List <Integer> proverbIndex; //Index into constant proverb array
	int proverbOffSet; //How many proverbs have I cycled through already?
	UUID requestID;
	
	
	/**Start tracking a new conversation by generating a UUID which will be returned to the client and keep track of the state
	 * for said conversation.
	 * */
	public static UUID addConnection() {//Add a new connection/conversation to be tracked.
		UUID addedID = UUID.randomUUID();
		stateInfo.put(addedID,  new ConnectionState(addedID));
		return addedID;
	}
	
	/**Get the information/state data structure for the connection with UUID req
	 * */
	public static ConnectionState getConnection(UUID req) {//Allows us to retrieve an old connection/conversation
		return stateInfo.get(req);
	}
	ConnectionState(UUID id){
		
		
		
		
		jokeOffSet = 0; //Initially, I have not cycled through any jokes.
		proverbOffSet = 0;//Initially, I have not cycled through any proverbs.
		shuffleJokes(); //Scramble jokes
		shuffleProverbs(); //Scramble proverbs
		requestID = id; //Keep track of the UUID for this conversation
		
		
		
		
	}
	
	/**
	 * Rather than implementing randomization of data from scratch, I let Java Collections do the work for me.
	 * I keep a list of indexes into a constant array of jokes and proverbs.
	 * I simply reorder the indexes to randomize jokes and proverbs and make note of when I reach the end of the list.
	 * */
	private void shuffleJokes() {
		jokeIndex = Arrays.asList(new Integer[] {0,1,2,3});
		Collections.shuffle(jokeIndex);
		JokeServer.log("User: " + requestID + "\nJokes for were shuffled to have the following order (0 = JA, 1 = JB, 2 = JC, 3 = JD): " + jokeIndex);
	}
	private void shuffleProverbs() {
		proverbIndex = Arrays.asList(new Integer[] {0,1,2,3});
		Collections.shuffle(proverbIndex);
		JokeServer.log("User: " + requestID +"\nProverbs were shuffled to have the following order (0 = PA, 1 = PB, 2 = PC, 3 = PD): " + proverbIndex);
	}
	
	
	
	
	/**Get the joke index for the current connection (called from the JokeWorker accordingly)
	 * */
	public int getJokeIndex() {
		if(jokeOffSet == 3) {//Cycled through 3 jokes.  I'm delivering the 4th one.  Time to re=randomize
			int retIndex = 3;
			jokeOffSet = 0;
			int retVal = jokeIndex.get(retIndex);
			shuffleJokes();
			JokeServer.log("User: " + requestID +"\nJOKE CYCLE COMPLETED"); //Jokes have been cycled through.
			return retVal;
			
		}
		return jokeIndex.get(jokeOffSet++);
	}
	
	/**Get the proverb index for the current connection (called from the JokeWorker accordingly)
	 * */
	public int getProverbIndex() {
		if(proverbOffSet == 3) {//Cycled through 3 proverbs.  I'm delivering the 4th one.  Time to re-randomize
			int retIndex = 3;
			proverbOffSet = 0;
			int retVal = proverbIndex.get(retIndex);
			shuffleProverbs();
			JokeServer.log("User: " + requestID +"\nPROVERB CYCLE COMPLETED"); //Proverbs have been cycled through.
			return retVal;
			
		}
		return proverbIndex.get(proverbOffSet++);
	}
	
}


