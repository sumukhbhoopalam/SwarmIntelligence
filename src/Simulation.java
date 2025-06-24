import java.util.ArrayList;

import javax.swing.*;

public class Simulation extends JFrame {
	static int sleep = 8; // 8
	static double pix = 0.2;// 0.2
	int anzFz = 160;//number of swarm objects
	ArrayList<Vehicle> allVehicles = new ArrayList<Vehicle>();
	JPanel canvas = new Canvas(allVehicles, pix);

	Simulation() {
		setTitle("Swarm");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);

		for (int k = 0; k < anzFz; k++) {
			Vehicle car = new Vehicle();
			if (k == 0)
				car.type = 1;
			allVehicles.add(car);
		}

		add(canvas);
		setSize(1000, 800);
		setVisible(true);

	}

	public static void main(String args[]) {
		Simulation xx = new Simulation();
		xx.run();
	}

	public void run() {
		Vehicle v;
		long startTime = System.currentTimeMillis();
		boolean specialRemoved = false;

		while (true) {
			long elapsed = System.currentTimeMillis() - startTime;

			// Find the special vehicle (type == 1)
			Vehicle special = null;
			for (Vehicle veh : allVehicles) {
				if (veh.type == 1) {
					special = veh;
					break;
				}
			}

			// After 10 seconds, remove the special vehicle and create new vehicles
			if (!specialRemoved && elapsed > 10000) {
				if (special != null) {
					allVehicles.remove(special);
				}
				ArrayList<Vehicle> newVehicles = new ArrayList<>();
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0) {
						Vehicle newVeh = new Vehicle();
						// Copy position and velocity
						newVeh.pos[0] = veh.pos[0];
						newVeh.pos[1] = veh.pos[1];
						newVeh.vel[0] = veh.vel[0];
						newVeh.vel[1] = veh.vel[1];
						newVeh.isNew = true;
						newVehicles.add(newVeh);
					}
				}
				// Instead of clearing, just add the new vehicles
				allVehicles.addAll(newVehicles);
				specialRemoved = true;
			}

			// Collect swarm vehicles to remove
			ArrayList<Vehicle> toRemove = new ArrayList<>();
			if (special != null) {
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0) {
						double dx = veh.pos[0] - special.pos[0];
						double dy = veh.pos[1] - special.pos[1];
						double dist = Math.sqrt(dx * dx + dy * dy);
						// Use a contact threshold based on vehicle sizes
						double contactThreshold = (veh.FZL + special.FZL) * 3.0; // 3x FZL for each
						if (dist < contactThreshold) {
							toRemove.add(veh);
						}
					}
				}
			}
			// Remove vehicles that are in contact
			allVehicles.removeAll(toRemove);

			for (int i = 0; i < allVehicles.size(); i++) {
				v = allVehicles.get(i);
				v.move(allVehicles);
			}

			try {
				Thread.sleep(sleep);
			} 
			catch (InterruptedException e) {
			}
			repaint();
		}
	}
}
