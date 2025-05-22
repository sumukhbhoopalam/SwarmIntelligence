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


	Vehicle() {
		allId++;
		this.id = allId;
		this.FZL = 2;
		this.FZB = 1;
		this.rad_sep = 7;// 50
		this.rad_zus = 25;// 25
		this.type = 0;
		this.max_acc = 0.05;// 0.1
		this.max_vel = 1;

		pos = new double[2];
		vel = new double[2];
		
		pos[0] = Simulation.pix * 500 * Math.random();
		pos[1] = Simulation.pix * 500 * Math.random();
		vel[0] = max_vel * Math.random();
		vel[1] = max_vel * Math.random();
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
		double f_zus = 0.05;
		double f_sep = 0.55; // 0.55
		double f_aus = 0.4;

		if (type == 1) {
			acc_dest = random();
		} else {
			acc_dest1 = cohesion(allVehicles);
			//acc_dest1 = follow(allVehicles);
			acc_dest2 = separation(allVehicles);
			acc_dest3 = alignment(allVehicles);

			acc_dest[0] = (f_zus * acc_dest1[0]) + (f_sep * acc_dest2[0] + (f_aus * acc_dest3[0]));
			acc_dest[1] = (f_zus * acc_dest1[1]) + (f_sep * acc_dest2[1] + (f_aus * acc_dest3[1]));

		}
		
		acc_dest = VectorCalculation.truncate(acc_dest, max_acc);
		return acc_dest;
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

	
}
