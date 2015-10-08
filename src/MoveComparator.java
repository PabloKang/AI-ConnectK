import java.util.Comparator;

	/** MoveComparator
	 * Compares moves in reverse priority
	 * @author Big
	 */
	class MoveComparator implements Comparator<Move>
	{
		public int compare(Move o1, Move o2){ 
			return 0 - o1.compareTo(o2);
		}
	}