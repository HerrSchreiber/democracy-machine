import java.util.*;
import java.io.*;

/**
 * Counts the votes in the argument csv file.
 */
public class VoteCounter {

	/**
	 * Counts the election
	 * @param args Name of csv file
	 */
	public static void main(String[] args) {

		int totalVotes = 0;
		
		// print usage if called incorrectly
		if (args.length != 1) {
			System.out.println("\nSet the CSV with the votes in it as the only argument when running this program\n");
			System.exit(1);
		}
			
		Hashtable<String, Boolean> hasVoted = new Hashtable<String, Boolean>();
		Hashtable<Choice, LinkedList<Vote>> voteTable = new Hashtable<Choice, LinkedList<Vote>>();
		
		// initialize vote table with empty LinkedList<Vote>s
		for (Choice c : Choice.values()) {
			voteTable.put(c, new LinkedList<Vote>());
		}

		// File IO stuff
		try {
			
			// Store all student IDs as keys in hasVoted
			Scanner studentIDReader = new Scanner(new File("studentids.csv"));
			studentIDReader.nextLine();
			while (studentIDReader.hasNext()) {
				String temp = studentIDReader.nextLine();
				String idNo = temp.split(",")[0];
				hasVoted.put(idNo, false);
			}

			// Create votes from csv in args
			Scanner fileReader = new Scanner(new File(args[0]));
			fileReader.nextLine();			
			while (fileReader.hasNext()) {
				String temp = fileReader.nextLine();
				if (temp.equals(",,,,")) continue;  // throw away empty spreadsheet rows
				String[] options = temp.split(",");
				if(!hasVoted.containsKey(options[1]) || hasVoted.get(options[1]) == true) continue; // if student ID is invalid or they've already voted, throw it away
				hasVoted.put(options[1], true);  // mark ID as hasVoted
				totalVotes++;
				Vote vote = new Vote(options[1], findUniqueChoices(Arrays.copyOfRange(options, 2, 5))); // Create vote from ID and array indices 2-4
				System.out.println(vote);
				voteTable.get(vote.getCurrentChoice()).add(vote);  // Add the vote to their first choice's LL in voteTable
			}
		} catch (IOException e) {
			System.out.println("\nFile Doesn't Exist: " + args[0] + "\n");
			System.exit(1);
		}

		
		// Outcome calculation
		try {

			System.out.println("\nInitial Results:\n");
			printCurrentResults(voteTable);

			removeLowestChoice(voteTable);

			printCurrentResults(voteTable);
			while (voteTable.keySet().size() != 1) {  // THERE CAN ONLY BE ONE!
				removeLowestChoice(voteTable);
				printCurrentResults(voteTable);
			}
		} catch (TieResultsException e) {  // but sometimes there can't
			String outcome = "\nTie Between: ";
			for (Choice c: voteTable.keySet()) {
				outcome += c + ", ";
			}
			System.out.println(outcome);
			System.exit(0);
		}
		Choice winner = voteTable.keySet().iterator().next();
		System.out.println(winner + " is the winner!");
		System.out.println("With " + (double)(voteTable.get(winner).size())*100/totalVotes + "% of people having chosen it as one of their 3 options!\n");
	}

	/**
	 * Removes the lowest choice from the voteTable
	 * @param vt The current election results
	 */
	private static void removeLowestChoice(Hashtable<Choice, LinkedList<Vote>> vt) {
		boolean allChoicesEqual = true;
		int lastVoteCount = -1;
		Choice lowestOption = null;
		int lowestOptionVotes = -1;
		for (Choice c : vt.keySet()) {
			if (lastVoteCount != -1 && lastVoteCount != vt.get(c).size()) allChoicesEqual = false;  // no tie on first different vote-count
			if (lowestOptionVotes == -1) {
				lowestOption = c;
				lowestOptionVotes = vt.get(c).size();
			}
			else if (vt.get(c).size() < lowestOptionVotes) {
				lowestOption = c;
				lowestOptionVotes = vt.get(c).size();
			}
			lastVoteCount = vt.get(c).size();
		}
		if (allChoicesEqual) throw new TieResultsException();
		System.out.println("Removing: " + lowestOption + " with " + lowestOptionVotes +" votes\n");
		LinkedList<Vote> orphanedVotes = vt.remove(lowestOption);
		for (Vote v : orphanedVotes) {  // redistribute votes for loser
			try {
				Choice newChoice = v.getNextChoice();
				while (!vt.containsKey(newChoice)) newChoice = v.getNextChoice();
				vt.get(newChoice).add(v);  // this line doesn't run if AllChoicesGoneException is thrown on previous line
			} catch (AllChoicesGoneException e) {}
		}
	}

	/**
	 * Creates an Enum array of unique values from a String array
	 * @param  stringChoices The String version of the choices
	 * @return               An Enum array of the unique Strings
	 */
	private static Choice[] findUniqueChoices(String[] stringChoices) {
		// pull out mandatory first choice
		int size = 1;
		Choice[] choices = new Choice[3];
		choices[0] = Choice.valueOf(stringChoices[0]);
		
		// get the rest
		for (int i = 1; i < stringChoices.length; i++) {
			if (stringChoices[i] == null)continue;  // choices aren't mandatory, can be null
			Choice temp = Choice.valueOf(stringChoices[i]);
			boolean unique = true;
			for (int k = 0; k < size; k++) {
				if (temp == choices[k]) unique = false;
			}
			if (unique) choices[size++] = temp;
		}

		// resize if there are empty values at the end
		if (size != 3) {
			Choice[] tempChoices = new Choice[size];
			for (int i = 0; i < size; i++) {
				tempChoices[i] = choices[i];
			}
			choices = tempChoices;
		}
		return choices;
	}

	/**
	 * Prints out the current results of the vote
	 * @param vt The hashtable of the votes
	 */
	private static void printCurrentResults(Hashtable<Choice, LinkedList<Vote>> vt) {
		for (Choice c : vt.keySet()) {
			System.out.println(c + ": " + vt.get(c).size());
		}
		System.out.println();
	}


	/**
	 * The choices for the vote
	 */
	private static enum Choice {
		Narwhals, Cobras, Swifts, Volts, Isotopes, Scientists, Robots, Geckos, Dingisos;
	}

	/**
	 * Wrapper for each vote
	 */
	private static class Vote {
		private String id;
		private int choiceIndex;  // index of choice array for current favorite choice
		private Choice[] choices;

		/**
		 * Creates a vote from student ID and their vote choices
		 * @param  id      student id number
		 * @param  choices array of vote choices in order of preference
		 */
		public Vote(String id, Choice[] choices) {
			this.id = id;
			this.choices = choices;
			choiceIndex = 0;
		}

		/**
		 * Returns the vote's current choice
		 * @return the vote's current choice
		 */
		public Choice getCurrentChoice() {
			return choices[choiceIndex];
		}

		/**
		 * Increments the current choice and returns it
		 * @return the new current choice
		 */
		public Choice getNextChoice() {
			if (++choiceIndex >= choices.length) throw new AllChoicesGoneException();

			return choices[choiceIndex];
		}

		/**
		 * Returns a String version of the class
		 * @return A String version of the class
		 */
		public String toString() {
			String retVal = id + ":  ";
			for (int i = 0; i < choices.length; i++) {
				retVal += (i + 1) + ": " + choices[i] + ", ";
			}
			return retVal;
		}
	}

	/**
	 * Thrown when a vote can't be transferred any more.
	 */
	private static class AllChoicesGoneException extends RuntimeException {}

	/**
	 * Thrown if the election results are a tie
	 */
	private static class TieResultsException extends RuntimeException {}
}