Name: Nathaniel Kaplan (nak74)
1/8/17

Each function required is described below.
-------------------------------------------
changeStateIfApplicable:

Spawn immidietly switches to the Wander state.

Wander switches to chase if the ship's target is within chasing distance. A ship will attempt to change its target every 5 seconds.

Chase switches to attack if the ship is able to hit its target. It wil go back to wandering if it's target leaves the ship's chasing distance area. A ship that is chasing will also attempt to change its target every 10 seconds to make it unpredictable.

Attack switches to chase if the ship cannot shoot. An attacking ship will also attempt to change its target every 5 seconds--since if it hasn't killed it's target in 5 seconds, it should probably try elsewhere (it also continues to add unpredictability)

-------------------------------------------

selectTarget:

A ship is initialized with a random target. If this function is called again, if there are more than 5 players, the ship tries to target another active ship. If there are fewer than 5 players remaining, if this function is called, the ship immidietly targets the player and will continue to do so for the rest of the game, making the game more difficult as it progresses.

-------------------------------------------

canShootTargetFrom:

Checks to see if a ship is within attacking distance (checks 4 squares if the ship is not on a power tile, 8 if it is).

-------------------------------------------

canShootTarget:

Same as canshootTargetFrom, but also checks of the ship can currently fire.

-------------------------------------------

markGoalTiles:

No goal tiles are marked when the ship spawns.

A random direction will be chosen for a wandering ship--this direction changes every half second (on ticks that aren't the half-second mark, the ship follows "prevGoal", which is the goal it picked from the random direction that last time the half-second tick was reached). A ship that tries to pick an inactive tile as a goal tile will not pick a goal at all.

A chasing ship will randomly select one of two sets of goals--right next to the target, or the exact attack distance away from the target. It will thus switch between getting in a target's face and keeping its distance while still attacking. This changing goal system also happens to result in behavior that causes ships to shoot other ships off the board more often, not just hit them (since a ship will constantly move around its target)

An attacking ship will do exactly the same thing as a chasing ship.

-------------------------------------------

getMoveAlongPathToGoalTile() 

I used breadth first search, as instructed. The big problem here was figuring out the path that the search took to reach the goal, and obtaining the first step of that path. To do this, I stored lists that represented paths inside my queue instead of tiles, and so once the algorithm was complete, I could simply look at the path that the algorithm returned, and use the first step of it (second index, since the first index is the start location). I created a new Pair class to handle tile locations more easily.

-------------------------------------------

HELPER METHODS:

inChase: Checks to see if a target is in chasing range.

class Pair: Has two fields, an x and a y, both ints. Used for the BFS algorithm to help handle tile locations.

getNeighbors: Returns the safe and unvisited neighbors of a given tile position

findMove: Returns the move the ship should make given a start and an end location--used for determining the actual move the ship should make after the BFS algorithm has run. It returns CONTROL_NO_ACTION if the start and end location are the same.

-------------

OTHER CHANGES:

I changes getAction's tick calculation by removing shipId from it, since it was messing with my other tick calculations.
