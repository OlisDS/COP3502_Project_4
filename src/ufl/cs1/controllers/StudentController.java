package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Actor;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.List;

public final class StudentController implements DefenderController
{
	public void init(Game game) { }

	public void shutdown(Game game) { }

	public int[] update(Game game,long timeDue)
	{
		int[] actions = new int[Game.NUM_DEFENDER];
		List<Defender> enemies = game.getDefenders();
		
		//Chooses a random LEGAL action if required. Could be much simpler by simply returning
		//any random number of all of the ghosts
		for(int i = 0; i < actions.length; i++)
		{
			Defender defender = enemies.get(i);
			List<Integer> possibleDirs = defender.getPossibleDirs();
			if (possibleDirs.size() != 0)
				actions[i]=possibleDirs.get(Game.rng.nextInt(possibleDirs.size()));
			else
				actions[i] = -1;
		}
		return actions;
	}

	/* Behavior will be implemented dynamically, i.e. when we update we will assign roles (who gets what method)
	based on current game states like who's in the best position to intercept, follow, etc.
	Will need way for the ghosts to communicate, booleans paths etc.
	 */

	public int[] ghost1(){
	    /* FIXME, blank method for now.
	    Intercepter-kun work off premise IF it can get between pacman and the pill he's going to get between the two on
	    pacman's current/predicted path (shortest), the best way of doing this would be to figure out the shortest path
	    to pacman's path but prioritize nodes later on in pacman's path (as he will be moving)
	    ELSE should wait at safe distance (out of pacman's reach for when he gets the pill)
	     */

        return new int[8];
    }

    public int[] ghost2(){
	    /* FIXME blank method for now.
	    Stalker-kun should follow at a safe distance, if pacman gets near the pill and intercepter isn't on intercept path
	    then should stay JUST out of range and then trigger pacman eating the pill by coming in range then running away
	     */

        return new int[8];
    }

    public int[] ghost3(){
        /* FIXME blank method for now
        Blocker-chan should block off paths that would result in pacman getting superpill position, path blocked
        should not be already covered by another ghost, determine based on last nodes in path
         */

        return new int[8];
    }

    public int[] ghost4(){
        /* FIXME blank method
        Goalie san should be the furthest away ghost from doing anything, job will be to camp a power pill elsewhere
        This is a means of blocking off future paths and decisions by pacman
         */
        return new int[8];
    }
}