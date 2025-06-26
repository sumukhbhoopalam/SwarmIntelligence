import java.util.ArrayList;

//This file represents each agent in the swarm. It knows: Its own position, velocity, acceleration
//Whether it’s a normal, special red, yellow, or lavender vehicle.If it should flee or follow
//Who its neighbors are and how to behave around them
public class Vehicle {
	static int allId = 0;
	int id; 
	double rad_sep; 
	double rad_zus; 
	int type; 
	final double FZL; 
	final double FZB; 
	
	double[] pos; 
	double[] vel; 
	
	final double max_acc; 
	double max_vel; // Changed from final to allow modification for special vehicles

	// For special vehicle random walk
	private double wanderAngle = 2 * Math.PI * Math.random();

	boolean isFleeing = false; // Track if this vehicle is fleeing for panic propagation
	boolean isNew = false; // True for new vehicles spawned after 10 seconds
	public boolean isOrange = false; // True for orange vehicles spawned after yellow is deleted

	// Add these fields to Vehicle
	private double cohesionWeight = 0.3; // default values
	private double separationWeight = 0.2;
	private double alignmentWeight = 0.5;

	int generation = 1; // 1=black, 2=green, 3=orange

	Vehicle() {
		allId++;
		this.id = allId;
		this.FZL = 2;
		this.FZB = 1;
		this.rad_sep = 15; // Moderate separation radius
		this.rad_zus = 120; // Larger cohesion/alignment radius
		this.type = 0;
		this.max_acc = 0.05;// 0.1
		this.max_vel = 1; // Default velocity for swarm vehicles
		this.generation = 1; // Default to black

		pos = new double[2];
		vel = new double[2];
		// Spawn swarm vehicles in a tight cluster near the center
		if (this.type == 1) {
			// Special vehicle: anywhere in 1000x800 canvas
			pos[0] = 1000 * Simulation.pix * Math.random();
			pos[1] = 800 * Simulation.pix * Math.random();
			this.max_vel = 1.0; // Red special vehicle speed
		} else if (this.type == 2) {
			// Golden yellow special vehicle
			this.max_vel = 0.01; // Much slower than other vehicles
		} else if (this.type == 3) {
			// Lavender special vehicle: same size as other specials
			pos[0] = 1000 * Simulation.pix * Math.random();
			pos[1] = 800 * Simulation.pix * Math.random();
			this.max_vel = 0.1;
			// FZL and FZB are already set to 2 and 1, but drawing code multiplies by 6x for specials
		} else {
			// Swarm: cluster in center with small random offset
			double centerX = 1600 * Simulation.pix / 2.0;
			double centerY = 800 * Simulation.pix / 2.0;
			pos[0] = centerX + 40 * (Math.random() - 0.5); // +/-20px
			pos[1] = centerY + 40 * (Math.random() - 0.5); // +/-20px
		}
		// Random direction, full speed
		double angle = 2 * Math.PI * Math.random();
		vel[0] = max_vel * Math.cos(angle);
		vel[1] = max_vel * Math.sin(angle);
		this.isNew = false;
	}

	// Overloaded constructor to set generation
	Vehicle(int generation) {
		this();
		this.generation = generation;
	}

	//Finds all vehicles within a given radius and returns them as a list.
	//Used to determine which vehicles are nearby and how to interact with them.
	ArrayList<Vehicle> neighbours(ArrayList<Vehicle> all, double radius1, double radius2) {
		ArrayList<Vehicle> neighbours = new ArrayList<Vehicle>();
		for (int i = 0; i < all.size(); i++) {
			Vehicle v = all.get(i);
			if (v.id != this.id) {
				double dist = Math.sqrt(Math.pow(v.pos[0] - this.pos[0], 2) + Math.pow(v.pos[1] - this.pos[1], 2));
				if (dist >= radius1 && dist < radius2) {
					neighbours.add(v);
				}
			}
		}
		return neighbours;
	}

	//Calculates the acceleration needed to reach a desired velocity.
	//Used to adjust vehicle speed and direction based on its neighbors.
	double[] calculateAcc(double[] vel_dest) {
		double[] acc_dest = new double[2];

		vel_dest = VectorCalculation.normalize(vel_dest);
		vel_dest[0] = vel_dest[0] * max_vel;
		vel_dest[1] = vel_dest[1] * max_vel;

		acc_dest[0] = vel_dest[0] - vel[0];
		acc_dest[1] = vel_dest[1] - vel[1];

		return acc_dest;
	}

	//Calculates the acceleration needed to maintain cohesion with nearby vehicles.
	//Used to keep the swarm together and moving in the same direction.
	double[] cohesion(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours;
		
		double[] pos_dest = new double[2];
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		neighbours = neighbours(all, rad_sep, rad_zus);

		if (neighbours.size() > 0) {
			pos_dest[0] = 0;
			pos_dest[1] = 0;
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v = neighbours.get(i);
				pos_dest[0] = pos_dest[0] + v.pos[0];
				pos_dest[1] = pos_dest[1] + v.pos[1];
			}
			pos_dest[0] = pos_dest[0] / neighbours.size();
			pos_dest[1] = pos_dest[1] / neighbours.size();

			vel_dest[0] = pos_dest[0] - pos[0];
			vel_dest[1] = pos_dest[1] - pos[1];

			acc_dest = calculateAcc(vel_dest);
		}
		return acc_dest;
	}

	//Calculates the acceleration needed to maintain separation from nearby vehicles.
	//Used to prevent vehicles from crowding each other.
	double[] separation(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours;
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		neighbours  = neighbours(all, 0, rad_sep);

		if (neighbours.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v    = neighbours.get(i);
				double[] vel = new double[2];
				double dist;

				vel[0] = v.pos[0] - pos[0];
				vel[1] = v.pos[1] - pos[1];
				
				dist   = rad_sep  - VectorCalculation.length(vel);
				if (dist < 0)System.out.println("mistake in rad");
				vel = VectorCalculation.normalize(vel);
				vel[0] = -vel[0] * dist;
				vel[1] = -vel[1] * dist;
				
				vel_dest[0] = vel_dest[0] + vel[0];
				vel_dest[1] = vel_dest[1] + vel[1];
			}

			acc_dest = calculateAcc(vel_dest);
		}

		return acc_dest;
	}

	//Calculates the acceleration needed to maintain alignment with nearby vehicles.
	//Used to keep the swarm moving in the same direction as its neighbors.

	double[] alignment(ArrayList<Vehicle> all) {
		/* Ivan Perko, 12.12.2022
		 * vel_dest[0] = neighbours.stream().mapToDouble(n -> n.vel[0]).average().getAsDouble();
           vel_dest[1] = neighbours.stream().mapToDouble(n -> n.vel[1]).average().getAsDouble();
		 */
		
		
		ArrayList<Vehicle> neighbours = new ArrayList<Vehicle>();
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];
		acc_dest[0] = 0;
		acc_dest[1] = 0;

		neighbours = neighbours(all, 0, rad_zus);


		if (neighbours.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v = neighbours.get(i);
				vel_dest[0] = vel_dest[0] + v.vel[0];
				vel_dest[1] = vel_dest[1] + v.vel[1];
			}
			
			acc_dest = calculateAcc(vel_dest);
		}

		return acc_dest;
	}

	//Generates random acceleration for vehicles that need a little extra push.
	//Used to add some natural variation to the swarm's movement.
	double[] random() {
		double[] acc_dest = new double[2];
		acc_dest[0] = 0;
		acc_dest[1] = 0;

		if (Math.random() < 0.01) {
			acc_dest[0] = max_acc * Math.random();
			acc_dest[1] = max_acc * Math.random();
		}

		return acc_dest;
	}

	//Calculates the total acceleration for a vehicle based on its current state and the 
	//behaviors of its neighbors. Used to determine how the vehicle should move.
	public double[] calculateWeightedAcc(ArrayList<Vehicle> allVehicles) {
		double[] acc_dest  = new double[2];
		double[] acc_dest1 = new double[2];
		double[] acc_dest2 = new double[2];
		double[] acc_dest3 = new double[2];
		double[] acc_flee  = new double[2];
		double[] acc_rand  = new double[2];
		double f_zus = this.cohesionWeight;
		double f_sep = this.separationWeight;
		double f_aus = this.alignmentWeight;
		double f_flee = 8.0;   // Flee force unchanged
		double f_rand = 0.15;  // Much less random force
		double fleeRadius = 0;
		for (Vehicle v : allVehicles) {
			if (v == null) continue;
			if (v.type == 1 || v.type == 2) {
				fleeRadius = Math.max(70, v.FZL * 12.0); // Restore original logic
				break;
			}
		}
		double panicRadius = 0; // Much larger panic effect for full swarm propagation

		Vehicle special = null;
		Vehicle goldenSpecial = null;
		Vehicle lavenderSpecial = null;
		for (Vehicle v : allVehicles) {
			if (v == null) continue;
			if (v.type == 1) {
				special = v;
			} else if (v.type == 2) {
				goldenSpecial = v;
			} else if (v.type == 3) {
				lavenderSpecial = v;
			}
		}
		if (type == 1) {
			isFleeing = false;
			acc_dest = chaseSwarm(allVehicles);
		} else if (type == 2) {
			isFleeing = false;
			acc_dest = goldenVehicleBehavior(allVehicles);
		} else if (isNew && goldenSpecial != null) {
			// Brown vehicles: follow yellow special vehicle as a swarm only if within vicinity, else brown-only swarm
			double dx = pos[0] - goldenSpecial.pos[0];
			double dy = pos[1] - goldenSpecial.pos[1];
			double dist = Math.sqrt(dx * dx + dy * dy);
			double vicinityRadius = 300;
			if (dist < vicinityRadius) {
				acc_dest = followGoldenVehicle(allVehicles, goldenSpecial);
			} else {
				acc_dest1 = cohesionWithBrownVehicles(allVehicles);
				acc_dest2 = separationWithBrownVehicles(allVehicles);
				acc_dest3 = alignmentWithBrownVehicles(allVehicles);
				acc_rand = randomSmall();
				acc_dest[0] = (f_zus * acc_dest1[0]) + (f_sep * acc_dest2[0]) + (f_aus * acc_dest3[0]) + (f_rand * acc_rand[0]);
				acc_dest[1] = (f_zus * acc_dest1[1]) + (f_sep * acc_dest2[1]) + (f_aus * acc_dest3[1]) + (f_rand * acc_rand[1]);
			}
			isFleeing = false;
		} else if (lavenderSpecial != null) {
			// All vehicles (black and brown) surround lavender vehicle
			acc_dest = surroundLavenderVehicle(allVehicles, lavenderSpecial);
			isFleeing = false;
		} else {
			// Check if this vehicle should flee from red or yellow special vehicle
			boolean shouldFlee = false;
			if (special != null) {
				double dx = pos[0] - special.pos[0];
				double dy = pos[1] - special.pos[1];
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist < fleeRadius) {
					shouldFlee = true;
				}
			}
			// If yellow special vehicle exists and this is a black vehicle, always flee
			if (goldenSpecial != null && !isNew) {
				shouldFlee = true;
			}
			// Panic propagation: if any other vehicle is fleeing and close, also flee
			if (!shouldFlee) {
				for (Vehicle v : allVehicles) {
					if (v == null || v == this || v.type == 1 || v.type == 2) continue;
					if (v.isFleeing) {
						double vdx = v.pos[0] - pos[0];
						double vdy = v.pos[1] - pos[1];
						double vdist = Math.sqrt(vdx * vdx + vdy * vdy);
						if (vdist < panicRadius) {
							shouldFlee = true;
							break;
						}
					}
				}
			}
			isFleeing = shouldFlee;
			if (shouldFlee) {
				acc_flee = fleeFromSpecialVehicle(allVehicles, fleeRadius);
				acc_dest1 = cohesion(allVehicles);
				acc_dest2 = separation(allVehicles);
				acc_dest3 = alignment(allVehicles);
				acc_rand = randomSmall();
				// Blend flee with swarm rules (strongest flee force, reduced swarm influence)
				double fleeWeight = 50.0;   // Even stronger flee force
				double zusWeight = 0.05;    // Lower cohesion during panic
				double sepWeight = 0;     // Lower separation during panic
				double ausWeight = 0.05;     // Lower alignment during panic
				double randWeight = 0;   // Lower randomness during panic
				acc_dest[0] = (fleeWeight * acc_flee[0]) + (zusWeight * acc_dest1[0]) + (sepWeight * acc_dest2[0]) + (ausWeight * acc_dest3[0]) + (randWeight * acc_rand[0]);
				acc_dest[1] = (fleeWeight * acc_flee[1]) + (zusWeight * acc_dest1[1]) + (sepWeight * acc_dest2[1]) + (ausWeight * acc_dest3[1]) + (randWeight * acc_rand[1]);
			} else if (goldenSpecial != null) {
				// Check if this vehicle is in the vicinity of the golden yellow vehicle
				double dx = pos[0] - goldenSpecial.pos[0];
				double dy = pos[1] - goldenSpecial.pos[1];
				double dist = Math.sqrt(dx * dx + dy * dy);
				double vicinityRadius = 300; // Increased radius to ensure all vehicles can reach the golden vehicle
				
				if (dist < vicinityRadius) {
					if (isNew) {
						// Brown vehicles (newly created) follow the golden yellow vehicle as a swarm
						acc_dest = followGoldenVehicle(allVehicles, goldenSpecial);
					} else {
						// Black vehicles (original) flee from the golden yellow vehicle
						acc_flee = fleeFromSpecialVehicle(allVehicles, fleeRadius);
						acc_dest1 = cohesion(allVehicles);
						acc_dest2 = separation(allVehicles);
						acc_dest3 = alignment(allVehicles);
						acc_rand = randomSmall();
						// Blend flee with swarm rules
						double fleeWeight = 50.0;   // Strong flee force
						double zusWeight = 0.05;    // Lower cohesion during panic
						double sepWeight = 0;     // Lower separation during panic
						double ausWeight = 0.05;     // Lower alignment during panic
						double randWeight = 0;   // Lower randomness during panic
						acc_dest[0] = (fleeWeight * acc_flee[0]) + (zusWeight * acc_dest1[0]) + (sepWeight * acc_dest2[0]) + (ausWeight * acc_dest3[0]) + (randWeight * acc_rand[0]);
						acc_dest[1] = (fleeWeight * acc_flee[1]) + (zusWeight * acc_dest1[1]) + (sepWeight * acc_dest2[1]) + (ausWeight * acc_dest3[1]) + (randWeight * acc_rand[1]);
					}
				} else {
					// Normal swarm behavior when not in vicinity
					acc_dest1 = cohesion(allVehicles);
					acc_dest2 = separation(allVehicles);
					acc_dest3 = alignment(allVehicles);
					acc_rand = randomSmall();
					acc_dest[0] = (f_zus * acc_dest1[0]) + (f_sep * acc_dest2[0]) + (f_aus * acc_dest3[0]) + (f_rand * acc_rand[0]);
					acc_dest[1] = (f_zus * acc_dest1[1]) + (f_sep * acc_dest2[1]) + (f_aus * acc_dest3[1]) + (f_rand * acc_rand[1]);
				}
			} else {
				acc_dest1 = cohesion(allVehicles);
				acc_dest2 = separation(allVehicles);
				acc_dest3 = alignment(allVehicles);
				acc_rand = randomSmall();
				acc_dest[0] = (f_zus * acc_dest1[0]) + (f_sep * acc_dest2[0]) + (f_aus * acc_dest3[0]) + (f_rand * acc_rand[0]);
				acc_dest[1] = (f_zus * acc_dest1[1]) + (f_sep * acc_dest2[1]) + (f_aus * acc_dest3[1]) + (f_rand * acc_rand[1]);
			}
		}
		acc_dest = VectorCalculation.truncate(acc_dest, max_acc);
		return acc_dest;
	}

	//Generates a small random vector for swarm vehicles.
	//Used to add some natural variation to the swarm's movement.
	private double[] randomSmall() {
		double[] acc = new double[2];
		double angle = 2 * Math.PI * Math.random();
		double mag = 1.0 + 2.0 * Math.random(); // 1.0 to 3.0 (more variable)
		// Add a small bias to change direction more often
		if (Math.random() < 0.2) {
			angle += (Math.random() - 0.5) * Math.PI;
		}
		acc[0] = Math.cos(angle) * mag;
		acc[1] = Math.sin(angle) * mag;
		return acc;
	}

	//Calculates the acceleration needed to chase a special vehicle.
	//Used to keep the swarm together and moving in the same direction as the special vehicle.
	private double[] chaseSwarm(ArrayList<Vehicle> allVehicles) {
		double[] acc = new double[2];
		acc[0] = 0;
		acc[1] = 0;
		
		// Find the nearest swarm vehicle or center of mass of nearby swarm vehicles
		double nearestDist = Double.MAX_VALUE;
		Vehicle nearest = null;
		double centerX = 0, centerY = 0;
		int nearbyCount = 0;
		double detectionRadius = 200; // Radius to detect swarm vehicles
		
		for (Vehicle v : allVehicles) {
			if (v == null || v.type != 0) continue; // Only consider swarm vehicles
			double dx = v.pos[0] - pos[0];
			double dy = v.pos[1] - pos[1];
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			// Track nearest vehicle
			if (dist < nearestDist) {
				nearestDist = dist;
				nearest = v;
			}
			
			// Calculate center of mass of nearby vehicles
			if (dist < detectionRadius) {
				centerX += v.pos[0];
				centerY += v.pos[1];
				nearbyCount++;
			}
		}
		
		if (nearest != null) {
			if (nearbyCount > 0) {
				// Move toward center of mass of nearby vehicles
				centerX /= nearbyCount;
				centerY /= nearbyCount;
				double dx = centerX - pos[0];
				double dy = centerY - pos[1];
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist > 0.01) {
					acc[0] = (dx / dist) * max_acc * 2.0; // Stronger chase force
					acc[1] = (dy / dist) * max_acc * 2.0;
				}
			} else {
				// Move toward nearest vehicle if no nearby ones
				double dx = nearest.pos[0] - pos[0];
				double dy = nearest.pos[1] - pos[1];
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist > 0.01) {
					acc[0] = (dx / dist) * max_acc * 2.0;
					acc[1] = (dy / dist) * max_acc * 2.0;
				}
			}
		}
		
		// Add some edge avoidance
		double margin = 100;
		if (pos[0] < margin) acc[0] += 2.0;
		if (pos[0] > 1000 * Simulation.pix - margin) acc[0] -= 2.0;
		if (pos[1] < margin) acc[1] += 2.0;
		if (pos[1] > 800 * Simulation.pix - margin) acc[1] -= 2.0;
		
		return acc;
	}

	//Calculates the acceleration needed to maintain a golden yellow special vehicle.
	//Used to keep the swarm together and moving in the same direction as the golden vehicle.
	private double[] goldenVehicleBehavior(ArrayList<Vehicle> allVehicles) {
		double[] acc = new double[2];
		acc[0] = 0;
		acc[1] = 0;
		
		// Golden yellow vehicle moves around the canvas
		// Add some random movement with edge avoidance
		double margin = 100;
		if (pos[0] < margin) acc[0] += 1.0;
		if (pos[0] > 1000 * Simulation.pix - margin) acc[0] -= 1.0;
		if (pos[1] < margin) acc[1] += 1.0;
		if (pos[1] > 800 * Simulation.pix - margin) acc[1] -= 1.0;
		
		// Add some random movement
		if (Math.random() < 0.02) { // 2% chance each frame
			double angle = 2 * Math.PI * Math.random();
			acc[0] += Math.cos(angle) * max_acc * 0.5;
			acc[1] += Math.sin(angle) * max_acc * 0.5;
		}
		
		return acc;
	}

	// Behavior for swarm vehicles to touch the golden yellow special vehicle
	private double[] surroundGoldenVehicle(ArrayList<Vehicle> allVehicles, Vehicle goldenSpecial) {
		double[] acc = new double[2];
		acc[0] = 0;
		acc[1] = 0;
		
		// Calculate distance to golden special vehicle
		double dx = goldenSpecial.pos[0] - pos[0];
		double dy = goldenSpecial.pos[1] - pos[1];
		double dist = Math.sqrt(dx * dx + dy * dy);
		
		// Contact threshold - when vehicles are close enough to touch
		double contactThreshold = (FZL + goldenSpecial.FZL) * 4.0; // Increased to 4x the combined vehicle sizes
		
		// If vehicle is already touching the golden vehicle, move with it
		if (dist < contactThreshold) {
			// Vehicle is touching, move with the golden vehicle
			acc[0] = goldenSpecial.vel[0] * 0.8; // Move at 80% of golden vehicle's speed
			acc[1] = goldenSpecial.vel[1] * 0.8;
			return acc;
		}
		
		// Move toward the golden vehicle to touch it
		if (dist > 0.01) {
			acc[0] = (dx / dist) * max_acc * 1.0;
			acc[1] = (dy / dist) * max_acc * 1.0;
		}
		
		// Once touching, move with the golden vehicle
		return acc;
	}

	// Behavior for brown vehicles (newly created) to follow the golden yellow vehicle as a swarm
	private double[] followGoldenVehicle(ArrayList<Vehicle> allVehicles, Vehicle goldenSpecial) {
		double[] acc = new double[2];
		double[] acc_dest1 = new double[2];
		double[] acc_dest2 = new double[2];
		double[] acc_dest3 = new double[2];
		double[] acc_rand = new double[2];
		acc[0] = 0;
		acc[1] = 0;
		
		// Calculate distance to golden special vehicle
		double dx = goldenSpecial.pos[0] - pos[0];
		double dy = goldenSpecial.pos[1] - pos[1];
		double dist = Math.sqrt(dx * dx + dy * dy);
		
		// Target radius for following (maintain distance around the golden vehicle)
		double targetRadius = 60; // Closer distance for following
		double tolerance = 15; // Tolerance for being in position
		
		// If vehicle is already at the target radius, maintain position with golden vehicle
		if (Math.abs(dist - targetRadius) < tolerance) {
			// Move with the golden vehicle while maintaining distance
			acc[0] = goldenSpecial.vel[0] * 0.9; // Move at 90% of golden vehicle's speed
			acc[1] = goldenSpecial.vel[1] * 0.9;
		} else {
			// Move toward the target radius around the golden vehicle
			double desiredX = goldenSpecial.pos[0] + targetRadius * (dx / dist);
			double desiredY = goldenSpecial.pos[1] + targetRadius * (dy / dist);
			
			double accX = desiredX - pos[0];
			double accY = desiredY - pos[1];
			double accDist = Math.sqrt(accX * accX + accY * accY);
			
			if (accDist > 0.01) {
				acc[0] = (accX / accDist) * max_acc * 1.2;
				acc[1] = (accY / accDist) * max_acc * 1.2;
			}
		}
		
		// Add swarm behavior with other brown vehicles only
		acc_dest1 = cohesionWithBrownVehicles(allVehicles);
		acc_dest2 = separationWithBrownVehicles(allVehicles);
		acc_dest3 = alignmentWithBrownVehicles(allVehicles);
		acc_rand = randomSmall();
		
		// Blend following behavior with swarm rules
		double followWeight = 2.0;
		double zusWeight = 0.6;
		double sepWeight = 0.4;
		double ausWeight = 0.3;
		double randWeight = 0.1;
		
		acc[0] = followWeight * acc[0] + zusWeight * acc_dest1[0] + sepWeight * acc_dest2[0] + ausWeight * acc_dest3[0] + randWeight * acc_rand[0];
		acc[1] = followWeight * acc[1] + zusWeight * acc_dest1[1] + sepWeight * acc_dest2[1] + ausWeight * acc_dest3[1] + randWeight * acc_rand[1];
		
		return acc;
	}

	//Updates the vehicle's position and velocity based on its current state and the 
	//behaviors of its neighbors. Used to move the vehicle around the canvas.
	void move(ArrayList<Vehicle> allVehicles) {
		double[] acc = calculateWeightedAcc(allVehicles);
	
		// Check if this vehicle is touching the golden yellow vehicle and should move with it
		boolean shouldMoveWithGolden = false;
		if (type == 0) { // Only check for swarm vehicles
			for (Vehicle v : allVehicles) {
				if (v == null || v.type != 2) continue; // Golden yellow vehicle
				double dx = pos[0] - v.pos[0];
				double dy = pos[1] - v.pos[1];
				double dist = Math.sqrt(dx * dx + dy * dy);
				double contactThreshold = (FZL + v.FZL) * 4.0; // Increased to 4x the combined vehicle sizes
				
				// If within vicinity and touching, move with the golden vehicle (only for brown vehicles)
				if (dist < 300 && dist < contactThreshold && isNew) {
					shouldMoveWithGolden = true;
					break;
				}
			}
		}
		
		// If should move with golden vehicle, use the calculated acceleration
		if (shouldMoveWithGolden) {
			vel[0] = vel[0] + acc[0];
			vel[1] = vel[1] + acc[1];
			vel    = VectorCalculation.normalize(vel);
			vel[0] = vel[0] * max_vel;
			vel[1] = vel[1] * max_vel;

			pos[0] = pos[0] + vel[0];
			pos[1] = pos[1] + vel[1];

			position_Box();
			return;
		}
		
		// Normal movement for vehicles not touching the golden vehicle
		vel[0] = vel[0] + acc[0];
		vel[1] = vel[1] + acc[1];
		vel    = VectorCalculation.normalize(vel);
		vel[0] = vel[0] * max_vel;
		vel[1] = vel[1] * max_vel;

		pos[0] = pos[0] + vel[0];
		pos[1] = pos[1] + vel[1];

		position_Box();
	}

	public void position_Box() {
		if (pos[0] < 10) {
			vel[0] = Math.abs(vel[0]);
			pos[0] = pos[0] + vel[0];
		}
		if (pos[0] > 1000 * Simulation.pix) {
			vel[0] = -Math.abs(vel[0]);
			pos[0] = pos[0] + vel[0];
		}
		if (pos[1] < 10) {
			vel[1] = Math.abs(vel[1]);
			pos[1] = pos[1] + vel[1];
		}
		if (pos[1] > 700 * Simulation.pix) {
			vel[1] = -Math.abs(vel[1]);
			pos[1] = pos[1] + vel[1];
		}
	}
	
	//Calculates the acceleration for a vehicle to follow a special vehicle. Used for leader-follower dynamics
	double[] follow(ArrayList<Vehicle> all) {
		double[] pos_dest = new double[2];
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];
		acc_dest[0] = 0;
		acc_dest[1] = 0;
		Vehicle v = null;

		if (type == 0) {
			for (int i = 0; i < all.size(); i++) {
				v = all.get(i);
				if (v == null || v.type != 1)
					continue;
				break;
			}
			double dist = Math.sqrt(Math.pow(v.pos[0] - this.pos[0], 2) + Math.pow(v.pos[1] - this.pos[1], 2));

			if (dist < rad_zus && inFront(v)) {
				double[] pkt = new double[2];
				double[] ort1 = new double[2];
				double[] ort2 = new double[2];
				double[] ort3 = new double[2];
				pkt[0] = pos[0];
				pkt[1] = pos[1];
				ort1[0] = v.pos[0];
				ort1[1] = v.pos[1];
				ort2[0] = v.pos[0] + (rad_zus * v.vel[0]);
				ort2[1] = v.pos[1] + (rad_zus * v.vel[1]);
				ort3 = VectorCalculation.dist(pkt, ort1, ort2);

				vel_dest[0] = pos[0] - ort3[0];// UUU
				vel_dest[1] = pos[1] - ort3[1];// III

				vel_dest = VectorCalculation.normalize(vel_dest);
				vel_dest[0] = vel_dest[0] * max_vel;
				vel_dest[1] = vel_dest[1] * max_vel;

				acc_dest[0] = vel_dest[0] - vel[0];
				acc_dest[1] = vel_dest[1] - vel[1];
			} else if (dist < rad_zus && !inFront(v)) {
				pos_dest[0] = v.pos[0] + v.vel[0];
				pos_dest[1] = v.pos[1] + v.vel[0];
				vel_dest[0] = pos_dest[0] - pos[0];
				vel_dest[1] = pos_dest[1] - pos[1];
				vel_dest = VectorCalculation.normalize(vel_dest);
				vel_dest[0] = vel_dest[0] * max_vel;
				vel_dest[1] = vel_dest[1] * max_vel;
				acc_dest[0] = vel_dest[0] - vel[0];
				acc_dest[1] = vel_dest[1] - vel[1];
			} else {
				acc_dest = cohesion(all);
			}
		}

		return acc_dest;
	}

	//Checks if a vehicle is in front of another vehicle. 
	boolean inFront(Vehicle v) {
		//
		boolean erg = false;
		double[] tmp = new double[2];
		tmp[0] = pos[0] - v.pos[0];
		tmp[1] = pos[1] - v.pos[1];

		if (VectorCalculation.angle(tmp, v.vel) < Math.PI / 2)
			erg = true;
		else
			erg = false;

		return erg;
	}

	// Flee from the special vehicle (type 1 or 2) if within a certain radius
	private double[] fleeFromSpecialVehicle(ArrayList<Vehicle> allVehicles, double fleeRadius) {
		double[] acc_flee = new double[2];
		acc_flee[0] = 0;
		acc_flee[1] = 0;
		Vehicle special = null;
		for (Vehicle v : allVehicles) {
			if (v == null || v.type != 1 && v.type != 2) continue;
			special = v;
			break;
		}
		if (special != null) {
			double dx = pos[0] - special.pos[0];
			double dy = pos[1] - special.pos[1];
			double dist = Math.sqrt(dx * dx + dy * dy);
			if (dist < fleeRadius && dist > 0.01) {
				// Flee direction is away from the special vehicle, always at max_acc
				acc_flee[0] = dx / dist * max_acc;
				acc_flee[1] = dy / dist * max_acc;
			}
		}
		return acc_flee;
	}

	// Cohesion behavior for brown vehicles - only with other brown vehicles
	private double[] cohesionWithBrownVehicles(ArrayList<Vehicle> allVehicles) {
		ArrayList<Vehicle> brownNeighbors = new ArrayList<Vehicle>();
		double[] pos_dest = new double[2];
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		
		// Find other brown vehicles within cohesion radius
		for (Vehicle v : allVehicles) {
			if (v == null || v.type != 0 || !v.isNew) continue; // Only consider other brown vehicles
			
			double dx = v.pos[0] - pos[0];
			double dy = v.pos[1] - pos[1];
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			if (dist >= rad_sep && dist < rad_zus) {
				brownNeighbors.add(v);
			}
		}

		if (brownNeighbors.size() > 0) {
			pos_dest[0] = 0;
			pos_dest[1] = 0;
			for (Vehicle v : brownNeighbors) {
				pos_dest[0] = pos_dest[0] + v.pos[0];
				pos_dest[1] = pos_dest[1] + v.pos[1];
			}
			pos_dest[0] = pos_dest[0] / brownNeighbors.size();
			pos_dest[1] = pos_dest[1] / brownNeighbors.size();

			vel_dest[0] = pos_dest[0] - pos[0];
			vel_dest[1] = pos_dest[1] - pos[1];

			acc_dest = calculateAcc(vel_dest);
		}
		return acc_dest;
	}

	// Separation behavior for brown vehicles - only with other brown vehicles
	private double[] separationWithBrownVehicles(ArrayList<Vehicle> allVehicles) {
		ArrayList<Vehicle> brownNeighbors = new ArrayList<Vehicle>();
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		
		// Find other brown vehicles within separation radius
		for (Vehicle v : allVehicles) {
			if (v == null || v.type != 0 || !v.isNew) continue; // Only consider other brown vehicles
			
			double dx = v.pos[0] - pos[0];
			double dy = v.pos[1] - pos[1];
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			if (dist < rad_sep) {
				brownNeighbors.add(v);
			}
		}

		if (brownNeighbors.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (Vehicle v : brownNeighbors) {
				double[] vel = new double[2];
				double dist;

				vel[0] = v.pos[0] - pos[0];
				vel[1] = v.pos[1] - pos[1];
				
				dist = rad_sep - VectorCalculation.length(vel);
				if (dist < 0) System.out.println("mistake in rad");
				vel = VectorCalculation.normalize(vel);
				vel[0] = -vel[0] * dist;
				vel[1] = -vel[1] * dist;
				
				vel_dest[0] = vel_dest[0] + vel[0];
				vel_dest[1] = vel_dest[1] + vel[1];
			}

			acc_dest = calculateAcc(vel_dest);
		}

		return acc_dest;
	}

	// Alignment behavior for brown vehicles - only with other brown vehicles
	private double[] alignmentWithBrownVehicles(ArrayList<Vehicle> allVehicles) {
		ArrayList<Vehicle> brownNeighbors = new ArrayList<Vehicle>();
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];
		
		acc_dest[0] = 0;
		acc_dest[1] = 0;

		// Find other brown vehicles within alignment radius
		for (Vehicle v : allVehicles) {
			if (v == null || v.type != 0 || !v.isNew) continue; // Only consider other brown vehicles
			
			double dx = v.pos[0] - pos[0];
			double dy = v.pos[1] - pos[1];
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			if (dist < rad_zus) {
				brownNeighbors.add(v);
			}
		}

		if (brownNeighbors.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (Vehicle v : brownNeighbors) {
				vel_dest[0] = vel_dest[0] + v.vel[0];
				vel_dest[1] = vel_dest[1] + v.vel[1];
			}
			
			acc_dest = calculateAcc(vel_dest);
		}

		return acc_dest;
	}

	// Behavior for all vehicles to surround the lavender special vehicle
	private double[] surroundLavenderVehicle(ArrayList<Vehicle> allVehicles, Vehicle lavenderSpecial) {
		double[] acc = new double[2];
		acc[0] = 0;
		acc[1] = 0;
		// Calculate distance to lavender special vehicle
		double dx = lavenderSpecial.pos[0] - pos[0];
		double dy = lavenderSpecial.pos[1] - pos[1];
		double dist = Math.sqrt(dx * dx + dy * dy);
		// Contact threshold - when vehicles are close enough to touch
		double contactThreshold = (FZL + lavenderSpecial.FZL) * 4.0;
		// If vehicle is already touching the lavender vehicle, move with it
		if (dist < contactThreshold) {
			acc[0] = lavenderSpecial.vel[0] * 0.8;
			acc[1] = lavenderSpecial.vel[1] * 0.8;
			return acc;
		}
		// Move toward the lavender vehicle to touch it
		if (dist > 0.01) {
			acc[0] = (dx / dist) * max_acc * 1.0;
			acc[1] = (dy / dist) * max_acc * 1.0;
		}
		return acc;
	}

	// Add setters
	public void setCohesionWeight(double w) { this.cohesionWeight = w; }
	public void setSeparationWeight(double w) { this.separationWeight = w; }
	public void setAlignmentWeight(double w) { this.alignmentWeight = w; }

	public double getCohesionWeight() { return this.cohesionWeight; }
	public double getAlignmentWeight() { return this.alignmentWeight; }
	public double getSeparationWeight() { return this.separationWeight; }

	public void setGeneration(int generation) { this.generation = generation; }
	public int getGeneration() { return this.generation; }
}
