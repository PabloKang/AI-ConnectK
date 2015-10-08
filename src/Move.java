import java.awt.Point;

/** CLASS: Move ********************************************************************************
 * Defines a single possible move and its alpha score
 * 
 * @author Pablo Kang (UCI ID: 58842064)
 ***********************************************************************************************/
class Move extends Point implements Comparable<Move>
{
	private static final long serialVersionUID = 1L;
	private double score;
	public Move(Point point, double score)
	{
		super(point);
		this.score = score;
	}
	public Move(int col, int row, double s)
	{
		super(col, row);
		this.score = s;
	}
	public double getScore()
	{
		return this.score;
	}
	public void setScore(double s)
	{
		this.score = s;
	}
	@Override
	public int compareTo(Move o) {
		if(this.score < o.score)
			return -1;
		else if(this.score > o.score)
			return 1;

		return 0;
	}

}/** END OF CLASS */




