import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;

public class Simulation extends JFrame {
	static int sleep = 8; // 8
	static double pix = 0.2;// 0.2
	int anzFz = 120; // number of swarm objects (excluding special vehicle)
	ArrayList<Vehicle> allVehicles = new ArrayList<Vehicle>();
	JPanel canvas = new Canvas(allVehicles, pix);
	JButton redButton;
	JButton yellowButton;
	JButton lavenderButton;
	volatile int specialVehicleType = 0; // 0: none, 1: red, 2: yellow, 3: lavender
	Timer redTimer = new Timer();
	TimerTask redTimerTask = null;
	Timer newVehiclesTimer = new Timer();
	TimerTask newVehiclesTask = null;

	Simulation() {
		setTitle("Swarm");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);

		for (int k = 0; k < anzFz; k++) {
			Vehicle car = new Vehicle();
			allVehicles.add(car);
		}

		redButton = new JButton("Start Red Special Vehicle");
		yellowButton = new JButton("Start Yellow Special Vehicle");
		lavenderButton = new JButton("Start Lavender Special Vehicle");
		redButton.setBounds(20, 10, 200, 30);
		yellowButton.setBounds(240, 10, 220, 30);
		lavenderButton.setBounds(480, 10, 240, 30);
		canvas.setBounds(0, 50, 1000, 750);
		add(redButton);
		add(yellowButton);
		add(lavenderButton);
		add(canvas);
		setSize(1020, 850);
		setVisible(true);

		redButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activateSpecialVehicle(1);
			}
		});
		yellowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activateSpecialVehicle(2);
			}
		});
		lavenderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activateSpecialVehicle(3);
			}
		});
	}

	private synchronized void activateSpecialVehicle(int type) {
		// Remove any existing special vehicle
		allVehicles.removeIf(v -> v.type == 1 || v.type == 2 || v.type == 3);
		// Cancel any running red timer
		if (redTimerTask != null) {
			redTimerTask.cancel();
			redTimerTask = null;
		}
		// Cancel any pending new vehicles task
		if (newVehiclesTask != null) {
			newVehiclesTask.cancel();
			newVehiclesTask = null;
		}
		// Add the new special vehicle
		Vehicle special = new Vehicle();
		special.type = type;
		if (type == 1) {
			special.max_vel = 1.0;
			special.pos[0] = 1000 * Simulation.pix * Math.random();
			special.pos[1] = 800 * Simulation.pix * Math.random();
			// Start timer to remove after 10 seconds
			redTimerTask = new TimerTask() {
				public void run() {
					synchronized (Simulation.this) {
						allVehicles.removeIf(v -> v.type == 1);
						if (specialVehicleType == 1) specialVehicleType = 0;
						repaint();
						// Schedule new vehicles creation after 3 seconds
						newVehiclesTask = new TimerTask() {
							public void run() {
								synchronized (Simulation.this) {
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
									repaint();
								}
							}
						};
						newVehiclesTimer.schedule(newVehiclesTask, 3000);
					}
				}
			};
			redTimer.schedule(redTimerTask, 10000);
		} else if (type == 2) {
			special.max_vel = 1.0;
			special.pos[0] = 1000 * Simulation.pix * Math.random();
			special.pos[1] = 800 * Simulation.pix * Math.random();
			// Start timer to remove after 10 seconds
			redTimerTask = new TimerTask() {
				public void run() {
					synchronized (Simulation.this) {
						allVehicles.removeIf(v -> v.type == 2);
						if (specialVehicleType == 2) specialVehicleType = 0;
						repaint();
					}
				}
			};
			redTimer.schedule(redTimerTask, 10000);
		} else if (type == 3) {
			special.max_vel = 0.1;
			special.pos[0] = 1000 * Simulation.pix * Math.random();
			special.pos[1] = 800 * Simulation.pix * Math.random();
			// Start timer to remove after 10 seconds
			redTimerTask = new TimerTask() {
				public void run() {
					synchronized (Simulation.this) {
						allVehicles.removeIf(v -> v.type == 3);
						if (specialVehicleType == 3) specialVehicleType = 0;
						repaint();
					}
				}
			};
			redTimer.schedule(redTimerTask, 10000);
		}
		double angle = 2 * Math.PI * Math.random();
		special.vel[0] = special.max_vel * Math.cos(angle);
		special.vel[1] = special.max_vel * Math.sin(angle);
		allVehicles.add(special);
		specialVehicleType = type;
	}

	public static void main(String args[]) {
		Simulation xx = new Simulation();
		xx.run();
	}

	public void run() {
		Vehicle v;
		while (true) {
			// Remove all but one special vehicle if any bug
			allVehicles.removeIf(veh -> (veh.type == 1 || veh.type == 2 || veh.type == 3) && veh.type != specialVehicleType);
			// Contact logic for red special vehicle
			Vehicle special = null;
			for (Vehicle veh : allVehicles) {
				if (veh.type == 1 || veh.type == 2 || veh.type == 3) {
					special = veh;
					break;
				}
			}
			// Red special vehicle logic
			if (special != null && special.type == 1) {
				ArrayList<Vehicle> toRemove = new ArrayList<>();
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0) {
						double dx = veh.pos[0] - special.pos[0];
						double dy = veh.pos[1] - special.pos[1];
						double dist = Math.sqrt(dx * dx + dy * dy);
						double contactThreshold = (veh.FZL + special.FZL) * 3.0;
						if (dist < contactThreshold) {
							toRemove.add(veh);
						}
					}
				}
				allVehicles.removeAll(toRemove);
			}
			// Yellow special vehicle logic
			if (special != null && special.type == 2) {
				ArrayList<Vehicle> toRemove = new ArrayList<>();
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0 && !veh.isNew) {
						double dx = veh.pos[0] - special.pos[0];
						double dy = veh.pos[1] - special.pos[1];
						double dist = Math.sqrt(dx * dx + dy * dy);
						double contactThreshold = (veh.FZL + special.FZL) * 3.0;
						if (dist < contactThreshold) {
							toRemove.add(veh);
						}
					}
				}
				allVehicles.removeAll(toRemove);
			}
			// Move all vehicles
			for (int i = 0; i < allVehicles.size(); i++) {
				v = allVehicles.get(i);
				v.move(allVehicles);
			}
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {}
			repaint();
		}
	}
}
