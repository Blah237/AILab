/*
 * AIController.java
 *
 * This class is an inplementation of InputController that uses AI and pathfinding
 * algorithms to determine the choice of input.
 *
 * NOTE: This is the file that you need to modify.  You should not need to 
 * modify any other files (though you may need to read Board.java heavily).
 *
 * Author: Walker M. White, Cristian Zaloj
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 * LibGDX version, 1/24/2015
 */
package edu.cornell.gdiac.ailab;
import com.badlogic.gdx.math.*;
import java.util.*;
import java.util.Vector;

/**CITATIONS:
* http://stackoverflow.com/questions/8922060/how-to-trace-the-path-in-a-breadth-first-search and
 * https://gist.github.com/gennad/791932
 * http://stackoverflow.com/questions/10234487/storing-number-pairs-in-java*/

/** 
 * InputController corresponding to AI control.
 * 
 * REMEMBER: As an implementation of InputController you will have access to
 * the control code constants in that interface.  You will want to use them.
 */
public class AIController implements InputController {
	/**
	 * Enumeration to encode the finite state machine.
	 */
	private static enum FSMState {
		/** The ship just spawned */
		SPAWN,
		/** The ship is patrolling around without a target */
		WANDER,
		/** The ship has a target, but must get closer */
		CHASE,
		/** The ship has a target and is attacking it */
		ATTACK
	}

	// Constants for chase algorithms
	/** How close a target must be for us to chase it */
	private static final int CHASE_DIST  = 9;
	/** How close a target must be for us to attack it */
	private static final int ATTACK_DIST = 4;

	// Instance Attributes
	/** The ship being controlled by this AIController */
	private Ship ship;
	/** The game board; used for pathfinding */
	private Board board;
	/** The other ships; used to find targets */
	private ShipList fleet;
	/** The ship's current state in the FSM */
	private FSMState state;
	/** The target ship (to chase or attack). */
	private Ship target; 
	/** The ship's next action (may include firing). */
	private int move; // A ControlCode
	/** The number of ticks since we started this controller */
	private long ticks;
	
	// Custom fields for AI algorithms
	//#region ADD YOUR CODE: 
	private LinkedList<Pair> finalpath;
	private boolean beginning;
	private Pair prevGoal;
	//#endregion
	
	/**
	 * Creates an AIController for the ship with the given id.
	 *
	 * @param id The unique ship identifier
	 * @param board The game board (for pathfinding)
	 * @param ships The list of ships (for targetting)
	 */
	public AIController(int id, Board board, ShipList ships) {
		this.ship = ships.get(id);
		this.board = board;
		this.fleet = ships;
		this.finalpath = null;
		this.beginning = true;
		
		state = FSMState.SPAWN;
		move  = CONTROL_NO_ACTION;
		ticks = 0;

		// Select an initial target
		target = null;
		selectTarget();
		this.beginning = false;
		this.prevGoal = new Pair(board.getWidth()/2,board.getHeight()/2);
	}

	/**
	 * Returns the action selected by this InputController
	 *
	 * The returned int is a bit-vector of more than one possible input 
	 * option. This is why we do not use an enumeration of Control Codes;
	 * Java does not (nicely) provide bitwise operation support for enums. 
	 *
	 * This function tests the environment and uses the FSM to chose the next
	 * action of the ship. This function SHOULD NOT need to be modified.  It
	 * just contains code that drives the functions that you need to implement.
	 *
	 * @return the action selected by this InputController
	 */
	public int getAction() {
		// Increment the number of ticks.
		ticks++;

		// Do not need to rework ourselves every frame. Just every 10 ticks.
		if ((ticks) % 10 == 0) {
			// Process the FSM
			changeStateIfApplicable();

			// Pathfinding
			markGoalTiles();
			move = getMoveAlongPathToGoalTile();
		}

		int action = move;

		// If we're attacking someone and we can shoot him now, then do so.
		if (state == FSMState.ATTACK && canShootTarget()) {
			action |= CONTROL_FIRE;
		}

		return action;
	}
	
	// FSM Code for Targeting (MODIFY ALL THE FOLLOWING METHODS)

	/**
	 * Change the state of the ship.
	 *
	 * A Finite State Machine (FSM) is just a collection of rules that,
	 * given a current state, and given certain observations about the
	 * environment, chooses a new state. For example, if we are currently
	 * in the ATTACK state, we may want to switch to the CHASE state if the
	 * target gets out of range.
	 */
	private void changeStateIfApplicable() {
		// Add initialization code as necessary
		//#region PUT YOUR CODE HERE
	
		//#endregion

		// Next state depends on current state.
		switch (state) {
		case SPAWN: // Do not pre-empt with FSMState in a case
			// Insert checks and spawning-to-??? transition code here
			//#region PUT YOUR CODE HERE
			state = FSMState.WANDER;
			//#endregion
			break;

		case WANDER: // Do not pre-empt with FSMState in a case
			// Insert checks and moving-to-??? transition code here
			//#region PUT YOUR CODE HERE
			if (!target.isActive()) {
				selectTarget();
			}
			if (ticks % 300 == 0) {
				selectTarget();
			}
			if (inChase(board.screenToBoard(ship.getPosition().x),
						board.screenToBoard(ship.getPosition().y), target)) {
			state = FSMState.CHASE;
		}
			//#endregion			
			break;

		case CHASE: // Do not pre-empt with FSMState in a case
			// insert checks and chasing-to-??? transition code here
			//#region PUT YOUR CODE HERE
			if (!target.isActive()) {
				selectTarget();
			}
			if (ticks % 600 == 0) {
				selectTarget();
			}
			if (canShootTarget()) {
				state = FSMState.ATTACK;
			}
			if (!inChase(board.screenToBoard(ship.getPosition().x), board.screenToBoard(ship.getPosition().y), target)) {
				state = FSMState.WANDER;
			}
			//#endregion			
			break;

		case ATTACK: // Do not pre-empt with FSMState in a case
			// insert checks and attacking-to-??? transition code here
			//#region PUT YOUR CODE HERE
			if (!target.isActive()) {
				selectTarget();
			}
			if (ticks % 300 == 0) {
				selectTarget();
			}
			if (!canShootTarget()) {
				state = FSMState.CHASE;
			}
			//#endregion			
			break;

		default:
			// Unknown or unhandled state, should never get here
			assert (false);
			state = FSMState.WANDER; // If debugging is off
			break;
		}
	}

	private boolean inChase(int x, int y, Ship target) {
		int targetX = board.screenToBoard(target.getPosition().x);
		int targetY = board.screenToBoard(target.getPosition().y);
			if (x + CHASE_DIST > targetX && x - CHASE_DIST < targetX
					&& y + CHASE_DIST > targetY && y - CHASE_DIST < targetY) {
				return true;
			}
		return false;
	}

	/**
	 * Acquire a target to attack (and put it in field target).
	 *
	 * Insert your checking and target selection code here. Note that this
	 * code does not need to reassign <c>target</c> every single time it is
	 * called. Like all other methods, make sure it works with any number
	 * of players (between 0 and 32 players will be checked). Also, it is a
	 * good idea to make sure the ship does not target itself or an
	 * already-fallen (e.g. inactive) ship.
	 */
	private void selectTarget() {
		//#region PUT YOUR CODE HERE
		ArrayList<Ship> activeShips = new ArrayList<Ship>();
		for (Ship s : fleet) {
			if (s.isActive() && s!= ship) {
				activeShips.add(s);
			}
		}
		if ((target == null && beginning || !target.isActive())) {
			Random r = new Random();
			int r2 = r.nextInt(activeShips.size());
			if (activeShips.get(r2).isActive()) {
				target = activeShips.get(r2);
			}
		} else if (fleet.size() > 3) {
			for (Ship s : activeShips) {
				if (inBigChase(board.screenToBoard(ship.getPosition().x), board.screenToBoard(ship.getPosition().y), s)) {
					target = s;
					break;
				}
			}
		} else {
			if (fleet.getPlayer().isActive()) {
				target = fleet.getPlayer();
			}
		}



		//#endregion			
	}


	private boolean inBigChase(int x, int y, Ship target) {
		int targetX = board.screenToBoard(target.getPosition().x);
		int targetY = board.screenToBoard(target.getPosition().y);
		if (x + CHASE_DIST + 5 > targetX && x - CHASE_DIST - 5 < targetX
				&& y + CHASE_DIST + 5 > targetY && y - CHASE_DIST - 5 < targetY) {
			return true;
		}
		return false;
	}
	/**
	 * Returns true if we can hit a target from here.
	 *
	 * Insert code to return true if a shot fired from the given (x,y) would
	 * be likely to hit the target. We can hit a target if it is in a straight
	 * line from this tile and within attack range. The implementation must take
	 * into consideration whether or not the source tile is a Power Tile.
	 *
	 * @param x The x-index of the source tile
	 * @param y The y-index of the source tile
	 *
	 * @return true if we can hit a target from here.
	 */
	private boolean canShootTargetFrom(int x, int y) {
		//#region PUT YOUR CODE HERE
		int targetX = board.screenToBoard(target.getPosition().x);
		int targetY = board.screenToBoard(target.getPosition().y);
		if (board.isPowerTileAt(x, y)) {
			if (x + ATTACK_DIST > targetX && x - ATTACK_DIST < targetX
					&& y + ATTACK_DIST > targetY && y - ATTACK_DIST < targetY) {
				return true;
			}
		} else {
			if ((x + ATTACK_DIST > targetX && y == targetY) || (x - ATTACK_DIST < targetX && y == targetY) ||
					(y + ATTACK_DIST > targetY && x == targetX) || (y - ATTACK_DIST < targetY && x == targetX)) {
				return true;
			}
		}
		return false;
		//#endregion			
	}
	/**
	 * Returns true if we can both fire and hit our target
	 *
	 * If we can fire now, and we could hit the target from where we are, 
	 * we should hit the target now.
	 *
	 * @return true if we can both fire and hit our target
	 */
	private boolean canShootTarget() {
		//#region PUT YOUR CODE HERE
		return (ship.canFire() && canShootTargetFrom
				((board.screenToBoard(ship.getPosition().x)),board.screenToBoard(ship.getPosition().y)));
		//#endregion			
	}

	// Pathfinding Code (MODIFY ALL THE FOLLOWING METHODS)

	/** 
	 * Mark all desirable tiles to move to.
	 *
	 * This method implements pathfinding through the use of goal tiles.
	 * It searches for all desirable tiles to move to (there may be more than
	 * one), and marks each one as a goal. Then, the pathfinding method
	 * getMoveAlongPathToGoalTile() moves the ship towards the closest one.
	 *
	 * POSTCONDITION: There is guaranteed to be at least one goal tile
     * when completed.
     */
	private void markGoalTiles() {
		// Clear out previous pathfinding data.
		board.clearMarks();
		boolean setGoal = false; // Until we find a goal

		// Add initialization code as necessary
		int theX = board.screenToBoard(ship.getPosition().x);
		int theY = board.screenToBoard(ship.getPosition().y);
		int targetX = board.screenToBoard(target.getPosition().x);
		int targetY = board.screenToBoard(target.getPosition().y);
		//#region PUT YOUR CODE HERE
		//#endregion

		switch (state) {
		case SPAWN: // Do not pre-empt with FSMState in a case
			// insert code here to mark tiles (if any) that spawning ships
			// want to go to, and set setGoal to true if we marked any.
			// Ships in the spawning state will immediately move to another
			// state, so there is no need for goal tiles here.

			//#region PUT YOUR CODE HERE
			//#endregion
			break;

		case WANDER: // Do not pre-empt with FSMState in a case
			// Insert code to mark tiles that will cause us to move around;
			// set setGoal to true if we marked any tiles.
			// NOTE: this case must work even if the ship has no target
			// (and changeStateIfApplicable should make sure we are never
			// in a state that won't work at the time)

			//#region PUT YOUR CODE HERE
			Random direction = new Random();
			int d = direction.nextInt(4) + 1;
			if (ticks % 30 == 0) {
				if (d == 1 && board.isSafeAt(theX + 1, theY)) {
					board.setGoal(theX + 1, theY);
					setGoal = true;
					prevGoal = new Pair(theX + 1, theY);
				}
				if (d == 2 && board.isSafeAt(theX - 1, theY)) {
					board.setGoal(theX - 1, theY);
					setGoal = true;
					prevGoal = new Pair(theX - 1, theY);
				}
				if (d == 3 && board.isSafeAt(theX, theY + 1)) {
					board.setGoal(theX, theY + 1);
					setGoal = true;
					prevGoal = new Pair(theX, theY + 1);
				}
				if (d == 4 && board.isSafeAt(theX, theY - 1)) {
					board.setGoal(theX, theY - 1);
					setGoal = true;
					prevGoal = new Pair(theX, theY - 1);

			}
				} else {
					if (board.isSafeAt(prevGoal.x, prevGoal.y)) {
						board.setGoal(prevGoal.x, prevGoal.y);
						setGoal = true;
					} else {
						if (d == 0 && board.isSafeAt(theX + 1, theY)) {
							board.setGoal(theX + 1, theY);
							setGoal = true;
							prevGoal = new Pair(theX + 1, theY);
						}
						if (d == 1 && board.isSafeAt(theX - 1, theY)) {
							board.setGoal(theX - 1, theY);
							setGoal = true;
							prevGoal = new Pair(theX - 1, theY);
						}
						if (d == 2 && board.isSafeAt(theX, theY + 1)) {
							board.setGoal(theX, theY + 1);
							setGoal = true;
							prevGoal = new Pair(theX, theY + 1);
						}
						if (d == 3 && board.isSafeAt(theX, theY - 1)) {
							board.setGoal(theX, theY - 1);
							setGoal = true;
							prevGoal = new Pair(theX, theY - 1);

						}
				}
			}

			//#endregion
			break;

		case CHASE: // Do not pre-empt with FSMState in a case
			// Insert code to mark tiles that will cause us to chase the target;
			// set setGoal to true if we marked any tiles.
			
			//#region PUT YOUR CODE HERE
			Random r1 = new Random();
			int r2 = r1.nextInt(2) + 1;

			if (r2 == 1) {
				if (board.isSafeAt(targetX + ATTACK_DIST - 1, targetY)) {
					board.setGoal(targetX + ATTACK_DIST - 1, targetY);
					setGoal = true;
				}
				if (board.isSafeAt(targetX - ATTACK_DIST + 1, targetY)) {
					board.setGoal(targetX - ATTACK_DIST + 1, targetY);
					setGoal = true;
				}
				if (board.isSafeAt(targetX, targetY - ATTACK_DIST + 1)) {
					board.setGoal(targetX, targetY - ATTACK_DIST + 1);
					setGoal = true;
				}
				if (board.isSafeAt(targetX, targetY + ATTACK_DIST - 1)) {
					board.setGoal(targetX, targetY + ATTACK_DIST - 1);
					setGoal = true;
				}
			} else {
				if (board.isSafeAt(targetX + 1, targetY)) {
					board.setGoal(targetX + 1, targetY);
					setGoal = true;
				}
				if (board.isSafeAt(targetX - 1, targetY)) {
					board.setGoal(targetX - 1, targetY);
					setGoal = true;
				}
				if (board.isSafeAt(targetX, targetY - 1)) {
					board.setGoal(targetX, targetY - 1);
					setGoal = true;
				}
				if (board.isSafeAt(targetX, targetY +  1)) {
					board.setGoal(targetX, targetY + 1);
					setGoal = true;
				}
			}
			//#endregion
			break;

		case ATTACK: // Do not pre-empt with FSMState in a case
			// Insert code here to mark tiles we can attack from, (see
			// canShootTargetFrom); set setGoal to true if we marked any tiles.

			//#region PUT YOUR CODE HERE
			Random r3 = new Random();
			int r4 = r3.nextInt(2) + 1;
				if (r4 == 1) {
					if (board.isSafeAt(targetX + ATTACK_DIST - 1, targetY)) {
						board.setGoal(targetX + ATTACK_DIST - 1, targetY);
						setGoal = true;
					}
					if (board.isSafeAt(targetX - ATTACK_DIST - 1, targetY)) {
						board.setGoal(targetX - ATTACK_DIST - 1, targetY);
						setGoal = true;
					}
					if (board.isSafeAt(targetX, targetY - ATTACK_DIST - 1)) {
						board.setGoal(targetX, targetY - ATTACK_DIST - 1);
						setGoal = true;
					}
					if (board.isSafeAt(targetX, targetY + ATTACK_DIST - 1)) {
						board.setGoal(targetX, targetY + ATTACK_DIST - 1);
						setGoal = true;
					}
				} else {
					if (board.isSafeAt(targetX + 1, targetY)) {
						board.setGoal(targetX + 1, targetY);
						setGoal = true;
					}
					if (board.isSafeAt(targetX - 1, targetY)) {
						board.setGoal(targetX - 1, targetY);
						setGoal = true;
					}
					if (board.isSafeAt(targetX, targetY - 1)) {
						board.setGoal(targetX, targetY - 1);
						setGoal = true;
					}
					if (board.isSafeAt(targetX, targetY + 1)) {
						board.setGoal(targetX, targetY + 1);
						setGoal = true;
					}
				}
			//#endregion
			break;
		}

		// If we have no goals, mark current position as a goal
		// so we do not spend time looking for nothing:
		if (!setGoal) {
			int sx = board.screenToBoard(ship.getX());
			int sy = board.screenToBoard(ship.getY());
			board.setGoal(sx, sy);
		}
	}

	private class Pair { //based on http://stackoverflow.com/questions/10234487/storing-number-pairs-in-java
		private int x;
		private int y;

		Pair(int x, int y) {
			this.x=x;this.y=y;
		}
	}

	/**
 	 * Returns a movement direction that moves towards a goal tile.
 	 *
 	 * This is one of the longest parts of the assignment. Implement
	 * breadth-first search (from 2110) to find the best goal tile
	 * to move to. However, just return the movement direction for
	 * the next step, not the entire path.
	 * 
	 * The value returned should be a control code.  See PlayerController
	 * for more information on how to use control codes.
	 *
 	 * @return a movement direction that moves towards a goal tile.
 	 */
	//Algorithm below was based on https://gist.github.com/gennad/791932 and
	// http://stackoverflow.com/questions/8922060/how-to-trace-the-path-in-a-breadth-first-search
	private int getMoveAlongPathToGoalTile() {
		//#region PUT YOUR CODE HERE
		Queue q = new LinkedList();
		int start_x = board.screenToBoard(ship.getPosition().x);
		int start_y = board.screenToBoard(ship.getPosition().y);
		board.setVisited(start_x, start_y);
		LinkedList<Pair> vlist;
		vlist = new LinkedList<Pair>();
		vlist.add(new Pair(start_x, start_y));
		q.add(vlist);
		while (!q.isEmpty()) {
			LinkedList<Pair> v2 = (LinkedList) q.remove();
			Pair last = v2.getLast();
			Queue<Pair> neighbors = getNeighbors(last);
			if (board.isGoal(last.x, last.y)) {
				this.finalpath = v2;
				break;
			}
			while (!neighbors.isEmpty()) {
				Pair check = neighbors.remove();
				board.setVisited(check.x,check.y);
				LinkedList<Pair> path = (LinkedList<Pair>)v2.clone();
				path.addLast(check);
				q.add(path);
			}
		}
		if (this.finalpath == null) {
			return CONTROL_NO_ACTION;
		}
		if (this.finalpath.size() == 1) {
			return CONTROL_NO_ACTION;
		}
		Pair wanted = finalpath.get(1);
		int the_move = findMove(new Pair(start_x, start_y), wanted);
		return the_move;
			//#endregion
	}

	// Add any auxiliary methods or data structures here
	//#region PUT YOUR CODE HERE
	/**
	 * Returns the safe and unvisited neighbors of a given tile position*/
	private Queue<Pair> getNeighbors(Pair tile) {
		Queue<Pair> neighbors = new LinkedList<Pair>();
			if (board.isSafeAt((int)(tile.x + 1),(int)(tile.y)) && !board.isVisited((int)(tile.x + 1),(int)(tile.y))) {
				neighbors.add(new Pair(tile.x +1, tile.y));
			}
			if (board.isSafeAt((int)(tile.x - 1),(int)(tile.y)) && !board.isVisited((int)(tile.x - 1),(int)(tile.y))) {
				neighbors.add(new Pair(tile.x - 1, tile.y));
			}
			if (board.isSafeAt((int)(tile.x),(int)(tile.y + 1)) && !board.isVisited((int)(tile.x),(int)(tile.y + 1))) {
				neighbors.add(new Pair(tile.x, tile.y + 1));
			}
			if (board.isSafeAt((int)(tile.x),(int)(tile.y - 1)) && !board.isVisited((int)(tile.x),(int)(tile.y - 1))) {
				neighbors.add(new Pair(tile.x, tile.y - 1));
			}
			return neighbors;
		}

	private int findMove(Pair start, Pair end) {
		if (end.x < start.x) {
			return CONTROL_MOVE_LEFT;
		}
		if (end.x > start.x) {
			return CONTROL_MOVE_RIGHT;
		}
		if (end.y < (int)start.y) {
			return CONTROL_MOVE_UP;
		}
		if (end.y > (int)start.y) {
			return CONTROL_MOVE_DOWN;
		}
		return CONTROL_NO_ACTION;
	}
	//#endregion
}