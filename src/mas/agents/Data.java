package mas.agents;

import jade.util.leap.Serializable;


/**
 * Class to save 3 data in a row
 * @param <E>
 * @param <F>
 * @param <G>
 */
public class Data<E,F,G> implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private E first ;
	private F mid ;
	private G right ;
	
	public Data(E n, F s, G att){
		first = n;
		mid = s;
		right = att ;
	}
	
	public E getLeft(){
		return first ;
	}
	
	public F getMid(){
		return mid ;
	}
	
	public G getRight(){
		return right ;
	}
}
