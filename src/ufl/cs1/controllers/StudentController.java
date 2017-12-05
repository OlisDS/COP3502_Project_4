package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.*;

import java.util.LinkedList;
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
		actions[0] = ghost1(0);
		actions[1] = ghost1(1);
		actions[2] = suicide(2);
		actions[3] = suicide(3);

		return actions;
	}

	/* Behavior will be implemented dynamically, i.e. when we update we will assign roles (who gets what method)
	based on current game states like who's in the best position to intercept, follow, etc.
	Will need way for the ghosts to communicate, booleans paths etc.
	 */

	public int ghost1(int ghostID)
	{
	    /* FIXME, blank method for now.
	    Intercepter-kun work off premise IF it can get between pacman and the pill he's going to get between the two on
	    pacman's current/predicted path (shortest), the best way of doing this would be to figure out the shortest path
	    to pacman's path but prioritize nodes later on in pacman's path (as he will be moving)
	    ELSE should wait at safe distance (out of pacman's reach for when he gets the pill)
	     */
		Defender ghost1 = enemies.get(ghostID);


		//If the ghost stands between PacMan and his likely target, the ghost will target PacMan
	    if (attackerLikelyPath.contains(ghost1.getLocation()))
	    	return ghost1.getNextDir(attackerLocation, true);


	    List<Node> pathToTarget = ghost1.getPathTo(attackerLikelyTargetLocation);

	    //Sees if the ghost can intercept PacMan
		//	if yes, the ghost contests the target
		//	if not, the ghost simply chases PacMan
		//	FIXME Interceptor needs to stop chasing PacMan once resistance is futile
	    if (pathToTarget.size() < attackerLikelyPath.size())
			return ghost1.getNextDir(attackerLikelyTargetLocation, true);
		if (attackerLikelyPath.size() < 10)
	    	return ghost1.getNextDir(attackerLikelyTargetLocation, false);
		else
			return ghost1.getNextDir(attackerLocation, true);
    }

    public int ghost2()
	{
	    /* FIXME blank method for now.
	    Stalker-kun should follow at a safe distance, if pacman gets near the pill and intercepter isn't on intercept path
	    then should stay JUST out of range and then trigger pacman eating the pill by coming in range then running away
	     */

        return 0;
    }

    public int ghost3()
	{
        /* FIXME blank method for now
        Blocker-chan should block off paths that would result in pacman getting superpill position, path blocked
        should not be already covered by another ghost, determine based on last nodes in path
         */

        return 0;
    }

    public int ghost4()
	{
        /* FIXME blank method
        Goalie san should be the furthest away ghost from doing anything, job will be to camp a power pill elsewhere
        This is a means of blocking off future paths and decisions by pacman
         */
        return 0;
    }

	public int suicide(int ghostID)
	{
		return enemies.get(ghostID).getNextDir(attackerLocation, true);
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
}