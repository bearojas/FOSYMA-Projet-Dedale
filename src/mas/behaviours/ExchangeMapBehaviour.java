package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.SimpleBehaviour;

public class ExchangeMapBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private int state = 0;
	private Graph myGraph ;
	
	public ExchangeMapBehaviour(final mas.abstractAgent myagent, Graph graph){
		super(myagent);
		myGraph = graph ;
		myGraph.setStrict(false);
	}
	
	// fonction de transformation d'un graphe vers une HashMap
	// cl� de la HashMap : identifiant du noeud
	// valeur pour chaque cl� : les voisins du noeud, l'�tat du noeud et les observations de ce noeud
	public HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> graphToHashmap(Graph graphToSend){
		HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> finalMap = new HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>>();
		//pour chaque noeud cr�er la cl�, liste de voisins vide, et observation
		for (Node n : graphToSend){
			finalMap.put(n.getId(), new Couple(new ArrayList(), new Couple(n.getAttribute("state"),n.getAttribute("content"))));
		}
		//pour chaque arc, on r�cup�re le noeud source #e.getNode0()#,
		//dans la hashMap � cette cl� on r�cup�re la liste des voisins #getLeft()#
		//et on y ins�re le noeud destination #add(e.getNode1())#
		for(Edge e : graphToSend.getEachEdge()){
			finalMap.get(e.getNode0().getId()).getLeft().add(e.getNode1().getId());
		}
		return finalMap;
	}
	
	//fonction de transformation d'une HashMap vers un graphe
	public Graph hashmapToGraph(HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> hMapReceived){
		Graph finalGraph = new SingleGraph("");
		finalGraph.setStrict(false);
		for (Entry<String, Couple<List<String>, Couple<String, List<Attribute>>>> entry : hMapReceived.entrySet()){
			//creation du noeud (si il existe deja retourne le noeud existant) 
			Node n = finalGraph.addNode(entry.getKey()) ;
			//ajout de l'attribut (modification si deja present)
			n.addAttribute("state", entry.getValue().getRight().getLeft());
			n.addAttribute("content", entry.getValue().getRight().getRight());
			// ajout des arcs, donc parcours des voisins 
			for (String neighborId : entry.getValue().getLeft()){
				// si l'arc existe deja, ne rien faire
				if ( finalGraph.getEdge(n.getId()+neighborId)== null && finalGraph.getEdge(neighborId+n.getId())==null){
					finalGraph.addNode(neighborId);
					finalGraph.addEdge(n.getId()+neighborId, n.getId(), neighborId);
				}
			}
		}
		return finalGraph ;
	}
	
	// fonction permettant de concatener 2 graphes, et donc de mettre � jour ses informations
	public void graphsFusion(Graph graphReceived){
		for (Node n : graphReceived){
			Node old_node = myGraph.getNode(n.getId());
			// si on ignorait l'existence de ce noeud, on l'ajoute � notre graphe ainsi que ses attributs
			if (old_node == null){
				Node new_node = myGraph.addNode(n.getId());
				new_node.addAttribute("state", n.getAttribute("state"));
				new_node.addAttribute("content", n.getAttribute("content"));
				
			} else { // si le noeud existait, on compare les attributs
				//si ce noeud a ete explore, on le marque closed (ne change rien s'il l'�tait deja)
				if (n.getAttribute("state").equals("closed"))
					old_node.setAttribute("state", "closed");

				List<Attribute> obs = n.getAttribute("content");
				List<Attribute> old_obs = old_node.getAttribute("content");
				
				// si il y avait un tr�sor, on garde la plus petite quantit� restante de ce tr�sor
				//on regarde les possibles attributs pour ce noeud dans les nouvelles observations 
				for(Attribute a : obs){
					if(a.getName().equals("Treasure")){
						int i = old_obs.indexOf("Treasure");
						if(i == -1){
							old_node.setAttribute("content", obs);
						}
						else{
							int oldTreasureValue = (int) old_obs.get(i).getValue();
							if((int)a.getValue() < oldTreasureValue ){
								old_obs.get(i).setValue(a.getValue());
							}
						}
					}
						
				}
			}
			
			// on parcourt tous les arcs du graphReceived
			for(Edge e : graphReceived.getEachEdge()){
				String id0 = e.getNode0().getId();
				String id1 = e.getNode1().getId();
				//si l'arc n'existe pas, on l'ajoute 
				if(myGraph.getEdge(id0+id1) == null && myGraph.getEdge(id1+id0) == null)
					myGraph.addEdge(id0+id1, id0, id1);
			}
		}
	}
	
	@Override
	public void action() {
		switch(state){
		case 0:
			// say hi
			//envoie un message "hello ?"
			//state ++
		case 1:
			// regardes sa boite aux lettres et attends un message de réponse (timeout)
			//timeout done --> finished = true
			// state ++
		case 2:
			//convertir le graph en map spécifique a chaque agent qui a répondu
			// et on le leur envoie
			//state++
		case 3:
			// on attend les graph des autres
			// on convertit les maps reçues en graphe
			// et on fusionne chaque graph avec le notre 
			// state ++ 
			
		default:
			finished = true;
		}
		
	}

	@Override
	public boolean done() {

		return finished;
	}

}
