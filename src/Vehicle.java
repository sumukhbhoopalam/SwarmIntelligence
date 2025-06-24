import java.util.ArrayList;

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
	final double max_vel; 

	// For special vehicle random walk
	private double wanderAngle = 2 * Math.PI * Math.random();

	boolean isFleeing = false; // Track if this vehicle is fleeing for panic propagation
	boolean isNew = false; // True for new vehicles spawned after 10 seconds

	Vehicle() {
		allId++;
		this.id = allId;
		this.FZL = 2;
		this.FZB = 1;
		this.rad_sep = 15; // Moderate separation radius
		this.rad_zus = 120; // Larger cohesion/alignment radius
		this.type = 0;
		this.max_acc = 0.05;// 0.1
		this.max_vel = 1;

		pos = new double[2];
		vel = new double[2];
		// Spawn swarm vehicles in a tight cluster near the center
		if (this.type == 1) {
			// Special vehicle: anywhere in 1000x800 canvas
			pos[0] = 1000 * Simulation.pix * Math.random();
			pos[1] = 800 * Simulation.pix * Math.random();
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

	double[] calculateAcc(double[] vel_dest) {
		double[] acc_dest = new double[2];

		vel_dest = VectorCalculation.normalize(vel_dest);
		vel_dest[0] = vel_dest[0] * max_vel;
		vel_dest[1] = vel_dest[1] * max_vel;

		acc_dest[0] = vel_dest[0] - vel[0];
		acc_dest[1] = vel_dest[1] - vel[1];

		return acc_dest;
	}


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

	public double[] calculateWeightedAcc(ArrayList<Vehicle> allVehicles) {
		double[] acc_dest  = new double[2];
		double[] acc_dest1 = new double[2];
		double[] acc_dest2 = new double[2];
		double[] acc_dest3 = new double[2];
		double[] acc_flee  = new double[2];
		double[] acc_rand  = new double[2];
		double f_zus = 0.30;   // Stronger cohesion
		double f_sep = 0.2;    // Moderate separation
		double f_aus = 0.5;    // Stronger alignment
		double f_flee = 8.0;   // Flee force unchanged
		double f_rand = 0.15;  // Much less random force
		double fleeRadius = 0;
		for (Vehicle v : allVehicles) {
			if (v.type == 1) {
				fleeRadius = Math.max(70, v.FZL * 12.0); // Restore original logic
				break;
			}
		}
		double panicRadius = 0; // Much larger panic effect for full swarm propagation

		if (type == 1) {
			isFleeing = false;
			acc_dest = robustRandomWalk();
		} else {
			// Find special vehicle
			Vehicle special = null;
			for (Vehicle v : allVehicles) {
				if (v.type == 1) {
					special = v;
					break;
				}
			}
			// Check if this vehicle should flee
			boolean shouldFlee = false;
			if (special != null) {
				double dx = pos[0] - special.pos[0];
				double dy = pos[1] - special.pos[1];
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist < fleeRadius) {
					shouldFlee = true;
				}
			}
			// Panic propagation: if any other vehicle is fleeing and close, also flee
			if (!shouldFlee) {
				for (Vehicle v : allVehicles) {
					if (v == this || v.type == 1) continue;
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

	// Small random vector for swarm vehicles
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

	// Robust random walk for special vehicle
	private double[] robustRandomWalk() {
		double[] acc = new double[2];
		// Persistent direction, but allow smooth change
		wanderAngle += (Math.random() - 0.5) * 0.08; // Very smooth
		double mag = 2.0 + 1.0 * Math.random(); // Fast: 2.0 to 3.0
		acc[0] = Math.cos(wanderAngle) * mag;
		acc[1] = Math.sin(wanderAngle) * mag;
		// Strong repulsion from all four edges (use 1000x800 canvas)
		double margin = 100;
		if (pos[0] < margin) acc[0] += 4.0;
		if (pos[0] > 1000 * Simulation.pix - margin) acc[0] -= 4.0;
		if (pos[1] < margin) acc[1] += 4.0;
		if (pos[1] > 800 * Simulation.pix - margin) acc[1] -= 4.0;
		return acc;
	}

	void move(ArrayList<Vehicle> allVehicles) {
		double[] acc = calculateWeightedAcc(allVehicles);
	
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
				if (v.type == 1)
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

	// Flee from the special vehicle (type 1) if within a certain radius
	private double[] fleeFromSpecialVehicle(ArrayList<Vehicle> allVehicles, double fleeRadius) {
		double[] acc_flee = new double[2];
		acc_flee[0] = 0;
		acc_flee[1] = 0;
		Vehicle special = null;
		for (Vehicle v : allVehicles) {
			if (v.type == 1) {
				special = v;
				break;
			}
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
}
