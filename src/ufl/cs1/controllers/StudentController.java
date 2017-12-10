package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.*;
import java.util.LinkedList;
import game.models.Maze;
import game.models.Attacker;
import java.util.List;

public final class StudentController implements DefenderController
{
	//updated as of Dec 10 15:10
	public void init(Game game) { }
	public void shutdown(Game game) { }

	//Store commonly needed variables here as static variables to avoid excessive method calling and memory overflow
	private static Node attackerLocation;
	private static Game game;
	private static List<Node> powerPills;
	private static List<Defender> enemies;
	private static Node attackerLikelyTargetLocation;
	private static Attacker attacker;
	private static List<Node> attackerLikelyPath;
	private static boolean canIntercept;

	public int[] update(Game game2,long timeDue)
	{

		game = game2;
		attacker = game.getAttacker();
		attackerLocation = game.getAttacker().getLocation();
		powerPills = game.getPowerPillList();
		enemies = game.getDefenders();

		setAttackerLikelyTargetLocation();
		attackerLikelyPath = attacker.getPathTo(attackerLikelyTargetLocation);


		int[] actions = new int[Game.NUM_DEFENDER];

		actions[0] = blocker(0);
		actions[1] = interceptor(1);
		actions[2] = goalie(2);
		actions[3] = stalker(3);

		return actions;
	}

	private int interceptor(int ghostID)
	{

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
	    	return chase(ghostID);

	    List<Node> pathToTarget = ghost.getPathTo(attackerLikelyTargetLocation);

	    //	Sees if the ghost can intercept PacMan
		//		if yes, the ghost contests the target
		//		if not, the ghost simply chases PacMan
	    if (pathToTarget.size() < attackerLikelyPath.size()) {
			canIntercept = true;
			return ghost.getNextDir(attackerLikelyTargetLocation, true);
		}
		else {
			canIntercept = false;
			return chase(ghostID);
		}
	}

	private int blocker(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
		if (ghost.isVulnerable() || attackerIsHoldingPattern()) return flee(ghostID);

        /*
        Blocker should block off paths that would result in pacman getting superpill position, path blocked
        should not be already covered by another ghost, determine based on last nodes in path
         */

		List<Node> powerPillsLocation = game.getPowerPillList();

		for (int i = 0; i < powerPillsLocation.size(); i++){
			if (attackerLikelyPath.contains(powerPillsLocation.get(i))) {
				return ghost.getNextDir(attackerLikelyTargetLocation, false);
			}
		}

		if (attackerLikelyPath.contains(ghost.getLocation())){

    		return chase(ghostID);
		}

		else{

			for (int i = 0; i < powerPillsLocation.size(); i++){
				return ghost.getNextDir(powerPillsLocation.get(i), true);
			}
		}

		return 0;
	}

	private int blockerSenpai(int ghostID)
	{
		//	Senpai is always trying to get a good view of PacMan.
		//	Whenever a defender is in his way, Senpai will find another way to see PacMan
		Defender ghost = enemies.get(ghostID);
		if (ghost.isVulnerable() || attackerIsHoldingPattern()) return flee(ghostID);
		if (attackerLikelyPath.contains(ghost.getLocation())) return chase(ghostID);

		List<Node> path = ghost.getPathTo(attackerLocation);
		for (Defender d : enemies) if (path.contains(d.getLocation())) return (ghost.getNextDir(attackerLocation, true) + 1) % 4;
        return chase(ghostID);
	}

	private int goalie(int ghostID)
	{
        /*
        Goalie should be the furthest away ghost from doing anything, job will be to camp a power pill elsewhere
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
		
		// Approach target power pill, if sufficiently close begin loop pattern
		// If pacman is close then attack him
		Defender ghost = enemies.get(ghostID);

		//	if pacman is chasing the goalie's pill, the goalie reads that it is in PacMan's likely path and proceeds to chase PacMan
		//	until it is not in PacMan's path
		if (attackerLikelyPath.contains(ghost.getLocation()))
			return chase(ghostID);

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

	private int stalker(int ghostID)
	{
		//	Stalker charges blindly at Pac Man until Pac Man reaches a Power Pill.
		//	Stalker waits outside of Pac Man's trigger range until the other ghosts are out of PacMan's reach
		//	Stalker rushes Pac Man and gets eaten immediately. and respawns to protect the other ghosts

		final int SAFETY_MARGIN_FOR_OTHER_GHOSTS = 50;
		final int SAFETY_MARGIN_FOR_THIS_GHOST = 50;

		Defender ghost = enemies.get(ghostID);
		if (!attackerIsHoldingPattern() || canIntercept) return chase(ghostID);

		boolean safeToRushTheAttacker = true;

		//	if a ghost (other than Stalker) is too close to PacMan, not safe
		for (Defender d : enemies){
			if (d == ghost) continue; //excluding the Stalker ghost
			if (d.getLocation().getPathDistance(attackerLocation) <= SAFETY_MARGIN_FOR_OTHER_GHOSTS && !powerPills.isEmpty()) safeToRushTheAttacker = false;
		}

		//	if other ghosts are safe distance, rush PacMan
		if (safeToRushTheAttacker) return chase(ghostID);

		//	if other ghosts are not safe distance, hold off on rushing PacMan
		if (ghost.getLocation().getPathDistance(attackerLocation) >= SAFETY_MARGIN_FOR_THIS_GHOST) return chase(ghostID);
		else return ghost.getNextDir(attackerLocation, false);
	}
	
	//	For when ghost is vulnerable
	private int flee(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
		return ghost.getNextDir(attackerLocation, false);
	}
	
	private int chase(int ghostID)
	{
		Defender ghost = enemies.get(ghostID);
		return ghost.getNextDir(attackerLocation, true);
	}

	private void setAttackerLikelyTargetLocation()
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
	private boolean attackerIsHoldingPattern()
	{
		final int TOO_CLOSE = 20;

		if (powerPills.isEmpty()) return false;
		return attackerLocation.getPathDistance(attacker.getTargetNode(powerPills, true)) <= TOO_CLOSE;
	}
}