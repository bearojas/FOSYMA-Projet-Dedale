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
import jade.core.behaviours.Behaviour;

public class ExchangeMapBehaviour extends Behaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private boolean finished = false;
	private Graph myGraph ;
	
	public ExchangeMapBehaviour(final mas.abstractAgent myagent, Graph graph){
		super(myagent);
		myGraph = graph ;
		myGraph.setStrict(false);
	}
	
	// fonction de transformation d'un graphe vers une HashMap
	// clé de la HashMap : identifiant du noeud
	// valeur pour chaque clé : les voisins du noeud, l'état du noeud et les observations de ce noeud
	public HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> graphToHashmap(Graph graphToSend){
		HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>> finalMap = new HashMap<String, Couple<List<String>, Couple<String, List<Attribute>>>>();
		//pour chaque noeud créer la clé, liste de voisins vide, et observation
		for (Node n : graphToSend){
			finalMap.put(n.getId(), new Couple(new ArrayList(), new Couple(n.getAttribute("state"),n.getAttribute("content"))));
		}
		//pour chaque arc, on récupère le noeud source #e.getNode0()#,
		//dans la hashMap à cette clé on récupère la liste des voisins #getLeft()#
		//et on y insère le noeud destination #add(e.getNode1())#
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
				if ( finalGraph.getEdge(n.getId()+neighborId)== null && finalGraph.getEdge(neighborId)+n.getId()==null){
					finalGraph.addNode(neighborId);
					finalGraph.addEdge(n.getId()+neighborId, n.getId(), neighborId);
				}
			}
		}
		return finalGraph ;
	}
	
	// fonction permettant de concatener 2 graphes, et donc de mettre à jour ses informations
	public void concatenateGraphs(Graph graphReceived){
		for (Node n : graphReceived){
			Node old_node = myGraph.getNode(n.getId());
			// si on ignorait l'existence de ce noeud, on l'ajoute à notre graphe
			if (old_node == null){
				myGraph.addNode(n.getId());
			} else { // si le noeud existait, on compare les attributs
				//si ce noeud a ete explore, on le marque closed (ne change rien s'il l'était deja)
				if (n.getAttribute("state").equals("closed"))
					old_node.setAttribute("state", "closed");

				List<Attribute> obs = n.getAttribute("content");
				List<Attribute> old_obs = old_node.getAttribute("content");
				// si il y avait un trésor, on garde la plus petite quantité restante de ce trésor
				if(obs.size()!=0 && old_obs.size()!=0) {
					if(obs.get(0).getValue() < old_obs.get(0).getValue())
						old_node.setAttribute("content", obs);;
				}
			}
			
			// Il faut traiter les arcs ! 
			// Parcourir tous les arcs du graphReceived et les ajouter si ils n'existent pas deja
		}
	}
	
	@Override
	public void action() {
		
		
	}

	@Override
	public boolean done() {

		return finished;
	}

}
