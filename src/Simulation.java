import java.util.ArrayList;

import javax.swing.*;

public class Simulation extends JFrame {
	static int sleep = 8; // 8
	static double pix = 0.2;// 0.2
	int anzFz = 120; // number of swarm objects (excluding special vehicle)
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
		boolean specialActive = false;

		while (true) {
			long elapsed = System.currentTimeMillis() - startTime;

			// Activate the special vehicle only between 3 and 13 seconds
			if (!specialActive && elapsed > 3000 && elapsed <= 13000) {
				specialActive = true;
			}
			if (specialActive && elapsed > 13000) {
				specialActive = false;
			}

			// Find the special vehicle (type == 1)
			Vehicle special = null;
			for (Vehicle veh : allVehicles) {
				if (veh.type == 1) {
					special = veh;
					break;
				}
			}

			// Only allow contact/deletion logic if special is active and not removed
			if (specialActive && !specialRemoved) {
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
			}

			// After 13 seconds (10 seconds of special active), remove the special vehicle and create new vehicles
			if (!specialRemoved && elapsed > 13000) {
				if (special != null) {
					allVehicles.remove(special);
				}
				int numTarget = 120;
				int numRemaining = 0;
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0) numRemaining++;
				}
				int numToAdd = numTarget - numRemaining;
				if (numToAdd > 0 && numRemaining > 0) {
					ArrayList<Vehicle> newVehicles = new ArrayList<>();
					for (int i = 0; i < numToAdd; i++) {
						Vehicle template = allVehicles.get(i % numRemaining);
						Vehicle newVeh = new Vehicle();
						newVeh.pos[0] = template.pos[0];
						newVeh.pos[1] = template.pos[1];
						newVeh.vel[0] = template.vel[0];
						newVeh.vel[1] = template.vel[1];
						newVeh.isNew = true;
						newVehicles.add(newVeh);
					}
					allVehicles.addAll(newVehicles);
				}
				specialRemoved = true;
			}

			for (int i = 0; i < allVehicles.size(); i++) {
				v = allVehicles.get(i);
				// Only move the special vehicle if it is active
				if (v.type == 1 && !specialActive) continue;
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
