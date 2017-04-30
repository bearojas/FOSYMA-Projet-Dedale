package mas.agents;

import jade.util.leap.Serializable;


/**
 * Class to save 4 data in a row
 * @param <E>
 * @param <F>
 * @param <G>
 * @param <H>>
 */
public class Data<E,F,G,H> implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private E first ;
	private F second ;
	private G third ;
	private H last ;
	
	public Data(E n, F s, G att, H aid){
		first = n;
		second = s;
		third= att ;
		last=aid ;
	}
	
	public E getFirst(){
		return first ;
	}
	
	public F getSecond(){
		return second ;
	}
	
	public G getThird(){
		return third ;
	}
	
	public H getLast(){
		return last ;
	}
}
