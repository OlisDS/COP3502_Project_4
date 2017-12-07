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

	static List<Integer> unassignedDefenderIDs;

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

		//unassignedDefenderIDs holds all the ghost IDs that have not been assigned roles
		unassignedDefenderIDs = new LinkedList<>();
		for (int i = 0; i < 4; i++) unassignedDefenderIDs.add(i);


		int[] actions = new int[Game.NUM_DEFENDER];

		int stalkerID = 1;
		actions[stalkerID] = kamikaze(stalkerID);
		unassignedDefenderIDs.remove(unassignedDefenderIDs.indexOf(stalkerID));

		int interceptorID = chooseInterceptor();
		actions[interceptorID] = interceptor(interceptorID);
		unassignedDefenderIDs.remove(unassignedDefenderIDs.indexOf(interceptorID));

		int blockerID = chooseInterceptor();
		actions[blockerID] = interceptor(blockerID);
		unassignedDefenderIDs.remove(unassignedDefenderIDs.indexOf(blockerID));

		int goalieID = chooseGoalie();
		actions[goalieID] = goalie(goalieID);
		unassignedDefenderIDs.remove(unassignedDefenderIDs.indexOf(goalieID));

		return actions;
	}

	/* FIXME Behavior will be implemented dynamically, i.e. when we update we will assign roles (who gets what method)
	based on current game states like who's in the best position to intercept, follow, etc.
	Will need way for the ghosts to communicate, booleans paths etc.
	 */

	//fixme. Each method below should decide the best ghost for the role. Available ghosts are from unassignedDefenderIDs list
	private int chooseInterceptor() {
		int bestGhostID = -1;
		int shortestPathDistanceToAttackersTarget = Integer.MAX_VALUE;

		for (Integer i : unassignedDefenderIDs){
			Defender ghost = enemies.get(i);
			int pathDistance = ghost.getLocation().getPathDistance(attackerLikelyTargetLocation);
			if (shortestPathDistanceToAttackersTarget > pathDistance){
				bestGhostID = i;
				shortestPathDistanceToAttackersTarget = pathDistance;
			}
		}
		return bestGhostID;
	}
	private int chooseStalker(){
		int bestGhostID = -1;
		int shortestPathDistanceToAttacker = Integer.MAX_VALUE;

		for (Integer i : unassignedDefenderIDs){
			Defender ghost = enemies.get(i);
			int pathDistance = ghost.getLocation().getPathDistance(attackerLocation);
			if (shortestPathDistanceToAttacker > pathDistance){
				bestGhostID = i;
				shortestPathDistanceToAttacker = pathDistance;
			}
		}
		return bestGhostID;
	}
	private int chooseBlocker(){return -1;}
	private int chooseGoalie(){
		int bestGhostID = -1;
		int longestPathDistanceToAttacker = Integer.MIN_VALUE;

		for (Integer i : unassignedDefenderIDs){
			Defender ghost = enemies.get(i);
			int pathDistance = ghost.getLocation().getPathDistance(attackerLocation);
			if (longestPathDistanceToAttacker < pathDistance){
				bestGhostID = i;
				longestPathDistanceToAttacker = pathDistance;
			}
		}
		return bestGhostID;
	}


	public int interceptor(int ghostID)
	{
	    /* Original idea:
	    Intercepter-kun work off premise IF it can get between pacman and the pill he's going to get between the two on
	    pacman's current/predicted path (shortest), the best way of doing this would be to figure out the shortest path
	    to pacman's path but prioritize nodes later on in pacman's path (as he will be moving)
	    ELSE should wait at safe distance (out of pacman's reach for when he gets the pill)
	     */

	    /* Actual implementation:
	    Interceptor sets its target Node to PacMan's target Node. Checks to see if it can intercept the target
	    	if can intercept, Interceptor intercepts
	    	if can't intercept, Interceptor just stalks PacMan
	    if Interceptor stands between PacMan and the target, Interceptor charges at PacMan
	     */
		Defender ghost = enemies.get(ghostID);
		if (ghost == null) return 0;
		if (ghost.isVulnerable() || attackerIsHoldingPattern()) return flee(ghostID);


		//	If the ghost stands between PacMan and his likely target, the ghost will target PacMan
	    if (attackerLikelyPath.contains(ghost.getLocation()))
	    	return ghost.getNextDir(attackerLocation, true);


	    List<Node> pathToTarget = ghost.getPathTo(attackerLikelyTargetLocation);

	    //	Sees if the ghost can intercept PacMan
		//		if yes, the ghost contests the target
		//		if not, the ghost simply chases PacMan
	    if (pathToTarget.size() < attackerLikelyPath.size())
			return ghost.getNextDir(attackerLikelyTargetLocation, true);
		else
			return ghost.getNextDir(attackerLocation, true);
	}

    public int stalker(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
		if (ghost.isVulnerable()) return flee(ghostID);

	    /* FIXME blank method for now.
	    Stalker-san should follow at a safe distance, if pacman gets near the pill and intercepter isn't on intercept path
	    then should stay JUST out of range and then trigger pacman eating the pill by coming in range then running away
	     */

		return 0;
	}


    public int blocker(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
		if (ghost.isVulnerable() || attackerIsHoldingPattern()) return flee(ghostID);

        /* FIXME blank method for now
        Blocker-chan should block off paths that would result in pacman getting superpill position, path blocked
        should not be already covered by another ghost, determine based on last nodes in path
         */

		return 0;
	}

    public int goalie(int ghostID)
	{
        /*
        Goalie kun should be the furthest away ghost from doing anything, job will be to camp a power pill elsewhere
        This is a means of blocking off future paths and decisions by pacman
         */
        Maze maze = game.getCurMaze();

		List<Node> powerPillsLocation = game.getPowerPillList();
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
		if (!(powerPillsLocation.size() > 1)){
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

	public int kamikaze(int ghostID)
	{
		//	Kamikaze (not one of our final ghosts) charges blindly at Pac Man until Pac Man reaches a Power Pill.
		//	Kamikaze waits outside of Pac Man's trigger range until the other ghosts are out of PacMan's reach
		//	Kamikaze rushes Pac Man and gets eaten immediately. and respawns to protect the other ghosts
		Defender ghost = enemies.get(ghostID);
		if (!attackerIsHoldingPattern()) return ghost.getNextDir(attackerLocation, true);

		boolean safeToRushTheAttacker = true;

		//	if a ghost (other than Kamikaze) is too close to PacMan, not safe
		for (Defender d : enemies){
			if (d == ghost) continue; //excluding the kamikaze ghost
			if (d.getLocation().getPathDistance(attackerLocation) <= 50 && !powerPills.isEmpty()) safeToRushTheAttacker = false;
		}

		//	if other ghosts are safe distance, rush PacMan
		if (safeToRushTheAttacker) return ghost.getNextDir(attackerLocation, true);

		//	if other ghosts are not safe distance, hold off on rushing PacMan
		if (ghost.getLocation().getPathDistance(attackerLocation) >= 50) return ghost.getNextDir(attackerLocation, true);
		else return ghost.getNextDir(attackerLocation, false);
	}

	public void setAttackerLikelyTargetLocation()
	{
		//	Predicts PacMan's target location depending on these priorities:
		//		1.	Vulnerable ghosts
		//		2.	Power pills
		//		3.	Pill
		List<Node> vulnerableDefenderLocations = new LinkedList<>();
		for (Defender d : enemies) if (d.isVulnerable()) vulnerableDefenderLocations.add(d.getLocation());

		if (!vulnerableDefenderLocations.isEmpty())
			attackerLikelyTargetLocation = attacker.getTargetNode(vulnerableDefenderLocations, true);
		else if (!powerPills.isEmpty())
			attackerLikelyTargetLocation = attacker.getTargetNode(powerPills, true);
		else
			attackerLikelyTargetLocation = attacker.getTargetNode(game.getPillList(), true);
	}

	//	Sees if PacMan is dangerously close to the Power Pill
	public boolean attackerIsHoldingPattern()
	{
		if (powerPills.isEmpty()) return false;
		return attackerLocation.getPathDistance(attacker.getTargetNode(powerPills, true)) <= 20;
	}

	//	For when ghost is vulnerable
	public int flee(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
		return ghost.getNextDir(attackerLocation, false);
	}
}