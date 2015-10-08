import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import connectK.BoardModel;
import connectK.CKPlayer;


/** CLASS: DrakenAI ****************************************************************************
 * 
 * 
 * @author Pablo Kang (UCI ID: 58842064)
 ***********************************************************************************************/
public class DrakenAI extends CKPlayer 
{
	// shows how many spaces of each column are used
	private int width;
	private int height;
	private int kLength;
	private byte enemy;
	private int depth;
	private boolean testQui;
	private long moveDeadline;
	
	private static final boolean PRUNE = true;
	private static final byte NOTGAMEOVER = -1;

	private PriorityQueue<Move> moveQueue;
	private HashMap<BoardModel, Double> boardMap;

	
	/** CONSTRUCTOR
	 * 
	 * @param player
	 * @param state
	 */
	public DrakenAI(byte player, BoardModel state) 
	{
		super(player, state);
		teamName = "DrakenAI";
		
		// Initialize state attributes
		width = state.getWidth();
		height = state.getHeight();
		kLength = state.getkLength();
		
		// Identify the opponent
		this.enemy = (byte)((player == 1) ? 2 : 1);		
	}

	
	/** getMove:
	 * Return a Point on the board to be used as this AI player's move.
	 * Uses minimax algorithm to choose best Point in an attempt to win.
	 * Passes to the time-limited version of this function with a default time. 
	 * @param state
	 * @return Point
	 */
	@Override
	public Point getMove(BoardModel state) 
	{
		return getMove(state, 5000);
	}
	
	
	/** getMove:
	 * Return a Point on the board to be used as this AI player's move.
	 * Uses minimax algorithm to choose best Point in an attempt to win.
	 * Passes to the time-limited version of this function with a default time. 
	 * @param state
	 * @param timeLimit
	 * @return Point
	 */
	@Override
	public Point getMove(BoardModel state, int timelimit)
	{
		// Set the deadline
		moveDeadline = System.currentTimeMillis() + (long) timelimit - 100;
		
		// If it's the first move, pick the middle spot.
		if(state.lastMove == null){
			if(state.gravityEnabled())
				return new Point(state.width/2, 0);
			else
				return new Point(state.width/2, state.height/2);
		}
		
		// Initialize the hash map of board states
		boardMap = new HashMap<BoardModel, Double>();

		// Retreive list of all moves
		ArrayList<Move> moves = generateMoves(state);

		// Initialize the best move found so far
		Move bestMove = new Move(new Point(), Double.NEGATIVE_INFINITY);		
		
		// ITERATIVE DEEPENING SEARCH:
		for(int depthLimit = 0; System.currentTimeMillis() < moveDeadline; depthLimit++)
		{
			// Initialize queue of moves and reset depth
			moveQueue = new PriorityQueue<Move>(11, new MoveComparator());
			depth = 0;
			
			// Initialize alpha and beta
			double alpha = Double.NEGATIVE_INFINITY;
			double beta = Double.POSITIVE_INFINITY;
			
			// If map has no move nodes, search through all moves iteratively.
			// Else, go through each move node in order of priority.
			// Either way, do a Depth First Search:
			if(boardMap.get(state.clone().placePiece(moves.get(0), player))== null) {
				for(Move move: moves)
				{
					depth = 0;
					move.setScore(minMove(state.clone().placePiece(move, player), depthLimit, alpha, beta));
					if(move.getScore() >= bestMove.getScore())
						bestMove = move;
				}
			}
			else {
				for(Move move: moves) {
					moveQueue.add(new Move(move, boardMap.get(state.clone().placePiece(move,player))));
				}
				while(!moveQueue.isEmpty())
				{
					Move move = moveQueue.remove();
					depth = 0;
					move.setScore(minMove(state.clone().placePiece(move, player), depthLimit, alpha, beta));
					if(move.getScore() >= bestMove.getScore())
						bestMove = move;
				}
			}
		}		

		// Empty the move storage
		moveQueue.clear();
		moves.clear();

		return bestMove;
	}
	
	
	/** minMove:
	 * Calulate the min score of the current board state.
	 * @param state
	 * @param depthLimit
	 * @param alpha
	 * @param beta
	 * @return
	 */
	private double minMove(BoardModel state, int depthLimit, double alpha, double beta)
	{
		depth += 1;

		// Check if it's time to pack up. If so, store the current state in the boardMap with its eval score.
		if(System.currentTimeMillis() > moveDeadline || depth >= depthLimit || state.winner() != NOTGAMEOVER) {
			double eval = eval(state, player);
			boardMap.put(state, eval);
			return eval;
		}

		// Initialize the move storages (List of possible moves, and priority queue of moves to attempt)
		ArrayList<Move> moves = generateMoves(state);
		PriorityQueue<Move> moveQ = new PriorityQueue<Move>(11, new MoveComparator());
		
		// Initialize min's bestScore to +infinity and the new board state after attempting moves
		double bestScore = Double.POSITIVE_INFINITY;
		boolean usePriorityBoarding = false; // Get it? Priority BOARDing? ;D
		BoardModel newState = null;
		int currentDepth = depth;

		// Check if boardMap has explored child states
		if(boardMap.containsKey(state.clone().placePiece(moves.get(0), enemy)))
			usePriorityBoarding = true;

		// Child state nodes of current state have been explored, use Priority Queue
		if(usePriorityBoarding) {
			for(Move move: moves) {
				newState = state.clone();
				newState.placePiece(move, enemy);
				moveQ.add(new Move(move, boardMap.get(newState)));
			}
			// Pop through the priority queue and use Depth First Search on each state node
			while(!moveQ.isEmpty()) {
				depth = currentDepth;
				newState = state.clone();
				newState.placePiece(moveQ.remove(), enemy);
				double maxMove = maxMove(newState, depthLimit, alpha, beta);

				if(maxMove < bestScore) {
					bestScore = maxMove;
					beta = bestScore;
					if(PRUNE && (alpha >= beta))
						break;
				}
			}
		}
		// Child states have not been explored, test all moves in any order
		else {
			for(Point move: moves) {
				depth = currentDepth;
				newState = state.clone();
				newState.placePiece(move, enemy);
				double maxMove = maxMove(newState, depthLimit, alpha, beta);

				if(maxMove < bestScore) {
					bestScore = maxMove;
					beta = bestScore;
					if(PRUNE && (alpha >= beta))
						break;
				}
			}
		}
		boardMap.put(state, bestScore);
		
		// Empty the move storage
		moveQ.clear();
		moves.clear();
		
		return bestScore;
	}

	
	/**
	 * 
	 * @param state
	 * @param depthLimit
	 * @param alpha
	 * @param beta
	 * @return
	 */
	private double maxMove(BoardModel state, int depthLimit, double alpha, double beta)
	{
		depth += 1;

		// Check if it's time to pack up. If so, store the current state in the boardMap with its eval score.
		if(System.currentTimeMillis() > moveDeadline || depth >= depthLimit || state.winner() != NOTGAMEOVER) {
			double eval = eval(state, player);
			boardMap.put(state, eval);
			return eval;
		}
		
		// Initialize the move storages (List of possible moves, and priority queue of moves to attempt)
		ArrayList<Move> moves = generateMoves(state);
		PriorityQueue<Move> moveQ = new PriorityQueue<Move>(11, new MoveComparator());
		
		// Initialize min's bestScore to +infinity and the new board state after attempting moves
		double bestScore = Double.NEGATIVE_INFINITY;
		boolean usePriorityBoarding = false;
		BoardModel newState = null;
		int currentDepth = depth;

		// Check if boardMap has explored child states
		if(boardMap.containsKey(state.clone().placePiece(moves.get(0), player)))
			usePriorityBoarding = true;

		// Child state nodes of current state have been explored, use Priority Queue
		if(usePriorityBoarding) {
			for(Point move: moves) {
				newState = state.clone();
				newState.placePiece(move, player);
				moveQ.add(new Move(move, boardMap.get(newState)));
			}
			// Pop through the priority queue and use Depth First Search on each state node
			while(!moveQ.isEmpty()) {
				depth = currentDepth;
				newState = state.clone();
				Move move = moveQ.remove();
				newState.placePiece(move, player);
				double minMove = minMove(newState, depthLimit, alpha, beta);

				if(minMove > bestScore) {
					bestScore = minMove;
					alpha = bestScore;
					if(PRUNE && (alpha >= beta))
						break;
				}
			}
		}

		else {
			for(Point move: moves) {
				depth = currentDepth;
				newState = state.clone();
				newState.placePiece(move, player);
				double minMove = minMove(newState, depthLimit, alpha, beta);

				// Replace alpha and/or prune if necessary
				if(minMove > bestScore) {
					bestScore = minMove;
					alpha = bestScore;
					if(PRUNE && (alpha >= beta))
						break;
				}
			}
		}

		// record this state with the best value found
		boardMap.put(state, bestScore);

		// Empty the move storage
		moveQ.clear();
		moves.clear();
		
		return bestScore;
	}

	
	/** eval:
	 * Go through every possible line on the board and score them, returning the total
	 * @param state
	 * @param p1
	 * @return
	 */
	private double eval(BoardModel state, byte p1)
	{
		// Check if current state has any wins for the player or enemy
		if(state.winner() == this.player) {
			return Double.POSITIVE_INFINITY;
		}
		else if (state.winner() == this.enemy) {
			return Double.NEGATIVE_INFINITY;
		}
		
		// Initialize variables
		byte p2 = (byte)((p1 == 1) ? 2 : 1);
		int p1Count = 0;
		int p2Count = 0;
		double p1Score = 0.0;
		double p2Score = 0.0;
		int kLim = kLength - 1;
		testQui = false;
		
		// CHECK ALL POSSIBLE LINES
		// Horizontals
		for(int i = 0; i < width - kLim; i++) {	
			for(int j = 0; j < height; j++) {
				
				// Count pieces within kLength spots to the right of (i,j)
				for(int k = 0; k < kLength; k++) {
					if(state.pieces[i + k][j] == p1)
						p1Count++;
					else if(state.pieces[i + k][j] == p2)
						p2Count++;
				}

				p1Score = evalLine(p1, p1Count, p2Count, p1Score);
				p2Score = evalLine(p2, p2Count, p1Count, p2Score);
				// Reset counters
				p1Count = 0;
				p2Count = 0;
			}
		}

		// Verticals
		for(int i = 0; i < width; i++) {	
			for(int j = 0; j < height - kLim; j++) {
				
				// Count pieces within kLength spots above (i,j)
				for(int k = 0; k < kLength; k++) {
					if(state.pieces[i][j + k] == p1)
						p1Count++;
					else if(state.pieces[i][j + k] == p2)
						p2Count++;
				}

				p1Score = evalLine(p1, p1Count, p2Count, p1Score);
				p2Score = evalLine(p2, p2Count, p1Count, p2Score);
				// Reset counters
				p1Count = 0;
				p2Count = 0;
			}
		}

		// Diagonals: /
		for(int i = 0; i < width - kLim; i++) {	
			for(int j = 0; j < height - kLim; j++) {
				
				// Count pieces above and to the right
				for(int k = 0; k < kLength; k++) {
					if(state.pieces[i + k][j + k] == p1)
						p1Count++;
					else if(state.pieces[i + k][j + k] == p2)
						p2Count++;
				}

				p1Score = evalLine(p1, p1Count, p2Count, p1Score);
				p2Score = evalLine(p2, p2Count, p1Count, p2Score);
				// Reset counters
				p1Count = 0;
				p2Count = 0;
			}
		}

		// Diagonals: \
		for(int i = kLim; i < width; i++) {	
			for(int j = 0; j < height - kLim; j++) {
				
				// Count pieces above and to the left
				for(int k = 0; k < kLength; k++) {
					if(state.pieces[i - k][j + k] == p1)
						p1Count++;

					else if(state.pieces[i - k][j + k] == p2)
						p2Count++;
				}

				p1Score = evalLine(p1, p1Count, p2Count, p1Score);
				p2Score = evalLine(p2, p2Count, p1Count, p2Score);
				// Reset counters
				p1Count = 0;
				p2Count = 0;
			}
		}

		// Return the difference between p1's and p2's scores
		return p1Score - p2Score;
	}
	
	
	/** evalLine:
	 * Evaluates a line and calculates its score. Executes the Quiescence test. 
	 * @param p
	 * @param pCount
	 * @param eCount
	 * @param pScore
	 * @return
	 */
	private double evalLine(byte p, int pCount, int eCount, double pScore)
	{
		// Only p1 has pieces, increase p1Score
		if((pCount > 0) && (eCount == 0)) {
			pScore += applyWeight(pCount);
			testQui = quiescence(p, pCount);
		}
		return pScore;
	}
	
	
	/** applyWeight:
	 * Calculates a weight to apply to a line's score based on the number of pieces in that line
	 * @param n
	 * @return
	 */
	private double applyWeight(int n)
	{
		if(n == kLength)
			return Double.MAX_VALUE;
		
		return n * n * n;
	}

	
	/** quiescence:
	 * Tests if the line is "quiet" or not
	 * @param p
	 * @param n
	 * @return
	 */
	private boolean quiescence(byte p, int n)
	{
		if(testQui)
			return true;
		
		if(p == enemy && n == kLength - 1)
			return true;

		return false;
	}

	
	/** generateMoves:
	 * Find all valid next moves on the current board state.
	 * Return List of moves in Move objects or empty list if Game Over.
	 * @param state
	 * @return List<Move>
	 */
	private ArrayList<Move> generateMoves(BoardModel state) 
	{
		ArrayList<Move> nextMoves = new ArrayList<Move>(); // allocate List

		// If gameover, i.e., no next move
		if (state.winner() != -1) {
			return nextMoves;   // return empty list
		}

		if(state.gravityEnabled()) {
			for (int col = 0; col < width; col++) {
				for (int row = 0; row < state.height; row++) {
					if (state.getSpace(col, row) == 0) {
						nextMoves.add(new Move(col, row, Integer.MIN_VALUE));
						break;
					}
				}
			}
		}
		else{
			// Search for empty cells and add to the List
			for (int row = 0; row < height; ++row) {
				for (int col = 0; col < width; ++col) {
					if (state.getSpace(col, row) == 0) {
						nextMoves.add(new Move(col, row, Integer.MIN_VALUE));
					}
				}
			}
		}
		return nextMoves;
	}
}
