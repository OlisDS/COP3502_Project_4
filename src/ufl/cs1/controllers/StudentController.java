package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.*;
import java.util.LinkedList;
import game.models.Maze;
import game.models.Attacker;
import java.util.List;

public final class StudentController implements DefenderController
{
	public void init(Game game) { }

	public void shutdown(Game game) { }

	//Store commonly needed variables here as static variables to avoid excessive method calling and memory overflow
	static Node attackerLocation;
	static Game game;
	static List<Node> powerPills;
	static List<Defender> enemies;
	static Node attackerLikelyTargetLocation;
	static Attacker attacker;
	static List<Node> attackerLikelyPath;

	public int[] update(Game game2,long timeDue)
	{
		//Assign values to static members here
		game = game2;
		attacker = game.getAttacker();
		attackerLocation = game.getAttacker().getLocation();
		powerPills = game.getPowerPillList();
		enemies = game.getDefenders();

		setAttackerLikelyTargetLocation();
		attackerLikelyPath = attacker.getPathTo(attackerLikelyTargetLocation);



		int[] actions = new int[Game.NUM_DEFENDER];

		List<Defender> enemies = game.getDefenders();

		//Chooses a random LEGAL action if required. Could be much simpler by simply returning
		//any random number of all of the ghosts
		/*
		for(int i = 0; i < actions.length; i++)
		{
			Defender defender = enemies.get(i);
			List<Integer> possibleDirs = defender.getPossibleDirs();
			if (possibleDirs.size() != 0)
				actions[i]=possibleDirs.get(Game.rng.nextInt(possibleDirs.size()));
			else
				actions[i] = -1;
		}
		*/
		//Just testing, 2 interceptors, 2 kamikaze

		actions[0] = interceptor(0);
		actions[1] = interceptor(1);
		actions[2] = goalie(2);
		actions[3] = kamikaze(3);

		return actions;
	}

	/* Behavior will be implemented dynamically, i.e. when we update we will assign roles (who gets what method)
	based on current game states like who's in the best position to intercept, follow, etc.
	Will need way for the ghosts to communicate, booleans paths etc.
	 */

	public int interceptor(int ghostID)
	{
	    /* FIXME, blank method for now.
	    Intercepter-kun work off premise IF it can get between pacman and the pill he's going to get between the two on
	    pacman's current/predicted path (shortest), the best way of doing this would be to figure out the shortest path
	    to pacman's path but prioritize nodes later on in pacman's path (as he will be moving)
	    ELSE should wait at safe distance (out of pacman's reach for when he gets the pill)
	     */
		Defender ghost = enemies.get(ghostID);
		if (ghost == null) return 0;


		//If the ghost stands between PacMan and his likely target, the ghost will target PacMan
	    if (attackerLikelyPath.contains(ghost.getLocation()))
	    	return ghost.getNextDir(attackerLocation, true);


	    List<Node> pathToTarget = ghost.getPathTo(attackerLikelyTargetLocation);

	    //Sees if the ghost can intercept PacMan
		//	if yes, the ghost contests the target
		//	if not, the ghost simply chases PacMan
	    if (pathToTarget.size() < attackerLikelyPath.size())
			return ghost.getNextDir(attackerLikelyTargetLocation, true);
	    if (attackerIsHoldingPattern()) return ghost.getNextDir(attackerLocation, false);
		else
			return ghost.getNextDir(attackerLocation, true);
	}

    public int stalker(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
	    /* FIXME blank method for now.
	    Stalker-san should follow at a safe distance, if pacman gets near the pill and intercepter isn't on intercept path
	    then should stay JUST out of range and then trigger pacman eating the pill by coming in range then running away
	     */

		return 0;
	}


    public int blocker(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
        /* FIXME blank method for now
        Blocker-chan should block off paths that would result in pacman getting superpill position, path blocked
        should not be already covered by another ghost, determine based on last nodes in path
         */

		return 0;
	}

    public int goalie(int ghostID)
	{
        /* FIXME blank method
        Goalie kun should be the furthest away ghost from doing anything, job will be to camp a power pill elsewhere
        This is a means of blocking off future paths and decisions by pacman
         */
        Maze maze = game.getCurMaze();

		List<Node> powerPillsLocation = maze.getPowerPillNodes();
		int[] distances = new int[powerPillsLocation.size()];

		// Evaluate the distances from pacman to all power pills on map
		for(Node powerPill: powerPillsLocation){
			for(int i = 0; i < powerPillsLocation.size(); i++){
				distances[i] = attackerLocation.getPathDistance(powerPill);
			}
		}

		// Find nodes for power pills second closest and first closest to Pacman
		Node secondClosest = null;
		Node firstClosest = null;
		int secondSmallestDistance = Integer.MAX_VALUE;
		int smallestDistance = Integer.MAX_VALUE;
		for(int i = 0; i < distances.length; i++){
			if(distances[i] < smallestDistance){
				secondSmallestDistance = smallestDistance;
				secondClosest = firstClosest;
				smallestDistance = distances[i];
				firstClosest = powerPillsLocation.get(i);
			}
			else if(distances[i] < secondSmallestDistance){
				secondSmallestDistance = distances[i];
				secondClosest = powerPillsLocation.get(i);
			}
		}

		// Complete by having hover shortest circular path through node and if pacman begins approaching node change to
		// attack pattern, like a goalie rushing a forward, perhaps implement this behavior via intercept in update method
		// FIXME Have circle around going through multiple times.
		// Come up with contingency for when no power pills left, roam and run away?
		// Approach target power pill, if sufficiently close begin loop pattern
		// If pacman is close then attack him
		Defender ghost = enemies.get(ghostID);
		if (!(maze.getPowerPillNodes().size() > 1)){
			return interceptor(ghostID);
		}
		if (attacker.getLocation().getPathDistance(secondClosest) > 7){
			return ghost.getNextDir(secondClosest, true);
		}
		else if(attacker.getLocation().getPathDistance(secondClosest) < ghost.getLocation().getPathDistance(secondClosest)) {
			return ghost.getNextDir(attacker.getLocation(), true);
		}
		else{
			return ghost.getNextDir(attacker.getLocation(), false);
		}
	}

	public int kamikaze(int ghostID) {
		// FIXME Kamikaze needs to only trigger PacMan's Holding Pattern when the other defenders are at a safe distance
		return enemies.get(ghostID).getNextDir(attackerLocation, true);
	}

	//FIXME for debugging purposes to test the attackerIsHoldingPattern(). Ghosts run away until Attacker is in holding pattern.
	public int suicideLOL(int ghostID){
		Defender ghost = enemies.get(ghostID);
		if (attackerIsHoldingPattern()) return ghost.getNextDir(attackerLocation, true);
		else return ghost.getNextDir(attackerLocation, false);
	}

	public void setAttackerLikelyTargetLocation(){
		/*
		Predicts PacMan's target location depending on these priorities:
			1.	Vulnerable ghosts
			2.	Power pills
			3.	Pill
		 */
		List<Node> vulnerableDefenderLocations = new LinkedList<>();
		for (Defender d : enemies) if (d.isVulnerable()) vulnerableDefenderLocations.add(d.getLocation());

		if (!vulnerableDefenderLocations.isEmpty())
			attackerLikelyTargetLocation = attacker.getTargetNode(vulnerableDefenderLocations, true);
		else if (!powerPills.isEmpty())
			attackerLikelyTargetLocation = attacker.getTargetNode(powerPills, true);
		else
			attackerLikelyTargetLocation = attacker.getTargetNode(game.getPillList(), true);
	}

	//FIXME this method below needs to tell if PacMan is in holding pattern.
	public boolean attackerIsHoldingPattern(){
		return false;
	}
}